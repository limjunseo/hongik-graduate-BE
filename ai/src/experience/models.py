"""Models for static experience question policies."""

from dataclasses import dataclass
from enum import Enum


class ExperienceTag(str, Enum):
    """Supported categories of experience."""

    VALUES = "values"
    PERSONALITY = "personality"
    JOB = "job"
    COLLABORATION = "collaboration"
    CHALLENGE = "challenge"


@dataclass(frozen=True, slots=True)
class ExperienceQuestionPolicy:
    """Immutable questions associated with an experience category."""

    tag: ExperienceTag
    label: str
    version: str
    questions: tuple[str, ...]
