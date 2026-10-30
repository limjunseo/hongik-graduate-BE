package com.hongik.graduate.ai.job;

import java.util.List;

/** Job role report contract. Any narrative must carry its own supporting source. */
public record RoleAnalysis(String position, String summary, List<String> skills,
        List<String> responsibilities, List<JobAnalysis.Evidence> evidence,
        String schemaVersion) {
    public RoleAnalysis {
        if (position == null || position.isBlank() || summary == null || summary.isBlank()
                || !JobAnalysis.VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid role analysis");
        }
        skills = List.copyOf(skills == null ? List.of() : skills);
        responsibilities = List.copyOf(responsibilities == null ? List.of() : responsibilities);
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
