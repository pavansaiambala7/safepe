# SafePe RAG evaluation (RAGAS)

Evaluates the two RAG pipelines in fraud-service against a **local** stack:

| Pipeline | Endpoint | Retrieval |
|---|---|---|
| Fraud patterns | `POST /api/v1/fraud/agentic-analyze` | Gemini embeddings + cosine similarity over 21 seeded patterns |
| Money Assistant | `POST /api/v1/assistant/chat` | The user's transactions from `fraud_transaction_view` |

Metrics: RAGAS faithfulness, answer relevancy, context precision / recall, and
factual correctness (Money Assistant), plus deterministic fraud-verdict
accuracy, false-positive rate and category hit rate.

## Setup

```
python -m venv .venv
.venv/Scripts/python -m pip install -r requirements.txt
```

`requirements.txt` pins LangChain 0.3: RAGAS 0.4.3 imports a module that the
LangChain 1.x line removed.

The local stack must expose fraud-service on `127.0.0.1:18082` (see
`docker-compose.override.yml` at the repo root, which is local-only):

```
docker compose up -d db redis kafka eureka-server fraud-service
```

## Gemini keys

Free-tier quota is per **Google Cloud project, per model** (gemini-2.5-flash:
20 generate requests/day). Put several keys, **each from a different project**,
in `keys.txt` (gitignored), one per line. Keys from the same project share one
quota, so rotating between them does not help.

- The **judge** (`gemini-3.5-flash` by default, `JUDGE_MODEL` to change)
  rotates to the next key when one hits its daily quota.
- The **app** (fraud-service) uses one key, from line `APP_KEY_LINE` of
  `keys.txt` (default: the last line). `gemini-2.5-flash` is closed to new
  projects, so this key must come from a project that already had access. It
  allows 20 calls a day, so the 46 app calls take three quota windows.

Without `keys.txt`, the single `GEMINI_API_KEY` from the environment / `.env`
is used. The first run sends a few parallel calls per key on a light model and
stops if the keys turn out to share a quota.

## Run

```
.venv/Scripts/python run_eval.py fraud     # or: money, all
.venv/Scripts/python run_eval.py all --wait
```

Answers are collected first, then judged, and all progress is saved to
`results/<RUN_LABEL>_state.json`. If Gemini fails, the run stops without
recording the fallback reply, because the judge would score it as a real answer.
Re-run the same command to resume, or pass `--wait` to sleep through quota
resets (midnight Pacific) and continue automatically.

| Variable | Default | Purpose |
|---|---|---|
| `SKIP_JUDGE=1` | off | Deterministic metrics only, no RAGAS judge calls |
| `RUN_LABEL` | `run` | Prefix for files in `results/` |
| `JUDGE_MODEL` | `gemini-3.5-flash` | RAGAS judge LLM |
| `APP_KEY_LINE` | last line | Line of `keys.txt` the app uses |
| `JUDGE_ONLY=1` | off | Score saved answers only; no app or docker calls |
| `APP_CALL_GAP_SECONDS` | `13` | Pause per app Gemini call (5 per minute limit) |

If Windows Smart App Control blocks pyarrow (`An Application Control policy
has blocked this file`), run the judge in a Linux container once all answers
are collected:

```
docker run --rm -v "C:/Users/<you>/safepe/rag-eval:/work/rag-eval" -w /work/rag-eval \
  -e RUN_LABEL=full -e JUDGE_ONLY=1 python:3.12-slim \
  sh -c "pip install -q -r requirements.txt && python run_eval.py all --wait"
```

Latest results: [results/full_report.md](results/full_report.md).

Approximate Gemini requests for `all`: app 46 (gemini-2.5-flash), judge ~160-175
(judge model). Embedding calls use a separate quota.
