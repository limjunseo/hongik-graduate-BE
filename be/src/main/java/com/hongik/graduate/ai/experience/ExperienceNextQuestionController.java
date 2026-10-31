package com.hongik.graduate.ai.experience;

import java.util.UUID;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiences/{id}/ai-chat")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ExperienceNextQuestionController {
    private final ExperienceNextQuestionService service;

    public ExperienceNextQuestionController(ExperienceNextQuestionService service) {
        this.service = service;
    }

    @PostMapping("/next-question")
    public ApiResponse<?> next(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(service.next(UUID.fromString(jwt.getSubject()), id));
    }
}
