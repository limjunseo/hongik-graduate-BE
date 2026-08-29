import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest

from ai.src.experience import (
    ExperienceQuestionPolicy,
    ExperienceTag,
    get_experience_question_policy,
)
from ai.src.experience.policy import _load_policy


class ExperiencePolicyTest(unittest.TestCase):
    def _write_policy(
        self,
        policy_dir: Path,
        tag: ExperienceTag,
        data: object,
    ) -> None:
        policy_path = policy_dir / f"{tag.value}.json"
        policy_path.write_text(
            json.dumps(data, ensure_ascii=False),
            encoding="utf-8",
        )

    def test_every_experience_tag_has_a_policy(self) -> None:
        for tag in ExperienceTag:
            with self.subTest(tag=tag):
                policy = get_experience_question_policy(tag)

                self.assertEqual(policy.tag, tag)
                self.assertTrue(policy.label.strip())
                self.assertTrue(policy.version.strip())
                self.assertGreater(len(policy.questions), 0)

    def test_collaboration_policy_content(self) -> None:
        policy = get_experience_question_policy(ExperienceTag.COLLABORATION)

        self.assertEqual(policy.label, "대인관계 및 협업 경험")
        self.assertIn("본인의 역할과 책임은 무엇이었나요?", policy.questions)
        self.assertIn(
            "갈등의 원인은 무엇이었고 어떻게 해결했나요?",
            policy.questions,
        )

    def test_job_policy_content(self) -> None:
        policy = get_experience_question_policy(ExperienceTag.JOB)

        self.assertIn(
            "그중 본인이 직접 설계하거나 구현한 부분은 무엇인가요?",
            policy.questions,
        )
        self.assertIn(
            "결과를 수치나 객관적인 지표로 표현할 수 있나요?",
            policy.questions,
        )

    def test_every_question_is_non_empty(self) -> None:
        for tag in ExperienceTag:
            for question in get_experience_question_policy(tag).questions:
                with self.subTest(tag=tag, question=question):
                    self.assertTrue(question.strip())

    def test_policies_do_not_have_duplicate_questions(self) -> None:
        for tag in ExperienceTag:
            questions = get_experience_question_policy(tag).questions
            with self.subTest(tag=tag):
                self.assertEqual(len(questions), len(set(questions)))

    def test_questions_are_returned_as_a_tuple(self) -> None:
        policy = get_experience_question_policy(ExperienceTag.VALUES)

        self.assertIsInstance(policy, ExperienceQuestionPolicy)
        self.assertIsInstance(policy.questions, tuple)

    def test_rejects_tag_mismatch(self) -> None:
        data = {
            "tag": "job",
            "label": "대인관계 및 협업 경험",
            "version": "1",
            "questions": ["질문"],
        }
        with TemporaryDirectory() as temporary_directory:
            policy_dir = Path(temporary_directory)
            self._write_policy(policy_dir, ExperienceTag.COLLABORATION, data)

            with self.assertRaisesRegex(ValueError, "tag mismatch"):
                _load_policy(ExperienceTag.COLLABORATION, policy_dir)

    def test_rejects_empty_questions(self) -> None:
        data = {
            "tag": "job",
            "label": "학업 및 직무 관련 경험",
            "version": "1",
            "questions": [],
        }
        with TemporaryDirectory() as temporary_directory:
            policy_dir = Path(temporary_directory)
            self._write_policy(policy_dir, ExperienceTag.JOB, data)

            with self.assertRaisesRegex(ValueError, "questions must not be empty"):
                _load_policy(ExperienceTag.JOB, policy_dir)

    def test_rejects_duplicate_questions(self) -> None:
        data = {
            "tag": "job",
            "label": "학업 및 직무 관련 경험",
            "version": "1",
            "questions": ["같은 질문", "같은 질문"],
        }
        with TemporaryDirectory() as temporary_directory:
            policy_dir = Path(temporary_directory)
            self._write_policy(policy_dir, ExperienceTag.JOB, data)

            with self.assertRaisesRegex(ValueError, "duplicates"):
                _load_policy(ExperienceTag.JOB, policy_dir)


if __name__ == "__main__":
    unittest.main()
