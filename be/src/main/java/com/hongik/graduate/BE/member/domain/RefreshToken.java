package com.hongik.graduate.BE.member.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "refresh_tokens", uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_hash", columnNames = "token_hash"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "jti", nullable = false, length = 36)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column
    private Instant revokedAt;

    protected RefreshToken() {
    }

    public RefreshToken(Member member, String tokenHash, String jti, Instant expiresAt) {
        this.member = member;
        this.tokenHash = tokenHash;
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public void revoke() { revokedAt = Instant.now(); }
    public Member getMember() { return member; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revokedAt != null; }
}
