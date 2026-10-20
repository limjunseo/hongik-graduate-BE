package com.hongik.graduate.BE.experience.domain;
import jakarta.persistence.*;import java.util.*;
@Entity @Table(name="experience_questions",uniqueConstraints=@UniqueConstraint(columnNames={"tag_code","code"}))
public class ExperienceQuestion {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="tag_code",nullable=false,length=50) public String tagCode;
 @Column(nullable=false,length=50) public String code;
 @Column(nullable=false,length=100) public String label;
 @Column(nullable=false,columnDefinition="TEXT") public String prompt;
 public int displayOrder;public boolean required=true;public boolean active=true;
 protected ExperienceQuestion(){}
 public ExperienceQuestion(String tag,String code,String label,String prompt,int order){
 this.tagCode=tag;this.code=code;this.label=label;this.prompt=prompt;displayOrder=order;}
}