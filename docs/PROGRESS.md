# BankOps AI — implementation status

## Current verification — 6 November 2026, 16:14 Europe/Vienna

The requested local verification is complete. These measured results supersede the historical infrastructure blockers retained below. This remains a synthetic-data decision-support prototype, not a production-ready or legally compliant system.

### Done

- Resolved the Compose conflict with sandboxing_go-postgres-1: BankOps now publishes PostgreSQL on 5434; the sandbox keeps 127.0.0.1:5433. Updated the Compose default, .env.example, local .env port setting and backend local JDBC default together. Existing pgdata/miniodata and sandbox volumes were preserved; no down -v, pruning or database reset was used. Both projects remain running.
- Applied compatible npm security fixes without --force or --legacy-peer-deps. npm audit fix initially left vulnerable Vite because the locked Angular/compiler peer tree prevented an in-place update. Re-resolved the existing package.json ranges with npm install after preserving the prior lockfile and node_modules in /tmp. package.json is unchanged. The new lockfile resolves Angular framework/compiler 22.2.4, build/CLI 22.2.6 and Vite 8.3.5. Final npm audit: **0 vulnerabilities**.
- Fixed unit translation warnings caused by missing work.* navigation keys in the shell test fixture, with an assertion against future missing-translation warnings. Final unit log contains no missing-translation warnings.
- Fixed runtime translations: owner-only JSON assets copied into the nginx image returned HTTP 403. The frontend image now grants read/traverse permissions to built public assets. Browser regression checks verify both language JSON responses, English heading rendering and German navigation after switching language.
- Corrected Playwright language selectors from button to radio, matching the Angular Material accessibility roles.

### Commands and actual results

| Command | Result |
|---|---|
| backend: mvn -B clean verify | PASS — 49 tests, 0 failures, 0 errors, 0 skips; packaged JAR; PostgreSQL 16 and MinIO integration tests executed with Docker |
| backend: mvn -B -P live-ai -DskipTests compile | PASS — 58 source files compiled against Spring AI 2.0.2; deprecated API warning remains |
| frontend: npm audit | PASS — 0 vulnerabilities |
| frontend: npm run build | PASS — production initial bundle 1.18 MB raw; no budget warnings |
| frontend: npm test -- --watch=false --browsers=ChromeHeadless --reporters=progress | PASS — 24 tests; no missing-translation warnings |
| docker compose config --quiet | PASS |
| docker compose up -d --build | PASS — BankOps services started; backend, frontend, PostgreSQL and Keycloak healthy; MinIO running |
| frontend: npm run e2e | PASS — 2 Chromium tests in 13.9 seconds against Compose |

The browser workflows exercised Keycloak login, German/English UI, synthetic case creation/submission and independent supervisor closure, MinIO upload and processing, stored policy evidence, AML simulation/investigation and activity browsing. The first browser attempt exposed the stale selectors and unreadable translation assets; both issues were resolved before the successful rerun. The initial Maven verification and unit run also passed (49 and 24 tests respectively), although that unit run emitted the translation warnings fixed above.

Final command logs are local /tmp/bankops-maven-final.log, /tmp/bankops-live-final.log, /tmp/bankops-npm-audit-final.log, /tmp/bankops-build.log, /tmp/bankops-unit-final.log, /tmp/bankops-compose-final.log and /tmp/bankops-e2e-final.log. Targeted pre-edit backups are in /tmp/bankops-fix-backup; the intermediate dependency tree/lock are also retained in /tmp. These temporary files are not durable CI artifacts.

### Files changed in this follow-up

- docker-compose.yml, .env.example and local .env: BankOps PostgreSQL port.
- backend/src/main/resources/application.yml: matching local JDBC port.
- frontend/package-lock.json: compatible security dependency resolution.
- frontend/Dockerfile: readable built static assets for nginx workers.
- frontend/src/app/core/layout/app-shell.spec.ts: complete navigation translations and warning regression assertion.
- frontend/e2e/auth.spec.ts and frontend/e2e/workspaces.spec.ts: language radio selectors; auth test also verifies language assets/rendering.
- docs/PROGRESS.md: this measured report.

### In progress, blockers and next steps

No requested verification remains in progress or blocked. No commit or push was made; this directory has no Git metadata. Existing source changes were preserved with targeted edits. Browser tests added synthetic workflow records to the existing BankOps database and object storage.

Live-AI verification here is compilation only: no external model was called, and provider behavior/quality remains unqualified. The Spring AI adapter emits a deprecation warning. Existing deployment limits (including MinIO image pinning, credential separation, orphan-object reconciliation and production hardening) remain future work. npm audit reports known advisories at this run, not a general security guarantee. No provider upload configuration or authorization policy was relaxed.

## Historical implementation and verification notes

The notes below describe earlier runs and their then-current blockers; they are retained for continuity and are superseded by the current verification above.

## Completed source implementation

The requested missing workflows now have backend services, migrations, APIs and Angular screens: operational/compliance cases, case history/notes, independent human review, an actual review queue, MinIO-backed documents and a PostgreSQL processing queue, deterministic extraction, cited policy evidence/abstention, synthetic AML simulations with source transactions and investigations, audit browsing and execution history. Dashboard availability flags and measured counts are wired to stored data.

The fixed Java/Angular/PostgreSQL/Keycloak/MinIO architecture is retained. Shared caller context and workspace errors live in common to avoid capability import cycles; static source inspection found no capability import cycles. No extra microservice is introduced. Optional live selection is behind an explicit Spring AI Maven profile; the deterministic default needs no provider dependency or key.

## Phase status

| Phase | Source work | Verification |
|---|---|---|
| Foundation repairs | Applied in the inspected original before this implementation | Prior builds/unit checks passed; full runtime blocked |
| Cases/reviews/documents | Implemented, including department grants and source storage | Default Java package and Angular build pass; integration source compiles; offline SQL assertions pass |
| Policy evidence/evaluation | Implemented mock pipeline, contracts, citation validation, thresholds and live-adapter source | Deterministic evaluation passes; storage-backed and live checks blocked |
| AML/activity | Implemented persisted synthetic transactions, rules, evidence, case review and history UI | Rule boundary tests pass; HTTP/browser workflows blocked |

The user explicitly requested the remaining implementation across phases. The runtime blockers are recorded here; a build or unexecuted test is not represented as a verified complete deployment.

## Commands and results

- `mvn -B -o -Dtest=AiEvaluationTest,AmlRulesTest,JwtClaimsTest,SecurityConfigTest package`: passed; ten unit tests, zero failures, zero skips; default JAR packaged.
- `npm run build`: passed; production bundle about 1.18 MB raw. Existing dashboard SCSS warning: 34 bytes above warning budget.
- `tsc -p tsconfig.spec.json --noEmit`: passed.
- `npm test -- --watch=false --browsers=ChromeHeadless --reporters=progress`: test bundles compile; Karma cannot bind port 9876 (`EPERM`).
- `npm run e2e -- --list`: two tests discovered. Browser workflow was not executed.
- `docker compose config --quiet` using placeholder passwords: passed.
- Fresh standalone PostgreSQL 18.6: V1–V3 migrations and seven SQL assertion groups passed (queue claim, search, supervisory grants, scope filtering, citation resolution, rejection of fabricated citation and audit TRUNCATE protection). PostgreSQL 16 HTTP integration is still required.
- `mvn -B -o verify`: failed on unavailable Docker/test database. Five integration classes failed their prerequisite checks; the MinIO Docker prerequisite also failed. The ten runnable unit tests passed.
- `mvn -B -o -P live-ai -DskipTests package`: blocked because Spring AI 2.0.2 is not cached; online resolution also failed on Maven Central DNS restrictions. Live source compilation and provider behavior remain unverified.

## Test implementation

`WorkspaceIntegrationTest` includes anonymous/wrong-role/authorized checks for all fifteen new endpoints, legitimate-role escalation checks, department isolation, independent/versioned review, stored citation resolution, injection exclusion, job lease fencing, bounded retries and upload validation. `MinioStorageIntegrationTest` checks signed private-object upload/download/delete. These are executable tests, but were not run successfully here because their infrastructure is unavailable. Scheduling is disabled in integration contexts to avoid background workers racing tests.

`AiEvaluationTest` enforces versioned datasets and thresholds in `evaluation/`. Measured results are in `docs/ai-evaluation/`. GitHub Actions also starts Compose and runs the existing login smoke test and the new multi-role workspace workflow.

## Verification handoff

The implementation described above is present in this source tree. The earlier `bankops-implementation` handoff was historical; do not reapply an old bundle solely because of that note.

Run the CI-equivalent checks with Docker access before marking the prototype verified. Production readiness remains outside this synthetic-data project scope.

## Follow-up verification — 6 November 2026

- `mvn -B -o verify`: failed. Six integration prerequisite checks failed because Docker is unavailable and no external test database is configured. Runnable unit tests passed; this is not a successful backend verification.
- `mvn -B -o -P live-ai -DskipTests compile`: blocked by uncached Spring AI 2.0.2 dependency. This does not establish an incompatible API or a successful compile.
- Angular production build passed. Duplicate grid styles were consolidated to address the stylesheet warning without changing the configured budget.
- Karma test bundles compile, but execution is blocked by `listen EPERM` on port 9876.
- Live MinIO, browser workflows, and authorization/citation runtime evidence remain pending. No measured production-security claims are made.

## Remaining product limits

UTF-8 text documents and labeled extraction fields are supported. PDF/OCR, semantic embeddings, real banking integrations, autonomous consequential actions and production hardening are outside this implemented synthetic prototype. MinIO image pinning, credential separation, orphan-object reconciliation, live-provider qualification and infrastructure verification remain deployment work. See README for details.
