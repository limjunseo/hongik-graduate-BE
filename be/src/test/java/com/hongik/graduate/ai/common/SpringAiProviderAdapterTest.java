package com.hongik.graduate.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import tools.jackson.databind.ObjectMapper;

class SpringAiProviderAdapterTest {
    private final ChatModel model = mock(ChatModel.class);
    private final AiPromptTemplateRegistry prompts =
            new AiPromptTemplateRegistry(new DefaultResourceLoader());
    private final AiOutputValidator validator = new AiOutputValidator(new ObjectMapper());
    private final SpringAiProviderAdapter adapter =
            new SpringAiProviderAdapter(model, prompts, validator, 5000, 2000, 3);

    private AiRequest generate(String schema) {
        return new AiRequest(AiTaskType.EXPERIENCE_SUMMARY, "generate", "v1",
                new AiContext(Map.of("instruction", "private experience")),
                schema, "corr-1", List.of());
    }

    private AiRequest chat() {
        return new AiRequest(AiTaskType.EXPERIENCE_CHAT, "chat", "v1",
                new AiContext(Map.of("instruction", "conversation topic")),
                null, "corr-2", List.of(
                new AiMessage(AiMessage.Role.USER, "one"),
                new AiMessage(AiMessage.Role.ASSISTANT, "two"),
                new AiMessage(AiMessage.Role.USER, "three")));
    }

    private void modelReturns(String text, boolean includeUsage) {
        ChatResponse response = mock(ChatResponse.class);
        Generation generation = mock(Generation.class);
        when(response.getResult()).thenReturn(generation);
        when(generation.getOutput()).thenReturn(new AssistantMessage(text));
        if (includeUsage) {
            ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
            Usage usage = mock(Usage.class);
            when(usage.getPromptTokens()).thenReturn(7);
            when(usage.getCompletionTokens()).thenReturn(11);
            when(response.getMetadata()).thenReturn(metadata);
            when(metadata.getUsage()).thenReturn(usage);
        }
        when(model.call(any(Prompt.class))).thenReturn(response);
    }

    @Test
    void generatesWithRenderedSystemPromptAndRecordsActualUsage() {
        modelReturns("response", true);
        AiResponse result = adapter.generate(generate(null));
        assertThat(result.isMock()).isFalse();
        assertThat(result.provider()).isEqualTo("openai");
        assertThat(result.content()).isEqualTo("response");
        assertThat(result.correlationId()).isEqualTo("corr-1");
        assertThat(result.usage()).isEqualTo(new AiUsage(7, 11));

        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(1);
        assertThat(prompt.getValue().getInstructions().get(0)).isInstanceOf(SystemMessage.class);
        assertThat(prompt.getValue().getInstructions().get(0).getText())
                .contains("private experience");
    }

    @Test
    void chatPreservesTurnRolesAndOrdering() {
        modelReturns("response", false);
        assertThat(adapter.chat(chat()).usage()).isEqualTo(new AiUsage(null, null));
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(4);
        assertThat(prompt.getValue().getInstructions().get(1)).isInstanceOf(UserMessage.class);
        assertThat(prompt.getValue().getInstructions().get(2)).isInstanceOf(AssistantMessage.class);
        assertThat(prompt.getValue().getInstructions().get(3)).isInstanceOf(UserMessage.class);
        assertThat(prompt.getValue().getInstructions().get(3).getText()).isEqualTo("three");
    }

    @Test
    void rejectsInvalidRequestsBeforeAnyNetworkCalls() {
        assertCode(AiProviderException.Code.INVALID_REQUEST, () -> adapter.chat(generate(null)));
        assertCode(AiProviderException.Code.INVALID_REQUEST, () -> adapter.generate(chat()));
        assertCode(AiProviderException.Code.INVALID_REQUEST,
                () -> new SpringAiProviderAdapter(model, prompts, validator, 5, 2000, 3)
                        .generate(generate(null)));
        assertCode(AiProviderException.Code.INVALID_REQUEST,
                () -> new SpringAiProviderAdapter(model, prompts, validator, 200, 2000, 2)
                        .chat(chat()));
        assertCode(AiProviderException.Code.TEMPLATE_NOT_FOUND,
                () -> adapter.generate(new AiRequest(AiTaskType.EXPERIENCE_SUMMARY,
                        "missing", "v1", new AiContext(Map.of()),
                        null, "corr-3", List.of())));
        assertCode(AiProviderException.Code.INVALID_REQUEST,
                () -> adapter.generate(generate("unknown-v1")));
        verifyNoInteractions(model);
    }

    @Test
    void rejectsInvalidJsonAndOversizedOrBlankResponsesWithoutMockFallback() {
        modelReturns("{bad", false);
        assertCode(AiProviderException.Code.INVALID_OUTPUT,
                () -> adapter.generate(generate("mock-result-v1")));
        modelReturns(" ", false);
        assertCode(AiProviderException.Code.INVALID_OUTPUT, () -> adapter.generate(generate(null)));
        new SpringAiProviderAdapter(model, prompts, validator, 200, 3, 3);
        modelReturns("long output", false);
        assertCode(AiProviderException.Code.INVALID_OUTPUT,
                () -> new SpringAiProviderAdapter(model, prompts, validator, 200, 3, 3)
                        .generate(generate(null)));
    }

    @Test
    void mapsRateLimitTimeoutAndOtherRemoteErrorsWithoutLeakingRawMessages() {
        assertThat(SpringAiProviderAdapter.mapFailure(
                new IllegalStateException("secret", HttpClientErrorException.create(
                        HttpStatus.TOO_MANY_REQUESTS, "Too Many", null, null, null))).code())
                .isEqualTo(AiProviderException.Code.RATE_LIMITED);
        assertThat(SpringAiProviderAdapter.mapFailure(
                new IllegalStateException("secret",
                        new ResourceAccessException("connection", new SocketTimeoutException()))).code())
                .isEqualTo(AiProviderException.Code.TIMEOUT);
        AiProviderException mapped = SpringAiProviderAdapter.mapFailure(
                new IllegalStateException("secret model response"));
        assertThat(mapped.code()).isEqualTo(AiProviderException.Code.PROVIDER_FAILURE);
        assertThat(mapped.getMessage()).doesNotContain("secret");
    }

    private void assertCode(AiProviderException.Code code, Runnable invocation) {
        assertThatThrownBy(invocation::run).isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(code);
    }

    @Test
    void mapsOpenAiSdkHttp429AndOtherServiceErrors() {
        com.openai.errors.OpenAIServiceException limited =
                mock(com.openai.errors.OpenAIServiceException.class);
        when(limited.statusCode()).thenReturn(429);
        assertThat(SpringAiProviderAdapter.mapFailure(
                new IllegalStateException("sensitive", limited)).code())
                .isEqualTo(AiProviderException.Code.RATE_LIMITED);

        com.openai.errors.OpenAIServiceException unauthorized =
                mock(com.openai.errors.OpenAIServiceException.class);
        when(unauthorized.statusCode()).thenReturn(401);
        AiProviderException mapped = SpringAiProviderAdapter.mapFailure(unauthorized);
        assertThat(mapped.code()).isEqualTo(AiProviderException.Code.PROVIDER_FAILURE);
        assertThat(mapped.getMessage()).doesNotContain("sensitive");

        assertThat(SpringAiProviderAdapter.mapFailure(
                new ResourceAccessException("connection refused")).code())
                .isEqualTo(AiProviderException.Code.PROVIDER_FAILURE);
    }}