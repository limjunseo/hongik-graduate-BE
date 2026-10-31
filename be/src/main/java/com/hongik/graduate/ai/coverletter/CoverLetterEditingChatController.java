package com.hongik.graduate.ai.coverletter;

import java.util.UUID;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cover-letters/{coverId}/questions/{questionId}/ai-chat")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class CoverLetterEditingChatController {
    private final CoverLetterEditingChatService service;

    public CoverLetterEditingChatController(CoverLetterEditingChatService service) {
        this.service = service;
    }

    @GetMapping("/turns")
    public ApiResponse<?> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.history(
                UUID.fromString(jwt.getSubject()), coverId, questionId, page, size));
    }

    @PostMapping("/turns")
    public ApiResponse<?> send(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId,
            @RequestBody CoverLetterEditingChatService.Input input) {
        return ApiResponse.success(service.send(
                UUID.fromString(jwt.getSubject()), coverId, questionId, input));
    }
}
