package com.hongik.graduate.BE.common.api;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String errorCode,
        String message,
        String path
) {
}
