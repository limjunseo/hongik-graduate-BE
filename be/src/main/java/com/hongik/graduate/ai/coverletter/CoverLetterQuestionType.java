package com.hongik.graduate.ai.coverletter;

public enum CoverLetterQuestionType {
    MOTIVATION("지원동기"),
    JOB_COMPETENCY("직무역량"),
    COLLABORATION("협업"),
    PROBLEM_SOLVING("문제해결"),
    CHALLENGE("도전"),
    STRENGTH_WEAKNESS("성격의 장단점"),
    GROWTH("성장과정"),
    FUTURE_PLAN("입사 후 포부");

    private final String label;
    CoverLetterQuestionType(String label) { this.label = label; }
    public String label() { return label; }
}
