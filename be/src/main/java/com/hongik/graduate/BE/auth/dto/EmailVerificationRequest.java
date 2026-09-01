package com.hongik.graduate.BE.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @Schema(description = "인증할 이메일 주소", example = "user@example.com")
        @NotBlank @Email String email
) {
}
