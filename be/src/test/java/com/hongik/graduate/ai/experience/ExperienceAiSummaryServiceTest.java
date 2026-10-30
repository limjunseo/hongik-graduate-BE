package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.ai.common.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;

class ExperienceAiSummaryServiceTest {
    private final UUID member = UUID.randomUUID(), id = UUID.randomUUID();
    private final ExperienceQuickAnswerPreviewService quick =
            mock(ExperienceQuickAnswerPreviewService.class);
    private final ExperienceAiChatMessageRepository chats =
            mock(ExperienceAiChatMessageRepository.class);
    private final AiGateway gateway = mock(AiGateway.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final SkillTaxonomy taxonomy = new SkillTaxonomy(mapper, new DefaultResourceLoader());
    private final ExperienceAiSummaryService service =
            new ExperienceAiSummaryService(quick, chats, taxonomy, gateway, mapper);

    private void facts(String situation) {
        when(quick.preview(member, id)).thenReturn(new ExperienceSummary(
                id, "Project", "STAR", situation, null, null, null,
                null, null, null, List.of(), List.of(), List.of(), "v1", "USER"));
        when(chats.findByExperienceIdOrderBySequenceNoAsc(id)).thenReturn(List.of());
    }

    private void generated(String skills) {
        when(gateway.generate(any())).thenReturn(new AiResponse(
                AiResponse.Status.SUCCESS, "openai", false,
                "{\"schemaVersion\":\"v1\",\"situation\":\"A project\","
                + "\"task\":null,\"action\":null,\"result\":null,\"role\":null,"
                + "\"conflict\":null,\"learned\":null,\"skills\":" + skills + ","
                + "\"keywords\":[],\"evidenceSources\":[]}",
                "experience-summary-v1", "req-1", new AiUsage(10, 20)));
    }

    @Test
    void generationProducesUnverifiedPreviewOnly() {
        facts("A project");
        generated("[]");
        var preview = service.generate(member, id);
        assertThat(preview.summary().origin()).isEqualTo("AI");
        assertThat(preview.summary().situation()).isEqualTo("A project");
        assertThat(preview.warnings()).contains("REQUIRES_USER_CONFIRMATION");
        assertThat(preview.summary().evidenceSources()).isEmpty();
        verify(gateway).generate(any());
    }

    @Test
    void rejectsUnrecognizedSkillsRatherThanPersistingAnUnsafeSuggestion() {
        facts("A project");
        generated("[\"NONEXISTENT_SKILL\"]");
        assertThatThrownBy(() -> service.generate(member, id))
                .isInstanceOf(AiProviderException.class)
                .extracting("code").isEqualTo(AiProviderException.Code.INVALID_OUTPUT);
    }

    @Test
    void refusesEmptySourceFactsBeforeCallingModel() {
        facts(null);
        assertThatThrownBy(() -> service.generate(member, id))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(gateway);
    }
}