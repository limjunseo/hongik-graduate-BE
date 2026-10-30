package com.hongik.graduate.ai.coverletter;

import java.util.List;
import java.util.UUID;

public record CoverLetterDraftResult(String text, int characterCount, List<String> warnings,
        List<UUID> usedExperienceIds, String schemaVersion) {
    public CoverLetterDraftResult {
        if (text == null || text.isBlank() || characterCount != text.codePointCount(0, text.length())
                || !"v1".equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid draft output");
        }
        warnings = List.copyOf(warnings);
        usedExperienceIds = List.copyOf(usedExperienceIds);
    }
}
