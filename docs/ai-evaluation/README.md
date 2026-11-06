# Measured mock evaluation

This report covers the local deterministic unit run, not a live LLM evaluation.

| Check | Measured result | Required threshold | Scope |
|---|---|---|---|
| Extraction field accuracy | 20/20 checks, 1.00 | ≥ 0.90 | Five synthetic documents, four fields each; correct absence counts as a field check |
| Instruction detector suite | 9/9 attacks flagged, 1.00 | 1.00 | Finite versioned attack set |
| Citation validator cases | 4/4 expected outcomes, 1.00 | 1.00 | Allowlisted, empty, fabricated and duplicate IDs against an in-memory candidate set |

The dataset files and thresholds are in `evaluation/`. `AiEvaluationTest` reads them, enforces the thresholds and writes `backend/target/ai-evaluation/results.json`. The corresponding measured local result is included alongside this report.

Storage-backed citation resolution, department isolation, independent review, job fencing, retry limits and signed MinIO round trips are covered by integration-test source. Those tests compiled but did not execute in this environment: Docker/socket access is unavailable. No measured 100% database-backed or live-provider result is claimed.

The full `mvn verify` attempt failed on environment prerequisites: five integration suites could not obtain PostgreSQL, and the MinIO test could not access Docker. The ten runnable unit tests passed with zero failures and zero skips.

No optional live-model evaluation was performed. The live Maven profile could not resolve its uncached Spring AI dependency because Maven Central DNS/network access is blocked. CI compiles that source separately; a real provider check requires explicit credentials and separate reporting.
