package com.hongik.graduate.ai.experience;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Unverified AI preview. Only the existing PUT structured-summary may persist confirmed fields. */
@Service
public class ExperienceAiSummaryService {
    private final ExperienceQuickAnswerPreviewService quickAnswers;
    private final ExperienceAiChatMessageRepository chats;
    private final SkillTaxonomy skills;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public ExperienceAiSummaryService(ExperienceQuickAnswerPreviewService quickAnswers,
            ExperienceAiChatMessageRepository chats, SkillTaxonomy skills,
            AiGateway gateway, ObjectMapper mapper) {
        this.quickAnswers = quickAnswers;
        this.chats = chats;
        this.skills = skills;
        this.gateway = gateway;
        this.mapper = mapper;
    }

    public record Preview(ExperienceSummary summary, String provider, boolean isMock,
            String promptVersion, String correlationId, AiUsage usage,
            List<String> warnings) { }

    public Preview generate(UUID member, UUID id) {
        ExperienceSummary original = quickAnswers.preview(member, id); // Owns the experience.
        List<ExperienceAiChatMessage> conversation = chats.findByExperienceIdOrderBySequenceNoAsc(id)
                .stream().filter(turn -> turn.role.equals("USER") || !turn.mock).toList();
        boolean hasQuickAnswers = List.of(
                val(original.situation()), val(original.task()), val(original.action()),
                val(original.result()), val(original.role()), val(original.conflict()),
                val(original.learned())).stream().anyMatch(s -> !s.isBlank());
        if (!hasQuickAnswers && conversation.stream().noneMatch(t -> t.role.equals("USER"))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<ExperienceAiChatMessage> recent = conversation.subList(
                Math.max(0, conversation.size() - 20), conversation.size());
        String reference = mapper.writeValueAsString(Map.of(
                "quickAnswers", original, "chatTurns", recent.stream()
                        .map(t -> Map.of("role", t.role, "content", t.content)).toList()));
        AiRequest request = new AiRequest(AiTaskType.EXPERIENCE_SUMMARY,
                "experience-summary", "v1",
                new AiContext(Map.of("reference", reference,
                        "allowedSkills", mapper.writeValueAsString(skills.all()))),
                "experience-summary-v1", UUID.randomUUID().toString(), List.of());
        AiResponse response = gateway.generate(request);
        ExperienceSummaryService.SummaryInput input;
        try {
            input = mapper.readValue(response.content(), ExperienceSummaryService.SummaryInput.class);
            if (input == null || !ExperienceSummary.VERSION.equals(input.schemaVersion())
                    || input.evidenceSources() == null || !input.evidenceSources().isEmpty()) {
                throw new IllegalArgumentException("unverified citations");
            }
            ExperienceSummary result = new ExperienceSummary(
                    id, original.title(), original.tagCode(),
                    input.situation(), input.task(), input.action(), input.result(),
                    input.role(), input.conflict(), input.learned(),
                    skills.normalizeAll(input.skills()), input.keywords(), List.of(),
                    ExperienceSummary.VERSION, response.isMock() ? "MOCK" : "AI");
            return new Preview(result, response.provider(), response.isMock(),
                    request.promptVersion(), response.correlationId(), response.usage(),
                    response.isMock() ? List.of("MOCK_ONLY", "REQUIRES_USER_CONFIRMATION")
                            : List.of("UNVERIFIED_AI_PREVIEW", "REQUIRES_USER_CONFIRMATION"));
        } catch (RuntimeException e) {
            throw new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                    "AI experience summary is not valid");
        }
    }

    private static String val(String input) { return input == null ? "" : input; }
}