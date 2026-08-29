import unittest

from ai.src.common import (
    AiContext,
    AiRequest,
    AiResponse,
    MockAiProvider,
    PromptTemplate,
)


def make_request() -> AiRequest:
    return AiRequest(
        prompt=PromptTemplate(name="greeting", template="Hello {{ name }}"),
        context=AiContext(values={"name": "Hongik"}),
    )


class MockAiProviderTest(unittest.TestCase):
    def test_generate_returns_configured_response_and_records_call(self) -> None:
        provider = MockAiProvider(generate_content="generated")

        response = provider.generate(make_request())

        self.assertEqual(response.content, "generated")
        self.assertEqual(response.provider, "mock")
        self.assertEqual(provider.calls[0].operation, "generate")
        self.assertEqual(provider.calls[0].rendered_prompt, "Hello Hongik")

    def test_chat_uses_separate_response(self) -> None:
        provider = MockAiProvider(chat_content="chat reply")

        response = provider.chat(make_request())

        self.assertEqual(response.content, "chat reply")
        self.assertEqual(provider.calls[0].operation, "chat")

    def test_custom_handler_can_create_structured_response(self) -> None:
        def handler(request: AiRequest) -> AiResponse:
            return AiResponse(
                content="structured",
                structured_output={"prompt": request.rendered_prompt},
                provider="mock",
                model="custom-test",
            )

        provider = MockAiProvider(generate_handler=handler)

        response = provider.generate(make_request())

        self.assertEqual(response.structured_output, {"prompt": "Hello Hongik"})

    def test_reset_clears_call_history(self) -> None:
        provider = MockAiProvider()
        provider.generate(make_request())

        provider.reset()

        self.assertEqual(provider.calls, ())


if __name__ == "__main__":
    unittest.main()
