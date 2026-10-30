package com.hongik.graduate.ai.common;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import com.openai.errors.OpenAIServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

/** Isolates Spring AI and remote failures from the provider-neutral application port. */
@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai")
public final class SpringAiProviderAdapter implements AiProvider {
    private static final Logger log = LoggerFactory.getLogger(SpringAiProviderAdapter.class);

    private final ChatModel model;
    private final AiPromptTemplateRegistry prompts;
    private final AiOutputValidator validator;
    private final int maxInputCharacters;
    private final int maxOutputCharacters;
    private final int maxChatMessages;

    public SpringAiProviderAdapter(ChatModel model, AiPromptTemplateRegistry prompts,
            AiOutputValidator validator,
            @Value("${app.ai.max-input-characters:32000}") int maxInputCharacters,
            @Value("${app.ai.max-output-characters:16000}") int maxOutputCharacters,
            @Value("${app.ai.max-chat-messages:30}") int maxChatMessages) {
        this.model = Objects.requireNonNull(model);
        this.prompts = Objects.requireNonNull(prompts);
        this.validator = Objects.requireNonNull(validator);
        if (maxInputCharacters < 1 || maxOutputCharacters < 1 || maxChatMessages < 1) {
            throw new IllegalArgumentException("AI request limits must be positive");
        }
        this.maxInputCharacters = maxInputCharacters;
        this.maxOutputCharacters = maxOutputCharacters;
        this.maxChatMessages = maxChatMessages;
    }

    @Override
    public AiResponse generate(AiRequest request) {
        return invoke(request, false);
    }

    @Override
    public AiResponse chat(AiRequest request) {
        return invoke(request, true);
    }

    private AiResponse invoke(AiRequest request, boolean conversation) {
        Objects.requireNonNull(request, "request");
        List<AiMessage> history = request.messages();
        if (!conversation && !history.isEmpty()
                || conversation && (history.isEmpty()
                || history.get(history.size() - 1).role() != AiMessage.Role.USER)) {
            throw invalidRequest("invalid AI conversation history");
        }
        if (history.size() > maxChatMessages) {
            throw invalidRequest("too many AI chat messages");
        }

        validator.requireKnownSchema(request.outputSchemaId());
        String system = prompts.resolve(request.promptName(), request.promptVersion())
                .render(request.context());
        List<Message> messages = new ArrayList<>(history.size() + 1);
        messages.add(new SystemMessage(system));
        long inputCharacters = system.length();
        for (AiMessage turn : history) {
            inputCharacters += turn.content().length();
            messages.add(switch (turn.role()) {
                case USER -> new UserMessage(turn.content());
                case ASSISTANT -> new AssistantMessage(turn.content());
                case SYSTEM -> throw invalidRequest("system messages are not accepted");
            });
        }
        if (inputCharacters > maxInputCharacters) {
            throw invalidRequest("AI request exceeds input size limit");
        }

        long started = System.nanoTime();
        try {
            ChatResponse response = model.call(new Prompt(messages));
            String content = response == null || response.getResult() == null
                    || response.getResult().getOutput() == null
                    ? null : response.getResult().getOutput().getText();
            if (content == null || content.isBlank() || content.length() > maxOutputCharacters) {
                throw new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                        "AI response is missing or too long");
            }
            validator.validate(request.outputSchemaId(), content);
            Usage usage = response.getMetadata() == null
                    ? null : response.getMetadata().getUsage();
            AiUsage counts = usage == null ? new AiUsage(null, null)
                    : new AiUsage(usage.getPromptTokens(), usage.getCompletionTokens());
            log.info("AI task={} request={} provider=openai status=success elapsedMs={} inputTokens={} outputTokens={}",
                    request.taskType(), request.correlationId(), elapsedMs(started),
                    counts.inputTokens(), counts.outputTokens());
            return new AiResponse(AiResponse.Status.SUCCESS, "openai", false,
                    content, request.outputSchemaId(), request.correlationId(), counts);
        } catch (AiProviderException error) {
            log.warn("AI task={} request={} provider=openai status={} elapsedMs={}",
                    request.taskType(), request.correlationId(), error.code(), elapsedMs(started));
            throw error;
        } catch (RuntimeException error) {
            AiProviderException mapped = mapFailure(error);
            // Do not log exception messages: client SDK errors may contain private inputs.
            log.warn("AI task={} request={} provider=openai status={} elapsedMs={}",
                    request.taskType(), request.correlationId(), mapped.code(), elapsedMs(started));
            throw mapped;
        }
    }

    private static long elapsedMs(long started) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    }

    private static AiProviderException invalidRequest(String message) {
        return new AiProviderException(AiProviderException.Code.INVALID_REQUEST, message);
    }

    static AiProviderException mapFailure(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof OpenAIServiceException status) {
                return new AiProviderException(
                        status.statusCode() == 429
                                ? AiProviderException.Code.RATE_LIMITED
                                : AiProviderException.Code.PROVIDER_FAILURE,
                        status.statusCode() == 429
                                ? "AI provider rate limit exceeded" : "AI provider request failed");
            }
            if (current instanceof HttpStatusCodeException status) {
                if (status.getStatusCode().value() == 429) {
                    return new AiProviderException(AiProviderException.Code.RATE_LIMITED,
                            "AI provider rate limit exceeded");
                }
                return new AiProviderException(AiProviderException.Code.PROVIDER_FAILURE,
                        "AI provider request failed");
            }
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof TimeoutException) {
                return new AiProviderException(AiProviderException.Code.TIMEOUT,
                        "AI provider request timed out");
            }
        }
        return new AiProviderException(AiProviderException.Code.PROVIDER_FAILURE,
                "AI provider request failed");
    }
}
