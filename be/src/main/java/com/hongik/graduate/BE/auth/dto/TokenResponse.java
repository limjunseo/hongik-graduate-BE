package com.hongik.graduate.BE.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record TokenResponse(
        @Schema(description = "API 인증에 사용하는 Access Token")
        String accessToken,
        @Schema(description = "Access Token 갱신에 사용하는 Refresh Token")
        String refreshToken,
        @Schema(description = "토큰 인증 방식", example = "Bearer")
        String tokenType,
        @Schema(description = "Access Token 만료 시각")
        Instant accessTokenExpiresAt,
        @Schema(description = "Refresh Token 만료 시각")
        Instant refreshTokenExpiresAt
) {
}
