package com.hongik.graduate.ai.common;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Application-facing switch. Disabled by default, including with a mock bean present. */
@Service
public final class AiGateway {
    private final AiProvider provider;
    private final boolean enabled;

    public AiGateway(AiProvider provider,
                     @Value("${app.ai.enabled:false}") boolean enabled) {
        this.provider = Objects.requireNonNull(provider);
        this.enabled = enabled;
    }

    public AiResponse generate(AiRequest request) {
        ensureEnabled();
        return provider.generate(request);
    }

    public AiResponse chat(AiRequest request) {
        ensureEnabled();
        return provider.chat(request);
    }

    private void ensureEnabled() {
        if (!enabled) {
            throw new AiProviderException(AiProviderException.Code.DISABLED,
                    "AI generation is disabled");
        }
    }
}
