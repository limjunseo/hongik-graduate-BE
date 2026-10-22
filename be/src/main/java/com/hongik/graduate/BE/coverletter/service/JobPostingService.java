package com.hongik.graduate.BE.coverletter.service;
import com.hongik.graduate.BE.coverletter.domain.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
@Service @Transactional
public class JobPostingService {
 private final CoverLetterRepository covers;
 private final JobPostingRepository postings;
 public JobPostingService(CoverLetterRepository covers,JobPostingRepository postings){
  this.covers=covers;this.postings=postings;
 }
 public record Input(String url,String jdOriginal,String qualifications,String preferences,String hiringProcess){}
 private BusinessException invalid(){return new BusinessException(ErrorCode.INVALID_REQUEST);}
 private void own(UUID user,UUID cover){
  if(covers.findByIdAndMemberId(cover,user).isEmpty())
   throw new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND);
 }
 private String validUrl(String url){
  if(url==null||url.isBlank())return null;
  if(url.length()>2048||!url.equals(url.trim()))throw invalid();
  try{
   URI uri=new URI(url);
   if(uri.getHost()==null||uri.getRawUserInfo()!=null||
    !Set.of("http","https").contains(uri.getScheme()==null?"":uri.getScheme().toLowerCase(Locale.ROOT)))
    throw invalid();
  }catch(URISyntaxException e){throw invalid();}
  return url;
 }
 private String validText(String value){
  if(value!=null&&value.length()>100000)throw invalid();
  return value;
 }
 @Transactional(readOnly=true) public JobPosting get(UUID user,UUID cover){
  own(user,cover);
  return postings.findByCoverLetterId(cover)
   .orElseThrow(()->new BusinessException(ErrorCode.COVER_LETTER_NOT_FOUND));
 }
 public JobPosting save(UUID user,UUID cover,Input input){
  own(user,cover);
  if(input==null)throw invalid();
  String url=validUrl(input.url());
  String original=validText(input.jdOriginal());
  String qualifications=validText(input.qualifications());
  String preferences=validText(input.preferences());
  String process=validText(input.hiringProcess());
  JobPosting posting=postings.findByCoverLetterId(cover).orElseGet(()->new JobPosting(cover));
  posting.url=url;
  posting.jdOriginal=original;
  posting.qualifications=qualifications;
  posting.preferences=preferences;
  posting.hiringProcess=process;
  return postings.save(posting);
 }
 public void delete(UUID user,UUID cover){
  own(user,cover);
  postings.deleteByCoverLetterId(cover);
 }
}