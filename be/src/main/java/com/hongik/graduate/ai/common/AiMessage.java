package com.hongik.graduate.ai.common;

import java.util.Objects;

public record AiMessage(Role role, String content) {
    public enum Role { SYSTEM, USER, ASSISTANT }

    public AiMessage {
        Objects.requireNonNull(role, "message role is required");
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("message content must not be blank");
        }
    }
}
