package com.hongik.graduate.ai;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

class SpringAiContractTest {

    @Test
    void promptTemplateRendersVariables() {
        PromptTemplate template = PromptTemplate.builder()
                .template("Summarize {experience} for {role}.")
                .build();

        String rendered = template.render(Map.of(
                "experience", "backend internship",
                "role", "developer"
        ));

        assertThat(rendered)
                .isEqualTo("Summarize backend internship for developer.");
    }

    @Test
    void chatModelProvidesTheProviderNeutralSpringAiContract() {
        ChatModel chatModel = prompt -> new ChatResponse(List.of(
                new Generation(new AssistantMessage("generated"))
        ));

        ChatResponse response = chatModel.call(new Prompt("Hello Hongik"));

        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput().getText())
                .isEqualTo("generated");
    }
}
