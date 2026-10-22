package com.hongik.graduate.BE.coverletter.domain;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CoverLetterVersionRepository extends JpaRepository<CoverLetterVersion,UUID>{
 List<CoverLetterVersion> findByCoverLetterIdOrderByCreatedAtDesc(UUID cover);
 Optional<CoverLetterVersion> findByIdAndCoverLetterId(UUID id,UUID cover);
 void deleteByCoverLetterId(UUID cover);
}