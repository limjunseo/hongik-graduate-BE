package com.hongik.graduate.ai.coverletter;

import java.util.List;

/** Suggestions are unverified and are never applied to an application automatically. */
public record SupportStrategyResult(List<String> strengths, List<String> evidenceGaps,
        List<String> nextActions, String schemaVersion) {
    public SupportStrategyResult {
        if (!"v1".equals(schemaVersion)) throw new IllegalArgumentException("invalid strategy version");
        strengths = validate(strengths);
        evidenceGaps = validate(evidenceGaps);
        nextActions = validate(nextActions);
    }

    private static List<String> validate(List<String> items) {
        if (items == null || items.size() > 20
                || items.stream().anyMatch(s -> s == null || s.isBlank() || s.length() > 500)) {
            throw new IllegalArgumentException("invalid strategy entries");
        }
        return List.copyOf(items);
    }
}
