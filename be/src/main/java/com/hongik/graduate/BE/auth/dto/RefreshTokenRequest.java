package com.hongik.graduate.BE.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "로그인 또는 갱신 시 발급된 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
        @NotBlank String refreshToken
) {
}
