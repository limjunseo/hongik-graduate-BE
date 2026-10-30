# Backend

Backend와 AI 기능을 함께 실행하는 Java 17 / Spring Boot 애플리케이션입니다.

## 구조

```text
be/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── src/
    ├── main/
    │   ├── java/com/hongik/graduate/
    │   │   ├── GraduateApplication.java
    │   │   ├── ai/common/        # Provider port, Mock, validation, feature gate
    │   │   └── ai/experience/
    │   └── resources/
    │       └── ai/prompts/experience/
    └── test/java/com/hongik/graduate/
```

## AI 공통 기반 (1단계)

도메인 로직은 `com.hongik.graduate.ai.common.AiProvider` 포트의
`generate(AiRequest)` / `chat(AiRequest)`를 사용합니다. `AiGateway`가
기능 활성화 여부를 검사합니다. Spring AI `ChatModel`은 나중에
실제 Provider 어댑터 내부에서 사용하며 업체 SDK 타입을 도메인에 노출하지 않습니다.

- 기본 설정: `app.ai.enabled=false`, `app.ai.provider=mock`
  (환경변수 `AI_ENABLED`, `AI_PROVIDER`로 제어).
- 실제 LLM 스타터/API Key는 없습니다. 현재 `MockAiProvider`만 Bean으로 등록됩니다.
  `AiGateway`가 꺼져 있으면 generate/chat 호출을 차단합니다.
- 개발 목적으로 `AI_ENABLED=true`로 설정하더라도 응답에
  `provider=mock`, `isMock=true` 및 `[MOCK ONLY]` 문구가 들어가며
  실제 글이나 조언을 생성하지 않습니다.
- 정적 템플릿: `resources/ai/prompts/common/{generate,chat}-v1.txt`,
  템플릿 변수는 `${instruction}`입니다. 템플릿 이름·버전은 allowlist로 검사합니다.
- `outputSchemaId=null`은 비어 있지 않은 일반 텍스트,
  `mock-result-v1`은 JSON 객체의 필수 필드/타입 검증입니다.
  추후 경험/JD/자소서별 실제 JSON 스키마는 해당 단계에서 추가합니다.
- 오류는 `AiProviderException.Code`로 구분하며 실패를 정상 응답으로 위장하지 않습니다.
- 채팅 히스토리는 SYSTEM 메시지 입력이 금지되고 USER 메시지로 끝나야 합니다.
  개인 데이터/프롬프트/대화 원문을 별도 로그에 출력하지 않습니다.

테스트: `AiCommonContractTest`, `AiBeanConfigurationTest`.
기존 `SpringAiContractTest`는 Spring AI 타입의 호환성을 확인하는 테스트로 유지합니다.

## Experience Question Policy

경험 질문 정책은 LLM과 무관한 정적 도메인 정책입니다. Spring Bean으로
등록된 `ExperienceQuestionPolicyLoader`를 주입받아 사용합니다.

```java
ExperienceQuestionPolicy policy =
        policyLoader.getExperienceQuestionPolicy(ExperienceTag.JOB);

for (String question : policy.questions()) {
    System.out.println(question);
}
```

정책 원본은 `src/main/resources/ai/prompts/experience/*.json`이며 로더는
classpath 기준으로 파일을 찾습니다. 태그, 필수 필드, 문자열 타입, 빈 질문,
중복 질문을 검증하고 질문 목록은 수정 불가능한 `List`로 반환합니다.

정책 조회는 `ChatModel`을 호출하지 않으며 LLM 토큰을 사용하지 않습니다.

## 실행 및 검증

```bash
./mvnw spring-boot:run
./mvnw verify
```

Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
.\mvnw.cmd verify
```
