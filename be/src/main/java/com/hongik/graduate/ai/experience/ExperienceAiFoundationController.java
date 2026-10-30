package com.hongik.graduate.ai.experience;

import java.util.UUID;

import com.hongik.graduate.BE.common.api.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Does not expose a model or auto-generate fields. */
@RestController
@RequestMapping("/api")
public final class ExperienceAiFoundationController {
    private final ExperienceSummaryService summaries;
    private final SkillTaxonomy taxonomy;
    private final ExperienceTagPolicyResolver policies;
    private final ExperienceQuickAnswerPreviewService previews;

    public ExperienceAiFoundationController(ExperienceSummaryService summaries,
            SkillTaxonomy taxonomy, ExperienceTagPolicyResolver policies,
            ExperienceQuickAnswerPreviewService previews) {
        this.summaries = summaries;
        this.taxonomy = taxonomy;
        this.policies = policies;
        this.previews = previews;
    }

    private UUID user(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    @GetMapping("/skills")
    public ApiResponse<?> skills() {
        return ApiResponse.success(java.util.Map.of(
                "version", taxonomy.version(), "skills", taxonomy.all()));
    }

    @GetMapping("/experience-tags/{tagCode}/ai-questions")
    public ApiResponse<?> guide(@PathVariable String tagCode) {
        return ApiResponse.success(policies.resolve(tagCode));
    }

    @GetMapping("/experiences/{id}/structured-summary")
    public ApiResponse<?> summary(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(summaries.get(user(jwt), id));
    }

    @GetMapping("/experiences/{id}/structured-summary/preview-from-answers")
    public ApiResponse<?> preview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(previews.preview(user(jwt), id));
    }

    @PutMapping("/experiences/{id}/structured-summary")
    public ApiResponse<?> save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestBody ExperienceSummaryService.SummaryInput input) {
        return ApiResponse.success(summaries.save(user(jwt), id, input));
    }

    @GetMapping("/experiences/{id}/completeness")
    public ApiResponse<?> completeness(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return ApiResponse.success(summaries.completeness(user(jwt), id));
    }
}
