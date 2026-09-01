package com.hongik.graduate.BE.member.repository;

import java.util.Optional;
import java.util.UUID;

import com.hongik.graduate.BE.member.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {
    Optional<EmailVerification> findByEmail(String email);
    void deleteByEmail(String email);
}
