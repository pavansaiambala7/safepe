"""Rotate across several Gemini API keys when the free-tier DAILY quota runs out.

Free-tier quota is counted per Google Cloud project, per model (for example
gemini-2.5-flash: 20 generate requests/day). Keys created in the SAME project
share one quota, so rotation only helps with keys from DIFFERENT projects.

Two consumers rotate independently:
  * RotatingChat / RotatingEmbeddings - the RAGAS judge, in-process.
  * the app (fraud-service) - run_eval.py recreates the container with the
    next key when the app's replies show Gemini failed.
"""
import asyncio
import re
import threading
import time
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from typing import Any, List, Optional

import openai
import requests
from langchain_core.callbacks import AsyncCallbackManagerForLLMRun, CallbackManagerForLLMRun
from langchain_core.embeddings import Embeddings
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage
from langchain_core.outputs import ChatResult
from langchain_openai import ChatOpenAI, OpenAIEmbeddings
from pydantic import PrivateAttr

GEMINI_OPENAI_URL = "https://generativelanguage.googleapis.com/v1beta/openai/"


def load_keys(path: Path) -> List[str]:
    """One key per line; blank lines and '#' comments ignored."""
    if not path.exists():
        return []
    keys = [l.strip() for l in path.read_text(encoding="utf-8").splitlines()]
    return [k for k in keys if k and not k.startswith("#")]


def mask(key: str) -> str:
    return f"{key[:6]}...{key[-3:]}" if len(key) > 12 else "***"


def quota_kind(err: Exception) -> str:
    """'daily' (rotate to the next key), 'minute' (wait and retry), or 'other'."""
    text = str(err)
    # 503 "high demand" spikes pass within minutes, so wait them out like a rate limit.
    if isinstance(err, openai.InternalServerError) and err.status_code == 503:
        return "minute"
    if not isinstance(err, openai.RateLimitError) and "RESOURCE_EXHAUSTED" not in text:
        return "other"
    if re.search(r"per ?day|PerDay", text, re.I) or re.search(r"retry in \d+h", text):
        return "daily"
    return "minute"


def retry_seconds(err: Exception, default: float = 30.0) -> float:
    m = re.search(r"retry in ([\d.]+)s", str(err))
    return min(float(m.group(1)) + 1, 90.0) if m else default


def check_separate_quotas(keys: List[str], model: str = "gemini-3.1-flash-lite", calls_per_key: int = 4) -> bool:
    """False if keys share a project. Sends calls_per_key parallel requests on each
    key: separate projects stay under any per-minute limit, while one shared project
    receives them all at once and returns 429. Uses a light model, so the app's and
    the judge's quotas are untouched."""
    url = f"{GEMINI_OPENAI_URL}chat/completions"

    def call(key):
        return requests.post(url, headers={"Authorization": f"Bearer {key}"}, timeout=120,
                             json={"model": model, "messages": [{"role": "user", "content": "Reply with OK"}]}).status_code

    jobs = [k for k in keys for _ in range(calls_per_key)]
    with ThreadPoolExecutor(max_workers=len(jobs)) as pool:
        codes = list(pool.map(call, jobs))
    print(f"Key check: {len(jobs)} parallel calls on {model}, status counts {dict(Counter(codes))}", flush=True)
    return 429 not in codes


class _Rotor:
    """Shared, thread-safe "which key are we on" state."""

    def __init__(self, keys: List[str], label: str):
        if not keys:
            raise ValueError("no Gemini keys available")
        self.keys, self.label, self.idx = keys, label, 0
        self.lock = threading.Lock()

    def exhausted(self, seen_idx: int) -> bool:
        """Advance past seen_idx. Returns False when every key is used up.

        Compare-and-set: two concurrent calls can both fail on the same key;
        only the first may advance, or a good key would be skipped.
        """
        with self.lock:
            if self.idx == seen_idx:
                self.idx += 1
                if self.idx < len(self.keys):
                    print(f"\n  [{self.label}] key {seen_idx + 1} hit its daily quota -> "
                          f"switching to key {self.idx + 1}/{len(self.keys)} ({mask(self.keys[self.idx])})")
            return self.idx < len(self.keys)


class RotatingChat(BaseChatModel):
    """ChatOpenAI against Gemini's OpenAI-compatible endpoint, with key rotation."""
    model_name: str
    temperature: float = 0.0
    timeout: float = 120.0
    _rotor: Any = PrivateAttr()
    _clients: dict = PrivateAttr(default_factory=dict)

    def __init__(self, keys: List[str], **kw):
        super().__init__(**kw)
        self._rotor = _Rotor(keys, f"judge {kw['model_name']}")

    @property
    def _llm_type(self) -> str:
        return "rotating-gemini-openai"

    @property
    def out_of_keys(self) -> bool:
        return self._rotor.idx >= len(self._rotor.keys)

    def _client(self, idx: int) -> ChatOpenAI:
        if idx not in self._clients:
            self._clients[idx] = ChatOpenAI(
                model=self.model_name, api_key=self._rotor.keys[idx], base_url=GEMINI_OPENAI_URL,
                temperature=self.temperature, max_retries=0, timeout=self.timeout)
        return self._clients[idx]

    def _generate(self, messages: List[BaseMessage], stop: Optional[List[str]] = None,
                  run_manager: Optional[CallbackManagerForLLMRun] = None, **kw) -> ChatResult:
        while True:
            idx = self._rotor.idx
            if idx >= len(self._rotor.keys):
                raise RuntimeError(f"All {len(self._rotor.keys)} judge keys exhausted for today")
            try:
                return self._client(idx)._generate(messages, stop=stop, **kw)
            except Exception as e:  # noqa: BLE001 - classified below
                kind = quota_kind(e)
                if kind == "daily":
                    if not self._rotor.exhausted(idx):
                        raise RuntimeError("All judge keys exhausted for today") from e
                elif kind == "minute":
                    time.sleep(retry_seconds(e))
                else:
                    raise

    async def _agenerate(self, messages: List[BaseMessage], stop: Optional[List[str]] = None,
                         run_manager: Optional[AsyncCallbackManagerForLLMRun] = None, **kw) -> ChatResult:
        while True:
            idx = self._rotor.idx
            if idx >= len(self._rotor.keys):
                raise RuntimeError(f"All {len(self._rotor.keys)} judge keys exhausted for today")
            try:
                return await self._client(idx)._agenerate(messages, stop=stop, **kw)
            except Exception as e:  # noqa: BLE001
                kind = quota_kind(e)
                if kind == "daily":
                    if not self._rotor.exhausted(idx):
                        raise RuntimeError("All judge keys exhausted for today") from e
                elif kind == "minute":
                    await asyncio.sleep(retry_seconds(e))
                else:
                    raise


class RotatingEmbeddings(Embeddings):
    """Gemini embeddings (OpenAI-compatible endpoint) with key rotation."""

    def __init__(self, keys: List[str], model: str):
        self.model = model.removeprefix("models/")
        self._rotor = _Rotor(keys, f"judge {self.model}")
        self._clients = {}

    @property
    def out_of_keys(self) -> bool:
        return self._rotor.idx >= len(self._rotor.keys)

    def _client(self, idx: int) -> OpenAIEmbeddings:
        if idx not in self._clients:
            self._clients[idx] = OpenAIEmbeddings(
                model=self.model, api_key=self._rotor.keys[idx], base_url=GEMINI_OPENAI_URL,
                # Otherwise it sends tiktoken token IDs, which only OpenAI accepts.
                check_embedding_ctx_length=False, max_retries=0)
        return self._clients[idx]

    def _call(self, fn_name: str, arg):
        while True:
            idx = self._rotor.idx
            if idx >= len(self._rotor.keys):
                raise RuntimeError("All embedding keys exhausted for today")
            try:
                return getattr(self._client(idx), fn_name)(arg)
            except Exception as e:  # noqa: BLE001
                kind = quota_kind(e)
                if kind == "daily":
                    self._rotor.exhausted(idx)
                elif kind == "minute":
                    time.sleep(retry_seconds(e))
                else:
                    raise

    async def _acall(self, fn_name: str, arg):
        while True:
            idx = self._rotor.idx
            if idx >= len(self._rotor.keys):
                raise RuntimeError("All embedding keys exhausted for today")
            try:
                return await getattr(self._client(idx), fn_name)(arg)
            except Exception as e:  # noqa: BLE001
                kind = quota_kind(e)
                if kind == "daily":
                    self._rotor.exhausted(idx)
                elif kind == "minute":
                    await asyncio.sleep(retry_seconds(e))
                else:
                    raise

    def embed_documents(self, texts):
        return self._call("embed_documents", texts)

    def embed_query(self, text):
        return self._call("embed_query", text)

    async def aembed_documents(self, texts):
        return await self._acall("aembed_documents", texts)

    async def aembed_query(self, text):
        return await self._acall("aembed_query", text)
