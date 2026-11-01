# Backend

Backend와 AI 기능을 함께 실행하는 Java 17 / Spring Boot 애플리케이션입니다.

## 로컬 DB: 서버 설치가 필요 없는 파일형 H2

- 별도 MySQL/PostgreSQL/SQLite 서버, Docker 컨테이너, DB 계정 생성이 **필요하지 않습니다**.
- 기본 로컬 프로필은 `jdbc:h2:file:./data/hongik-graduate`를 사용합니다.
- `be/` 폴더에서 실행하면 `be/data/hongik-graduate.mv.db` 파일에 데이터가 영구 저장됩니다.
- 서버를 다시 시작해도 저장된 데이터가 유지됩니다. 테이블은 로컬 Hibernate 설정으로 자동 생성/업데이트됩니다.
- `be/data/`는 Git에서 제외되므로 팀원마다 **서로 다른 로컬 DB**를 사용합니다.
- 저장 위치를 변경할 때만 `be/.env-local`에 `LOCAL_DB_FILE=./data/my-database`를 설정합니다.
- 파일 백업은 백엔드를 종료한 뒤 FE 저장소의 `backup-local-db.ps1`을 사용하세요.
- 레포 폴더를 지우면 로컬 DB도 지워집니다. 중요한 데이터는 따로 백업하세요.

현재 졸업 프로젝트 범위에서는 별도의 운영 RDB 서버나 DB 배포를 계획하지 않습니다. 여러 팀원의 **같은 데이터 실시간 공유**는 로컬 파일 DB만으로는 제공되지 않습니다.

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
기능 활성화 여부를 검사합니다. 실제 OpenAI 연결은 Spring AI `ChatModel`을
`SpringAiProviderAdapter` 내부에서 사용하며 업체 SDK 타입을 도메인에 노출하지 않습니다.

- 기본 설정: `app.ai.enabled=false`, `app.ai.provider=mock`. 키가 없으면
  원격 API가 호출되지 않으며 `AiGateway`가 AI 기능을 차단합니다.
- `be/.env-local`에 `AI_API_KEY`만 넣으면 OpenAI 어댑터가 자동 선택됩니다.
  `AI_ENABLED` 또는 `AI_PROVIDER`를 명시하면 그 설정이 우선합니다.
- Mock만 사용하면 응답에 `provider=mock`, `isMock=true`, `[MOCK ONLY]`가 나타나며
  실제 글이나 조언을 생성하지 않습니다.
- 설정법·신규 API는 `../docs/AI_READY_GUIDE.md`에서 확인합니다.
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

## Experience Foundation (2단계)

AI 없이 작동하는 구조화된 경험 데이터 기반을 제공합니다.

- 경험 태그: DB의 STAR 포함 6종을 기존 AI 질문 정책 5종과 명시적으로 연결합니다.
  `GET /api/experience-tags/{tagCode}/ai-questions` (STAR는 고정 STAR 질문).
- 공통 구조: `ExperienceSummary` v1 (S/T/A/R, role/conflict/learned,
  skills[], keywords[], evidenceSources[]). 사용자 확인 입력만 저장하며,
  기존 `experiences.summary` 자유 텍스트는 변경하지 않습니다.
- 저장: `experience_summaries` (experience_id PK, snapshot_json, origin, schemaVersion).
  경험 삭제 시 함께 삭제. **운영 DB에는 별도 스키마 마이그레이션 필요**.
- `GET /api/experiences/{id}/structured-summary/preview-from-answers`:
  기존 빠른 작성의 **명확히 대응되는 질문 답변만** 같은 Summary v1로 투영합니다.
  애매한 결과/배운 점은 임의로 분해하지 않고 비워 둡니다. 이 조회는 저장하지 않습니다.
- `PUT/GET /api/experiences/{id}/structured-summary`: 본인 경험만 저장/조회.
  미등록 조회는 404. 서버가 제목·태그·origin=USER를 확정합니다.
- `GET /api/experiences/{id}/completeness`: 태그별 필수 필드
  존재 여부만 평가(0~100%, 누락·다음 정적 질문). 자유 텍스트를 자동 해석하지 않습니다.
- `GET /api/skills`: `resources/ai/taxonomy/skills-v1.json` 역량 11종,
  한글 별칭→영문 code 정규화/중복 제거; Java/Spring은 역량이 아닌 키워드로 저장.
- `GET /api/imports/{id}/experience-candidates`: 소유 PDF 또는 URL의 문단을
  원문 그대로 `sourceId/location/quote`와 `REVIEW_REQUIRED` 상태로 반환.
  PDF는 PDFBox로 텍스트 추출(60페이지/총 6만자 제한), 이미지 PDF는 `NO_TEXT`,
  OCR 및 비공개 Notion OAuth는 지원하지 않습니다. 원본 저장 파일은 유지됩니다.
- 경험 요약 evidenceSources는 소유한 자료의 추출 결과와 **위치·인용 일치**가
  검증되어야 저장 가능합니다. 근거 없이 STAR 항목을 자동 생성하지 않습니다.
- 기존 빠른 작성, 경험 CRUD, 가져오기 PDF/URL API의 경로·응답 형태는 보존합니다.

테스트: `ExperienceFoundationUnitTest`, `ExperienceFoundationIntegrationTest`,
`ImportExperienceCandidateServiceTest`. 전체 `.\mvnw.cmd verify` 실행.
실제 LLM 토큰, 원격 모델, UI 자동 적용은 필요하지 않습니다.

## JD 분석 계약 · 경험 매칭 (3단계)

- 기존 `job_postings` 원문 저장과 별개로 `job_analyses`에 사용자 입력의 구조화된 분석(v1)을 저장합니다.
- `PUT/GET /api/cover-letters/{coverId}/posting/analysis`: JWT로 소유권 확인, 역량 taxonomy 검증,
  JD 원문에 대한 정확한 offset/quote 근거 검증. 아직 LLM이 JD를 자동 분석하지 않습니다.
- `GET /api/cover-letters/{coverId}/experience-recommendations`: 본인 구조화 경험의 필수 역량
  교집합 / JD 필수 역량 개수 × 100 (반올림). 빈 필수 역량은 `NOT_SCORABLE`.
  규칙 기반 결과이며 합격 확률 또는 의미적 유사도 점수가 아닙니다.
- `GET /api/cover-letter-question-types`: 자소서 문항 유형 8종 및 추천 역량 정책(v1) 조회.
  문항 자동 분류는 하지 않습니다.
- 원문 공고 수정·삭제, 자소서 삭제 시 저장된 분석 결과를 무효화/삭제합니다.
- `CompanyAnalysis`/`RoleAnalysis`는 출처/스키마 계약만 정의하며 실제 기업 자료 수집은 5단계입니다.
- 운영 환경은 `job_analyses` DB 마이그레이션이 별도로 필요합니다.

## 자소서·면접 AI Context/Pipeline (4단계)

- `CoverLetterContextBuilder`: JWT 회원 소유 자소서/문항 + 기존 연결 관계로 확인된 경험만 구조화 Context에 포함합니다.
  JD 분석이 없으면 `JD_NOT_ANALYZED`, 선택 경험이 없으면 `MISSING_EXPERIENCE` 경고를 기록합니다.
  권한 없는 회원/연결되지 않은 경험은 차단합니다.
- `CoverLetterRequestFactory`: 자소서 초안, 첨삭(6종), 지원 전략, 첨삭 대화용 `AiRequest`를 준비합니다.
  `SupportStrategyContextBuilder`는 역량 근거의 강점·부족 정보를 제공합니다.
- `InterviewRequestFactory`: 질문 7종과 답변 피드백 요청 Context 및 v1 JSON 스키마를 구성합니다.
  현재 텍스트 기반 인터뷰 세션 API·대화 기록 저장은 없습니다.
- `ai/prompts/coverletter/`와 `ai/prompts/interview/`에 버전된 템플릿을 유지하며,
  `AiPromptTemplateRegistry` allowlist, `AiOutputValidator` 필수 타입 검사, `MockAiProvider` 예제 출력을 사용합니다.
- **AI 글은 생성하지 않습니다.** Mock 응답에는 `isMock=true`/MOCK ONLY가 표시되고,
  초안·첨삭은 기존 `answerText`나 버전을 수정하지 않습니다. 실제 LLM 연결 및 저장 승인 흐름은 5단계입니다.
- 테스트: `CoverLetterPipelineUnitTest`, `CoverLetterPipelineIntegrationTest`.
  전체 `.\\mvnw.cmd verify`는 API Key 없이 통과하도록 설계했습니다.

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
