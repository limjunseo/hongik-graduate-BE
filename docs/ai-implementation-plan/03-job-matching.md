# 3단계 — JD 분석 계약, 경험 매칭, 자소서 문항 매핑

> 상태: **로컬 구현·검증 완료 (2026-10-30)** · GitHub 이슈: AI-06, AI-07, AI-08, AI-09 · 선행: [2단계](./02-experience-foundation.md) · 외부 토큰: 불필요

## 목표
원문 JD의 **분석 결과 형식**을 먼저 고정하고, 사용자 경험의 구조화된 역량과 JD 요구 역량을 규칙 기반으로 매칭해 자소서에 사용할 경험을 추천한다. 자유 서술 JD 자동 해석·기업 정보 생성은 5단계로 미룬다.

## 기존 데이터/API와 연결
- `JobPosting`은 `coverLetterId`당 1개이며 `url`, `jdOriginal`, `qualifications`, `preferences`, `hiringProcess`를 보관한다.
- 기존 `GET/PUT/DELETE /api/cover-letters/{coverId}/posting`은 **저장/조회** API이지 AI 분석 API가 아니다. 유지한다.
- 자소서 문항과 경험의 연결 API `/api/cover-letters/{coverId}/questions/{questionId}/experiences`는 이미 존재한다. AI 추천 결과는 곧바로 자동 연결하지 않는다.
- 기존 경험은 `com.hongik.graduate.BE.experience`와 소유권을 통해 조회하고, 2단계의 `ExperienceSummary.skills[]`를 이용한다.

## JobAnalysis 결과 스키마 (제안)
```text
JobAnalysis
  jobPostingId: UUID?
  company: String?
  position: String?
  requiredSkills: SkillCode[]
  preferredSkills: SkillCode[]
  responsibilities: String[]
  coreCompetencies: SkillCode[]
  keywords: String[]
  companyValues: String[]
  evidence: EvidenceRef[]
  analysisStatus: NOT_ANALYZED | DRAFT | CONFIRMED
  schemaVersion: String
```
- `companyValues`/`evidence`는 데이터 출처가 있을 때만 채운다. 공고에 없는 기업 문화나 업무 조건을 임의 생성하지 않는다.
- 3단계에서는 API 사용자가 구조화된 필드를 입력하거나 테스트 fixture로 전달한다. 텍스트 `jdOriginal`에서 skill을 **자동으로 알아냈다고 표시하지 않는다**.
- 기존 `job_postings` 원문 컬럼을 변경하지 않고 분석 결과는 별도 저장 레코드(예: `job_analyses`, `job_posting_id` FK, 분석 버전)로 관리하는 것을 우선 제안한다.
- 기업 분석(`CompanyAnalysis`) 및 직무 분석(`RoleAnalysis`)도 보고서 `summary`, `sourceUrl`, `retrievedAt`, `evidence[]`, `skills[]` 계약을 정의하되 실제 웹 수집/요약은 5단계.

## 규칙 기반 매칭 v1
```text
ExperienceSummary.skills[] + JobAnalysis.requiredSkills[]
  → SkillCode 정규화 → 교집합 → 점수/근거/부족한 역량 → 정렬
```
- 기본 점수: `score = 100 × |경험 ∩ JD 필수 역량| / |JD 필수 역량|` (반올림 규칙 고정). 예: 필수 3개 중 2개 일치 → 약 67점.
- `requiredSkills`가 비어 있으면 0점 단정 대신 `NOT_SCORABLE`로 반환. 우대 역량 가중치는 v2 정책으로 분리하고 결과의 `scoringVersion`을 표시한다.
- 결과: `experienceId`, `score`(0~100, nullable), `status`, `matchedSkills[]`, `missingSkills[]`, `reasonCodes[]`, `scoringVersion`.
- 추천 이유는 "역량 코드 일치"라는 사실만 반환한다. 의미 유사도, 역량 수준, 합격 확률은 규칙 기반 MVP에서 평가하지 않는다.
- 사용자 소유 경험만 랭킹에 포함하며 동일 점수는 안정적 기준(경험 갱신 시각 + ID 등)으로 정렬한다.

## 자소서 문항 유형
| QuestionType | 표시명 | 관련 SkillCode 예시 |
| --- | --- | --- |
| MOTIVATION | 지원동기 | initiative, responsibility |
| JOB_COMPETENCY | 직무역량 | technical_skill, backend, problem_solving |
| COLLABORATION | 협업 | collaboration, communication, conflict_resolution |
| PROBLEM_SOLVING | 문제해결 | problem_solving, analytical_thinking, initiative |
| CHALLENGE | 도전 | adaptability, initiative |
| STRENGTH_WEAKNESS | 성격의 장단점 | responsibility, communication |
| GROWTH | 성장과정 | adaptability, responsibility |
| FUTURE_PLAN | 입사 후 포부 | initiative, technical_skill |
- 위 표는 **초기 정책 제안**이다. 문항이 어떤 유형인지 AI가 자동 분류하는 것은 5단계이며, 현재는 FE/서버에서 명시한 `QuestionType`을 입력받는다.
- 타입과 역량 간 관계는 코드 switch문에 분산하지 않고 버전 있는 JSON/리소스(예: `ai/prompts/coverletter/question-skill-map.json`)에서 읽는다.

## 구현 체크리스트
- [x] AI-06: `JobAnalysis`, `CompanyAnalysis`, `RoleAnalysis` 데이터 계약과 상태·근거·버전 검증.
- [x] AI-06: 기존 `JobPosting`의 URL/원문/필수·우대사항과 분석 결과를 분리·연결(저장 스키마/조회 DTO).
- [x] AI-07: `ExperienceMatcher`, `ExperienceMatchResult`, 필수 역량 coverage 점수, 동일 점수 정렬, 결과 설명 코드.
- [x] AI-07: 인증된 사용자의 소유 경험만 조회. 결과 추천을 실제 문항에 연결하려면 기존 연결 API를 사용하고 사용자 선택을 요구.
- [x] AI-08: `CoverLetterQuestionType` enum 및 값 검증.
- [x] AI-09: 타입↔역량 정책/로더, 다중 권장 역량, 정규화 및 매핑 누락 처리.
- [x] API(제안): `PUT/GET /api/cover-letters/{coverId}/posting/analysis` (수동 구조화 등록/조회), `GET /api/cover-letters/{coverId}/experience-recommendations`, `GET /api/cover-letter-question-types`. 실제 경로는 기존 OpenAPI와 겹침 검사 후 결정.
- [x] Swagger 응답에 `scoringVersion`, '규칙 기반/비AI 분석' 여부 명시.

## 테스트 및 완료 기준 (DoD)
- [x] 3개 JD 필수 skill 중 2개 매칭 → 약 67점, 일치/누락 코드 정확.
- [x] JD 필수 skill 0개, 경험 skill 0개, 중복/별칭/알 수 없는 코드, 순위 동점 처리.
- [x] 문항 유형별 역량 매핑 테스트 및 잘못된 유형 입력 거부.
- [x] 다른 사용자 JD/경험을 읽거나 추천받지 못함; 기존 자소서·공고·경험 연결 API 회귀 테스트.
- [x] `be/.\mvnw.cmd verify` 통과, LLM/외부 검색 호출 0회.

## 제외 및 다음 단계
- 제외: JD URL 크롤링/자연어 분석(AI-16), 임베딩 매칭, 기업 최신 정보 수집, 자동 문항 분석(AI-17).
- 다음: [4단계 자소서·면접 Context](./04-coverletter-interview-contracts.md).

## 구현 결과 (2026-10-30)

- `JobAnalysis` v1 및 `CompanyAnalysis`/`RoleAnalysis` 구조화 계약 정의. 기업·직무의 원문 자동 분석 및 최신 정보 수집은 5단계까지 수행하지 않는다.
- `job_analyses` 테이블은 기존 `job_postings`와 별개로 저장한다. ID는 `job_posting_id`이고 JSON/버전/상태/수정시각을 보관한다. 운영 DB 마이그레이션은 별도 작업.
- 수동 입력 JD의 역량은 2단계 `SkillTaxonomy`를 통해 검증·정규화한다. 인용 `evidence`는 `jdOriginal` 원문과 문자열 범위가 정확히 일치해야 저장 가능하다.
- 기존 JD 원문/자격요건/우대사항 변경 시 이전 분석을 무효화하며 채용공고 또는 지원건 삭제 시 분석도 삭제한다. 자동 분석을 한 것으로 표시하지 않는다.
- `ExperienceMatcher` v1: 필수 역량 coverage ×100을 사사오입, 우대 역량 가중치 없음. 필수 역량 없으면 `NOT_SCORABLE`·점수 null, 역량 겹침/누락 및 `scoringVersion=v1`을 반환한다. 정렬은 점수 내림차순 + 기존 소유 경험 목록 순서(갱신시각 기반)로 안정화.
- `QuestionSkillPolicy` v1: 8가지 문항 유형↔역량 매핑 JSON. 문항 자동 분류는 5단계에서 수행한다.
- 신규 API: `PUT/GET /api/cover-letters/{coverId}/posting/analysis`, `GET /api/cover-letters/{coverId}/experience-recommendations`, `GET /api/cover-letter-question-types`. JWT 소유권 검사 및 기존 수동 API 독립성 유지.
- 테스트: `JobFoundationUnitTest`, `JobFoundationIntegrationTest`. 전체 `be/.\\mvnw.cmd -q verify` 수행, 실패 여부는 커밋 전 검증 기록 참고.
- 범위 제외: JD 자연어 분석·기업 웹 수집·임베딩/시맨틱 매칭·합격 확률 예측.
