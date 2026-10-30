package com.hongik.graduate.BE.experience.service;
import com.hongik.graduate.BE.experience.domain.*;
import com.hongik.graduate.BE.experience.repository.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;
@Service @Transactional
public class ExperienceService {
 private final ExperienceRepository experiences;private final ExperienceTagRepository tags;
 private final ExperienceQuestionRepository questions;private final ExperienceAnswerRepository answers;
 private final com.hongik.graduate.BE.coverletter.domain.QuestionExperienceLinkRepository links;
 private final com.hongik.graduate.ai.experience.ExperienceSummaryRepository structuredSummaries;
 public ExperienceService(ExperienceRepository e,ExperienceTagRepository t,ExperienceQuestionRepository q,ExperienceAnswerRepository a,
 com.hongik.graduate.BE.coverletter.domain.QuestionExperienceLinkRepository links,
 com.hongik.graduate.ai.experience.ExperienceSummaryRepository structuredSummaries){
 experiences=e;tags=t;questions=q;answers=a;this.links=links;this.structuredSummaries=structuredSummaries;}
 public record Create(String title,String tagCode,String creationMethod,LocalDate startedAt,LocalDate endedAt){}
 public record Patch(String title,String summary,LocalDate startedAt,LocalDate endedAt){}
 public record AnswerInput(UUID questionId,String customLabel,String content,Integer displayOrder){}
 public record AnswersInput(List<AnswerInput> answers){}
 public record StatusInput(String status){}
 private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
 private Experience own(UUID user,UUID id){return experiences.findByIdAndMemberId(id,user).orElseThrow(()->new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND));}
 private void dates(LocalDate a,LocalDate b){if(a!=null&&b!=null&&b.isBefore(a))throw invalid();}
 private void title(String t){if(t==null||t.isBlank()||t.length()>150)throw invalid();}
 public List<ExperienceTag> tags(){return tags.findByActiveTrueOrderByDisplayOrderAsc();}
 @Transactional(readOnly=true) public List<ExperienceQuestion> questions(String tag){if(!tags.existsById(tag))throw invalid();return questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(tag);}
 public Experience create(UUID user,Create request){
 title(request.title());dates(request.startedAt(),request.endedAt());
 if(!tags.existsById(request.tagCode())||!Set.of("QUICK","CONVERSATION","IMPORT").contains(request.creationMethod()))throw invalid();
 return experiences.save(new Experience(user,request.tagCode(),request.title(),request.creationMethod(),request.startedAt(),request.endedAt()));
 }
 @Transactional(readOnly=true) public Experience get(UUID user,UUID id){return own(user,id);}
 @Transactional(readOnly=true) public List<Experience> list(UUID user,String keyword,String tag,String status,int page,int size){
 if(page<0||size<1||size>100)throw invalid();
 return experiences.findByMemberIdOrderByUpdatedAtDesc(user).stream()
 .filter(e->tag==null||tag.equals(e.tagCode)).filter(e->status==null||status.equals(e.status))
 .filter(e->keyword==null||keyword.isBlank()||e.title.contains(keyword)||(e.summary!=null&&e.summary.contains(keyword)))
 .skip((long)page*size).limit(size).toList();
 }
 public Experience patch(UUID user,UUID id,Patch req){
 Experience e=own(user,id);
 if(req.title()!=null){title(req.title());e.title=req.title();}
 if(req.summary()!=null){if(req.summary().length()>20000)throw invalid();e.summary=req.summary();}
 LocalDate a=req.startedAt()==null?e.startedAt:req.startedAt(),b=req.endedAt()==null?e.endedAt:req.endedAt();
 dates(a,b);e.startedAt=a;e.endedAt=b;return e;
 }
 @Transactional(readOnly=true) public List<ExperienceAnswer> answers(UUID user,UUID id){own(user,id);return answers.findByExperienceIdOrderByDisplayOrderAsc(id);}
 @Transactional(readOnly=true) public int progress(UUID user,UUID id){
 Experience e=own(user,id);
 var required=questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(e.tagCode).stream().filter(q->q.required).toList();
 if(required.isEmpty())return 100;
 var filled=answers.findByExperienceIdOrderByDisplayOrderAsc(id).stream()
 .filter(a->a.questionId!=null&&a.content!=null&&!a.content.isBlank()).map(a->a.questionId).collect(java.util.stream.Collectors.toSet());
 long n=required.stream().filter(q->filled.contains(q.id)).count();
 return (int)Math.round(100.0*n/required.size());
 }
 public List<ExperienceAnswer> putAnswers(UUID user,UUID id,AnswersInput request){
 Experience e=own(user,id);
 if(request==null||request.answers()==null||request.answers().size()>50)throw invalid();
 Set<UUID> used=new HashSet<>();List<ExperienceAnswer> replacement=new ArrayList<>();
 for(AnswerInput a:request.answers()){
 if(a==null||a.content()!=null&&a.content().length()>5000||a.displayOrder()==null||a.displayOrder()<0)throw invalid();
 if(a.questionId()!=null){
 var question=questions.findById(a.questionId()).orElseThrow(this::invalid);
 if(!question.active||!question.tagCode.equals(e.tagCode)||!used.add(a.questionId())||a.customLabel()!=null)throw invalid();
 } else if(a.customLabel()==null||a.customLabel().isBlank()||a.customLabel().length()>200)throw invalid();
 replacement.add(new ExperienceAnswer(id,a.questionId(),a.customLabel(),a.content(),a.displayOrder()));
 }
 answers.deleteByExperienceId(id);answers.flush();answers.saveAll(replacement);
 if("COMPLETED".equals(e.status))validateComplete(e);
 return answers.findByExperienceIdOrderByDisplayOrderAsc(id);
 }
 public ExperienceAnswer putSingleAnswer(UUID user,UUID id,UUID questionId,String content){
 Experience e=own(user,id);
 if(content==null||content.length()>5000)throw invalid();
 var q=questions.findById(questionId).orElseThrow(this::invalid);
 if(!q.active||!q.tagCode.equals(e.tagCode))throw invalid();
 ExperienceAnswer a=answers.findByExperienceIdOrderByDisplayOrderAsc(id).stream()
 .filter(x->questionId.equals(x.questionId)).findFirst().orElseGet(()->
 new ExperienceAnswer(id,questionId,null,content,q.displayOrder));
 a.content=content;
 ExperienceAnswer saved=answers.save(a);
 if("COMPLETED".equals(e.status))validateComplete(e);
 return saved;
 }
 public Experience status(UUID user,UUID id,StatusInput request){
 Experience e=own(user,id);if(request==null||!Set.of("DRAFT","COMPLETED").contains(request.status()))throw invalid();
 if("COMPLETED".equals(request.status()))validateComplete(e);
 e.status=request.status();return e;
 }
 private void validateComplete(Experience e){
 title(e.title);dates(e.startedAt,e.endedAt);
 if(e.startedAt==null||e.endedAt==null)throw invalid();
 Set<UUID> filled=new HashSet<>();
 for(ExperienceAnswer a:answers.findByExperienceIdOrderByDisplayOrderAsc(e.id))
 if(a.questionId!=null&&a.content!=null&&!a.content.isBlank())filled.add(a.questionId);
 for(ExperienceQuestion q:questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(e.tagCode))
 if(q.required&&!filled.contains(q.id))throw invalid();
 }
 public void delete(UUID user,UUID id){own(user,id);links.deleteByExperienceId(id);structuredSummaries.deleteById(id);answers.deleteByExperienceId(id);experiences.deleteById(id);}
}