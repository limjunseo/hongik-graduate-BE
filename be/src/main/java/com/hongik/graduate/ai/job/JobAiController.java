package com.hongik.graduate.ai.job;

import java.util.Map;
import java.util.UUID;
import com.hongik.graduate.BE.common.api.ApiResponse;
import com.hongik.graduate.ai.coverletter.CoverLetterQuestionType;
import com.hongik.graduate.ai.coverletter.QuestionSkillPolicy;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
public class JobAiController {
    private final JobAnalysisService analyses;
    private final ExperienceRecommendationService recommendations;
    private final QuestionSkillPolicy policy;

    public JobAiController(JobAnalysisService analyses,
            ExperienceRecommendationService recommendations, QuestionSkillPolicy policy) {
        this.analyses = analyses; this.recommendations = recommendations; this.policy = policy;
    }

    private UUID user(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }

    @PutMapping("/cover-letters/{coverId}/posting/analysis")
    public ApiResponse<?> save(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID coverId,
            @RequestBody JobAnalysisService.Input input) {
        return ApiResponse.success(analyses.save(user(jwt), coverId, input));
    }

    @GetMapping("/cover-letters/{coverId}/posting/analysis")
    public ApiResponse<?> analysis(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID coverId) {
        return ApiResponse.success(analyses.get(user(jwt), coverId));
    }

    @GetMapping("/cover-letters/{coverId}/experience-recommendations")
    public ApiResponse<?> recommendations(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID coverId) {
        return ApiResponse.success(recommendations.rank(user(jwt), coverId));
    }

    @GetMapping("/cover-letter-question-types")
    public ApiResponse<?> questionTypes() {
        return ApiResponse.success(Map.of("version", policy.version(), "types",
                java.util.Arrays.stream(CoverLetterQuestionType.values())
                        .map(t -> Map.of("code", t.name(), "label", t.label(),
                                "skills", policy.requiredSkills(t))).toList()));
    }
}
