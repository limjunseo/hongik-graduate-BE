# hongik-graduate-BE

홍익대학교 졸업 프로젝트 백엔드 저장소입니다.

Backend와 AI 기능을 하나의 Java 17 / Spring Boot 애플리케이션으로 관리합니다.
AI 공통 계약은 Spring AI를 사용하고, 경험 질문 정책은 외부 모델 호출이 없는
정적 도메인 정책으로 구현되어 있습니다.

## 기술 스택

- Java 17
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Maven Wrapper
- JUnit 6 / AssertJ

## 검증

저장소 루트에서 다음 명령을 실행합니다.

```bash
cd be
./mvnw verify
```

Windows PowerShell에서는 `.\mvnw.cmd verify`를 사용합니다.

자세한 디렉터리 사용 원칙은 [`SKILL.md`](./SKILL.md)를 참고하세요.
