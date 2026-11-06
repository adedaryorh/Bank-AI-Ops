# BankOps AI: Agent Rules

## Purpose

BankOps AI is a portfolio project for Forward Deployed Engineer and AI Engineer roles at Austrian
and European banks. It is a decision-support platform for bank employees. It uses synthetic data
only and is never presented as production-ready or legally compliant.

## Non-negotiable safety rules

1. The system never approves or rejects loans or customers, freezes accounts, blocks transactions,
   reports anyone to authorities or assigns guilt. Consequential actions need an authorized human
   decision that is recorded.
2. AI output is untrusted until validated. Use structured output with schema validation. Deterministic
   rules stay authoritative wherever a rule exists.
3. Never fabricate results. Every citation must point to a stored knowledge chunk ID, and a test must
   enforce this. Every metric in docs or UI must come from a measured run, otherwise label it simulated.
4. Defend against prompt injection from uploaded documents and retrieved text. AI tools are allowlisted
   and each tool checks the caller's authorization on its own.
5. No uploaded document goes to an external LLM provider unless that provider is explicitly configured
   in the environment.
6. Never commit secrets. Use .env.example with placeholders.

## Fixed stack (do not substitute)

Backend: Java 21, Spring Boot 4.0.x or 4.1.x, Spring Web, Security, OAuth2 Resource Server, Data JPA,
Validation, Actuator, Spring AI 2.0.x, Flyway, springdoc OpenAPI, Maven.
Frontend: current stable Angular, TypeScript strict mode, standalone components, Signals, Angular
Material, SCSS, Reactive Forms, Transloco (German and English), Chart.js.
Data and infrastructure: PostgreSQL 16 with pgvector, Keycloak (OIDC), MinIO from the first phase that
stores files, Docker Compose. RabbitMQ only in the hardening phase. Until then use a Postgres job table
with FOR UPDATE SKIP LOCKED.
Testing: JUnit 5, Mockito, Spring Boot Test, Testcontainers, Playwright. CI: GitHub Actions.
No Python, LangChain or extra microservices.
Before pinning versions, check Maven Central or start.spring.io, and verify that every Spring AI API you
use exists in the selected release.

## Architecture rules

- Modular monolith. Backend packages are organized by business capability under com.bankops, with
  explicit interfaces between modules and no circular dependencies.
- Controllers stay thin and business logic lives in services. Never expose JPA entities. Use DTOs and
  server-side validation.
- Flyway for every schema change. Create only the tables the current phase needs. Use UUIDs for
  externally exposed IDs.
- REST under /api/v1 with ProblemDetail errors, pagination, correlation IDs and OpenAPI documentation.

## Security rules

- Keycloak OIDC with authorization code flow and PKCE. Keep tokens in memory only and never in local
  storage.
- Roles: ADMIN, OPERATIONS_OFFICER, COMPLIANCE_ANALYST, AML_ANALYST, SUPERVISOR, AUDITOR. Enforce roles
  and resource-level access in the backend. Frontend checks are for usability only.
- Audit events are append-only. Mask sensitive data in logs.
- Every protected endpoint needs tests for an unauthenticated call, a wrong-role call and a right-role call.

## AI rules

- The default profile uses a deterministic mock LLM, so the app and CI run without API keys. A live
  provider is enabled only through environment variables.
- Version every prompt. Record each AI execution with prompt version, model, duration and token usage
  when available.
- Each AI operation defines its purpose, allowed tools, input and output schema, timeout and retry policy.

## Evaluation rules

- Datasets and thresholds live in evaluation/, and CI fails below threshold.
- Starting targets (change only with a documented reason): every citation resolves to a stored chunk
  100 percent of the time, the prompt-injection suite passes 100 percent, every protected endpoint has
  an authorization test, extraction field accuracy is at least 0.90 on the synthetic set.
- Automated CI uses the mock LLM. Live-model evaluation is a separate optional job.
- Report measured results in docs/ai-evaluation, including failures.

## Working agreement

- Inspect the repository first and do not overwrite existing work without a reason.
- Work on the current phase only and make small commits.
- Run builds and tests before claiming anything works, and state what you could not verify.
- Keep docs/PROGRESS.md updated with done, in progress, blockers and next steps, so a new session can
  resume.
- Never describe a feature as complete if it is a stub or a TODO.
- After each phase report files changed, commands run, test results, limitations and security concerns.
  Do not start the next phase until the current one is verified or its blockers are documented.
