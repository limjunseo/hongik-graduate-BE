package com.hongik.graduate.BE.coverletter.domain;
import org.springframework.data.jpa.repository.JpaRepository;import java.util.*;
public interface CoverLetterRepository extends JpaRepository<CoverLetter,UUID>{
 Optional<CoverLetter> findByIdAndMemberId(UUID id,UUID memberId);
 List<CoverLetter> findByMemberIdOrderByUpdatedAtDesc(UUID memberId);
}