package com.hongik.graduate.BE.experience.domain;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="experiences") public class Experience {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="member_id",nullable=false) public UUID memberId;
 @Column(name="tag_code",nullable=false,length=50) public String tagCode;
 @Column(nullable=false,length=150) public String title;
 public LocalDate startedAt; public LocalDate endedAt;
 @Column(name="creation_method",nullable=false,length=20) public String creationMethod;
 @Column(nullable=false,length=20) public String status="DRAFT";
 @Column(columnDefinition="TEXT") public String summary;
 public Instant createdAt;public Instant updatedAt;
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=Instant.now();}
 protected Experience(){}
 public Experience(UUID memberId,String tag,String title,String method,LocalDate from,LocalDate to){
 this.memberId=memberId;this.tagCode=tag;this.title=title;this.creationMethod=method;this.startedAt=from;this.endedAt=to;}
}