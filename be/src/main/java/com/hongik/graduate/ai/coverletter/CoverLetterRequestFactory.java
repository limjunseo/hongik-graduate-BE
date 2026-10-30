package com.hongik.graduate.ai.coverletter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Builds LLM-ready requests but NEVER calls a provider or saves an answer. */
@Component
public class CoverLetterRequestFactory {
    private final CoverLetterContextBuilder contexts;
    private final SupportStrategyContextBuilder strategies;
    private final ObjectMapper mapper;
    private final AiPromptTemplateRegistry prompts;

    public CoverLetterRequestFactory(CoverLetterContextBuilder contexts,
            SupportStrategyContextBuilder strategies, ObjectMapper mapper,
            AiPromptTemplateRegistry prompts) {
        this.contexts = contexts; this.strategies = strategies;
        this.mapper = mapper; this.prompts = prompts;
    }

    public AiRequest prepareDraft(UUID memberId, UUID coverId, UUID questionId,
            CoverLetterQuestionType type, List<UUID> selected, String correlationId) {
        CoverLetterContext context = contexts.build(memberId, coverId, questionId, type, selected);
        if (context.experiences().isEmpty())
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        return request(AiTaskType.COVER_LETTER_DRAFT, "coverletter-draft",
                "coverletter-draft-v1", correlationId, Map.of(
                    "context", serialize(context), "question", context.question()), List.of());
    }

    public AiRequest prepareRevision(UUID memberId, UUID coverId, UUID questionId,
            CoverLetterQuestionType type, List<UUID> selected, String original,
            RevisionType revisionType, String instruction, String correlationId) {
        CoverLetterContext context = contexts.build(memberId, coverId, questionId, type, selected);
        RevisionRequest revision;
        try {
            revision = new RevisionRequest(context, original, revisionType, instruction);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return revisionRequest(revision, List.of(), correlationId);
    }

    public AiRequest prepareEditingChat(EditingChatContext chat, String correlationId) {
        if (chat.priorTurns().isEmpty() || chat.priorTurns().get(chat.priorTurns().size()-1)
                .role() != AiMessage.Role.USER) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return revisionRequest(new RevisionRequest(chat.context(), chat.currentDraft(),
                chat.goal(), null), chat.priorTurns(), correlationId);
    }

    private AiRequest revisionRequest(RevisionRequest revision,
            List<AiMessage> messages, String correlationId) {
        var variables = Map.of(
                "context", serialize(revision.context()),
                "question", revision.context().question(),
                "original", revision.originalText(),
                "revisionType", revision.revisionType().name(),
                "instruction", revision.instruction() == null ? "No additional instruction"
                        : revision.instruction());
        return request(AiTaskType.COVER_LETTER_REVISION, "coverletter-revision",
                "coverletter-revision-v1", correlationId, variables, messages);
    }

    public AiRequest prepareSupportStrategy(UUID memberId, UUID coverId, UUID questionId,
            CoverLetterQuestionType type, List<UUID> selected, String correlationId) {
        var strategy = strategies.build(memberId, coverId, questionId, type, selected);
        return request(AiTaskType.JOB_STRATEGY, "support-strategy",
                "support-strategy-v1", correlationId,
                Map.of("context", serialize(strategy)), List.of());
    }

    private AiRequest request(AiTaskType type, String name, String outputSchema,
            String correlationId, Map<String, ?> variables, List<AiMessage> messages) {
        Map<String, Object> copied = new java.util.LinkedHashMap<>();
        variables.forEach(copied::put);
        AiContext data = new AiContext(copied);
        prompts.resolve(name, "v1").render(data);
        return new AiRequest(type, name, "v1", data, outputSchema, correlationId, messages);
    }

    private String serialize(Object data) {
        String json = mapper.writeValueAsString(data);
        if (json.length() > 150000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return json;
    }
}
