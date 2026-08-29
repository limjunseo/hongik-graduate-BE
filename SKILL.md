# Repository Structure Guide

이 문서는 `hongik-graduate-BE` 저장소의 기본 디렉터리 구조와 사용 원칙을 설명합니다.

현재 단계에서는 세부 도메인이나 프레임워크 구조를 미리 확정하지 않고, Backend와 AI 영역을 구분하는 최소 구조만 유지합니다.

## Directory Structure

```text
.
├── be/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   └── config/
├── ai/
│   ├── src/
│   ├── prompts/
│   ├── config/
│   └── tests/
├── docs/
├── scripts/
├── .github/
├── .env.example
├── .gitignore
└── SKILL.md
```

## be/

일반 백엔드 애플리케이션 코드를 관리합니다.

향후 회원/인증, 경험 관리, 자기소개서, 채용공고, 지원 현황, 면접, DB 접근, 외부 API 연동 등의 기능이 구현될 수 있습니다.

현재는 `controller`, `service`, `repository` 같은 세부 구현 폴더를 미리 만들지 않습니다. 기술 스택과 도메인 설계가 확정된 뒤 필요한 구조만 추가합니다.

## ai/

AI 관련 코드와 설정을 관리합니다.

- `src/`: LLM 호출, 응답 처리, 문서 분석 등 AI 실행 코드
- `prompts/`: 프롬프트 관리
- `config/`: 모델 및 AI 관련 설정
- `tests/`: AI 관련 테스트

AI 영역은 현재 같은 저장소 안에서 관리하되, 향후 독립 서비스로 분리하기 쉽도록 백엔드 비즈니스 로직과 책임을 구분합니다.

## docs/

API 명세, ERD, 아키텍처, 기능 명세, 개발 규칙 등의 문서를 관리합니다.

## scripts/

개발 환경 구성, 데이터 초기화, 배포 및 반복 작업 자동화에 필요한 스크립트를 관리합니다.

## .github/

Pull Request Template, Issue Template, GitHub Actions, CODEOWNERS 등 GitHub 관련 설정을 관리합니다.

## Environment Variables

실제 비밀 값은 저장소에 커밋하지 않습니다. 필요한 환경변수 이름만 `.env.example`에 기록하고 실제 `.env` 파일은 `.gitignore`에서 제외합니다.

## Structure Principles

1. 현재 필요하지 않은 구현 폴더를 과도하게 미리 생성하지 않습니다.
2. Backend와 AI 코드의 책임을 구분합니다.
3. 새로운 기능은 기존 디렉터리의 책임에 맞게 추가합니다.
4. API Key, 비밀번호 등 비밀 값은 Git에 커밋하지 않습니다.
5. 프로젝트 구조가 크게 변경되면 이 문서를 함께 수정합니다.
