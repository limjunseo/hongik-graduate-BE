package com.hongik.graduate.BE.experience.repository;
import com.hongik.graduate.BE.experience.domain.ExperienceAnswer;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ExperienceAnswerRepository extends JpaRepository<ExperienceAnswer,UUID>{
 List<ExperienceAnswer> findByExperienceIdOrderByDisplayOrderAsc(UUID experienceId);
 void deleteByExperienceId(UUID experienceId);
}