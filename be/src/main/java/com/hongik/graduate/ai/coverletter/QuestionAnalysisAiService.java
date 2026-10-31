package com.hongik.graduate.ai.coverletter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterRepository;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestionRepository;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Classifies a stored, member-owned question. Never changes the question or answer. */
@Service
public class QuestionAnalysisAiService {
    private final CoverLetterRepository covers;
    private final CoverLetterQuestionRepository questions;
    private final QuestionSkillPolicy policy;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public QuestionAnalysisAiService(CoverLetterRepository covers,
            CoverLetterQuestionRepository questions, QuestionSkillPolicy policy,
            AiGateway gateway, ObjectMapper mapper) {
        this.covers = covers;
        this.questions = questions;
        this.policy = policy;
        this.gateway = gateway;
        this.mapper = mapper;
    }

    public record Classification(String questionType, String evaluationIntent,
            String evidenceQuote, String schemaVersion) { }
    public record Preview(CoverLetterQuestionType questionType, String evaluationIntent,
            String evidenceQuote, List<String> recommendedSkills, List<String> warnings,
            String provider, boolean isMock, String promptVersion, String correlationId) { }

    public Preview classify(UUID member, UUID cover, UUID questionId) {
        if (covers.findByIdAndMemberId(cover, member).isEmpty()) {
            throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
        }
        var question = questions.findByIdAndCoverLetterId(questionId, cover)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        AiRequest request = new AiRequest(AiTaskType.COVER_LETTER_CLASSIFICATION,
                "coverletter-classification", "v1", new AiContext(Map.of(
                        "question", question.questionText,
                        "allowedTypes", mapper.writeValueAsString(
                                java.util.Arrays.stream(CoverLetterQuestionType.values())
                                        .map(type -> Map.of("code", type.name(),
                                                "label", type.label())).toList()))),
                "coverletter-classification-v1", UUID.randomUUID().toString(), List.of());
        AiResponse response = gateway.generate(request);
        try {
            Classification data = mapper.readValue(response.content(), Classification.class);
            CoverLetterQuestionType type = CoverLetterQuestionType.valueOf(data.questionType());
            if (!"v1".equals(data.schemaVersion()) || data.evaluationIntent() == null
                    || data.evaluationIntent().isBlank() || data.evaluationIntent().length() > 500
                    || data.evidenceQuote() == null || data.evidenceQuote().length() > 500
                    || !data.evidenceQuote().isEmpty()
                    && !question.questionText.contains(data.evidenceQuote())) {
                throw new IllegalArgumentException("invalid classification");
            }
            return new Preview(type, data.evaluationIntent(), data.evidenceQuote(),
                    policy.requiredSkills(type),
                    response.isMock() ? List.of("MOCK_ONLY") :
                            data.evidenceQuote().isBlank() ? List.of("REVIEW_CLASSIFICATION") : List.of(),
                    response.provider(), response.isMock(), request.promptVersion(),
                    response.correlationId());
        } catch (RuntimeException exception) {
            throw new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                    "AI classification response is invalid");
        }
    }
}
