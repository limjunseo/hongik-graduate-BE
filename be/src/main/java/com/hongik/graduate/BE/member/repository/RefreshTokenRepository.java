package com.hongik.graduate.BE.member.repository;

import java.util.Optional;
import java.util.UUID;

import com.hongik.graduate.BE.member.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
}
