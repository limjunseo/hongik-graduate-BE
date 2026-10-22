package com.hongik.graduate.BE.coverletter.service;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.experience.repository.ExperienceRepository;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class QuestionExperienceService {
 private final CoverLetterRepository covers;
 private final CoverLetterQuestionRepository questions;
 private final ExperienceRepository experiences;
 private final QuestionExperienceLinkRepository links;
 public QuestionExperienceService(CoverLetterRepository c,CoverLetterQuestionRepository q,ExperienceRepository e,QuestionExperienceLinkRepository l){
  covers=c;questions=q;experiences=e;links=l;
 }
 private void own(UUID user,UUID cover,UUID question){
  if(covers.findByIdAndMemberId(cover,user).isEmpty()
   ||questions.findByIdAndCoverLetterId(question,cover).isEmpty())
   throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
 }
 @Transactional(readOnly=true)
 public List<QuestionExperienceLink> list(UUID user,UUID cover,UUID question){
  own(user,cover,question);return links.findByQuestionIdOrderByDisplayOrderAsc(question);
 }
 public QuestionExperienceLink connect(UUID user,UUID cover,UUID question,UUID experienceId){
  own(user,cover,question);
  if(experiences.findByIdAndMemberId(experienceId,user).isEmpty())
   throw new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND);
  if(links.existsByQuestionIdAndExperienceId(question,experienceId))
   throw new BusinessException(ErrorCode.INVALID_REQUEST);
  return links.save(new QuestionExperienceLink(question,experienceId,(int)links.countByQuestionId(question)+1));
 }
 public void disconnect(UUID user,UUID cover,UUID question,UUID experienceId){
  own(user,cover,question);
  if(!links.existsByQuestionIdAndExperienceId(question,experienceId))
   throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
  links.deleteByQuestionIdAndExperienceId(question,experienceId);
  links.flush();
  var rest=links.findByQuestionIdOrderByDisplayOrderAsc(question);
  for(int i=0;i<rest.size();i++)rest.get(i).displayOrder=i+1;
 }
 @Transactional(readOnly=true)
 public List<QuestionExperienceLink> usages(UUID user,UUID experienceId){
  if(experiences.findByIdAndMemberId(experienceId,user).isEmpty())
   throw new BusinessException(ErrorCode.EXPERIENCE_NOT_FOUND);
  return links.findByExperienceId(experienceId);
 }
}