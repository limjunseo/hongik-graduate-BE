package com.hongik.graduate.ai.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.coverletter.domain.JobPosting;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.ai.common.*;
import com.hongik.graduate.ai.experience.SkillTaxonomy;
import org.springframework.core.io.DefaultResourceLoader;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JobAnalysisAiServiceTest {
    private final UUID member = UUID.randomUUID(), cover = UUID.randomUUID();
    private final JobPostingService postings = mock(JobPostingService.class);
    private final JobAnalysisService analyses = mock(JobAnalysisService.class);
    private final AiGateway gateway = mock(AiGateway.class);
    private final JobAnalysisAiService service =
            new JobAnalysisAiService(postings, analyses, gateway, new ObjectMapper(),
                    new SkillTaxonomy(new ObjectMapper(), new DefaultResourceLoader()));

    private void posting(String jd) {
        JobPosting job = new JobPosting(cover);
        job.id = UUID.randomUUID();
        job.jdOriginal = jd;
        when(postings.get(member, cover)).thenReturn(job);
    }

    private void generated(String analysisStatus) {
        when(gateway.generate(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "openai", false,
                "{\"requiredSkills\":[],\"preferredSkills\":[],\"responsibilities\":[],"
                + "\"coreCompetencies\":[],\"keywords\":[],\"companyValues\":[],"
                + "\"evidence\":[{\"start\":0,\"end\":4,\"quote\":\"Java\"}],"
                + "\"analysisStatus\":\"" + analysisStatus + "\",\"schemaVersion\":\"v1\"}",
                "job-analysis-v1", "req-1", new AiUsage(2, 3)));
    }

    @Test
    void extractionIsSavedAsDraftOnlyAfterValidation() {
        posting("Java REST API");
        generated("DRAFT");
        JobAnalysis expected = mock(JobAnalysis.class);
        when(analyses.save(eq(member), eq(cover), any())).thenReturn(expected);
        var result = service.analyze(member, cover);
        assertThat(result.analysis()).isSameAs(expected);
        assertThat(result.provider()).isEqualTo("openai");
        var captured = org.mockito.ArgumentCaptor.forClass(JobAnalysisService.Input.class);
        verify(analyses).save(eq(member), eq(cover), captured.capture());
        assertThat(captured.getValue().analysisStatus()).isEqualTo(JobAnalysis.Status.DRAFT);
        assertThat(captured.getValue().evidence().get(0).quote()).isEqualTo("Java");
    }

    @Test
    void refusesModelClaimThatAnalysisIsConfirmed() {
        posting("Java REST API");
        generated("CONFIRMED");
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        verify(analyses).get(member, cover);
        verify(analyses, never()).save(any(), any(), any());
    }

    @Test
    void noOwnedPostingTextMeansNoProviderCall() {
        posting(" ");
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(gateway, analyses);
    }

    @Test
    void promptIncludesCanonicalSkillsForTheModel() {
        posting("Java REST API");
        generated("DRAFT");
        when(analyses.save(eq(member), eq(cover), any()))
                .thenReturn(mock(JobAnalysis.class));
        service.analyze(member, cover);
        var request = org.mockito.ArgumentCaptor.forClass(AiRequest.class);
        verify(gateway).generate(request.capture());
        assertThat(request.getValue().context().values()).containsKey("allowedSkills");
        assertThat(request.getValue().context().values().get("allowedSkills").toString())
                .contains("code");
        var prompt = new AiPromptTemplateRegistry(new DefaultResourceLoader())
                .resolve("job-analysis", "v1").render(request.getValue().context());
        assertThat(prompt).contains("ALLOWED_SKILLS_START", "Java REST API");
    }

    @Test
    void refusesModelGeneratedFactsWithoutVerbatimEvidence() {
        posting("Java REST API");
        String unsupported = "{\"requiredSkills\":[\"communication\"],"
                + "\"preferredSkills\":[],\"responsibilities\":[],\"coreCompetencies\":[],"
                + "\"keywords\":[],\"companyValues\":[],\"evidence\":[],"
                + "\"analysisStatus\":\"DRAFT\",\"schemaVersion\":\"v1\"}";
        when(gateway.generate(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "openai", false, unsupported, "job-analysis-v1", "req-2", new AiUsage(1, 1)));
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        verify(analyses, never()).save(any(), any(), any());
    }

    @Test
    void nullJsonIsRejectedAsInvalidOutput() {
        posting("Java REST API");
        when(gateway.generate(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "openai", false, "null", "job-analysis-v1", "req-3", new AiUsage(1, 1)));
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        verify(analyses, never()).save(any(), any(), any());
    }

    @Test
    void confirmedAnalysisIsNeverReplacedWithoutUserApproval() {
        posting("Java REST API");
        var confirmed = new JobAnalysis(UUID.randomUUID(), null, null,
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), JobAnalysis.Status.CONFIRMED, JobAnalysis.VERSION);
        when(analyses.get(member, cover)).thenReturn(confirmed);
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(gateway);
        verify(analyses, never()).save(any(), any(), any());
    }

    @Test
    void detectsConfirmationWhileGenerationWasInFlight() {
        posting("Java REST API");
        generated("DRAFT");
        var confirmed = new JobAnalysis(UUID.randomUUID(), null, null,
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), java.util.List.of(), java.util.List.of(),
                java.util.List.of(), JobAnalysis.Status.CONFIRMED, JobAnalysis.VERSION);
        when(analyses.get(member, cover)).thenReturn(null, confirmed);
        assertThatThrownBy(() -> service.analyze(member, cover))
                .isInstanceOf(BusinessException.class);
        verify(analyses, never()).save(any(), any(), any());
    }
}
