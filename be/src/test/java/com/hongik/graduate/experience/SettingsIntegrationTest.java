package com.hongik.graduate.experience;
import com.hongik.graduate.BE.member.service.SettingsService;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.BE.member.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest
class SettingsIntegrationTest {
 @Autowired MemberRepository members;
 @Autowired SettingsService settings;
 @Test void defaultsUpdateAndIsolation(){
  Member one=members.save(Member.google("settings-one@example.com","settings-one"));
  Member two=members.save(Member.google("settings-two@example.com","settings-two"));
  assertEquals("SYSTEM",settings.get(one.getId()).theme());
  assertTrue(settings.get(one.getId()).notificationsEnabled());
  settings.update(one.getId(),new SettingsService.Request("DARK",false));
  assertEquals("DARK",settings.get(one.getId()).theme());
  assertFalse(settings.get(one.getId()).notificationsEnabled());
  settings.update(one.getId(),new SettingsService.Request("LIGHT",null));
  assertFalse(settings.get(one.getId()).notificationsEnabled());
  assertEquals("SYSTEM",settings.get(two.getId()).theme());
  assertThrows(RuntimeException.class,()->settings.update(one.getId(),new SettingsService.Request("INVALID",true)));
  assertThrows(RuntimeException.class,()->settings.update(one.getId(),new SettingsService.Request(null,null)));
 }
}