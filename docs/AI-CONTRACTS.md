# AI and extraction contracts

All content is synthetic. These are decision-support operations, not autonomous banking actions.

| Operation | Input / output | Tools | Timeout / retries | Model |
|---|---|---|---|---|
| `DOCUMENT_EXTRACTION`, `extract-fields-v1` | UTF-8 text up to 1 MiB → optional validated string fields `reference`, `applicant`, `amount`, `currency` | None | One storage request up to 30 s; three leased queue attempts, 15 s backoff; local extraction | `deterministic-rules-v1` |
| `POLICY_EVIDENCE_SELECTION`, `policy-evidence-v1` | Question up to 1,500 characters plus up to five retrieved chunks → up to three unique UUIDs from that exact set | None | Mock is local; optional live request has a 10 s timeout and zero application retries | Deterministic mock by default; explicit Spring AI live adapter optionally |

The final answer is rendered from stored excerpts. A model cannot invent an answer body or supply arbitrary quotations. Every returned citation is selected from authorized retrieved chunks; a database foreign key also prevents persisting a citation to an unstored chunk. Unsupported, suspicious, invalid and unavailable results abstain.

Retrieval uses PostgreSQL full-text search with simple tokenization and OR-ranked search terms. This is an extractive policy assistant, not an embeddings-based semantic search engine. No document is sent to a live LLM in mock mode. When the live provider is explicitly enabled, the question and retrieved policy excerpts are sent to the configured endpoint. Extraction remains local.

The instruction detector is a conservative heuristic. The checked-in attack suite is finite and does not establish universal prompt-injection immunity. The stronger boundaries are no callable tools, independent authorization, schema validation, an exact citation allowlist and server-rendered stored excerpts.

AI executions record operation, prompt/contract version, model, status, measured duration and token usage when the provider supplies it. Mock token usage is null, not estimated.

The optional live adapter implements Spring AI 2.0.2 `ChatModel` using an OpenAI-compatible JSON HTTP transport. It is isolated behind the `live-ai` Maven profile because provider dependencies could not be downloaded in the restricted implementation environment. Its source is provided, but its compilation and provider integration remain unverified here. CI explicitly compiles this profile when internet access is available.

Primary API/version references checked during implementation:

- https://docs.spring.io/spring-ai/reference/getting-started.html
- https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/chat/model/ChatModel.html
- https://docs.spring.io/spring-ai/docs/current/api/org/springframework/ai/chat/model/ChatResponse.html
- https://docs.spring.io/spring-ai/docs/2.0.x/api/org/springframework/ai/chat/prompt/Prompt.html
