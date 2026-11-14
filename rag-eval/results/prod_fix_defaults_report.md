# SafePe RAG evaluation (RAGAS)

Run: `prod_fix_defaults` at 2026-11-06 01:20 | Judge: skipped (SKIP_JUDGE=1), deterministic metrics only


## Fraud-pattern RAG (`/fraud/agentic-analyze`)

18 cases: 12 scam paraphrases, 6 legitimate look-alikes.


| Metric | Score | What it measures |
|---|---|---|
| Verdict accuracy | 1.000 | Scam flagged / legit allowed (deterministic) |
| Scam detection rate (recall) | 1.000 | Share of scams flagged or blocked |
| False-positive rate | 0.000 | Share of legit messages wrongly flagged |
| Top-1 category hit | 1.000 | Best match is the right scam family (scams) |
| Any category hit | 1.000 | Right family anywhere in results (scams) |
| Faithfulness | n/a | Summary claims supported by the evidence shown to the LLM |
| Answer relevancy | n/a | Summary addresses "is this a scam?" |
| Context precision | n/a | Retrieved patterns are relevant, ranked first |
| Context recall | n/a | Retrieved patterns cover the reference (scams) |

| ID | Label | Action | Max sim % | Top category | Expected | Faith | Relev | Prec | Recall |
|---|---|---|---|---|---|---|---|---|---|
| S01 | SCAM | BLOCK | 91.01 | PHISHING | PHISHING, KYC_FRAUD | n/a | n/a | n/a | n/a |
| S02 | SCAM | BLOCK | 90.96 | PHISHING | PHISHING | n/a | n/a | n/a | n/a |
| S03 | SCAM | BLOCK | 85.46 | UPI_FRAUD | UPI_FRAUD | n/a | n/a | n/a | n/a |
| S04 | SCAM | BLOCK | 87.23 | UPI_FRAUD | UPI_FRAUD | n/a | n/a | n/a | n/a |
| S05 | SCAM | FLAG_VERIFICATION | 83.69 | LOAN_SCAM | LOAN_SCAM | n/a | n/a | n/a | n/a |
| S06 | SCAM | BLOCK | 96.72 | KYC_FRAUD | KYC_FRAUD | n/a | n/a | n/a | n/a |
| S07 | SCAM | BLOCK | 87.53 | LOTTERY_SCAM | LOTTERY_SCAM | n/a | n/a | n/a | n/a |
| S08 | SCAM | BLOCK | 90.16 | INVESTMENT_FRAUD | INVESTMENT_FRAUD | n/a | n/a | n/a | n/a |
| S09 | SCAM | BLOCK | 90.49 | IMPERSONATION | IMPERSONATION | n/a | n/a | n/a | n/a |
| S10 | SCAM | BLOCK | 90.66 | IMPERSONATION | IMPERSONATION | n/a | n/a | n/a | n/a |
| S11 | SCAM | BLOCK | 84.76 | VISHING | VISHING | n/a | n/a | n/a | n/a |
| S12 | SCAM | BLOCK | 85.71 | VISHING | VISHING | n/a | n/a | n/a | n/a |
| B01 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
| B02 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
| B03 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
| B04 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
| B05 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
| B06 | LEGIT | ALLOW | 0.0 | - | - | n/a | n/a | n/a | n/a |
