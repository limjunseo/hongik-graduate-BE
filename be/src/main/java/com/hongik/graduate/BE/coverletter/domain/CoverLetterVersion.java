package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;
import java.util.*;import java.time.*;
@Entity @Table(name="cover_letter_versions")
public class CoverLetterVersion {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="cover_letter_id",nullable=false) public UUID coverLetterId;
 @Column(nullable=false,length=100) public String label;
 @Column(name="snapshot_json",nullable=false,columnDefinition="TEXT") public String snapshotJson;
 @Column(nullable=false,updatable=false) public Instant createdAt;
 protected CoverLetterVersion(){}
 public CoverLetterVersion(UUID cover,String label,String snapshot){coverLetterId=cover;this.label=label;snapshotJson=snapshot;}
 @PrePersist void prePersist(){createdAt=Instant.now();}
}