"""Exceptions raised by the provider-neutral AI layer."""


class AiError(Exception):
    """Base exception for AI-layer failures."""


class InvalidPromptTemplateError(AiError, ValueError):
    """Raised when a prompt template has an invalid placeholder."""


class MissingPromptVariableError(AiError, KeyError):
    """Raised when prompt rendering is missing a required context value."""
