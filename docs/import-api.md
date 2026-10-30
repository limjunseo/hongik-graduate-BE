# Pofit 경험 관리 — 자료 가져오기

## 현재 MVP 구현
경험 정리에서 PDF 또는 공개 URL의 자료를 가져온다. Notion 전용 API 및 공통 토큰은 제거했다.

| 종류 | API | 저장 내용 |
| --- | --- | --- |
| PDF | POST /api/imports/pdf (multipart file) | PDF 원본을 UUID 파일명으로 저장, 가져오기 메타데이터 기록 |
| URL | POST /api/imports/url, JSON {"url":"https://example.com/post"} | HTML 제목과 본문 텍스트 및 원본 주소 저장 |
| 목록·상세 | GET /api/imports, GET /api/imports/{id} | 본인 자료만 반환 |
| PDF 다운로드 | GET /api/imports/{id}/download | 본인 PDF만 다운로드 |

PDF는 10MB 제한, 확장자·헤더 검사. 기본 파일 경로는 ./uploads/imports이며 배포할 때 IMPORT_STORAGE_DIR로 영속 볼륨을 지정한다. PDF 텍스트 추출과 경험 카드 자동 변환은 이번 범위 밖이다.

URL 수집은 공개 HTTP(S) HTML에 한정한다. Jsoup으로 제목과 본문 텍스트를 추출한다. 응답 1MB, 본문 10만 자, 연결 4초, 요청 8초 제한을 둔다. 리다이렉트는 따르지 않는다. 로그인 필요 페이지 및 JavaScript 렌더링만 하는 페이지는 실패하거나 일부만 수집될 수 있다.

## 결정 이유
가져오기는 Notion 전용이 아니라 블로그, 개인 사이트 등 다양한 경험 출처를 수용해야 한다. 사용자별 Notion OAuth를 필수로 두면 특정 서비스에 종속되고, 연결 화면·토큰 보관·권한 및 해제 구현이 필요해 MVP 범위가 커진다. 비공개 Notion 페이지까지 읽는 것은 현재 필수 요건이 아니므로 PDF와 공개 URL을 공통 입력 수단으로 채택했다. 공개 Notion 게시 페이지도 일반 웹페이지처럼 접근 가능할 때만 수집할 수 있다.

## 안전성과 운영 한계
- JWT 인증 및 자료별 소유권 검사 적용.
- HTTP(S) 주소만 허용하며 localhost, 내부 IP 등 목적지 주소를 DNS 조회 결과 기준으로 차단한다. 포트는 기본값이나 80/443만 허용하고 리다이렉트는 따르지 않는다.
- **DNS 검사 후 연결 시점에 주소가 바뀌는 DNS 재바인딩 등의 SSRF 위험을 현재 애플리케이션 검사만으로 완전히 방어할 수는 없다.** 운영에서는 네트워크 egress 차단 정책이나 고정 해석 IP 프록시를 추가하고 보안 테스트를 완료해야 한다.
- 사이트별 robots 정책과 이용약관을 준수하며 인증·접근제어를 우회하지 않는다. 추출 실패 때 사용자가 직접 텍스트를 입력하는 UI는 별도 작업이다.
- PDF 저장은 단일 서버 로컬 디스크다. 운영 시 영속 볼륨 또는 S3 등 객체 스토리지가 필요하다.

## 이후 변경·확장 가능성
자료 출처를 ImportedSource.sourceType (PDF/URL)으로 분리하고 원본 URL은 sourceUrl, 수집 본문은 collectedText에 보관한다. Notion OAuth, 블로그 제공 API 등 출처별 수집기를 나중에 추가하더라도 기존 목록·상세 조회 API는 유지하는 방향으로 확장한다.

비공개 Notion 페이지 지원이 실제 요구로 확인되면 **선택적 연동**으로 OAuth 연결, 회원별 암호화 토큰 저장, 권한 관리·연결 해제를 추가할 수 있다. 이는 현재 공개 URL 가져오기를 대체하지 않는다. PDF 텍스트 추출과 경험 카드 자동 구성도 별도 단계로 다룬다.

## 검증 범위
PDF 업로드·다운로드·접근 제한, URL 내부 주소 차단을 테스트한다. 공개 Notion 및 다양한 블로그 실제 본문 추출 성공률, DNS 재바인딩 방어 및 사이트별 파싱 품질은 추가 검증이 필요하다.

## 2단계: 근거 문단 후보 추출 (구현)
- `GET /api/imports/{id}/experience-candidates` (JWT): 본인 자료에서 문단 후보 반환. 기존 업로드/URL 수집 API는 수정하지 않는다.
- URL은 기존 `ImportedSource.collectedText`에서, PDF는 보관한 원본 파일을 PDFBox 3.0.5로 읽어 페이지 단위로 추출한다. 원본 PDF 자체를 변경하지 않는다.
- 후보는 `sourceId`, `sourceType`, `location`(예: `page:1:paragraph:1` 또는 `url:paragraph:1`), `quote`, `status=REVIEW_REQUIRED`를 가진다. 내용은 원문 일부로 한정하며 S/T/A/R을 임의 구성하지 않는다.
- 총 추출은 최대 60페이지/6만 글자/100문단 후보, 문단 인용은 1000자 이하로 제한한다. OCR·로그인 필요 Notion·시맨틱 추출은 미지원; 텍스트 없는 PDF는 `NO_TEXT`.
- 구조화 경험 저장 시 `evidenceSources[]`는 동일 회원이 소유한 자료의 동일 `location`·`quote` 후보와 일치해야 한다. 사용자 승인을 거친 `PUT /api/experiences/{id}/structured-summary`에서만 실제 경험 데이터에 반영한다.
- PDFBox 추가 의존성은 운영 배포 시 CVE 점검·리소스 제한·운영 DB 마이그레이션 검토가 필요하다. PDF 파싱이 실패하면 원본 파일은 계속 보존한다.
