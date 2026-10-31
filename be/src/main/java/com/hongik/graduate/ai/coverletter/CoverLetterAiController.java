package com.hongik.graduate.ai.coverletter;

import java.util.UUID;

import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Drafts are never persisted until the caller explicitly PATCHes the cover letter answer. */
@RestController
@RequestMapping("/api/cover-letters/{coverId}/questions/{questionId}")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class CoverLetterAiController {
    private final CoverLetterAiService service;
    private final QuestionAnalysisAiService classifier;

    public CoverLetterAiController(CoverLetterAiService service,
            QuestionAnalysisAiService classifier) {
        this.service = service;
        this.classifier = classifier;
    }

    @PostMapping("/analyze")
    public ApiResponse<?> analyze(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId) {
        return ApiResponse.success(classifier.classify(
                UUID.fromString(jwt.getSubject()), coverId, questionId));
    }

    @PostMapping("/strategy")
    public ApiResponse<?> strategy(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId,
            @RequestBody CoverLetterAiService.DraftInput request) {
        return ApiResponse.success(service.strategy(
                UUID.fromString(jwt.getSubject()), coverId, questionId, request));
    }

    @PostMapping("/draft")
    public ApiResponse<?> draft(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId,
            @RequestBody CoverLetterAiService.DraftInput request) {
        return ApiResponse.success(service.draft(
                UUID.fromString(jwt.getSubject()), coverId, questionId, request));
    }

    @PostMapping("/revise")
    public ApiResponse<?> revise(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId, @PathVariable UUID questionId,
            @RequestBody CoverLetterAiService.RevisionInput request) {
        return ApiResponse.success(service.revise(
                UUID.fromString(jwt.getSubject()), coverId, questionId, request));
    }
}