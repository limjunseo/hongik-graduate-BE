package com.hongik.graduate.ai.experience;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Stable skill codes; technologies such as Java and Spring belong to keywords. */
@Component
public final class SkillTaxonomy {
    private static final String RESOURCE = "classpath:ai/taxonomy/skills-v1.json";
    private final String version;
    private final List<Skill> skills;
    private final Map<String, String> lookups;

    public SkillTaxonomy(ObjectMapper mapper, ResourceLoader loader) {
        JsonNode root;
        try (var stream = loader.getResource(RESOURCE).getInputStream()) {
            root = mapper.readTree(stream);
        } catch (IOException e) {
            throw new IllegalStateException("skill taxonomy resource unavailable", e);
        }
        if (root == null || !root.isObject() || root.get("version") == null
                || !root.get("version").isString() || !root.get("skills").isArray()) {
            throw new IllegalStateException("invalid skill taxonomy schema");
        }
        version = root.get("version").stringValue();
        List<Skill> rows = new ArrayList<>();
        Map<String, String> lookup = new LinkedHashMap<>();
        for (JsonNode node : root.get("skills")) {
            if (!node.isObject() || !node.get("code").isString()
                    || !node.get("label").isString()
                    || !node.get("aliases").isArray()) {
                throw new IllegalStateException("invalid skill taxonomy entry");
            }
            String code = node.get("code").stringValue();
            String label = node.get("label").stringValue();
            if (!code.matches("[a-z][a-z0-9_]*") || label.isBlank()) {
                throw new IllegalStateException("invalid skill taxonomy identifiers");
            }
            List<String> aliases = new ArrayList<>();
            for (JsonNode alias : node.get("aliases")) {
                if (!alias.isString() || alias.stringValue().isBlank()) {
                    throw new IllegalStateException("invalid skill alias");
                }
                aliases.add(alias.stringValue());
            }
            Skill skill = new Skill(code, label, aliases);
            rows.add(skill);
            for (String synonym : concat(code, label, aliases)) {
                String key = lookupKey(synonym);
                String previous = lookup.putIfAbsent(key, code);
                if (previous != null && !previous.equals(code)) {
                    throw new IllegalStateException("conflicting skill taxonomy aliases");
                }
            }
        }
        if (rows.isEmpty()) {
            throw new IllegalStateException("skill taxonomy must not be empty");
        }
        skills = List.copyOf(rows);
        lookups = Map.copyOf(lookup);
    }

    private static List<String> concat(String code, String label, List<String> aliases) {
        List<String> all = new ArrayList<>(List.of(code, label));
        all.addAll(aliases);
        return all;
    }

    private static String lookupKey(String value) {
        return value.strip().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public String version() { return version; }
    public List<Skill> all() { return skills; }

    public String normalize(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            throw new IllegalArgumentException("empty skill code");
        }
        String code = lookups.get(lookupKey(candidate));
        if (code == null) {
            throw new IllegalArgumentException("unknown skill code");
        }
        return code;
    }

    public List<String> normalizeAll(List<String> values) {
        if (values == null || values.size() > 30) {
            throw new IllegalArgumentException("invalid skill list");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            normalized.add(normalize(value));
        }
        return List.copyOf(normalized);
    }

    public record Skill(String code, String label, List<String> aliases) {
        public Skill {
            aliases = List.copyOf(aliases);
        }
    }
}
