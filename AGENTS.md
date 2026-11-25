# AGENTS.md

이 프로젝트(`com.example.app`)에서 일하는 에이전트를 위한 **지도**다. 자세한 내용은 링크된 문서에 있고, 이 파일은 어디를 봐야 하는지만 알려준다.

## 1. 작업 흐름
모든 작업은 아래 순서를 따른다.

```
(이슈)      issue-report 스킬     문제·제안을 GitHub 이슈로 (선택)
      ↓
① 계획      plan-task 스킬        plan/NNN-slug/{plan.md, task.md} 생성 → pr-draft로 브랜치 NNN-slug + Draft PR
      ↓
② 구현      tdd-agent-teams 스킬  task 하나씩 Red → Green → Refactor (서브에이전트 팀)
      ↓
③ 기록      task.md [x] + 메모, 설계 결정은 plan.md 결정 기록에, task마다 커밋·push
      ↓
④ 구조 반영  ARCHITECTURE.md 갱신 (아래 2절 규칙)
      ↓
⑤ 최종 검증  계획의 task가 모두 끝나면 ./gradlew check 전체 통과 확인 (아래 표)
      ↓
⑥ 마무리    plan-task archive로 plan/archive/ 이동 → pr-draft로 PR Ready (머지는 사용자)
```

**최종 검증 (⑤)**: 계획의 마지막 task를 끝낸 뒤, archive하기 전에 리드가 직접 `./gradlew check`를 실행한다. 판정 단계이므로 파일을 고치는 `spotlessApply`는 붙이지 않는다. 실패하면 archive하지 않고 고친 뒤 다시 `check`한다.

| 검사 | 도구 | 실패하면 |
|---|---|---|
| 단위·슬라이스·통합 테스트, Flyway 마이그레이션 ↔ Entity 일치 | JUnit (`test`) | 실패 테스트를 고칠 task를 `task.md`에 추가 |
| 계층·컨텍스트 의존, 클래스 위치 | ArchUnit (`ArchitectureTest`, `test`에 포함) | 규칙을 끄지 말고 코드를 옮긴다 |
| 서식 | Spotless (`spotlessCheck`, 검사만 함) | `./gradlew spotlessApply`로 고치고 바뀐 파일을 확인한 뒤 다시 `check` |
| 이름·금지 API·import 규칙 | Checkstyle (`checkstyleMain`·`checkstyleTest`) | 위반 위치를 고친다 |

- `ArchitectureTest`가 실제로 돌았는지 `build/test-results/test/TEST-*ArchitectureTest.xml`에서 `tests`가 0보다 크고 `failures`·`errors`가 0인지 확인한다.
- 결과(전체 테스트 수, 실패 0, 린트 통과)를 `task.md` 맨 아래에 `> 최종 검증: <날짜> ./gradlew check 통과, 테스트 <n>/<n>`으로 남긴다.
- 같은 검사를 CI(`.github/workflows/ci.yml`)가 push·PR마다 다시 돌린다.

**GitHub 연동**: 계획 하나 = 브랜치 `NNN-slug` 하나 = PR 하나. push, PR 생성·변경, 이슈 등록은 외부에 공개되므로 스킬이 초안을 보여주고 확인을 받은 뒤 실행한다. remote가 없으면 로컬 브랜치·커밋까지만 한다. 머지는 사람이 한다.

| 상황 | 쓸 것 |
|---|---|
| 문제를 발견함, 작업을 제안함 | `/issue-report bug <요약>` · `/issue-report task <제목>` |
| 새 기능·작업을 계획 | `/plan-task new <제목> [#이슈]` (Draft PR까지 연다) |
| 진행 현황 확인 | `/plan-task status` |
| 코드 구현·버그 수정 | `/tdd-agent-teams plan/NNN-slug` |
| 오타·설정값 하나 같은 사소한 수정 | 리드가 직접 처리 (팀 스폰 안 함) |
| 계획 완료 | `./gradlew check` 통과 확인 후 `/plan-task archive NNN` (PR을 Ready로 바꾼다) |

## 2. ARCHITECTURE.md 갱신 규칙
- **언제:** TDD 사이클이 끝났을 때 패키지·엔티티·API·외부 연동·설정 중 하나라도 바뀌었으면 갱신한다. 계획을 archive하기 전에는 바뀐 게 없는지 반드시 한 번 더 확인한다.
- **무엇을:** 현재 상태만 적는다. 새 Bounded Context는 Context Map에, Aggregate·Value Object는 도메인 모델에 추가한다. 계획·할 일은 `plan/`에, 결정의 이유는 `plan.md` 결정 기록에 둔다.
- **어떻게:** 바뀐 절만 현재 상태로 고쳐 쓴다. 변경 이력·날짜·"이전에는 ~였다" 같은 과거 내용은 적지 않는다. 이력은 git이 관리한다.

## 3. 저장소 지도
```
./                            # 저장소 루트
├── README.md                 # 템플릿 목적, 시작하기 (init.sh)
├── AGENTS.md / CLAUDE.md     # 에이전트 지침 (CLAUDE.md → AGENTS.md)
├── ARCHITECTURE.md           # 현재 구조: DDD 계층·의존 방향, Context Map, 도메인, API, 설정
├── CONVENTIONS.md            # 이름·API·에러 응답·Lombok·로깅·테스트·커밋·Bean 등록·Entity 시간·설정 값·DB 마이그레이션 규칙, 린트 도구
├── plan/
│   ├── NNN-slug/plan.md      # 계획의 목표·설계·결정 기록
│   ├── NNN-slug/task.md      # 실행 단위 작업과 진행 상태
│   └── archive/              # 끝난 계획 (과거 결정의 이유)
├── src/main/java/com/example/app/
│   └── common/               # Shared Kernel: 설정(config), 에러 모델(error), 생성·수정 시각 기반 Entity(audit), 에러 응답·추적 ID(web)
├── src/main/resources/       # application.yml(공통), application-local.yml, application-prod.yml
│   └── db/migration/         # Flyway 마이그레이션 V{n}__{설명}.sql (스키마의 유일한 출처)
├── .env.example              # 환경 변수 목록·기본값(DB 접속, 시간대, 부하 테스트로 정한 튜닝 값). 개인 값은 .env(커밋 금지)
├── src/test/java/com/example/app/
├── src/test/resources/       # 테스트 전용 설정: config/application.yml(기본 프로필 test), application-test.yml, test.env(테스트 변수)
│   └── db/migration/         # 테스트 전용 Entity의 테이블 (R__, 테스트에서만 적용)
├── .github/
│   ├── workflows/ci.yml          # push·PR마다 ./gradlew check
│   ├── pull_request_template.md  # PR 양식 (pr-draft 스킬도 같은 양식)
│   └── ISSUE_TEMPLATE/           # bug.yml, task.yml 이슈 양식 (issue-report 스킬도 같은 양식)
├── compose.yaml              # 로컬 개발용 MySQL 8.4 (bootRun용, 테스트는 Testcontainers)
├── build.gradle.kts          # 의존성, Spotless·Checkstyle 설정
├── config/checkstyle/        # checkstyle.xml(린트 규칙), import-control.xml(계층 의존 규칙)
├── lombok.config             # 금지 어노테이션 검사
├── .editorconfig
└── .claude/
    ├── settings.json                # 공용 권한(gradlew·docker·git 조회). 개인 설정은 settings.local.json
    ├── skills/plan-task/            # 계획 쌍 생성·갱신·보관, 템플릿
    ├── skills/tdd-agent-teams/      # 리드의 TDD 진행 절차
    ├── skills/pr-draft/             # 계획별 Draft PR 열기(open)·Ready 전환(ready)
    ├── skills/issue-report/         # 이슈 양식에 맞춰 이슈 작성·등록
    ├── skills/karpathy-guidelines/  # 코딩 행동 원칙 (외부, MIT)
    └── agents/tdd-{red,green,refactor}.md  # TDD 팀 서브에이전트
```

## 4. 명령어
| 목적 | 명령 |
|---|---|
| MySQL 실행 / 종료 | `docker compose up -d` / `docker compose down` |
| 로컬 값 바꾸기 (포트 충돌 등) | `cp .env.example .env` 후 `.env` 수정 (예: `DB_PORT=3307`). compose와 앱이 같은 `.env`를 읽는다 |
| 앱 실행 | `./gradlew bootRun` (8080, 프로필을 주지 않으면 `local`) |
| 운영 프로필로 실행 | `.env.example`의 변수를 모두 환경변수로 넣고 `SPRING_PROFILES_ACTIVE=prod` (예: `docker run --env-file .env.prod …`). prod는 파일을 읽지 않는다 |
| 헬스 | `/actuator/health` |
| 전체 테스트 | `./gradlew test` (Docker 필요, 로컬 MySQL 불필요) |
| 특정 테스트 | `./gradlew test --tests 'com.example.app.<클래스>'` |
| 서식 자동 정리 | `./gradlew spotlessApply` |
| 테스트 + 린트 (커밋 전) | `./gradlew spotlessApply check` |

## 5. 기술 규칙
- Java 25, Spring Boot 4.2.0, Gradle Kotlin DSL, MySQL 8.4, Lombok.
- Boot 4는 starter·테스트 어노테이션 패키지가 3.x와 다르다. import 경로는 추측하지 말고 클래스패스에서 확인한다.
- 스키마는 Flyway 마이그레이션만으로 바꾼다. 모든 환경이 `ddl-auto: validate`이므로 Entity를 추가·변경하면 같은 변경에서 `db/migration`에 마이그레이션을 추가해야 테스트가 통과한다. 규칙은 CONVENTIONS.md 12절.
- DB 테스트는 Testcontainers MySQL을 쓴다. DB가 필요한 테스트에는 `@Import(TestcontainersConfig.class)`를, JPA 슬라이스에는 추가로 `@AutoConfigureTestDatabase(replace = NONE)`를 붙인다. 로컬 개발 DB(compose)는 테스트에 쓰지 않는다.
- 문서·주석·`@DisplayName`은 한국어, 코드 식별자는 영어.
- 이름·API·에러 응답·Lombok·로깅·테스트·커밋 규칙은 [CONVENTIONS.md](./CONVENTIONS.md)를 따른다. 작업을 끝내기 전에 `./gradlew spotlessApply check`가 통과해야 한다.

## 6. DDD 규칙
구조(패키지·의존 방향·계층별 테스트)는 [ARCHITECTURE.md 3절](./ARCHITECTURE.md#3-ddd-구조)에 있다. 코드를 쓸 때는 아래를 지킨다.

**모델링**
- 새 기능은 먼저 어느 Bounded Context에 속하는지 정한다. 기존 컨텍스트에 맞지 않으면 새 컨텍스트를 만들기 전에 `plan.md`에 이유를 적고 사용자와 합의한다.
- 용어는 Ubiquitous Language로 통일한다. 같은 개념은 코드·문서·`@DisplayName`에서 같은 이름을 쓴다.
- 식별자가 의미 있으면 Entity, 값으로만 비교하면 Value Object(`record` 또는 `@Embeddable`, 불변)로 만든다.

**Aggregate와 상태 변경**

이 템플릿은 **단일 DB 모놀리스**를 전제로 한다. 상태는 도메인 메서드가 바꾸고, 트랜잭션은 Application Service가 정한다. 예시 코드는 [CONVENTIONS.md 13절](./CONVENTIONS.md#13-상태-변경과-트랜잭션).

| 누가 | 하는 일 |
|---|---|
| Aggregate Root (`domain`) | 의도가 드러나는 메서드(`cancel()`, `ship()`, `changeName()`)로 자기 상태를 바꾸고, 그 전에 불변식·상태 전이 규칙을 검사해 어기면 `BusinessException`을 던진다 |
| Application Service (`application`) | `@Transactional` 경계 안에서 조회 → 도메인 메서드 호출. 변경은 dirty checking으로 반영되므로 `save()`는 새 Aggregate에만 부른다 |

- public setter는 두지 않는다. Lombok `@Setter`는 `lombok.config`가, 손으로 쓴 `setXxx()`는 ArchUnit이 막는다. **[자동]**
- 상태 전이 규칙(예: 배송된 주문은 취소 불가)은 Service의 `if`가 아니라 도메인 메서드 안에 둔다. Service에 `if (order.getStatus() == ...)`가 생기면 도메인으로 옮길 신호다.
- 다른 Aggregate는 객체가 아니라 ID로 참조한다. Aggregate가 다른 Aggregate의 메서드를 직접 호출하지 않는다.
- 여러 Aggregate를 함께 바꿔야 하면 Application Service가 각각의 도메인 메서드를 차례로 호출한다. 한 트랜잭션에서 여러 Aggregate를 바꿔도 된다(단일 DB라 원자성이 보장된다).
- 다른 Bounded Context의 Aggregate를 바꿀 때도 그 컨텍스트의 Application Service를 호출해 같은 트랜잭션에 참여한다.
- Domain Event는 기본으로 쓰지 않는다. 부가 작업(알림, 외부 연동)이 실패해도 본 작업은 성공해야 하거나 비동기가 필요할 때만 쓰고, 도입할 때는 `plan.md` 결정 기록에 이유를 남긴다.
- Repository는 Aggregate Root 단위로만 만든다.

**계층**
- 아래 규칙은 Checkstyle(`config/checkstyle/import-control.xml`, import 문)과 ArchUnit(`ArchitectureTest`, 실제 의존성)이 빌드에서 막는다. 규칙을 바꾸면 두 파일을 함께 고친다.
- `domain`에 Spring 빈, 웹, 외부 API 코드를 넣지 않는다. JPA 매핑 어노테이션만 허용한다.
- `@Transactional`은 `application` 계층에만 붙인다.
- `presentation`은 Entity를 그대로 반환하지 않고 Response DTO로 바꾼다. Controller는 Repository를 직접 호출하지 않는다.
- 비즈니스 규칙은 `domain`에 둔다. `application`은 흐름 조율(조회 → 도메인 메서드 호출, 새 Aggregate면 저장)과 트랜잭션 경계만 맡는다.
- 실패는 `BusinessException` + 컨텍스트의 `ErrorCode`로 던진다. HTTP 응답 변환은 `common.web.GlobalExceptionHandler`가 하므로 Controller에서 에러 응답을 직접 만들지 않는다.
