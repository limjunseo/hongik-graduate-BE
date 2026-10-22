package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.QuestionExperienceService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@RestController @RequestMapping("/api/cover-letters/{coverId}/questions/{questionId}/experiences")
public class QuestionExperienceController {
 private final QuestionExperienceService service;
 public QuestionExperienceController(QuestionExperienceService s){service=s;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 public record ConnectRequest(UUID experienceId){}
 @GetMapping public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId,@PathVariable UUID questionId){
  return ApiResponse.success(service.list(user(jwt),coverId,questionId));
 }
 @PostMapping public ApiResponse<?> connect(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId,@PathVariable UUID questionId,
 @RequestBody ConnectRequest request){
  return ApiResponse.success(service.connect(user(jwt),coverId,questionId,request.experienceId()));
 }
 @DeleteMapping("/{experienceId}") public ApiResponse<?> disconnect(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID coverId,
 @PathVariable UUID questionId,@PathVariable UUID experienceId){
  service.disconnect(user(jwt),coverId,questionId,experienceId);
  return ApiResponse.success(Map.of("deleted",true));
 }
}