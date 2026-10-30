package com.hongik.graduate.ai.job;

import java.util.List;
import java.util.UUID;

/** Manually supplied structured JD; not a claim of automated extraction. */
public record JobAnalysis(UUID jobPostingId, String company, String position,
        List<String> requiredSkills, List<String> preferredSkills,
        List<String> responsibilities, List<String> coreCompetencies,
        List<String> keywords, List<String> companyValues,
        List<Evidence> evidence, Status analysisStatus, String schemaVersion) {
    public static final String VERSION = "v1";
    public enum Status { DRAFT, CONFIRMED }

    public JobAnalysis {
        if (jobPostingId == null || !VERSION.equals(schemaVersion)
                || analysisStatus == null) {
            throw new IllegalArgumentException("invalid job analysis metadata");
        }
        requiredSkills = copySkills(requiredSkills);
        preferredSkills = copySkills(preferredSkills);
        coreCompetencies = copySkills(coreCompetencies);
        responsibilities = copyTexts(responsibilities);
        keywords = copyTexts(keywords);
        companyValues = copyTexts(companyValues);
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
        if (evidence.size() > 30 || evidence.stream().anyMatch(e -> e == null)) {
            throw new IllegalArgumentException("invalid JD evidence");
        }
        if (company != null && company.length() > 150
                || position != null && position.length() > 150) {
            throw new IllegalArgumentException("company or position too long");
        }
    }

    private static List<String> copySkills(List<String> items) {
        var values = List.copyOf(items == null ? List.<String>of() : items);
        if (values.size() > 30 || values.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException("invalid JD skill list");
        }
        return values;
    }

    private static List<String> copyTexts(List<String> items) {
        var values = List.copyOf(items == null ? List.<String>of() : items);
        if (values.size() > 50 || values.stream().anyMatch(s -> s.isBlank() || s.length() > 1000)) {
            throw new IllegalArgumentException("invalid JD text list");
        }
        return values;
    }

    public record Evidence(int start, int end, String quote) {
        public Evidence {
            if (start < 0 || end <= start || quote == null || quote.isBlank()
                    || quote.length() > 1000) {
                throw new IllegalArgumentException("invalid JD evidence range");
            }
        }
    }
}
