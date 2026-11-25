# 001. 기존 규칙 테스트 보강 — plan

> **무엇을, 왜** 만드는지 다룬다. 작업 항목과 진행 상태는 [task.md](./task.md)에서 관리한다.

| 항목 | 값 |
|---|---|
| 상태 | 완료 <!-- 진행 중 / 보류 / 완료 --> |
| 시작일 | 2026-11-25 |
| 완료일 | 2026-11-25 |
| 관련 계획 | 없음 |

## 1. 목표
템플릿이 강제한다고 문서에 적은 규칙이 **실제로 위반을 잡는지** 테스트로 보장한다. 지금은 Bounded Context가 없어 ArchUnit 규칙의 검사 대상이 0개이고(`failOnEmptyShould=false`), Checkstyle의 직접 정의 규칙도 위반 코드로 검증된 적이 없어서, 패턴 오타가 있어도 모든 규칙이 "통과"한다.

## 2. 범위
**포함**
- ArchUnit 규칙(`ArchitectureTest`) 13개: 위반 fixture에서 실패하고, 규칙을 지키는 fixture에서 통과하는지 검증
- Checkstyle 직접 정의 규칙: `import-control.xml`의 계층 의존 규칙, 인자 없는 `now()` 금지, 필드 `@Autowired` 금지, `System.out`, `javax.*` 금지
- `GlobalExceptionHandler`에서 빠진 동작: `ErrorType` 6종 → 상태 코드, 없는 경로 404 `RESOURCE_NOT_FOUND`, 깨진 JSON 400 `INVALID_INPUT`, 에러 응답의 `X-Request-Id` 헤더
- `TraceIdFilter` 길이 상한(64자) 경계

**제외**
- ArchUnit 라이브러리 규칙 4개(`GeneralCodingRules`): ArchUnit이 제공하고 검증하는 규칙이라 다시 검증하지 않는다
- `lombok.config` 금지 어노테이션: Lombok 표준 기능(`flagUsage`)이라 설정 값만 맞으면 동작한다
- Checkstyle 표준 규칙(이름, 복잡도, 줄 길이 등): Checkstyle 자체 기능
- prod 프로필에서 환경변수가 빠졌을 때의 시작 실패: 전체 부팅이 필요하고 Spring 기본 동작이다
- `ClockConfig`: `Clock.systemDefaultZone()`을 반환하는 것뿐이라 테스트할 로직이 없다

## 3. 설계

### 3.1 ArchUnit 규칙을 fixture로 검증
- `ArchitectureTest`의 규칙 정의를 테스트 소스의 `com.example.app.ArchitectureRules`로 옮기고, **기준 패키지를 인자로 받는 정적 팩터리**(`static ArchRule layerDependencies(String base)` 등)로 만든다. `ArchitectureTest`는 `ArchitectureRules.xxx(BASE)`로 앱 코드에 적용한다(필드 이름·`as(...)` 설명은 그대로 유지).
- fixture는 앱 기준 패키지 **밖**의 테스트 소스 `archfixture` 패키지에 둔다. Spring 컴포넌트 스캔과 JPA 엔티티 스캔(`com.example.app` 하위)이 보지 않으므로 fixture에 `@Entity`, `@Service`, `@RestController`를 붙여도 다른 테스트에 영향이 없다.
  ```
  src/test/java/archfixture/
  ├── valid/<context>/{domain,application,infrastructure,presentation}/   # 모든 규칙을 지키는 예
  └── <위반 사례>/<context>/...                                              # 사례마다 기준 패키지 하나
  ```
- 검증 테스트 `ArchitectureRulesTest`:
  - 위반 사례: `ArchitectureRules.xxx("archfixture.<사례>")`를 그 사례의 클래스에 적용하면 실패한다(`AssertionError`).
  - 정상 사례: 모든 규칙을 `archfixture.valid`에 적용하면 통과한다. 이때 규칙에 `allowEmptyShould(false)`를 걸어 검사 대상이 0개가 아님을 함께 보장한다.

### 3.2 Checkstyle 설정 검증
- 테스트 의존성에 `com.puppycrawl.tools:checkstyle`(Gradle `checkstyle.toolVersion`과 같은 버전)를 추가한다.
- `config/checkstyle/checkstyle.xml`을 그대로 읽어 `Checker`로 fixture 소스(`src/test/resources/checkstyle-fixture/`, 컴파일하지 않음)를 검사하고, 기대한 규칙 위반이 나오는지 확인한다. 정상 fixture는 위반 0개.

### 3.3 기존 동작 특성 테스트
- `GlobalExceptionHandlerTest`, `TraceIdFilterTest`에 빠진 경우를 추가한다. 이미 구현된 동작이므로 테스트가 처음부터 통과할 수 있다(아래 결정 기록 참고).

## 4. 결정 기록
| 날짜 | 결정 | 이유 |
|---|---|---|
| 2026-11-25 | ArchUnit 규칙을 기준 패키지를 받는 팩터리로 바꾸고 fixture를 `archfixture`(앱 패키지 밖)에 둔다 | 규칙이 `com.example.app.*.domain..` 패턴에 묶여 있어 fixture를 앱 패키지 안에 두면 Spring·JPA 스캔에 잡혀 다른 테스트(Flyway validate 등)를 깨뜨린다 |
| 2026-11-25 | 이미 구현된 동작의 특성 테스트는 Red에서 "실패 확인" 대신 **변이 확인**을 한다: 프로덕션 코드를 잠시 바꿔 테스트가 실패하는지 본 뒤 되돌린다. Green은 테스트가 버그를 드러낸 경우에만 코드를 고친다 | 동작이 이미 있어 정상적인 Red 실패가 나오지 않는다. 변이 확인으로 테스트가 실제로 그 동작을 검증하는지 보장한다 |
| 2026-11-25 | `BusinessException`은 공통 하나로 두고 컨텍스트 구분은 `ErrorCode` enum으로 한다. 컨텍스트별 하위 예외는 템플릿 사용자에게 맡긴다 | 상속하면 `GlobalExceptionHandler`가 그대로 처리하므로 템플릿이 정할 필요가 없다 |

| 2026-11-25 | 규칙을 옮길 때 패키지 패턴과 `.as(...)` 설명은 그대로 두고 `BASE` 자리만 `base` 인자로 바꾼다. `ArchitectureTest`는 같은 필드 이름으로 팩터리를 호출한다 | 앱에 적용되는 규칙과 fixture로 검증되는 규칙이 같은 코드임을 보장하고, 테스트 리포트의 규칙 이름을 유지한다 |
| 2026-11-25 | Checkstyle 버전을 `checkstyleVersion` 하나로 관리하고, 테스트 의존성에서 `slf4j-simple`을 뺀다 | Gradle 플러그인과 테스트의 검사기가 같은 버전이어야 검증이 의미 있다. checkstyle이 끌고 오는 slf4j-simple이 Logback과 충돌해 Spring 컨텍스트 테스트를 깨뜨렸다 |
| 2026-11-25 | Checkstyle fixture는 `@TempDir`의 `src/main/java`·`src/test/java` 경로로 복사해 검사한다 | 설정이 파일 경로로 규칙을 켜고 끄므로(import-control은 src/main만, now()·필드 주입은 src/test에서 해제) 실제 위치와 같은 경로에서 검사해야 한다 |
| 2026-11-25 | (완료 후) `ArchitectureRules`·`ArchitectureRulesTest`·`archfixture`와 `CheckstyleConfigTest`·`CheckstyleRunner`·fixture를 제거하고 규칙을 `ArchitectureTest` 한 파일로 되돌린다. `GlobalExceptionHandler`·`TraceIdFilter` 테스트는 유지 | 도구(ArchUnit·Checkstyle) 설정을 도구로 다시 검증하는 비용(fixture 40여 개, 테스트 의존성·slf4j 충돌 우회)이 템플릿에 비해 크다. 규칙은 실제 Bounded Context 코드에 대해 `ArchitectureTest`·`check`가 검사한다 |

## 5. 열린 질문
- 없음

## 6. 회고 (완료 시 작성)
- 잘된 점: 규칙을 기준 패키지를 받는 팩터리로 바꿔, 앱에 적용하는 규칙과 fixture로 검증하는 규칙이 같은 코드가 됐다. 정상 fixture에 `allowEmptyShould(false)`를 걸어 "검사 대상 0개라 통과"를 막았다. Checkstyle이 경로로 규칙을 켜고 끄는 점을 Red 전에 파악해 무의미한 테스트를 피했다. 특성 테스트는 변이 13개로 실제 검증 여부를 확인했다.
- 아쉬운 점: Checkstyle 테스트 의존성이 slf4j-simple을 끌고 와 Spring 컨텍스트 테스트 11개를 깨뜨린 것을 Green 단계에서야 발견했다. 새 테스트 라이브러리를 넣을 때는 전이 의존성(특히 로깅 바인딩)을 먼저 확인한다. 특성 테스트 사이클은 일반 Red→Green 흐름에 맞지 않아 Green을 건너뛰었다.
- 다음 계획으로 넘길 것: `TraceIdFilterTest`의 헤더 테스트 4개에 반복되는 요청 생성 코드 정리(선택). 첫 Bounded Context를 추가할 때 `archunit.properties`의 `failOnEmptyShould=false` 제거 검토.
