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
    │   │   └── ai/experience/
    │   └── resources/
    │       └── ai/prompts/experience/
    └── test/java/com/hongik/graduate/
```

## Spring AI 공통 계약

별도 Provider 추상화를 만들지 않고 Spring AI의 `ChatModel`, `Prompt`,
`PromptTemplate`, `ChatResponse`를 사용합니다. 현재는 실제 모델 starter를
추가하지 않았으므로 애플리케이션 기동이나 테스트에 API Key가 필요하지
않습니다.

향후 실제 Provider를 연결할 때 필요한 Spring AI starter와 설정만 추가합니다.

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
