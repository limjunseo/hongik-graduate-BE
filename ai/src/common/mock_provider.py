"""Deterministic provider for local development and tests."""

from __future__ import annotations

from collections.abc import Callable
from dataclasses import dataclass
from typing import Literal

from .models import AiRequest, AiResponse, TokenUsage
from .provider import AiProvider


MockHandler = Callable[[AiRequest], AiResponse]


@dataclass(frozen=True, slots=True)
class MockCall:
    """A recorded invocation of the mock provider."""

    operation: Literal["generate", "chat"]
    request: AiRequest
    rendered_prompt: str


class MockAiProvider(AiProvider):
    """Return deterministic responses without calling an external LLM."""

    def __init__(
        self,
        *,
        generate_content: str = "mock generated response",
        chat_content: str = "mock chat response",
        generate_handler: MockHandler | None = None,
        chat_handler: MockHandler | None = None,
    ) -> None:
        self._generate_content = generate_content
        self._chat_content = chat_content
        self._generate_handler = generate_handler
        self._chat_handler = chat_handler
        self._calls: list[MockCall] = []

    @property
    def calls(self) -> tuple[MockCall, ...]:
        """Return a read-only snapshot of calls made to this provider."""

        return tuple(self._calls)

    def reset(self) -> None:
        """Clear recorded calls."""

        self._calls.clear()

    def generate(self, request: AiRequest) -> AiResponse:
        rendered_prompt = request.rendered_prompt
        self._calls.append(MockCall("generate", request, rendered_prompt))
        if self._generate_handler is not None:
            return self._generate_handler(request)
        return self._response(self._generate_content)

    def chat(self, request: AiRequest) -> AiResponse:
        rendered_prompt = request.rendered_prompt
        self._calls.append(MockCall("chat", request, rendered_prompt))
        if self._chat_handler is not None:
            return self._chat_handler(request)
        return self._response(self._chat_content)

    @staticmethod
    def _response(content: str) -> AiResponse:
        return AiResponse(
            content=content,
            provider="mock",
            model="mock-v1",
            finish_reason="stop",
            usage=TokenUsage(),
        )
