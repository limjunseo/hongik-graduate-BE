# Repository Structure Guide

이 저장소는 Backend와 AI 기능을 Java/Spring 기반의 단일 애플리케이션으로 관리합니다.
도메인 책임은 Java 패키지와 리소스 경로로 분리합니다.

## Directory Structure

```text
.
├── be/
│   ├── pom.xml
│   ├── mvnw
│   ├── mvnw.cmd
│   ├── .mvn/
│   ├── config/
│   └── src/
│       ├── main/
│       │   ├── java/com/hongik/graduate/
│       │   └── resources/
│       └── test/java/com/hongik/graduate/
├── docs/
├── scripts/
├── .github/
├── .env.example
├── .gitignore
└── SKILL.md
```

## be/

Spring Boot 백엔드와 AI 도메인 코드를 함께 관리합니다.

`com.hongik.graduate.ai` 패키지는 Spring AI 기반 모델 연동과 AI 도메인
기능을 담당합니다. 정적 프롬프트 및 정책 파일은
`src/main/resources/ai/`에 둡니다.

실제 LLM Provider를 추가할 때는 Spring AI starter를 사용하고 Provider SDK
타입이 도메인 경계를 넘어가지 않도록 합니다. 정적 정책 조회에는
`ChatModel`이나 외부 API를 사용하지 않습니다.

현재 필요하지 않은 `controller`, `service`, `repository` 패키지는 미리 만들지
않고 기술과 도메인 요구사항이 확정된 뒤 필요한 구조만 추가합니다.

## docs/

API 명세, ERD, 아키텍처, 기능 명세, 개발 규칙 등의 문서를 관리합니다.

## scripts/

개발 환경 구성, 데이터 초기화, 배포 및 반복 작업 자동화 스크립트를 관리합니다.

## .github/

Pull Request Template, Issue Template, GitHub Actions, CODEOWNERS 등 GitHub 관련
설정을 관리합니다.

## Environment Variables

실제 비밀 값은 저장소에 커밋하지 않습니다. 필요한 환경변수 이름만
`.env.example`에 기록하고 실제 `.env` 파일은 `.gitignore`에서 제외합니다.

## Version Policy

Java, Spring Boot, Spring AI 또는 Maven 버전을 변경하기 전에 루트의
[`VERSION_GUIDE.md`](./VERSION_GUIDE.md)를 확인합니다. Java 컴파일 버전과 CI의
JDK 버전은 항상 함께 변경하고, Spring Boot parent와 Spring AI BOM의 호환 조합을
공식 문서에서 검증합니다. 프레임워크가 관리하는 전이 의존성 버전을 임의로
개별 고정하지 않습니다.

## Structure Principles

1. 현재 필요하지 않은 구현 패키지를 과도하게 미리 생성하지 않습니다.
2. Backend와 AI 코드의 책임을 Java 패키지 단위로 구분합니다.
3. 새로운 기능은 기존 디렉터리와 패키지의 책임에 맞게 추가합니다.
4. API Key, 비밀번호 등 비밀 값은 Git에 커밋하지 않습니다.
5. 빌드와 테스트는 `be/mvnw` Maven Wrapper를 기준으로 실행합니다.
6. 프로젝트 구조가 크게 변경되면 이 문서를 함께 수정합니다.
