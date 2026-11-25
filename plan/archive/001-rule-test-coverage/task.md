# 001. 기존 규칙 테스트 보강 — task

> [plan.md](./plan.md)를 실행 단위 작업으로 쪼개 진행 상태를 관리한다.
> 표기: `[ ]` 할 일 · `[~]` 진행 중 · `[x]` 완료 · `[-]` 취소(이유 기재)

## Phase 1. ArchUnit 규칙 검증
완료 기준: `ArchitectureTest`의 직접 정의 규칙 13개가 모두 위반 fixture에서 실패하고 정상 fixture(검사 대상 0개 아님)에서 통과한다. `ArchitectureTest`는 기존과 같은 규칙 이름으로 계속 통과한다.

- [x] 규칙을 `ArchitectureRules` 팩터리로 옮기고 의존 규칙 5개 검증: `LAYER_DEPENDENCIES`, `DOMAIN_IS_FRAMEWORK_FREE`, `COMMON_ERROR_IS_FRAMEWORK_FREE`, `COMMON_DOES_NOT_DEPEND_ON_CONTEXTS`, `CONTEXTS_TALK_THROUGH_APPLICATION`
  > 메모: 2026-11-25 테스트 ArchitectureRulesTest 10개 추가(fixture `archfixture/**` 16개), 전체 44/44 통과
- [x] 위치·책임 규칙 8개 검증 (+ 정상 fixture에 domain → `common.audit` 상속 사례 추가): `ENTITIES_IN_DOMAIN`, `CONTROLLERS_IN_PRESENTATION`, `SERVICES_IN_APPLICATION`, `SPRING_DATA_REPOSITORIES_IN_INFRASTRUCTURE`, `TRANSACTIONAL_CLASSES_ONLY_IN_APPLICATION`, `TRANSACTIONAL_METHODS_ONLY_IN_APPLICATION`, `CONTROLLERS_DO_NOT_RETURN_ENTITIES`, `DOMAIN_HAS_NO_PUBLIC_SETTERS`
  > 메모: 2026-11-25 ArchitectureRulesTest 27개(정상 13·위반 14, 한 형식으로 통일), 직접 정의 규칙 13개 모두 ArchitectureRules로 이동, 전체 61/61 통과

## Phase 2. Checkstyle 직접 정의 규칙 검증
완료 기준: `config/checkstyle/checkstyle.xml`을 그대로 읽은 검사기가 위반 fixture마다 기대한 위반을 보고하고, 정상 fixture에서는 위반 0개다.

- [x] Checkstyle 설정 검증 테스트: import-control 계층 규칙(domain→Spring, application→presentation, presentation→Repository, domain→`common.config`, `common.error`→Spring), 인자 없는 `now()`, 필드 `@Autowired`, `System.out`, `javax.persistence`
  > 메모: 2026-11-25 테스트 CheckstyleConfigTest 12개(위반 9·정상 1·테스트 경로 예외 2) + CheckstyleRunner, fixture 10개, 전체 73/73 통과

## Phase 3. 기존 동작 특성 테스트
완료 기준: ARCHITECTURE.md 6.1절 표의 모든 행과 `TraceIdFilter`의 형식 경계가 테스트로 있고, 각 테스트가 변이 확인으로 실제 동작을 검증함이 확인됐다.

- [x] `GlobalExceptionHandler`: `ErrorType` 6종 → 상태 코드, 없는 경로 404 `RESOURCE_NOT_FOUND`, 깨진 JSON 400 `INVALID_INPUT`, 에러 응답의 `X-Request-Id` 헤더 / `TraceIdFilter`: 64자 허용·65자 거부
  > 메모: 2026-11-25 GlobalExceptionHandlerTest +10(ErrorType 6종 @EnumSource, 404, 깨진 JSON, X-Request-Id 2), TraceIdFilterTest +2(64·65자). 변이 13개로 검증, 버그 없어 Green 생략, 전체 85/85 통과

> 최종 검증: 2026-11-25 ./gradlew check 통과, 테스트 85/85 (ArchitectureTest 17, ArchitectureRulesTest 27 실행 확인)
