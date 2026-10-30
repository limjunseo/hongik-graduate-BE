package com.hongik.graduate.ai.coverletter;

import java.util.List;
import java.util.UUID;
import com.hongik.graduate.ai.experience.ExperienceSummary;
import com.hongik.graduate.ai.job.ExperienceMatchResult;
import com.hongik.graduate.ai.job.JobAnalysis;

/** Immutable, versioned input snapshot for future model calls. */
public record CoverLetterContext(UUID coverLetterId, UUID questionId,
        String company, String position, String question, int characterLimit,
        CoverLetterQuestionType questionType, JobAnalysis jobAnalysis,
        List<ExperienceSummary> experiences, List<ExperienceMatchResult> rankedMatches,
        List<String> relevantSkills, List<String> provenance,
        List<String> warnings, String contextVersion) {
    public CoverLetterContext {
        if (coverLetterId == null || questionId == null || company == null || company.isBlank()
                || position == null || position.isBlank()
                || question == null || question.isBlank() || characterLimit < 1
                || characterLimit > 10000 || questionType == null
                || !"v1".equals(contextVersion)) {
            throw new IllegalArgumentException("invalid cover letter context");
        }
        experiences = List.copyOf(experiences);
        rankedMatches = List.copyOf(rankedMatches);
        relevantSkills = List.copyOf(relevantSkills);
        provenance = List.copyOf(provenance);
        warnings = List.copyOf(warnings);
    }
}
