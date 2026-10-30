package com.hongik.graduate.ai.interview;

import java.util.List;

public record InterviewFeedbackResult(String feedback, String followUpQuestion,
        List<String> evidence, String schemaVersion) {
    public InterviewFeedbackResult {
        if (feedback == null || feedback.isBlank() || followUpQuestion == null
                || followUpQuestion.isBlank() || !"v1".equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid interview feedback");
        }
        evidence = List.copyOf(evidence);
    }
}
