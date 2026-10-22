package com.hongik.graduate.BE.importdata;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import java.util.*;
@io.swagger.v3.oas.annotations.tags.Tag(name="가져오기",description="PDF 파일 업로드·저장 및 Notion 페이지 텍스트 수집")
@RestController @RequestMapping("/api/imports")
public class ImportController {
 private final ImportFileService files;
 private final NotionImportService notion;
 public ImportController(ImportFileService files,NotionImportService notion){this.files=files;this.notion=notion;}
 private UUID user(Jwt jwt){return UUID.fromString(jwt.getSubject());}
 @io.swagger.v3.oas.annotations.Operation(summary="PDF 업로드",description="10MB 이하의 PDF를 서버에 저장하고 가져오기 기록을 생성합니다.")
 @PostMapping(value="/pdf",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public ApiResponse<?> upload(@AuthenticationPrincipal Jwt jwt,@RequestPart("file") MultipartFile file){
 return ApiResponse.success(files.upload(user(jwt),file));}
 @io.swagger.v3.oas.annotations.Operation(summary="Notion 페이지 가져오기",description="서버에 설정된 Notion 통합 토큰으로 공유된 페이지의 텍스트를 수집·저장합니다.")
 @PostMapping("/notion") public ApiResponse<?> notion(@AuthenticationPrincipal Jwt jwt,@RequestBody NotionImportService.ImportRequest request){
 return ApiResponse.success(notion.collect(user(jwt),request));}
 @io.swagger.v3.oas.annotations.Operation(summary="가져온 자료 목록",description="내가 가져온 PDF 및 Notion 자료 목록을 조회합니다.")
 @GetMapping public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt){
 return ApiResponse.success(files.list(user(jwt)));}
 @io.swagger.v3.oas.annotations.Operation(summary="가져온 자료 조회",description="내가 가져온 자료와 Notion 텍스트를 조회합니다.")
 @GetMapping("/{id}") public ApiResponse<?> get(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 return ApiResponse.success(files.get(user(jwt),id));}
 @io.swagger.v3.oas.annotations.Operation(summary="PDF 다운로드",description="본인이 업로드한 PDF를 다운로드합니다.")
 @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id){
 ImportedSource item=files.get(user(jwt),id);
 return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=document.pdf")
 .contentType(MediaType.APPLICATION_PDF).body(files.download(user(jwt),id));}
}