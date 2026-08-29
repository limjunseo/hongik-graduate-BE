from types import MappingProxyType
import unittest

from ai.src.common import (
    AiContext,
    AiRequest,
    ChatMessage,
    ChatRole,
    InvalidPromptTemplateError,
    MissingPromptVariableError,
    OutputSchema,
    PromptTemplate,
    TokenUsage,
)


class PromptTemplateTest(unittest.TestCase):
    def test_renders_context_values(self) -> None:
        prompt = PromptTemplate(
            name="experience.summary",
            template="Summarize {{ experience }} for {{ role }}.",
        )

        rendered = prompt.render(
            AiContext(values={"experience": "backend internship", "role": "developer"})
        )

        self.assertEqual(rendered, "Summarize backend internship for developer.")
        self.assertEqual(prompt.required_variables, ("experience", "role"))

    def test_reports_all_missing_variables(self) -> None:
        prompt = PromptTemplate(name="missing", template="{{ first }} {{ second }}")

        with self.assertRaises(MissingPromptVariableError) as raised:
            prompt.render({})

        self.assertIn("first, second", str(raised.exception))

    def test_rejects_invalid_variable_name(self) -> None:
        with self.assertRaises(InvalidPromptTemplateError):
            PromptTemplate(name="invalid", template="Hello {{ user.name }}")

    def test_rejects_unmatched_braces(self) -> None:
        with self.assertRaises(InvalidPromptTemplateError):
            PromptTemplate(name="invalid", template="Hello {{ name")


class ContractTest(unittest.TestCase):
    def test_request_copies_mutable_mappings(self) -> None:
        values = {"name": "before"}
        options = {"temperature": 0}
        request = AiRequest(
            prompt=PromptTemplate(name="hello", template="Hello {{ name }}"),
            context=AiContext(values=values),
            options=options,
        )

        values["name"] = "after"
        options["temperature"] = 1

        self.assertEqual(request.rendered_prompt, "Hello before")
        self.assertEqual(request.options["temperature"], 0)
        self.assertIsInstance(request.options, MappingProxyType)

    def test_context_supports_chat_history_and_copy_on_write(self) -> None:
        context = AiContext(
            values={"topic": "resume"},
            messages=(ChatMessage(ChatRole.USER, "Help me"),),
        )

        updated = context.with_values(language="Korean")

        self.assertEqual(updated.values["topic"], "resume")
        self.assertEqual(updated.values["language"], "Korean")
        self.assertEqual(updated.messages, context.messages)
        self.assertNotIn("language", context.values)

    def test_output_schema_and_token_usage(self) -> None:
        schema = OutputSchema(
            name="summary",
            schema={"type": "object", "required": ["summary"]},
        )
        usage = TokenUsage(input_tokens=10, output_tokens=5)

        self.assertEqual(schema.schema["type"], "object")
        self.assertEqual(usage.total_tokens, 15)


if __name__ == "__main__":
    unittest.main()
