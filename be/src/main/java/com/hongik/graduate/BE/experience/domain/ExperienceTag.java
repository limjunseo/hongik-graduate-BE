package com.hongik.graduate.BE.experience.domain;
import jakarta.persistence.*;
@Entity @Table(name="experience_tags") public class ExperienceTag {
 @Id @Column(length=50) public String code;
 @Column(nullable=false,length=100) public String name;
 public int displayOrder;public boolean active=true;
 protected ExperienceTag(){}
 public ExperienceTag(String c,String n,int o){code=c;name=n;displayOrder=o;}
}