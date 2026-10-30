package com.hongik.graduate.ai.job;

import java.util.Map;
import java.util.List;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.ai.common.AiContext;
import com.hongik.graduate.ai.common.AiGateway;
import com.hongik.graduate.ai.common.AiProviderException;
import com.hongik.graduate.ai.common.AiRequest;
import com.hongik.graduate.ai.common.AiResponse;
import com.hongik.graduate.ai.common.AiTaskType;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/** Generates a draft extraction from the owned JD; evidence is verified before persistence. */
@Service
public class JobAnalysisAiService {
    private final JobPostingService postings;
    private final JobAnalysisService analyses;
    private final AiGateway gateway;
    private final ObjectMapper mapper;

    public JobAnalysisAiService(JobPostingService postings, JobAnalysisService analyses,
            AiGateway gateway, ObjectMapper mapper) {
        this.postings = postings;
        this.analyses = analyses;
        this.gateway = gateway;
        this.mapper = mapper;
    }

    public record Result(JobAnalysis analysis, String provider, boolean isMock,
            String correlationId, String promptVersion, boolean persisted) { }

    public Result analyze(UUID member, UUID cover) {
        var posting = postings.get(member, cover); // Always authorize before a remote call.
        String jd = posting.jdOriginal;
        if (jd == null || jd.isBlank() || jd.length() > 20000) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        AiRequest request = new AiRequest(AiTaskType.JOB_ANALYSIS, "job-analysis", "v1",
                new AiContext(Map.of("jd", jd)), "job-analysis-v1",
                UUID.randomUUID().toString(), List.of());
        AiResponse response = gateway.generate(request);
        JobAnalysisService.Input input;
        try {
            input = mapper.readValue(response.content(), JobAnalysisService.Input.class);
        } catch (RuntimeException e) {
            throw invalidOutput();
        }
        if (!JobAnalysis.VERSION.equals(input.schemaVersion())
                || input.analysisStatus() != JobAnalysis.Status.DRAFT) {
            throw invalidOutput();
        }
        if (response.isMock()) {
            JobAnalysis placeholder = new JobAnalysis(posting.id, null, null,
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), JobAnalysis.Status.DRAFT, JobAnalysis.VERSION);
            return new Result(placeholder, response.provider(), true,
                    response.correlationId(), request.promptVersion(), false);
        }
        JobAnalysis saved;
        try {
            saved = analyses.save(member, cover, input);
        } catch (BusinessException e) {
            if (e.errorCode() == ErrorCode.INVALID_REQUEST) {
                throw invalidOutput();
            }
            throw e;
        }
        return new Result(saved, response.provider(), response.isMock(),
                response.correlationId(), request.promptVersion(), true);
    }

    private static AiProviderException invalidOutput() {
        return new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                "AI job analysis failed evidence or schema validation");
    }
}