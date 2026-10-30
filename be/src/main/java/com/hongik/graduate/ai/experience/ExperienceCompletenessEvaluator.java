package com.hongik.graduate.ai.experience;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Rule-based field presence only; NEVER infers STAR content from prose. */
@Component
public final class ExperienceCompletenessEvaluator {
    private static final Map<String, List<String>> REQUIRED = Map.of(
            "STAR", List.of("situation", "task", "action", "result"),
            "COLLABORATION", List.of("situation", "role", "action", "result"),
            "ACADEMIC_CAREER", List.of("situation", "task", "action", "result"),
            "CHALLENGE", List.of("situation", "task", "action", "result"),
            "GROWTH_VALUES", List.of("situation", "action", "learned"),
            "STRENGTH_WEAKNESS", List.of("situation", "action", "learned")
    );

    private static final Map<String, String> QUESTIONS = Map.of(
            "situation", "어떤 상황과 배경이었나요?",
            "task", "어떤 목표나 과제를 맡았나요?",
            "action", "본인이 직접 어떤 행동을 했나요?",
            "result", "그 결과는 무엇이었나요?",
            "role", "본인의 역할과 책임은 무엇이었나요?",
            "conflict", "갈등이나 의견 차이가 있었나요?",
            "learned", "경험을 통해 무엇을 배웠나요?"
    );

    public CompletenessResult evaluate(ExperienceSummary summary) {
        List<String> required = REQUIRED.get(summary.tagCode());
        if (required == null) {
            throw new IllegalArgumentException("unknown experience tag code");
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("situation", summary.situation());
        values.put("task", summary.task());
        values.put("action", summary.action());
        values.put("result", summary.result());
        values.put("role", summary.role());
        values.put("conflict", summary.conflict());
        values.put("learned", summary.learned());
        List<String> present = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String field : required) {
            String value = values.get(field);
            if (value == null || value.isBlank()) {
                missing.add(field);
            } else {
                present.add(field);
            }
        }
        String next = missing.isEmpty() ? null : missing.get(0);
        return new CompletenessResult(List.copyOf(required), present, missing,
                (int) Math.round(100.0 * present.size() / required.size()),
                next, next == null ? null : QUESTIONS.get(next));
    }

    public record CompletenessResult(List<String> requiredFields,
            List<String> presentFields, List<String> missingFields,
            int completionPercent, String nextRecommendedField, String nextQuestion) {
        public CompletenessResult {
            requiredFields = List.copyOf(requiredFields);
            presentFields = List.copyOf(presentFields);
            missingFields = List.copyOf(missingFields);
        }
    }
}
