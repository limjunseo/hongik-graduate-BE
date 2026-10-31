package com.hongik.graduate.ai.common;

import static org.assertj.core.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class AiKeyAutoConfigurationTest {
    private final AiKeyAutoConfiguration initializer = new AiKeyAutoConfiguration();

    @Test
    void apiKeyAloneEnablesRealProvider() {
        var environment = environment(Map.of("AI_API_KEY", "testing-key"));
        initializer.postProcessEnvironment(environment, null);
        assertThat(environment.getProperty("app.ai.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("app.ai.provider")).isEqualTo("openai");
        assertThat(environment.getProperty("spring.ai.model.chat")).isEqualTo("openai");
        assertThat(environment.getProperty("spring.ai.openai.api-key")).isEqualTo("testing-key");
    }

    @Test
    void absentKeyDoesNotModifyConfiguration() {
        var environment = environment(Map.of());
        initializer.postProcessEnvironment(environment, null);
        assertThat(environment.getPropertySources().contains("ai-api-key-automatic-opt-in"))
                .isFalse();
    }

    @Test
    void explicitOptOutIsRespectedEvenIfKeyExists() {
        var environment = environment(Map.of(
                "AI_API_KEY", "testing-key", "AI_ENABLED", "false"));
        initializer.postProcessEnvironment(environment, null);
        assertThat(environment.getPropertySources().contains("ai-api-key-automatic-opt-in"))
                .isFalse();
    }

    private static StandardEnvironment environment(Map<String, Object> properties) {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return environment;
    }
}
