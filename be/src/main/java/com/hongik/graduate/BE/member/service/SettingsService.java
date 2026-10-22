package com.hongik.graduate.BE.member.service;
import com.hongik.graduate.BE.member.domain.*;
import com.hongik.graduate.BE.member.repository.*;
import com.hongik.graduate.BE.common.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service @Transactional
public class SettingsService {
 private final MemberRepository members;
 private final MemberSettingsRepository settings;
 public SettingsService(MemberRepository members,MemberSettingsRepository settings){this.members=members;this.settings=settings;}
 public record Request(String theme,Boolean notificationsEnabled){}
 public record Response(String theme,boolean notificationsEnabled){}
 @Transactional(readOnly=true) public Response get(UUID memberId){
  ensureMember(memberId);
  return settings.findByMemberId(memberId).map(s->new Response(s.theme,s.notificationsEnabled))
   .orElse(new Response("SYSTEM",true));
 }
 public Response update(UUID memberId,Request input){
  ensureMember(memberId);
  if(input==null||(input.theme()==null&&input.notificationsEnabled()==null))throw new BusinessException(ErrorCode.INVALID_REQUEST);
  if(input.theme()!=null&&!Set.of("SYSTEM","LIGHT","DARK").contains(input.theme()))throw new BusinessException(ErrorCode.INVALID_REQUEST);
  MemberSettings current=settings.findByMemberId(memberId).orElseGet(()->new MemberSettings(memberId));
  if(input.theme()!=null)current.theme=input.theme();
  if(input.notificationsEnabled()!=null)current.notificationsEnabled=input.notificationsEnabled();
  settings.save(current);
  return new Response(current.theme,current.notificationsEnabled);
 }
 private void ensureMember(UUID memberId){
  if(!members.existsById(memberId))throw new BusinessException(ErrorCode.INVALID_REQUEST);
 }
}
