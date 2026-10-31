package com.hongik.graduate.ai.coverletter;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoverLetterEditTurnRepository extends JpaRepository<CoverLetterEditTurn, UUID> {
    List<CoverLetterEditTurn> findByQuestionIdOrderBySequenceNoAsc(UUID questionId);
    void deleteByQuestionId(UUID questionId);
    void deleteByCoverLetterId(UUID coverLetterId);
}
