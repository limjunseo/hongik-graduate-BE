package com.hongik.graduate.experience;
import com.hongik.graduate.BE.coverletter.service.*;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class JobPostingIntegrationTest {
 @Autowired MemberRepository members;
 @Autowired CoverLetterService covers;
 @Autowired JobPostingService postings;
 @Test void createUpdateDeleteAndOwnership(){
  Member owner=members.save(Member.google("jd-owner@example.com","jd-owner"));
  Member other=members.save(Member.google("jd-other@example.com","jd-other"));
  var cover=covers.create(owner.getId(),new CoverLetterService.Create("회사","직무",null,null));
  var saved=postings.save(owner.getId(),cover.id,new JobPostingService.Input(
   "https://jobs.example.com/a","JD 원문","자격요건","우대사항","서류-면접"));
  assertEquals("JD 원문",postings.get(owner.getId(),cover.id).jdOriginal);
  assertEquals(saved.id,postings.save(owner.getId(),cover.id,new JobPostingService.Input(
   "https://jobs.example.com/b","변경된 JD","자격","우대","전형")).id);
  assertEquals("변경된 JD",postings.get(owner.getId(),cover.id).jdOriginal);
  assertThrows(RuntimeException.class,()->postings.get(other.getId(),cover.id));
  assertThrows(RuntimeException.class,()->postings.save(owner.getId(),cover.id,
   new JobPostingService.Input("javascript:alert(1)","JD",null,null,null)));
  postings.delete(owner.getId(),cover.id);
  assertThrows(RuntimeException.class,()->postings.get(owner.getId(),cover.id));
  postings.save(owner.getId(),cover.id,new JobPostingService.Input(null,"원문",null,null,null));
  covers.delete(owner.getId(),cover.id);
  assertThrows(RuntimeException.class,()->postings.get(owner.getId(),cover.id));
 }
}