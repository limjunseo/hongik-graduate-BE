package com.hongik.graduate.ai.experience;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Maps DB tag codes to existing AI policies; STAR uses canonical DB questions. */
@Component
public final class ExperienceTagPolicyResolver {
    private static final Map<String, ExperienceTag> MAPPING = Map.of(
            "GROWTH_VALUES", ExperienceTag.VALUES,
            "STRENGTH_WEAKNESS", ExperienceTag.PERSONALITY,
            "ACADEMIC_CAREER", ExperienceTag.JOB,
            "COLLABORATION", ExperienceTag.COLLABORATION,
            "CHALLENGE", ExperienceTag.CHALLENGE
    );

    private static final List<String> STAR_QUESTIONS = List.of(
            "어떤 상황과 배경이었나요?",
            "어떤 목표와 역할을 맡았나요?",
            "본인이 구체적으로 어떤 행동을 했나요?",
            "결과와 객관적인 성과는 무엇인가요?"
    );
    private final ExperienceQuestionPolicyLoader loader;

    public ExperienceTagPolicyResolver(ExperienceQuestionPolicyLoader loader) {
        this.loader = loader;
    }

    public TagGuide resolve(String dbTagCode) {
        if ("STAR".equals(dbTagCode)) {
            return new TagGuide("STAR", "STAR", "v1", STAR_QUESTIONS);
        }
        ExperienceTag policyTag = MAPPING.get(dbTagCode);
        if (policyTag == null) {
            throw new com.hongik.graduate.BE.common.exception.BusinessException(
                    com.hongik.graduate.BE.common.exception.ErrorCode.INVALID_REQUEST);
        }
        ExperienceQuestionPolicy policy = loader.getExperienceQuestionPolicy(policyTag);
        return new TagGuide(dbTagCode, policy.label(), policy.version(), policy.questions());
    }

    public record TagGuide(String tagCode, String label, String version, List<String> questions) {
        public TagGuide {
            questions = List.copyOf(questions);
        }
    }
}
