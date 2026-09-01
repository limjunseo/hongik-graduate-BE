package com.hongik.graduate.BE.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeVerifyRequest(
        @Schema(description = "인증코드를 받은 이메일 주소", example = "user@example.com")
        @NotBlank @Email String email,
        @Schema(description = "이메일로 받은 5자리 인증코드", example = "12345")
        @NotBlank String code
) {
}
