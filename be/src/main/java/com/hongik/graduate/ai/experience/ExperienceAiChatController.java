package com.hongik.graduate.ai.experience;

import java.util.UUID;

import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiences/{id}/ai-chat/messages")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ExperienceAiChatController {
    private final ExperienceAiChatService chats;

    public ExperienceAiChatController(ExperienceAiChatService chats) {
        this.chats = chats;
    }

    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "40") int size) {
        return ApiResponse.success(chats.list(
                UUID.fromString(jwt.getSubject()), id, page, size));
    }

    @PostMapping
    public ApiResponse<?> send(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody ExperienceAiChatService.SendInput input) {
        return ApiResponse.success(chats.send(
                UUID.fromString(jwt.getSubject()), id, input));
    }
}