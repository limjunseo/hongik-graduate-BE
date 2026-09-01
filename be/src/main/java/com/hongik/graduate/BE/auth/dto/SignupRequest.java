package com.hongik.graduate.BE.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "이메일 인증을 완료한 이메일 주소", example = "user@example.com")
        @NotBlank @Email String email,
        @Schema(description = "8~72자의 비밀번호", example = "password123", format = "password")
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
