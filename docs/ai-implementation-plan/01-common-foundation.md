# 1단계 — AI 공통 기반과 Mock Provider

> 상태: **로컬 구현 및 전체 검증 완료 (2026-10-30)** · GitHub 이슈: AI-01, AI-02(기존 자산 정리) · 선행 단계: 없음 · 외부 토큰: 불필요

## 목표
기능 코드가 특정 LLM 업체의 API/SDK에 직접 의존하지 않도록 공통 입력·출력, 프롬프트, 대화, 오류, Mock 계약을 정한다. **실제 OpenAI/Claude 연결은 5단계**에서 한다.

## 현재 저장소에서 확인한 사실
- 별도 `ai/` Maven 모듈이 아니라 `be/` 단일 Spring Boot 프로젝트다. Java 기준은 17, Spring Boot 4.1.1, Spring AI 2.0.1 BOM 및 `spring-ai-model` 의존성이 이미 있다.
- `com.hongik.graduate.ai.experience`에 `ExperienceTag`, `ExperienceQuestionPolicy`, `ExperienceQuestionPolicyLoader`가 구현되어 있다.
- `be/src/main/resources/ai/prompts/experience/{values,personality,job,collaboration,challenge}.json` 질문 정책 5종과 테스트가 존재한다.
- `be/README.md`는 Spring AI `ChatModel`을 공통 계약으로 사용하고 별도 Provider 추상화를 두지 않는다고 명시한다. **이번 계획에서 요구한 `AiProvider`와 충돌하므로 구현 시 한 가지 공식 경계를 정하고 README/SKILL 문서를 함께 갱신**한다.
- 기존 API·인증·데이터 접근은 `com.hongik.graduate.BE` 아래에 있다. 패키지를 무리하게 전면 이동하지 않는다.

## 결정할 아키텍처
- 비즈니스 서비스가 보는 경계는 얇은 포트 `AiProvider` (`generate` / `chat`)로 정한다. `AiRequest`/`AiResponse`는 Spring AI나 업체 전용 타입을 외부에 노출하지 않는다.
- 1단계 구현은 **`MockAiProvider` 한 개만** 등록하며, Spring AI의 `ChatModel`/`PromptTemplate`는 후속 Provider 어댑터 내부에서만 사용한다. `spring-ai-model`을 제거하거나 기존 `SpringAiContractTest`를 깨지 않는다.
- `AiProvider`가 두 추상화를 중첩해 복잡해지지 않도록 책임을 제한한다. 추론 스트리밍, 툴 호출, 벡터 DB, 범용 워크플로 엔진은 만들지 않는다.
- 운영 환경은 Mock을 실제 AI처럼 오인하지 않도록 기능 플래그로 AI 생성 기능을 비활성화한다. Mock 응답에는 `provider=mock`/`isMock=true` 등 출처를 명시한다.
- 공통 계약에는 `taskType`, `promptVersion`, `context`, `outputSchemaId`, `correlationId`, `responseStatus`, `usage`(선택)을 포함한다. 원문 개인정보나 비밀 값은 로그에 기록하지 않는다.

## 제안 위치 (실제 파일명은 구현 시 확정)
```text
be/src/main/java/com/hongik/graduate/ai/
  common/
    AiProvider.java
    AiRequest.java
    AiResponse.java
    AiMessage.java
    AiContext.java
    AiTaskType.java
    AiProviderException.java
    MockAiProvider.java
  experience/                  # 현재 정책 파일 유지
  job/                         # 필요 시 3단계부터
  coverletter/                 # 필요 시 4단계부터
  interview/                   # 필요 시 4단계부터
be/src/main/resources/ai/prompts/
  experience/                  # 현재 JSON 정책 재사용
  job/ coverletter/ interview/ # 필요해질 때만 생성
be/src/test/java/com/hongik/graduate/ai/common/
```

## 구현 체크리스트
- [x] AI-01: `AiTaskType`, 역할(USER/ASSISTANT/SYSTEM), 요청/응답 레코드, 출력 스키마 식별자 및 검증 정의.
- [x] AI-01: `AiProvider.generate(request)`와 `AiProvider.chat(request)`의 차이·응답 계약 정의. 대화 이력은 순서가 보장된 메시지 목록으로 입력.
- [x] AI-01: 프롬프트 템플릿 이름·버전·필수 변수, 컨텍스트 구성 규칙(비어 있는 필드/변수 누락 오류) 정의.
- [x] AI-01: 결정적 `MockAiProvider` 구현. 작업 종류별 예제 JSON/텍스트 반환, 실패/타임아웃/잘못된 출력 시나리오 주입 가능.
- [x] AI-01: JSON 출력 검증 및 실패를 정상 응답과 구분하는 오류 타입 정의. 잘못된 JSON을 성공으로 포장하지 않기.
- [x] AI-02: 기존 `ExperienceQuestionPolicyLoader`가 이 계층에 무리 없이 연결되도록 사용 규칙 정리. **기존 정책 파일·테스트를 중복 구현하지 않음**.
- [x] 문서: `be/README.md`, 필요 시 `SKILL.md`의 Provider 정책을 실제 선택한 경계로 일치시키기.

## 계약 예시 (개념, 확정 DTO 아님)
```json
{
  "taskType": "EXPERIENCE_SUMMARY",
  "promptVersion": "experience-summary-v1",
  "context": { "experienceId": "uuid", "situation": "...", "action": "..." },
  "outputSchemaId": "experience-summary-v1"
}
```
응답 예시: `{ "status": "SUCCESS", "provider": "mock", "isMock": true, "output": {...} }`.
Mock은 실제 문장 생성 품질을 검증하는 도구가 아니라 계약·흐름 테스트용이다.

## 테스트 및 완료 기준 (DoD)
- [x] API Key, 인터넷, 외부 LLM 스타터 없이 `be/.\mvnw.cmd verify` 통과.
- [x] 동일 입력에 동일 Mock 출력, 다중 턴 대화 순서 유지, 빈/유효하지 않은 요청 거부.
- [x] 필수 프롬프트 변수가 빠지면 명확하게 실패하고 오류 메시지에 민감정보가 없다.
- [x] Spring 컨텍스트가 Mock 하나만 선택하고 Provider Bean 충돌이 없다.
- [x] 기존 `SpringAiContractTest` 및 `ExperienceQuestionPolicyLoaderTest`가 계속 통과한다.
- [x] 실제 LLM 연결 클래스/`ChatModel` Bean이 없음을 Spring 통합 테스트로 검증한다. 외부 서비스 호출은 Mock 코드에 존재하지 않는다.

## 제외 및 다음 단계
- 제외: API Key 주입, 원격 AI 호출, 사용자 대화 DB, AI 품질 평가/과금.
- 다음: [2단계 경험 데이터](./02-experience-foundation.md). 경험 정책의 **AI 태그 5개와 DB 경험 태그 6개(STAR 포함)**를 이 단계 또는 2단계 착수 시 명시적으로 매핑한다.

## 구현 결과 (2026-10-30, 로컬 검증)
- 신규 패키지: `be/src/main/java/com/hongik/graduate/ai/common/`.
- 계약: `AiRequest`, `AiResponse`, `AiMessage`, `AiContext`, `AiTaskType`, `AiUsage`, `AiProvider`, `AiProviderException`.
- 정책/검증: `AiPromptTemplate`, `AiPromptTemplateRegistry`, `AiOutputSchema`, `AiOutputValidator`.
- 어댑터/게이트: `MockAiProvider`, `AiGateway`, `app.ai.enabled=false` 및 `app.ai.provider=mock` (기본값).
- 프롬프트 파일: `be/src/main/resources/ai/prompts/common/generate-v1.txt`, `chat-v1.txt`.
- JSON 스키마: 지금은 `mock-result-v1` 예제 형식만 유효; 경험 요약/JD/자소서 전용 스키마는 2~4단계에서 정의한다.
- 예외 시나리오: 템플릿 누락, 변수 누락, 잘못된 JSON/필수 타입, provider 실패/타임아웃, 채팅 메시지 순서/역할, Context 순환/깊이 초과.
- 신규 테스트: `AiCommonContractTest`, `AiBeanConfigurationTest` (총 13 테스트). 기존 테스트 포함 전체 `be/.\mvnw.cmd -q verify` 성공 (Exit 0).
- **출시 범위:** 생성/채팅 HTTP API는 아직 없으며, 실제 AI 서비스 호출은 하지 않는다. 사용자 경험·정적 태그 질문 정책은 기존대로 동작한다.
- 변경 사항은 로컬 작업 트리에 있으며 Git 커밋/푸시는 별도 단계다.
