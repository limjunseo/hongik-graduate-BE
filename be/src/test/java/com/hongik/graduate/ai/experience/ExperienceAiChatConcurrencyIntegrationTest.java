package com.hongik.graduate.ai.experience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.ai.common.AiGateway;
import com.hongik.graduate.ai.common.AiRequest;
import com.hongik.graduate.ai.common.AiResponse;
import com.hongik.graduate.ai.common.AiUsage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class ExperienceAiChatConcurrencyIntegrationTest {
    @Autowired ExperienceRepository experiences;
    @Autowired ExperienceAiChatMessageRepository messages;
    @Autowired ExperienceAiChatService service;
    @MockitoBean AiGateway gateway;

    @Test
    void concurrentDistinctRequestsHaveSequentialPairs() throws Exception {
        UUID member = UUID.randomUUID();
        Experience saved = experiences.saveAndFlush(
                new Experience(member, "STAR", "Concurrent", "CONVERSATION", null, null));
        when(gateway.chat(any())).thenAnswer(invocation -> {
            AiRequest request = invocation.getArgument(0);
            Thread.sleep(120);
            return new AiResponse(AiResponse.Status.SUCCESS, "mock", true,
                    "response", null, request.correlationId(), new AiUsage(null, null));
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var start = new CountDownLatch(1);
            var first = executor.submit(() -> {
                start.await();
                return service.send(member, saved.id,
                        new ExperienceAiChatService.SendInput("first", "once-1"));
            });
            var second = executor.submit(() -> {
                start.await();
                return service.send(member, saved.id,
                        new ExperienceAiChatService.SendInput("second", "once-2"));
            });
            start.countDown();
            var left = first.get(15, TimeUnit.SECONDS);
            var right = second.get(15, TimeUnit.SECONDS);
            assertThat(List.of(left.user().sequenceNo(), right.user().sequenceNo()))
                    .containsExactlyInAnyOrder(1L, 3L);
            var persisted = messages.findByExperienceIdOrderBySequenceNoAsc(saved.id);
            assertThat(persisted).extracting(m -> m.sequenceNo)
                    .containsExactly(1L, 2L, 3L, 4L);
        } finally {
            executor.shutdownNow();
            messages.deleteAll(messages.findByExperienceIdOrderBySequenceNoAsc(saved.id));
            experiences.deleteById(saved.id);
        }
    }
}