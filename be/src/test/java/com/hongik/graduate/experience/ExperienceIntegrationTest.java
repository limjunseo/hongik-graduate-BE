package com.hongik.graduate.experience;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.hongik.graduate.BE.experience.repository.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class ExperienceIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired MemberRepository members;
 @Autowired AuthService auth;
 @Autowired ExperienceQuestionRepository questions;
 @Test void experienceDraftCompleteAndOwnership() throws Exception {
  Member member=members.saveAndFlush(Member.google("exp-user@example.com","exp-user-id"));
  Member stranger=members.saveAndFlush(Member.google("exp-stranger@example.com","exp-stranger-id"));
  String token=auth.issueForAuthenticatedMember(member).accessToken();
  String other=auth.issueForAuthenticatedMember(stranger).accessToken();
  mvc.perform(get("/api/experience-tags").header("Authorization","Bearer "+token))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(6));
  String created=mvc.perform(post("/api/experiences").header("Authorization","Bearer "+token)
   .contentType(MediaType.APPLICATION_JSON)
   .content("{\"title\":\"테스트 경험\",\"tagCode\":\"STAR\",\"creationMethod\":\"QUICK\",\"startedAt\":\"2026-01-01\",\"endedAt\":\"2026-02-01\"}"))
   .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("DRAFT"))
   .andReturn().getResponse().getContentAsString();
  String id=JsonPath.read(created,"$.data.id");
  mvc.perform(get("/api/experiences/"+id).header("Authorization","Bearer "+other))
    .andExpect(status().isNotFound());
  mvc.perform(get("/api/experiences/"+id).header("Authorization","Bearer "+token))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.progress").value(0));
  mvc.perform(patch("/api/experiences/"+id+"/status").header("Authorization","Bearer "+token)
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
    .andExpect(status().isBadRequest());
  var qs=questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc("STAR");
  StringBuilder body=new StringBuilder("{\"answers\":[");
  for(int i=0;i<qs.size();i++){
   if(i>0)body.append(",");
   body.append("{\"questionId\":\"").append(qs.get(i).id).append("\",\"content\":\"테스트 답변\",\"displayOrder\":").append(i+1).append("}");
  }
  body.append("]}");
  mvc.perform(put("/api/experiences/"+id+"/answers/"+qs.get(0).id).header("Authorization","Bearer "+token)
    .contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"개별 저장 검증\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.content").value("개별 저장 검증"));
  mvc.perform(put("/api/experiences/"+id+"/answers").header("Authorization","Bearer "+token)
     .contentType(MediaType.APPLICATION_JSON).content(body.toString()))
     .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(4));
  mvc.perform(get("/api/experiences/"+id).header("Authorization","Bearer "+token))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.progress").value(100));
  mvc.perform(patch("/api/experiences/"+id+"/status").header("Authorization","Bearer "+token)
    .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"COMPLETED\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
  mvc.perform(delete("/api/experiences/"+id).header("Authorization","Bearer "+token))
    .andExpect(status().isOk());
  mvc.perform(get("/api/experiences/"+id).header("Authorization","Bearer "+token))
    .andExpect(status().isNotFound());
 }
}