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
        this.templates = Map.ofEntries(
                Map.entry("generate:v1", load(resourceLoader, "generate", "v1")),
                Map.entry("chat:v1", load(resourceLoader, "chat", "v1")),
                Map.entry("coverletter-draft:v1", load(resourceLoader, "coverletter-draft", "v1")),
                Map.entry("coverletter-revision:v1", load(resourceLoader, "coverletter-revision", "v1")),
                Map.entry("support-strategy:v1", load(resourceLoader, "support-strategy", "v1")),
                Map.entry("coverletter-classification:v1", load(resourceLoader, "coverletter-classification", "v1")),
                Map.entry("experience-next-question:v1", load(resourceLoader, "experience-next-question", "v1")),
                Map.entry("interview-question:v1", load(resourceLoader, "interview-question", "v1")),
                Map.entry("interview-feedback:v1", load(resourceLoader, "interview-feedback", "v1")),
                Map.entry("job-analysis:v1", load(resourceLoader, "job-analysis", "v1")),
                Map.entry("company-analysis:v1", load(resourceLoader, "company-analysis", "v1")),
                Map.entry("role-analysis:v1", load(resourceLoader, "role-analysis", "v1")),
                Map.entry("experience-summary:v1", load(resourceLoader, "experience-summary", "v1")));
    }

    private static AiPromptTemplate load(ResourceLoader loader, String name, String version) {
        String folder = name.startsWith("company-") || name.startsWith("role-") ? "job"
                : name.startsWith("interview-") ? "interview"
                : name.startsWith("job-") ? "job"
                : name.startsWith("experience-") ? "experience"
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
