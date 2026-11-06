# BankOps AI

A synthetic-data portfolio application for internal bank operations, compliance policy evidence and AML investigation. It assists employees and records human review. It is not a production banking system or a claim of legal compliance.

## Implemented workflows

- Cases: create, list, inspect history, add notes, submit for review, independently close or return. Version checks reject stale updates.
- Review queue: explicit department grants, recorded reviewer/reason and separation from the creator/latest submitter.
- Documents: private MinIO storage, authenticated downloads, checksums, UTF-8 text validation, queued processing, validated field extraction, visible processing states and blocked-content handling.
- Policy evidence: PostgreSQL full-text retrieval, deterministic mock selection, stored chunk citations, exact stored quotations and abstention. Optional explicitly configured Spring AI live selection adapter.
- AML: persisted synthetic simulations and transactions, deterministic explainable rules, investigation cases with transaction evidence, notes and human review. No account or transaction action.
- Audit/activity: append-only events, per-user execution history, global auditor/admin access and measured dashboard counts.
- English/German Angular screens, reactive forms, pagination, busy/error states and role-aware navigation.

## Start locally

Docker Desktop must be running. Preserve your existing `.env`. For a new installation only, copy `.env.example` to `.env` and set the infrastructure passwords.

```bash
docker compose up --build --wait
```

Open http://localhost:4200. Follow [the demo guide](docs/DEMO.md), using the sample documents in `samples/`.

The backend receives MinIO credentials through Compose. For a native backend start, export the relevant environment variables yourself; Spring does not automatically read a root `.env` file. Blank `AI_PROVIDER` in an existing Compose environment falls back to `mock`.

## Verify

```bash
cd backend
mvn -B verify
```

This requires Docker for PostgreSQL 16/pgvector and signed MinIO round-trip integration tests. Missing Docker fails verification instead of silently skipping tests. To use an isolated external PostgreSQL test database, set `TEST_DATABASE_URL`, `TEST_DATABASE_USER` and `TEST_DATABASE_PASSWORD`; MinIO verification still requires Docker. Never point the test suite at a database with real data.

```bash
cd frontend
npm ci
npm run build
npm test -- --watch=false --browsers=ChromeHeadless --reporters=progress
npx playwright install chromium
npm run e2e
```

E2E requires the running stack. GitHub Actions includes backend integration tests, mock evaluation, frontend build/unit tests, live-adapter compilation and a Compose-backed Playwright workflow.

## Optional live evidence selection

Default mock mode uses no keys or external model calls. The Spring AI adapter is opt-in and was not compiled or provider-tested in the restricted implementation environment.

```bash
cd backend
mvn -B -P live-ai -DskipTests package
AI_PROVIDER=openai-compatible AI_BASE_URL=https://YOUR_PROVIDER/v1 AI_API_KEY=YOUR_KEY AI_MODEL=YOUR_MODEL java -jar target/bankops-backend-0.1.0-SNAPSHOT.jar
```

Also provide the normal database, OIDC and storage environment variables for a native start. The provider must support OpenAI-compatible chat completions with JSON output. Only the question and retrieved policy excerpts are submitted; extraction remains local. See [AI contracts](docs/AI-CONTRACTS.md).

## API and access

All application endpoints are under `/api/v1` and require an application role. Swagger/OpenAPI remains available at `/swagger-ui.html`.

| Routes | Access |
|---|---|
| `GET /cases`, `GET /cases/{id}` | Application roles within department/review grants; admin/auditor global |
| `POST /cases` | Admin, Operations, Compliance; compliance cases require Admin/Compliance |
| `POST /cases/{id}/notes`, `/submit` | Authorized case workers; supervisor may add notes but does not submit |
| `POST /cases/{id}/reviews` | Independent Admin/Supervisor within scope |
| `GET /documents`, `GET /documents/{id}/download` | Scoped case documents; policies are internally shared |
| `POST /documents` | Case writers; policy uploads require Admin/Compliance |
| `POST /policies/questions` | Application roles; only ready, safe policy chunks are retrieved |
| `GET /aml/alerts`, `GET /aml/cases/{caseId}/transactions` | Admin/AML/Supervisor/Auditor, with case scope |
| `POST /aml/simulations` | Admin/AML |
| `GET /audit/events` | Admin/Auditor |
| `GET /ai/executions` | Own executions; Admin/Auditor global |

## Verification status and limits

The default backend packaged successfully and ten local unit tests passed. The Angular production build passed, TypeScript unit sources compiled, and two Playwright tests were discovered. Migration SQL plus seven assertion groups passed using standalone PostgreSQL 18.6. This is not a substitute for the PostgreSQL 16 integration run.

Full integration, MinIO, browser execution, real login and the optional live adapter remain unverified here because Docker/network sockets and uncached Maven dependencies are blocked. The dashboard stylesheet now builds within its existing budget after duplicate grid rules were consolidated. See [progress](docs/PROGRESS.md) and [measured mock evaluation](docs/ai-evaluation/README.md).

Supported documents are UTF-8 `.txt` files up to 1 MiB. PDF, image OCR, embeddings, real bank connectors, external regulatory reporting and autonomous banking actions are not implemented. Extraction accepts labeled fields; it is not a general-purpose document-understanding model. Instruction detection is heuristic; no universal security guarantee is made.

MinIO bucket creation is lazy and objects are private. Database rollback cleanup for an uploaded object is best-effort; operators must reconcile orphan object keys after storage failures. The demo uses development Keycloak, seeded accounts, infrastructure credentials and database-owner privileges. A database owner can disable audit triggers. The MinIO image remains the project's existing `latest` image and must be verified/pinned for any controlled deployment.

For repeatable local verification with Docker available, run `bash scripts/verify-local.sh`. Start the development Compose stack and install Playwright Chromium before the browser check. The script preserves service data and reports blocked checks as unsuccessful.
