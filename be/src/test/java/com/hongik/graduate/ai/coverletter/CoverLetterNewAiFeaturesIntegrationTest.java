package com.hongik.graduate.ai.coverletter;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.coverletter.service.CoverLetterService;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.ai.common.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.ai.enabled=true")
class CoverLetterNewAiFeaturesIntegrationTest {
    @Autowired MemberRepository members;
    @Autowired CoverLetterRepository covers;
    @Autowired CoverLetterQuestionRepository questions;
    @Autowired QuestionAnalysisAiService classifier;
    @Autowired CoverLetterAiService coverLetterAi;
    @Autowired CoverLetterEditingChatService chat;
    @Autowired CoverLetterEditTurnRepository history;
    @Autowired CoverLetterService coverLetterService;

    @Test
    void classificationStrategyAndEditingChatRemainPreviewsAndAreOwnerScoped() {
        var owner = member();
        var outsider = member();
        var cover = covers.saveAndFlush(new CoverLetter(
                owner.getId(), "Example", "Engineer", null));
        var question = questions.saveAndFlush(new CoverLetterQuestion(
                cover.id, "Why are you applying?", 20, 1));
        question.answerText = "My manually saved answer";
        questions.saveAndFlush(question);

        assertThatThrownBy(() -> classifier.classify(outsider.getId(), cover.id, question.id))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> chat.history(outsider.getId(), cover.id, question.id, 0, 10))
                .isInstanceOf(BusinessException.class);
        var classified = classifier.classify(owner.getId(), cover.id, question.id);
        assertThat(classified.isMock()).isTrue();
        assertThat(classified.questionType()).isEqualTo(CoverLetterQuestionType.MOTIVATION);
        assertThat(classified.recommendedSkills()).isNotEmpty();

        var strategy = coverLetterAi.strategy(owner.getId(), cover.id, question.id,
                new CoverLetterAiService.DraftInput(CoverLetterQuestionType.MOTIVATION, List.of()));
        assertThat(strategy.isMock()).isTrue();
        assertThat(strategy.warnings()).contains("MOCK_ONLY");

        var input = new CoverLetterEditingChatService.Input(
                CoverLetterQuestionType.MOTIVATION, List.of(),
                "Initial draft", RevisionType.CONCISE, "Make this more concise", "edit-1");
        var first = chat.send(owner.getId(), cover.id, question.id, input);
        assertThat(first.isMock()).isTrue();
        assertThat(first.revisedText()).contains("MOCK ONLY");
        assertThat(chat.send(owner.getId(), cover.id, question.id, input).sequenceNo()).isEqualTo(1);
        assertThat(history.findByQuestionIdOrderBySequenceNoAsc(question.id)).hasSize(1);
        assertThatThrownBy(() -> chat.send(owner.getId(), cover.id, question.id,
                new CoverLetterEditingChatService.Input(
                        CoverLetterQuestionType.MOTIVATION, List.of(), "Different draft",
                        RevisionType.CONCISE, "Make this more concise", "edit-1")))
                .isInstanceOf(BusinessException.class);

        var second = chat.send(owner.getId(), cover.id, question.id,
                new CoverLetterEditingChatService.Input(
                        CoverLetterQuestionType.MOTIVATION, List.of(), first.revisedText(),
                        RevisionType.LOGIC, "Improve reasoning", "edit-2"));
        assertThat(second.sequenceNo()).isEqualTo(2);
        assertThat(chat.history(owner.getId(), cover.id, question.id, 0, 10))
                .extracting(CoverLetterEditingChatService.Turn::sequenceNo).containsExactly(1L, 2L);
        assertThat(questions.findById(question.id).orElseThrow().answerText)
                .isEqualTo("My manually saved answer");
        coverLetterService.delete(owner.getId(), cover.id);
        assertThat(history.findByQuestionIdOrderBySequenceNoAsc(question.id)).isEmpty();
    }

    @Test
    void invalidEditingChatInputDoesNotWriteHistory() {
        var owner = member();
        var cover = covers.saveAndFlush(new CoverLetter(owner.getId(), "Example", "QA", null));
        var question = questions.saveAndFlush(new CoverLetterQuestion(cover.id, "Describe a challenge", 400, 1));
        assertThatThrownBy(() -> chat.send(owner.getId(), cover.id, question.id,
                new CoverLetterEditingChatService.Input(
                        CoverLetterQuestionType.CHALLENGE, List.of(), "draft",
                        RevisionType.LOGIC, " ", "bad")))
                .isInstanceOf(BusinessException.class);
        assertThat(history.findByQuestionIdOrderBySequenceNoAsc(question.id)).isEmpty();
    }

    private Member member() {
        String suffix = UUID.randomUUID().toString();
        return members.saveAndFlush(Member.google(
                "edit-" + suffix + "@example.com", "edit-" + suffix));
    }
}
