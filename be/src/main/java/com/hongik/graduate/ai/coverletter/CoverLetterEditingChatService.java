package com.hongik.graduate.ai.coverletter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterRepository;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestionRepository;
import com.hongik.graduate.ai.common.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/** Each stored turn is an unapproved edit preview, never a saved cover-letter answer. */
@Service
@Transactional
public class CoverLetterEditingChatService {
    private static final int MAX_TURNS = 100;
    private final CoverLetterRepository covers;
    private final CoverLetterQuestionRepository questions;
    private final CoverLetterEditTurnRepository turns;
    private final CoverLetterContextBuilder contexts;
    private final CoverLetterRequestFactory factory;
    private final AiGateway gateway;
    private final ObjectMapper mapper;
    private final EntityManager manager;

    public CoverLetterEditingChatService(CoverLetterRepository covers,
            CoverLetterQuestionRepository questions, CoverLetterEditTurnRepository turns,
            CoverLetterContextBuilder contexts, CoverLetterRequestFactory factory,
            AiGateway gateway, ObjectMapper mapper, EntityManager manager) {
        this.covers = covers; this.questions = questions; this.turns = turns;
        this.contexts = contexts; this.factory = factory; this.gateway = gateway;
        this.mapper = mapper; this.manager = manager;
    }

    public record Input(CoverLetterQuestionType questionType, List<UUID> experienceIds,
            String currentDraft, RevisionType revisionType, String instruction,
            String requestId) { }

    public record Turn(long sequenceNo, String requestId, CoverLetterQuestionType questionType,
            List<UUID> experienceIds, String originalText, String instruction,
            RevisionType revisionType, String revisedText, int characterCount,
            List<String> changeReasons, List<String> warnings, String provider,
            boolean isMock, String promptVersion, Instant createdAt) { }

    private com.hongik.graduate.BE.coverletter.domain.CoverLetterQuestion own(
            UUID member, UUID cover, UUID question) {
        if (covers.findByIdAndMemberId(cover, member).isEmpty()) {
            throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
        }
        return questions.findByIdAndCoverLetterId(question, cover)
                .orElseThrow(() -> new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<Turn> history(UUID member, UUID cover, UUID question, int page, int size) {
        own(member, cover, question);
        if (page < 0 || size < 1 || size > 50) throw invalid();
        return turns.findByQuestionIdOrderBySequenceNoAsc(question).stream()
                .skip((long) page * size).limit(size).map(this::toTurn).toList();
    }

    public Turn send(UUID member, UUID cover, UUID question, Input input) {
        var entity = own(member, cover, question);
        if (input == null || input.questionType() == null || input.experienceIds() == null
                || input.currentDraft() == null || input.currentDraft().isBlank()
                || input.currentDraft().length() > 10000 || input.revisionType() == null
                || input.instruction() == null || input.instruction().isBlank()
                || input.instruction().length() > 1000
                || input.requestId() != null && !input.requestId().matches("[A-Za-z0-9_.:-]{1,128}")) {
            throw invalid();
        }
        // Serializes retries and sequence allocation for the same question.
        manager.lock(entity, LockModeType.PESSIMISTIC_WRITE);
        String requestId = input.requestId() == null ? UUID.randomUUID().toString() : input.requestId();
        List<CoverLetterEditTurn> previous = turns.findByQuestionIdOrderBySequenceNoAsc(question);
        String selectedJson = mapper.writeValueAsString(input.experienceIds());
        for (var prior : previous) {
            if (prior.requestId.equals(requestId)) {
                if (!prior.memberId.equals(member) || !prior.questionType.equals(input.questionType().name())
                        || !prior.experienceIdsJson.equals(selectedJson)
                        || !prior.draftBefore.equals(input.currentDraft())
                        || !prior.instruction.equals(input.instruction())
                        || !prior.revisionType.equals(input.revisionType().name())) throw invalid();
                return toTurn(prior);
            }
        }
        if (previous.size() >= MAX_TURNS) throw invalid();
        var context = contexts.build(member, cover, question,
                input.questionType(), input.experienceIds());
        List<AiMessage> messages = new ArrayList<>();
        // Older turns whose selected experience links differ must not enter this context.
        // The current context builder revalidates every selected link and its owner.
        List<CoverLetterEditTurn> relevant = previous.stream()
                .filter(prior -> prior.questionType.equals(input.questionType().name())
                        && prior.experienceIdsJson.equals(selectedJson)).toList();
        for (var prior : relevant.subList(Math.max(0, relevant.size() - 2), relevant.size())) {
            messages.add(new AiMessage(AiMessage.Role.USER, prior.instruction));
            messages.add(new AiMessage(AiMessage.Role.ASSISTANT, prior.draftAfter));
        }
        messages.add(new AiMessage(AiMessage.Role.USER, input.instruction()));
        AiRequest request = factory.prepareEditingChat(
                new EditingChatContext(context, input.currentDraft(),
                        input.revisionType(), messages, "v1"), requestId);
        AiResponse answer = gateway.chat(request);
        RevisionResult revision;
        try {
            revision = mapper.readValue(answer.content(), RevisionResult.class);
        } catch (RuntimeException exception) {
            throw new AiProviderException(AiProviderException.Code.INVALID_OUTPUT,
                    "AI editing chat response is invalid");
        }
        int count = revision.revisedText().codePointCount(0, revision.revisedText().length());
        List<String> warnings = new ArrayList<>(revision.warnings());
        if (count > entity.characterLimit) warnings.add("CHARACTER_LIMIT_EXCEEDED");
        long sequence = previous.isEmpty() ? 1 : previous.get(previous.size()-1).sequenceNo + 1;
        var stored = new CoverLetterEditTurn(cover, question, member, sequence,
                requestId, input.questionType().name(), selectedJson,
                input.currentDraft(), input.instruction(), input.revisionType().name(),
                revision.revisedText(), mapper.writeValueAsString(revision.changeReasons()),
                mapper.writeValueAsString(warnings), answer.provider(), answer.isMock(),
                request.promptVersion());
        turns.save(stored);
        return toTurn(stored);
    }

    private Turn toTurn(CoverLetterEditTurn stored) {
        List<UUID> selected = List.of(mapper.readValue(stored.experienceIdsJson, UUID[].class));
        List<String> reasons = Arrays.asList(mapper.readValue(stored.changeReasonsJson, String[].class));
        List<String> warnings = Arrays.asList(mapper.readValue(stored.warningsJson, String[].class));
        return new Turn(stored.sequenceNo, stored.requestId,
                CoverLetterQuestionType.valueOf(stored.questionType), selected,
                stored.draftBefore, stored.instruction,
                RevisionType.valueOf(stored.revisionType), stored.draftAfter,
                stored.draftAfter.codePointCount(0, stored.draftAfter.length()),
                reasons, warnings, stored.provider, stored.mock, stored.promptVersion, stored.createdAt);
    }

    private static BusinessException invalid() {
        return new BusinessException(ErrorCode.INVALID_REQUEST);
    }
}
