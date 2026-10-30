package com.hongik.graduate.ai.job;

import java.util.List;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterRepository;
import com.hongik.graduate.BE.coverletter.domain.JobPosting;
import com.hongik.graduate.BE.coverletter.domain.JobPostingRepository;
import com.hongik.graduate.ai.experience.SkillTaxonomy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Explicit structured entry, never extracts skills from raw JD automatically. */
@Service @Transactional
public class JobAnalysisService {
    private final CoverLetterRepository covers;
    private final JobPostingRepository postings;
    private final JobAnalysisRepository analyses;
    private final SkillTaxonomy taxonomy;
    private final ObjectMapper mapper;

    public JobAnalysisService(CoverLetterRepository covers, JobPostingRepository postings,
            JobAnalysisRepository analyses, SkillTaxonomy taxonomy, ObjectMapper mapper) {
        this.covers = covers; this.postings = postings; this.analyses = analyses;
        this.taxonomy = taxonomy; this.mapper = mapper;
    }

    private JobPosting ownPosting(UUID member, UUID cover) {
        if (covers.findByIdAndMemberId(cover, member).isEmpty())
            throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
        return postings.findByCoverLetterId(cover)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
    }

    public record Input(String schemaVersion, List<String> requiredSkills,
            List<String> preferredSkills, List<String> responsibilities,
            List<String> coreCompetencies, List<String> keywords,
            List<String> companyValues, List<JobAnalysis.Evidence> evidence,
            JobAnalysis.Status analysisStatus) { }

    public JobAnalysis save(UUID member, UUID cover, Input input) {
        JobPosting posting = ownPosting(member, cover);
        if (input == null || input.schemaVersion() != null
                && !JobAnalysis.VERSION.equals(input.schemaVersion()))
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        try {
            JobAnalysis result = new JobAnalysis(posting.id,
                    covers.findById(cover).orElseThrow().companyName,
                    covers.findById(cover).orElseThrow().positionName,
                    taxonomy.normalizeAll(nullToEmpty(input.requiredSkills())),
                    taxonomy.normalizeAll(nullToEmpty(input.preferredSkills())),
                    input.responsibilities(),
                    taxonomy.normalizeAll(nullToEmpty(input.coreCompetencies())),
                    input.keywords(), input.companyValues(), input.evidence(),
                    input.analysisStatus() == null ? JobAnalysis.Status.DRAFT
                            : input.analysisStatus(), JobAnalysis.VERSION);
            for (var evidence : result.evidence()) {
                String source = posting.jdOriginal;
                if (source == null || evidence.end() > source.length()
                        || !source.substring(evidence.start(), evidence.end())
                                .equals(evidence.quote())) {
                    throw new IllegalArgumentException("unverified job analysis evidence");
                }
            }
            String json = mapper.writeValueAsString(result);
            JobAnalysisEntity entity = analyses.findById(posting.id)
                    .orElseGet(() -> new JobAnalysisEntity(
                            posting.id, json, JobAnalysis.VERSION, result.analysisStatus().name()));
            entity.snapshotJson = json;
            entity.schemaVersion = JobAnalysis.VERSION;
            entity.status = result.analysisStatus().name();
            analyses.save(entity);
            return result;
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public JobAnalysis get(UUID member, UUID cover) {
        JobPosting posting = ownPosting(member, cover);
        return analyses.findById(posting.id)
                .map(item -> mapper.readValue(item.snapshotJson, JobAnalysis.class))
                .orElseThrow(() -> new BusinessException(ErrorCode.JOB_ANALYSIS_NOT_FOUND));
    }

    private static List<String> nullToEmpty(List<String> values) {
        return values == null ? List.of() : values;
    }
}
