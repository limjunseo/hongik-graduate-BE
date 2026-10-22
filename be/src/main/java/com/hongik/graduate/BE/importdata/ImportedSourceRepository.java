package com.hongik.graduate.BE.importdata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ImportedSourceRepository extends JpaRepository<ImportedSource,UUID>{
 List<ImportedSource> findByMemberIdOrderByCreatedAtDesc(UUID memberId);
 Optional<ImportedSource> findByIdAndMemberId(UUID id,UUID memberId);
}