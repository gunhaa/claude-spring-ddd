# CONVENTIONS.md

코드를 쓸 때 지키는 약속이다. 도구가 검사하는 것은 **[자동]**으로 표시했다. 구조와 계층 규칙은 [AGENTS.md 6절](./AGENTS.md#6-ddd-규칙)과 [ARCHITECTURE.md](./ARCHITECTURE.md)에 있다.

## 1. 서식과 린트
| 도구 | 하는 일 | 명령 |
|---|---|---|
| Spotless (palantir-java-format) | 들여쓰기·줄바꿈·import 정렬 **[자동]** | `./gradlew spotlessApply` |
| Checkstyle (`config/checkstyle/checkstyle.xml`) | 계층 의존, 금지 API, 이름, 복잡도, 버그 패턴 **[자동]** | `./gradlew checkstyleMain checkstyleTest` |
| `config/checkstyle/import-control.xml` | DDD 계층 의존 규칙 (Checkstyle `ImportControl`, `src/main`만) **[자동]** | 위와 같음 |
| `lombok.config` | 금지된 Lombok 어노테이션을 컴파일 오류로 만든다 **[자동]** | `./gradlew compileJava` |
| ArchUnit (`src/test/java/.../ArchitectureTest.java`) | DDD 계층·컨텍스트 의존과 클래스 위치를 바이트코드로 검사 **[자동]** | `./gradlew test --tests '*ArchitectureTest'` |
| `.editorconfig` | 에디터 기본 설정 (UTF-8, LF, 공백 4칸, yml 2칸) | - |

- 커밋 전에 `./gradlew spotlessApply check`를 실행한다. `check`에 테스트·Spotless·Checkstyle이 모두 포함된다.
- 규칙을 꺼야 하면 그 자리에만 `@SuppressWarnings("checkstyle:<규칙명>")`을 쓰고 이유를 주석으로 남긴다.

### Checkstyle이 막는 것
| 분류 | 규칙 |
|---|---|
| 계층 의존 | `domain` → 다른 계층·Spring·`jakarta.servlet`·`jakarta.transaction` 금지 (JPA 매핑만 허용)<br>`application` → `infrastructure`·`presentation`·`org.springframework.web`·`org.springframework.http` 금지<br>`infrastructure` → `presentation`·웹·`@Transactional` 금지<br>`presentation` → `infrastructure`·`*Repository`·Spring Data·`@Transactional` 금지<br>`domain`·`application` → `common.config` 금지<br>`common.error` → Spring·웹 금지 |
| 금지 API | JUnit 4(`org.junit.Test` 등) → JUnit 5 / `javax.persistence`·`javax.validation`·`javax.servlet` → `jakarta.*` / `java.util.Date`·`Calendar`·`SimpleDateFormat` → `java.time` / `jakarta.transaction.Transactional` → Spring `@Transactional` / `System.out`·`System.err`·`printStackTrace()` → 로거 / 필드 주입 `@Autowired` → 생성자 주입 (테스트는 허용) / `new Boolean()` 등 래퍼 생성자 |
| 이름 | 패키지 소문자, 클래스 `PascalCase`, 메서드·변수 `camelCase` (테스트 메서드는 `_` 허용), 상수 `UPPER_SNAKE_CASE`, 약어도 단어처럼 (`HttpClient`) |
| 클래스 설계 | public 필드 금지, 유틸리티 클래스는 private 생성자, 예외 클래스 필드는 `final`, 상수 전용 인터페이스 금지, 오버로드 메서드는 붙여서 선언, 불필요한 기본값 초기화(`= null`, `= 0`) 금지 |
| 버그 패턴 | `equals`/`hashCode` 짝, 문자열 `==` 비교, `"상수".equals(x)` 순서, 조건문 안 대입, 파라미터 재대입, 반복 변수 수정, `switch` fall-through·`default` 누락, 필드를 가리는 지역 변수, 안 쓰는 지역 변수, 빈 블록·빈 `catch` |
| 시간 | `src/main`에서 인자 없는 `LocalDateTime.now()` 등 금지 → `now(clock)` |
| 크기·복잡도 | 줄 120자, 파일 500줄, 메서드 60줄(빈 줄 제외), 파라미터 7개, 순환 복잡도 10, `if` 중첩 2단계, `for` 중첩 2단계, `try` 중첩 1단계, 불리언 연산자 3개 |

## 2. 이름
| 대상 | 규칙 | 위치 |
|---|---|---|
| Request / Response DTO | `XxxRequest`, `XxxResponse` (`record`) | `presentation` |
| Command / 결과 DTO | `XxxCommand`, `XxxResult` (`record`) | `application` |
| Application Service | `XxxService` | `application` |
| Repository | 인터페이스 `XxxRepository` / 구현 `JpaXxxRepository` | `domain` / `infrastructure` |
| Controller | `XxxController` | `presentation` |
| 오류 코드 | `XxxErrorCode` enum, 상수는 `UPPER_SNAKE_CASE` (`PRODUCT_NOT_FOUND`) | `domain` |

- 약어도 단어처럼 쓴다: `HttpClient`, `userId` (`HTTPClient`, `userID` 아님). **[자동]**
- 상태 변경 메서드는 의도를 이름에 드러낸다: `changeName()`, `cancel()` (`setName()`, `update()` 아님).

## 3. API
- 경로: `/api/v1/{복수형-kebab-case}` (예: `/api/v1/order-items/{id}`).
- 성공 응답은 Response DTO를 그대로 본문에 담는다. `ApiResponse<T>` 같은 공통 래퍼는 쓰지 않는다.
- 상태 코드: 생성 `201` + `Location` 헤더, 본문 없는 성공 `204`, 그 밖의 성공 `200`.
- 요청 검증은 Request DTO에 Bean Validation(`@NotBlank` 등)을 붙이고 Controller에서 `@Valid`로 받는다.

## 4. 예외와 에러 응답
에러 응답은 모두 RFC 9457 Problem Details(`application/problem+json`)이고, `common.web.GlobalExceptionHandler`가 만든다.

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "상품을 찾을 수 없습니다.",
  "instance": "/api/v1/products/1",
  "code": "PRODUCT_NOT_FOUND",
  "traceId": "3f2a9c...",
  "errors": [{ "field": "name", "message": "공백일 수 없습니다" }]
}
```
`errors`는 입력 검증 실패일 때만 들어간다.

- 비즈니스 규칙 위반은 `BusinessException`에 컨텍스트의 `ErrorCode`를 담아 던진다.
  ```java
  throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND);
  throw new BusinessException(ProductErrorCode.PRODUCT_NOT_FOUND, "상품 %d을 찾을 수 없습니다.".formatted(id));
  ```
- `ErrorCode`는 `ErrorType`(`INVALID_INPUT`, `NOT_FOUND`, `CONFLICT` 등)만 정하고 HTTP 상태 코드는 모른다. 변환은 `GlobalExceptionHandler`가 한다.
- `detail`은 클라이언트에 그대로 노출된다. SQL, 스택 정보, 내부 ID 규칙 같은 내부 정보를 넣지 않는다.
- Controller·Service에서 `try-catch`로 에러 응답을 직접 만들지 않는다.
- 클라이언트는 `code`로 분기하고, `detail`은 사람이 읽는 메시지로만 쓴다.

## 5. Lombok
| 어노테이션 | 규칙 |
|---|---|
| `@Setter`, `@Data` | 금지 **[자동]**: 상태 변경은 의도가 드러나는 메서드로만 한다 |
| `@SneakyThrows`, `val`/`var`, `experimental` | 금지 **[자동]** |
| `@Getter` | 허용 |
| `@NoArgsConstructor(access = AccessLevel.PROTECTED)` | JPA Entity에 붙인다 |
| `@RequiredArgsConstructor` | 생성자 주입에 쓴다 |
| `@Builder` | 불변식 검사가 필요한 Entity에는 쓰지 않는다. 정적 팩터리 메서드(`Product.create(...)`)로 만든다 |
| `@Slf4j` | 로거 선언에 쓴다 |

DTO는 Lombok 대신 `record`를 쓴다.

## 6. 로깅
- 로그는 ECS JSON으로 출력되고, 요청마다 `trace.id`가 붙는다(`TraceIdFilter`). 에러 응답의 `traceId`와 같은 값이다.
- 메시지는 `{}` 자리표시자를 쓴다: `log.info("주문 생성: orderId={}", orderId)`. 문자열 연결은 쓰지 않는다.
- 레벨: 예상한 실패(4xx)는 `WARN`, 예상하지 못한 실패(5xx)는 `ERROR`와 예외 객체, 흐름 추적은 `DEBUG`. 예외 로그는 `GlobalExceptionHandler`가 남기므로 던지는 쪽에서 또 남기지 않는다.
- 개인정보(이름, 연락처, 주소), 비밀번호, 토큰은 남기지 않는다.

## 7. 테스트
- 메서드 이름: `메서드_상황_결과` (영어, 예: `cancel_whenAlreadyShipped_throwsException`).
- `@DisplayName`: 기대 동작을 한국어 문장으로 적는다.
- 본문은 given / when / then 순서로 빈 줄을 두어 나눈다.
- 계층별 테스트 종류는 [ARCHITECTURE.md 3.3절](./ARCHITECTURE.md#33-계층별-테스트)을 따른다.

## 8. 커밋
- [Conventional Commits](https://www.conventionalcommits.org/) 형식, 제목은 한국어: `feat: 상품 조회 API 추가`.
- 타입: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `build`.
- 한 커밋에는 한 가지 변경만 담는다. 서식 정리만 하는 변경은 따로 커밋한다.

## 9. Bean 등록
| 방식 | 쓰는 경우 |
|---|---|
| `@Component` 계열 (`@RestController`, `@Service`, `@Repository`, `@Component`) | 이 프로젝트에서 만든 클래스이고 구현이 하나일 때. Controller, Application Service, Repository 구현, Filter, ControllerAdvice |
| `@Configuration` + `@Bean` | ① 외부 라이브러리·JDK 객체 (`Clock`, `RestClient`, 외부 API 클라이언트) ② `@Enable*` 설정 (`@EnableJpaAuditing`) ③ 구현이 여러 개라 환경·설정으로 골라야 할 때 (`@Profile`, `@ConditionalOnProperty`) |

- 구현이 하나일 때는 `@Component`로 시작하고, 구현이 둘 이상이 되어 선택이 필요해지면 그 빈만 `@Configuration`으로 옮긴다.
- 의존성은 생성자로 주입한다(`@RequiredArgsConstructor` + `private final`). **[자동]**
- 설정 클래스는 `XxxConfig`로 짓고 `@Configuration(proxyBeanMethods = false)`를 붙인다. 공통 설정은 `common.config`, 한 컨텍스트에만 필요한 설정은 그 컨텍스트의 `infrastructure`에 둔다.
- 슬라이스 테스트(`@WebMvcTest`, `@DataJpaTest`)는 `@Configuration` 클래스를 스캔하지 않는다. 테스트에 필요한 설정은 `@Import`로 가져온다(예: JPA 슬라이스의 `JpaAuditingConfig`).

## 10. Entity와 시간
- Aggregate Root와 Entity는 `common.audit`의 상위 클래스를 상속해 생성·수정 시각을 자동으로 기록한다.
  | 상위 클래스 | 컬럼 | 쓰는 곳 |
  |---|---|---|
  | `BaseTimeEntity` | `created_at`, `updated_at` | 값이 바뀌는 Entity (기본) |
  | `BaseCreatedTimeEntity` | `created_at` | 저장 후 바뀌지 않는 기록 (이력, 로그성 데이터) |
- 시각 타입은 `LocalDateTime`이고, 기준 시간대는 `app.time-zone`(환경 변수 `APP_TIME_ZONE`, 기본 `Asia/Seoul`)이다. JVM 기본 시간대에 기대지 않는다.
- 현재 시각은 `Clock` 빈으로만 얻는다: `LocalDateTime.now(clock)`. 인자 없는 `now()`는 `src/main`에서 금지한다. **[자동]** 테스트에서 시계를 고정해 시각을 검증하기 위해서다.

## 11. 설정 값
- 환경마다 다른 값(접속 정보)과 부하 테스트로 정한 튜닝 값은 yml에 숫자로 쓰지 않는다. `${변수}`로 받고 `.env.example`에 변수와 기본값, 의미를 적는다.
- 그 밖의 값(기능 on/off, 고정 정책)은 yml에 그대로 쓴다.
- 비밀 값(운영 비밀번호, 토큰)은 `.env.example`에 실제 값을 넣지 않는다. `.env`와 `.env.*`는 커밋하지 않는다(`.env.example` 제외).
- 변수를 추가하면 `.env.example`과 테스트용 `src/test/resources/test.env`에 함께 추가한다. prod는 `.env.example`에 있는 변수를 모두 배포 환경변수로 받아야 시작하고, 테스트는 `test.env`만 읽는다.

## 12. DB 스키마 (Flyway)
- 스키마는 `src/main/resources/db/migration`의 Flyway 마이그레이션으로만 바꾼다. 모든 환경이 `ddl-auto: validate`라서 Entity와 테이블이 다르면 앱과 테스트가 시작하지 않는다. **[자동]**
- 파일 이름: `V{n}__{설명}.sql`, 설명은 snake_case 영어 (예: `V1__create_product.sql`, `V2__add_product_price.sql`). 번호는 기존 최대 번호 + 1.
- **이미 커밋한 마이그레이션은 고치지 않는다.** 바꿀 게 있으면 새 버전을 추가한다. Flyway가 체크섬 불일치로 시작을 막는다.
- Entity를 추가·변경하는 커밋에 마이그레이션도 함께 넣는다. 테이블·컬럼 이름은 snake_case, 시각 컬럼은 `DATETIME(6)` (`BaseTimeEntity`의 `created_at`·`updated_at` 포함).
- 테스트 전용 Entity의 테이블은 `src/test/resources/db/migration/R__test_only_tables.sql`에 추가한다. 운영 jar에 들어가지 않는다.
- 로컬 DB가 마이그레이션과 어긋나면(예: 예전 `ddl-auto: update`로 만든 테이블) `docker compose down -v`로 볼륨을 지우고 다시 띄운다.

## 13. 상태 변경과 트랜잭션
상태는 Aggregate Root의 도메인 메서드가 바꾸고, 트랜잭션은 Application Service가 정한다(근거는 [AGENTS.md 6절](./AGENTS.md#6-ddd-규칙)).

```java
// domain: 상태 전이 규칙과 변경을 한곳에
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseTimeEntity {

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    public void cancel() {
        if (status == OrderStatus.SHIPPED) {
            throw new BusinessException(OrderErrorCode.ORDER_ALREADY_SHIPPED);
        }
        this.status = OrderStatus.CANCELED;
    }
}

// application: 트랜잭션 경계 + 조회 → 도메인 메서드 호출
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(OrderErrorCode.ORDER_NOT_FOUND));
        order.cancel(); // dirty checking으로 커밋 시 UPDATE. save() 불필요
    }
}
```

- 메서드 이름은 상태 전이의 의도로 짓는다: `cancel()`, `ship()`, `approve()`. `changeStatus(OrderStatus)`처럼 아무 상태로나 바꿀 수 있는 메서드는 전이 규칙을 숨기므로 쓰지 않는다. 꼭 필요하면 메서드 안에서 허용된 전이인지 검사한다.
- 상태는 `enum` + `@Enumerated(EnumType.STRING)`으로 저장한다(`ORDINAL`은 상수 순서가 바뀌면 데이터가 깨진다).
- Application Service는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`을 붙인다.
- `save()`는 새 Aggregate를 저장할 때만 부른다. 조회한 Aggregate의 변경은 트랜잭션이 끝날 때 반영된다.
- 상태 전이 규칙은 `domain` 단위 테스트(Spring 없음)로 검증하고, Service 테스트는 조회·호출 흐름만 검증한다.
- 금지: domain의 public `setXxx()` **[자동]**, Service에서 상태를 읽고 `if`로 전이를 판단하는 코드.
