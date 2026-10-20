package com.hongik.graduate.BE.member.dto;

import com.hongik.graduate.BE.member.domain.Member;
import java.util.UUID;

public record ProfileResponse(UUID memberId, String email, Integer age, String gender,
                              String school, String desiredJob, boolean profileCompleted,
                              String signupStatus, String onboardingStep) {
    public static ProfileResponse from(Member m) {
        return new ProfileResponse(m.getId(), m.getEmail(), m.getAge(), m.getGender(),
                m.getSchool(), m.getDesiredJob(), m.isProfileCompleted(),
                m.isProfileCompleted() ? "COMPLETED" : "IN_PROGRESS", m.onboardingStep());
    }
}
