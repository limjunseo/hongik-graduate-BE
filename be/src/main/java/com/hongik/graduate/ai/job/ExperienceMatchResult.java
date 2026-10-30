package com.hongik.graduate.ai.job;

import java.util.List;
import java.util.UUID;

public record ExperienceMatchResult(UUID experienceId, Integer score, Status status,
        List<String> matchedSkills, List<String> missingSkills,
        List<String> reasonCodes, String scoringVersion) {
    public enum Status { SCORED, NOT_SCORABLE }
    public ExperienceMatchResult {
        if (experienceId == null || status == null || !"v1".equals(scoringVersion)
                || status == Status.SCORED && (score == null || score < 0 || score > 100)
                || status == Status.NOT_SCORABLE && score != null) {
            throw new IllegalArgumentException("invalid experience match");
        }
        matchedSkills = List.copyOf(matchedSkills);
        missingSkills = List.copyOf(missingSkills);
        reasonCodes = List.copyOf(reasonCodes);
    }
}
