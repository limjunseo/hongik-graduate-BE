# 자기소개서 관리 API·ERD 설계 (v1)

> 기준: `홍대졸프_기능명세서.xlsx`의 자소서 관련 요구사항 및 `Pofit 포핏 (3).zip`의 자기소개서 화면. **1단계(자소서/문항 CRUD)는 `feat/cover-letter-management` 브랜치에서 구현**, 2단계 이상은 설계안이다. 기존 경험 관리 API(PR #8)는 최신 `main`에 반영되어 있다.

## 1. 화면 흐름 및 구현 범위

| 단계 | 화면/명세서 근거 | 백엔드 책임 |
| --- | --- | --- |
| 목록 | 자소서 대시보드, 명세서 '자소서 카드 목록' | 본인 지원건 목록, 기업명·직무·마감일, 검색/필터/정렬 |
| 신규 생성 | '새 자소서 만들기-링크붙여넣기', 인식 성공·실패, 명세서 2.2.2~2.2.4 | 기업·직무·마감일 저장, 공고 URL/텍스트 수집, 구조화·근거 저장, 실패 시 수동입력 |
| 기업 분석 | '기업 분석 리포트', 명세서 2.2.1·2.2.4 | 기업 분석 내용·근거 및 상태 조회 |
| 문항 분석 | '문항 분석', '자소서 문항 답하기', 명세서 3.1.1 | 문항 추가·수정·삭제·드래그 정렬, 질문·글자수 제한·답변 저장 |
| 경험 연결 | '초안 생성하기-경험카드 검색하기', '태그별 필터', '경험카드 드래그&드롭' | 기존 experience 카드 조회 및 문항별 연결·해제·순서 관리 |
| 초안·첨삭 | '초안 생성하기', 'ai 대화첨삭', 명세서 3.3.1·4.2 | AI 생성 요청/결과 저장, 사용자의 직접 수정, 대화 세션·메시지 기록 |
| 버전 | '현재버전 저장', '버전으로 저장', '버전 관리', 명세서 4.3 | 문항별/지원건별 저장 시점의 스냅샷·조회·복원 |
| 최종 검토 | '자소서 최종 검토', '전체 복사하기', 'pdf로 내보내기' | 최종 문항·답변 묶음 제공, PDF 파일 생성은 별도 구현 |

**구조 선택:** '폴더'는 화면상 회사별 자소서 카드/지원건을 가리키므로 별도 폴더 테이블은 만들지 않는다. 동일 회사에 여러 직무/지원건 생성은 허용한다(권장 정책). UI가 실제 중첩 폴더를 요구할 때만 폴더 엔티티를 분리한다.

## 2. ERD: 테이블 및 관계

| 테이블 | PK | FK 및 관계 | 역할 |
| --- | --- | --- | --- |
| `members` (기존) | `id` UUID | 1:N → `cover_letters` | 계정 소유자 |
| `cover_letters` | `id` UUID | `member_id` → members | 기업·직무별 자소서 작업공간 / 지원건 |
| `job_postings` | `id` UUID | `cover_letter_id` → cover_letters (1:0..1) | 공고 URL/원문/분석 상태 및 구조화 JD |
| `posting_evidence` | `id` UUID | `job_posting_id` → job_postings (1:N) | JD 항목의 원문 근거·출처 |
| `company_reports` | `id` UUID | `cover_letter_id` → cover_letters (1:0..1) | 기업 분석 인사이트 및 생성 상태 |
| `cover_letter_questions` | `id` UUID | `cover_letter_id` → cover_letters (1:N) | 자소서 문항·작성 내용·글자수 제한·정렬 |
| `question_experiences` | (`question_id`,`experience_id`) | questions ↔ 기존 experiences (N:M) | 문항별 경험 카드 연결, 표시 순서 |
| `cover_letter_versions` | `id` UUID | `cover_letter_id` → cover_letters (1:N) | 저장 시점의 문항·답변 스냅샷 |
| `editing_chats` | `id` UUID | `question_id` → questions (1:N) | AI 첨삭 세션 |
| `editing_chat_messages` | `id` UUID | `chat_id` → editing_chats (1:N) | 첨삭 대화 메시지 |

### 핵심 컬럼 (권장)

| 테이블 | 컬럼 | 자료형 | 제약·설명 |
| --- | --- | --- | --- |
| cover_letters | id, member_id | UUID | PK, FK, NOT NULL |
| cover_letters | company_name, position_name | VARCHAR(150) | 필수 |
| cover_letters | deadline | DATE | 선택 |
| cover_letters | jd_url | VARCHAR(2048) | 채용공고 원문 링크, 선택; 이번 작업에 구현 |
| cover_letters | status, created_at, updated_at | VARCHAR(20), TIMESTAMP, TIMESTAMP | DRAFT/IN_PROGRESS/COMPLETED; 시간은 서버 생성 |
| job_postings | id, cover_letter_id | UUID | PK, FK, cover_letter_id UNIQUE |
| job_postings | source_type, source_url, original_text | VARCHAR(20), VARCHAR(2048), TEXT | URL/TEXT; 접근 불가 시 TEXT 입력 |
| job_postings | analysis_status, duties, qualifications, preferences, hiring_steps | VARCHAR(20), TEXT×4 | PENDING/SUCCESS/FAILED; 분류된 공고 |
| job_postings | analyzed_at, error_reason | TIMESTAMP, TEXT | 분석 결과 및 오류 |
| posting_evidence | id, job_posting_id, section, excerpt, source_url | UUID, UUID, VARCHAR(50), TEXT, VARCHAR(2048) | 원문 근거 |
| company_reports | id, cover_letter_id, status, report_json, updated_at | UUID, UUID, VARCHAR(20), JSON/TEXT, TIMESTAMP | 회사 소개·키워드·역량·전략 보고서(스키마는 생성 기능 구현 시 정의) |
| cover_letter_questions | id, cover_letter_id, display_order | UUID, UUID, INT | PK, FK; 지원건별 순서 중복 불가 |
| cover_letter_questions | question_text, character_limit, answer_text | TEXT, INT, TEXT | 문항, 글자수, 현재 답변; 답변은 빈 문자열 허용 |
| cover_letter_questions | created_at, updated_at | TIMESTAMP, TIMESTAMP | 서버 생성 |
| question_experiences | question_id, experience_id, display_order, created_at | UUID, UUID, INT, TIMESTAMP | 문항당 여러 경험 허용; 회원 소유권 일치 필수 |
| cover_letter_versions | id, cover_letter_id, label, snapshot_json, created_at | UUID, UUID, VARCHAR(100), JSON/TEXT, TIMESTAMP | 문항·답변 전체를 불변 스냅샷으로 저장 |
| editing_chats | id, question_id, created_at | UUID, UUID, TIMESTAMP | 문항별 AI 첨삭 세션 |
| editing_chat_messages | id, chat_id, role, content, sequence, created_at | UUID, UUID, VARCHAR(20), TEXT, INT, TIMESTAMP | USER/ASSISTANT |

**데이터 경계:** 모두 인증 사용자의 지원건 기준 조회. 연결 경험은 같은 회원이 소유해야 한다. 자소서 삭제 시 하위 문항·연결·버전·대화 기록 삭제. 버전은 저장 당시 답변 복사본으로 보존하여 현재 답변 변경의 영향을 받지 않는다.

## 3. API 설계

| Method | 경로 | 기능 | 권장 구현 순서 |
| --- | --- | --- | --- |
| GET | `/api/cover-letters` | 내 작업공간 목록, 검색/정렬/상태 필터 | 1 |
| POST | `/api/cover-letters` | 기업명·직무·마감일로 생성 | 1 |
| GET | `/api/cover-letters/{id}` | 지원건 및 문항 전체 조회 | 1 |
| PATCH | `/api/cover-letters/{id}` | 기본정보 수정 | 1 |
| DELETE | `/api/cover-letters/{id}` | 작업공간 삭제 | 1 |
| POST | `/api/cover-letters/{id}/questions` | 문항 추가 | 1 |
| PATCH | `/api/cover-letters/{id}/questions/{questionId}` | 질문/글자수/답변 수정(부분 저장) | 1 |
| DELETE | `/api/cover-letters/{id}/questions/{questionId}` | 문항 삭제 | 1 |
| PUT | `/api/cover-letters/{id}/questions/order` | 질문 ID 배열로 순서 일괄 변경 | 1 |
| GET | `/api/cover-letters/{id}/questions/{questionId}/experiences` | 연결된 경험 조회 | 2 |
| POST | `/api/cover-letters/{id}/questions/{questionId}/experiences` | 경험 카드 연결 | 2 |
| DELETE | `/api/cover-letters/{id}/questions/{questionId}/experiences/{experienceId}` | 연결 해제 | 2 |
| POST | `/api/cover-letters/{id}/posting/analyze` | URL/텍스트 입력 후 공고 분석 시작 | 3 |
| GET | `/api/cover-letters/{id}/posting` | 분석 상태·JD·원문 근거 조회 | 3 |
| GET | `/api/cover-letters/{id}/company-report` | 기업 분석 리포트 조회 | 3 |
| POST | `/api/cover-letters/{id}/questions/{questionId}/draft` | 연결 경험 기반 AI 초안 생성 | 4 |
| POST | `/api/cover-letters/{id}/questions/{questionId}/chats/{chatId}/messages` | AI 첨삭 대화 | 4 |
| POST | `/api/cover-letters/{id}/versions` | 현재 문항과 답변 전체 버전 저장 | 5 |
| GET | `/api/cover-letters/{id}/versions` | 버전 목록 | 5 |
| GET | `/api/cover-letters/{id}/versions/{versionId}` | 저장된 버전 상세 | 5 |
| POST | `/api/cover-letters/{id}/versions/{versionId}/restore` | 버전 복원 (현재 상태 선백업 권장) | 5 |
| GET | `/api/cover-letters/{id}/review` | 최종 검토용 질문·답변 전체 | 5 |
| GET | `/api/cover-letters/{id}/export/pdf` | PDF 출력 | 6 |

목록의 페이지네이션: `page=0&size=20`을 기본으로 하며 회원 소유 범위에서만 검색한다. 응답 래퍼는 기존 프로젝트의 `ApiResponse(success,data)` 사용.

## 4. 정책 및 예외

| 주제 | 화면·명세서에 있는 요구 | 구현 기본값(제안) |
| --- | --- | --- |
| 자소서 생성 | 기업명·직무명·마감기한 표시 | 기업명·직무명 필수, 마감일 선택; 동일 기업 복수 지원건 허용 |
| 공고 URL | URL 검사·크롤링 실패 안내·텍스트 붙여넣기 대체 | URL은 http(s)만 허용, 내부망/루프백 주소 차단(SSRF 방어), 요청 시간·본문 크기 제한; 실패 시 기존 입력 보존 |
| 문항 수/정렬 | 문항 추가·삭제·드래그 정렬 | 최대 30개, 정렬은 전체 questionId 배열의 중복·누락 검증 후 트랜잭션 저장 |
| 글자수 | 문항별 제한 설정, 실시간 카운트 | `character_limit` 1~10,000자, 공백 포함 Unicode 코드포인트 수로 측정; FE·BE 동일 기준 |
| 답변 저장 | 실시간 텍스트 편집 | 명시적 저장 API + FE 디바운스 자동저장(권장 1초). 저장 충돌은 `updated_at`/버전 키로 검증하도록 확장 |
| 경험 연결 | 검색·태그 필터·문항에 경험 연결 | 같은 회원의 경험만 연결; 경험 연결은 원본 카드를 복사하지 않고 참조 |
| 경험 삭제 영향 | 명세서에 연결된 자소서 문항 영향 안내 | 경험 삭제 전 연결 문항 수·목록 조회 API 추가, 삭제 시 연결만 해제 (연결된 초안 본문 보존) |
| AI 초안 | 공고 분석·경험 카드 기반 생성 | 원문 경험/공고를 입력으로 전달, 결과는 **사용자 승인 후** 현재 답변에 적용, 모델 결과를 사실로 확정하지 않음 |
| 버전 | 현재 버전 저장/목록/복원 화면 | 사용자 지정 버전명, 불변 스냅샷, 복원 시 기존 현재본을 자동 백업 |
| PDF·전체 복사 | 최종 검토/내보내기 화면 | 전체 복사는 FE, PDF 출력은 BE 서버 파일 제공 또는 FE 출력 중 방식 합의 필요 |
| 오류 | 상세 화면에서 데이터 불일치·권한 오류 처리 | 401 미인증, 404 타인 소유 또는 미존재, 400 입력 검증, 409 저장 충돌 |

## 5. 개발 단계 제안

1. **기본 관리:** 지원건 CRUD + 문항 CRUD/정렬/답변 저장 + 통합 테스트. 현재 경험 관리 API와 독립적으로 구현 가능.
2. **경험 연계:** 기존 경험 카드 참조·검색·연결/해제·삭제 영향 알림. 이때 경험 삭제 API와 연결 관계를 통합 검증.
3. **공고/기업 분석:** URL 수집·수동 입력·구조화 JD·근거·리포트.
4. **AI 작성/첨삭:** 경험 추천, 초안 생성, 대화 및 저장.
5. **버전/검토:** 버전 저장·복원, 최종 검토, PDF/전체 복사.

### 유의 사항
- 명세서에서 문항 추가·삭제·정렬, URL 실패 대체, 경험 추천, 버전, AI 첨삭 요구를 확인했다. 위 표의 **숫자 제한값, URL 보안정책, 동시편집 충돌정책, 내부 테이블 분할과 일부 API 경로는 설계 제안**이며 원문에 확정되어 있지 않다.
- 1단계 자소서·문항 CRUD 엔티티, Repository, Service, Controller 구현 완료. Maven 전체 테스트 통과.
- 목록은 keyword/status 필터, page/size를 제공한다(현 구현은 인메모리 페이징이며 totalElements 미제공).
- 소유권 검사, 문항 30개 제한, 글자수 1~10,000자, 문항 순서 일괄 변경 검증 적용.
- 자기소개서 JD URL 저장·조회·수정: `cover_letters.jd_url` 선택 컬럼 추가. 기존 `POST /api/cover-letters` 및 `PATCH /api/cover-letters/{id}` 요청의 `jdUrl`을 받고, 기존 목록·상세 응답에 포함한다. 2,048자 이하의 유효한 HTTP(S) 주소만 허용하며 PATCH에서 빈 문자열로 초기화할 수 있다. URL 수집·분석은 수행하지 않는다.
- 경험 연결: `question_experiences`에서 문항별 경험 조회·연결·해제 지원. 회원 소유권 검사 및 중복 연결 방지 적용.
- 경험 연결 API: `GET/POST /api/cover-letters/{id}/questions/{questionId}/experiences`, `DELETE /api/cover-letters/{id}/questions/{questionId}/experiences/{experienceId}`.
- 버전 관리: 초안/1.0/2.0 등 라벨과 자소서 문항·답변의 저장 시점 스냅샷 보관. 버전 생성, 목록, 상세 및 복원 API 구현.
- 채용공고 데이터 저장: `job_postings` 테이블에 자기소개서별 공고 URL, JD 원문, 자격요건, 우대사항, 전형절차 저장·조회·갱신·삭제 구현. `GET/PUT/DELETE /api/cover-letters/{coverId}/posting`을 제공한다. PUT은 전체 필드를 갱신하며 선택 필드를 null로 초기화할 수 있다. 동일 자소서에는 공고 하나만 연결되고, 자소서 삭제 시 공고도 삭제된다. URL 수집·분석 및 AI 기능은 포함하지 않는다.
- 지원 현황 칸반: 기존 자소서 작성 상태 `status`와 분리한 `application_status` 컬럼을 추가했다. `GET /api/application-board`는 단계별 지원건 목록을 반환하고, `PATCH /api/application-board/{coverLetterId}/status`는 본인 지원건 상태를 변경한다. 상태값은 현재 `PLANNED`, `PREPARING`, `SUBMITTED`, `CLOSED`로 설정했으며 프론트와 최종 명칭 합의가 필요하다. 면접 기능은 추가하지 않는다.
- 지원 상세: `GET/PUT /api/cover-letters/{coverId}/application-detail`. 기존 `cover_letters.deadline`을 마감일로 공유하고, `application_details`에는 면접일(interviewDate), 서류 결과(documentResult: PENDING/PASSED/FAILED/null), 이력서(hasResume) 및 포트폴리오(hasPortfolio) 여부를 보관한다. PUT은 전체 필드 덮어쓰기로 null 마감일/면접일 초기화를 허용한다. 지원건 삭제 시 상세도 삭제하며 본인 소유권을 검사한다. 면접 질문/답변 및 복기 기능은 포함하지 않는다.
- 후속 작업: 채용공고 분석·AI 첨삭·PDF. 운영 DB 마이그레이션 및 main 머지는 별도 작업이다.

