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


## 2026-11-01 졸프 요청 범위 추가 구현 (기존 2026-10-31 스냅샷 이후)

- **토큰 준비**: `AiKeyAutoConfiguration`이 `AI_API_KEY`만 설정된 경우 실제 OpenAI Provider/ChatModel을 선택합니다. 명시적 `AI_ENABLED` / `AI_PROVIDER`는 우선합니다. 가짜 키로 Bean 설정 확인 테스트만 수행하며 실제 유료 요청은 하지 않습니다.
- **경험 고도화**: `POST /api/experiences/{id}/ai-chat/next-question`이 구조화 요약, completeness, 최근 대화에 기반해 미완성 필드 하나에 대한 다음 질문을 생성합니다. 자동 저장하지 않습니다.
- **JD/기업/직무**: `POST /api/cover-letters/{coverId}/posting/role-analyze`(소유 JD 기반)와 `POST /api/cover-letters/{coverId}/company/analyze`(사용자가 제공한 URL과 본문 기반, 서버 원격 수집 없음)을 추가했습니다. 모델 출력의 원문 offset/quote를 검사하고 결과는 미리보기로 반환합니다. 기업 출처 URL의 진위나 최신성을 검증하지 않습니다.
- **지원 전략**: `POST /api/cover-letters/{coverId}/questions/{questionId}/strategy`의 구조화 AI 응답과 스키마·검증을 추가했습니다.
- **문항 자동 분류**: `POST /api/cover-letters/{coverId}/questions/{questionId}/analyze`를 추가했습니다. 문항 유형·의도·정확한 문항 인용 및 기존 유형별 역량 정책을 미리보기로 반환합니다.
- **문항별 첨삭 대화**: `GET/POST /api/cover-letters/{coverId}/questions/{questionId}/ai-chat/turns` 및 `cover_letter_ai_edit_turns` 테이블을 추가했습니다. JWT 소유권·선택 경험 재검증, requestId 멱등성, 글자 수 경고, 원본 answerText 비자동저장, 질문 삭제 시 기록 제거를 포함합니다.
- **가이드**: `docs/AI_READY_GUIDE.md`에 키만 넣는 방법과 요청 예시, 제외 범위를 정리했습니다.
- **요청에 따라 제외**: 운영용 요청 횟수/비용 쿼터/서킷브레이커, 면접 세션/피드백 워크플로우. 기존 인터뷰 계약은 변경하지 않았습니다.
- **한계**: 실제 OpenAI 토큰으로 모델 품질·스키마 순응도 E2E 확인 전입니다. AI 요약/초안은 미승인 상태이고 사용자가 검증해야 합니다. 운영 DB에는 신규 첨삭 대화 테이블 스키마 적용이 필요합니다. 본 문서의 앞부분에 적힌 2026-10-31 미구현 항목 중 위에서 구현된 항목은 이 최신 절을 우선합니다.
