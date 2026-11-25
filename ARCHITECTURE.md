# ARCHITECTURE.md

이 애플리케이션의 **현재** 구조. 계획과 할 일은 `plan/`에, 결정의 이유는 각 `plan.md` 결정 기록에, 변경 이력은 git에 있다.
이 문서는 지금 코드가 어떻게 되어 있는지만 적는다. 갱신 규칙은 [AGENTS.md 2절](./AGENTS.md#2-architecturemd-갱신-규칙)을 따른다.

## 1. 개요
| 항목 | 값 |
|---|---|
| 형태 | Spring Boot 단일 애플리케이션 (REST API, 모듈러 모놀리스) |
| 설계 방식 | DDD (Bounded Context별 패키지 + 계층형) |
| 런타임 | Java 25, Spring Boot 4.2.0 |
| 데이터 | MySQL 8.4 + Spring Data JPA, 스키마는 Flyway 마이그레이션 (모든 환경 `ddl-auto: validate`) |
| 운영 | Actuator (`/actuator/health`) |
| 상태 | 공통 기반(에러 응답·추적 ID·ECS 로깅·JPA Auditing·린트)만 있음, Bounded Context 없음 |

## 2. 런타임 구성
```
[Client] → HTTP :8080 → [app (Spring Boot)] → JDBC :3306 → [MySQL app-mysql]
```

## 3. DDD 구조

### 3.1 패키지 규칙
최상위는 **Bounded Context**로 나누고, 각 컨텍스트 안을 4개 계층으로 나눈다.

```
com.example.app
├── AppApplication
├── <context>/                    # Bounded Context 하나 (예: product)
│   ├── domain/                   # Aggregate(상태 변경 메서드·불변식), Entity, Value Object,
│   │                             # Domain Service, Repository 인터페이스, ErrorCode
│   ├── application/              # 유스케이스(Application Service), Command/Result DTO, 트랜잭션 경계
│   ├── infrastructure/           # Repository 구현(Spring Data JPA), 외부 API 어댑터
│   └── presentation/             # Controller, Request/Response DTO
└── common/                       # Shared Kernel (특정 컨텍스트에 속하지 않는 것만)
    ├── audit/                    # BaseTimeEntity(created_at·updated_at), BaseCreatedTimeEntity(created_at)
    ├── config/                   # ClockConfig(Clock 빈), JpaAuditingConfig(@EnableJpaAuditing)
    ├── error/                    # ErrorType, ErrorCode, CommonErrorCode, BusinessException (순수 Java)
    └── web/                      # GlobalExceptionHandler, TraceIdFilter, FieldViolation
```

### 3.2 의존 방향
```
presentation ──→ application ──→ domain ←── infrastructure
```
- `domain`은 다른 계층에 의존하지 않는다. Spring에는 JPA 매핑 어노테이션만 허용한다.
- `infrastructure`는 `domain`의 Repository 인터페이스를 구현한다(의존성 역전).
- 다른 Bounded Context는 상대의 `application` 계층을 통해서만 호출하고, 상대의 `domain`·`infrastructure`·`presentation`을 직접 쓰지 않는다. 참조는 ID로 한다.
- `common.error`는 모든 계층이 쓸 수 있다(Spring 의존 없음). `common.web`은 presentation 계층과 같은 위치로 보고, domain·application에서 쓰지 않는다.
- `domain`의 Entity는 `common.audit`의 상위 클래스를 상속할 수 있다. `common.config`는 domain·application에서 import하지 않는다.
- 위 의존 규칙은 두 단계로 검사한다. Checkstyle `ImportControl`(`config/checkstyle/import-control.xml`)이 import 문을, ArchUnit(`ArchitectureTest`)이 바이트코드의 실제 의존성을 본다. ArchUnit은 FQN 사용, 컨텍스트 간 참조, Entity 반환, 클래스 위치(`@Entity`→domain, Controller→presentation, `@Service`→application, Spring Data Repository→infrastructure), `@Transactional` 위치, domain의 public setter 금지까지 검사한다.
- 상태는 Aggregate Root의 도메인 메서드가 바꾸고, 트랜잭션 경계는 `application`의 `@Transactional`이 정한다. 단일 DB이므로 여러 Aggregate·컨텍스트에 걸친 변경도 한 트랜잭션으로 묶는다(Domain Event는 기본 구성이 아니다).

### 3.3 계층별 테스트
| 계층 | 테스트 종류 | Spring 컨텍스트 |
|---|---|---|
| `domain` | 순수 단위 테스트 | 없음 |
| `application` | 단위 테스트 (Repository는 가짜 구현) | 없음 |
| `infrastructure` | JPA 슬라이스 (`@AutoConfigureTestDatabase(replace = NONE)` + `@Import(TestcontainersConfig.class)`, Auditing이 필요하면 `JpaAuditingConfig`도 `@Import`). Flyway 마이그레이션으로 만든 스키마를 `validate`로 검증한다 | 일부 |
| `presentation` | Web MVC 슬라이스 (application 서비스는 mock) | 일부 |

## 4. Context Map
| Bounded Context | 패키지 | 책임 | 의존하는 컨텍스트 |
|---|---|---|---|
| (아직 없음) | | | |

## 5. 도메인 모델
아직 없음.
<!-- 컨텍스트별로: Aggregate Root / 내부 Entity / Value Object / 주요 불변식 -->

## 6. API
아직 없음.

### 6.1 에러 응답 (모든 API 공통)
RFC 9457 Problem Details(`application/problem+json`)로 통일한다. 형식과 예외 사용법은 [CONVENTIONS.md 4절](./CONVENTIONS.md#4-예외와-에러-응답).

| 상황 | 상태 코드 | `code` |
|---|---|---|
| `BusinessException` | `ErrorType`으로 정함: `INVALID_INPUT` 400, `UNAUTHENTICATED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `CONFLICT` 409, `INTERNAL` 500. `detail`은 던질 때 준 메시지(없으면 `ErrorCode` 기본 메시지)를 5xx라도 그대로 노출 | `ErrorCode.code()` |
| 입력 검증 실패 (`@Valid` 본문, 파라미터 제약) | 400 | `INVALID_INPUT` + `errors[]` |
| Spring MVC 표준 예외 | Spring 기본값 | 400 `INVALID_INPUT`, 404 `RESOURCE_NOT_FOUND`, 5xx `INTERNAL_ERROR`, 그 외 HTTP 상태 이름 (예: `METHOD_NOT_ALLOWED`) |
| 그 밖의 예외 | 500 | `INTERNAL_ERROR` (내부 메시지는 노출하지 않음) |

모든 에러 응답에 `instance`(요청 경로)와 `traceId`가 들어간다. 모든 응답에 `X-Request-Id` 헤더가 붙는다(요청에 안전한 형식의 값이 있으면 이어 씀).
<!-- 메서드 · 경로 · 담당 컨텍스트 · 설명 -->

## 7. 설정
| 파일 | 내용 |
|---|---|
| `.env.example` | 환경 변수 목록과 기본값. DB 접속(`DB_*`), 애플리케이션 시간대(`APP_TIME_ZONE`), 기준 부하 테스트(DB가 병목인 로컬 클러스터)로 SLO에 맞춘 튜닝 값(`TOMCAT_*`, `DB_POOL_*`, `DB_*_TIMEOUT`, `JPA_DEFAULT_BATCH_FETCH_SIZE`). yml에는 숫자 대신 이 변수를 쓴다 |
| `application.yml` | 공통: 기본 프로필 `local`, Hikari(auto-commit off, MySQL 드라이버 최적화 옵션, 드라이버 시간대 `connectionTimeZone` = `APP_TIME_ZONE`), JPA(`ddl-auto: validate`, `open-in-view: false`, 배치 fetch·쓰기 배치, IN 절 패딩, 컬렉션 fetch join 페이징 금지), Tomcat, Actuator(`health`만 노출), 콘솔 로그 ECS JSON(`trace.id` 포함), 애플리케이션 시간대 `app.time-zone` |
| `application-local.yml` | 로컬 개발: 변수를 `.env.example` → `.env` 순으로 읽음(뒤가 우선, OS 환경변수가 최우선), Actuator 노출 확대, SQL 로그 DEBUG |
| `src/test/resources/config/application.yml`, `application-test.yml`, `test.env` | 테스트: 테스트 클래스패스에서 기본 프로필을 `test`로 바꾸고, 변수는 `test.env`에서만 읽음(개인 `.env`·작업 디렉터리와 무관). DB 접속은 Testcontainers가 채움 |
| `application-prod.yml` | 운영(`SPRING_PROFILES_ACTIVE=prod`): 파일을 읽지 않고 `.env.example`의 변수를 모두 배포 환경변수로 받음(빠지면 시작 실패) |
| `db/migration/` | Flyway 마이그레이션(`V{n}__{설명}.sql`). 앱이 시작할 때 모든 환경에 적용한다. 아직 마이그레이션 없음 |
| `.github/workflows/ci.yml` | push(main)·PR마다 `./gradlew check` (테스트·ArchUnit·Spotless·Checkstyle, Testcontainers는 러너의 Docker 사용). 실패하면 `build/reports/`를 아티팩트로 올림 |
| `.github/pull_request_template.md`, `.github/ISSUE_TEMPLATE/` | PR 양식(개요·계획 링크·주요 변경·검증·리뷰 포인트), 이슈 양식 `bug.yml`(traceId 필수)·`task.yml`. 빈 이슈 금지. `pr-draft`·`issue-report` 스킬이 같은 양식을 채운다 |
| `.claude/settings.json` | Claude Code 공용 권한: `./gradlew`, `docker info`·`docker compose`, `git status`·`diff`·`log`, `gh` 조회(`pr view`·`pr list`·`pr checks`·`issue view`·`issue list`) 허용. push·PR·이슈 생성은 허용하지 않음(매번 확인). 개인 설정은 `.claude/settings.local.json`(커밋 금지) |
| `compose.yaml` | 로컬 MySQL 8.4, utf8mb4, 시간대 `APP_TIME_ZONE`(`.env`, 없으면 Asia/Seoul), 볼륨 `mysql-data`. 접속 값은 `.env`에서 읽음(없으면 `app`/3306) |
| `build.gradle.kts` | Spotless(palantir-java-format), Checkstyle. 둘 다 `check`에 포함 |
| `config/checkstyle/checkstyle.xml` | 계층 의존·금지 API·이름·복잡도·버그 패턴 규칙 (서식 규칙은 Spotless가 맡음). 목록은 CONVENTIONS.md 1절 |
| `config/checkstyle/import-control.xml` | 3.2절 의존 방향을 `src/main`에 강제하는 import 규칙 |
| `lombok.config` | `@Setter`·`@Data`·`@SneakyThrows`·`val`/`var`·experimental 사용 시 컴파일 오류 |
| `common.config` | `Clock` 빈(`app.time-zone` = `APP_TIME_ZONE` 시간대), JPA Auditing(`DateTimeProvider`가 `Clock`을 씀) |

## 8. 테스트 구성
| 종류 | 위치 | 비고 |
|---|---|---|
| 컨텍스트 로드, ECS 로그 형식, test 프로필 분리 | `AppApplicationTests` | Testcontainers |
| `Clock` 시간대 설정 | `common.config.ClockConfigTest` | `ApplicationContextRunner`, Docker 불필요 |
| 아키텍처 규칙 (ArchUnit) | `ArchitectureTest` | DDD 계층·컨텍스트 의존, 클래스 위치, 계층별 책임, 일반 코딩 규칙. 테스트 코드는 검사하지 않음 |
| 에러 모델 단위 | `common.error.BusinessExceptionTest` | |
| JPA Auditing (JPA 슬라이스) | `common.audit.JpaAuditingTest` | Testcontainers, 고정 시계 사용 |
| 테스트 전용 테이블 | `src/test/resources/db/migration/R__test_only_tables.sql` | 테스트 전용 Entity의 테이블. 테스트 클래스패스에서만 Flyway가 적용 (repeatable이라 `V{n}` 번호와 겹치지 않음) |
| DB 테스트 지원 | `support.TestcontainersConfig` | MySQL 8.4 컨테이너 하나를 모든 테스트 컨텍스트가 공유 (`@ServiceConnection`, 컨텍스트가 닫혀도 멈추지 않음), 시간대는 `test.env`의 `APP_TIME_ZONE`, Docker 필요 |
| 추적 ID 필터 단위 | `common.web.TraceIdFilterTest` | |
| 에러 응답 (Web MVC 슬라이스) | `common.web.GlobalExceptionHandlerTest` | 테스트 전용 컨트롤러 사용 |
