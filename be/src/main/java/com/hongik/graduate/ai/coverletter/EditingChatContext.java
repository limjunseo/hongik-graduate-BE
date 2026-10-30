package com.hongik.graduate.ai.coverletter;

import java.util.List;
import com.hongik.graduate.ai.common.AiMessage;

/** Multi-turn input contract; no editing chat table or API in stage 4. */
public record EditingChatContext(CoverLetterContext context, String currentDraft,
        RevisionType goal, List<AiMessage> priorTurns, String sessionVersion) {
    public EditingChatContext {
        if (context == null || currentDraft == null
                || currentDraft.length() > 10000 || goal == null
                || !"v1".equals(sessionVersion) || priorTurns == null
                || priorTurns.size() > 60 || priorTurns.stream().anyMatch(
                    m -> m == null || m.role() == AiMessage.Role.SYSTEM)) {
            throw new IllegalArgumentException("invalid editing chat context");
        }
        priorTurns = List.copyOf(priorTurns);
    }
}
