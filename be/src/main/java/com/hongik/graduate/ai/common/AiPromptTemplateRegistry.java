package com.hongik.graduate.ai.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/** Explicit allowlist; user input is never used as a resource file path. */
@Component
public final class AiPromptTemplateRegistry {
    private final Map<String, AiPromptTemplate> templates;

    public AiPromptTemplateRegistry(ResourceLoader resourceLoader) {
        this.templates = Map.of(
                "generate:v1", load(resourceLoader, "generate", "v1"),
                "chat:v1", load(resourceLoader, "chat", "v1"),
                "coverletter-draft:v1", load(resourceLoader, "coverletter-draft", "v1"),
                "coverletter-revision:v1", load(resourceLoader, "coverletter-revision", "v1"),
                "support-strategy:v1", load(resourceLoader, "support-strategy", "v1"),
                "interview-question:v1", load(resourceLoader, "interview-question", "v1"),
                "interview-feedback:v1", load(resourceLoader, "interview-feedback", "v1"));
    }

    private static AiPromptTemplate load(ResourceLoader loader, String name, String version) {
        String folder = name.startsWith("interview-") ? "interview"
                : name.startsWith("coverletter-") || name.equals("support-strategy")
                ? "coverletter" : "common";
        Resource resource = loader.getResource(
                "classpath:ai/prompts/" + folder + "/" + name + "-" + version + ".txt");
        try (var input = resource.getInputStream()) {
            return new AiPromptTemplate(
                    name, version, new String(input.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException("AI prompt template resource unavailable: "
                    + name + ":" + version, e);
        }
    }

    public AiPromptTemplate resolve(String name, String version) {
        AiPromptTemplate template = templates.get(name + ":" + version);
        if (template == null) {
            throw new AiProviderException(
                    AiProviderException.Code.TEMPLATE_NOT_FOUND,
                    "unknown prompt name/version");
        }
        return template;
    }
}
