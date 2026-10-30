package com.hongik.graduate.ai.experience;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.common.exception.ErrorCode;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.domain.ExperienceQuestion;
import com.hongik.graduate.BE.experience.repository.ExperienceAnswerRepository;
import com.hongik.graduate.BE.experience.repository.ExperienceQuestionRepository;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Literal projection of known quick-form question codes; never invents facts. */
@Service
public class ExperienceQuickAnswerPreviewService {
    private final ExperienceRepository experiences;
    private final ExperienceQuestionRepository questions;
    private final ExperienceAnswerRepository answers;

    public ExperienceQuickAnswerPreviewService(ExperienceRepository experiences,
            ExperienceQuestionRepository questions, ExperienceAnswerRepository answers) {
        this.experiences = experiences;
        this.questions = questions;
        this.answers = answers;
    }

    @Transactional(readOnly = true)
    public ExperienceSummary preview(UUID memberId, UUID experienceId) {
        Experience experience = experiences.findByIdAndMemberId(experienceId, memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));
        Map<UUID, String> codes = new HashMap<>();
        for (ExperienceQuestion question :
                questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(experience.tagCode)) {
            codes.put(question.id, question.code);
        }
        Map<String, String> answersByCode = new HashMap<>();
        for (var answer : answers.findByExperienceIdOrderByDisplayOrderAsc(experienceId)) {
            String code = codes.get(answer.questionId);
            if (code != null && answer.content != null && !answer.content.isBlank()) {
                answersByCode.putIfAbsent(code, answer.content);
            }
        }
        String s = null, t = null, a = null, r = null, role = null;
        switch (experience.tagCode) {
            case "STAR" -> {
                s = answersByCode.get("SITUATION");
                t = answersByCode.get("TASK");
                a = answersByCode.get("ACTION");
                r = answersByCode.get("RESULT");
            }
            case "COLLABORATION" -> {
                s = answersByCode.get("SITUATION");
                role = answersByCode.get("ROLE");
                a = answersByCode.get("APPROACH");
                r = answersByCode.get("RESULT_LEARNING");
            }
            case "ACADEMIC_CAREER" -> {
                s = answersByCode.get("BACKGROUND");
                t = answersByCode.get("ROLE_GOAL");
                a = answersByCode.get("PROCESS");
                r = answersByCode.get("ACHIEVEMENT");
            }
            case "CHALLENGE" -> {
                s = answersByCode.get("SITUATION");
                t = answersByCode.get("PROBLEM_GOAL");
                a = answersByCode.get("PROCESS");
                r = answersByCode.get("RESULT_GROWTH");
            }
            case "GROWTH_VALUES" -> {
                s = answersByCode.get("BACKGROUND");
                a = answersByCode.get("PRACTICE");
            }
            case "STRENGTH_WEAKNESS" -> {
                a = answersByCode.get("IMPROVEMENT");
                r = answersByCode.get("CHANGE");
            }
            default -> throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        // Ambiguous/mixed answers are not split into fabricated results/lessons.
        return new ExperienceSummary(experience.id, experience.title, experience.tagCode,
                s, t, a, r, role, null, null, List.of(), List.of(), List.of(),
                ExperienceSummary.VERSION, "USER");
    }
}
