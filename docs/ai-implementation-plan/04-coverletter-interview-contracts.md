# 4단계 — 자소서 Context/Pipeline 및 면접 계약

> 상태: 미구현(계획) · GitHub 이슈: AI-10, AI-11, AI-12 · 선행: [3단계](./03-job-matching.md) · 외부 토큰: 불필요

## 목표
LLM 호출 직전까지의 **자소서 초안, 첨삭, AI 채팅, 면접 질문 입력 데이터와 호출 순서**를 완성한다. 생성 결과가 없더라도 Mock과 스냅샷 테스트로 Context의 일관성을 입증한다.

## 기존 백엔드에서 재사용할 것
- `CoverLetter`, `CoverLetterQuestion`의 회사/직무/문항/`characterLimit`/`answerText`, `JobPosting` 원문.
- `QuestionExperienceLink`를 통한 선택 경험. 실제 초안 생성은 기존 `docs/cover-letter-api-erd.md`의 `POST /api/cover-letters/{id}/questions/{questionId}/draft` 설계를 따른다(아직 실제 기능이 아님).
- 문항별 편집 채팅, 버전 스냅샷 계획 및 기존 버전 저장/복원 API. **AI 응답 수신과 사용자가 답변을 저장하는 행위를 분리**한다.
- `ApiResponse(success,data)` 형식, JWT 인증, `coverId`/`questionId`/`experienceId` 소유권 검증.

## CoverLetterContextBuilder 입력·출력
```text
Input:
  memberId, coverLetterId, questionId
  selectedExperienceIds[]
  optionalJobAnalysisId
  questionType
Output (immutable):
  company, position
  question, characterLimit, questionType
  jobAnalysis: requiredSkills[], keywords[], responsibilities[]
  experiences: ExperienceSummary[] (선택된 경험만)
  rankedMatches: ExperienceMatchResult[]
  relevantSkills: SkillCode[]
  contextVersion, provenance[]
```
- 입력 값만 믿지 않고 DB를 조회해 관계·소유권을 검증한다. 최신 사용자 수정 내용을 존중하고 확정되지 않은 정보는 `null` 또는 `UNKNOWN`으로 표시한다.
- `CoverLetterContextBuilder`는 **DB/도메인 데이터를 AI 요청용 읽기 전용 Context로 조립**하는 책임만 가진다. 텍스트 생성/DB 저장/프롬프트 문자열 가공은 분리한다.
- 선택 경험 0개일 때 정책을 정한다(`MISSING_EXPERIENCE`로 생성 차단 또는 질문별 허용 여부). 부적절한 임의 경험을 끼워 넣지 않는다.
- 문항 제한 글자 수는 기존 정책(공백 포함 유니코드 코드포인트)을 따라 Context에 포함하고, 후속 생성 결과는 서버에서 재검증한다.

## 자소서 준비 Pipeline
```text
문항 + QuestionType
  → JD 구조 데이터/역량 조회
  → 경험 목록 및 소유권 검사
  → 규칙 기반 경험 Ranking
  → 선택 경험 구조화 요약
  → CoverLetterContextBuilder
  → Prompt Template + Output Schema
  → MockAiProvider (계약 검증용)
  → 5단계: 실제 AI 호출 / 사용자가 초안을 적용
```
- 지원 전략 생성 입력도 같은 Context를 사용하며, `SupportStrategyContext`에 JD 핵심 역량, 현재 매칭 근거, 보완할 질문을 포함한다. 전략 문장 자체의 생성은 5단계.
- 프롬프트 템플릿은 `be/src/main/resources/ai/prompts/coverletter/`에 버전 관리. 사용자 경험·JD 데이터는 **명령이 아닌 비신뢰 입력 데이터**로 취급한다.

## 첨삭 계약
```text
RevisionType:
  GENERAL | LOGIC | CONCISE | JOB_FIT | GRAMMAR | IMPACT

RevisionRequest:
  coverLetterId, questionId, originalText
  question, characterLimit, revisionType
  jobContext?, experienceContext?, instruction?
RevisionResult (향후):
  revisedText, changeReasons[], warnings[], provider, promptVersion
```
- 4단계에서는 요청·Context 검증, 변경 유형 정책, Mock 응답만 구현한다. 사용자의 `answerText`를 자동 갱신하지 않는다.
- AI 채팅용 `EditingChatContext`는 문항, 이전 대화 이력(순서/역할), 현재 초안, 선택 경험, 수정 목표로 구성. 5단계에서 턴 저장과 실제 호출을 연결한다.

## 면접 계약 (Beta를 위한 최소 설계)
```text
InterviewQuestionType:
  COMMON | RESUME | COVER_LETTER | JOB | COMPANY | BEHAVIOR | FOLLOW_UP

InterviewContext:
  company, jobAnalysis?, coverLetterText?
  experiences[], previousAnswer?, priorTurns[]
  questionType, difficulty?, sessionId?
```
- `InterviewSession`의 상태(STARTED/ENDED), 면접 문항, 사용자 답변, 피드백, 꼬리질문 간 관계만 설계. DB 생성과 공개 API는 5단계 Beta 착수 시 결정 가능.
- 질문 생성과 답변 평가의 `OutputSchema`를 분리한다. 근거 없는 합격 점수/인성 평가를 만들지 않는다.

## 구현 체크리스트
- [ ] AI-10: `CoverLetterContextBuilder`, `SupportStrategyContextBuilder`, 스냅샷 직렬화/검증, 프롬프트 변수 누락 검사.
- [ ] AI-10: 실제 `CoverLetterQuestion`·`JobPosting`·`Experience` 소유권/연결 관계에서 안전한 컨텍스트 구성.
- [ ] AI-11: `RevisionType`/`RevisionRequest` 및 유형별 필수 입력 검증, Mock 응답 비교.
- [ ] AI-11: 편집 채팅 Context(다중 턴, 현재 글, 버전), 생성 결과 저장은 사용자 승인 후 수행하도록 상태 모델 설계.
- [ ] AI-12: `InterviewQuestionType`/`InterviewContext`, 질문/피드백/꼬리질문 출력 스키마 및 예제 fixture.
- [ ] 내부 테스트 엔드포인트가 필요하다면 개발 프로필 전용으로 제한; 공개 생성 API는 5단계 실제 기능이 준비되기 전 비활성화.
- [ ] 기존 자소서 수동 CRUD/버전 관리/공고 저장 및 경험 연결 API가 변경 없이 동작하도록 회귀 테스트.

## 테스트 및 완료 기준 (DoD)
- [ ] 같은 입력 데이터는 같은 Context 및 스키마 버전을 생성.
- [ ] 경험 0개/여러 개, JD 부재, 문항 유형 미확정, 글자수 제한 경계값 오류 처리.
- [ ] 다른 회원의 자소서/문항/경험 ID를 섞어 Context를 조회할 수 없음.
- [ ] Mock 생성 결과가 실제 답변 저장이나 버전 복원에 영향을 주지 않음.
- [ ] 자소서 첨삭 6개 타입과 면접 질문 7개 타입 직렬화 테스트 통과.
- [ ] `be/.\mvnw.cmd verify` 통과, 토큰/외부 호출 불필요.

## 제외 및 다음 단계
- 제외: 실제 자소서 문장 생성·첨삭, 회사 사실 수집, 실시간 AI 대화/면접 API.
- 다음: [5단계 LLM 연결과 서비스 기능](./05-llm-integration.md).
