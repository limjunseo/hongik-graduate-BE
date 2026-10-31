package com.hongik.graduate.ai.common;

import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Local-development convenience: a configured AI_API_KEY is enough to opt in.
 * Explicit AI_ENABLED / AI_PROVIDER always take precedence, including opt-out.
 * Registered after Spring Boot ConfigData has loaded optional .env-local files.
 */
public final class AiKeyAutoConfiguration implements EnvironmentPostProcessor, Ordered {
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment,
            SpringApplication application) {
        if (environment.containsProperty("AI_ENABLED")
                || environment.containsProperty("AI_PROVIDER")) {
            return;
        }
        String key = environment.getProperty("AI_API_KEY");
        if (key == null || key.isBlank()) return;
        environment.getPropertySources().addFirst(new MapPropertySource(
                "ai-api-key-automatic-opt-in",
                Map.of("app.ai.enabled", "true",
                        "app.ai.provider", "openai",
                        "spring.ai.model.chat", "openai",
                        "spring.ai.openai.api-key", key)));
    }
}
