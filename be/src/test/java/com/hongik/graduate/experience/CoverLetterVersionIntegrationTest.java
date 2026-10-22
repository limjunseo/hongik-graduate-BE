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
class CoverLetterVersionIntegrationTest {
 @Autowired MemberRepository members;
 @Autowired CoverLetterService covers;
 @Autowired CoverLetterVersionService versions;
 @Test void saveListRestoreAndOwnership(){
  Member member=members.save(Member.google("version-owner@example.com","version-owner"));
  Member other=members.save(Member.google("version-other@example.com","version-other"));
  var cover=covers.create(member.getId(),new CoverLetterService.Create("회사","직무",null,null));
  var q=covers.addQuestion(member.getId(),cover.id,new CoverLetterService.QuestionCreate("문항",300));
  covers.patchQuestion(member.getId(),cover.id,q.id,new CoverLetterService.QuestionPatch(null,null,"초안"));
  var v1=versions.save(member.getId(),cover.id,new CoverLetterVersionService.SaveRequest("초안"));
  covers.patchQuestion(member.getId(),cover.id,q.id,new CoverLetterService.QuestionPatch(null,null,"수정본"));
  var v2=versions.save(member.getId(),cover.id,new CoverLetterVersionService.SaveRequest("1.0"));
  assertEquals(2,versions.list(member.getId(),cover.id).size());
  assertTrue(versions.get(member.getId(),cover.id,v1.id).snapshotJson.contains("초안"));
  assertTrue(versions.get(member.getId(),cover.id,v2.id).snapshotJson.contains("수정본"));
  assertThrows(RuntimeException.class,()->versions.get(other.getId(),cover.id,v1.id));
  versions.restore(member.getId(),cover.id,v1.id);
  assertEquals("초안",covers.questions(member.getId(),cover.id).get(0).answerText);
  assertTrue(versions.get(member.getId(),cover.id,v2.id).snapshotJson.contains("수정본"));
  covers.delete(member.getId(),cover.id);
 }
}