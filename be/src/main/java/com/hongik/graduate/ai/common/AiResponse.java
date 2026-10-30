package com.hongik.graduate.ai.common;

import java.util.Objects;

public record AiResponse(
        Status status,
        String provider,
        boolean isMock,
        String content,
        String outputSchemaId,
        String correlationId,
        AiUsage usage
) {
    public enum Status { SUCCESS }

    public AiResponse {
        Objects.requireNonNull(status, "status is required");
        if (provider == null || provider.isBlank()
                || content == null || content.isBlank()
                || correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("invalid AI response metadata");
        }
        if (outputSchemaId != null && outputSchemaId.isBlank()) {
            throw new IllegalArgumentException("output schema id must not be blank");
        }
    }
}
