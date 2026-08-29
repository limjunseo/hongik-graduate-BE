# Version Compatibility Guide

이 문서는 Java, Spring Boot, Spring AI 버전을 선택하고 변경할 때 따르는 저장소
공통 기준입니다. 버전 변경 전에는 이 문서와 연결된 공식 문서를 먼저 확인합니다.

마지막 확인일: 2026-08-29

## 현재 고정 버전

| 구성 요소 | 프로젝트 버전 | 선택 기준 |
| --- | --- | --- |
| Java | 17 | 컴파일 및 CI의 최소 기준인 LTS 버전 |
| Spring Boot | 4.1.1 | 현재 stable 릴리스 |
| Spring Framework | 7.0.9 이상 | Spring Boot parent가 관리하므로 직접 고정하지 않음 |
| Spring AI | 2.0.1 | Spring Boot 4.0/4.1용 stable 릴리스 |
| Maven Wrapper | 3.9.16 | 모든 환경에서 동일한 Maven 버전 사용 |

Spring Boot 4.1.1은 Java 17 이상 26 이하를 지원합니다. 따라서 Java 17을 쓰는
이유는 최신 Spring과 호환되지 않아서가 아니라, 팀의 최소 실행 환경과 생성되는
바이트코드 기준을 안정적으로 고정하기 위해서입니다.

Spring AI 2.0.x는 Spring Boot 4.0/4.1과 Spring Framework 7.0을 기준으로
설계되었습니다. 현재 `Spring Boot 4.1.1 + Spring AI 2.0.1` 조합은 공식 지원
범위입니다.

## Java 버전 정책

- 빌드 기준은 `be/pom.xml`의 `java.version`과 Java CI의 `java-version`을 항상
  동일하게 유지합니다.
- 개발자는 Spring Boot가 지원하는 더 높은 JDK로 Java 17 대상 코드를 빌드할 수
  있지만, 팀 표준과 CI 재현성을 위해 기본 JDK는 현재 17입니다.
- Java 21 또는 25 LTS의 언어·런타임 기능이 필요하면 두 설정을 같은 PR에서 함께
  올립니다.
- 단순히 새 버전이 출시됐다는 이유만으로 비-LTS Java나 preview 기능을 적용하지
  않습니다.

## Spring 버전 정책

- Spring Boot 버전은 Maven parent에서만 관리합니다.
- Spring AI 버전은 `spring-ai-bom`에서만 관리합니다.
- Spring Framework, Jackson 등 Boot가 관리하는 전이 의존성 버전을 개별적으로
  덮어쓰지 않습니다. 보안 수정처럼 명확한 사유가 있으면 PR에 근거와 제거 조건을
  기록합니다.
- `SNAPSHOT`, milestone, release candidate는 별도 실험 브랜치 외에는 사용하지
  않습니다.

## 업그레이드 절차

1. Spring Boot 시스템 요구사항에서 지원 Java 범위를 확인합니다.
2. Spring AI 릴리스 및 업그레이드 문서에서 목표 Boot 버전과의 호환성을 확인합니다.
3. `be/pom.xml`과 `.github/workflows/java-ci.yml`의 Java 기준이 같은지 확인합니다.
4. `cd be && ./mvnw clean verify`를 실행합니다.
5. `./mvnw dependency:tree`로 Boot와 Spring AI가 의도한 버전으로 해석되는지
   확인합니다.
6. 생성된 실행 JAR의 기동과 classpath 정책 리소스 포함 여부를 확인합니다.
7. GitHub Actions가 성공한 뒤에만 `main`에 머지합니다.

메이저 버전 변경 시에는 Jackson, Jakarta EE, 서블릿 API처럼 함께 바뀌는 플랫폼
기준도 별도로 점검합니다.

## 공식 확인 경로

- [Oracle Java SE 지원 로드맵과 LTS 버전](https://www.oracle.com/java/technologies/java-se-support-roadmap.html)
- [Spring Boot 시스템 요구사항](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot stable 문서](https://docs.spring.io/spring-boot/)
- [Spring AI 프로젝트 및 stable 버전](https://spring.io/projects/spring-ai/)
- [Spring AI 2.0의 Boot 4 기준 설명](https://spring.io/blog/2026/06/12/spring-ai-2-0-0-GA-available-now/)
