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
        var rules = new java.util.LinkedHashMap<String, AiOutputSchema>();
        rules.put("mock-result-v1", new AiOutputSchema("mock-result-v1", Map.of(
                "taskType", AiOutputSchema.Kind.STRING,
                "message", AiOutputSchema.Kind.STRING,
                "fingerprint", AiOutputSchema.Kind.STRING,
                "turns", AiOutputSchema.Kind.NUMBER)));
        rules.put("coverletter-draft-v1", new AiOutputSchema("coverletter-draft-v1", Map.of(
                "text", AiOutputSchema.Kind.STRING,
                "characterCount", AiOutputSchema.Kind.NUMBER,
                "warnings", AiOutputSchema.Kind.ARRAY,
                "usedExperienceIds", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("coverletter-revision-v1", new AiOutputSchema("coverletter-revision-v1", Map.of(
                "revisedText", AiOutputSchema.Kind.STRING,
                "changeReasons", AiOutputSchema.Kind.ARRAY,
                "warnings", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("support-strategy-v1", new AiOutputSchema("support-strategy-v1", Map.of(
                "strengths", AiOutputSchema.Kind.ARRAY,
                "evidenceGaps", AiOutputSchema.Kind.ARRAY,
                "nextActions", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("experience-summary-v1", new AiOutputSchema("experience-summary-v1", Map.of(
                "skills", AiOutputSchema.Kind.ARRAY,
                "keywords", AiOutputSchema.Kind.ARRAY,
                "evidenceSources", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("job-analysis-v1", new AiOutputSchema("job-analysis-v1", Map.of(
                "requiredSkills", AiOutputSchema.Kind.ARRAY,
                "preferredSkills", AiOutputSchema.Kind.ARRAY,
                "responsibilities", AiOutputSchema.Kind.ARRAY,
                "coreCompetencies", AiOutputSchema.Kind.ARRAY,
                "keywords", AiOutputSchema.Kind.ARRAY,
                "companyValues", AiOutputSchema.Kind.ARRAY,
                "evidence", AiOutputSchema.Kind.ARRAY,
                "analysisStatus", AiOutputSchema.Kind.STRING,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("interview-question-v1", new AiOutputSchema("interview-question-v1", Map.of(
                "question", AiOutputSchema.Kind.STRING,
                "questionType", AiOutputSchema.Kind.STRING,
                "evidence", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        rules.put("interview-feedback-v1", new AiOutputSchema("interview-feedback-v1", Map.of(
                "feedback", AiOutputSchema.Kind.STRING,
                "followUpQuestion", AiOutputSchema.Kind.STRING,
                "evidence", AiOutputSchema.Kind.ARRAY,
                "schemaVersion", AiOutputSchema.Kind.STRING)));
        this.schemas = Map.copyOf(rules);
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
