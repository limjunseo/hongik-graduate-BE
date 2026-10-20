package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.CoverLetterService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@RestController @RequestMapping("/api/cover-letters")
public class CoverLetterController {
 private final CoverLetterService service;
 public CoverLetterController(CoverLetterService service){this.service=service;}
 private UUID uid(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @GetMapping public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt,
 @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,
 @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
 return ApiResponse.success(service.list(uid(jwt),keyword,status,page,size));}
 @PostMapping public ApiResponse<?> create(@AuthenticationPrincipal Jwt jwt,@RequestBody CoverLetterService.Create req){
 return ApiResponse.success(service.create(uid(jwt),req));}
 @GetMapping("/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(Map.of("coverLetter",service.get(uid(jwt),id),"questions",service.questions(uid(jwt),id)));}
 @PatchMapping("/{id}") public ApiResponse<?> patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody CoverLetterService.Patch req){
 return ApiResponse.success(service.patch(uid(jwt),id,req));}
 @DeleteMapping("/{id}") public ApiResponse<?> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 service.delete(uid(jwt),id);return ApiResponse.success(Map.of("deleted",true));}
 @PostMapping("/{id}/questions") public ApiResponse<?> add(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody CoverLetterService.QuestionCreate req){
 return ApiResponse.success(service.addQuestion(uid(jwt),id,req));}
 @PatchMapping("/{id}/questions/{questionId}") public ApiResponse<?> patchQuestion(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@PathVariable UUID questionId,@RequestBody CoverLetterService.QuestionPatch req){
 return ApiResponse.success(service.patchQuestion(uid(jwt),id,questionId,req));}
 @DeleteMapping("/{id}/questions/{questionId}") public ApiResponse<?> deleteQuestion(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@PathVariable UUID questionId){
 service.deleteQuestion(uid(jwt),id,questionId);return ApiResponse.success(Map.of("deleted",true));}
 @PutMapping("/{id}/questions/order") public ApiResponse<?> reorder(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@RequestBody CoverLetterService.OrderRequest req){
 return ApiResponse.success(service.reorder(uid(jwt),id,req));}
}