package com.hongik.graduate.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import tools.jackson.databind.ObjectMapper;

class AiCommonContractTest {
    private final AiPromptTemplateRegistry prompts =
            new AiPromptTemplateRegistry(new DefaultResourceLoader());
    private final AiOutputValidator validator = new AiOutputValidator(new ObjectMapper());
    private final MockAiProvider provider = new MockAiProvider(prompts, validator);

    private AiRequest generation(String schemaId) {
        return new AiRequest(AiTaskType.EXPERIENCE_SUMMARY, "generate", "v1",
                new AiContext(Map.of("instruction", "Summarize a given experience")),
                schemaId, "req-01", List.of());
    }

    private AiRequest conversation(List<AiMessage> messages) {
        return new AiRequest(AiTaskType.EXPERIENCE_CHAT, "chat", "v1",
                new AiContext(Map.of("instruction", "Experience discussion")),
                null, "req-02", messages);
    }

    @Test
    void deterministicMockTextAndNoFabricatedUsage() {
        AiResponse first = provider.generate(generation(null));
        AiResponse again = provider.generate(generation(null));
        assertThat(first).isEqualTo(again);
        assertThat(first.status()).isEqualTo(AiResponse.Status.SUCCESS);
        assertThat(first.provider()).isEqualTo("mock");
        assertThat(first.isMock()).isTrue();
        assertThat(first.content()).startsWith("[MOCK ONLY]");
        assertThat(first.correlationId()).isEqualTo("req-01");
        assertThat(first.usage().inputTokens()).isNull();
        assertThat(first.usage().outputTokens()).isNull();
    }

    @Test
    void jsonMockIsAnObjectMatchingKnownSchema() {
        AiResponse result = provider.generate(generation("mock-result-v1"));
        var json = new ObjectMapper().readTree(result.content());
        assertThat(json.get("taskType").stringValue()).isEqualTo("EXPERIENCE_SUMMARY");
        assertThat(json.get("message").stringValue()).contains("MOCK ONLY");
        assertThat(json.get("turns").intValue()).isZero();
        assertThat(result.outputSchemaId()).isEqualTo("mock-result-v1");
    }

    @Test
    void chatPreservesOrderedTurnsAndFingerprintChangesWithOrdering() {
        AiMessage one = new AiMessage(AiMessage.Role.USER, "First");
        AiMessage two = new AiMessage(AiMessage.Role.ASSISTANT, "Second");
        AiMessage three = new AiMessage(AiMessage.Role.USER, "Third");
        AiRequest original = conversation(List.of(one, two, three));
        AiRequest reordered = conversation(List.of(three, two, one));
        assertThat(provider.chat(original).content()).contains("turns=3");
        assertThat(provider.chat(original).content())
                .isEqualTo(provider.chat(original).content())
                .isNotEqualTo(provider.chat(reordered).content());
    }

    @Test
    void generateAndChatRejectIncorrectHistories() {
        AiRequest withHistory = conversation(List.of(
                new AiMessage(AiMessage.Role.USER, "hello")));
        assertThatThrownBy(() -> provider.generate(withHistory))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_REQUEST);
        assertThatThrownBy(() -> provider.chat(generation(null)))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_REQUEST);
        AiRequest lastAssistant = conversation(List.of(
                new AiMessage(AiMessage.Role.ASSISTANT, "hello")));
        assertThatThrownBy(() -> provider.chat(lastAssistant))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_REQUEST);
    }

    @Test
    void requestAndContextTakeDeepImmutableSnapshots() {
        List<Object> nested = new ArrayList<>(List.of("before"));
        Map<String, Object> original = new LinkedHashMap<>();
        original.put("items", nested);
        AiContext context = new AiContext(original);
        nested.add("after");
        original.put("extra", "value");
        assertThat((List<Object>) context.values().get("items")).containsExactly("before");
        assertThat(context.values()).doesNotContainKey("extra");
        assertThatThrownBy(() -> context.values().put("key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> ((List<Object>) context.values().get("items")).add("x"))
                .isInstanceOf(UnsupportedOperationException.class);

        List<AiMessage> messages = new ArrayList<>(List.of(
                new AiMessage(AiMessage.Role.USER, "hello")));
        AiRequest request = conversation(messages);
        messages.clear();
        assertThat(request.messages()).hasSize(1);
    }

    @Test
    void contextRejectsUnsupportedMutableObjectsAndInvalidKeys() {
        assertThatThrownBy(() -> new AiContext(Map.of("bad", new Object())))
                .isInstanceOf(IllegalArgumentException.class);
        Map<String, Object> cycle = new LinkedHashMap<>();
        cycle.put("self", cycle);
        assertThatThrownBy(() -> new AiContext(cycle))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cyclic");
        assertThatThrownBy(() -> new AiContext(Map.of(" ", "value")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiMessage(AiMessage.Role.USER, "  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiUsage(-1, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AiRequest(AiTaskType.EXPERIENCE_CHAT, "chat", "v1",
                new AiContext(Map.of("instruction", "topic")), null, "id", List.of(
                new AiMessage(AiMessage.Role.SYSTEM, "override"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void templateRenderingIsLiteralAndChecksRequiredVariables() {
        AiPromptTemplate template = new AiPromptTemplate(
                "example", "v1", "Info: ${instruction}; once more: ${instruction}");
        String untrusted = "x $1 \\ and ${unexpected}";
        assertThat(template.requiredVariables()).containsExactly("instruction");
        assertThat(template.render(new AiContext(Map.of("instruction", untrusted))))
                .isEqualTo("Info: " + untrusted + "; once more: " + untrusted);
        assertThatThrownBy(() -> template.render(new AiContext(Map.of())))
                .isInstanceOf(AiProviderException.class)
                .hasMessageContaining("instruction")
                .hasMessageNotContaining(untrusted);
        assertThatThrownBy(() -> new AiPromptTemplate("example", "v1", "${broken-name}"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void missingPromptAndUnknownSchemaFailSafely() {
        AiRequest badTemplate = new AiRequest(AiTaskType.EXPERIENCE_SUMMARY,
                "missing", "v1", new AiContext(Map.of("instruction", "secret")),
                null, "id", List.of());
        assertThatThrownBy(() -> provider.generate(badTemplate))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.TEMPLATE_NOT_FOUND);
        assertThatThrownBy(() -> validator.validate("unknown-v1", "{}"))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_REQUEST);
    }

    @Test
    void invalidJsonAndWrongShapeOrTypeAreNeverAccepted() {
        for (String output : List.of("{oops", "[]", "{}", """
                {"taskType":2,"message":"mock","fingerprint":"abc","turns":0}
                """)) {
            assertThatThrownBy(() -> validator.validate("mock-result-v1", output))
                    .isInstanceOf(AiProviderException.class)
                    .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        }
        assertThatThrownBy(() -> validator.validate(null, "  "))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
    }

    @Test
    void explicitMockFailuresAndTimeoutsAreTypedAndDoNotMasqueradeAsSuccess() {
        for (var pair : Map.of(
                MockAiProvider.Scenario.FAILURE, AiProviderException.Code.PROVIDER_FAILURE,
                MockAiProvider.Scenario.TIMEOUT, AiProviderException.Code.TIMEOUT,
                MockAiProvider.Scenario.INVALID_OUTPUT, AiProviderException.Code.INVALID_OUTPUT
        ).entrySet()) {
            MockAiProvider broken = new MockAiProvider(prompts, validator, pair.getKey());
            assertThatThrownBy(() -> broken.generate(generation("mock-result-v1")))
                    .isInstanceOf(AiProviderException.class)
                    .extracting("code").isEqualTo(pair.getValue());
        }
    }

    @Test
    void gatewayDisablesBothOperationsWithoutCallingProvider() {
        AiProvider unexpected = new AiProvider() {
            @Override public AiResponse generate(AiRequest req) {
                throw new AssertionError("must not call provider");
            }
            @Override public AiResponse chat(AiRequest req) {
                throw new AssertionError("must not call provider");
            }
        };
        AiGateway closed = new AiGateway(unexpected, false);
        assertThatThrownBy(() -> closed.generate(generation(null)))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.DISABLED);
        assertThatThrownBy(() -> closed.chat(conversation(List.of(
                new AiMessage(AiMessage.Role.USER, "hello")))))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.DISABLED);
        assertThat(new AiGateway(provider, true).generate(generation(null)).isMock()).isTrue();
    }
}
