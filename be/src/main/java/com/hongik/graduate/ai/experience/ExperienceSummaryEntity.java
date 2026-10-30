package com.hongik.graduate.ai.experience;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Separate from legacy experiences.summary; one structured snapshot per experience. */
@Entity
@Table(name = "experience_summaries")
public class ExperienceSummaryEntity {
    @Id
    @Column(name = "experience_id", nullable = false)
    public UUID experienceId;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    public String snapshotJson;

    @Column(nullable = false, length = 10)
    public String schemaVersion;

    @Column(nullable = false, length = 10)
    public String origin;

    @Column(nullable = false)
    public Instant updatedAt;

    protected ExperienceSummaryEntity() { }

    public ExperienceSummaryEntity(UUID experienceId, String snapshotJson,
            String schemaVersion, String origin) {
        this.experienceId = experienceId;
        this.snapshotJson = snapshotJson;
        this.schemaVersion = schemaVersion;
        this.origin = origin;
    }

    @PrePersist @PreUpdate
    void updated() {
        updatedAt = Instant.now();
    }
}
