package com.hongik.graduate.ai.interview;

import java.util.List;

public record InterviewQuestionResult(String question, InterviewQuestionType questionType,
        List<String> evidence, String schemaVersion) {
    public InterviewQuestionResult {
        if (question == null || question.isBlank() || questionType == null
                || !"v1".equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid interview question result");
        }
        evidence = List.copyOf(evidence);
    }
}
