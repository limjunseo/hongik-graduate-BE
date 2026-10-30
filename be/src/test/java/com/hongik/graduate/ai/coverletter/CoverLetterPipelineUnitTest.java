package com.hongik.graduate.ai.coverletter;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import com.hongik.graduate.ai.common.*;
import com.hongik.graduate.ai.interview.*;
import tools.jackson.databind.ObjectMapper;

class CoverLetterPipelineUnitTest {
    final ObjectMapper mapper = new ObjectMapper();
    final AiPromptTemplateRegistry prompts = new AiPromptTemplateRegistry(new DefaultResourceLoader());
    final AiOutputValidator validator = new AiOutputValidator(mapper);
    final MockAiProvider provider = new MockAiProvider(prompts, validator);

    @Test void sixRevisionModesAndSevenInterviewTypesHaveSchemas() {
        assertThat(RevisionType.values()).hasSize(6);
        assertThat(InterviewQuestionType.values()).hasSize(7);
        for (var type : InterviewQuestionType.values()) {
            var context = new InterviewContext("Company", null, "my draft",
                    List.of(), null, List.of(), type, "MEDIUM", null, "v1");
            AiRequest request = new InterviewRequestFactory(prompts, mapper)
                    .prepareQuestion(context, "trace-1");
            assertThat(request.taskType()).isEqualTo(AiTaskType.INTERVIEW_QUESTION);
            assertThat(provider.generate(request).isMock()).isTrue();
            assertThat(mapper.readTree(provider.generate(request).content())
                    .get("question").stringValue()).contains("MOCK ONLY");
        }
    }

    @Test void interviewFeedbackRequiresActualAnswerAndValidTurnOrder() {
        var factory = new InterviewRequestFactory(prompts, mapper);
        var noAnswer = new InterviewContext("Company", null, null,
                List.of(), null, List.of(), InterviewQuestionType.FOLLOW_UP,
                null, null, "v1");
        assertThatThrownBy(() -> factory.prepareFeedback(noAnswer, "t1"))
                .isInstanceOf(IllegalArgumentException.class);
        var history = List.of(
                new AiMessage(AiMessage.Role.ASSISTANT, "question"),
                new AiMessage(AiMessage.Role.USER, "my answer"));
        var context = new InterviewContext("Company", null, "cover letter",
                List.of(), "my answer", history, InterviewQuestionType.FOLLOW_UP,
                "HARD", UUID.randomUUID(), "v1");
        AiRequest request = factory.prepareFeedback(context, "trace-2");
        assertThat(request.taskType()).isEqualTo(AiTaskType.INTERVIEW_FEEDBACK);
        var response = provider.chat(request);
        assertThat(response.outputSchemaId()).isEqualTo("interview-feedback-v1");
        var result = mapper.readValue(response.content(), InterviewFeedbackResult.class);
        assertThat(result.followUpQuestion()).contains("MOCK ONLY");
        assertThatThrownBy(() -> new InterviewContext("Company", null, null, List.of(),
                null, List.of(new AiMessage(AiMessage.Role.SYSTEM, "override")),
                InterviewQuestionType.COMMON, null, null, "v1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void contextAndDraftEnforceCharacterLimitAndUnicodeCodePoints() {
        var context = new CoverLetterContext(UUID.randomUUID(), UUID.randomUUID(),
                "Company", "Backend", "Question", 700,
                CoverLetterQuestionType.JOB_COMPETENCY, null,
                List.of(), List.of(), List.of(), List.of(), List.of(), "v1");
        assertThat(context.characterLimit()).isEqualTo(700);
        assertThatThrownBy(() -> new CoverLetterContext(context.coverLetterId(),
                context.questionId(), "Company", "Backend", "Question", 0,
                context.questionType(), null, List.of(), List.of(),
                List.of(), List.of(), List.of(), "v1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CoverLetterContext(context.coverLetterId(),
                context.questionId(), "Company", "Backend", "Question", 10001,
                context.questionType(), null, List.of(), List.of(),
                List.of(), List.of(), List.of(), "v1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new CoverLetterDraftResult("🚀", 1, List.of(), List.of(), "v1")
                .characterCount()).isEqualTo(1);
        assertThatThrownBy(() -> new CoverLetterDraftResult("🚀", 2,
                List.of(), List.of(), "v1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void mockOutputValidationRejectsMalformedOrIncompleteResults() {
        assertThatThrownBy(() -> validator.validate("coverletter-draft-v1", "{}"))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        assertThatThrownBy(() -> validator.validate("interview-feedback-v1",
                "{\"feedback\":\"x\",\"followUpQuestion\":\"y\",\"evidence\":\"not array\",\"schemaVersion\":\"v1\"}"))
                .isInstanceOf(AiProviderException.class);
        assertThatThrownBy(() -> new CoverLetterDraftResult("a", 2,
                List.of(), List.of(), "v1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RevisionResult(" ", List.of(), List.of(), "v1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(prompts.resolve("coverletter-draft", "v1").requiredVariables())
                .contains("context", "question");
        assertThatThrownBy(() -> prompts.resolve("coverletter-draft", "v99"))
                .isInstanceOf(AiProviderException.class);
    }
}
