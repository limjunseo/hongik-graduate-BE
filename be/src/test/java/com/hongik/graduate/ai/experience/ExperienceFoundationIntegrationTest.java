package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.domain.ExperienceAnswer;
import com.hongik.graduate.BE.experience.repository.ExperienceAnswerRepository;
import com.hongik.graduate.BE.experience.repository.ExperienceQuestionRepository;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.importdata.ImportedSource;
import com.hongik.graduate.BE.importdata.ImportedSourceRepository;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.jayway.jsonpath.JsonPath;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ExperienceFoundationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @Autowired AuthService auth;
    @Autowired ExperienceRepository experiences;
    @Autowired ExperienceQuestionRepository questions;
    @Autowired ExperienceAnswerRepository answers;
    @Autowired ExperienceSummaryRepository summaries;
    @Autowired ImportedSourceRepository imports;

    @Test
    void summarySaveCompletenessOwnershipEvidenceAndDelete() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Member one = members.saveAndFlush(Member.google(
                "summary-" + suffix + "@example.com", "summary-" + suffix));
        Member other = members.saveAndFlush(Member.google(
                "other-" + suffix + "@example.com", "other-" + suffix));
        String token = auth.issueForAuthenticatedMember(one).accessToken();
        String otherToken = auth.issueForAuthenticatedMember(other).accessToken();
        Experience exp = experiences.saveAndFlush(
                new Experience(one.getId(), "STAR", "백엔드 프로젝트",
                        "QUICK", null, null));
        exp.summary = "기존 자유 텍스트 요약";
        experiences.saveAndFlush(exp);
        ImportedSource source = new ImportedSource(one.getId(), "URL", "public page");
        source.sourceUrl = "https://example.org/article";
        source.collectedText = "첫 번째 프로젝트 문단.\n\n두 번째 프로젝트 문단.";
        source = imports.saveAndFlush(source);
        String base = "/api/experiences/" + exp.id;
        var situationQuestion = questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc("STAR")
                .stream().filter(q -> q.code.equals("SITUATION")).findFirst().orElseThrow();
        answers.saveAndFlush(new ExperienceAnswer(exp.id, situationQuestion.id,
                null, "기존 빠른 작성의 상황", 1));
        mvc.perform(get(base + "/structured-summary/preview-from-answers")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.situation").value("기존 빠른 작성의 상황"))
                .andExpect(jsonPath("$.data.task").doesNotExist());
        assertThat(summaries.existsById(exp.id)).isFalse();
        mvc.perform(get(base + "/structured-summary/preview-from-answers")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/skills").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value("v1"))
                .andExpect(jsonPath("$.data.skills.length()").value(11));
        mvc.perform(get("/api/experience-tags/STAR/ai-questions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.questions.length()").value(4));
        mvc.perform(get("/api/experience-tags/UNKNOWN/ai-questions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/skills")).andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/completeness")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completionPercent").value(0));
        mvc.perform(get(base + "/structured-summary")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        String candidateJson = mvc.perform(get("/api/imports/" + source.id
                + "/experience-candidates").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REVIEW_REQUIRED"))
                .andReturn().getResponse().getContentAsString();
        String quote = JsonPath.read(candidateJson, "$.data.candidates[0].quote");
        String location = JsonPath.read(candidateJson, "$.data.candidates[0].location");
        assertThat(quote).isEqualTo("첫 번째 프로젝트 문단.");

        String input = """
                {"schemaVersion":"v1","situation":"프로젝트에서 협업",
                 "skills":["협업","collaboration","백엔드 개발"],
                 "keywords":["Java"],"evidenceSources":[
                    {"sourceId":"%s","location":"%s","quote":"%s"}]}
                """.formatted(source.id, location, quote);
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.origin").value("USER"))
                .andExpect(jsonPath("$.data.skills.length()").value(2));
        mvc.perform(get(base + "/completeness")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completionPercent").value(25))
                .andExpect(jsonPath("$.data.missingFields[0]").value("task"));
        mvc.perform(get(base + "/structured-summary")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.situation").value("프로젝트에서 협업"))
                .andExpect(jsonPath("$.data.keywords[0]").value("Java"));
        assertThat(experiences.findById(exp.id).orElseThrow().summary)
                .isEqualTo("기존 자유 텍스트 요약");

        mvc.perform(get(base + "/structured-summary")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + otherToken)
                .contentType(MediaType.APPLICATION_JSON).content(input))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/imports/" + source.id + "/experience-candidates")
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        String forgedQuote = """
                {"situation":"s","evidenceSources":[
                  {"sourceId":"%s","location":"%s","quote":"조작된 성과"}]}
                """.formatted(source.id, location);
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(forgedQuote))
                .andExpect(status().isBadRequest());
        ImportedSource inaccessibleSource = new ImportedSource(other.getId(), "URL", "private");
        inaccessibleSource.collectedText = "private data";
        inaccessibleSource = imports.saveAndFlush(inaccessibleSource);
        String otherEvidence = """
                {"situation":"s","evidenceSources":[
                  {"sourceId":"%s","location":"url:paragraph:1","quote":"private data"}]}
                """.formatted(inaccessibleSource.id);
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(otherEvidence))
                .andExpect(status().isNotFound());
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"skills\":[\"not-known\"]}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(base + "/structured-summary")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"schemaVersion\":\"v2\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(delete(base).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        assertThat(summaries.existsById(exp.id)).isFalse();
    }
}
