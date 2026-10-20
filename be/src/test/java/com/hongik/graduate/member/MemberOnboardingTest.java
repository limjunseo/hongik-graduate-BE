package com.hongik.graduate.member;

import com.hongik.graduate.BE.member.domain.Member;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MemberOnboardingTest {
    @Test
    void tracksOnboardingStages() {
        Member member = new Member("test@example.com", "hashed");
        assertEquals("BASIC", member.onboardingStep());
        assertFalse(member.isProfileCompleted());
        member.updateBasic(24, "FEMALE", "홍익대학교");
        assertEquals("JOB", member.onboardingStep());
        assertFalse(member.isProfileCompleted());
        member.updateJob("BACKEND");
        assertEquals("COMPLETED", member.onboardingStep());
        assertTrue(member.isProfileCompleted());
    }
}
