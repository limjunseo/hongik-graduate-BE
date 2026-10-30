package com.hongik.graduate.ai.experience;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ExperienceAiChatService {
    private static final int MAX_STORED_MESSAGES = 200;
    private static final int MAX_CONTEXT_MESSAGES = 20;
    private final ExperienceRepository experiences;
    private final ExperienceAiChatMessageRepository messages;
    private final AiGateway gateway;
    private final EntityManager entityManager;

    public ExperienceAiChatService(ExperienceRepository experiences,
            ExperienceAiChatMessageRepository messages, AiGateway gateway,
            EntityManager entityManager) {
        this.experiences = experiences;
        this.messages = messages;
        this.gateway = gateway;
        this.entityManager = entityManager;
    }

    public record SendInput(String message, String requestId) { }
    public record Turn(String role, String content, long sequenceNo,
            String requestId, boolean isMock, String provider) { }
    public record Reply(String requestId, Turn user, Turn assistant,
            String provider, boolean isMock, String promptVersion, AiUsage usage) { }

    private Experience own(UUID member, UUID id) {
        return experiences.findByIdAndMemberId(id, member)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Turn> list(UUID member, UUID id, int page, int size) {
        own(member, id);
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<ExperienceAiChatMessage> stored = messages.findByExperienceIdOrderBySequenceNoAsc(id);
        return stored.stream().skip((long) page * size).limit(size)
                .map(ExperienceAiChatService::toTurn).toList();
    }

    public Reply send(UUID member, UUID id, SendInput input) {
        Experience experience = own(member, id);
        if (input == null || input.message() == null || input.message().isBlank()
                || input.message().length() > 2000
                || input.requestId() != null
                && !input.requestId().matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // Serialize concurrent writes for this experience across application instances.
        // The lock remains held until both turns are committed, including the model call.
        entityManager.lock(experience, LockModeType.PESSIMISTIC_WRITE);
        String idempotencyKey = input.requestId() == null
                ? UUID.randomUUID().toString() : input.requestId();
        List<ExperienceAiChatMessage> stored = messages.findByExperienceIdOrderBySequenceNoAsc(id);
        for (int i = 0; i < stored.size(); i++) {
            ExperienceAiChatMessage item = stored.get(i);
            if (idempotencyKey.equals(item.requestId)) {
                if (i + 1 < stored.size()
                        && idempotencyKey.equals(stored.get(i + 1).requestId)
                        && item.role.equals("USER")
                        && stored.get(i + 1).role.equals("ASSISTANT")
                        && item.content.equals(input.message())) {
                    ExperienceAiChatMessage assistant = stored.get(i + 1);
                    return new Reply(idempotencyKey, toTurn(item), toTurn(assistant),
                            assistant.provider, assistant.mock, assistant.promptVersion,
                            new AiUsage(null, null));
                }
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
        if (stored.size() > MAX_STORED_MESSAGES - 2) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        List<AiMessage> history = new ArrayList<>();
        for (ExperienceAiChatMessage prior : stored.subList(
                Math.max(0, stored.size() - MAX_CONTEXT_MESSAGES), stored.size())) {
            history.add(new AiMessage(
                    prior.role.equals("USER") ? AiMessage.Role.USER : AiMessage.Role.ASSISTANT,
                    prior.content));
        }
        history.add(new AiMessage(AiMessage.Role.USER, input.message()));
        String instructions = "Help the user clarify this experience without inventing facts. "
                + "Ask one grounded follow-up question if facts are missing. "
                + "Experience title: " + experience.title + "; tag: " + experience.tagCode;
        AiRequest request = new AiRequest(AiTaskType.EXPERIENCE_CHAT, "chat", "v1",
                new AiContext(Map.of("instruction", instructions)),
                null, idempotencyKey, history);
        AiResponse ai = gateway.chat(request); // Nothing is saved on a failed model call.
        long next = stored.isEmpty() ? 1 : stored.get(stored.size() - 1).sequenceNo + 1;
        ExperienceAiChatMessage userTurn = new ExperienceAiChatMessage(id, member, next,
                idempotencyKey, "USER", input.message(), "user", false, request.promptVersion());
        ExperienceAiChatMessage assistantTurn = new ExperienceAiChatMessage(id, member, next + 1,
                idempotencyKey, "ASSISTANT", ai.content(),
                ai.provider(), ai.isMock(), request.promptVersion());
        messages.saveAll(List.of(userTurn, assistantTurn));
        return new Reply(idempotencyKey, toTurn(userTurn), toTurn(assistantTurn),
                ai.provider(), ai.isMock(), request.promptVersion(), ai.usage());
    }

    private static Turn toTurn(ExperienceAiChatMessage message) {
        return new Turn(message.role, message.content, message.sequenceNo,
                message.requestId, message.mock, message.provider);
    }
}