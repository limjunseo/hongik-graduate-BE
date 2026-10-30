package com.hongik.graduate.ai.experience;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceAiChatMessageRepository
        extends JpaRepository<ExperienceAiChatMessage, UUID> {
    List<ExperienceAiChatMessage> findByExperienceIdOrderBySequenceNoAsc(UUID experienceId);
    void deleteByExperienceId(UUID experienceId);
}