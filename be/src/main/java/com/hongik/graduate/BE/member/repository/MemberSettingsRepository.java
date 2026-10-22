package com.hongik.graduate.BE.member.repository;
import com.hongik.graduate.BE.member.domain.MemberSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MemberSettingsRepository extends JpaRepository<MemberSettings,UUID>{
 Optional<MemberSettings> findByMemberId(UUID memberId);
}