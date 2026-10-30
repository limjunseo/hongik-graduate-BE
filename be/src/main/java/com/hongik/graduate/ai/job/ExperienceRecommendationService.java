package com.hongik.graduate.ai.job;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.ai.experience.ExperienceSummary;
import com.hongik.graduate.ai.experience.ExperienceSummaryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Deterministic ranking of owned experiences, without linking them automatically. */
@Service
public class ExperienceRecommendationService {
    private final ExperienceRepository experiences;
    private final ExperienceSummaryRepository summaries;
    private final JobAnalysisService analysisService;
    private final ExperienceMatcher matcher;
    private final ObjectMapper mapper;

    public ExperienceRecommendationService(ExperienceRepository experiences,
            ExperienceSummaryRepository summaries, JobAnalysisService analysisService,
            ExperienceMatcher matcher, ObjectMapper mapper) {
        this.experiences = experiences; this.summaries = summaries;
        this.analysisService = analysisService; this.matcher = matcher; this.mapper = mapper;
    }

    public record Recommendations(UUID coverLetterId, String scoringVersion,
            String method, JobAnalysis.Status analysisStatus,
            List<ExperienceMatchResult> results) {
        public Recommendations { results = List.copyOf(results); }
    }

    @Transactional(readOnly = true)
    public Recommendations rank(UUID member, UUID cover) {
        JobAnalysis jd = analysisService.get(member, cover);
        var owned = experiences.findByMemberIdOrderByUpdatedAtDesc(member);
        Map<UUID, ExperienceSummary> known = summaries.findAllById(
                owned.stream().map(e -> e.id).toList()).stream()
                .map(row -> mapper.readValue(row.snapshotJson, ExperienceSummary.class))
                .collect(Collectors.toMap(ExperienceSummary::experienceId, Function.identity()));
        List<ExperienceMatchResult> ranks = new ArrayList<>();
        for (var exp : owned) {
            ExperienceSummary structured = known.get(exp.id);
            ranks.add(matcher.match(exp.id, structured == null ? List.of()
                    : structured.skills(), jd.requiredSkills()));
        }
        Map<UUID, Integer> stableOrder = new java.util.HashMap<>();
        for (int i = 0; i < owned.size(); i++) stableOrder.put(owned.get(i).id, i);
        ranks.sort(Comparator.comparingInt((ExperienceMatchResult m) ->
                m.score() == null ? -1 : m.score()).reversed()
                .thenComparingInt(m -> stableOrder.get(m.experienceId()))
                .thenComparing(m -> m.experienceId().toString()));
        return new Recommendations(cover, "v1", "RULE_BASED", jd.analysisStatus(), ranks);
    }
}
