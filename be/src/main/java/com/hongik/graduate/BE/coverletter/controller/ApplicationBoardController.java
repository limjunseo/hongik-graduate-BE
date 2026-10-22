package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.ApplicationBoardService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="지원 현황",description="칸반 단계별 지원 현황 조회 및 상태 변경")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController @RequestMapping("/api/application-board")
public class ApplicationBoardController {
 private final ApplicationBoardService service;
 public ApplicationBoardController(ApplicationBoardService service){this.service=service;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="칸반 지원 현황 조회",description="내 지원건을 단계별로 그룹화해 조회합니다.")
 @GetMapping public ApiResponse<?> board(@AuthenticationPrincipal Jwt jwt){
  return ApiResponse.success(service.board(user(jwt)));
 }
 @io.swagger.v3.oas.annotations.Operation(summary="지원 단계 변경",description="지원건 카드를 다른 칸반 단계로 이동합니다. PLANNED, PREPARING, SUBMITTED, CLOSED를 사용합니다.")
 @PatchMapping("/{coverLetterId}/status")
 public ApiResponse<?> move(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverLetterId,
 @RequestBody ApplicationBoardService.StatusRequest request){
  return ApiResponse.success(service.move(user(jwt),coverLetterId,request));
 }
}