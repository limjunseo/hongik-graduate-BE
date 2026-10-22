package com.hongik.graduate.BE.coverletter.service;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;import java.util.stream.*;
@Service @Transactional
public class CoverLetterService {
 private final CoverLetterRepository covers;private final CoverLetterQuestionRepository questions;
 private final CoverLetterVersionRepository versions;
 public CoverLetterService(CoverLetterRepository covers,CoverLetterQuestionRepository questions,CoverLetterVersionRepository versions){this.covers=covers;this.questions=questions;this.versions=versions;}
 public record Create(String companyName,String positionName,LocalDate deadline,String jdUrl){}
 public record Patch(String companyName,String positionName,LocalDate deadline,String status,String jdUrl){}
 public record QuestionCreate(String questionText,Integer characterLimit){}
 public record QuestionPatch(String questionText,Integer characterLimit,String answerText){}
 public record OrderRequest(List<UUID> questionIds){}
 private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
 private void text(String value,int max){if(value==null||value.isBlank()||value.length()>max)throw invalid();}
 private String validateJdUrl(String url){
  if(url==null||url.isBlank())return null;
  if(url.length()>2048||!url.equals(url.trim()))throw invalid();
  try{
   java.net.URI uri=new java.net.URI(url);
   String scheme=uri.getScheme();
   if(scheme==null||!(scheme.equalsIgnoreCase("http")||scheme.equalsIgnoreCase("https"))
     ||uri.getHost()==null||uri.getRawUserInfo()!=null)throw invalid();
   return url;
  }catch(java.net.URISyntaxException e){throw invalid();}
 }
 private void limit(Integer n){if(n==null||n<1||n>10000)throw invalid();}
 private void answer(String value,int max){if(value!=null&&value.codePointCount(0,value.length())>max)throw invalid();}
 private CoverLetter own(UUID user,UUID id){return covers.findByIdAndMemberId(id,user).orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));}
 private CoverLetterQuestion ownQuestion(UUID user,UUID parent,UUID q){own(user,parent);return questions.findByIdAndCoverLetterId(q,parent).orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));}
 public CoverLetter create(UUID user,Create req){
 if(req==null)throw invalid();text(req.companyName(),150);text(req.positionName(),150);
 CoverLetter cover=new CoverLetter(user,req.companyName(),req.positionName(),req.deadline());
 cover.jdUrl=validateJdUrl(req.jdUrl());
 return covers.save(cover);}
 @Transactional(readOnly=true) public List<CoverLetter> list(UUID user,String keyword,String status,int page,int size){
 if(page<0||size<1||size>100)throw invalid();
 return covers.findByMemberIdOrderByUpdatedAtDesc(user).stream()
 .filter(e->status==null||status.equals(e.status))
 .filter(e->keyword==null||keyword.isBlank()||e.companyName.contains(keyword)||e.positionName.contains(keyword))
 .skip((long)page*size).limit(size).toList();}
 @Transactional(readOnly=true) public CoverLetter get(UUID user,UUID id){return own(user,id);}
 @Transactional(readOnly=true) public List<CoverLetterQuestion> questions(UUID user,UUID parent){
 own(user,parent);return questions.findByCoverLetterIdOrderByDisplayOrderAsc(parent);}
 public CoverLetter patch(UUID user,UUID id,Patch req){
 if(req==null)throw invalid();CoverLetter c=own(user,id);
 if(req.companyName()!=null){text(req.companyName(),150);c.companyName=req.companyName();}
 if(req.positionName()!=null){text(req.positionName(),150);c.positionName=req.positionName();}
 if(req.deadline()!=null)c.deadline=req.deadline();
 if(req.jdUrl()!=null)c.jdUrl=validateJdUrl(req.jdUrl());
 if(req.status()!=null){if(!Set.of("DRAFT","IN_PROGRESS","COMPLETED").contains(req.status()))throw invalid();c.status=req.status();}
 return c;}
 public void delete(UUID user,UUID id){own(user,id);versions.deleteByCoverLetterId(id);questions.deleteByCoverLetterId(id);covers.deleteById(id);}
 public CoverLetterQuestion addQuestion(UUID user,UUID parent,QuestionCreate req){
 own(user,parent);if(req==null)throw invalid();text(req.questionText(),10000);limit(req.characterLimit());
 long count=questions.countByCoverLetterId(parent);if(count>=30)throw invalid();
 return questions.save(new CoverLetterQuestion(parent,req.questionText(),req.characterLimit(),(int)count+1));}
 public CoverLetterQuestion patchQuestion(UUID user,UUID parent,UUID q,QuestionPatch req){
 CoverLetterQuestion e=ownQuestion(user,parent,q);if(req==null)throw invalid();
 if(req.questionText()!=null){text(req.questionText(),10000);e.questionText=req.questionText();}
 if(req.characterLimit()!=null){limit(req.characterLimit());answer(e.answerText,req.characterLimit());e.characterLimit=req.characterLimit();}
 if(req.answerText()!=null){answer(req.answerText(),e.characterLimit);e.answerText=req.answerText();}
 return e;}
 public void deleteQuestion(UUID user,UUID parent,UUID q){
 ownQuestion(user,parent,q);questions.deleteById(q);
 List<CoverLetterQuestion> rest=questions.findByCoverLetterIdOrderByDisplayOrderAsc(parent);
 for(int i=0;i<rest.size();i++)rest.get(i).displayOrder=i+1;}
 public List<CoverLetterQuestion> reorder(UUID user,UUID parent,OrderRequest req){
 own(user,parent);List<CoverLetterQuestion> old=questions.findByCoverLetterIdOrderByDisplayOrderAsc(parent);
 if(req==null||req.questionIds()==null||old.size()!=req.questionIds().size())throw invalid();
 Set<UUID> desired=new HashSet<>(req.questionIds());
 if(desired.size()!=old.size()||!desired.equals(old.stream().map(q->q.id).collect(Collectors.toSet())))throw invalid();
 Map<UUID,CoverLetterQuestion> byId=old.stream().collect(Collectors.toMap(q->q.id,q->q));
 for(int i=0;i<req.questionIds().size();i++)byId.get(req.questionIds().get(i)).displayOrder=i+1;
 questions.flush();
 return questions.findByCoverLetterIdOrderByDisplayOrderAsc(parent);}
}