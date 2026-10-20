# 경험 정리 API 및 ERD 설계 · 1차 구현 (v1.2)

> 상태: 1차 경험 관리 API 로컬 구현 및 통합 테스트 통과 / 기준: Figma 디자인페이지(노드 `0:1`), 사용자 제공 태그별 질문 화면, 기능명세서. DB 스키마는 로컬 H2에서만 검증했으며 운영 DB에 적용하지 않았다.
> 범위: 경험 대시보드, 프리셋 태그, 빠르게 작성하기, 질문별 답변, 임시저장·완료. 대화형 정리·PDF/Notion 가져오기는 확장 설계만 포함.

## 1. 기능 범위와 데이터 구조
- 경험당 태그는 하나만 저장한다. 사용 가능한 태그는 `STAR`, `GROWTH_VALUES`(성장·가치관), `STRENGTH_WEAKNESS`(장단점), `COLLABORATION`(협업), `ACADEMIC_CAREER`(학업·직무), `CHALLENGE`(도전·극복) 6종이다. 코드 문자열은 API 설계 제안값이며 프론트와 공유한다.
- 작성 방식(`creationMethod`)과 태그(`tagCode`)는 별개: 작성 방식은 `QUICK`, `CONVERSATION`, `IMPORT`; 빠르게 작성하기는 태그에 맞는 질문 폼을 보여준다.
- 화면에 제목, 태그, 경험 기간, 질문별 답변, 질문 추가, 진행률, 임시저장, 카드 최종확인이 있다. STAR는 S/T/A/R 외 Insight가 표시되는 최종 화면도 있다.
- 프리셋 태그는 단일 선택만 지원한다. 사용자 정의 태그 API나 다대다 태그 매핑 테이블은 만들지 않는다.
- 경험 카드는 한 공통 목록에서 조회·검색·필터·수정·삭제한다.
- STAR·장단점은 화면과 질문 구성이 다른 **별도 유형**으로 유지하지만, 테이블을 종류별로 6개 만드는 대신 공통 질문-답변 테이블을 사용한다. `tagCode`로 유형을 구분하고 서로 다른 질문 세트를 내려준다.

## 2. ERD (표)

### 테이블 및 관계
| 테이블 | PK | FK / 관계 | 용도 |
| --- | --- | --- | --- |
| `members` (기존) | `id` (UUID) | 1:N → `experiences` | 경험 소유자 |
| `experience_tags` | `code` (VARCHAR(50)) | 1:N → `experiences`, `experience_questions` | 6개 고정 프리셋 태그 |
| `experiences` | `id` (UUID) | `member_id` → `members.id`, `tag_code` → `experience_tags.code` | 공통 경험 카드, 단일 태그 |
| `experience_questions` | `id` (UUID) | `tag_code` → `experience_tags.code` | 태그별 기본 질문 템플릿 |
| `experience_answers` | `id` (UUID) | `experience_id` → `experiences.id`, `question_id` → `experience_questions.id` (NULL 가능) | 프리셋/추가 질문별 답변 |
| `experience_conversation_messages` (2차) | `id` (UUID) | `experience_id` → `experiences.id` | 대화형 작성 중 질문과 응답 |

### 테이블 컬럼
| 테이블 | 컬럼 | 자료형 | 제약 및 설명 |
| --- | --- | --- | --- |
| `experience_tags` | `code`, `name` | VARCHAR(50), VARCHAR(100) | 코드 PK; 이름 NOT NULL |
| `experience_tags` | `display_order`, `active` | INT, BOOLEAN | 표시 순서·활성 여부 |
| `experiences` | `id`, `member_id` | UUID, UUID | PK, 회원 FK / NOT NULL |
| `experiences` | `tag_code`, `title` | VARCHAR(50), VARCHAR(150) | 태그 FK / NOT NULL, 제목 NOT NULL |
| `experiences` | `started_at`, `ended_at` | DATE, DATE | 경험 기간, 시작일 ≤ 종료일 |
| `experiences` | `creation_method`, `status` | VARCHAR(20), VARCHAR(20) | QUICK/CONVERSATION/IMPORT, DRAFT/COMPLETED |
| `experiences` | `summary` | TEXT | 카드 요약 / NULL 가능 |
| `experiences` | `created_at`, `updated_at` | TIMESTAMP, TIMESTAMP | 생성·수정 시각 |
| `experience_questions` | `id`, `tag_code`, `code` | UUID, VARCHAR(50), VARCHAR(50) | PK, 태그 FK, (tag_code, code) UNIQUE |
| `experience_questions` | `label`, `prompt` | VARCHAR(200), TEXT | 질문 라벨·본문 |
| `experience_questions` | `display_order`, `required`, `active` | INT, BOOLEAN, BOOLEAN | 순서·완료 시 필수 여부·활성 상태 |
| `experience_answers` | `id`, `experience_id`, `question_id` | UUID, UUID, UUID | PK, 경험 FK, 기본 질문 FK(NULL 가능) |
| `experience_answers` | `custom_label`, `content` | VARCHAR(200), TEXT | 추가 질문 제목 또는 답변 본문 |
| `experience_answers` | `display_order`, `updated_at` | INT, TIMESTAMP | 질문 순서·갱신 시각 |
| `experience_conversation_messages` | `id`, `experience_id` | UUID, UUID | PK, 경험 FK |
| `experience_conversation_messages` | `sequence`, `role`, `content`, `created_at` | INT, VARCHAR(20), TEXT, TIMESTAMP | 대화 순서, 역할, 내용, 시각 |

### 제약·소유권
- `experiences.member_id`는 기존 `members.id(UUID)` FK. 모든 경험 API는 인증된 JWT의 사용자 기준으로 검색한다.
- `experiences.tag_code`는 필수 단일 FK이며 **경험당 태그는 정확히 1개**다. 다중 태그 선택이나 태그 매핑 테이블은 설계 범위에서 제외한다.
- `experience_questions`는 관리형 프리셋. `(tag_code, code)` 유니크, `(tag_code, display_order)` 정렬.
- `experience_answers.question_id`는 기본 질문일 때 FK, 사용자가 '추가하기'로 만든 항목은 null이며 `custom_label`을 저장한다. 두 값 중 하나는 필수(CHECK), 같은 경험에서 같은 프리셋 질문은 최대 1회(부분 유니크 인덱스 또는 서비스 검증).
- 질문이 해당 경험의 태그에 속하는지 서버에서 검증. 완료된 경험도 수정 가능하고 완료조건을 다시 검증한다.
- `status`: `DRAFT`, `COMPLETED`; `creation_method`: `QUICK`, `CONVERSATION`, `IMPORT`.
- 경험 삭제 시 답변/대화 연결을 함께 제거. 프리셋 태그·질문은 삭제하지 않는다.

## 3. 태그별 기본 질문 (2026-10-20 사용자 제공 화면 기준)
태그는 1개만 선택한다. 모든 태그에는 서로 다른 4개의 기본 질문이 있으며 질문 순서대로 `experience_questions`를 시드한다.

| 태그 | 순서 | 질문 라벨 | 화면의 안내 문구 |
| --- | ---: | --- | --- |
| STAR | 1 | Situation | 배경 / 상황 |
| STAR | 2 | Task | 역할 / 과제 |
| STAR | 3 | Action | 행동 |
| STAR | 4 | Result | 결과 |
| 성장·가치관 | 1 | 성장 배경 | 어떤 환경과 경험 속에서 성장했나요? |
| 성장·가치관 | 2 | 가치관 | 그 경험을 통해 갖게 된 가치관은 무엇인가요? |
| 성장·가치관 | 3 | 실천 사례 | 이후 그 가치관을 실제로 실천했던 사례가 있나요? |
| 성장·가치관 | 4 | 현재의 나 | 그 가치관이 지금의 나에게 어떤 영향을 주고 있나요? |
| 장단점 | 1 | 성격의 강점 | 내 성격의 강점은 무엇이라고 생각하나요? |
| 장단점 | 2 | 성격의 단점 | 반대로 보완하고 싶은 성격의 단점은 무엇인가요? |
| 장단점 | 3 | 보완 노력 | 그 단점을 보완하기 위해 어떤 노력을 하고 있나요? |
| 장단점 | 4 | 현재의 변화 | 그 노력으로 지금은 어떤 점이 달라졌나요? |
| 협업 | 1 | 협업 상황 | 내 성격의 장점은 무엇이라고 생각하나요? |
| 협업 | 2 | 나의 역할 | 그 협업에서 내가 맡은 역할과 책임은 무엇이었나요? |
| 협업 | 3 | 협업 방식 | 의견 차이나 문제를 어떻게 조율하고 해결했나요? |
| 협업 | 4 | 결과와 배운 점 | 협업의 결과와 그 과정에서 배운 점은 무엇인가요? |
| 학업·직무 | 1 | 경험 배경 | 학업이나 직무와 관련해 가장 기억에 남는 경험은 무엇인가요? |
| 학업·직무 | 2 | 역할과 목표 | 그 경험에서 맡은 역할과 달성해야 했던 목표는 무엇이었나요? |
| 학업·직무 | 3 | 수행 과정 | 목표를 이루기 위해 어떤 지식이나 방법을 활용했나요? |
| 학업·직무 | 4 | 성과와 역량 | 그 경험을 통해 어떤 성과와 직무 역량을 얻었나요? |
| 도전·극복 | 1 | 도전 상황 | 나에게 가장 어렵거나 도전적이었던 경험은 무엇인가요? |
| 도전·극복 | 2 | 문제와 목표 | 그 상황에서 해결해야 했던 문제와 목표는 무엇이었나요? |
| 도전·극복 | 3 | 극복 과정 | 어려움을 해결하기 위해 어떤 행동과 노력을 했나요? |
| 도전·극복 | 4 | 결과와 성장 | 그 결과 무엇이 달라졌고, 어떤 점에서 성장했나요? |

**디자인 확인 사항:** 협업 1번 질문의 안내 문구가 '내 성격의 장점은 무엇이라고 생각하나요?'로 표시되어 있음. 해당 문구는 화면을 그대로 반영했으나 협업 상황과 내용이 맞지 않아 기획/디자인팀에 오탈자 여부 확인 권장. STAR의 Insight는 기본 4문항 외 최종 카드 부가 항목이므로 완료 조건에서 제외한다.

**저장 정책:** 질문마다 있는 저장 버튼은 `PUT /api/experiences/{id}/answers/{questionId}`로 해당 기본 질문 답변만 갱신한다. 프론트의 취소 버튼은 서버 저장 전 편집을 취소하는 UI 처리다. 사용자 추가 질문의 등록·삭제·재정렬은 `PUT /api/experiences/{id}/answers`로 답변 목록 전체를 교체한다. 완료된 카드에서 필수 답변을 빈 값으로 변경하면 서버가 거부한다.

## 4. API 계약 (초안 → 구현 전 FE 합의)
| Method | URI | 기능 |
| --- | --- | --- |
| GET | `/api/experience-tags` | 사용 가능한 6종 태그 조회 |
| GET | `/api/experience-tags/{tagCode}/questions` | 태그별 프리셋 질문 및 입력 순서 조회 |
| POST | `/api/experiences` | 경험 초안 생성 (`DRAFT`) |
| GET | `/api/experiences` | 내 경험 목록·검색·태그/상태 필터, 페이지네이션 |
| GET | `/api/experiences/{id}` | 카드 기본 정보 + 질문·답변 + 진행률(0~100) 조회 |
| PATCH | `/api/experiences/{id}` | 제목·기간·요약 수정 |
| PUT | `/api/experiences/{id}/answers` | 프리셋/추가 질문 답변 전체 교체 (임시저장) |
| PUT | `/api/experiences/{id}/answers/{questionId}` | 기본 질문 1개의 답변만 저장/수정 |
| PATCH | `/api/experiences/{id}/status` | 작성 완료 처리 또는 임시저장 전환 |
| DELETE | `/api/experiences/{id}` | 내 경험 삭제 |

대화형 정리 후속 API(2차): `GET/POST /api/experiences/{id}/messages` 및 카드 구조화 저장. 가져오기(3차)는 파일/Notion과 분리된 API에서 추출 후 동일 경험 저장 구조로 변환.

### 요청 예시
`POST /api/experiences`
```json
{
  "title": "프로젝트 협업 경험",
  "tagCode": "COLLABORATION",
  "creationMethod": "QUICK",
  "startedAt": "2026-03-01",
  "endedAt": "2026-06-30"
}
```

`PUT /api/experiences/{id}/answers` — 전체 답변 대체(upsert + 누락 항목 처리 정책은 '보낸 항목만 갱신'이 아닌 전체 교체)
```json
{
  "answers": [
    {"questionId": "00000000-0000-0000-0000-000000000001", "content": "경험 내용", "displayOrder": 1},
    {"customLabel": "추가 질문", "content": "추가 답변", "displayOrder": 99}
  ]
}
```
`questionId`는 설명용 UUID이므로 실제로는 질문 조회 API의 ID를 사용한다.

`PATCH /api/experiences/{id}/status`
```json
{"status": "COMPLETED"}
```

`GET /api/experiences?keyword=협업&tagCode=COLLABORATION&status=COMPLETED&page=0&size=20`.
성공 응답은 기존 백엔드와 동일한 `{"success":true,"data":...}` 래퍼 사용.

## 5. 저장·완료·에러 정책
- 초안은 빈 답변을 허용하며 반복 저장 가능. 화면 진행률은 **필수 기본 질문** 중 답변이 비어 있지 않은 문항 수 / 필수 기본 질문 수 × 100을 정수 반올림한다. 추가 질문은 계산에서 제외한다. 상세 조회 응답의 `data.progress`에 포함한다.
- 완료 전환 때 제목·태그·기간 유효성(종료일 ≥ 시작일)과 해당 태그의 필수 프리셋 질문 응답을 확인한다. STAR는 S/T/A/R 필수, Insight는 별도 확정 전까지 완료조건에서 제외.
- 경험 미존재 또는 다른 회원 소유: `404`; 형식 오류/태그에 속하지 않는 질문/완료조건 미충족: `400`; 미인증: `401`.
- 모든 목록/상세/수정/삭제에서 사용자 소유권 검증; createdAt/updatedAt 서버 생성.
- 저장 중 오류의 부분 반영을 방지하도록 경험/답변 갱신은 트랜잭션 처리한다.

## 6. 구현 순서
1. **1차:** 태그·질문 시드, 경험 CRUD, 답변 저장, 임시저장·완료 검증, 검색·필터, Swagger, 통합 테스트.
2. **2차:** 대화형 질문·메시지 저장/복원·경험 카드 변환. 명세서 3.3.2.3에 따라 중간 저장 및 재개를 지원한다.
3. **3차:** PDF/Notion/텍스트 가져오기. 추출 결과를 편집한 뒤 경험 카드로 저장한다.

## 7. 확인이 필요한 세부 정책과 추천값
| 항목 | Figma / 기능명세서 확인 | 구현 추천값 | 1차 구현 전 필수? |
| --- | --- | --- | --- |
| 태그별 질문 원문 | 사용자가 제공한 6개 화면에서 총 24문항의 라벨·안내 문구 확보 | 사용자 제공 문구를 코드의 프리셋 시드에 반영. 단 협업 첫 질문은 협업 내용과 맞지 않아 화면과 동일하게 유지하고 디자인팀 확인 권장 | 반영 완료 |
| 질문별 글자수 상한 | 명세서 3장에서는 구체적인 최대 글자수가 확인되지 않음 | DB 답변은 `TEXT`; 요청 DTO는 우선 항목당 5,000자 상한(서비스 정책 제안). UI 문구상 제한이 확인되면 일치시킴 | **아니오**: 안전 상한은 BE 기본값으로 선적용 가능 |
| 임시저장/완료 검증 | 명세서에 대화형 중간 저장·재개, 작성 완료 전 미리보기/편집 흐름 명시. STAR 네 항목과 성격형 네 항목 명시 | DRAFT는 일부 답변 허용; COMPLETED는 해당 유형의 필수 질문 전부 입력·제목/기간 확인. STAR Insight는 기획에서 필수로 지정하기 전까지 선택 사항 | **아니오**: 추천값으로 구현 가능 |
| 진행률 | 디자인에 작성 진행률 표시, 명세서에는 계산식 없음 | 기본 필수 질문 중 내용이 있는 질문 수 / 필수 질문 수 × 100, 정수 반올림. 사용자 추가 질문은 분모에서 제외 | **아니오**: FE에 계산식 공유 후 구현 |
| 추가 질문 삭제·정렬 | 디자인에 질문 추가 UI. 명세서에서 추가 질문의 삭제/순서 규칙은 확인되지 않음 | 각 추가 질문에 UUID와 displayOrder 부여. 삭제 시 해당 사용자 질문·답변만 삭제; 기본 질문 삭제 금지. 순서는 입력 순 유지 | **아니오**: 서버 기준 기본 정책으로 구현 |
| 대화형 작성 임시저장 | 명세서 3.3.2.3에 중간 저장 및 재개 명시 | 세션과 메시지를 경험에 저장, 상태 DRAFT 유지. 대화 결과를 구조화한 뒤 사용자 확인 시 COMPLETED 전환 | **1차는 아니오**, 2차 구현 시 필요 |
| 가져오기 중 임시저장 | 명세서에는 PDF·Notion·텍스트 추출 → 검토·수정 → 카드 저장 명시. 업로드 도중 자동 임시저장 여부는 없음 | 변환 결과를 DRAFT 경험으로 저장하고 확인 시 완료. 업로드 중인 원본의 자동 임시저장은 도입하지 않음 | **1차는 아니오**, 3차 구현 시 필요 |
| 삭제 시 자소서 연결 안내 | 명세서 2.4.1에 명시 | 향후 문항 연결 데이터가 생기면 삭제 전 사용처 개수·영향을 조회할 수 있도록 확장. 1차에서는 연결 엔티티가 없어 삭제만 처리 | **아니오**: 연결 기능 구현 때 연동 |

1차 구현에서는 답변당 최대 5,000자, 완료 시 기본 4문항 필수, 추가 질문은 선택, 기본 질문의 작성 수 기준 진행률 산정 정책을 적용한다. 대화형/가져오기는 별도 구현 단계로 남겨 둔다. 프론트는 API 에러 및 질문 코드를 문서와 동일하게 사용한다.

**구현 현황:** 1차 CRUD·프리셋 질문·답변 일괄/단건 저장·완료 검증·검색·필터·진행률 조회 API를 `feat/experience-api-design` 로컬 브랜치에 구현했다. JWT 권한 확인 및 경험 생성→답변 저장→완료→삭제/타 계정 접근 차단 통합 테스트를 추가했다. 2차 대화형 정리·3차 PDF/Notion 가져오기·운영 DB 마이그레이션은 미구현이다. GitHub 푸시·PR·main 머지는 별도 작업이다.
