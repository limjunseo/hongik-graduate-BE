"""Interface implemented by all AI providers."""

from __future__ import annotations

from abc import ABC, abstractmethod

from .models import AiRequest, AiResponse


class AiProvider(ABC):
    """Vendor-neutral interface for text generation and conversations."""

    @abstractmethod
    def generate(self, request: AiRequest) -> AiResponse:
        """Generate one response from a rendered prompt."""

    @abstractmethod
    def chat(self, request: AiRequest) -> AiResponse:
        """Generate one response using the context's conversation messages."""
