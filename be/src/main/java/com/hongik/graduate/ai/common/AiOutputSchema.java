package com.hongik.graduate.ai.common;

import java.util.Map;

/** Minimal versioned JSON shape; feature-specific schemas arrive in later stages. */
public record AiOutputSchema(String id, Map<String, Kind> requiredFields) {
    public enum Kind { STRING, NUMBER, BOOLEAN, ARRAY, OBJECT }

    public AiOutputSchema {
        if (id == null || !id.matches("[a-z][a-z0-9-]{0,79}")
                || requiredFields == null || requiredFields.isEmpty()
                || requiredFields.entrySet().stream().anyMatch(
                        e -> e.getKey() == null || e.getKey().isBlank()
                                || e.getValue() == null)) {
            throw new IllegalArgumentException("invalid AI output schema");
        }
        requiredFields = Map.copyOf(requiredFields);
    }
}
