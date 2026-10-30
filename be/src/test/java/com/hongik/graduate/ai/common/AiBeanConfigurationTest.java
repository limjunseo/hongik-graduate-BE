package com.hongik.graduate.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.hongik.graduate.ai.experience.ExperienceQuestionPolicyLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class AiBeanConfigurationTest {
    @Autowired private ApplicationContext context;
    @Autowired private AiGateway gateway;
    @Autowired private ExperienceQuestionPolicyLoader existingPolicy;

    @Test
    void oneMockProviderRegisteredAndNoRemoteModelBean() {
        assertThat(context.getBeansOfType(AiProvider.class)).hasSize(1);
        assertThat(context.getBean(AiProvider.class)).isInstanceOf(MockAiProvider.class);
        assertThat(context.getBeansOfType(org.springframework.ai.chat.model.ChatModel.class))
                .isEmpty();
        assertThat(existingPolicy).isNotNull();
    }

    @Test
    void aiIsOffByDefaultEvenWhenMockIsRegistered() {
        AiRequest request = new AiRequest(AiTaskType.EXPERIENCE_SUMMARY,
                "generate", "v1", new AiContext(java.util.Map.of("instruction", "hello")),
                null, "trace-1", java.util.List.of());
        assertThatThrownBy(() -> gateway.generate(request))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.DISABLED);
    }
}
