package com.hongik.graduate.BE.member.controller;

import com.hongik.graduate.BE.common.api.ApiResponse;
import com.hongik.graduate.BE.member.dto.ProfileResponse;
import com.hongik.graduate.BE.member.service.ProfileService;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@io.swagger.v3.oas.annotations.tags.Tag(name = "회원 프로필", description = "나이·성별·학교·희망 직군 조회 및 수정")
@RestController
@RequestMapping("/api/members/me/profile")
public class ProfileController {
    private final ProfileService profiles;
    public ProfileController(ProfileService profiles) { this.profiles = profiles; }
    public record BasicRequest(Integer age, String gender, String school) {}
    public record JobRequest(String desiredJob) {}
    @io.swagger.v3.oas.annotations.Operation(summary = "프로필 조회", description = "내 프로필 정보를 조회합니다.")
 @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(profiles.get(UUID.fromString(jwt.getSubject())));
    }
    @io.swagger.v3.oas.annotations.Operation(summary = "기본 프로필 수정", description = "나이, 성별, 학교를 저장하거나 수정합니다.")
 @PutMapping("/basic")
    public ApiResponse<ProfileResponse> basic(@AuthenticationPrincipal Jwt jwt,
                                             @RequestBody BasicRequest request) {
        return ApiResponse.success(profiles.basic(UUID.fromString(jwt.getSubject()),
                request.age(), request.gender(), request.school()));
    }
    @io.swagger.v3.oas.annotations.Operation(summary = "희망 직군 수정", description = "희망 직군을 저장하거나 수정합니다.")
 @PutMapping("/job")
    public ApiResponse<ProfileResponse> job(@AuthenticationPrincipal Jwt jwt,
                                           @RequestBody JobRequest request) {
        return ApiResponse.success(profiles.job(UUID.fromString(jwt.getSubject()),request.desiredJob()));
    }
}
