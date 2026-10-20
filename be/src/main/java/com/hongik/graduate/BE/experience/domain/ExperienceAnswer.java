package com.hongik.graduate.BE.experience.domain;
import jakarta.persistence.*;import java.time.*;import java.util.*;
@Entity @Table(name="experience_answers") public class ExperienceAnswer {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="experience_id",nullable=false) public UUID experienceId;
 @Column(name="question_id") public UUID questionId;
 @Column(name="custom_label",length=200) public String customLabel;
 @Column(columnDefinition="TEXT") public String content;
 public int displayOrder;public Instant updatedAt;
 protected ExperienceAnswer(){}
 public ExperienceAnswer(UUID exp,UUID q,String label,String text,int order){
 experienceId=exp;questionId=q;customLabel=label;content=text;displayOrder=order;}
 @PrePersist @PreUpdate void touch(){updatedAt=Instant.now();}
}