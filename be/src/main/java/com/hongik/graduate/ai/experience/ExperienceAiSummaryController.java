package com.hongik.graduate.ai.experience;

import java.util.UUID;

import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/experiences/{id}")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class ExperienceAiSummaryController {
    private final ExperienceAiSummaryService service;

    public ExperienceAiSummaryController(ExperienceAiSummaryService service) {
        this.service = service;
    }

    @PostMapping("/ai-summary:generate")
    public ApiResponse<?> generate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(service.generate(UUID.fromString(jwt.getSubject()), id));
    }
}