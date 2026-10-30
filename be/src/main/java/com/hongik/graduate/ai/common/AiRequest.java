package com.hongik.graduate.ai.common;

import java.util.List;
import java.util.Objects;

/** No SDK types, and no raw model/credentials in a domain request. */
public record AiRequest(
        AiTaskType taskType,
        String promptName,
        String promptVersion,
        AiContext context,
        String outputSchemaId,
        String correlationId,
        List<AiMessage> messages
) {
    public AiRequest {
        Objects.requireNonNull(taskType, "AI task type is required");
        Objects.requireNonNull(context, "AI context is required");
        if (promptName == null || !promptName.matches("[a-z][a-z0-9-]{0,79}")
                || promptVersion == null || !promptVersion.matches("v[1-9][0-9]*")) {
            throw new IllegalArgumentException("invalid prompt name/version");
        }
        if (outputSchemaId != null
                && !outputSchemaId.matches("[a-z][a-z0-9-]{0,79}")) {
            throw new IllegalArgumentException("invalid output schema id");
        }
        if (correlationId == null
                || !correlationId.matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new IllegalArgumentException("invalid correlation id");
        }
        if (messages == null || messages.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("AI messages must not contain null");
        }
        if (messages.stream().anyMatch(m -> m.role() == AiMessage.Role.SYSTEM)) {
            throw new IllegalArgumentException("system messages belong to the prompt, not chat history");
        }
        messages = List.copyOf(messages);
    }
}
