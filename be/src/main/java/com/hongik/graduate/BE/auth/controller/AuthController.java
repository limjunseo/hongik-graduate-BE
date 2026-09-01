package com.hongik.graduate.BE.auth.controller;

import com.hongik.graduate.BE.auth.dto.EmailCodeVerifyRequest;
import com.hongik.graduate.BE.auth.dto.EmailVerificationRequest;
import com.hongik.graduate.BE.auth.dto.LoginRequest;
import com.hongik.graduate.BE.auth.dto.MessageResponse;
import com.hongik.graduate.BE.auth.dto.RefreshTokenRequest;
import com.hongik.graduate.BE.auth.dto.SignupRequest;
import com.hongik.graduate.BE.auth.dto.TokenResponse;
import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import com.hongik.graduate.BE.common.api.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "이메일/비밀번호 기반 인증 API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/email/send")
    @Operation(summary = "이메일 인증코드 발송", description = "가입 전에 이메일 인증코드를 발송합니다. 인증코드는 10분간 유효합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "인증코드 발송 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "이메일 형식 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "재발송 제한", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<MessageResponse> sendEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.sendVerification(request.email());
        return ApiResponse.success(new MessageResponse("인증메일을 발송했습니다."));
    }

    @PostMapping("/email/verify")
    @Operation(summary = "이메일 인증코드 확인", description = "이메일로 받은 인증코드를 검증합니다. 인증 완료 상태는 회원가입 전까지 일정 시간만 유지됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "이메일 인증 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "인증코드 오류 또는 만료", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<MessageResponse> verifyEmail(@Valid @RequestBody EmailCodeVerifyRequest request) {
        authService.verifyEmail(request.email(), request.code());
        return ApiResponse.success(new MessageResponse("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "이메일 인증을 완료한 이메일과 비밀번호로 회원가입합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류 또는 이메일 미인증", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 가입된 이메일", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<MessageResponse>> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(new MessageResponse("회원가입이 완료되었습니다.")));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 검증하고 Access Token과 Refresh Token을 발급합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "유효한 Refresh Token을 검증하고 토큰을 갱신합니다. Refresh Token Rotation이 적용됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 무효", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화합니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh Token 만료 또는 무효", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ApiResponse<MessageResponse> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ApiResponse.success(new MessageResponse("로그아웃이 완료되었습니다."));
    }
}
