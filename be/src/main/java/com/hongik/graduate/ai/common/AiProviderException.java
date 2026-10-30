package com.hongik.graduate.ai.common;

/** Safe, non-sensitive errors for internal AI pipeline consumers. */
public final class AiProviderException extends RuntimeException {
    public enum Code {
        DISABLED, INVALID_REQUEST, TEMPLATE_NOT_FOUND,
        INVALID_OUTPUT, PROVIDER_FAILURE, RATE_LIMITED, TIMEOUT
    }

    private final Code code;

    public AiProviderException(Code code, String safeMessage) {
        super(safeMessage);
        this.code = java.util.Objects.requireNonNull(code);
    }

    public Code code() {
        return code;
    }
}
