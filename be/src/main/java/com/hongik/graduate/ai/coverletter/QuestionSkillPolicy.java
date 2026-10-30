package com.hongik.graduate.ai.coverletter;

import java.io.IOException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.hongik.graduate.ai.experience.SkillTaxonomy;

/** Versioned, curated mapping; does NOT classify free text. */
@Component
public class QuestionSkillPolicy {
    private final String version;
    private final Map<CoverLetterQuestionType, List<String>> mapping;

    public QuestionSkillPolicy(ResourceLoader resources, ObjectMapper mapper, SkillTaxonomy taxonomy) {
        JsonNode root;
        try (var input = resources.getResource(
                "classpath:ai/prompts/coverletter/question-skill-map-v1.json").getInputStream()) {
            root = mapper.readTree(input);
        } catch (IOException e) {
            throw new IllegalStateException("question skill mapping missing", e);
        }
        if (root == null || !root.isObject() || root.get("version") == null
                || !root.get("version").isString() || !root.get("mapping").isObject()) {
            throw new IllegalStateException("invalid question skill mapping");
        }
        version = root.get("version").stringValue();
        if (!"v1".equals(version)) throw new IllegalStateException("unsupported question policy");
        Map<CoverLetterQuestionType, List<String>> result =
                new EnumMap<>(CoverLetterQuestionType.class);
        for (CoverLetterQuestionType type : CoverLetterQuestionType.values()) {
            JsonNode skills = root.get("mapping").get(type.name());
            if (skills == null || !skills.isArray() || skills.isEmpty()) {
                throw new IllegalStateException("missing question skill policy: " + type);
            }
            java.util.List<String> codes = new java.util.ArrayList<>();
            for (JsonNode skill : skills) {
                if (!skill.isString()) throw new IllegalStateException("invalid mapping skill");
                codes.add(skill.stringValue());
            }
            result.put(type, taxonomy.normalizeAll(codes));
        }
        if (root.get("mapping").size() != CoverLetterQuestionType.values().length) {
            throw new IllegalStateException("unexpected question mapping entries");
        }
        mapping = Map.copyOf(result);
    }

    public String version() { return version; }
    public List<String> requiredSkills(CoverLetterQuestionType type) {
        if (type == null) throw new IllegalArgumentException("question type required");
        return mapping.get(type);
    }
    public Map<CoverLetterQuestionType, List<String>> all() { return mapping; }
}
