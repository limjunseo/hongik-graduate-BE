package com.hongik.graduate.ai.job;

import static org.assertj.core.api.Assertions.*;
import java.util.UUID;
import com.hongik.graduate.BE.common.exception.BusinessException;
import com.hongik.graduate.BE.coverletter.domain.CoverLetter;
import com.hongik.graduate.BE.coverletter.domain.CoverLetterRepository;
import com.hongik.graduate.BE.coverletter.service.JobPostingService;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.ai.enabled=true")
class SourceBoundAnalysisAiIntegrationTest {
    @Autowired SourceBoundAnalysisAiService service;
    @Autowired MemberRepository members;
    @Autowired CoverLetterRepository covers;
    @Autowired JobPostingService postings;

    @Test
    void mockReportsNeverFabricateSourcesOrPersist() {
        Member owner = member();
        Member other = member();
        var cover = covers.saveAndFlush(new CoverLetter(
                owner.getId(), "Example", "Java Engineer", null));
        postings.save(owner.getId(), cover.id,
                new JobPostingService.Input(null, "Build Java backend services", null, null, null));

        assertThatThrownBy(() -> service.role(other.getId(), cover.id))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.company(other.getId(), cover.id,
                new SourceBoundAnalysisAiService.CompanyInput(
                        "https://example.com/about", "Our product is software.")))
                .isInstanceOf(BusinessException.class);

        var role = service.role(owner.getId(), cover.id);
        assertThat(role.isMock()).isTrue();
        assertThat(role.analysis().summary()).contains("MOCK ONLY");
        assertThat(role.sourceType()).isEqualTo("OWNED_JOB_POSTING");
        var company = service.company(owner.getId(), cover.id,
                new SourceBoundAnalysisAiService.CompanyInput(
                        "https://example.com/about", "Our product is software."));
        assertThat(company.isMock()).isTrue();
        assertThat(company.sourceType()).isEqualTo("USER_SUPPLIED_TEXT_NOT_FETCHED");
        assertThat(company.warnings()).contains("SOURCE_NOT_VERIFIED");
        assertThat(company.analysis().sourceUrl().toString())
                .isEqualTo("https://example.com/about");
    }

    @Test
    void sourceMustBeProvidedByUserAndHaveHttpUrl() {
        Member owner = member();
        var cover = covers.saveAndFlush(new CoverLetter(
                owner.getId(), "Example", "Designer", null));
        assertThatThrownBy(() -> service.company(owner.getId(), cover.id,
                new SourceBoundAnalysisAiService.CompanyInput(
                        "file:///etc/passwd", "data")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.company(owner.getId(), cover.id,
                new SourceBoundAnalysisAiService.CompanyInput(
                        "https://example.com", "")))
                .isInstanceOf(BusinessException.class);
    }

    private Member member() {
        String suffix = UUID.randomUUID().toString();
        return members.saveAndFlush(Member.google(
                "source-" + suffix + "@example.com", "source-" + suffix));
    }
}
