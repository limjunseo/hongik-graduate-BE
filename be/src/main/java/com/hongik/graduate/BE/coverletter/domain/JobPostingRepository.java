package com.hongik.graduate.BE.coverletter.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface JobPostingRepository extends JpaRepository<JobPosting,UUID>{
 Optional<JobPosting> findByCoverLetterId(UUID coverLetterId);
 void deleteByCoverLetterId(UUID coverLetterId);
}