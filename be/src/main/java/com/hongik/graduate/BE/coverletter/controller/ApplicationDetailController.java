package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.ApplicationDetailService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="지원 상세",description="마감일, 면접일, 서류결과, 이력서 및 포트폴리오 준비 여부")
@RestController @RequestMapping("/api/cover-letters/{coverId}/application-detail")
public class ApplicationDetailController{
 private final ApplicationDetailService service;
 public ApplicationDetailController(ApplicationDetailService service){this.service=service;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="지원 상세 조회",description="지원건의 마감일, 면접일, 서류결과 및 서류 준비 여부를 조회합니다.")
 @GetMapping public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId){
  return ApiResponse.success(service.get(user(jwt),coverId));
 }
 @io.swagger.v3.oas.annotations.Operation(summary="지원 상세 저장·수정",description="지원 상세를 전체 저장합니다. 생략한 선택 항목은 초기화되며, 이력서와 포트폴리오 여부는 true/false입니다.")
 @PutMapping public ApiResponse<?> save(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId,
 @RequestBody ApplicationDetailService.Input input){
  return ApiResponse.success(service.save(user(jwt),coverId,input));
 }
}