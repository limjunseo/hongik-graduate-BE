package com.hongik.graduate.ai.common;

import java.util.Map;

import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Reject malformed JSON, unknown schemas and missing/wrong-type required fields. */
@Component
public final class AiOutputValidator {
    private final ObjectMapper mapper;
    private final Map<String, AiOutputSchema> schemas;

    public AiOutputValidator(ObjectMapper mapper) {
        this.mapper = mapper;
        this.schemas = Map.of("mock-result-v1",
                new AiOutputSchema("mock-result-v1", Map.of(
                        "taskType", AiOutputSchema.Kind.STRING,
                        "message", AiOutputSchema.Kind.STRING,
                        "fingerprint", AiOutputSchema.Kind.STRING,
                        "turns", AiOutputSchema.Kind.NUMBER)));
    }

    public void requireKnownSchema(String schemaId) {
        if (schemaId != null && !schemas.containsKey(schemaId)) {
            throw new AiProviderException(AiProviderException.Code.INVALID_REQUEST,
                    "unknown output schema id");
        }
    }

    public void validate(String schemaId, String content) {
        requireKnownSchema(schemaId);
        if (content == null || content.isBlank()) {
            throw invalidOutput();
        }
        if (schemaId == null) {
            return; // Text output.
        }
        JsonNode root;
        try {
            root = mapper.readTree(content);
        } catch (JacksonException e) {
            throw invalidOutput();
        }
        if (root == null || !root.isObject()) {
            throw invalidOutput();
        }
        AiOutputSchema schema = schemas.get(schemaId);
        for (var required : schema.requiredFields().entrySet()) {
            JsonNode field = root.get(required.getKey());
            if (field == null || !matches(field, required.getValue())) {
                throw invalidOutput();
            }
        }
    }

    private static boolean matches(JsonNode node, AiOutputSchema.Kind kind) {
        return switch (kind) {
            case STRING -> node.isString();
            case NUMBER -> node.isNumber();
            case BOOLEAN -> node.isBoolean();
            case ARRAY -> node.isArray();
            case OBJECT -> node.isObject();
        };
    }

    private static AiProviderException invalidOutput() {
        return new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                "AI output does not match the declared schema");
    }
}
