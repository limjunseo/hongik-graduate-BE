package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;

class ExperienceFoundationUnitTest {
    private final SkillTaxonomy taxonomy = new SkillTaxonomy(
            new ObjectMapper(), new DefaultResourceLoader());
    private final ExperienceCompletenessEvaluator evaluator =
            new ExperienceCompletenessEvaluator();
    private final ExperienceTagPolicyResolver policies = new ExperienceTagPolicyResolver(
            new ExperienceQuestionPolicyLoader(
                    new ObjectMapper(), new DefaultResourceLoader()));

    private ExperienceSummary summary(String tag, String situation, String task,
            String action, String result, String role, String learned) {
        return new ExperienceSummary(UUID.randomUUID(), "Sample", tag,
                situation, task, action, result, role, null, learned,
                List.of("backend"), List.of("Java"), List.of(), "v1", "USER");
    }

    @Test
    void allSixDbTagsMapToExistingPoliciesOrCanonicalStarQuestions() {
        for (String tag : List.of("STAR", "GROWTH_VALUES",
                "STRENGTH_WEAKNESS", "ACADEMIC_CAREER",
                "COLLABORATION", "CHALLENGE")) {
            var guide = policies.resolve(tag);
            assertThat(guide.questions()).isNotEmpty();
            assertThat(guide.tagCode()).isEqualTo(tag);
            assertThat(evaluator.evaluate(
                    summary(tag, null, null, null, null, null, null))
                    .completionPercent()).isZero();
        }
        assertThat(policies.resolve("STAR").questions()).hasSize(4);
        assertThatThrownBy(() -> policies.resolve("UNKNOWN"))
                .isInstanceOf(com.hongik.graduate.BE.common.exception.BusinessException.class);
    }

    @Test
    void completenessOnlyEvaluatesStructuredFieldsAndDoesNotInferAnswers() {
        var onlySituation = summary("STAR", "We collaborated", null, null, null,
                null, null);
        var result = evaluator.evaluate(onlySituation);
        assertThat(result.requiredFields()).containsExactly(
                "situation", "task", "action", "result");
        assertThat(result.missingFields()).containsExactly("task", "action", "result");
        assertThat(result.completionPercent()).isEqualTo(25);
        assertThat(result.nextRecommendedField()).isEqualTo("task");
        assertThat(result.nextQuestion()).isNotBlank();
        assertThat(evaluator.evaluate(summary("STAR", "S", "T", "A", "R",
                null, null)).completionPercent()).isEqualTo(100);
    }

    @Test
    void optionalConflictIsNotRequiredForCollaboration() {
        var result = evaluator.evaluate(summary("COLLABORATION",
                "situation", null, "action", "result", "my role", null));
        assertThat(result.completionPercent()).isEqualTo(100);
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    void taxonomyHasStableCodesAndDeduplicatesAliases() {
        assertThat(taxonomy.version()).isEqualTo("v1");
        assertThat(taxonomy.all()).hasSize(11);
        assertThat(taxonomy.normalize(" 팀워크 ")).isEqualTo("collaboration");
        assertThat(taxonomy.normalize("BACKEND")).isEqualTo("backend");
        assertThat(taxonomy.normalizeAll(List.of("협업", "collaboration", "팀워크")))
                .containsExactly("collaboration");
        assertThatThrownBy(() -> taxonomy.normalize("Spring"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> taxonomy.normalizeAll(List.of("unknown")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
