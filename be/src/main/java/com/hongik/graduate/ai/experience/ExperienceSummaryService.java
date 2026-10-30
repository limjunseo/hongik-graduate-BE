package com.hongik.graduate.ai.experience;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.importdata.ImportExperienceCandidateService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

/** Structured experience persistence, independent of the legacy free-text summary. */
@Service
@Transactional
public class ExperienceSummaryService {
    private final ExperienceRepository experiences;
    private final ExperienceSummaryRepository summaries;
    private final SkillTaxonomy skills;
    private final ExperienceCompletenessEvaluator completeness;
    private final ImportExperienceCandidateService sources;
    private final ObjectMapper mapper;

    public ExperienceSummaryService(ExperienceRepository experiences,
            ExperienceSummaryRepository summaries, SkillTaxonomy skills,
            ExperienceCompletenessEvaluator completeness,
            ImportExperienceCandidateService sources, ObjectMapper mapper) {
        this.experiences = experiences;
        this.summaries = summaries;
        this.skills = skills;
        this.completeness = completeness;
        this.sources = sources;
        this.mapper = mapper;
    }

    private Experience own(UUID memberId, UUID experienceId) {
        return experiences.findByIdAndMemberId(experienceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));
    }

    public record SummaryInput(String schemaVersion,
            String situation, String task, String action, String result,
            String role, String conflict, String learned,
            List<String> skills, List<String> keywords,
            List<ExperienceSummary.SourceRef> evidenceSources) { }

    private ExperienceSummary build(Experience experience, SummaryInput input,
            List<String> normalizedSkills, List<String> keywords) {
        return new ExperienceSummary(experience.id, experience.title, experience.tagCode,
                input.situation(), input.task(), input.action(), input.result(),
                input.role(), input.conflict(), input.learned(),
                normalizedSkills, keywords, input.evidenceSources(),
                ExperienceSummary.VERSION, "USER");
    }

    public ExperienceSummary save(UUID memberId, UUID experienceId, SummaryInput input) {
        Experience experience = own(memberId, experienceId);
        if (input == null || (input.schemaVersion() != null
                && !ExperienceSummary.VERSION.equals(input.schemaVersion()))) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        try {
            List<String> normalizedSkills = skills.normalizeAll(
                    input.skills() == null ? List.of() : input.skills());
            List<String> keywords = input.keywords() == null ? List.of()
                    : List.copyOf(new LinkedHashSet<>(input.keywords()));
            ExperienceSummary snapshot = build(experience, input, normalizedSkills, keywords);
            completeness.evaluate(snapshot); // Tag and schema must be supported.
            for (ExperienceSummary.SourceRef source : snapshot.evidenceSources()) {
                sources.verifyEvidence(memberId, source);
            }
            String json = mapper.writeValueAsString(snapshot);
            ExperienceSummaryEntity entity = summaries.findById(experienceId)
                    .orElseGet(() -> new ExperienceSummaryEntity(
                            experienceId, json, ExperienceSummary.VERSION, "USER"));
            entity.snapshotJson = json;
            entity.schemaVersion = ExperienceSummary.VERSION;
            entity.origin = "USER";
            summaries.save(entity);
            return snapshot;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public ExperienceSummary get(UUID memberId, UUID experienceId) {
        Experience experience = own(memberId, experienceId);
        ExperienceSummaryEntity entity = summaries.findById(experienceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_SUMMARY_NOT_FOUND));
        ExperienceSummary stored = mapper.readValue(entity.snapshotJson, ExperienceSummary.class);
        return new ExperienceSummary(experience.id, experience.title, experience.tagCode,
                stored.situation(), stored.task(), stored.action(), stored.result(),
                stored.role(), stored.conflict(), stored.learned(),
                stored.skills(), stored.keywords(), stored.evidenceSources(),
                stored.schemaVersion(), stored.origin());
    }

    @Transactional(readOnly = true)
    public ExperienceCompletenessEvaluator.CompletenessResult completeness(
            UUID memberId, UUID experienceId) {
        Experience experience = own(memberId, experienceId);
        ExperienceSummary summary = summaries.findById(experienceId)
                .map(row -> mapper.readValue(row.snapshotJson, ExperienceSummary.class))
                .orElseGet(() -> new ExperienceSummary(
                        experience.id, experience.title, experience.tagCode,
                        null, null, null, null, null, null, null,
                        List.of(), List.of(), List.of(), ExperienceSummary.VERSION, "USER"));
        return completeness.evaluate(summary);
    }
}
