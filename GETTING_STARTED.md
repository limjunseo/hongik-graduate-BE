# 팀원 설치 및 로컬 실행 — Windows CMD 한 줄

Windows 10/11 **명령 프롬프트(CMD)** 에서 다음 한 줄을 실행하세요.

```cmd
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $p=Join-Path $env:TEMP 'stori-bootstrap.ps1'; Invoke-WebRequest 'https://raw.githubusercontent.com/limjunseo/hongik-graduate-FE/main/bootstrap.ps1' -UseBasicParsing -OutFile $p; & $p"
```

이 명령어는 [FE 저장소의 bootstrap.ps1](https://github.com/limjunseo/hongik-graduate-FE/blob/main/bootstrap.ps1)
을 HTTPS로 다운로드해 실행합니다. 스크립트 파일은 실행 전에 확인할 수 있습니다.

1. Git, Node.js 22 이상, JDK 17 이상 설치 여부를 확인하고, 부족하면
   공식 WinGet 패키지로 자동 설치합니다. 관리자 승인 또는 OS 정책상 예외가 필요할 수 있습니다.
2. `%USERPROFILE%\stori-workspace\hongik-graduate-FE` 및
   `%USERPROFILE%\stori-workspace\hongik-graduate-BE` 저장소를
   내려받거나 로컬 변경사항이 없는 main 브랜치만 안전하게 업데이트합니다.
3. FE npm 의존성, BE Maven Wrapper, JWT 시크릿, 로컬 파일형 H2 DB를 준비합니다.
4. FE(5173) + BE(기본 8080, 충돌하면 8081~8090)를 각각 실행하고 브라우저를 엽니다.

- 실행 주소: **http://localhost:5173**
- 다시 시작: 같은 명령어를 반복하거나 FE의 `start-local.cmd` 더블클릭
- 백엔드 파일 DB: `be/data/hongik-graduate.mv.db` (Git 제외, 팀원별 로컬 데이터)
- AI API Key: `be/.env-local`에 개인 키를 입력; 프론트 코드에 입력 금지
- 이메일 가입: 별도 SMTP 설정 없이는 메일 발송 불가
- 오류 및 상세 옵션: [FE README](https://github.com/limjunseo/hongik-graduate-FE#readme)

**참고:** WinGet 없는 오래된 Windows 환경, 회사·학교 설치 정책, UAC 승인 거부 시
완전 무인 설치는 불가능할 수 있습니다. 설치 파일을 몰래 실행하거나 권한을 우회하지 않습니다.
