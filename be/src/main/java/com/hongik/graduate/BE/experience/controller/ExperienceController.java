package com.hongik.graduate.BE.experience.controller;
import com.hongik.graduate.BE.experience.service.ExperienceService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;import java.util.*;
@RestController @RequestMapping("/api")
public class ExperienceController {
 private final ExperienceService service;
 public ExperienceController(ExperienceService service){this.service=service;}
 private UUID uid(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @GetMapping("/experience-tags") public ApiResponse<?> tags(){return ApiResponse.success(service.tags());}
 @GetMapping("/experience-tags/{code}/questions") public ApiResponse<?> questions(@PathVariable String code){return ApiResponse.success(service.questions(code));}
 @PostMapping("/experiences") public ApiResponse<?> create(@AuthenticationPrincipal Jwt jwt,@RequestBody ExperienceService.Create req){return ApiResponse.success(service.create(uid(jwt),req));}
 @GetMapping("/experiences") public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String keyword,
 @RequestParam(required=false) String tagCode,@RequestParam(required=false) String status,
 @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
 return ApiResponse.success(service.list(uid(jwt),keyword,tagCode,status,page,size));}
 @GetMapping("/experiences/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(Map.of("experience",service.get(uid(jwt),id),"answers",service.answers(uid(jwt),id),"progress",service.progress(uid(jwt),id)));}
 @PatchMapping("/experiences/{id}") public ApiResponse<?> patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.Patch req){
 return ApiResponse.success(service.patch(uid(jwt),id,req));}
 @PutMapping("/experiences/{id}/answers") public ApiResponse<?> answers(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.AnswersInput req){
 return ApiResponse.success(service.putAnswers(uid(jwt),id,req));}
 public record SingleAnswerRequest(String content){}
 @PutMapping("/experiences/{id}/answers/{questionId}") public ApiResponse<?> singleAnswer(
 @AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID questionId,@RequestBody SingleAnswerRequest request){
 return ApiResponse.success(service.putSingleAnswer(uid(jwt),id,questionId,request.content()));}
 @PatchMapping("/experiences/{id}/status") public ApiResponse<?> status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.StatusInput req){
 return ApiResponse.success(service.status(uid(jwt),id,req));}
 @DeleteMapping("/experiences/{id}") public ApiResponse<?> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 service.delete(uid(jwt),id);return ApiResponse.success(Map.of("deleted",true));}
}