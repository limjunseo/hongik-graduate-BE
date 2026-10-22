package com.hongik.graduate.experience;
import com.hongik.graduate.BE.coverletter.service.*;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class ApplicationBoardIntegrationTest {
 @Autowired MemberRepository members;
 @Autowired CoverLetterService covers;
 @Autowired ApplicationBoardService board;
 @Test void columnsMoveAndOwnership(){
  Member a=members.save(Member.google("board-user@example.com","board-user"));
  Member b=members.save(Member.google("board-other@example.com","board-other"));
  var one=covers.create(a.getId(),new CoverLetterService.Create("회사1","직무1",null,null));
  var two=covers.create(a.getId(),new CoverLetterService.Create("회사2","직무2",null,null));
  covers.create(b.getId(),new CoverLetterService.Create("타인회사","직무",null,null));
  assertEquals(2,board.board(a.getId()).columns().get("PLANNED").size());
  board.move(a.getId(),one.id,new ApplicationBoardService.StatusRequest("SUBMITTED"));
  assertEquals(1,board.board(a.getId()).columns().get("SUBMITTED").size());
  assertEquals(1,board.board(a.getId()).columns().get("PLANNED").size());
  assertEquals("DRAFT",covers.get(a.getId(),one.id).status);
  assertThrows(RuntimeException.class,()->board.move(a.getId(),two.id,
    new ApplicationBoardService.StatusRequest("INTERVIEW")));
  assertThrows(RuntimeException.class,()->board.move(b.getId(),one.id,
    new ApplicationBoardService.StatusRequest("CLOSED")));
  assertEquals(0,board.board(b.getId()).columns().get("SUBMITTED").size());
 }
}