# hongik-graduate-BE

홍익대학교 졸업 프로젝트 백엔드 저장소입니다.

Backend와 AI 기능을 하나의 Java 17 / Spring Boot 애플리케이션으로 관리합니다.
AI 공통 계약은 Spring AI를 사용하고, 경험 질문 정책은 외부 모델 호출이 없는
정적 도메인 정책으로 구현되어 있습니다.

## Windows 팀원 원클릭 실행

CMD에서 **명령어 한 줄**로 Git·Node.js·JDK 자동 확인/설치부터 FE·BE 복제 및 실행까지 진행합니다.

[한 줄 설치 명령어 및 제한사항 → GETTING_STARTED.md](./GETTING_STARTED.md)

별도 MySQL·PostgreSQL 서버는 필요 없으며 H2 파일 DB가 자동으로 초기화됩니다.

## 기술 스택

- Java 17
- Spring Boot 4.1.1
- Spring AI 2.0.1
- Maven Wrapper
- JUnit 6 / AssertJ

## 버전 정책

현재 조합은 Java 17, Spring Boot 4.1.1, Spring AI 2.0.1입니다. Java 17은
최신 Spring과의 호환성 한계가 아니라 프로젝트의 최소 컴파일·CI 기준입니다.
Spring Boot 4.1.1은 Java 17부터 26까지 지원합니다.

버전 호환 범위, Java LTS 변경 방법과 Spring 업그레이드 검증 절차는
[`VERSION_GUIDE.md`](./VERSION_GUIDE.md)를 따릅니다.

## 데이터 저장

팀별로 별도 RDB 서버를 구축하지 않습니다. Spring Boot가 내장형 H2 파일 DB를 사용해
`be/data/hongik-graduate.mv.db`에 저장합니다. 로컬 실행 후 재시작해도 유지되고,
Git에 올라가지 않습니다. 중요한 데이터는 BE 실행을 종료한 후 FE의 백업 스크립트로
별도 보관하세요. 자세한 내용은 [`be/README.md`](./be/README.md)를 참고하세요.

## 검증

저장소 루트에서 다음 명령을 실행합니다.

```bash
cd be
./mvnw verify
```

Windows PowerShell에서는 `.\mvnw.cmd verify`를 사용합니다.

자세한 디렉터리 사용 원칙은 [`SKILL.md`](./SKILL.md)를 참고하세요.

## AI 기능 (모의면접/운영 쿼터 제외)

경험 채팅·추가 질문·요약, JD/기업/직무 분석, 지원 전략,
자기소개서 문항 자동 분류·초안·첨삭·문항별 첨삭 대화를 지원합니다.
`be/.env-local`에 `AI_API_KEY`만 넣으면 OpenAI Provider를 자동으로 활성화합니다.
실행과 API 입력/주의사항은 [`docs/AI_READY_GUIDE.md`](./docs/AI_READY_GUIDE.md)를 참고하세요.
실제 유료 API 호출을 수반하는 검증은 키를 넣은 이후에만 가능합니다.
