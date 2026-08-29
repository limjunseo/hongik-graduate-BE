# AI

외부 LLM SDK에 의존하지 않는 AI 공통 계약과 도메인별 코드를 관리합니다.

## 구조

```text
ai/
├── src/
│   ├── common/       # 공통 계약, Provider 인터페이스, Mock Provider
│   ├── experience/
│   ├── job/
│   ├── coverletter/
│   └── interview/
├── prompts/          # 도메인별 프롬프트 파일
├── config/
└── tests/
```

## 공통 계약

- `AiRequest`, `AiResponse`: Provider가 주고받는 공통 입출력
- `PromptTemplate`: `{{ variable }}` 형식의 버전 관리 가능한 프롬프트
- `AiContext`: 프롬프트 값과 채팅 메시지
- `OutputSchema`: 구조화 출력 명세
- `AiProvider`: `generate()`, `chat()` 인터페이스
- `MockAiProvider`: 외부 API 없이 사용할 수 있는 결정론적 구현

새로운 실제 Provider는 `AiProvider`를 구현하고, 외부 SDK의 타입이나 응답을
공통 계약 밖으로 노출하지 않습니다.

## 사용 예시

```python
from ai.src.common import AiContext, AiRequest, PromptTemplate
from ai.src.common.mock_provider import MockAiProvider

request = AiRequest(
    prompt=PromptTemplate(
        name="experience.summary",
        template="{{ experience }} 경험을 한 문장으로 요약하세요.",
    ),
    context=AiContext(values={"experience": "백엔드 인턴"}),
)

provider = MockAiProvider(generate_content="백엔드 서비스 개발 경험이 있습니다.")
response = provider.generate(request)
```

## 테스트

저장소 루트에서 실행합니다.

```bash
python -m unittest discover -s ai/tests -v
```

테스트와 현재 구현은 Python 표준 라이브러리만 사용합니다.
