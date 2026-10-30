package com.hongik.graduate.ai.job;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** v1 required-skill coverage only; not semantic relevance or hiring probability. */
@Component
public class ExperienceMatcher {
    public ExperienceMatchResult match(UUID experienceId,
            List<String> experienceSkills, List<String> requiredSkills) {
        if (experienceId == null || experienceSkills == null || requiredSkills == null) {
            throw new IllegalArgumentException("invalid match arguments");
        }
        List<String> required = List.copyOf(new java.util.LinkedHashSet<>(requiredSkills));
        Set<String> supplied = new HashSet<>(experienceSkills);
        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String requirement : required) {
            if (supplied.contains(requirement)) matched.add(requirement);
            else missing.add(requirement);
        }
        if (required.isEmpty()) {
            return new ExperienceMatchResult(experienceId, null,
                    ExperienceMatchResult.Status.NOT_SCORABLE, List.of(), List.of(),
                    List.of("JD_REQUIRED_SKILLS_MISSING"), "v1");
        }
        int score = (int) Math.round(100.0 * matched.size() / required.size());
        return new ExperienceMatchResult(experienceId, score, ExperienceMatchResult.Status.SCORED,
                matched, missing, matched.isEmpty() ? List.of("NO_REQUIRED_SKILL_MATCH")
                        : List.of("REQUIRED_SKILL_OVERLAP"), "v1");
    }
}
