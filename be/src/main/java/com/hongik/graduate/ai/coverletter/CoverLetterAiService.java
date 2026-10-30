package com.hongik.graduate.ai.coverletter;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestionRepository;
import com.hongik.graduate.ai.common.AiGateway;
import com.hongik.graduate.ai.common.AiProviderException;
import com.hongik.graduate.ai.common.AiResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Preview-only AI workflows. The existing answer PATCH remains the sole persistence path. */
@Service
public class CoverLetterAiService {
    private final CoverLetterRequestFactory requests;
    private final CoverLetterQuestionRepository questions;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public CoverLetterAiService(CoverLetterRequestFactory requests,
            CoverLetterQuestionRepository questions, AiGateway gateway, ObjectMapper mapper) {
        this.requests = requests;
        this.questions = questions;
        this.gateway = gateway;
        this.mapper = mapper;
    }

    public record DraftInput(CoverLetterQuestionType questionType, List<UUID> experienceIds) { }
    public record RevisionInput(CoverLetterQuestionType questionType, List<UUID> experienceIds,
            String originalText, RevisionType revisionType, String instruction) { }
    public record Preview(String text, int characterCount, int characterLimit, List<String> warnings,
            List<UUID> usedExperienceIds, List<String> changeReasons,
            String provider, boolean isMock, String promptVersion, String correlationId) { }

    public Preview draft(UUID member, UUID cover, UUID question, DraftInput input) {
        if (input == null || input.questionType() == null || input.experienceIds() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var request = requests.prepareDraft(member, cover, question, input.questionType(),
                input.experienceIds(), UUID.randomUUID().toString());
        AiResponse response = gateway.generate(request);
        CoverLetterDraftResult draft;
        try {
            draft = mapper.readValue(response.content(), CoverLetterDraftResult.class);
        } catch (RuntimeException e) {
            throw invalidOutput();
        }
        if (!new HashSet<>(input.experienceIds()).containsAll(draft.usedExperienceIds())) {
            throw invalidOutput();
        }
        int limit = characterLimit(cover, question);
        List<String> warnings = new java.util.ArrayList<>(draft.warnings());
        if (draft.characterCount() > limit) {
            warnings.add("CHARACTER_LIMIT_EXCEEDED");
        }
        return new Preview(draft.text(), draft.characterCount(), limit, List.copyOf(warnings),
                draft.usedExperienceIds(), List.of(), response.provider(), response.isMock(),
                request.promptVersion(), response.correlationId());
    }

    public Preview revise(UUID member, UUID cover, UUID question, RevisionInput input) {
        if (input == null || input.questionType() == null || input.experienceIds() == null
                || input.revisionType() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        var request = requests.prepareRevision(member, cover, question,
                input.questionType(), input.experienceIds(), input.originalText(),
                input.revisionType(), input.instruction(), UUID.randomUUID().toString());
        AiResponse response = gateway.generate(request);
        RevisionResult revision;
        try {
            revision = mapper.readValue(response.content(), RevisionResult.class);
        } catch (RuntimeException e) {
            throw invalidOutput();
        }
        int limit = characterLimit(cover, question);
        int count = revision.revisedText().codePointCount(0, revision.revisedText().length());
        List<String> warnings = new java.util.ArrayList<>(revision.warnings());
        if (count > limit) {
            warnings.add("CHARACTER_LIMIT_EXCEEDED");
        }
        return new Preview(revision.revisedText(), count, limit, List.copyOf(warnings),
                List.of(), revision.changeReasons(), response.provider(),
                response.isMock(), request.promptVersion(), response.correlationId());
    }

    private int characterLimit(UUID cover, UUID question) {
        return questions.findByIdAndCoverLetterId(question, cover)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND))
                .characterLimit;
    }

    private static AiProviderException invalidOutput() {
        return new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                "AI response has invalid cover letter fields");
    }
}