package com.hongik.graduate.BE.coverletter.service;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class ApplicationBoardService {
 private static final List<String> STAGES=List.of("PLANNED","PREPARING","SUBMITTED","CLOSED");
 private final CoverLetterRepository covers;
 public ApplicationBoardService(CoverLetterRepository covers){this.covers=covers;}
 public record StatusRequest(String status){}
 public record BoardResponse(List<String> stages,Map<String,List<CoverLetter>> columns){}
 @Transactional(readOnly=true)
 public BoardResponse board(UUID user){
  Map<String,List<CoverLetter>> columns=new LinkedHashMap<>();
  for(String stage:STAGES)columns.put(stage,new ArrayList<>());
  for(CoverLetter cover:covers.findByMemberIdOrderByUpdatedAtDesc(user)){
   String stage=STAGES.contains(cover.applicationStatus)?cover.applicationStatus:"PLANNED";
   columns.get(stage).add(cover);
  }
  return new BoardResponse(STAGES,columns);
 }
 public CoverLetter move(UUID user,UUID id,StatusRequest request){
  if(request==null||request.status()==null||!STAGES.contains(request.status()))
   throw new BusinessException(ErrorCode.INVALID_REQUEST);
  CoverLetter cover=covers.findByIdAndMemberId(id,user)
   .orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
  cover.applicationStatus=request.status();
  return cover;
 }
}