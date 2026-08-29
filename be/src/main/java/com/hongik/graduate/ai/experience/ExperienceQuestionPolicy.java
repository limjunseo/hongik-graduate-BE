package com.hongik.graduate.ai.experience;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public record ExperienceQuestionPolicy(
        ExperienceTag tag,
        String label,
        String version,
        List<String> questions
) {

    public ExperienceQuestionPolicy {
        Objects.requireNonNull(tag, "tag must not be null");
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("label must be a non-empty string");
        }
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must be a non-empty string");
        }
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("questions must not be empty");
        }
        if (questions.stream().anyMatch(question -> question == null || question.isBlank())) {
            throw new IllegalArgumentException("questions must contain non-empty strings");
        }
        if (new HashSet<>(questions).size() != questions.size()) {
            throw new IllegalArgumentException("questions must not contain duplicates");
        }
        questions = List.copyOf(questions);
    }
}
