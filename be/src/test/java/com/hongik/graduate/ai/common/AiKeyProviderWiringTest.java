package com.hongik.graduate.ai.common;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/** Only creates the OpenAI bean with a fake key; never calls a remote model. */
@SpringBootTest(properties = "AI_API_KEY=dummy-key-for-wiring-only")
class AiKeyProviderWiringTest {
    @Autowired ApplicationContext context;
    @Autowired org.springframework.core.env.Environment environment;

    @Test
    void keyOnlyChoosesRemoteAdapterWithoutSendingRequests() {
        assertThat(environment.getProperty("app.ai.enabled")).isEqualTo("true");
        assertThat(environment.getProperty("app.ai.provider")).isEqualTo("openai");
        assertThat(context.getBean(AiProvider.class))
                .isInstanceOf(SpringAiProviderAdapter.class);
        assertThat(context.getBeansOfType(org.springframework.ai.chat.model.ChatModel.class))
                .hasSize(1);
    }
}
