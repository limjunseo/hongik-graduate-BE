package com.hongik.graduate.BE.coverletter.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ApplicationDetailRepository extends JpaRepository<ApplicationDetail,UUID>{
 Optional<ApplicationDetail> findByCoverLetterId(UUID coverLetterId);
 void deleteByCoverLetterId(UUID coverLetterId);
}