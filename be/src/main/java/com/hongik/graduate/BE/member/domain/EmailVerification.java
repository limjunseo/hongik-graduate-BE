package com.hongik.graduate.BE.member.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "email_verifications", uniqueConstraints = @UniqueConstraint(name = "uk_email_verifications_email", columnNames = "email"))
public class EmailVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private Instant sentAt;

    @Column
    private Instant verifiedAt;

    @Column(nullable = false)
    private int attempts;

    protected EmailVerification() {
    }

    public EmailVerification(String email, String codeHash, Instant sentAt, Instant expiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (attempts < 0) {
            attempts = 0;
        }
    }

    public void replaceCode(String codeHash, Instant sentAt, Instant expiresAt) {
        this.codeHash = codeHash;
        this.sentAt = sentAt;
        this.expiresAt = expiresAt;
        this.verifiedAt = null;
        this.attempts = 0;
    }

    public void increaseAttempts() { attempts++; }
    public void verify(Instant verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getEmail() { return email; }
    public String getCodeHash() { return codeHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getSentAt() { return sentAt; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public int getAttempts() { return attempts; }
}
