package com.hongik.graduate.BE.coverletter.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface QuestionExperienceLinkRepository extends JpaRepository<QuestionExperienceLink,UUID>{
 List<QuestionExperienceLink> findByQuestionIdOrderByDisplayOrderAsc(UUID questionId);
 boolean existsByQuestionIdAndExperienceId(UUID questionId,UUID experienceId);
 void deleteByQuestionIdAndExperienceId(UUID questionId,UUID experienceId);
 void deleteByQuestionId(UUID questionId);
 void deleteByExperienceId(UUID experienceId);
 long countByQuestionId(UUID questionId);
 List<QuestionExperienceLink> findByExperienceId(UUID experienceId);
}