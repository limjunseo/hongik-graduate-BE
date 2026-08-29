package com.hongik.graduate.ai.experience;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class ExperienceQuestionPolicyLoader {

    private static final String POLICY_LOCATION =
            "classpath:ai/prompts/experience/%s.json";
    private static final List<String> REQUIRED_FIELDS =
            List.of("tag", "label", "version", "questions");

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    public ExperienceQuestionPolicyLoader(
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader
    ) {
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    public ExperienceQuestionPolicy getExperienceQuestionPolicy(ExperienceTag tag) {
        Objects.requireNonNull(tag, "tag must not be null");
        Resource resource = resourceLoader.getResource(
                POLICY_LOCATION.formatted(tag.value())
        );
        if (!resource.exists()) {
            throw new IllegalStateException(
                    "Experience policy for '%s' not found: %s"
                            .formatted(tag.value(), resource.getDescription())
            );
        }
        return loadPolicy(tag, resource);
    }

    ExperienceQuestionPolicy loadPolicy(ExperienceTag tag, Resource resource) {
        Objects.requireNonNull(tag, "tag must not be null");
        Objects.requireNonNull(resource, "resource must not be null");

        JsonNode data;
        try (InputStream inputStream = resource.getInputStream()) {
            data = objectMapper.readTree(inputStream);
        } catch (JacksonException error) {
            throw invalidPolicy(tag, resource, "could not read valid JSON", error);
        } catch (IOException error) {
            throw invalidPolicy(tag, resource, "could not read valid JSON", error);
        }

        if (data == null || !data.isObject()) {
            throw invalidPolicy(tag, resource, "top-level value must be an object");
        }

        List<String> missingFields = REQUIRED_FIELDS.stream()
                .filter(field -> !data.has(field))
                .toList();
        if (!missingFields.isEmpty()) {
            throw invalidPolicy(
                    tag,
                    resource,
                    "missing required field(s): " + String.join(", ", missingFields)
            );
        }

        JsonNode tagNode = data.get("tag");
        if (!tagNode.isString() || !tag.value().equals(tagNode.stringValue())) {
            throw invalidPolicy(tag, resource, "tag mismatch");
        }

        String label = requireNonBlankText(tag, resource, data.get("label"), "label");
        String version = requireNonBlankText(
                tag,
                resource,
                data.get("version"),
                "version"
        );

        JsonNode questionsNode = data.get("questions");
        if (!questionsNode.isArray()) {
            throw invalidPolicy(tag, resource, "questions must be a list");
        }
        if (questionsNode.isEmpty()) {
            throw invalidPolicy(tag, resource, "questions must not be empty");
        }

        List<String> questions = new ArrayList<>(questionsNode.size());
        Set<String> uniqueQuestions = new HashSet<>();
        for (int index = 0; index < questionsNode.size(); index++) {
            JsonNode questionNode = questionsNode.get(index);
            if (!questionNode.isString()) {
                throw invalidPolicy(
                        tag,
                        resource,
                        "question at index %d must be a string".formatted(index)
                );
            }
            String question = questionNode.stringValue();
            if (question.isBlank()) {
                throw invalidPolicy(
                        tag,
                        resource,
                        "question at index %d must not be empty".formatted(index)
                );
            }
            if (!uniqueQuestions.add(question)) {
                throw invalidPolicy(
                        tag,
                        resource,
                        "questions must not contain duplicates"
                );
            }
            questions.add(question);
        }

        return new ExperienceQuestionPolicy(tag, label, version, questions);
    }

    private String requireNonBlankText(
            ExperienceTag tag,
            Resource resource,
            JsonNode value,
            String field
    ) {
        if (!value.isString() || value.stringValue().isBlank()) {
            throw invalidPolicy(
                    tag,
                    resource,
                    field + " must be a non-empty string"
            );
        }
        return value.stringValue();
    }

    private IllegalArgumentException invalidPolicy(
            ExperienceTag tag,
            Resource resource,
            String reason
    ) {
        return invalidPolicy(tag, resource, reason, null);
    }

    private IllegalArgumentException invalidPolicy(
            ExperienceTag tag,
            Resource resource,
            String reason,
            Exception cause
    ) {
        String message = "Invalid experience policy for '%s' at '%s': %s"
                .formatted(tag.value(), resource.getDescription(), reason);
        return cause == null
                ? new IllegalArgumentException(message)
                : new IllegalArgumentException(message, cause);
    }
}
