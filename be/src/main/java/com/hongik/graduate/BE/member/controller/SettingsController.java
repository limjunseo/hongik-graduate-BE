package com.hongik.graduate.BE.member.controller;
import com.hongik.graduate.BE.member.service.SettingsService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="사용자 설정",description="테마 및 알림 수신 환경설정")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController @RequestMapping("/api/members/me/settings")
public class SettingsController{
 private final SettingsService service;
 public SettingsController(SettingsService service){this.service=service;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="사용자 설정 조회",description="미설정 시 기본값을 반환합니다.")
 @GetMapping public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt){return ApiResponse.success(service.get(user(jwt)));}
 @io.swagger.v3.oas.annotations.Operation(summary="사용자 설정 수정",description="테마 및 알림 수신 설정을 부분 수정합니다.")
 @PatchMapping public ApiResponse<?> update(@AuthenticationPrincipal Jwt jwt,@RequestBody SettingsService.Request request){return ApiResponse.success(service.update(user(jwt),request));}
}
