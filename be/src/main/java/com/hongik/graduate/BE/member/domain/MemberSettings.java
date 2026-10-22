package com.hongik.graduate.BE.member.domain;
import jakarta.persistence.*;
import java.util.*;
import java.time.*;
@Entity @Table(name="member_settings",uniqueConstraints=@UniqueConstraint(columnNames="member_id"))
public class MemberSettings {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(name="member_id",nullable=false,unique=true) public UUID memberId;
 @Column(nullable=false,length=10) public String theme="SYSTEM";
 @Column(name="notifications_enabled",nullable=false) public boolean notificationsEnabled=true;
 @Column(nullable=false,updatable=false) public Instant createdAt;
 @Column(nullable=false) public Instant updatedAt;
 protected MemberSettings(){}
 public MemberSettings(UUID memberId){this.memberId=memberId;}
 @PrePersist void create(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void update(){updatedAt=Instant.now();}
}