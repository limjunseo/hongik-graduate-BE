package com.hongik.graduate;

import static org.assertj.core.api.Assertions.assertThat;

import com.hongik.graduate.ai.experience.ExperienceQuestionPolicyLoader;
import com.hongik.graduate.ai.experience.ExperienceTag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class GraduateApplicationTests {

    @Autowired
    private ExperienceQuestionPolicyLoader policyLoader;

    @Test
    void contextLoadsWithEveryExperiencePolicy() {
        assertThat(policyLoader).isNotNull();
        assertThat(ExperienceTag.values())
                .allSatisfy(tag -> assertThat(
                        policyLoader.getExperienceQuestionPolicy(tag).tag()
                ).isEqualTo(tag));
    }
}
