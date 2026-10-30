package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.ai.common.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

class ExperienceAiChatServiceTest {
    private final UUID member = UUID.randomUUID(), experience = UUID.randomUUID();
    private final ExperienceRepository experiences = mock(ExperienceRepository.class);
    private final ExperienceAiChatMessageRepository messages =
            mock(ExperienceAiChatMessageRepository.class);
    private final AiGateway gateway = mock(AiGateway.class);
    private final EntityManager entityManager = mock(EntityManager.class);
    private final ExperienceAiChatService service =
            new ExperienceAiChatService(experiences, messages, gateway, entityManager);

    private void owned() {
        Experience item = new Experience(member, "STAR", "A project", "CONVERSATION", null, null);
        item.id = experience;
        when(experiences.findByIdAndMemberId(experience, member))
                .thenReturn(Optional.of(item));
    }

    @Test
    void sendsOrderedConversationAndStoresPairAfterSuccess() {
        owned();
        when(messages.findByExperienceIdOrderBySequenceNoAsc(experience))
                .thenReturn(List.of());
        when(gateway.chat(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "mock", true, "[MOCK ONLY] Question", null, "req-10",
                new AiUsage(null, null)));
        var result = service.send(member, experience,
                new ExperienceAiChatService.SendInput("A project story", "req-10"));
        assertThat(result.user().sequenceNo()).isEqualTo(1);
        assertThat(result.assistant().sequenceNo()).isEqualTo(2);
        assertThat(result.isMock()).isTrue();
        var request = ArgumentCaptor.forClass(AiRequest.class);
        verify(gateway).chat(request.capture());
        assertThat(request.getValue().messages()).hasSize(1);
        assertThat(request.getValue().messages().get(0).role())
                .isEqualTo(AiMessage.Role.USER);
        var stored = ArgumentCaptor.forClass(List.class);
        verify(messages).saveAll(stored.capture());
        assertThat(stored.getValue()).hasSize(2);
        verify(entityManager).lock(any(Experience.class), eq(LockModeType.PESSIMISTIC_WRITE));
    }

    @Test
    void replayingSameRequestIdReturnsPreviousAnswerWithoutCallingModel() {
        owned();
        var user = new ExperienceAiChatMessage(experience, member, 1, "id-1",
                "USER", "Hello", "user", false, "v1");
        var assistant = new ExperienceAiChatMessage(experience, member, 2, "id-1",
                "ASSISTANT", "Welcome", "mock", true, "v1");
        when(messages.findByExperienceIdOrderBySequenceNoAsc(experience))
                .thenReturn(List.of(user, assistant));
        var reply = service.send(member, experience,
                new ExperienceAiChatService.SendInput("Hello", "id-1"));
        assertThat(reply.assistant().content()).isEqualTo("Welcome");
        verifyNoInteractions(gateway);
        verify(messages, never()).saveAll(any());
    }

    @Test
    void forbiddenAccessAndFailedGenerationDoNotWriteMessages() {
        assertThatThrownBy(() -> service.send(member, experience,
                new ExperienceAiChatService.SendInput("hello", "id-1")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(gateway, messages);
        owned();
        when(messages.findByExperienceIdOrderBySequenceNoAsc(experience))
                .thenReturn(List.of());
        when(gateway.chat(any())).thenThrow(
                new AiProviderException(AiProviderException.Code.TIMEOUT, "timeout"));
        assertThatThrownBy(() -> service.send(member, experience,
                new ExperienceAiChatService.SendInput("hello", "id-1")))
                .isInstanceOf(AiProviderException.class);
        verify(messages, never()).saveAll(any());
    }

    @Test
    void rejectsBlankOrOversizedMessagesBeforeCallingModel() {
        owned();
        assertThatThrownBy(() -> service.send(member, experience,
                new ExperienceAiChatService.SendInput(" ", "id")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.send(member, experience,
                new ExperienceAiChatService.SendInput("a".repeat(2001), "id")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(gateway);
    }

    @Test
    void locksOwnerRowBeforeReadingHistoryOrCallingProvider() {
        owned();
        when(messages.findByExperienceIdOrderBySequenceNoAsc(experience))
                .thenReturn(List.of());
        when(gateway.chat(any())).thenReturn(new AiResponse(AiResponse.Status.SUCCESS,
                "mock", true, "reply", null, "id-serial", new AiUsage(null, null)));
        service.send(member, experience,
                new ExperienceAiChatService.SendInput("first", "id-serial"));
        var order = inOrder(entityManager, messages, gateway);
        order.verify(entityManager).lock(any(Experience.class), eq(LockModeType.PESSIMISTIC_WRITE));
        order.verify(messages).findByExperienceIdOrderBySequenceNoAsc(experience);
        order.verify(gateway).chat(any());
        order.verify(messages).saveAll(any());
    }}