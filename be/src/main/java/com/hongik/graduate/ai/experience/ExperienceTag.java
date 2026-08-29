package com.hongik.graduate.ai.experience;

public enum ExperienceTag {
    VALUES("values"),
    PERSONALITY("personality"),
    JOB("job"),
    COLLABORATION("collaboration"),
    CHALLENGE("challenge");

    private final String value;

    ExperienceTag(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
