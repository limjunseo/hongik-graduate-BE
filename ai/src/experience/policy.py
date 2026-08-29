"""Load and validate static experience question policies."""

from __future__ import annotations

import json
from pathlib import Path
from typing import Any

from .models import ExperienceQuestionPolicy, ExperienceTag


_POLICY_DIR = Path(__file__).resolve().parents[2] / "prompts" / "experience"
_REQUIRED_FIELDS = ("tag", "label", "version", "questions")


def _invalid_policy(tag: ExperienceTag, policy_path: Path, reason: str) -> ValueError:
    return ValueError(
        f"Invalid experience policy for {tag.value!r} at {str(policy_path)!r}: "
        f"{reason}"
    )


def _load_policy(
    tag: ExperienceTag,
    policy_dir: Path,
) -> ExperienceQuestionPolicy:
    """Load one policy from the supplied directory and validate its JSON payload."""

    policy_path = policy_dir / f"{tag.value}.json"
    try:
        with policy_path.open(encoding="utf-8") as policy_file:
            data: Any = json.load(policy_file)
    except FileNotFoundError:
        raise FileNotFoundError(
            f"Experience policy for {tag.value!r} not found: {policy_path}"
        ) from None
    except json.JSONDecodeError as error:
        raise _invalid_policy(tag, policy_path, f"invalid JSON ({error.msg})") from error

    if not isinstance(data, dict):
        raise _invalid_policy(tag, policy_path, "top-level value must be an object")

    missing_fields = [field for field in _REQUIRED_FIELDS if field not in data]
    if missing_fields:
        raise _invalid_policy(
            tag,
            policy_path,
            f"missing required field(s): {', '.join(missing_fields)}",
        )

    if data["tag"] != tag.value:
        raise _invalid_policy(tag, policy_path, "tag mismatch")

    label = data["label"]
    if not isinstance(label, str) or not label.strip():
        raise _invalid_policy(tag, policy_path, "label must be a non-empty string")

    version = data["version"]
    if not isinstance(version, str) or not version.strip():
        raise _invalid_policy(tag, policy_path, "version must be a non-empty string")

    questions = data["questions"]
    if not isinstance(questions, list):
        raise _invalid_policy(tag, policy_path, "questions must be a list")
    if not questions:
        raise _invalid_policy(tag, policy_path, "questions must not be empty")

    for index, question in enumerate(questions):
        if not isinstance(question, str):
            raise _invalid_policy(
                tag,
                policy_path,
                f"question at index {index} must be a string",
            )
        if not question.strip():
            raise _invalid_policy(
                tag,
                policy_path,
                f"question at index {index} must not be empty",
            )

    if len(questions) != len(set(questions)):
        raise _invalid_policy(tag, policy_path, "questions must not contain duplicates")

    return ExperienceQuestionPolicy(
        tag=tag,
        label=label,
        version=version,
        questions=tuple(questions),
    )


def get_experience_question_policy(
    tag: ExperienceTag,
) -> ExperienceQuestionPolicy:
    """Return the validated static question policy for the given tag."""

    return _load_policy(tag, _POLICY_DIR)
