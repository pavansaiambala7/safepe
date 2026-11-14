# SafePe RAG evaluation (RAGAS)

Run: `full` at 2026-11-14 14:02 | App model: `gemini-2.5-flash` | Judge: `gemini-3.5-flash` + `models/gemini-embedding-001`


## Fraud-pattern RAG (`/fraud/agentic-analyze`)

18 cases: 12 scam paraphrases, 6 legitimate look-alikes.


| Metric | Score | What it measures |
|---|---|---|
| Verdict accuracy | 1.000 | Scam flagged / legit allowed (deterministic) |
| Scam detection rate (recall) | 1.000 | Share of scams flagged or blocked |
| False-positive rate | 0.000 | Share of legit messages wrongly flagged |
| Top-1 category hit | 1.000 | Best match is the right scam family (scams) |
| Any category hit | 1.000 | Right family anywhere in results (scams) |
| Faithfulness | 0.771 | Summary claims supported by the evidence shown to the LLM |
| Answer relevancy | 0.662 | Summary addresses "is this a scam?" |
| Context precision | 0.750 | Retrieved patterns are relevant, ranked first |
| Context recall | 0.000 | Retrieved patterns cover the reference (scams) |

| ID | Label | Action | Max sim % | Top category | Expected | Faith | Relev | Prec | Recall |
|---|---|---|---|---|---|---|---|---|---|
| S01 | SCAM | BLOCK | 91.01 | PHISHING | PHISHING, KYC_FRAUD | 1.000 | 0.637 | 0.000 | 0.000 |
| S02 | SCAM | BLOCK | 90.96 | PHISHING | PHISHING | 1.000 | 0.798 | 1.000 | 0.000 |
| S03 | SCAM | BLOCK | 85.46 | UPI_FRAUD | UPI_FRAUD | 0.333 | 0.639 | 1.000 | 0.000 |
| S04 | SCAM | BLOCK | 87.23 | UPI_FRAUD | UPI_FRAUD | 1.000 | 0.751 | 1.000 | 0.000 |
| S05 | SCAM | FLAG_VERIFICATION | 83.69 | LOAN_SCAM | LOAN_SCAM | 1.000 | 0.744 | 1.000 | 0.000 |
| S06 | SCAM | BLOCK | 96.72 | KYC_FRAUD | KYC_FRAUD | 1.000 | 0.561 | 0.000 | 0.000 |
| S07 | SCAM | BLOCK | 87.53 | LOTTERY_SCAM | LOTTERY_SCAM | 1.000 | 0.518 | 1.000 | 0.000 |
| S08 | SCAM | BLOCK | 90.16 | INVESTMENT_FRAUD | INVESTMENT_FRAUD | 1.000 | 0.548 | 0.000 | 0.000 |
| S09 | SCAM | BLOCK | 90.49 | IMPERSONATION | IMPERSONATION | 0.250 | 0.539 | 1.000 | 0.000 |
| S10 | SCAM | BLOCK | 90.66 | IMPERSONATION | IMPERSONATION | 1.000 | 0.711 | 1.000 | 0.000 |
| S11 | SCAM | BLOCK | 84.76 | VISHING | VISHING | 0.000 | 0.564 | 1.000 | 0.000 |
| S12 | SCAM | BLOCK | 85.71 | VISHING | VISHING | 1.000 | 0.574 | 1.000 | 0.000 |
| B01 | LEGIT | ALLOW | 0.0 | - | - | 0.800 | 0.792 | n/a | n/a |
| B02 | LEGIT | ALLOW | 0.0 | - | - | 1.000 | 0.550 | n/a | n/a |
| B03 | LEGIT | ALLOW | 0.0 | - | - | 0.000 | 0.770 | n/a | n/a |
| B04 | LEGIT | ALLOW | 0.0 | - | - | 0.750 | 0.794 | n/a | n/a |
| B05 | LEGIT | ALLOW | 0.0 | - | - | 1.000 | 0.707 | n/a | n/a |
| B06 | LEGIT | ALLOW | 0.0 | - | - | 0.750 | 0.720 | n/a | n/a |

## Money Assistant (`/assistant/chat`)

10 questions over 11 seeded transactions for `ragas_money_user`.


| Metric | Score | What it measures |
|---|---|---|
| Faithfulness | 0.725 | Answer claims supported by the transactions given |
| Answer relevancy | 0.940 | Answer addresses the question |
| Factual correctness | 0.610 | Answer matches the computed ground truth |
| Context recall | 1.000 | Needed facts were present in the context |

| # | Question | Faith | Relev | Factual | Response |
|---|---|---|---|---|---|
| 1 | What is my total spending? | 0.500 | 0.853 | 0.670 | Hi there! Happy to help you with that.  Your total spending so far is **₹23147.00**. |
| 2 | What was my largest transaction and who was it to? | 1.000 | 0.943 | 0.800 | Hi there! Happy to help you with that.  Your largest transaction was: *   **₹15,000.00** to `landlord@okicici` on November 1st. |
| 3 | How much did I spend on Swiggy in total? | 1.000 | 0.979 | 0.400 | Hi there! I can help you with that.  You've spent a total of **₹1380.00** on Swiggy across these transactions: *   ₹610.00 on 2026-11-03 *   ₹320.00 on 2026-11-… |
| 4 | How many transactions have I made? | 1.000 | 0.963 | 1.000 | Hi there! You've made **11 transactions** so far. 😊 |
| 5 | How much have I spent on Uber rides? | 1.000 | 0.947 | 0.500 | Hi there! Let's take a look.  You've spent a total of **₹550.00** on Uber rides from your recent transactions.  Here are the details: *   2026-11-03: ₹340.00 to… |
| 6 | Did I pay my electricity bill, and how much was it? | 0.500 | 0.934 | 0.400 | Hi there! Yes, you did pay your electricity bill.  *   You paid ₹1840.00 to BESCOM on November 1st. |
| 7 | What was my smallest transaction? | 1.000 | 1.000 | 0.000 | Hi there!  Your smallest transaction was **₹210.00** to uber@paytm on November 2, 2026. |
| 8 | How much did I spend on food delivery from Swiggy and Zomato combined? | 1.000 | 0.983 | 1.000 | Hey there! Let's check that for you.  You've spent a total of **₹1660.00** on food delivery from Swiggy and Zomato.  Here's the breakdown: *   **Swiggy:** ₹1380… |
| 9 | Did I make any payment to Netflix? | 0.250 | 0.924 | 1.000 | Hello there!  Looking at your recent transactions, I don't see any payments made to Netflix.  If you believe you made a payment to Netflix and it's not showing … |
| 10 | Based on my transactions, what is one way I could reduce my spending? | 0.000 | 0.876 | 0.330 | Hi there! I can definitely help you with that. Looking at your recent transactions, one area where you could potentially reduce spending is on **food delivery**… |

## Reviewer notes: reading the judge scores

Every answer above was also checked by hand against the seeded data and the
evidence the model was shown. Several judge scores disagree with that check:

- **All 10 Money Assistant answers are factually correct**, including the
  Netflix hallucination check (it correctly reports no Netflix payment).
  Factual correctness is low on Q3, Q5, Q6 and Q10 because RAGAS penalises
  correct extra detail (itemised orders) that the reference omits. Q7 scores
  0.000 although its answer matches the reference word for word: a judge error.
- **Fraud faithfulness:** S03, S09, S11 and B03 score low, but the S03 and S11
  summaries (checked) restate the classification and matched pattern almost
  verbatim. Judge noise, not hallucination.
- **Fraud context precision:** S01, S06 and S08 score 0.000 although each
  retrieved pattern is a near-paraphrase of the incoming scam; S02, an
  equivalent match, scores 1.000. The judge is inconsistent here. The
  deterministic top-1 category hit (12/12) is the more reliable retrieval signal.
- **Fraud context recall is 0.000 by construction, not a retrieval failure.**
  The references state verdicts and advice ("this is a scam", "do not click"),
  which a stored scam example can never contain. Rewrite the references to
  describe only what the matching pattern should contain before using this metric.
- Money Assistant faithfulness is pulled down by conversational filler ("I'm
  here to help") and by world knowledge the data does not state (Q6: BESCOM is
  an electricity company).
- The 0.75 similarity threshold was tuned on these same 18 messages, so the
  perfect verdict scores are optimistic; confirm on held-out messages.

Answers were collected on Nov 6-14 2026 over several free-tier quota windows
(gemini-2.5-flash allows 20 calls a day per project). Judging ran in a Linux
container (`JUDGE_ONLY=1`) because Windows Smart App Control blocked pyarrow.
