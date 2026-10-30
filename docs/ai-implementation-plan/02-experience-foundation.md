# 2단계 — 경험 정리 데이터·완성도·역량 사전

> 상태: **로컬 2단계 구현 및 전체 테스트 통과 (2026-10-30)** · GitHub 이슈: AI-02, AI-03, AI-04, AI-05 + 경험 가져오기 확장 · 선행: [1단계](./01-common-foundation.md) · 외부 토큰: 불필요

## 목표
빠른 작성, 대화, PDF/URL 가져오기 등 입력 방식에 관계없이 동일한 `ExperienceSummary`를 만들고 검증할 기반을 마련한다. **단순한 규칙 검사는 하되 LLM 없는 단계에서 자유 서술의 의미를 이해했다고 주장하지 않는다.**

## 재사용할 기존 백엔드
- 경험 CRUD·태그·질문·답변 API: `/api/experience-tags`, `/api/experience-tags/{code}/questions`, `/api/experiences`, `/api/experiences/{id}/answers`.
- `experiences`: `id/member_id/tag_code/title/creation_method/status/summary`, 질문과 답변 테이블은 이미 존재한다.
- 기존 DB 태그 6종: `STAR`, `GROWTH_VALUES`, `STRENGTH_WEAKNESS`, `COLLABORATION`, `ACADEMIC_CAREER`, `CHALLENGE`.
- 기존 AI 질문 정책 5종: `values`, `personality`, `job`, `collaboration`, `challenge`. **DB 코드를 AI 코드에 그대로 캐스팅하지 말고 명시적 매핑**한다. `STAR`는 범용 정책을 정의하거나 별도 처리한다.
- `/api/imports/pdf`는 원본 저장까지만, `/api/imports/url`은 공개 HTML 텍스트 수집까지 구현되어 있다. 비공개 Notion OAuth는 없다. 근거: `docs/import-api.md`.

## 데이터 계약 (제안)
```text
ExperienceSummary
  experienceId: UUID
  title: String
  tagCode: String
  situation: String?
  task: String?
  action: String?
  result: String?
  role: String?
  conflict: String?
  learned: String?
  skills: SkillCode[]
  keywords: String[]
  evidenceSources: SourceRef[]
  schemaVersion: String
```
- 협업 `conflict` 등은 선택 필드로 두고 공통 STAR 4요소를 기준으로 재사용한다. 필드를 요구하지 않는 태그는 completeness 정책에서 제외할 수 있다.
- 저장 방식은 기존 `experiences.summary` 텍스트를 **그대로 유지**하고, 정규화 요약을 경험당 1개인 별도 `experience_summaries` 테이블(JSON 컬럼/텍스트 매핑 포함)로 확장하는 안을 우선 검토한다. `experienceId` 유니크, 사용자 소유권은 `experiences.member_id`를 통해 확인한다.
- 요약 원본은 사용자 작성 vs Mock vs 실제 AI, 사용한 `sourceId`, `schemaVersion`을 구분한다. 자동 결과를 원본 경험에 덮어쓰지 않는다.

## 규칙 기반 완성도 검증
- `CompletenessResult`: `requiredFields[]`, `presentFields[]`, `missingFields[]`, `completionPercent`, `nextRecommendedField`.
- 판단 기준은 구조화 필드의 비어 있지 않음/최소 길이 등 **명시적 규칙**뿐. '프로젝트에서 협업했습니다' 한 문장만으로 `action`/`result`를 추론해서 채우지 않는다.
- 태그별 요구 필드를 설정으로 분리한다. 예: 공통 STAR는 S/T/A/R, 협업은 역할/갈등/기여를 추가 점검. 선택 필드 누락은 오류와 구분한다.
- 누락 필드에 대한 다음 질문은 1단계의 정적 질문 정책·필드별 질문으로 반환한다. 자유 생성은 5단계.

## 경험 역량 Taxonomy
| code | 표시값 | 예시 별칭 |
| --- | --- | --- |
| problem_solving | 문제 해결 | 문제해결 |
| communication | 커뮤니케이션 | 의사소통 |
| collaboration | 협업 | 팀워크 |
| leadership | 리더십 | 팀 리딩 |
| initiative | 주도성 | 자발성 |
| responsibility | 책임감 | 오너십 |
| technical_skill | 기술 역량 | 기술 활용 |
| analytical_thinking | 분석적 사고 | 분석력 |
| adaptability | 적응력 | 변화 대응 |
| conflict_resolution | 갈등 해결 | 갈등 조율 |
| backend | 백엔드 개발 | 서버 개발 |
- `SkillCode` 식별자는 영문 안정값, 표시명·별칭은 변경 가능하게 둔다. 역량과 기술 키워드(Java/Spring)를 혼동하지 않도록 `skills[]`와 `keywords[]`를 분리한다.
- 사전 외 자유 키워드는 keywords에 보존하지만 매칭 점수 계산에는 정규화된 skill code만 사용한다.

## 작업 체크리스트
- [x] AI-02: DB 6종 ↔ AI 질문 정책 5종 매핑 표와 STAR 처리 추가; 중복 질문 데이터/출처를 정리하되 기존 API 호환성 유지.
- [x] AI-03: `ExperienceSummary` DTO/스키마·검증·저장/조회 로직 도입; 기존 `summary` 필드와 마이그레이션 정책 확정.
- [x] AI-04: `ExperienceCompletenessEvaluator` 구현. 태그별 필수·선택 필드·누락 질문·상태를 순수 함수로 검사.
- [x] AI-05: `SkillTaxonomy`, 별칭 정규화, 중복 제거, 알 수 없는 code 처리, 사전 버전 관리.
- [x] 자료: `ImportedSource`의 PDF 텍스트 추출(예: PDFBox 도입 검토) 및 기존 URL `collectedText` 재사용. 공개 Notion 페이지는 **공개 URL**로만 수집하며 비공개 Notion 연동은 범위 밖.
- [x] 자료: `ImportExperienceCandidate`는 sourceId, 원문 문단/위치, 인용 근거, 추출 상태 등을 보유. 무근거 STAR 자동 완성은 하지 않고 사용자가 확인 후 경험으로 저장.
- [x] API(제안): `GET/PUT /api/experiences/{id}/structured-summary`, `GET /api/experiences/{id}/completeness`, `GET /api/skills`. 기존 `/api/experiences`와 충돌하지 않게 상세 설계 후 추가.
- [x] 권한: 요약 및 임포트 자료는 로그인한 회원 소유만 조회·연결·저장 가능.

## 테스트 및 완료 기준 (DoD)
- [x] 6개 DB 태그에서 올바른 질문 정책/STAR 처리 보장; 알 수 없는 태그는 명시적 오류.
- [x] S만 있는 경험은 T/A/R 누락으로 판정; 태그별 선택 요소를 필수로 오인하지 않음.
- [x] 입력 방식이 달라도 동일 `ExperienceSummary` 구조 반환; 스키마 버전/기존 텍스트와 호환.
- [x] 역량 별칭/중복/미등록 code 테스트 추가. 빈 JD 처리는 3단계 JD 매칭 테스트에서 검증한다.
- [x] PDF 텍스트 추출은 텍스트 PDF만 보장하며 이미지 PDF(OCR)는 별도 범위로 표시. 실패한 PDF는 원본을 보존하고 오류 상태 명시.
- [x] 다른 회원의 경험·가져온 파일에 접근 불가, 기존 경험 CRUD·임포트 API 회귀 테스트 통과.
- [x] `be/.\mvnw.cmd verify` 통과, 외부 LLM 호출 0회.

## 제외 및 다음 단계
- 제외: 자유 대화 기반 요약 생성(AI-14/15), 의미 기반 역량 추출, 비공개 Notion OAuth/OCR.
- 다음: [3단계 JD 분석 계약 및 매칭](./03-job-matching.md).

## 구현 결과 (2026-10-30, 로컬 검증)
- DB 경험 태그 6종과 기존 AI 정적 질문 정책 5종을 `ExperienceTagPolicyResolver`로 연결하며 STAR는 범용 4문항을 별도 제공한다. 기존 질문 정책/빠른 작성 API는 변경하지 않았다.
- `ExperienceSummary` v1 + `experience_summaries` 별도 테이블(경험 ID 기본키/JSON 스냅샷/버전/출처)을 사용한다. 기존 `experiences.summary` 텍스트는 보존하며 경험 삭제 시 별도 요약도 삭제한다.
- `ExperienceQuickAnswerPreviewService`를 추가하여 기존 빠른 작성 질문별 답변을 구조화 요약 형태로 **조회 전용 투영**한다. 모호한 항목은 임의 추론하지 않으며 실제 저장에는 사용자의 명시적인 PUT 호출이 필요하다.
- `ExperienceCompletenessEvaluator`는 태그별 필수 필드의 존재 여부만 평가한다(필수/입력/누락/완성률/다음 질문). STAR의 Situation만 채운 경우 Task/Action/Result 누락 및 25%를 반환한다.
- `SkillTaxonomy` v1은 역량 11종(정식 코드/한국어 레이블/별칭)을 JSON으로 관리하며 별칭 정규화, 중복 제거, 미등록 역량 거부를 수행한다. Java/Spring 같은 기술 키워드는 별도 `keywords`에 담는다.
- 기존 PDF 업로드/URL 수집을 재사용한다. PDFBox 3.0.5로 텍스트 PDF의 페이지별 문장을, URL의 수집 본문에서 문단 후보를 원문 그대로 제시한다. 후보에는 소스 ID·위치·인용·`REVIEW_REQUIRED`를 포함한다. 이미지 PDF는 `NO_TEXT`; OCR/비공개 Notion OAuth/의미 기반 STAR 자동 정리는 제외다.
- 자료 후보 인용을 구조화 요약에 넣을 때 같은 회원이 소유한 소스인지, 소스 내 실제 위치·인용과 정확하게 일치하는지 검사한다. 다른 회원의 경험/소스는 조회 및 저장이 불가하다.
- 신규 API: `GET /api/skills`, `GET /api/experience-tags/{tagCode}/ai-questions`, `GET/PUT /api/experiences/{id}/structured-summary`, `GET /api/experiences/{id}/structured-summary/preview-from-answers`, `GET /api/experiences/{id}/completeness`, `GET /api/imports/{id}/experience-candidates`.
- 신규 테스트: `ExperienceFoundationUnitTest`, `ExperienceFoundationIntegrationTest`, `ImportExperienceCandidateServiceTest`. 전체 `be/.\\mvnw.cmd -q verify`: **59개 테스트, 실패 0, 오류 0, 건너뜀 0**.
- **운영 전 별도 작업:** 운영 DB `experience_summaries` 마이그레이션, PDF 파서 보안/리소스 점검, 운영 환경 저장소 유지 정책. 현재 테스트는 로컬 H2 기반이다. 실제 LLM Provider 및 외부 API 토큰은 사용하지 않았다.
