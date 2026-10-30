package com.hongik.graduate.ai.coverletter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SupportStrategyContextBuilder {
    private final CoverLetterContextBuilder builder;
    public SupportStrategyContextBuilder(CoverLetterContextBuilder builder) {
        this.builder = builder;
    }

    public StrategyContext build(UUID memberId, UUID coverId, UUID questionId,
            CoverLetterQuestionType type, List<UUID> experienceIds) {
        var context = builder.build(memberId, coverId, questionId, type, experienceIds);
        Set<String> strengths = new LinkedHashSet<>();
        Set<String> gaps = new LinkedHashSet<>();
        for (var match : context.rankedMatches()) {
            strengths.addAll(match.matchedSkills());
            gaps.addAll(match.missingSkills());
        }
        gaps.removeAll(strengths);
        if (context.jobAnalysis() != null && context.rankedMatches().isEmpty()) {
            gaps.addAll(context.jobAnalysis().requiredSkills());
        }
        return new StrategyContext(context, List.copyOf(strengths), List.copyOf(gaps),
                context.warnings(), "v1");
    }

    public record StrategyContext(CoverLetterContext context, List<String> strengths,
            List<String> evidenceGaps, List<String> warnings, String schemaVersion) {
        public StrategyContext {
            if (!"v1".equals(schemaVersion)) throw new IllegalArgumentException("strategy version");
            strengths = List.copyOf(strengths);
            evidenceGaps = List.copyOf(evidenceGaps);
            warnings = List.copyOf(warnings);
        }
    }
}
