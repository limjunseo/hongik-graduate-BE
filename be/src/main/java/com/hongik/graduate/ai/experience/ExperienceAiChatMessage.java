package com.hongik.graduate.ai.experience;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

/** Single user or assistant turn; scoped by an owned experience. */
@Entity
@Table(name = "experience_ai_chat_messages",
        indexes = @Index(name = "idx_exp_chat_sequence", columnList = "experience_id,sequence_no"),
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_exp_chat_sequence",
                        columnNames = {"experience_id", "sequence_no"}),
                @UniqueConstraint(name = "uq_exp_chat_request_role",
                        columnNames = {"experience_id", "request_id", "role"})
        })
public class ExperienceAiChatMessage {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    public UUID id;
    @Column(name = "experience_id", nullable = false)
    public UUID experienceId;
    @Column(name = "member_id", nullable = false)
    public UUID memberId;
    @Column(name = "sequence_no", nullable = false)
    public long sequenceNo;
    @Column(name = "request_id", nullable = false, length = 128)
    public String requestId;
    @Column(nullable = false, length = 12)
    public String role;
    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;
    @Column(nullable = false, length = 20)
    public String provider;
    @Column(nullable = false)
    public boolean mock;
    @Column(name = "prompt_version", nullable = false, length = 20)
    public String promptVersion;
    @Column(nullable = false)
    public Instant createdAt;

    protected ExperienceAiChatMessage() { }

    public ExperienceAiChatMessage(UUID experience, UUID member, long sequence,
            String requestId, String role, String content,
            String provider, boolean mock, String promptVersion) {
        this.experienceId = experience;
        this.memberId = member;
        this.sequenceNo = sequence;
        this.requestId = requestId;
        this.role = role;
        this.content = content;
        this.provider = provider;
        this.mock = mock;
        this.promptVersion = promptVersion;
        this.createdAt = Instant.now();
    }
}