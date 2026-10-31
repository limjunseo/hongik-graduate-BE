package com.hongik.graduate.ai.job;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterRepository;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.ai.common.*;
import com.hongik.graduate.ai.experience.SkillTaxonomy;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Preview-only research using the owned JD or explicitly user-provided source text. */
@Service
public class SourceBoundAnalysisAiService {
    private final CoverLetterRepository covers;
    private final JobPostingService postings;
    private final SkillTaxonomy taxonomy;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public SourceBoundAnalysisAiService(CoverLetterRepository covers, JobPostingService postings,
            SkillTaxonomy taxonomy, AiGateway gateway, ObjectMapper mapper) {
        this.covers = covers; this.postings = postings; this.taxonomy = taxonomy;
        this.gateway = gateway; this.mapper = mapper;
    }

    public record RoleExtraction(String summary, List<String> skills,
            List<String> responsibilities, List<JobAnalysis.Evidence> evidence,
            String schemaVersion) { }
    public record CompanyExtraction(String summary, List<JobAnalysis.Evidence> evidence,
            String schemaVersion) { }
    public record CompanyInput(String sourceUrl, String sourceText) { }
    public record Preview<T>(T analysis, String sourceType, List<String> warnings,
            String provider, boolean isMock, String promptVersion, String correlationId) { }

    public Preview<RoleAnalysis> role(UUID member, UUID cover) {
        var owned = covers.findByIdAndMemberId(cover, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        String jd = postings.get(member, cover).jdOriginal;
        if (jd == null || jd.isBlank() || jd.length() > 20000) throw invalidRequest();
        AiRequest request = new AiRequest(AiTaskType.ROLE_ANALYSIS,
                "role-analysis", "v1", new AiContext(Map.of(
                        "position", owned.positionName, "jd", jd,
                        "allowedSkills", mapper.writeValueAsString(taxonomy.all()))),
                "role-analysis-v1", UUID.randomUUID().toString(), List.of());
        AiResponse response = gateway.generate(request);
        try {
            RoleExtraction input = mapper.readValue(response.content(), RoleExtraction.class);
            if (input == null || !"v1".equals(input.schemaVersion())
                    || !summaryValid(input.summary()) || input.responsibilities() == null
                    || input.responsibilities().size() > 30 || input.responsibilities().stream()
                    .anyMatch(s -> s == null || s.isBlank() || s.length() > 500)) throw new IllegalArgumentException();
            List<String> skills = taxonomy.normalizeAll(input.skills());
            validateEvidence(input.evidence(), jd, response.isMock());
            var report = new RoleAnalysis(owned.positionName, input.summary(), skills,
                    input.responsibilities(), input.evidence(), "v1");
            return new Preview<>(report, "OWNED_JOB_POSTING",
                    response.isMock() ? List.of("MOCK_ONLY") : List.of("REVIEW_SOURCE_INTERPRETATION"),
                    response.provider(), response.isMock(), request.promptVersion(), response.correlationId());
        } catch (RuntimeException ex) {
            throw invalidOutput();
        }
    }

    public Preview<CompanyAnalysis> company(UUID member, UUID cover, CompanyInput input) {
        var owned = covers.findByIdAndMemberId(cover, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
        if (input == null || input.sourceText() == null || input.sourceText().isBlank()
                || input.sourceText().length() > 20000 || input.sourceUrl() == null
                || input.sourceUrl().length() > 2048) throw invalidRequest();
        URI source;
        try {
            source = URI.create(input.sourceUrl());
            if (!List.of("http", "https").contains(source.getScheme())
                    || source.getHost() == null || source.getRawUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException ex) {
            throw invalidRequest();
        }
        AiRequest request = new AiRequest(AiTaskType.COMPANY_ANALYSIS,
                "company-analysis", "v1", new AiContext(Map.of(
                        "company", owned.companyName, "sourceText", input.sourceText())),
                "company-analysis-v1", UUID.randomUUID().toString(), List.of());
        AiResponse response = gateway.generate(request);
        try {
            CompanyExtraction data = mapper.readValue(response.content(), CompanyExtraction.class);
            if (data == null || !"v1".equals(data.schemaVersion())
                    || !summaryValid(data.summary())) throw new IllegalArgumentException();
            validateEvidence(data.evidence(), input.sourceText(), response.isMock());
            var report = new CompanyAnalysis(owned.companyName, data.summary(),
                    source, Instant.now(), data.evidence(), "v1");
            return new Preview<>(report, "USER_SUPPLIED_TEXT_NOT_FETCHED",
                    response.isMock() ? List.of("MOCK_ONLY", "SOURCE_NOT_VERIFIED")
                            : List.of("SOURCE_NOT_VERIFIED", "REVIEW_SOURCE_INTERPRETATION"),
                    response.provider(), response.isMock(), request.promptVersion(), response.correlationId());
        } catch (RuntimeException ex) {
            throw invalidOutput();
        }
    }

    private static void validateEvidence(List<JobAnalysis.Evidence> spans,
            String source, boolean mock) {
        if (spans == null || spans.size() > 30 || !mock && spans.isEmpty()) {
            throw new IllegalArgumentException("missing source evidence");
        }
        for (JobAnalysis.Evidence span : spans) {
            if (span == null || span.end() > source.length()
                    || !source.substring(span.start(), span.end()).equals(span.quote())) {
                throw new IllegalArgumentException("unverified evidence quote");
            }
        }
    }

    private static boolean summaryValid(String summary) {
        return summary != null && !summary.isBlank() && summary.length() <= 1500;
    }
    private static BusinessException invalidRequest() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }
    private static AiProviderException invalidOutput() {
        return new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                "AI source-bound analysis is invalid");
    }
}
