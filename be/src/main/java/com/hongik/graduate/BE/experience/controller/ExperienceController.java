package com.hongik.graduate.BE.experience.controller;
import com.hongik.graduate.BE.experience.service.ExperienceService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name = "경험 정리", description = "경험 카드와 빠르게 작성 답변 관리")
@RestController @RequestMapping("/api")
public class ExperienceController {
 private final ExperienceService service;
 public ExperienceController(ExperienceService service){this.service=service;}
 private UUID uid(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 태그 목록 조회", description = "경험 정리에 사용하는 태그 목록을 조회합니다.")
 @GetMapping("/experience-tags") public ApiResponse<?> tags(){return ApiResponse.success(service.tags());}
 @io.swagger.v3.oas.annotations.Operation(summary = "태그별 질문 조회", description = "선택한 경험 태그의 작성 질문을 조회합니다.")
 @GetMapping("/experience-tags/{code}/questions") public ApiResponse<?> questions(@PathVariable String code){return ApiResponse.success(service.questions(code));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 생성", description = "경험 카드를 새로 생성합니다.")
 @PostMapping("/experiences") public ApiResponse<?> create(@AuthenticationPrincipal Jwt jwt,@RequestBody ExperienceService.Create req){return ApiResponse.success(service.create(uid(jwt),req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "내 경험 목록 조회")
 @GetMapping("/experiences") public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) String keyword,
 @RequestParam(required=false) String tagCode,@RequestParam(required=false) String status,
 @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
 return ApiResponse.success(service.list(uid(jwt),keyword,tagCode,status,page,size));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 상세 조회")
 @GetMapping("/experiences/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(Map.of("experience",service.get(uid(jwt),id),"answers",service.answers(uid(jwt),id),"progress",service.progress(uid(jwt),id)));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 수정")
 @PatchMapping("/experiences/{id}") public ApiResponse<?> patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.Patch req){
 return ApiResponse.success(service.patch(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 답변 일괄 저장")
 @PutMapping("/experiences/{id}/answers") public ApiResponse<?> answers(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.AnswersInput req){
 return ApiResponse.success(service.putAnswers(uid(jwt),id,req));}
 public record SingleAnswerRequest(String content){}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 질문별 답변 저장")
 @PutMapping("/experiences/{id}/answers/{questionId}") public ApiResponse<?> singleAnswer(
 @AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID questionId,@RequestBody SingleAnswerRequest request){
 return ApiResponse.success(service.putSingleAnswer(uid(jwt),id,questionId,request.content()));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 상태 변경")
 @PatchMapping("/experiences/{id}/status") public ApiResponse<?> status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody ExperienceService.StatusInput req){
 return ApiResponse.success(service.status(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "경험 삭제")
 @DeleteMapping("/experiences/{id}") public ApiResponse<?> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 service.delete(uid(jwt),id);return ApiResponse.success(Map.of("deleted",true));}
}