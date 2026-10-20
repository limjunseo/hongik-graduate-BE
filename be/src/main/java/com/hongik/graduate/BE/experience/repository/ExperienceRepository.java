package com.hongik.graduate.BE.experience.repository;
import com.hongik.graduate.BE.experience.domain.Experience;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface ExperienceRepository extends JpaRepository<Experience,UUID>{
 Optional<Experience> findByIdAndMemberId(UUID id,UUID memberId);
 List<Experience> findByMemberIdOrderByUpdatedAtDesc(UUID memberId);
}