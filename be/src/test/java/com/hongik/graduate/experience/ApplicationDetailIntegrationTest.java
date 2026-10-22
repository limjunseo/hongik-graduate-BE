package com.hongik.graduate.experience;
import com.hongik.graduate.BE.coverletter.service.*;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ApplicationDetailIntegrationTest{
 @Autowired MemberRepository members;
 @Autowired CoverLetterService covers;
 @Autowired ApplicationDetailService details;
 @Autowired ApplicationDetailRepository repository;
 @Test void detailsOwnershipValidationUpdateAndCascade(){
  var a=members.save(Member.google("details-owner@example.com","details-owner"));
  var b=members.save(Member.google("details-other@example.com","details-other"));
  var cover=covers.create(a.getId(),new CoverLetterService.Create("회사","직무",LocalDate.of(2026,11,1),null));
  assertEquals(LocalDate.of(2026,11,1),details.get(a.getId(),cover.id).deadline());
  assertFalse(details.get(a.getId(),cover.id).hasResume());
  var saved=details.save(a.getId(),cover.id,new ApplicationDetailService.Input(
   LocalDate.of(2026,11,15),LocalDate.of(2026,11,25),"PENDING",true,false));
  assertEquals("PENDING",saved.documentResult());
  assertTrue(saved.hasResume());
  assertEquals(LocalDate.of(2026,11,15),covers.get(a.getId(),cover.id).deadline);
  assertThrows(RuntimeException.class,()->details.get(b.getId(),cover.id));
  assertThrows(RuntimeException.class,()->details.save(b.getId(),cover.id,
   new ApplicationDetailService.Input(null,null,"PASSED",false,true)));
  assertThrows(RuntimeException.class,()->details.save(a.getId(),cover.id,
   new ApplicationDetailService.Input(null,null,"UNKNOWN",false,true)));
  details.save(a.getId(),cover.id,new ApplicationDetailService.Input(null,null,"PASSED",false,true));
  assertNull(details.get(a.getId(),cover.id).deadline());
  assertEquals("PASSED",details.get(a.getId(),cover.id).documentResult());
  assertTrue(details.get(a.getId(),cover.id).hasPortfolio());
  covers.delete(a.getId(),cover.id);
  assertTrue(repository.findByCoverLetterId(cover.id).isEmpty());
 }
}