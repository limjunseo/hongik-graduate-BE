package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="cover_letters")
public class CoverLetter {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="member_id",nullable=false) public UUID memberId;
 @Column(name="company_name",nullable=false,length=150) public String companyName;
 @Column(name="position_name",nullable=false,length=150) public String positionName;
 public LocalDate deadline;
 @Column(nullable=false,length=20) public String status="DRAFT";
 @Column(nullable=false,updatable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 protected CoverLetter(){}
 public CoverLetter(UUID user,String company,String position,LocalDate deadline){memberId=user;companyName=company;positionName=position;this.deadline=deadline;}
 @PrePersist void onCreate(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void onUpdate(){updatedAt=Instant.now();}
}