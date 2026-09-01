package com.hongik.graduate.BE.member.repository;

import java.util.Optional;
import java.util.UUID;

import com.hongik.graduate.BE.member.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    boolean existsByEmail(String email);
    Optional<Member> findByEmail(String email);
}
