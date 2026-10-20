# 회원가입 및 온보딩 API

## 가입 흐름
- 이메일: `/api/auth/email/send` → `/api/auth/email/verify` → `/api/auth/signup` → `/api/auth/login`
- Google: `GET /oauth2/authorization/google` → Google 동의 → `GET /login/oauth2/code/google` (로컬 테스트용)
- 인증 수단에 관계없이 같은 `Member(UUID)`, JWT 및 `members` 테이블 사용
- 계정 생성 직후에는 가입 미완료. 기본 정보(나이·성별·학교)와 희망 직군을 등록해야 완료

## 프로필 API (Bearer Access Token 필수)
| Method | 경로 | 용도 |
| --- | --- | --- |
| GET | /api/members/me/profile | 정보와 가입 진행 상태 조회 |
| PUT | /api/members/me/profile/basic | `{"age":24,"gender":"FEMALE","school":"홍익대학교"}` |
| PUT | /api/members/me/profile/job | `{"desiredJob":"BACKEND"}` |

응답 래퍼: `{"success":true,"data":{...}}`

data에는 `memberId`, `email`, `age`, `gender`, `school`, `desiredJob`, `profileCompleted`, `signupStatus`, `onboardingStep`이 포함됨.

| onboardingStep | 프론트 처리 |
| --- | --- |
| BASIC | 기본 정보 입력 화면 |
| JOB | 직군 선택 화면 |
| COMPLETED | 가입 완료 후 홈 |

재로그인·새로고침 시 GET 프로필로 진행 상태 복원. 기본 정보 없이는 직군 등록 불가.
직군 및 성별 값은 현재 문자열로 검증하며, 실제 선택지 코드는 프론트/기획과 확정 필요.

## Google OAuth 로컬 연동
- 백엔드 로컬 포트: 8080
- Google Console 승인된 Redirect URI: `http://localhost:8080/login/oauth2/code/google`
- `be/.env-local`에 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `JWT_SECRET` 설정 (Git 제외)
- 콜백은 로컬 테스트용으로만 활성화됨. 현재 콜백 응답은 **JWT JSON을 직접 표시**하므로 외부 공유/실서비스 사용 금지. 실서비스는 안전한 프론트 전달 방식으로 변경 필요.
- 기존 `POST /api/auth/login/google`은 인가코드/redirectUri를 받아 공통 토큰을 발급함.
- 동일 이메일의 LOCAL/GOOGLE 계정은 자동 연결하지 않음.

## DB 및 배포
- `members`에 nullable `provider_id`(고유), `profile_age`, `profile_gender`, `profile_school`, `desired_job` 추가
- 로컬 H2는 `ddl-auto=update`. 운영 DB 변경 시 백업·스키마 확인 후 별도 마이그레이션 필요.
- 이메일 인증/로그인 및 프로필 단계 전환 테스트 통과. 프론트 화면 이동과 운영 OAuth 구성은 별도 연동 필요.
