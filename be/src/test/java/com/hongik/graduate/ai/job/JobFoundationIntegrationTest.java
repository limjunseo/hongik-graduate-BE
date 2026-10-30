package com.hongik.graduate.ai.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.ai.experience.*;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest @AutoConfigureMockMvc
class JobFoundationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired AuthService auth;
    @Autowired CoverLetterRepository covers;
    @Autowired JobPostingService postingService;
    @Autowired JobAnalysisRepository analyses;
    @Autowired ExperienceRepository experiences;
    @Autowired ExperienceSummaryRepository summaries;
    @Autowired ObjectMapper mapper;

    @Test void manualAnalysisRankingOwnershipEvidenceAndStaleness() throws Exception {
        String suffix = UUID.randomUUID().toString();
        Member owner = members.saveAndFlush(Member.google("job-" + suffix + "@example.com", "job-" + suffix));
        Member stranger = members.saveAndFlush(Member.google("other-" + suffix + "@example.com", "other-" + suffix));
        String token = "Bearer " + auth.issueForAuthenticatedMember(owner).accessToken();
        String other = "Bearer " + auth.issueForAuthenticatedMember(stranger).accessToken();
        var cover = covers.saveAndFlush(new CoverLetter(
                owner.getId(), "Example Company", "Backend Developer", null));
        String jd = "Backend developer builds APIs and communicates with others.";
        postingService.save(owner.getId(), cover.id,
                new JobPostingService.Input(null, jd, null, null, null));
        Experience first = experiences.saveAndFlush(new Experience(
                owner.getId(), "STAR", "API project", "QUICK", null, null));
        Experience second = experiences.saveAndFlush(new Experience(
                owner.getId(), "STAR", "Other project", "QUICK", null, null));
        Experience privateExp = experiences.saveAndFlush(new Experience(
                stranger.getId(), "STAR", "Private project", "QUICK", null, null));
        ExperienceSummary summary = new ExperienceSummary(first.id, first.title, "STAR",
                "s", "t", "a", "r", null, null, null,
                List.of("backend", "problem_solving"), List.of("Java"), List.of(), "v1", "USER");
        summaries.saveAndFlush(new ExperienceSummaryEntity(first.id,
                mapper.writeValueAsString(summary), "v1", "USER"));
        summaries.saveAndFlush(new ExperienceSummaryEntity(privateExp.id,
                mapper.writeValueAsString(summary), "v1", "USER"));
        String base = "/api/cover-letters/" + cover.id;
        String valid = """
                {"requiredSkills":["backend","problem_solving","communication"],
                 "preferredSkills":["collaboration"],"responsibilities":["build APIs"],
                 "coreCompetencies":["teamwork"],"keywords":["Java"],
                 "evidence":[{"start":0,"end":7,"quote":"Backend"}],
                 "analysisStatus":"CONFIRMED"}
                """;
        // Invalid normalized alias must fail; unknown skill is not silently accepted.
        mvc.perform(put(base + "/posting/analysis").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isBadRequest());
        valid = valid.replace("teamwork", "collaboration");
        mvc.perform(put(base + "/posting/analysis").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content(valid))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.analysisStatus").value("CONFIRMED"));
        mvc.perform(get(base + "/experience-recommendations").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.method").value("RULE_BASED"))
                .andExpect(jsonPath("$.data.results[0].score").value(67))
                .andExpect(jsonPath("$.data.results[0].experienceId").value(first.id.toString()))
                .andExpect(jsonPath("$.data.results[1].score").value(0));
        mvc.perform(get(base + "/posting/analysis").header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(get(base + "/experience-recommendations").header("Authorization", other))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/cover-letter-question-types").header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.types.length()").value(8));
        mvc.perform(get(base + "/posting/analysis").header("Authorization", token))
                .andExpect(status().isOk());
        mvc.perform(put(base + "/posting/analysis").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"evidence\":[{\"start\":0,\"end\":7,\"quote\":\"fabricate\"}]}"))
                .andExpect(status().isBadRequest());
        assertThat(analyses.count()).isGreaterThanOrEqualTo(1);
        postingService.save(owner.getId(), cover.id,
                new JobPostingService.Input(null, jd + " Updated", null, null, null));
        mvc.perform(get(base + "/posting/analysis").header("Authorization", token))
                .andExpect(status().isNotFound());
        assertThat(analyses.findAll().stream().noneMatch(x ->
                x.jobPostingId.equals(postingService.get(owner.getId(), cover.id).id))).isTrue();
    }
}
