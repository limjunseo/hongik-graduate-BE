package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
@Entity @Table(name="job_postings",uniqueConstraints=@UniqueConstraint(columnNames="cover_letter_id"))
public class JobPosting {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="cover_letter_id",nullable=false) public UUID coverLetterId;
 @Column(name="source_url",length=2048) public String url;
 @Column(name="jd_original",columnDefinition="TEXT") public String jdOriginal;
 @Column(columnDefinition="TEXT") public String qualifications;
 @Column(columnDefinition="TEXT") public String preferences;
 @Column(name="hiring_process",columnDefinition="TEXT") public String hiringProcess;
 @Column(nullable=false,updatable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 protected JobPosting(){}
 public JobPosting(UUID id){coverLetterId=id;}
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=Instant.now();}
}