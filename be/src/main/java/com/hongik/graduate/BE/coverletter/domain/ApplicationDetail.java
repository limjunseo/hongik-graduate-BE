package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;
import java.time.*;import java.util.*;
@Entity @Table(name="application_details",uniqueConstraints=@UniqueConstraint(columnNames="cover_letter_id"))
public class ApplicationDetail{
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="cover_letter_id",nullable=false) public UUID coverLetterId;
 public LocalDate interviewDate;
 @Column(length=20) public String documentResult;
 @Column(nullable=false) public boolean hasResume;
 @Column(nullable=false) public boolean hasPortfolio;
 @Column(nullable=false,updatable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 protected ApplicationDetail(){}
 public ApplicationDetail(UUID coverLetterId){this.coverLetterId=coverLetterId;}
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=Instant.now();}
}