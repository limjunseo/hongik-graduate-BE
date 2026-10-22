package com.hongik.graduate.BE.importdata;
import jakarta.persistence.*;
import java.time.*;import java.util.*;
@Entity @Table(name="imported_sources")
public class ImportedSource {
 @Id @GeneratedValue(strategy=GenerationType.UUID) public UUID id;
 @Column(nullable=false) public UUID memberId;
 @Column(nullable=false,length=20) public String sourceType;
 @Column(nullable=false,length=255) public String title;
 @Column(length=2048) public String sourceUrl;
 @Column(length=255) public String storageKey;
 @Column(columnDefinition="TEXT") public String collectedText;
 public long sizeBytes;
 public Instant createdAt;
 protected ImportedSource(){}
 public ImportedSource(UUID user,String type,String title){memberId=user;sourceType=type;this.title=title;}
 @PrePersist void created(){createdAt=Instant.now();}
}