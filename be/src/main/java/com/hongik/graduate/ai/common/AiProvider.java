package com.hongik.graduate.ai.common;

/** Provider-neutral port. Implementations must not leak vendor SDK types. */
public interface AiProvider {
    /** One-shot generation; requests must not contain a conversation history. */
    AiResponse generate(AiRequest request);

    /** Ordered conversation, ending with a USER message. */
    AiResponse chat(AiRequest request);
}
