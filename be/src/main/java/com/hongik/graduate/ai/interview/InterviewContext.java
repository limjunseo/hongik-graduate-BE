package com.hongik.graduate.ai.interview;

import java.util.List;
import java.util.UUID;
import com.hongik.graduate.ai.common.AiMessage;
import com.hongik.graduate.ai.experience.ExperienceSummary;
import com.hongik.graduate.ai.job.JobAnalysis;

/** Text-based beta contract only; no automated personality or hiring score. */
public record InterviewContext(String company, JobAnalysis jobAnalysis,
        String coverLetterText, List<ExperienceSummary> experiences, String previousAnswer,
        List<AiMessage> priorTurns, InterviewQuestionType questionType,
        String difficulty, UUID sessionId, String contextVersion) {
    public InterviewContext {
        if (company == null || company.isBlank() || company.length() > 150
                || questionType == null || !"v1".equals(contextVersion)
                || coverLetterText != null && coverLetterText.length() > 10000
                || previousAnswer != null && previousAnswer.length() > 5000
                || difficulty != null && !List.of("EASY","MEDIUM","HARD").contains(difficulty)
                || experiences == null || experiences.size() > 10
                || priorTurns == null || priorTurns.size() > 60
                || priorTurns.stream().anyMatch(m -> m == null
                        || m.role() == AiMessage.Role.SYSTEM)) {
            throw new IllegalArgumentException("invalid interview context");
        }
        experiences = List.copyOf(experiences);
        priorTurns = List.copyOf(priorTurns);
    }
}
