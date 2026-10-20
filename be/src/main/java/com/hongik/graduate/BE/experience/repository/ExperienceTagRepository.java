package com.hongik.graduate.BE.experience.repository;
import com.hongik.graduate.BE.experience.domain.ExperienceTag;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ExperienceTagRepository extends JpaRepository<ExperienceTag,String>{
 List<ExperienceTag> findByActiveTrueOrderByDisplayOrderAsc();
}