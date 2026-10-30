# Phase 5 implementation status (2026-10-31)

This document records **implemented slices**, not completion of every item in the phase-5 plan.
The baseline 1–4 phase contracts and existing CRUD endpoints remain in place.

## Implemented

- **5A / AI-13 (initial):** Spring AI OpenAI ChatModel adapter under the existing `AiProvider` interface. The default is `AI_ENABLED=false`, `AI_PROVIDER=mock`, with no remote model bean. Opt-in requires `AI_ENABLED=true`, `AI_PROVIDER=openai` and `AI_API_KEY`. Request/response length bounds, chat turn limit, JSON schema validation, token usage when available, provider failure/429/timeout handling, correlation-ID-only logs without raw user content. No silent mock fallback.
- **5B (initial):** Authenticated experience chat, stored by experience with USER/ASSISTANT roles, request-ID deduplication, paging, and deletion alongside the owning experience. Summary generation produces a **preview only** with canonical skills and explicit user-confirmation warnings. User-confirmed saving still uses the existing structured-summary PUT.
- **5C (initial):** JD `/posting/analyze` calls the provider, checks extracted schema and verbatim JD evidence offsets, and saves validated real analysis as `DRAFT`. Mock responses are returned as empty mock previews without persistence. Existing JD CRUD/manual analysis/experience ranking remain unchanged.
- **5D (initial):** Cover-letter question `/draft` and `/revise` endpoints use already-authorized context and existing prompt/request contracts. The generated text is **never automatically saved**; callers must use the existing answer PATCH after review. Character-limit warnings, provenance IDs and mock metadata are included.

## API endpoints (authenticated unless otherwise noted)

| Method | Path | Behavior |
|---|---|---|
| POST | `/api/experiences/{id}/ai-chat/messages` | Send `{"message":"...", "requestId":"optional-idempotency-key"}` |
| GET | `/api/experiences/{id}/ai-chat/messages?page=0&size=40` | Page through conversation history |
| POST | `/api/experiences/{id}/ai-summary:generate` | Produce unverified structured summary preview; never persist |
| POST | `/api/cover-letters/{coverId}/posting/analyze` | Analyze owned JD and persist validated real DRAFT; never persist mock |
| POST | `/api/cover-letters/{coverId}/questions/{questionId}/draft` | Body `{"questionType":"MOTIVATION","experienceIds":["..."]}`; preview |
| POST | `/api/cover-letters/{coverId}/questions/{questionId}/revise` | Body `{"questionType":"MOTIVATION","experienceIds":["..."],"originalText":"...","revisionType":"CONCISE","instruction":"..."}`; preview |

All responses use the existing `ApiResponse` wrapper. The caller must submit JWT bearer authorization.
The `requestId` is not an API authentication credential.

## Configuration

Copy variable names from the root `.env.example`. Never commit provider secrets.
Only configure `AI_PROVIDER=openai` when ready to make billable remote calls.
The default uses a deterministic `MockAiProvider` and the `AiGateway` stays disabled.
Configure `AI_MODEL`, `AI_TIMEOUT`, `AI_MAX_RETRIES`,
`AI_MAX_INPUT_CHARS`, `AI_MAX_OUTPUT_CHARS`, `AI_MAX_OUTPUT_TOKENS`,
and `AI_MAX_CHAT_MESSAGES` to limit model usage.
Run `cd be && .\mvnw.cmd verify` without API keys for offline unit/integration tests.

## Still outstanding

- **5A production readiness:** per-member rate/cost quotas, production-grade retry/circuit breakers, observability and model-API live tests with real keys; exact error handling is SDK-dependent. No paid API test is performed by CI.
- **5B:** multi-stage adaptive experience questioning and semantic evidence validation. AI-generated text is not factually guaranteed by schema checks.
- **5C:** trusted external company research, role/company analysis expansion, enhanced evidence matching beyond the existing rule-based system.
- **5D:** persistent per-question editing conversation threads and question auto-classification; preview response does not imply a saved answer.
- **5E / AI-20:** persistent interview sessions, turn-by-turn question/answer/feedback APIs and their own integration tests. Interview prompt/context/request contracts exist from phase 4, but this beta service is not implemented here.
- Production DB migration strategy for the new `experience_ai_chat_messages` table and concurrency controls for parallel messages per experience. Local integration tests use the application's configured test schema.


## AI internal review fixes (2026-10-31)

- The OpenAI SDK's own 429 error type is now mapped to `RATE_LIMITED` rather than the generic provider failure; non-timeout connectivity errors are no longer falsely labeled timeouts.
- The shared response validator rejects JSON output whose `schemaVersion` disagrees with the declared versioned schema.
- The JD analysis prompt now contains the actual canonical skill taxonomy. Any nonempty AI-extracted facts require at least one JD evidence span; the pre-existing JD range/substring validator checks each supplied citation before storage. The service checks that an existing user-confirmed analysis has not been replaced both before the remote request and before save.
- AI experience chat acquires a database pessimistic lock on the owning experience before checking request idempotency and appending its USER/ASSISTANT pair, serializing concurrent sends for one experience. The lock is held during the remote call, so model latency can cause database contention under load.
- AI experience summarization accepts the conflict/learned fields as legitimate source facts when no other quick-answer fields exist.

**Verification:** Java 17 Maven verify; dedicated tests of JSON schema versions, SDK rate-limit error mapping, evidence handling, confirmed draft protection, and concurrent H2 chat writes.

**Remaining limitations:** No paid live model tests; AI output evidence does not establish semantic truth; provider-wide and per-user quotas are not implemented; confirmed-JD protection should ultimately be made atomic against concurrent manual edits; long-running AI calls currently hold experience row locks. The interview session/feedback application workflow remains out of scope.
