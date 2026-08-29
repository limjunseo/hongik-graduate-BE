"""Provider-neutral contracts shared by all AI domains."""

from .exceptions import AiError, InvalidPromptTemplateError, MissingPromptVariableError
from .mock_provider import MockAiProvider, MockCall
from .models import (
    AiContext,
    AiRequest,
    AiResponse,
    ChatMessage,
    ChatRole,
    OutputSchema,
    PromptTemplate,
    TokenUsage,
)
from .provider import AiProvider

__all__ = [
    "AiContext",
    "AiError",
    "AiProvider",
    "AiRequest",
    "AiResponse",
    "ChatMessage",
    "ChatRole",
    "InvalidPromptTemplateError",
    "MissingPromptVariableError",
    "MockAiProvider",
    "MockCall",
    "OutputSchema",
    "PromptTemplate",
    "TokenUsage",
]
