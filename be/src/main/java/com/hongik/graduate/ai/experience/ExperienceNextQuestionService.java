package com.hongik.graduate.ai.experience;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** An adaptive preview for one missing experience field, without fabricating STAR details. */
@Service
public class ExperienceNextQuestionService {
    private final ExperienceSummaryService summaries;
    private final ExperienceQuickAnswerPreviewService quickAnswers;
    private final ExperienceCompletenessEvaluator completeness;
    private final ExperienceAiChatMessageRepository chats;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public ExperienceNextQuestionService(ExperienceSummaryService summaries,
            ExperienceQuickAnswerPreviewService quickAnswers,
            ExperienceCompletenessEvaluator completeness,
            ExperienceAiChatMessageRepository chats, AiGateway gateway, ObjectMapper mapper) {
        this.summaries = summaries; this.quickAnswers = quickAnswers;
        this.completeness = completeness; this.chats = chats;
        this.gateway = gateway; this.mapper = mapper;
    }

    public record Suggested(String question, String targetField, String reason,
            String schemaVersion) { }
    public record Preview(boolean completed, String question, String targetField,
            String reason, int completionPercent, List<String> missingFields,
            String provider, boolean isMock, String promptVersion, String correlationId) { }

    public Preview next(UUID member, UUID experience) {
        // Preview enforces ownership even when there is no saved structured summary.
        ExperienceSummary base = quickAnswers.preview(member, experience);
        try {
            base = summaries.get(member, experience);
        } catch (BusinessException ex) {
            if (ex.errorCode() != ErrorCode.EXPERIENCE_SUMMARY_NOT_FOUND) throw ex;
        }
        var score = completeness.evaluate(base);
        if (score.missingFields().isEmpty()) {
            return new Preview(true, null, null, null, 100, List.of(),
                    "static", false, null, null);
        }
        var stored = chats.findByExperienceIdOrderBySequenceNoAsc(experience);
        var recent = stored.subList(Math.max(0, stored.size() - 10), stored.size())
                .stream().filter(t -> t.role.equals("USER") || !t.mock)
                .map(t -> Map.of("role", t.role, "content", t.content)).toList();
        String source = mapper.writeValueAsString(Map.of(
                "summary", base, "missingFields", score.missingFields(),
                "nextRecommendedField", score.nextRecommendedField(),
                "fallbackQuestion", score.nextQuestion(), "chatTurns", recent));
        AiRequest request = new AiRequest(AiTaskType.EXPERIENCE_NEXT_QUESTION,
                "experience-next-question", "v1", new AiContext(Map.of("reference", source)),
                "experience-next-question-v1", UUID.randomUUID().toString(), List.of());
        AiResponse answer = gateway.generate(request);
        try {
            Suggested suggestion = mapper.readValue(answer.content(), Suggested.class);
            if (!"v1".equals(suggestion.schemaVersion()) || suggestion.question() == null
                    || suggestion.question().isBlank() || suggestion.question().length() > 300
                    || suggestion.reason() == null || suggestion.reason().length() > 300
                    || suggestion.targetField() == null
                    || !answer.isMock() && !score.missingFields().contains(suggestion.targetField())) {
                throw new IllegalArgumentException("unrelated question");
            }
            return new Preview(false, suggestion.question(),
                    answer.isMock() ? score.nextRecommendedField() : suggestion.targetField(),
                    suggestion.reason(), score.completionPercent(), score.missingFields(),
                    answer.provider(), answer.isMock(), request.promptVersion(),
                    answer.correlationId());
        } catch (RuntimeException exception) {
            throw new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                    "AI next-question response is invalid");
        }
    }
}
