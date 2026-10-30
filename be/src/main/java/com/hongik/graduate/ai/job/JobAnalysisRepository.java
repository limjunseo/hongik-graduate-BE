package com.hongik.graduate.ai.job;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobAnalysisRepository extends JpaRepository<JobAnalysisEntity, UUID> { }
