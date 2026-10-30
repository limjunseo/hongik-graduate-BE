package com.hongik.graduate.ai.coverletter;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.ai.common.*;
import com.hongik.graduate.ai.experience.*;
import com.hongik.graduate.ai.job.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class CoverLetterPipelineIntegrationTest {
    @Autowired MemberRepository members;
    @Autowired CoverLetterRepository covers;
    @Autowired CoverLetterQuestionRepository questions;
    @Autowired QuestionExperienceLinkRepository links;
    @Autowired ExperienceRepository experiences;
    @Autowired ExperienceSummaryRepository summaries;
    @Autowired JobPostingService postings;
    @Autowired JobAnalysisService analysisService;
    @Autowired CoverLetterContextBuilder builder;
    @Autowired SupportStrategyContextBuilder strategies;
    @Autowired CoverLetterRequestFactory factory;
    @Autowired AiProvider provider;
    @Autowired AiGateway gateway;
    @Autowired ObjectMapper mapper;

    @Test void authorizedContextAndMockPipelineNeverSaveAnswer() {
        String suffix = UUID.randomUUID().toString();
        Member owner = members.saveAndFlush(Member.google(
                "ctx-" + suffix + "@example.com", "ctx-" + suffix));
        Member other = members.saveAndFlush(Member.google(
                "ctxother-" + suffix + "@example.com", "ctxother-" + suffix));
        CoverLetter cover = covers.saveAndFlush(new CoverLetter(
                owner.getId(), "Example Corp", "Backend Engineer", null));
        CoverLetterQuestion question = questions.saveAndFlush(new CoverLetterQuestion(
                cover.id, "How did you solve the problem?", 700, 1));
        question.answerText = "User's original draft";
        questions.saveAndFlush(question);
        Experience exp = experiences.saveAndFlush(
                new Experience(owner.getId(), "STAR", "Backend API", "QUICK", null, null));
        Experience privateExp = experiences.saveAndFlush(
                new Experience(other.getId(), "STAR", "Private", "QUICK", null, null));
        links.saveAndFlush(new QuestionExperienceLink(question.id, exp.id, 1));
        Experience notLinked = experiences.saveAndFlush(new Experience(
                owner.getId(), "STAR", "Unlinked", "QUICK", null, null));
        Experience secondLinked = experiences.saveAndFlush(new Experience(
                owner.getId(), "STAR", "Second linked", "QUICK", null, null));
        links.saveAndFlush(new QuestionExperienceLink(question.id, secondLinked.id, 2));
        var summary = new ExperienceSummary(exp.id, exp.title, exp.tagCode,
                "context", "task", "implemented", "improved", null, null, null,
                List.of("backend", "problem_solving"), List.of("Java"), List.of(), "v1", "USER");
        summaries.saveAndFlush(new ExperienceSummaryEntity(exp.id,
                mapper.writeValueAsString(summary), "v1", "USER"));
        var secondSummary = new ExperienceSummary(secondLinked.id, secondLinked.title,
                secondLinked.tagCode, "s", "t", "a", "r", null, null, null,
                List.of("collaboration"), List.of(), List.of(), "v1", "USER");
        summaries.saveAndFlush(new ExperienceSummaryEntity(secondLinked.id,
                mapper.writeValueAsString(secondSummary), "v1", "USER"));
        var contextWithoutJd = builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id));
        assertThat(contextWithoutJd.warnings()).contains("JD_NOT_ANALYZED");
        assertThat(contextWithoutJd.rankedMatches().get(0).status())
                .isEqualTo(ExperienceMatchResult.Status.NOT_SCORABLE);
        var contextWithoutExperience = builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of());
        assertThat(contextWithoutExperience.warnings()).contains("MISSING_EXPERIENCE");
        postings.save(owner.getId(), cover.id, new JobPostingService.Input(
                null, "backend communication", null, null, null));
        analysisService.save(owner.getId(), cover.id, new JobAnalysisService.Input(
                "v1", List.of("backend", "communication"), List.of(),
                List.of("API development"), List.of("backend"),
                List.of("Java"), List.of(), List.of(), JobAnalysis.Status.DRAFT));

        var context = builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id));
        assertThat(context.company()).isEqualTo("Example Corp");
        assertThat(context.characterLimit()).isEqualTo(700);
        assertThat(context.jobAnalysis().requiredSkills()).containsExactly("backend", "communication");
        assertThat(context.rankedMatches().get(0).score()).isEqualTo(50);
        assertThat(context.experiences()).hasSize(1);
        var multi = builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id, secondLinked.id));
        assertThat(multi.experiences()).hasSize(2);
        assertThat(multi.rankedMatches()).extracting(ExperienceMatchResult::score)
                .containsExactly(50, 0);
        assertThat(context.provenance()).anyMatch(x -> x.equals("experience:" + exp.id));
        assertThat(mapper.writeValueAsString(context)).isEqualTo(mapper.writeValueAsString(
                builder.build(owner.getId(), cover.id, question.id,
                        CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id))));

        var request = factory.prepareDraft(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id), "draft-1");
        assertThat(request.outputSchemaId()).isEqualTo("coverletter-draft-v1");
        AiResponse mockResponse = provider.generate(request);
        assertThat(mockResponse.isMock()).isTrue();
        var drafted = mapper.readValue(mockResponse.content(), CoverLetterDraftResult.class);
        assertThat(drafted.text()).contains("MOCK ONLY");
        assertThatThrownBy(() -> gateway.generate(request))
                .isInstanceOf(AiProviderException.class).extracting("code")
                .isEqualTo(AiProviderException.Code.DISABLED);

        for (RevisionType type : RevisionType.values()) {
            var revision = factory.prepareRevision(owner.getId(), cover.id, question.id,
                    CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id),
                    question.answerText, type, "improve clarity", "revision-1");
            assertThat(mapper.readValue(provider.generate(revision).content(), RevisionResult.class)
                    .revisedText()).contains("MOCK ONLY");
        }
        var chatContext = new EditingChatContext(context, "Original", RevisionType.LOGIC,
                List.of(new AiMessage(AiMessage.Role.USER, "Revise logic")), "v1");
        assertThat(provider.chat(factory.prepareEditingChat(chatContext, "chat-1")).isMock()).isTrue();
        assertThat(provider.generate(factory.prepareSupportStrategy(owner.getId(),
                cover.id, question.id, CoverLetterQuestionType.JOB_COMPETENCY,
                List.of(exp.id), "strategy-1")).isMock()).isTrue();
        assertThat(strategies.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id)).evidenceGaps())
                .containsExactly("communication");

        assertThat(questions.findById(question.id).orElseThrow().answerText)
                .isEqualTo("User's original draft");
        assertThatThrownBy(() -> builder.build(other.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(notLinked.id)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(privateExp.id)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> builder.build(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id, exp.id)))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> factory.prepareDraft(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(), "missing-exp"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> factory.prepareRevision(owner.getId(), cover.id, question.id,
                CoverLetterQuestionType.JOB_COMPETENCY, List.of(exp.id),
                "", RevisionType.LOGIC, null, "bad"))
                .isInstanceOf(BusinessException.class);
    }
}
