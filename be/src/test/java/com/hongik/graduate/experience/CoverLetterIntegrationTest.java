package com.hongik.graduate.experience;
import com.hongik.graduate.BE.auth.service.AuthService;
import com.hongik.graduate.BE.member.domain.Member;
import com.hongik.graduate.BE.member.repository.MemberRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class CoverLetterIntegrationTest {
 @Autowired MockMvc mvc;@Autowired MemberRepository members;@Autowired AuthService auth;
 @Test void crudQuestionsOrderingAndOwnership() throws Exception {
  Member me=members.saveAndFlush(Member.google("cover-owner@example.com","cover-owner"));
  Member other=members.saveAndFlush(Member.google("cover-other@example.com","cover-other"));
  String jwt=auth.issueForAuthenticatedMember(me).accessToken();
  String stranger=auth.issueForAuthenticatedMember(other).accessToken();
  String created=mvc.perform(post("/api/cover-letters").header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"companyName\":\"회사A\",\"positionName\":\"백엔드\",\"deadline\":\"2026-12-01\",\"jdUrl\":\"https://jobs.example.com/openings/42\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.companyName").value("회사A"))
    .andExpect(jsonPath("$.data.jdUrl").value("https://jobs.example.com/openings/42"))
    .andReturn().getResponse().getContentAsString();
  String id=JsonPath.read(created,"$.data.id");
  mvc.perform(get("/api/cover-letters/"+id).header("Authorization","Bearer "+jwt))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.coverLetter.jdUrl").value("https://jobs.example.com/openings/42"));
  mvc.perform(patch("/api/cover-letters/"+id).header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"jdUrl\":\"https://jobs.example.com/openings/43\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.jdUrl").value("https://jobs.example.com/openings/43"));
  mvc.perform(patch("/api/cover-letters/"+id).header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"jdUrl\":\"javascript:alert(1)\"}"))
    .andExpect(status().isBadRequest());
  mvc.perform(patch("/api/cover-letters/"+id).header("Authorization","Bearer "+stranger)
    .contentType(MediaType.APPLICATION_JSON).content("{\"jdUrl\":\"https://jobs.example.com/private\"}"))
    .andExpect(status().isNotFound());
  mvc.perform(get("/api/cover-letters/"+id).header("Authorization","Bearer "+stranger)).andExpect(status().isNotFound());
  String first=mvc.perform(post("/api/cover-letters/"+id+"/questions").header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"questionText\":\"지원 동기\",\"characterLimit\":300}"))
    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String q1=JsonPath.read(first,"$.data.id");
  String second=mvc.perform(post("/api/cover-letters/"+id+"/questions").header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"questionText\":\"입사 후 포부\",\"characterLimit\":500}"))
    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
  String q2=JsonPath.read(second,"$.data.id");
  mvc.perform(patch("/api/cover-letters/"+id+"/questions/"+q1).header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"answerText\":\"답변 입력\"}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data.answerText").value("답변 입력"));
  mvc.perform(put("/api/cover-letters/"+id+"/questions/order").header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"questionIds\":[\""+q2+"\",\""+q1+"\"]}"))
    .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(q2));
  mvc.perform(put("/api/cover-letters/"+id+"/questions/order").header("Authorization","Bearer "+jwt)
    .contentType(MediaType.APPLICATION_JSON).content("{\"questionIds\":[\""+q1+"\",\""+q1+"\"]}"))
    .andExpect(status().isBadRequest());
  mvc.perform(delete("/api/cover-letters/"+id+"/questions/"+q1).header("Authorization","Bearer "+stranger)).andExpect(status().isNotFound());
  mvc.perform(delete("/api/cover-letters/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isOk());
  mvc.perform(get("/api/cover-letters/"+id).header("Authorization","Bearer "+jwt)).andExpect(status().isNotFound());
 }
}