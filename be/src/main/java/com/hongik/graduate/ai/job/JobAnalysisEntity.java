package com.hongik.graduate.ai.job;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;

@Entity
@Table(name = "job_analyses")
public class JobAnalysisEntity {
    @Id @Column(name = "job_posting_id", nullable = false)
    public UUID jobPostingId;
    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    public String snapshotJson;
    @Column(nullable = false, length = 10) public String schemaVersion;
    @Column(nullable = false, length = 12) public String status;
    @Column(nullable = false) public Instant updatedAt;

    protected JobAnalysisEntity() { }
    public JobAnalysisEntity(UUID postingId, String json, String version, String status) {
        this.jobPostingId = postingId;
        this.snapshotJson = json;
        this.schemaVersion = version;
        this.status = status;
    }
    @PrePersist @PreUpdate void touch() { updatedAt = Instant.now(); }
}
