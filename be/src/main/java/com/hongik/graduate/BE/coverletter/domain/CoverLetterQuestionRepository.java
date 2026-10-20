package com.hongik.graduate.BE.coverletter.domain;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface CoverLetterQuestionRepository extends JpaRepository<CoverLetterQuestion,UUID>{
 List<CoverLetterQuestion> findByCoverLetterIdOrderByDisplayOrderAsc(UUID parent);
 Optional<CoverLetterQuestion> findByIdAndCoverLetterId(UUID id,UUID parent);
 long countByCoverLetterId(UUID parent);
 void deleteByCoverLetterId(UUID parent);
}