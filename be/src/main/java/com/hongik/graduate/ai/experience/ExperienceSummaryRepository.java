package com.hongik.graduate.ai.experience;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceSummaryRepository
        extends JpaRepository<ExperienceSummaryEntity, UUID> {
}
