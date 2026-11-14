"""RAGAS evaluation for SafePe's two RAG pipelines.

  fraud  - /api/v1/fraud/agentic-analyze: Gemini-embedding vector search over
           the 21 seeded fraud patterns, then a Gemini verdict summary.
  money  - /api/v1/assistant/chat: the Money Assistant, which loads a user's
           transactions from fraud_transaction_view and answers with Gemini.

Runs against a LOCAL stack. fraud-service is called directly on
127.0.0.1:18082 (see docker-compose.override.yml) with an X-User-Id header,
bypassing the gateway's Clerk JWT check.

Answers are collected first, then judged. Every answer and score is saved to
results/<RUN_LABEL>_state.json as it arrives, so a run can stop at a quota limit
and resume later. A Gemini failure stops the run: a fallback reply is never
recorded, because the judge would score it as if the model had written it.

Usage:
    .venv/Scripts/python run_eval.py fraud
    .venv/Scripts/python run_eval.py money
    .venv/Scripts/python run_eval.py all
    .venv/Scripts/python run_eval.py all --wait   # sleep through quota resets
"""
import json
import math
import os
import subprocess
import sys
import time
import uuid
import warnings
from datetime import datetime, timedelta, timezone
from pathlib import Path

import requests

from gemini_keys import (RotatingChat, RotatingEmbeddings, check_separate_quotas, load_keys, mask)

warnings.filterwarnings("ignore", category=DeprecationWarning)
# The Windows console defaults to cp1252, which cannot print the rupee sign.
sys.stdout.reconfigure(encoding="utf-8")

HERE = Path(__file__).resolve().parent
REPO = HERE.parent
RESULTS = HERE / "results"

FRAUD_URL = os.getenv("FRAUD_URL", "http://127.0.0.1:18082")
FRAUD_USER = "ragas_fraud_user"   # has no transactions -> velocity stays NORMAL
MONEY_USER = "ragas_money_user"   # seeded with known transactions below

# Pause per Gemini generation the app makes. The free tier allows 5 generations
# a minute, so 13s stays under it. agentic-analyze makes 2, assistant/chat 1.
APP_CALL_GAP_SECONDS = float(os.getenv("APP_CALL_GAP_SECONDS", "13"))
# SKIP_JUDGE=1 runs only the deterministic metrics (verdict accuracy, category
# hits) - no RAGAS LLM-judge calls. RUN_LABEL keeps runs apart.
SKIP_JUDGE = os.getenv("SKIP_JUDGE") == "1"
# JUDGE_ONLY=1 scores already-collected answers without touching the app or
# docker - e.g. to judge inside a Linux container (see README).
JUDGE_ONLY = os.getenv("JUDGE_ONLY") == "1"
RUN_LABEL = os.getenv("RUN_LABEL", "run")
MAX_JUDGE_ATTEMPTS = 3  # per sample; a score still missing after this stays n/a


class AppQuotaExhausted(Exception):
    """The app's Gemini key has used its daily quota."""


class AppGeminiFailing(Exception):
    """The app's Gemini calls keep failing for a reason other than daily quota."""


class JudgeExhausted(Exception):
    """Every judge key has used its daily quota."""


# ─────────────────────────── config ───────────────────────────

def load_env():
    env = {}
    path = REPO / ".env"
    if path.exists():
        for line in path.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if line and not line.startswith("#") and "=" in line:
                k, v = line.split("=", 1)
                env[k.strip()] = v.strip()
    return env


ENV = load_env()
# Same precedence as docker compose: a real environment variable beats .env.
GEMINI_KEY = (os.getenv("RAGAS_JUDGE_API_KEY") or os.getenv("GEMINI_API_KEY")
              or ENV.get("GEMINI_API_KEY", ""))
# A Flash model, deliberately not the app's own gemini-2.5-flash: the judge
# should not grade its own answers, and free-tier quota is per model, so the
# judge and the app draw on separate daily allowances of the same key.
JUDGE_MODEL = os.getenv("JUDGE_MODEL", "gemini-3.5-flash")
APP_MODEL = os.getenv("GEMINI_MODEL", "gemini-2.5-flash")
# Several free-tier keys, each from a DIFFERENT Google Cloud project, one per line.
KEYS = load_keys(HERE / "keys.txt") or ([GEMINI_KEY] if GEMINI_KEY else [])
# gemini-2.5-flash is closed to new projects, so the app needs a key from a
# project that already had access. APP_KEY_LINE is its line number in keys.txt.
APP_KEY_LINE = int(os.getenv("APP_KEY_LINE", str(len(KEYS) or 1)))
JUDGE_EMBED_MODEL = os.getenv("JUDGE_EMBED_MODEL", "models/gemini-embedding-001")
STATE_FILE = RESULTS / f"{RUN_LABEL}_state.json"


def require_real_key():
    if not KEYS or KEYS[0].startswith("local_dev") or len(KEYS[0]) < 20:
        sys.exit("No usable Gemini key. Add keys to rag-eval/keys.txt (one per line), "
                 "or set a real GEMINI_API_KEY in ../.env, then re-run.")


def judge():
    """RAGAS judge LLM + embeddings: Gemini via its OpenAI-compatible endpoint,
    rotating to the next key in keys.txt when one hits its daily quota.

    Not langchain-google-genai: its 2.x async path runs over gRPC and fails
    inside RAGAS's event loop ("Event loop is closed"), which RAGAS records as
    a 0 score - silently wrong results.

    Returns (llm, embeddings, out_of_keys) - out_of_keys() reports exhaustion.
    """
    from ragas.embeddings import LangchainEmbeddingsWrapper
    from ragas.llms import LangchainLLMWrapper
    chat = RotatingChat(KEYS, model_name=JUDGE_MODEL)
    emb = RotatingEmbeddings(KEYS, JUDGE_EMBED_MODEL)
    out_of_keys = lambda: chat.out_of_keys or emb.out_of_keys
    return LangchainLLMWrapper(chat), LangchainEmbeddingsWrapper(emb), out_of_keys


def run_config():
    from ragas import RunConfig
    # One at a time: the judge's current key allows 5 generations a minute.
    # RotatingChat already waits out rate limits, so few retries are needed here.
    return RunConfig(max_workers=1, max_retries=3, max_wait=60, timeout=300)


def ragas_eval(samples, metrics, llm, emb):
    """Run RAGAS; returns a list of per-sample score dicts (NaN on judge failure)."""
    from ragas import EvaluationDataset, evaluate
    result = evaluate(dataset=EvaluationDataset(samples=samples), metrics=metrics,
                      llm=llm, embeddings=emb, run_config=run_config(),
                      show_progress=False, raise_exceptions=False)
    return result.to_pandas().to_dict(orient="records")


def is_score(x):
    return isinstance(x, (int, float)) and not (isinstance(x, float) and math.isnan(x))


def mean(xs):
    xs = [x for x in xs if is_score(x)]
    return (sum(xs) / len(xs)) if xs else float("nan")


def fmt(x):
    return "n/a" if not is_score(x) else f"{x:.3f}"


def post(path, body, user):
    r = requests.post(FRAUD_URL + path, json=body, timeout=180,
                      headers={"X-User-Id": user, "Content-Type": "application/json"})
    r.raise_for_status()
    return r.json()


# ─────────────────────────── saved progress ───────────────────────────

def load_state():
    if STATE_FILE.exists():
        return json.loads(STATE_FILE.read_text(encoding="utf-8"))
    return {"fraud": {}, "money": {}, "judge_attempts": {}}


def save_state(state):
    """Write via a temp file so a kill mid-write cannot corrupt saved progress."""
    RESULTS.mkdir(exist_ok=True)
    tmp = STATE_FILE.with_suffix(".tmp")
    tmp.write_text(json.dumps(state, indent=1, default=str), encoding="utf-8")
    tmp.replace(STATE_FILE)


# ─────────────────────────── app calls ───────────────────────────

def ensure_app_key():
    """Give fraud-service the app key. Recreates it only when the key differs,
    because each startup spends one Gemini request pre-fetching FD rates."""
    key = KEYS[APP_KEY_LINE - 1]
    current = subprocess.run(["docker", "exec", "safepe-fraud-service", "printenv", "GEMINI_API_KEY"],
                             capture_output=True, text=True).stdout.strip()
    if current == key:
        print(f"[app] fraud-service already on keys.txt line {APP_KEY_LINE} ({mask(key)})")
    else:
        env = dict(os.environ, GEMINI_API_KEY=key)  # shell env beats .env in compose
        subprocess.run(["docker", "compose", "up", "-d", "--force-recreate", "fraud-service"],
                       cwd=REPO, env=env, check=True, capture_output=True)
        print(f"[app] fraud-service recreated with keys.txt line {APP_KEY_LINE} ({mask(key)})")
    for _ in range(60):
        try:
            if requests.get(FRAUD_URL + "/actuator/health", timeout=3).status_code == 200:
                return
        except requests.RequestException:
            pass
        time.sleep(3)
    raise requests.ConnectionError("fraud-service did not become healthy")


def app_quota_kind():
    """Read fraud-service's recent log to see why its Gemini call failed. The
    error body spans several log lines, so search the text rather than a line."""
    import re
    log = subprocess.run(["docker", "compose", "logs", "--since", "2m", "fraud-service"],
                         cwd=REPO, capture_output=True, text=True, encoding="utf-8",
                         errors="replace").stdout
    limits = {m.lower() for m in re.findall(r"per ?(day|minute)", log, re.I)}
    return "daily" if "day" in limits else "minute" if "minute" in limits else "other"


def call_app(path, body, user, failed):
    """POST to the app and return a real Gemini-backed answer, or raise."""
    for attempt in range(1, 4):
        res = post(path, body, user)
        if not failed(res):
            return res
        kind = app_quota_kind()
        if kind == "daily":
            raise AppQuotaExhausted()
        print(f"  [app] Gemini call failed ({kind}), waiting 65s (attempt {attempt}/3)", flush=True)
        time.sleep(65)
    raise AppGeminiFailing()


def fraud_failed(res):
    steps = {s["stepNumber"]: s.get("result", "") for s in res.get("reasoningSteps", [])}
    # Classification and summary both fall back to fixed text when Gemini fails.
    return "Classification failed" in steps.get(1, "") or res.get("summary", "").startswith(("RAG Vector DB match (", "RAG Vector DB verified:"))


def money_failed(res):
    reply = res.get("reply", "")
    return reply.startswith("Error: Could not reach the AI") or reply.startswith("Sorry, I couldn't generate")


# ─────────────────────────── judging ───────────────────────────

def judge_stage(state, stage, rows, row_id, make_sample, metrics, fields, judge_tools):
    """Score rows one at a time, saving after each. fields: {row field: metric name prefix}."""
    llm, emb, out_of_keys = judge_tools
    attempts = state["judge_attempts"]
    for r in rows:
        tag = f"{stage}:{row_id(r)}"
        while (not all(is_score(r.get(f)) for f in fields)
               and attempts.get(tag, 0) < MAX_JUDGE_ATTEMPTS):
            scores = ragas_eval([make_sample(r)], metrics, llm, emb)[0]
            if out_of_keys():
                raise JudgeExhausted()
            for field, prefix in fields.items():
                r[field] = next((v for k, v in scores.items() if k.startswith(prefix)), None)
            attempts[tag] = attempts.get(tag, 0) + 1
            save_state(state)
            print(f"  [judge] {tag} " + " ".join(f"{f}={fmt(r.get(f))}" for f in fields), flush=True)


# ─────────────────────────── fraud RAG ───────────────────────────

def collect_fraud(state):
    cases = json.loads((HERE / "datasets" / "fraud_cases.json").read_text(encoding="utf-8"))["cases"]
    print(f"\n[fraud] {len(state['fraud'])}/{len(cases)} answers already collected", flush=True)
    for i, c in enumerate(cases, 1):
        if c["id"] in state["fraud"]:
            continue
        res = call_app("/api/v1/fraud/agentic-analyze", {"message": c["message"]}, FRAUD_USER, fraud_failed)
        steps = {s["stepNumber"]: s.get("result", "") for s in res.get("reasoningSteps", [])}
        patterns = res.get("matchedPatterns") or []
        cats = [p["category"] for p in patterns]
        flagged = res["action"] in ("BLOCK", "FLAG_VERIFICATION")
        state["fraud"][c["id"]] = {
            **c,
            "action": res["action"], "risk": res["riskScore"],
            "max_similarity": res["maxSimilarityPercent"],
            "summary": res.get("summary", ""),
            "retrieved": [p["description"] for p in patterns],
            "retrieved_categories": cats,
            # Everything the summary LLM was shown (see AgenticFraudEngine step 3 prompt).
            # Built from fields rather than step 3's text, which contains the summary itself.
            "generation_context": [
                "Classification: " + steps.get(1, ""),
                steps.get(2, ""),
                f"Calculated Trust Score: {res['trustScore']}% | Risk Score: "
                f"{res['riskScore']}% | Action: {res['action']}",
            ],
            "verdict_correct": flagged == (c["label"] == "SCAM"),
            "top1_category_hit": bool(cats) and cats[0] in c["expected_categories"],
            "any_category_hit": any(x in c["expected_categories"] for x in cats),
        }
        save_state(state)
        print(f"  {i:2}/{len(cases)} {c['id']} {c['label']:5} -> {res['action']:17} "
              f"sim={res['maxSimilarityPercent']:5.1f}% top={cats[0] if cats else '-'}", flush=True)
        time.sleep(2 * APP_CALL_GAP_SECONDS)


def judge_fraud(state, judge_tools):
    from ragas import SingleTurnSample
    from ragas.metrics import (Faithfulness, LLMContextPrecisionWithReference,
                               LLMContextRecall, ResponseRelevancy)
    rows = list(state["fraud"].values())
    q = lambda r: f"Is this SMS a scam? Explain the verdict.\n\nSMS: {r['message']}"
    row_id = lambda r: r["id"]

    # 1) Generation quality: is the summary grounded in what the model was shown,
    #    and does it address the question?
    judge_stage(state, "fraud-generation", rows, row_id,
                lambda r: SingleTurnSample(user_input=q(r), response=r["summary"],
                                           retrieved_contexts=r["generation_context"], reference=r["reference"]),
                [Faithfulness(), ResponseRelevancy(strictness=1)],
                {"faithfulness": "faithfulness", "answer_relevancy": "answer_relevancy"}, judge_tools)

    # 2) Retrieval quality on the vector search itself. Only samples that
    #    retrieved something can be scored; recall only makes sense for scams
    #    (a legit message has no "relevant" pattern to recall).
    with_ctx = [r for r in rows if r["retrieved"]]
    retrieval_sample = lambda r: SingleTurnSample(user_input=q(r), retrieved_contexts=r["retrieved"],
                                                  reference=r["reference"])
    judge_stage(state, "fraud-precision", with_ctx, row_id, retrieval_sample,
                [LLMContextPrecisionWithReference()],
                {"context_precision": "llm_context_precision_with_reference"}, judge_tools)
    judge_stage(state, "fraud-recall", [r for r in with_ctx if r["label"] == "SCAM"], row_id, retrieval_sample,
                [LLMContextRecall()], {"context_recall": "context_recall"}, judge_tools)


# ─────────────────────────── Money Assistant ───────────────────────────

SEED = [  # (payee_upi, amount, created_at)
    ("swiggy@icici", 450.00, "2026-11-01 13:05:00"),
    ("swiggy@icici", 320.00, "2026-11-02 20:40:00"),
    ("swiggy@icici", 610.00, "2026-11-03 21:15:00"),
    ("zomato@hdfc", 280.00, "2026-11-02 13:30:00"),
    ("amazon@apl", 2499.00, "2026-11-01 11:00:00"),
    ("flipkart@axis", 1299.00, "2026-11-03 10:20:00"),
    ("bescom@sbi", 1840.00, "2026-11-01 09:10:00"),
    ("airtel@ybl", 299.00, "2026-11-02 08:45:00"),
    ("landlord@okicici", 15000.00, "2026-11-01 08:00:00"),
    ("uber@paytm", 210.00, "2026-11-02 18:25:00"),
    ("uber@paytm", 340.00, "2026-11-03 09:05:00"),
]


def money_questions():
    total = sum(a for _, a, _ in SEED)
    by = {}
    for p, a, _ in SEED:
        by.setdefault(p, []).append(a)
    s = lambda p: sum(by.get(p, []))
    largest = max(SEED, key=lambda t: t[1])
    smallest = min(SEED, key=lambda t: t[1])
    food = s("swiggy@icici") + s("zomato@hdfc")
    inr = lambda x: f"₹{x:,.2f}"
    return [
        ("What is my total spending?",
         f"Your total spending is {inr(total)} across {len(SEED)} transactions."),
        ("What was my largest transaction and who was it to?",
         f"Your largest transaction was {inr(largest[1])} to {largest[0]}."),
        ("How much did I spend on Swiggy in total?",
         f"You spent {inr(s('swiggy@icici'))} on Swiggy across {len(by['swiggy@icici'])} transactions."),
        ("How many transactions have I made?",
         f"You have made {len(SEED)} transactions."),
        ("How much have I spent on Uber rides?",
         f"You spent {inr(s('uber@paytm'))} on Uber across {len(by['uber@paytm'])} rides."),
        ("Did I pay my electricity bill, and how much was it?",
         f"Yes, you paid {inr(s('bescom@sbi'))} to bescom@sbi, the electricity provider."),
        ("What was my smallest transaction?",
         f"Your smallest transaction was {inr(smallest[1])} to {smallest[0]}."),
        ("How much did I spend on food delivery from Swiggy and Zomato combined?",
         f"You spent {inr(food)} on food delivery: {inr(s('swiggy@icici'))} on Swiggy and "
         f"{inr(s('zomato@hdfc'))} on Zomato."),
        ("Did I make any payment to Netflix?",
         "No. There are no payments to Netflix in your transactions."),
        ("Based on my transactions, what is one way I could reduce my spending?",
         f"Rent of {inr(largest[1])} is your largest expense; among discretionary spending, "
         f"food delivery totals {inr(food)} across 4 orders and could be reduced by cooking more often."),
    ]


def seed_money_user():
    import psycopg2
    conn = psycopg2.connect(host="127.0.0.1", port=5432, dbname=ENV.get("POSTGRES_DB", "safepe_db"),
                            user=ENV.get("POSTGRES_USER", "safepe_user"),
                            password=ENV["POSTGRES_PASSWORD"])
    with conn, conn.cursor() as cur:
        cur.execute("DELETE FROM fraud_transaction_view WHERE user_id IN (%s, %s)",
                    (MONEY_USER, FRAUD_USER))
        for payee, amount, ts in SEED:
            cur.execute(
                "INSERT INTO fraud_transaction_view (id, user_id, payee_upi, amount, currency, "
                "type, status, created_at) VALUES (%s, %s, %s, %s, 'INR', 'UPI', 'SUCCESS', %s)",
                (str(uuid.uuid4()), MONEY_USER, payee, amount, ts))
    conn.close()


def money_contexts():
    """The context MoneyAssistantService builds: summary + rows, newest first."""
    rows = sorted(SEED, key=lambda t: t[2], reverse=True)
    # LocalDateTime.toString() drops ":00" seconds, matching what the prompt shows.
    stamp = lambda ts: datetime.strptime(ts, "%Y-%m-%d %H:%M:%S").strftime("%Y-%m-%dT%H:%M")
    ctx = [f"USER FINANCIAL SUMMARY: Total Transactions: {len(SEED)}; "
           f"Total Spent: ₹{sum(a for _, a, _ in SEED):.2f}"]
    ctx += [f"- {stamp(ts)} | ₹{a:.2f} | to {p} | Type: UPI | Status: SUCCESS" for p, a, ts in rows]
    return ctx


def collect_money(state):
    qs = money_questions()
    print(f"\n[money] {len(state['money'])}/{len(qs)} answers already collected", flush=True)
    if len(state["money"]) == len(qs):
        return
    seed_money_user()
    for i, (question, reference) in enumerate(qs, 1):
        if question in state["money"]:
            continue
        res = call_app("/api/v1/assistant/chat", {"message": question}, MONEY_USER, money_failed)
        state["money"][question] = {"question": question, "reference": reference, "response": res.get("reply", "")}
        save_state(state)
        print(f"  {i:2}/{len(qs)} {question[:60]}", flush=True)
        time.sleep(APP_CALL_GAP_SECONDS)


def judge_money(state, judge_tools):
    from ragas import SingleTurnSample
    from ragas.metrics import (FactualCorrectness, Faithfulness, LLMContextRecall,
                               ResponseRelevancy)
    ctx = money_contexts()
    # Context precision is omitted: retrieval here is "all of the user's
    # transactions" regardless of the question, so there is nothing to rank.
    judge_stage(state, "money", list(state["money"].values()), lambda r: r["question"],
                lambda r: SingleTurnSample(user_input=r["question"], response=r["response"],
                                           retrieved_contexts=ctx, reference=r["reference"]),
                [Faithfulness(), ResponseRelevancy(strictness=1), FactualCorrectness(), LLMContextRecall()],
                {"faithfulness": "faithfulness", "answer_relevancy": "answer_relevancy",
                 "factual_correctness": "factual_correctness", "context_recall": "context_recall"},
                judge_tools)


# ─────────────────────────── report ───────────────────────────

def write_report(fraud_rows, money_rows):
    RESULTS.mkdir(exist_ok=True)
    stamp = datetime.now().strftime("%Y-%m-%d %H:%M")
    out = [f"# SafePe RAG evaluation (RAGAS)\n",
           f"Run: `{RUN_LABEL}` at {stamp} | App model: `{APP_MODEL}` | Judge: "
           + ("skipped (SKIP_JUDGE=1), deterministic metrics only" if SKIP_JUDGE
              else f"`{JUDGE_MODEL}` + `{JUDGE_EMBED_MODEL}`") + "\n"]

    if fraud_rows:
        (RESULTS / f"{RUN_LABEL}_fraud_results.json").write_text(json.dumps(fraud_rows, indent=2, default=str),
                                                    encoding="utf-8")
        scams = [r for r in fraud_rows if r["label"] == "SCAM"]
        legit = [r for r in fraud_rows if r["label"] == "LEGIT"]
        out += ["\n## Fraud-pattern RAG (`/fraud/agentic-analyze`)\n",
                f"{len(fraud_rows)} cases: {len(scams)} scam paraphrases, {len(legit)} legitimate look-alikes.\n",
                "\n| Metric | Score | What it measures |", "|---|---|---|",
                f"| Verdict accuracy | {fmt(mean([r['verdict_correct'] for r in fraud_rows]))} | Scam flagged / legit allowed (deterministic) |",
                f"| Scam detection rate (recall) | {fmt(mean([r['verdict_correct'] for r in scams]))} | Share of scams flagged or blocked |",
                f"| False-positive rate | {fmt(1 - mean([r['verdict_correct'] for r in legit]) if legit else float('nan'))} | Share of legit messages wrongly flagged |",
                f"| Top-1 category hit | {fmt(mean([r['top1_category_hit'] for r in scams]))} | Best match is the right scam family (scams) |",
                f"| Any category hit | {fmt(mean([r['any_category_hit'] for r in scams]))} | Right family anywhere in results (scams) |",
                f"| Faithfulness | {fmt(mean([r.get('faithfulness') for r in fraud_rows]))} | Summary claims supported by the evidence shown to the LLM |",
                f"| Answer relevancy | {fmt(mean([r.get('answer_relevancy') for r in fraud_rows]))} | Summary addresses \"is this a scam?\" |",
                f"| Context precision | {fmt(mean([r.get('context_precision') for r in fraud_rows]))} | Retrieved patterns are relevant, ranked first |",
                f"| Context recall | {fmt(mean([r.get('context_recall') for r in fraud_rows]))} | Retrieved patterns cover the reference (scams) |",
                "\n| ID | Label | Action | Max sim % | Top category | Expected | Faith | Relev | Prec | Recall |",
                "|---|---|---|---|---|---|---|---|---|---|"]
        for r in fraud_rows:
            mark = "" if r["verdict_correct"] else " ❌"
            out.append(f"| {r['id']} | {r['label']} | {r['action']}{mark} | {r['max_similarity']} | "
                       f"{(r['retrieved_categories'] or ['-'])[0]} | {', '.join(r['expected_categories']) or '-'} | "
                       f"{fmt(r.get('faithfulness'))} | {fmt(r.get('answer_relevancy'))} | "
                       f"{fmt(r.get('context_precision'))} | {fmt(r.get('context_recall'))} |")

    if money_rows:
        (RESULTS / f"{RUN_LABEL}_money_results.json").write_text(json.dumps(money_rows, indent=2, default=str),
                                                    encoding="utf-8")
        out += ["\n## Money Assistant (`/assistant/chat`)\n",
                f"{len(money_rows)} questions over {len(SEED)} seeded transactions for `{MONEY_USER}`.\n",
                "\n| Metric | Score | What it measures |", "|---|---|---|",
                f"| Faithfulness | {fmt(mean([r.get('faithfulness') for r in money_rows]))} | Answer claims supported by the transactions given |",
                f"| Answer relevancy | {fmt(mean([r.get('answer_relevancy') for r in money_rows]))} | Answer addresses the question |",
                f"| Factual correctness | {fmt(mean([r.get('factual_correctness') for r in money_rows]))} | Answer matches the computed ground truth |",
                f"| Context recall | {fmt(mean([r.get('context_recall') for r in money_rows]))} | Needed facts were present in the context |",
                "\n| # | Question | Faith | Relev | Factual | Response |", "|---|---|---|---|---|---|"]
        for i, r in enumerate(money_rows, 1):
            resp = r["response"].replace("\n", " ").replace("|", "/")
            out.append(f"| {i} | {r['question']} | {fmt(r.get('faithfulness'))} | "
                       f"{fmt(r.get('answer_relevancy'))} | {fmt(r.get('factual_correctness'))} | "
                       f"{resp[:160]}{'…' if len(resp) > 160 else ''} |")

    report = RESULTS / f"{RUN_LABEL}_report.md"
    report.write_text("\n".join(out) + "\n", encoding="utf-8")
    print(f"\nReport: {report}", flush=True)


# ─────────────────────────── main ───────────────────────────

def wait_until_quota_reset():
    """Free-tier daily quota resets at midnight Pacific (08:00 UTC in winter).
    Polls the wall clock, since a long sleep would not count time the laptop
    spends asleep."""
    now = datetime.now(timezone.utc)
    reset = now.replace(hour=8, minute=5, second=0, microsecond=0)
    if reset <= now:
        reset += timedelta(days=1)
    print(f"Waiting for quota reset at {reset.astimezone():%Y-%m-%d %H:%M %Z} ...", flush=True)
    while datetime.now(timezone.utc) < reset:
        time.sleep(60)


def run_once(which, state):
    if JUDGE_ONLY:
        missing = [n for n, total, have in (("fraud", 18, state["fraud"]), ("money", 10, state["money"]))
                   if which in (n, "all") and len(have) < total]
        if missing:
            sys.exit(f"JUDGE_ONLY=1 needs every answer collected first; missing: {', '.join(missing)}")
    else:
        ensure_app_key()
        if which in ("fraud", "all"):
            collect_fraud(state)
        if which in ("money", "all"):
            collect_money(state)
    if not SKIP_JUDGE:
        tools = judge()
        if which in ("fraud", "all"):
            judge_fraud(state, tools)
        if which in ("money", "all"):
            judge_money(state, tools)
    write_report(list(state["fraud"].values()), list(state["money"].values()))


def main():
    args = [a for a in sys.argv[1:] if a != "--wait"]
    wait = "--wait" in sys.argv[1:]
    which = args[0] if args else "all"
    if which not in ("fraud", "money", "all"):
        sys.exit(__doc__)
    require_real_key()
    fresh = not STATE_FILE.exists()
    state = load_state()
    print(f"{len(KEYS)} Gemini key(s) | app: {APP_MODEL} on keys.txt line {APP_KEY_LINE} | judge: {JUDGE_MODEL}",
          flush=True)
    if fresh and not SKIP_JUDGE and len(KEYS) > 1 and not check_separate_quotas(KEYS):
        sys.exit("Keys share a quota: some of them are in the same Google Cloud project. "
                 "Create each key in its own project.")
    while True:
        try:
            run_once(which, state)
            return
        except (AppQuotaExhausted, JudgeExhausted) as e:
            print(f"\n{type(e).__name__}: answers fraud={len(state['fraud'])} money={len(state['money'])}. "
                  "Progress is saved.", flush=True)
            if not wait:
                sys.exit("Re-run the same command after the daily quota resets.")
            wait_until_quota_reset()
        except (AppGeminiFailing, requests.RequestException, subprocess.CalledProcessError) as e:
            print(f"\n{type(e).__name__}: Gemini or the local stack is unavailable. Progress is saved.", flush=True)
            if not wait:
                sys.exit("Fix the cause and re-run the same command.")
            print("Retrying in 30 minutes ...", flush=True)
            time.sleep(1800)


if __name__ == "__main__":
    main()
