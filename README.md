# claude-spring-ddd

[Claude Code](https://claude.com/claude-code)와 함께 **DDD 구조의 Spring Boot 애플리케이션**을 만들기 위한 템플릿이다. Anthropic 공식 저장소가 아닌 개인 템플릿이다.

## 목적
에이전트에게 코드를 맡기면 구조가 흐트러지고, 테스트 없이 구현이 앞서가고, 왜 그렇게 만들었는지 기록이 남지 않기 쉽다. 이 템플릿은 이를 막기 위해 처음부터 다음을 갖춰 둔다.

- **DDD 뼈대:** Bounded Context별 패키지와 `domain / application / infrastructure / presentation` 4계층, 의존 방향, 계층별 테스트 방식을 정해 둔다.
- **에이전트 지침:** `AGENTS.md`(`CLAUDE.md`가 참조)에 작업 흐름, 기술 규칙, DDD 규칙을 적어 두어 에이전트가 같은 원칙으로 일한다.
- **계획 → TDD → 기록 흐름:** `plan-task` 스킬로 계획(`plan.md`)과 작업 목록(`task.md`)을 만들고, `tdd-agent-teams` 스킬이 Red → Green → Refactor 서브에이전트 팀으로 작업을 하나씩 구현한다. 계획마다 브랜치와 Draft PR이 열리고, 계획을 끝내면 PR이 Ready로 바뀐다.
- **살아 있는 구조 문서:** `ARCHITECTURE.md`에는 현재 구조만 적고 TDD 사이클마다 갱신한다. 결정의 이유는 `plan/`에, 이력은 git에 남긴다.
- **공통 기반:** RFC 9457 에러 응답(`GlobalExceptionHandler`), 요청 추적 ID(`X-Request-Id` → 로그 `trace.id`·응답 `traceId`), ECS JSON 로깅, JPA Auditing(`BaseTimeEntity`·`BaseCreatedTimeEntity`, `Clock` 기반), 린트(Spotless·Checkstyle·`lombok.config`)를 갖춰 두어 첫 기능부터 같은 형식으로 시작한다.

## 기술 스택
Java 25 · Spring Boot 4.2.0 · Gradle Kotlin DSL · Spring Data JPA · Flyway · MySQL 8.4 (Docker Compose) · Actuator · Lombok

린트: Spotless(palantir-java-format) · Checkstyle · `lombok.config`. 커밋 전에 `./gradlew spotlessApply check`. CI(GitHub Actions)가 push·PR마다 `./gradlew check`를 돌린다. 계획을 만들면 `pr-draft` 스킬이 Draft PR을 열고, 문제는 `issue-report` 스킬로 이슈 양식에 맞춰 남긴다.

## 시작하기
1. 이 템플릿으로 새 저장소를 만들고 클론한다.
2. 패키지와 프로젝트 이름을 정해 초기화한다. `init.sh`는 실행이 끝나면 스스로 삭제된다.
   ```bash
   ./init.sh com.acme.shop            # 프로젝트 이름은 마지막 세그먼트(shop)
   ./init.sh com.acme.shop shop-api   # 프로젝트 이름을 직접 지정
   ```
   | 바뀌는 것 | 기본값 | 예: `./init.sh com.acme.shop shop-api` |
   |---|---|---|
   | Java 패키지 | `com.example.app` | `com.acme.shop` |
   | 메인 클래스 | `AppApplication` | `ShopApiApplication` |
   | Gradle `group` / `rootProject.name` | `com.example` / `app` | `com.acme` / `shop-api` |
   | `spring.application.name` | `app` | `shop-api` |
   | DB 이름 / MySQL 컨테이너 | `app` / `app-mysql` | `shop_api` / `shop-api-mysql` |
3. 테스트로 확인한다. DB 테스트는 Testcontainers가 MySQL 컨테이너를 띄우므로 Docker만 켜져 있으면 된다.
   ```bash
   ./gradlew check   # 테스트 + 린트
   ```
   앱을 직접 실행할 때는 로컬 MySQL을 띄운다: `docker compose up -d && ./gradlew bootRun`
   로컬 값(포트, 계정)을 바꾸려면 `cp .env.example .env` 후 `.env`를 고친다. compose와 앱이 같은 `.env`를 읽는다.
4. 이 README를 새 프로젝트 설명으로 바꾼다.

## 작업 방법
Claude Code에서 다음 순서로 진행한다. 자세한 규칙은 [AGENTS.md](./AGENTS.md)를 본다.

```
/issue-report bug <요약>            # 문제를 이슈 양식으로 남기기
/plan-task new <기능 이름> [#이슈]  # 계획과 작업 목록 만들기 + 브랜치·Draft PR
/tdd-agent-teams plan/NNN-<slug>    # 작업 하나를 TDD로 구현
/plan-task status                   # 진행 현황
/plan-task archive NNN              # 최종 검증 → 끝난 계획 보관 → PR Ready
```

## 문서
| 파일 | 내용 |
|---|---|
| [AGENTS.md](./AGENTS.md) | 에이전트 작업 흐름, 명령어, 기술·DDD 규칙 |
| [ARCHITECTURE.md](./ARCHITECTURE.md) | 현재 구조: 계층, Context Map, 도메인 모델, API, 설정 |
| [CONVENTIONS.md](./CONVENTIONS.md) | 이름, API, 에러 응답, Lombok, 로깅, 테스트, 커밋, DB 마이그레이션 규칙 |
| `plan/` | 계획별 `plan.md`·`task.md`, 끝난 계획은 `plan/archive/` |
