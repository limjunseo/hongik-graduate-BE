package com.hongik.graduate.BE.coverletter.service;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;import java.util.*;
@Service @Transactional
public class ApplicationDetailService{
 private static final Set<String> RESULTS=Set.of("PENDING","PASSED","FAILED");
 private final CoverLetterRepository covers;
 private final ApplicationDetailRepository details;
 public ApplicationDetailService(CoverLetterRepository covers,ApplicationDetailRepository details){
  this.covers=covers;this.details=details;
 }
 public record Input(LocalDate deadline,LocalDate interviewDate,String documentResult,boolean hasResume,boolean hasPortfolio){}
 public record Response(UUID coverLetterId,LocalDate deadline,LocalDate interviewDate,String documentResult,boolean hasResume,boolean hasPortfolio){}
 private CoverLetter own(UUID user,UUID cover){
  return covers.findByIdAndMemberId(cover,user).orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
 }
 private Response response(CoverLetter cover,ApplicationDetail detail){
  return new Response(cover.id,cover.deadline,detail==null?null:detail.interviewDate,
    detail==null?null:detail.documentResult,detail!=null&&detail.hasResume,detail!=null&&detail.hasPortfolio);
 }
 @Transactional(readOnly=true) public Response get(UUID user,UUID coverId){
  CoverLetter cover=own(user,coverId);
  return response(cover,details.findByCoverLetterId(coverId).orElse(null));
 }
 public Response save(UUID user,UUID coverId,Input input){
  CoverLetter cover=own(user,coverId);
  if(input==null||input.documentResult()!=null&&!RESULTS.contains(input.documentResult()))
   throw new BusinessException(ErrorCode.INVALID_REQUEST);
  cover.deadline=input.deadline();
  ApplicationDetail detail=details.findByCoverLetterId(coverId).orElseGet(()->new ApplicationDetail(coverId));
  detail.interviewDate=input.interviewDate();
  detail.documentResult=input.documentResult();
  detail.hasResume=input.hasResume();detail.hasPortfolio=input.hasPortfolio();
  details.save(detail);
  return response(cover,detail);
 }
}