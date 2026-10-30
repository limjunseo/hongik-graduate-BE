package com.hongik.graduate.ai.coverletter;

import java.util.List;

/** Preview-only revision. Persistence requires an independent user-approved update. */
public record RevisionRequest(CoverLetterContext context, String originalText,
        RevisionType revisionType, String instruction) {
    public RevisionRequest {
        if (context == null || originalText == null || originalText.isBlank()
                || originalText.length() > 10000 || revisionType == null
                || instruction != null && instruction.length() > 1000) {
            throw new IllegalArgumentException("invalid revision request");
        }
    }
}
