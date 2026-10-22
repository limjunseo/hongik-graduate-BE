# 사용자 설정 API

GET /api/members/me/settings: 사용자 설정 조회. 미저장 회원은 테마 SYSTEM, 알림 수신 true 반환.

PATCH /api/members/me/settings: 제공한 필드만 수정. 예시: {"theme":"DARK","notificationsEnabled":false}.

테마는 SYSTEM / LIGHT / DARK로 제한하고, 사용자별 member_settings 행에 값을 보관한다. 기존 프로필 조회·수정과 분리한다.

테마 표시 및 알림 수신 정책 반영은 프론트·알림 시스템 연동 단계에서 수행한다. 세부 설정 항목은 아직 확정되지 않아 최소 기본 설정만 구현했으며 필요 시 DTO·엔티티 확장이 가능하다.

운영 DB 연결·마이그레이션·배포는 담당 범위에서 제외한다.
