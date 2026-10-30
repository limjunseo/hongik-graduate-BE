package com.hongik.graduate.ai.coverletter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestion;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestionRepository;
import com.hongik.graduate.ai.common.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class CoverLetterAiServiceTest {
    private final UUID member = UUID.randomUUID(), cover = UUID.randomUUID(),
            question = UUID.randomUUID(), experience = UUID.randomUUID();
    private final CoverLetterRequestFactory factory = mock(CoverLetterRequestFactory.class);
    private final CoverLetterQuestionRepository questions = mock(CoverLetterQuestionRepository.class);
    private final AiGateway gateway = mock(AiGateway.class);
    private final CoverLetterAiService service =
            new CoverLetterAiService(factory, questions, gateway, new ObjectMapper());

    private AiRequest request(String schema) {
        return new AiRequest(AiTaskType.COVER_LETTER_DRAFT, "coverletter-draft", "v1",
                new AiContext(Map.of("context", "{}", "question", "Q")),
                schema, "safe-id", List.of());
    }

    @Test
    void draftIsPreviewOnlyAndWarnsIfOverLimit() {
        when(factory.prepareDraft(eq(member), eq(cover), eq(question),
                eq(CoverLetterQuestionType.MOTIVATION), eq(List.of(experience)), anyString()))
                .thenReturn(request("coverletter-draft-v1"));
        when(questions.findByIdAndCoverLetterId(question, cover))
                .thenReturn(Optional.of(new CoverLetterQuestion(cover, "Q", 4, 0)));
        when(gateway.generate(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "mock", true, "{\"text\":\"hello\",\"characterCount\":5,\"warnings\":[],"
                + "\"usedExperienceIds\":[\"" + experience + "\"],\"schemaVersion\":\"v1\"}",
                "coverletter-draft-v1", "safe-id", new AiUsage(null, null)));
        var result = service.draft(member, cover, question,
                new CoverLetterAiService.DraftInput(CoverLetterQuestionType.MOTIVATION,
                        List.of(experience)));
        assertThat(result.text()).isEqualTo("hello");
        assertThat(result.warnings()).containsExactly("CHARACTER_LIMIT_EXCEEDED");
        assertThat(result.isMock()).isTrue();
        assertThat(result.usedExperienceIds()).containsExactly(experience);
        verify(questions).findByIdAndCoverLetterId(question, cover);
        verifyNoMoreInteractions(questions); // No saves or answer updates.
    }

    @Test
    void hallucinatedExperienceIdsAreRejected() {
        when(factory.prepareDraft(any(), any(), any(), any(), any(), anyString()))
                .thenReturn(request("coverletter-draft-v1"));
        when(gateway.generate(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "openai", false, "{\"text\":\"hi\",\"characterCount\":2,\"warnings\":[],"
                + "\"usedExperienceIds\":[\"" + UUID.randomUUID() + "\"],\"schemaVersion\":\"v1\"}",
                "coverletter-draft-v1", "safe-id", new AiUsage(1, 1)));
        assertThatThrownBy(() -> service.draft(member, cover, question,
                new CoverLetterAiService.DraftInput(CoverLetterQuestionType.MOTIVATION,
                        List.of(experience)))).isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
        verifyNoInteractions(questions);
    }

    @Test
    void invalidAndMissingParametersNeverCallAi() {
        assertThatThrownBy(() -> service.draft(member, cover, question, null))
                .isInstanceOf(com.hongik.graduate.BE.common.exception.BusinessException.class);
        assertThatThrownBy(() -> service.revise(member, cover, question,
                new CoverLetterAiService.RevisionInput(CoverLetterQuestionType.GROWTH,
                        List.of(), "original", null, null)))
                .isInstanceOf(com.hongik.graduate.BE.common.exception.BusinessException.class);
        verifyNoInteractions(gateway);
    }
}