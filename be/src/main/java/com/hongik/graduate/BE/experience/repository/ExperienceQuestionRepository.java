package com.hongik.graduate.BE.experience.repository;
import com.hongik.graduate.BE.experience.domain.ExperienceQuestion;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ExperienceQuestionRepository extends JpaRepository<ExperienceQuestion,UUID>{
 List<ExperienceQuestion> findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(String tagCode);
}