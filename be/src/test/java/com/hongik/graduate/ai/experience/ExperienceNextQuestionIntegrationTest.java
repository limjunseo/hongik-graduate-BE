package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = "app.ai.enabled=true")
class ExperienceNextQuestionIntegrationTest {
    @Autowired MemberRepository members;
    @Autowired ExperienceRepository experiences;
    @Autowired ExperienceSummaryRepository snapshots;
    @Autowired ExperienceNextQuestionService next;
    @Autowired ObjectMapper mapper;

    @Test
    void nextQuestionUsesMissingFieldsAndDoesNotPersistSummary() {
        Member owner = member();
        Member other = member();
        Experience experience = experiences.saveAndFlush(
                new Experience(owner.getId(), "STAR", "New project", "QUICK", null, null));
        assertThatThrownBy(() -> next.next(other.getId(), experience.id))
                .isInstanceOf(BusinessException.class);
        var result = next.next(owner.getId(), experience.id);
        assertThat(result.completed()).isFalse();
        assertThat(result.isMock()).isTrue();
        assertThat(result.targetField()).isEqualTo("situation");
        assertThat(result.missingFields()).contains("situation", "task", "action", "result");
        assertThat(snapshots.findById(experience.id)).isEmpty();

        var summary = new ExperienceSummary(experience.id, "New project", "STAR",
                "S", "T", "A", "R", null, null, null,
                List.of(), List.of(), List.of(), "v1", "USER");
        snapshots.saveAndFlush(new ExperienceSummaryEntity(experience.id,
                mapper.writeValueAsString(summary), "v1", "USER"));
        var complete = next.next(owner.getId(), experience.id);
        assertThat(complete.completed()).isTrue();
        assertThat(complete.question()).isNull();
        assertThat(complete.provider()).isEqualTo("static");
        assertThat(complete.completionPercent()).isEqualTo(100);
    }

    private Member member() {
        String suffix = UUID.randomUUID().toString();
        return members.saveAndFlush(Member.google(
                "next-" + suffix + "@example.com", "next-" + suffix));
    }
}
