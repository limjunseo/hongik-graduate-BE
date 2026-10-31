package com.hongik.graduate.ai.job;

import java.util.UUID;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cover-letters/{coverId}")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class SourceBoundAnalysisAiController {
    private final SourceBoundAnalysisAiService service;

    public SourceBoundAnalysisAiController(SourceBoundAnalysisAiService service) {
        this.service = service;
    }

    @PostMapping("/posting/role-analyze")
    public ApiResponse<?> role(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID coverId) {
        return ApiResponse.success(service.role(UUID.fromString(jwt.getSubject()), coverId));
    }

    @PostMapping("/company/analyze")
    public ApiResponse<?> company(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID coverId,
            @RequestBody SourceBoundAnalysisAiService.CompanyInput input) {
        return ApiResponse.success(service.company(UUID.fromString(jwt.getSubject()), coverId, input));
    }
}
