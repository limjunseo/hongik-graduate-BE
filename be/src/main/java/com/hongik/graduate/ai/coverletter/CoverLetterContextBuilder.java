package com.hongik.graduate.ai.coverletter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.ai.experience.ExperienceSummary;
import com.hongik.graduate.ai.experience.ExperienceSummaryRepository;
import com.hongik.graduate.ai.job.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Only authorized, already-linked experiences can enter an AI drafting context. */
@Component
public class CoverLetterContextBuilder {
    private final CoverLetterRepository covers;
    private final CoverLetterQuestionRepository questions;
    private final QuestionExperienceLinkRepository links;
    private final ExperienceRepository experiences;
    private final ExperienceSummaryRepository summaries;
    private final JobPostingRepository postings;
    private final JobAnalysisRepository analyses;
    private final ExperienceMatcher matcher;
    private final QuestionSkillPolicy policy;
    private final ObjectMapper mapper;

    public CoverLetterContextBuilder(CoverLetterRepository covers,
            CoverLetterQuestionRepository questions, QuestionExperienceLinkRepository links,
            ExperienceRepository experiences, ExperienceSummaryRepository summaries,
            JobPostingRepository postings, JobAnalysisRepository analyses,
            ExperienceMatcher matcher, QuestionSkillPolicy policy, ObjectMapper mapper) {
        this.covers = covers; this.questions = questions; this.links = links;
        this.experiences = experiences; this.summaries = summaries; this.postings = postings;
        this.analyses = analyses; this.matcher = matcher; this.policy = policy; this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CoverLetterContext build(UUID memberId, UUID coverId, UUID questionId,
            CoverLetterQuestionType type, List<UUID> selectedIds) {
        if (type == null || selectedIds == null || selectedIds.size() > 10
                || selectedIds.stream().anyMatch(java.util.Objects::isNull)
                || selectedIds.size() != new HashSet<>(selectedIds).size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        CoverLetter cover = covers.findByIdAndMemberId(coverId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        CoverLetterQuestion question = questions.findByIdAndCoverLetterId(questionId, coverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        Set<UUID> allowed = new HashSet<>();
        for (var link : links.findByQuestionIdOrderByDisplayOrderAsc(questionId)) {
            allowed.add(link.experienceId);
        }
        List<ExperienceSummary> selected = new ArrayList<>();
        for (UUID id : selectedIds) {
            if (experiences.findByIdAndMemberId(id, memberId).isEmpty()) {
                throw new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND);
            }
            if (!allowed.contains(id)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
            var entity = summaries.findById(id).orElseThrow(
                    () -> new BusinessException(ErrorCode.EXPERIENCE_SUMMARY_NOT_FOUND));
            selected.add(mapper.readValue(entity.snapshotJson, ExperienceSummary.class));
        }
        JobAnalysis jd = postings.findByCoverLetterId(coverId)
                .flatMap(p -> analyses.findById(p.id))
                .map(row -> mapper.readValue(row.snapshotJson, JobAnalysis.class)).orElse(null);
        List<String> skills = jd == null ? List.of() : jd.requiredSkills();
        List<ExperienceMatchResult> scores = new ArrayList<>();
        for (ExperienceSummary exp : selected) {
            scores.add(matcher.match(exp.experienceId(), exp.skills(), skills));
        }
        scores.sort(java.util.Comparator.comparingInt(
                (ExperienceMatchResult m) -> m.score() == null ? -1 : m.score()).reversed());
        List<String> provenance = new ArrayList<>(List.of(
                "cover-letter:" + coverId, "question:" + questionId,
                "question-skill-policy:" + policy.version()));
        if (jd != null) provenance.add("jd-analysis:" + jd.jobPostingId());
        selected.forEach(exp -> provenance.add("experience:" + exp.experienceId()));
        List<String> warnings = new ArrayList<>();
        if (jd == null) warnings.add("JD_NOT_ANALYZED");
        if (selected.isEmpty()) warnings.add("MISSING_EXPERIENCE");
        return new CoverLetterContext(coverId, questionId, cover.companyName,
                cover.positionName, question.questionText, question.characterLimit, type, jd,
                selected, scores, policy.requiredSkills(type), provenance, warnings, "v1");
    }
}
