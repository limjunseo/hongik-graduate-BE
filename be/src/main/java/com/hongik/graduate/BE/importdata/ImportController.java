package com.hongik.graduate.BE.importdata;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="가져오기",description="PDF 파일 업로드·저장 및 공개 URL 본문 수집")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@RestController @RequestMapping("/api/imports")
public class ImportController {
 private final ImportFileService files;
 private final WebImportService web;
 public ImportController(ImportFileService files,WebImportService web){this.files=files;this.web=web;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="PDF 업로드",description="10MB 이하의 PDF를 서버에 저장하고 가져오기 기록을 생성합니다.")
 @PostMapping(value="/pdf",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ApiResponse<?> upload(@AuthenticationPrincipal Jwt jwt,@RequestPart("file") MultipartFile file){
 return ApiResponse.success(files.upload(user(jwt),file));}
 @io.swagger.v3.oas.annotations.Operation(summary="공개 URL 가져오기",description="공개 웹페이지의 제목과 본문 텍스트를 수집합니다. 로그인이 필요한 링크는 지원하지 않습니다.")
 @PostMapping("/url") public ApiResponse<?> url(@AuthenticationPrincipal Jwt jwt,@RequestBody WebImportService.Request request){
 return ApiResponse.success(web.collect(user(jwt),request));}
 @io.swagger.v3.oas.annotations.Operation(summary="가져온 자료 목록",description="내가 가져온 PDF 및 URL 자료 목록을 조회합니다.")
 @GetMapping public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt){
 return ApiResponse.success(files.list(user(jwt)));}
 @io.swagger.v3.oas.annotations.Operation(summary="가져온 자료 조회",description="내가 가져온 자료와 수집된 본문 텍스트를 조회합니다.")
 @GetMapping("/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(files.get(user(jwt),id));}
 @io.swagger.v3.oas.annotations.Operation(summary="PDF 다운로드",description="본인이 업로드한 PDF를 다운로드합니다.")
 @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 ImportedSource item=files.get(user(jwt),id);
 return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=document.pdf")
 .contentType(MediaType.APPLICATION_PDF).body(files.download(user(jwt),id));}
}