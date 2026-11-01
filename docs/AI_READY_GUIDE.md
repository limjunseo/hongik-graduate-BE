# AI 기능 연결 가이드 (2026-11-01)

## 범위
백엔드의 AI 경험 채팅·구조화 요약·다음 질문, JD 분석, 근거 기반 기업/직무 분석,
지원 전략, 자기소개서 문항 유형 추천·초안·첨삭·문항별 수정 대화가 준비되어 있습니다.
면접 세션/면접 피드백과 운영용 사용자별 비용·쿼터 제한은 요청에 따라 제외했습니다.
AI가 만든 답변과 분석은 검증·승인이 필요한 **미리보기**입니다.

## AI 키만 넣는 방법 (Windows PowerShell)
`be/.env-local` 파일을 만들고 아래 한 줄만 입력합니다.
```properties
AI_API_KEY=YOUR_API_KEY_HERE
```
이 파일은 Git에서 무시됩니다. 키 값을 Git/채팅/로그에 붙여 넣지 마세요.
`AI_API_KEY`만 있으면 서버 기동 시 `app.ai.enabled=true` 및
`app.ai.provider=openai`가 자동 선택됩니다. 명시적으로 `AI_ENABLED` 또는
`AI_PROVIDER`를 설정한 경우에는 명시 설정이 우선합니다.

```powershell
cd C:\Users\1234\Desktop\졸프\be
.\mvnw.cmd spring-boot:run
```
다른 실행 환경에서는 `AI_API_KEY` 환경변수만 주입해도 됩니다.
모델을 바꿀 때 `AI_MODEL`을 설정합니다(기본 `gpt-4o-mini`).
모든 AI API는 기존 JWT Bearer 인증을 요구합니다. 공통 응답은 기존 `ApiResponse`.
예시의 ID는 실제 사용자 소유 리소스 ID로 교체해야 합니다.

## 엔드포인트
| Method | URI | 내용 |
| --- | --- | --- |
| POST | /api/experiences/{id}/ai-chat/messages | 경험 대화 `{"message":"...","requestId":"unique-key"}` |
| GET | /api/experiences/{id}/ai-chat/messages | 기존 경험 대화 이력 |
| POST | /api/experiences/{id}/ai-chat/next-question | 보완할 경험 항목에 대한 질문 한 개 (미리보기) |
| POST | /api/experiences/{id}/ai-summary:generate | 경험 요약 미리보기 |
| POST | /api/cover-letters/{coverId}/posting/analyze | 소유 JD AI 분석(DRAFT) |
| POST | /api/cover-letters/{coverId}/posting/role-analyze | JD 기반 직무 분석 미리보기 |
| POST | /api/cover-letters/{coverId}/company/analyze | 사용자가 제공한 출처 텍스트로 기업 분석 미리보기 |
| POST | /api/cover-letters/{coverId}/questions/{questionId}/analyze | 자소서 문항 유형·의도 추천 |
| POST | /api/cover-letters/{coverId}/questions/{questionId}/strategy | 역량 기반 지원 전략 미리보기 |
| POST | /api/cover-letters/{coverId}/questions/{questionId}/draft | 자소서 초안 미리보기 |
| POST | /api/cover-letters/{coverId}/questions/{questionId}/revise | 자소서 첨삭 미리보기 |
| POST | /api/cover-letters/{coverId}/questions/{questionId}/ai-chat/turns | 문항별 첨삭 대화 및 이력 저장 |
| GET | /api/cover-letters/{coverId}/questions/{questionId}/ai-chat/turns | 문항별 첨삭 이력 조회 |

## 입력 예시
기업 분석: `{"sourceUrl":"https://example.com/about","sourceText":"사용자가 붙여 넣은 출처 전문"}`.
URL을 서버가 자동으로 방문하지 않습니다. 날짜는 텍스트를 전달받은 시각이며
원격 웹페이지를 읽거나 해당 URL의 신뢰성을 확인했다는 의미가 아닙니다.

초안·지원 전략: `{"questionType":"MOTIVATION","experienceIds":[]}`.
선택 경험은 실제로 자기소개서 문항과 연결된 자기 소유 경험이어야 합니다.
초안 API는 선택 경험이 필요할 수 있으니 Swagger 계약을 확인하세요.

첨삭 대화: `{"questionType":"MOTIVATION","experienceIds":[],"currentDraft":"내 초안","revisionType":"CONCISE","instruction":"짧고 명확하게","requestId":"edit-1"}`.
`requestId`가 같으면 동일한 요청의 중복 AI 호출을 방지합니다. 서로 다른 입력으로
동일한 `requestId`를 사용하면 400 오류입니다. 출력 `revisedText`는 자소서 답변으로
자동 저장되지 않습니다. 사용자가 확인한 뒤 기존 답변 PATCH를 사용하세요.

## 구분해야 할 것
- API 키가 **없는 상태**에서는 Mock이 기본이며 AI 생성 기능이 비활성화됩니다.
- Mock을 활성화한 테스트에서는 반드시 `isMock=true` 와 `MOCK ONLY` 표시를 사용합니다.
- 실제 키로 유료 원격 모델에 요청하는 E2E 테스트는 수행하지 않았습니다.
  모델별 JSON 준수·한글 품질·원문 근거 검증은 키를 넣은 뒤 확인해야 합니다.
- 경험 구조화 요약·자소서 답변은 사용자 승인 없이 저장하지 않습니다.
- 문항별 첨삭 기록은 새 테이블 `cover_letter_ai_edit_turns`을 사용합니다.
  별도 운영 RDB 없이 로컬 H2 파일 DB를 사용하며 로컬 `ddl-auto=update`가 테이블을 반영합니다.
  DB는 `be/data/hongik-graduate.mv.db`에 저장됩니다.
- 기존 경험 채팅도 긴 AI 호출 동안 DB 잠금을 유지합니다. 현재 요청 범위는
  개발용 기능 완성으로 한정되며 고부하 운영 최적화는 포함하지 않았습니다.
- 외부 기업 자료를 서버가 자동 수집하는 기능은 제공하지 않습니다.
  출처 URL+본문을 직접 제공하면 그 본문에만 근거한 분석을 생성합니다.

## 검증
```powershell
cd be
.\mvnw.cmd verify
```
키가 없어도 기존 테스트는 Mock으로 검증할 수 있도록 구성되어 있습니다.
별도 테스트 `AiKeyProviderWiringTest`는 가짜 키로 Provider Bean 선택만 검증하며
실제 API 호출/과금은 하지 않습니다.
