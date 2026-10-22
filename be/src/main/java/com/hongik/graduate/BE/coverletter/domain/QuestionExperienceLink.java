package com.hongik.graduate.BE.coverletter.domain;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="question_experiences",uniqueConstraints=@UniqueConstraint(name="uk_question_experience",columnNames={"question_id","experience_id"}))
public class QuestionExperienceLink{
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="question_id",nullable=false) public UUID questionId;
 @Column(name="experience_id",nullable=false) public UUID experienceId;
 @Column(name="display_order",nullable=false) public int displayOrder;
 @Column(name="created_at",nullable=false,updatable=false) public Instant createdAt;
 protected QuestionExperienceLink(){}
 public QuestionExperienceLink(UUID q,UUID exp,int order){questionId=q;experienceId=exp;displayOrder=order;}
 @PrePersist void create(){createdAt=Instant.now();}
}