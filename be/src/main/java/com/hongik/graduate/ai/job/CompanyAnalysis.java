package com.hongik.graduate.ai.job;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/** No company research happens in stages 1–4: this is an evidence contract. */
public record CompanyAnalysis(String company, String summary, URI sourceUrl,
        Instant retrievedAt, List<JobAnalysis.Evidence> evidence, String schemaVersion) {
    public CompanyAnalysis {
        if (company == null || company.isBlank() || summary == null || summary.isBlank()
                || sourceUrl == null || !List.of("http", "https").contains(sourceUrl.getScheme())
                || sourceUrl.getHost() == null || retrievedAt == null
                || !JobAnalysis.VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("invalid sourced company analysis");
        }
        evidence = List.copyOf(evidence == null ? List.of() : evidence);
    }
}
