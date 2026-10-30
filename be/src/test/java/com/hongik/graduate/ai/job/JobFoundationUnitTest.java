package com.hongik.graduate.ai.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import tools.jackson.databind.ObjectMapper;
import com.hongik.graduate.ai.coverletter.CoverLetterQuestionType;
import com.hongik.graduate.ai.coverletter.QuestionSkillPolicy;
import com.hongik.graduate.ai.experience.SkillTaxonomy;

class JobFoundationUnitTest {
    final ExperienceMatcher matcher = new ExperienceMatcher();
    final SkillTaxonomy skills = new SkillTaxonomy(new ObjectMapper(), new DefaultResourceLoader());
    final QuestionSkillPolicy policy = new QuestionSkillPolicy(
            new DefaultResourceLoader(), new ObjectMapper(), skills);

    @Test void requiredSkillCoverageUsesRoundedPercentageAndMissingSkills() {
        UUID id = UUID.randomUUID();
        var match = matcher.match(id, List.of("backend", "problem_solving", "collaboration"),
                List.of("backend", "problem_solving", "communication"));
        assertThat(match.score()).isEqualTo(67);
        assertThat(match.status()).isEqualTo(ExperienceMatchResult.Status.SCORED);
        assertThat(match.matchedSkills()).containsExactly("backend", "problem_solving");
        assertThat(match.missingSkills()).containsExactly("communication");
        assertThat(match.scoringVersion()).isEqualTo("v1");
        assertThat(matcher.match(id, List.of(), List.of("backend")).score()).isZero();
        assertThat(matcher.match(id, List.of(), List.of()).score()).isNull();
        assertThat(matcher.match(id, List.of(), List.of()).status())
                .isEqualTo(ExperienceMatchResult.Status.NOT_SCORABLE);
        assertThat(matcher.match(id, List.of("backend"),
                List.of("backend", "backend")).score()).isEqualTo(100);
    }

    @Test void allEightQuestionTypesHaveCanonicalAndStableMappings() {
        assertThat(CoverLetterQuestionType.values()).hasSize(8);
        assertThat(policy.version()).isEqualTo("v1");
        for (CoverLetterQuestionType type : CoverLetterQuestionType.values()) {
            assertThat(policy.requiredSkills(type)).isNotEmpty()
                    .allSatisfy(skill -> assertThat(skills.normalize(skill)).isEqualTo(skill));
        }
        assertThat(policy.requiredSkills(CoverLetterQuestionType.COLLABORATION))
                .contains("communication", "collaboration", "conflict_resolution");
        assertThatThrownBy(() -> policy.requiredSkills(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CoverLetterQuestionType.valueOf("WHATEVER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void analysisContractIsVersionedAndEvidenceMustBeWellFormed() {
        UUID id = UUID.randomUUID();
        var analysis = new JobAnalysis(id, "example", "backend",
                List.of("backend"), List.of(), List.of("build APIs"),
                List.of("collaboration"), List.of("Java"), List.of(),
                List.of(new JobAnalysis.Evidence(0, 7, "backend")),
                JobAnalysis.Status.DRAFT, "v1");
        assertThat(analysis.analysisStatus()).isEqualTo(JobAnalysis.Status.DRAFT);
        assertThatThrownBy(() -> new JobAnalysis(id, null, null, List.of(),
                List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(), JobAnalysis.Status.DRAFT, "v2"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JobAnalysis.Evidence(-1, 2, "bad"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
