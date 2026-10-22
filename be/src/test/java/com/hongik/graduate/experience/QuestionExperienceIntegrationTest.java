package com.hongik.graduate.experience;
import com.hongik.graduate.BE.coverletter.service.QuestionExperienceService;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.experience.domain.Experience;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class QuestionExperienceIntegrationTest{
 @Autowired MemberRepository members;
 @Autowired CoverLetterRepository covers;
 @Autowired CoverLetterQuestionRepository questions;
 @Autowired ExperienceRepository experiences;
 @Autowired QuestionExperienceService service;
 @Test void connectAndDisconnect(){
  Member owner=members.save(Member.google("link-owner-21@example.com","link-owner-21"));
  Member outsider=members.save(Member.google("link-other-21@example.com","link-other-21"));
  CoverLetter cover=covers.save(new CoverLetter(owner.getId(),"회사","직무",null));
  CoverLetterQuestion q=questions.save(new CoverLetterQuestion(cover.id,"지원 동기",500,1));
  Experience e=experiences.save(new Experience(owner.getId(),"STAR","경험","QUICK",null,null));
  Experience other=experiences.save(new Experience(outsider.getId(),"STAR","타인 경험","QUICK",null,null));
  service.connect(owner.getId(),cover.id,q.id,e.id);
  assertEquals(1,service.list(owner.getId(),cover.id,q.id).size());
  assertThrows(RuntimeException.class,()->service.connect(owner.getId(),cover.id,q.id,e.id));
  assertThrows(RuntimeException.class,()->service.connect(owner.getId(),cover.id,q.id,other.id));
  assertThrows(RuntimeException.class,()->service.list(outsider.getId(),cover.id,q.id));
  service.disconnect(owner.getId(),cover.id,q.id,e.id);
  assertEquals(0,service.list(owner.getId(),cover.id,q.id).size());
 }
}