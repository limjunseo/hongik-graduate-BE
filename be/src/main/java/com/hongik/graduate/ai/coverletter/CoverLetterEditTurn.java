package com.hongik.graduate.ai.coverletter;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "cover_letter_ai_edit_turns",
        indexes = @Index(name = "idx_cl_edit_question_order", columnList = "question_id,sequence_no"),
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_cl_edit_sequence", columnNames = {"question_id", "sequence_no"}),
                @UniqueConstraint(name = "uq_cl_edit_request", columnNames = {"question_id", "request_id"})
        })
public class CoverLetterEditTurn {
    @Id @GeneratedValue(strategy = GenerationType.UUID) public UUID id;
    @Column(name = "cover_letter_id", nullable = false) public UUID coverLetterId;
    @Column(name = "question_id", nullable = false) public UUID questionId;
    @Column(name = "member_id", nullable = false) public UUID memberId;
    @Column(name = "sequence_no", nullable = false) public long sequenceNo;
    @Column(name = "request_id", nullable = false, length = 128) public String requestId;
    @Column(nullable = false, length = 40) public String questionType;
    @Column(name = "experience_ids_json", nullable = false, columnDefinition = "TEXT") public String experienceIdsJson;
    @Column(name = "draft_before", nullable = false, columnDefinition = "TEXT") public String draftBefore;
    @Column(nullable = false, columnDefinition = "TEXT") public String instruction;
    @Column(name = "revision_type", nullable = false, length = 40) public String revisionType;
    @Column(name = "draft_after", nullable = false, columnDefinition = "TEXT") public String draftAfter;
    @Column(name = "change_reasons_json", nullable = false, columnDefinition = "TEXT") public String changeReasonsJson;
    @Column(name = "warnings_json", nullable = false, columnDefinition = "TEXT") public String warningsJson;
    @Column(nullable = false, length = 30) public String provider;
    @Column(nullable = false) public boolean mock;
    @Column(name = "prompt_version", nullable = false, length = 20) public String promptVersion;
    @Column(nullable = false) public Instant createdAt;

    protected CoverLetterEditTurn() { }

    public CoverLetterEditTurn(UUID cover, UUID question, UUID member, long sequence,
            String requestId, String questionType, String experienceIdsJson, String before,
            String instruction, String revisionType, String after, String changeReasonsJson,
            String warningsJson, String provider, boolean mock, String promptVersion) {
        this.coverLetterId = cover; this.questionId = question; this.memberId = member;
        this.sequenceNo = sequence; this.requestId = requestId; this.questionType = questionType;
        this.experienceIdsJson = experienceIdsJson; this.draftBefore = before;
        this.instruction = instruction; this.revisionType = revisionType; this.draftAfter = after;
        this.changeReasonsJson = changeReasonsJson; this.warningsJson = warningsJson;
        this.provider = provider; this.mock = mock; this.promptVersion = promptVersion;
        this.createdAt = Instant.now();
    }
}
