package com.hongik.graduate.BE.experience.service;
import com.hongik.graduate.BE.experience.domain.*;
import com.hongik.graduate.BE.experience.repository.*;
import org.springframework.boot.CommandLineRunner;import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component public class ExperienceSeed implements CommandLineRunner {
 private final ExperienceTagRepository tags;private final ExperienceQuestionRepository questions;
 public ExperienceSeed(ExperienceTagRepository tags,ExperienceQuestionRepository questions){this.tags=tags;this.questions=questions;}
 private void tag(String code,String name,int order,String[][] q){
 if(!tags.existsById(code))tags.save(new ExperienceTag(code,name,order));
 for(int i=0;i<q.length;i++){String[] v=q[i];boolean exists=questions.findByTagCodeAndActiveTrueOrderByDisplayOrderAsc(code).stream().anyMatch(e->e.code.equals(v[0]));
 if(!exists)questions.save(new ExperienceQuestion(code,v[0],v[1],v[2],i+1));}
 }
 @Override @Transactional public void run(String...args){
 tag("STAR","STAR",1,new String[][]{
 {"SITUATION","Situation","배경 / 상황"},{"TASK","Task","역할 / 과제"},
 {"ACTION","Action","행동"},{"RESULT","Result","결과"}});
 tag("GROWTH_VALUES","성장·가치관",2,new String[][]{
 {"BACKGROUND","성장 배경","어떤 환경과 경험 속에서 성장했나요?"},
 {"VALUES","가치관","그 경험을 통해 갖게 된 가치관은 무엇인가요?"},
 {"PRACTICE","실천 사례","이후 그 가치관을 실제로 실천했던 사례가 있나요?"},
 {"CURRENT_SELF","현재의 나","그 가치관이 지금의 나에게 어떤 영향을 주고 있나요?"}});
 tag("STRENGTH_WEAKNESS","장단점",3,new String[][]{
 {"STRENGTH","성격의 강점","내 성격의 강점은 무엇이라고 생각하나요?"},
 {"WEAKNESS","성격의 단점","반대로 보완하고 싶은 성격의 단점은 무엇인가요?"},
 {"IMPROVEMENT","보완 노력","그 단점을 보완하기 위해 어떤 노력을 하고 있나요?"},
 {"CHANGE","현재의 변화","그 노력으로 지금은 어떤 점이 달라졌나요?"}});
 tag("COLLABORATION","협업",4,new String[][]{
 {"SITUATION","협업 상황","내 성격의 장점은 무엇이라고 생각하나요?"},
 {"ROLE","나의 역할","그 협업에서 내가 맡은 역할과 책임은 무엇이었나요?"},
 {"APPROACH","협업 방식","의견 차이나 문제를 어떻게 조율하고 해결했나요?"},
 {"RESULT_LEARNING","결과와 배운 점","협업의 결과와 그 과정에서 배운 점은 무엇인가요?"}});
 tag("ACADEMIC_CAREER","학업·직무",5,new String[][]{
 {"BACKGROUND","경험 배경","학업이나 직무와 관련해 가장 기억에 남는 경험은 무엇인가요?"},
 {"ROLE_GOAL","역할과 목표","그 경험에서 맡은 역할과 달성해야 했던 목표는 무엇이었나요?"},
 {"PROCESS","수행 과정","목표를 이루기 위해 어떤 지식이나 방법을 활용했나요?"},
 {"ACHIEVEMENT","성과와 역량","그 경험을 통해 어떤 성과와 직무 역량을 얻었나요?"}});
 tag("CHALLENGE","도전·극복",6,new String[][]{
 {"SITUATION","도전 상황","나에게 가장 어렵거나 도전적이었던 경험은 무엇인가요?"},
 {"PROBLEM_GOAL","문제와 목표","그 상황에서 해결해야 했던 문제와 목표는 무엇이었나요?"},
 {"PROCESS","극복 과정","어려움을 해결하기 위해 어떤 행동과 노력을 했나요?"},
 {"RESULT_GROWTH","결과와 성장","그 결과 무엇이 달라졌고, 어떤 점에서 성장했나요?"}});
 }
}