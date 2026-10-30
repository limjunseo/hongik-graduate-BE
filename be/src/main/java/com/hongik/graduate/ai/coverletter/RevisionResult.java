package com.hongik.graduate.ai.coverletter;

import java.util.List;

public record RevisionResult(String revisedText, List<String> changeReasons,
        List<String> warnings, String schemaVersion) {
    public RevisionResult {
        if (revisedText == null || revisedText.isBlank() || !"v1".equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid revision output");
        }
        changeReasons = List.copyOf(changeReasons);
        warnings = List.copyOf(warnings);
    }
}
