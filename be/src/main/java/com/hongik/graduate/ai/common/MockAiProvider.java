package com.hongik.graduate.ai.common;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Deterministic test adapter. Never generates real advice or contacts a model. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider",
        havingValue = "mock", matchIfMissing = true)
public final class MockAiProvider implements AiProvider {
    enum Scenario { NORMAL, FAILURE, TIMEOUT, INVALID_OUTPUT }

    private final AiPromptTemplateRegistry prompts;
    private final AiOutputValidator validator;
    private final Scenario scenario;

    @org.springframework.beans.factory.annotation.Autowired
    public MockAiProvider(AiPromptTemplateRegistry prompts, AiOutputValidator validator) {
        this(prompts, validator, Scenario.NORMAL);
    }

    MockAiProvider(AiPromptTemplateRegistry prompts, AiOutputValidator validator,
                   Scenario scenario) {
        this.prompts = Objects.requireNonNull(prompts);
        this.validator = Objects.requireNonNull(validator);
        this.scenario = Objects.requireNonNull(scenario);
    }

    @Override
    public AiResponse generate(AiRequest request) {
        validateMessages(request, false);
        return respond(request);
    }

    @Override
    public AiResponse chat(AiRequest request) {
        validateMessages(request, true);
        return respond(request);
    }

    private void validateMessages(AiRequest request, boolean isChat) {
        Objects.requireNonNull(request, "AI request is required");
        if (isChat) {
            if (request.messages().isEmpty()
                    || request.messages().get(request.messages().size() - 1).role()
                    != AiMessage.Role.USER) {
                throw new AiProviderException(AiProviderException.Code.INVALID_REQUEST,
                        "chat must end with a user message");
            }
        } else if (!request.messages().isEmpty()) {
            throw new AiProviderException(AiProviderException.Code.INVALID_REQUEST,
                    "generate does not accept chat history");
        }
    }

    private AiResponse respond(AiRequest request) {
        String renderedPrompt = prompts.resolve(
                request.promptName(), request.promptVersion()).render(request.context());
        validator.requireKnownSchema(request.outputSchemaId());
        if (scenario == Scenario.TIMEOUT) {
            throw new AiProviderException(AiProviderException.Code.TIMEOUT,
                    "mock provider timeout");
        }
        if (scenario == Scenario.FAILURE) {
            throw new AiProviderException(AiProviderException.Code.PROVIDER_FAILURE,
                    "mock provider failure");
        }
        String fingerprint = fingerprint(request, renderedPrompt);
        String content;
        if (scenario == Scenario.INVALID_OUTPUT) {
            content = "{invalid JSON";
        } else if (request.outputSchemaId() == null) {
            content = "[MOCK ONLY] " + request.taskType()
                    + " turns=" + request.messages().size() + " fingerprint=" + fingerprint;
        } else {
            String marker = "[MOCK ONLY - NOT AI GENERATED]";
            content = switch (request.outputSchemaId()) {
                case "coverletter-draft-v1" -> "{\"text\":\"" + marker
                        + "\",\"characterCount\":" + marker.length()
                        + ",\"warnings\":[\"MOCK_ONLY\"],\"usedExperienceIds\":[],\"schemaVersion\":\"v1\"}";
                case "coverletter-revision-v1" -> "{\"revisedText\":\"" + marker
                        + "\",\"changeReasons\":[],\"warnings\":[\"MOCK_ONLY\"],\"schemaVersion\":\"v1\"}";
                case "coverletter-classification-v1" -> "{\"questionType\":\"MOTIVATION\",\"evaluationIntent\":\"MOCK ONLY\",\"evidenceQuote\":\"\",\"schemaVersion\":\"v1\"}";
                case "experience-next-question-v1" -> "{\"question\":\"[MOCK ONLY]\",\"targetField\":\"situation\",\"reason\":\"MOCK ONLY\",\"schemaVersion\":\"v1\"}";
                case "support-strategy-v1" -> "{\"strengths\":[],\"evidenceGaps\":[],\"nextActions\":[],"
                        + "\"schemaVersion\":\"v1\"}";
                case "experience-summary-v1" -> "{\"schemaVersion\":\"v1\","
                        + "\"situation\":null,\"task\":null,\"action\":null,"
                        + "\"result\":null,\"role\":null,\"conflict\":null,\"learned\":null,"
                        + "\"skills\":[],\"keywords\":[],\"evidenceSources\":[]}";                case "company-analysis-v1" -> "{\"summary\":\"[MOCK ONLY]\",\"evidence\":[],\"schemaVersion\":\"v1\"}";
                case "role-analysis-v1" -> "{\"summary\":\"[MOCK ONLY]\",\"skills\":[],\"responsibilities\":[],\"evidence\":[],\"schemaVersion\":\"v1\"}";
                case "job-analysis-v1" -> "{\"requiredSkills\":[],\"preferredSkills\":[],"
                        + "\"responsibilities\":[],\"coreCompetencies\":[],"
                        + "\"keywords\":[],\"companyValues\":[],"
                        + "\"evidence\":[],\"analysisStatus\":\"DRAFT\",\"schemaVersion\":\"v1\"}";                case "interview-question-v1" -> "{\"question\":\"" + marker
                        + "\",\"questionType\":\"COMMON\",\"evidence\":[],\"schemaVersion\":\"v1\"}";
                case "interview-feedback-v1" -> "{\"feedback\":\"" + marker
                        + "\",\"followUpQuestion\":\"" + marker
                        + "\",\"evidence\":[],\"schemaVersion\":\"v1\"}";
                default -> "{\"taskType\":\"" + request.taskType()
                        + "\",\"message\":\"MOCK ONLY - NOT AI GENERATED\""
                        + ",\"fingerprint\":\"" + fingerprint
                        + "\",\"turns\":" + request.messages().size() + "}";
            };
        }
        validator.validate(request.outputSchemaId(), content);
        return new AiResponse(AiResponse.Status.SUCCESS, "mock", true,
                content, request.outputSchemaId(), request.correlationId(),
                new AiUsage(null, null));
    }

    private static String fingerprint(AiRequest request, String renderedPrompt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, request.taskType().name());
            add(digest, renderedPrompt);
            add(digest, request.outputSchemaId() == null ? "TEXT" : request.outputSchemaId());
            for (AiMessage message : request.messages()) {
                add(digest, message.role().name());
                add(digest, message.content());
            }
            return HexFormat.of().formatHex(digest.digest()).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
        digest.update(bytes);
    }
}
