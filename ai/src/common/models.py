"""Provider-neutral request, response, prompt, and context models."""

from __future__ import annotations

from dataclasses import dataclass, field
from enum import Enum
import re
from types import MappingProxyType
from typing import Any, Mapping

from .exceptions import InvalidPromptTemplateError, MissingPromptVariableError


_PLACEHOLDER_PATTERN = re.compile(r"{{\s*([^{}]+?)\s*}}")
_VARIABLE_NAME_PATTERN = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def _immutable_mapping(value: Mapping[str, Any]) -> Mapping[str, Any]:
    """Copy a mapping so callers cannot mutate a frozen contract indirectly."""

    return MappingProxyType(dict(value))


class ChatRole(str, Enum):
    """A vendor-neutral chat message role."""

    SYSTEM = "system"
    USER = "user"
    ASSISTANT = "assistant"


@dataclass(frozen=True, slots=True)
class ChatMessage:
    """One message in a provider-neutral conversation."""

    role: ChatRole
    content: str

    def __post_init__(self) -> None:
        if not self.content.strip():
            raise ValueError("Chat message content must not be empty.")


@dataclass(frozen=True, slots=True)
class AiContext:
    """Values and optional conversation history used to build an AI request."""

    values: Mapping[str, Any] = field(default_factory=dict)
    messages: tuple[ChatMessage, ...] = ()

    def __post_init__(self) -> None:
        object.__setattr__(self, "values", _immutable_mapping(self.values))
        object.__setattr__(self, "messages", tuple(self.messages))

    def with_values(self, **values: Any) -> AiContext:
        """Return a new context with the supplied values merged in."""

        merged = dict(self.values)
        merged.update(values)
        return AiContext(values=merged, messages=self.messages)


@dataclass(frozen=True, slots=True)
class PromptTemplate:
    """A versioned prompt using ``{{ variable }}`` placeholders."""

    name: str
    template: str
    version: str = "1"
    description: str | None = None

    def __post_init__(self) -> None:
        if not self.name.strip():
            raise ValueError("Prompt template name must not be empty.")
        if not self.template.strip():
            raise ValueError("Prompt template content must not be empty.")

        invalid_names = [
            name
            for name in _PLACEHOLDER_PATTERN.findall(self.template)
            if not _VARIABLE_NAME_PATTERN.fullmatch(name)
        ]
        if invalid_names:
            raise InvalidPromptTemplateError(
                f"Invalid prompt variable name: {invalid_names[0]!r}"
            )
        unmatched_braces = _PLACEHOLDER_PATTERN.sub("", self.template)
        if "{{" in unmatched_braces or "}}" in unmatched_braces:
            raise InvalidPromptTemplateError("Prompt template contains unmatched braces.")

    @property
    def required_variables(self) -> tuple[str, ...]:
        """Return unique placeholder names in their first-seen order."""

        return tuple(dict.fromkeys(_PLACEHOLDER_PATTERN.findall(self.template)))

    def render(self, context: AiContext | Mapping[str, Any]) -> str:
        """Render this template using context values."""

        values = context.values if isinstance(context, AiContext) else context
        missing = [name for name in self.required_variables if name not in values]
        if missing:
            raise MissingPromptVariableError(
                f"Missing prompt variables for {self.name!r}: {', '.join(missing)}"
            )

        def replace(match: re.Match[str]) -> str:
            return str(values[match.group(1)])

        return _PLACEHOLDER_PATTERN.sub(replace, self.template)


@dataclass(frozen=True, slots=True)
class OutputSchema:
    """A provider-neutral description of the expected structured output."""

    name: str
    schema: Mapping[str, Any]
    description: str | None = None
    strict: bool = True

    def __post_init__(self) -> None:
        if not self.name.strip():
            raise ValueError("Output schema name must not be empty.")
        if not self.schema:
            raise ValueError("Output schema must not be empty.")
        object.__setattr__(self, "schema", _immutable_mapping(self.schema))


@dataclass(frozen=True, slots=True)
class AiRequest:
    """Input contract accepted by every AI provider."""

    prompt: PromptTemplate
    context: AiContext = field(default_factory=AiContext)
    output_schema: OutputSchema | None = None
    options: Mapping[str, Any] = field(default_factory=dict)
    metadata: Mapping[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        object.__setattr__(self, "options", _immutable_mapping(self.options))
        object.__setattr__(self, "metadata", _immutable_mapping(self.metadata))

    @property
    def rendered_prompt(self) -> str:
        """Render the request prompt against its context."""

        return self.prompt.render(self.context)


@dataclass(frozen=True, slots=True)
class TokenUsage:
    """Optional token accounting reported by a provider."""

    input_tokens: int = 0
    output_tokens: int = 0

    def __post_init__(self) -> None:
        if self.input_tokens < 0 or self.output_tokens < 0:
            raise ValueError("Token counts must not be negative.")

    @property
    def total_tokens(self) -> int:
        return self.input_tokens + self.output_tokens


@dataclass(frozen=True, slots=True)
class AiResponse:
    """Output contract returned by every AI provider."""

    content: str
    provider: str
    model: str
    structured_output: Any | None = None
    finish_reason: str | None = None
    usage: TokenUsage = field(default_factory=TokenUsage)
    metadata: Mapping[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        if not self.provider.strip():
            raise ValueError("Provider name must not be empty.")
        if not self.model.strip():
            raise ValueError("Model name must not be empty.")
        object.__setattr__(self, "metadata", _immutable_mapping(self.metadata))
