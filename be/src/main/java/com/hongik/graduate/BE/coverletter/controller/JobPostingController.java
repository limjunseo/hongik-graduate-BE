package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="채용공고",description="자기소개서별 채용공고 원문 및 조건 저장")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController @RequestMapping("/api/cover-letters/{coverId}/posting")
public class JobPostingController {
 private final JobPostingService service;
 public JobPostingController(JobPostingService service){this.service=service;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="채용공고 조회",description="해당 자기소개서에 저장된 공고 URL, JD 원문, 자격요건, 우대사항 및 전형절차를 조회합니다.")
 @GetMapping public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId){
  return ApiResponse.success(service.get(user(jwt),coverId));
 }
 @io.swagger.v3.oas.annotations.Operation(summary="채용공고 저장·수정",description="채용공고 내용을 저장하거나 덮어씁니다. AI 분석이나 URL 수집은 수행하지 않습니다.")
 @PutMapping public ApiResponse<?> save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId,
  @RequestBody JobPostingService.Input request){
  return ApiResponse.success(service.save(user(jwt),coverId,request));
 }
 @io.swagger.v3.oas.annotations.Operation(summary="채용공고 삭제",description="자기소개서는 유지하고 저장된 채용공고 정보만 삭제합니다.")
 @DeleteMapping public ApiResponse<?> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId){
  service.delete(user(jwt),coverId);return ApiResponse.success(Map.of("deleted",true));
 }
}