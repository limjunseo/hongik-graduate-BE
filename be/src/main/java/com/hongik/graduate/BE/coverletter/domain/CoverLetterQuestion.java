package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;import java.util.*;import java.time.*;
@Entity @Table(name="cover_letter_questions",indexes=@Index(name="idx_cl_questions_parent",columnList="cover_letter_id"))
public class CoverLetterQuestion {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="cover_letter_id",nullable=false) public UUID coverLetterId;
 @Column(nullable=false,columnDefinition="TEXT") public String questionText;
 @Column(nullable=false) public int characterLimit;
 @Column(nullable=false,columnDefinition="TEXT") public String answerText="";
 @Column(nullable=false) public int displayOrder;
 public Instant createdAt;public Instant updatedAt;
 protected CoverLetterQuestion(){}
 public CoverLetterQuestion(UUID parent,String question,int limit,int order){coverLetterId=parent;questionText=question;characterLimit=limit;displayOrder=order;}
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=Instant.now();}
}