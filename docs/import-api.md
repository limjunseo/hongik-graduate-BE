# 가져오기 API (PDF 및 Notion)

백엔드에서 PDF 원본 보관 및 Notion 블록 텍스트 수집을 담당한다. 경험 카드 변환/AI 분석은 이 PR의 범위가 아니다.

## 환경 설정
- `IMPORT_STORAGE_DIR`: PDF 보관 폴더, 기본 `./uploads/imports`; 서버 재시작과 배포 이후에도 유지되는 볼륨 권장
- `NOTION_IMPORT_TOKEN`: Notion Internal Integration Token. 서버에서만 보관하고 프론트에 노출하지 않는다.
- Notion에서 해당 페이지를 이 통합에 공유해야 한다.
- `NOTION_IMPORT_TOKEN` 미설정 시 Notion 가져오기 요청은 400으로 차단한다.

## 인증된 API
| Method | 경로 | 기능 |
| --- | --- | --- |
| POST (multipart/file) | `/api/imports/pdf` | 10MB 이하 PDF 검사·저장, 소유자별 DB 메타데이터 생성 |
| POST (JSON) | `/api/imports/notion` | `{"pageId":"노션-페이지-ID-또는-URL"}` 로 텍스트 수집 및 저장 |
| GET | `/api/imports` | 내 가져오기 목록 |
| GET | `/api/imports/{id}` | 내 가져오기 상세 |
| GET | `/api/imports/{id}/download` | 내가 업로드한 PDF 다운로드 |

## 제한 및 후속 과제
- 파일 확장자 및 PDF 헤더 검사, 서버 10MB 업로드 한도. PDF 내용 텍스트 추출은 하지 않는다.
- Notion은 페이지 내 블록의 `rich_text`를 수집하며 하위 블록도 탐색한다. API 요청 20회, 최대 문자 100,000자, 대기열 100개까지로 제한해 무한 수집을 막는다.
- Notion 실제 인증·권한 부여·텍스트 회수는 테스트용 통합 토큰이 없어 검증하지 못했다. 운영 전 연결 테스트 필수.
- 가져온 데이터는 유저별로 접근 제어한다. 파일 내용이 경험 정리 카드로 자동 반영되지는 않는다.
- 로컬 저장소는 다중 서버 환경에서는 공유되지 않으므로 운영에서는 S3 또는 공용 볼륨으로 전환해야 한다.
