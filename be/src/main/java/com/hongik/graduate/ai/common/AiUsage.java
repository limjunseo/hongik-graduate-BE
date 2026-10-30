package com.hongik.graduate.ai.common;

/** Token counts are absent in the mock; never invent model usage. */
public record AiUsage(Integer inputTokens, Integer outputTokens) {
    public AiUsage {
        if ((inputTokens != null && inputTokens < 0)
                || (outputTokens != null && outputTokens < 0)) {
            throw new IllegalArgumentException("token counts must be non-negative");
        }
    }
}
