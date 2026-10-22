package com.hongik.graduate.BE.coverletter.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class CoverLetterVersionService {
 private final CoverLetterRepository covers;
 private final CoverLetterQuestionRepository questions;
 private final CoverLetterVersionRepository versions;
 private final ObjectMapper mapper=new ObjectMapper();
 public CoverLetterVersionService(CoverLetterRepository c,CoverLetterQuestionRepository q,CoverLetterVersionRepository v){
 covers=c;questions=q;versions=v;}
 public record SaveRequest(String label){}
 public record QuestionSnapshot(UUID id,String questionText,int characterLimit,String answerText,int displayOrder){}
 public record Snapshot(List<QuestionSnapshot> questions){}
 private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
 private void own(UUID user,UUID cover){
 if(covers.findByIdAndMemberId(cover,user).isEmpty())throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);}
 private CoverLetterVersion find(UUID cover,UUID id){
 return versions.findByIdAndCoverLetterId(id,cover).orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));}
 public CoverLetterVersion save(UUID user,UUID cover,SaveRequest request){
 own(user,cover);
 if(request==null||request.label()==null||request.label().isBlank()||request.label().length()>100)throw invalid();
 return versions.save(new CoverLetterVersion(cover,request.label(),snapshot(cover)));}
 private String snapshot(UUID cover){
 var copy=questions.findByCoverLetterIdOrderByDisplayOrderAsc(cover).stream()
 .map(q->new QuestionSnapshot(q.id,q.questionText,q.characterLimit,q.answerText,q.displayOrder)).toList();
 try{return mapper.writeValueAsString(new Snapshot(copy));}
 catch(JsonProcessingException e){throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);}
 }
 public List<CoverLetterVersion> list(UUID user,UUID cover){
 own(user,cover);
 return versions.findByCoverLetterIdOrderByCreatedAtDesc(cover);
 }
 public CoverLetterVersion get(UUID user,UUID cover,UUID id){
 own(user,cover);return find(cover,id);
 }
 private Snapshot parse(String json){
 try{return mapper.readValue(json,Snapshot.class);}
 catch(JsonProcessingException e){throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);}
 }
 public List<CoverLetterQuestion> restore(UUID user,UUID cover,UUID versionId){
 own(user,cover);
 Snapshot snapshot=parse(find(cover,versionId).snapshotJson);
 if(snapshot.questions()==null||snapshot.questions().size()>30)throw invalid();
 Map<UUID,CoverLetterQuestion> existing=new HashMap<>();
 var current=questions.findByCoverLetterIdOrderByDisplayOrderAsc(cover);
 for(var q:current)existing.put(q.id,q);
 Set<UUID> retained=new HashSet<>();int order=1;
 for(var item:snapshot.questions()){
  if(item==null||item.id()==null||!retained.add(item.id()))throw invalid();
  CoverLetterQuestion q=existing.get(item.id());
  if(q==null)q=new CoverLetterQuestion(cover,item.questionText(),item.characterLimit(),order);
  q.questionText=item.questionText();q.characterLimit=item.characterLimit();
  q.answerText=item.answerText()==null?"":item.answerText();q.displayOrder=order++;
  questions.save(q);
 }
 for(var q:current)if(!retained.contains(q.id))questions.delete(q);
 questions.flush();
 return questions.findByCoverLetterIdOrderByDisplayOrderAsc(cover);
 }
}
