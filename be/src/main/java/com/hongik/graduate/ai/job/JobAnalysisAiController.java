package com.hongik.graduate.ai.job;

import java.util.UUID;

import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cover-letters/{coverId}/posting")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class JobAnalysisAiController {
    private final JobAnalysisAiService service;

    public JobAnalysisAiController(JobAnalysisAiService service) {
        this.service = service;
    }

    @PostMapping("/analyze")
    public ApiResponse<?> analyze(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId) {
        return ApiResponse.success(service.analyze(UUID.fromString(jwt.getSubject()), coverId));
    }
}