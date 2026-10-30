package com.hongik.graduate.ai.interview;

import java.util.List;
import java.util.Map;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Pure request assembly; interview sessions and chat storage are stage-5 work. */
@Component
public class InterviewRequestFactory {
    private final AiPromptTemplateRegistry prompts;
    private final ObjectMapper mapper;

    public InterviewRequestFactory(AiPromptTemplateRegistry prompts, ObjectMapper mapper) {
        this.prompts = prompts; this.mapper = mapper;
    }

    public AiRequest prepareQuestion(InterviewContext context, String correlationId) {
        return request(context, "interview-question", "interview-question-v1",
                context.priorTurns(), correlationId);
    }

    public AiRequest prepareFeedback(InterviewContext context, String correlationId) {
        if (context.previousAnswer() == null || context.previousAnswer().isBlank()) {
            throw new IllegalArgumentException("previous interview answer is required");
        }
        return request(context, "interview-feedback", "interview-feedback-v1",
                context.priorTurns(), correlationId);
    }

    private AiRequest request(InterviewContext context, String template, String schemaId,
            List<AiMessage> turns, String correlationId) {
        String serialized = mapper.writeValueAsString(context);
        if (serialized.length() > 150000) {
            throw new IllegalArgumentException("interview context too large");
        }
        AiContext variables = new AiContext(Map.of(
                "context", serialized, "questionType", context.questionType().name()));
        prompts.resolve(template, "v1").render(variables);
        if (!turns.isEmpty() && turns.get(turns.size() - 1).role() != AiMessage.Role.USER) {
            throw new IllegalArgumentException("interview history must end with a user answer");
        }
        AiTaskType task = template.equals("interview-feedback")
                ? AiTaskType.INTERVIEW_FEEDBACK : AiTaskType.INTERVIEW_QUESTION;
        return new AiRequest(task, template, "v1",
                variables, schemaId, correlationId, turns);
    }
}
