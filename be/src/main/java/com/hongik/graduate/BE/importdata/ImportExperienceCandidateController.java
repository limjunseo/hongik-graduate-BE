package com.hongik.graduate.BE.importdata;

import java.util.UUID;
import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/imports")
public final class ImportExperienceCandidateController {
    private final ImportExperienceCandidateService service;

    public ImportExperienceCandidateController(ImportExperienceCandidateService service) {
        this.service = service;
    }

    @GetMapping("/{id}/experience-candidates")
    public ApiResponse<?> candidates(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(service.extract(UUID.fromString(jwt.getSubject()), id));
    }
}
