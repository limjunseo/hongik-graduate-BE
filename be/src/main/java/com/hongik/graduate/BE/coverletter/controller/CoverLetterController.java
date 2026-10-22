package com.hongik.graduate.BE.coverletter.controller;
import com.hongik.graduate.BE.coverletter.service.CoverLetterService;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name = "자기소개서", description = "기업·직무별 자기소개서, 문항 및 작성 버전 관리")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController @RequestMapping("/api/cover-letters")
public class CoverLetterController {
 private final CoverLetterService service;
 private final com.hongik.graduate.BE.coverletter.service.CoverLetterVersionService versions;
 public CoverLetterController(CoverLetterService service,com.hongik.graduate.BE.coverletter.service.CoverLetterVersionService versions){this.service=service;this.versions=versions;}
 private UUID uid(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 목록 조회")
 @GetMapping public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt,
 @RequestParam(required=false) String keyword,@RequestParam(required=false) String status,
 @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){
 return ApiResponse.success(service.list(uid(jwt),keyword,status,page,size));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 생성")
 @PostMapping public ApiResponse<?> create(@AuthenticationPrincipal Jwt jwt,@RequestBody CoverLetterService.Create req){
 return ApiResponse.success(service.create(uid(jwt),req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 상세 조회")
 @GetMapping("/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(Map.of("coverLetter",service.get(uid(jwt),id),"questions",service.questions(uid(jwt),id)));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 수정")
 @PatchMapping("/{id}") public ApiResponse<?> patch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody CoverLetterService.Patch req){
 return ApiResponse.success(service.patch(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 삭제")
 @DeleteMapping("/{id}") public ApiResponse<?> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 service.delete(uid(jwt),id);return ApiResponse.success(Map.of("deleted",true));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 문항 추가")
 @PostMapping("/{id}/questions") public ApiResponse<?> add(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody CoverLetterService.QuestionCreate req){
 return ApiResponse.success(service.addQuestion(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 문항 및 답변 수정")
 @PatchMapping("/{id}/questions/{questionId}") public ApiResponse<?> patchQuestion(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@PathVariable UUID questionId,@RequestBody CoverLetterService.QuestionPatch req){
 return ApiResponse.success(service.patchQuestion(uid(jwt),id,questionId,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "자기소개서 문항 삭제")
 @DeleteMapping("/{id}/questions/{questionId}") public ApiResponse<?> deleteQuestion(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@PathVariable UUID questionId){
 service.deleteQuestion(uid(jwt),id,questionId);return ApiResponse.success(Map.of("deleted",true));}
 @io.swagger.v3.oas.annotations.Operation(summary = "문항 순서 변경")
 @PutMapping("/{id}/questions/order") public ApiResponse<?> reorder(@AuthenticationPrincipal Jwt jwt,
 @PathVariable UUID id,@RequestBody CoverLetterService.OrderRequest req){
 return ApiResponse.success(service.reorder(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "작성 버전 저장")
 @PostMapping("/{id}/versions") public ApiResponse<?> saveVersion(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@RequestBody com.hongik.graduate.BE.coverletter.service.CoverLetterVersionService.SaveRequest req){return ApiResponse.success(versions.save(uid(jwt),id,req));}
 @io.swagger.v3.oas.annotations.Operation(summary = "작성 버전 목록 조회")
 @GetMapping("/{id}/versions") public ApiResponse<?> listVersions(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){return ApiResponse.success(versions.list(uid(jwt),id));}
 @io.swagger.v3.oas.annotations.Operation(summary = "작성 버전 상세 조회")
 @GetMapping("/{id}/versions/{versionId}") public ApiResponse<?> getVersion(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID versionId){return ApiResponse.success(versions.get(uid(jwt),id,versionId));}
 @io.swagger.v3.oas.annotations.Operation(summary = "이전 버전 복원")
 @PostMapping("/{id}/versions/{versionId}/restore") public ApiResponse<?> restoreVersion(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@PathVariable UUID versionId){return ApiResponse.success(versions.restore(uid(jwt),id,versionId));}
}