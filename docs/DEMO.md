# Run the complete synthetic workflow

Start the Compose stack from the project root. Default addresses are frontend `http://localhost:4200`, API `http://localhost:8080`, Keycloak `http://localhost:8180`, and MinIO console `http://localhost:9001`.

The imported demo accounts use `demo1234` unless you customized the realm. They are synthetic local-demo credentials only.

1. Sign in as `ops@bankops.local`. Open Cases, enter a title, description and `SYN-...` reference, confirm synthetic data, and create the case.
2. Open the case, follow Documents, and upload `samples/operations-request.txt`. Confirm synthetic data. The page refreshes queued/processing rows automatically; wait for Ready and inspect the extracted fields.
3. Return through Cases, open the case, enter a submission summary and submit for review.
4. Sign in as `supervisor@bankops.local` in a separate private browser window. Open Awaiting human review. The Risk supervisor has explicit grants for Operations, Compliance and AML. Open the case and record a reason to close or return it. The creator and latest submitter cannot review their own case.
5. Sign in as `compliance@bankops.local`. Open Documents, choose Policy evidence, upload `samples/synthetic-policy.txt`, confirm synthetic data and wait for Ready.
6. Open Policy evidence and ask `independent review`. The answer includes stored excerpts and chunk IDs. Download the cited source to inspect it. An unrelated question abstains.
7. Sign in as `aml@bankops.local`. Open AML investigations and generate a synthetic simulation. The application stores eight artificial transactions and creates two rule-based review cases. Open a case to inspect its source transactions, add notes and submit it to the Risk supervisor.
8. Sign in as `auditor@bankops.local`. Open Audit & AI activity to inspect persisted events and executions. Other roles can see their own execution records but cannot access the global audit endpoint.

The optional Playwright test performs the admin/supervisor case flow, document ingestion, policy evidence retrieval and AML simulation. It requires the actual stack; listing the tests is not an executed E2E result.
