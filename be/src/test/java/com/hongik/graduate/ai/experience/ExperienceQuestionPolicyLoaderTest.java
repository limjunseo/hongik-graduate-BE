package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import tools.jackson.databind.ObjectMapper;

class ExperienceQuestionPolicyLoaderTest {

    private final ExperienceQuestionPolicyLoader loader =
            new ExperienceQuestionPolicyLoader(
                    new ObjectMapper(),
                    new DefaultResourceLoader()
            );

    @Test
    void everyExperienceTagHasAClasspathPolicy() {
        for (ExperienceTag tag : ExperienceTag.values()) {
            ExperienceQuestionPolicy policy =
                    loader.getExperienceQuestionPolicy(tag);

            assertThat(policy.tag()).isEqualTo(tag);
            assertThat(policy.label()).isNotBlank();
            assertThat(policy.version()).isNotBlank();
            assertThat(policy.questions()).isNotEmpty();
        }
    }

    @Test
    void collaborationPolicyContainsExpectedQuestions() {
        ExperienceQuestionPolicy policy =
                loader.getExperienceQuestionPolicy(ExperienceTag.COLLABORATION);

        assertThat(policy.label()).isEqualTo("대인관계 및 협업 경험");
        assertThat(policy.questions()).contains(
                "본인의 역할과 책임은 무엇이었나요?",
                "갈등의 원인은 무엇이었고 어떻게 해결했나요?"
        );
    }

    @Test
    void jobPolicyContainsExpectedQuestions() {
        ExperienceQuestionPolicy policy =
                loader.getExperienceQuestionPolicy(ExperienceTag.JOB);

        assertThat(policy.questions()).contains(
                "그중 본인이 직접 설계하거나 구현한 부분은 무엇인가요?",
                "결과를 수치나 객관적인 지표로 표현할 수 있나요?"
        );
    }

    @Test
    void everyPolicyHasNonBlankUniqueQuestions() {
        for (ExperienceTag tag : ExperienceTag.values()) {
            List<String> questions =
                    loader.getExperienceQuestionPolicy(tag).questions();

            assertThat(questions).allSatisfy(question ->
                    assertThat(question).isNotBlank()
            );
            assertThat(new HashSet<>(questions)).hasSameSizeAs(questions);
        }
    }

    @Test
    void questionsAreImmutable() {
        List<String> questions = loader
                .getExperienceQuestionPolicy(ExperienceTag.VALUES)
                .questions();

        assertThatThrownBy(() -> questions.add("새 질문"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNonObjectJson() {
        assertInvalid(
                ExperienceTag.JOB,
                "[\"not\", \"an\", \"object\"]",
                "top-level value must be an object"
        );
    }

    @Test
    void rejectsMalformedJson() {
        assertInvalid(
                ExperienceTag.JOB,
                "{\"tag\":",
                "could not read valid JSON"
        );
    }

    @Test
    void rejectsMissingRequiredFields() {
        assertInvalid(
                ExperienceTag.JOB,
                """
                {
                  "tag": "job",
                  "label": "학업 및 직무 관련 경험"
                }
                """,
                "missing required field(s): version, questions"
        );
    }

    @Test
    void rejectsTagMismatch() {
        assertInvalid(
                ExperienceTag.COLLABORATION,
                validJson("job", "\"질문\""),
                "tag mismatch"
        );
    }

    @Test
    void rejectsBlankLabel() {
        assertInvalid(
                ExperienceTag.JOB,
                """
                {
                  "tag": "job",
                  "label": " ",
                  "version": "1",
                  "questions": ["질문"]
                }
                """,
                "label must be a non-empty string"
        );
    }

    @Test
    void rejectsNonStringVersion() {
        assertInvalid(
                ExperienceTag.JOB,
                """
                {
                  "tag": "job",
                  "label": "학업 및 직무 관련 경험",
                  "version": 1,
                  "questions": ["질문"]
                }
                """,
                "version must be a non-empty string"
        );
    }

    @Test
    void rejectsQuestionsThatAreNotAnArray() {
        assertInvalid(
                ExperienceTag.JOB,
                """
                {
                  "tag": "job",
                  "label": "학업 및 직무 관련 경험",
                  "version": "1",
                  "questions": "질문"
                }
                """,
                "questions must be a list"
        );
    }

    @Test
    void rejectsEmptyQuestions() {
        assertInvalid(
                ExperienceTag.JOB,
                validJson("job", ""),
                "questions must not be empty"
        );
    }

    @Test
    void rejectsNonStringQuestion() {
        assertInvalid(
                ExperienceTag.JOB,
                validJson("job", "1"),
                "question at index 0 must be a string"
        );
    }

    @Test
    void rejectsBlankQuestion() {
        assertInvalid(
                ExperienceTag.JOB,
                validJson("job", "\" \""),
                "question at index 0 must not be empty"
        );
    }

    @Test
    void rejectsDuplicateQuestions() {
        assertInvalid(
                ExperienceTag.JOB,
                validJson("job", "\"같은 질문\", \"같은 질문\""),
                "questions must not contain duplicates"
        );
    }

    @Test
    void reportsMissingPolicyResource() {
        ResourceLoader missingResourceLoader = new ResourceLoader() {
            @Override
            public Resource getResource(String location) {
                return new ClassPathResource("missing-policy.json");
            }

            @Override
            public ClassLoader getClassLoader() {
                return getClass().getClassLoader();
            }
        };
        ExperienceQuestionPolicyLoader missingPolicyLoader =
                new ExperienceQuestionPolicyLoader(
                        new ObjectMapper(),
                        missingResourceLoader
                );

        assertThatThrownBy(() ->
                missingPolicyLoader.getExperienceQuestionPolicy(
                        ExperienceTag.JOB
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Experience policy for 'job' not found")
                .hasMessageContaining("missing-policy.json");
    }

    private void assertInvalid(
            ExperienceTag tag,
            String json,
            String expectedReason
    ) {
        assertThatThrownBy(() -> loader.loadPolicy(tag, resource(json)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(
                        "Invalid experience policy for '%s'".formatted(tag.value())
                )
                .hasMessageContaining("test-policy.json")
                .hasMessageContaining(expectedReason);
    }

    private Resource resource(String json) {
        return new ByteArrayResource(
                json.getBytes(StandardCharsets.UTF_8),
                "test-policy.json"
        );
    }

    private String validJson(String tag, String questions) {
        return """
                {
                  "tag": "%s",
                  "label": "학업 및 직무 관련 경험",
                  "version": "1",
                  "questions": [%s]
                }
                """.formatted(tag, questions);
    }
}
