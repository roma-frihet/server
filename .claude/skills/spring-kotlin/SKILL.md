---
name: spring-kotlin
description: Spring Boot 4 + Kotlin + JPA 코드를 작성하거나 리뷰할 때 사용합니다. 엔티티, DTO와 검증, 트랜잭션, 코루틴, Flyway 마이그레이션, 설정 작업에 적용합니다. 테스트를 작성할 때는 references/testing.md를 함께 읽습니다.
---

# Spring Boot + Kotlin

github/awesome-copilot의 `kotlin-springboot`, `java-hibernate-jpa`를 참고해 Kotlin 기준으로 바로잡아 다시 쓴 규칙입니다.

## 의존성 주입과 설정

- 주 생성자의 `private val`로 주입합니다. `@Autowired lateinit var`는 쓰지 않습니다.
- 설정은 `@ConfigurationProperties`와 `data class`로 바인딩합니다.
- 비밀값은 환경 변수로 받습니다. `application*.yml`에는 `${ENV_VAR}`만 둡니다.

## 컴파일러 플러그인

프로젝트에 이미 설정되어 있습니다. 동작을 알고 지우지 않도록 역할을 적어 둡니다.

- `kotlin("plugin.spring")`: `@Component`, `@Transactional` 등이 붙은 클래스를 `open`으로 만듭니다. 없으면 프록시가 동작하지 않습니다.
- `kotlin("plugin.jpa")`: 엔티티에 인자 없는 생성자를 만듭니다. `open`으로 만들지는 않습니다.
- `allOpen { annotation("jakarta.persistence.Entity") ... }`: 엔티티를 `open`으로 만듭니다. 없으면 지연 로딩 프록시를 만들 수 없습니다.

## 엔티티

`data class`는 쓰지 않습니다. 생성된 `equals`, `hashCode`, `toString`이 연관관계를 건드려 지연 로딩이나 무한 재귀를 일으킵니다. 프로퍼티는 `protected set`으로 막고 상태 변경은 메서드로 표현합니다. `allOpen`으로 프로퍼티도 `open`이 되므로 `private set`은 컴파일되지 않습니다.

```kotlin
@Entity
@Table(name = "orders")
class Order(
    customerId: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 50)
    val id: Long? = null

    var customerId: Long = customerId
        protected set

    override fun equals(other: Any?): Boolean =
        this === other || (other is Order && id != null && id == other.id)

    override fun hashCode(): Int = Order::class.hashCode()
}
```

- `@ManyToOne`, `@OneToOne`은 `fetch = FetchType.LAZY`를 명시합니다. 즉시 로딩이 필요하면 쿼리별로 `JOIN FETCH`나 엔티티 그래프를 씁니다.
- 컬렉션 `JOIN FETCH`와 페이지네이션을 함께 쓰지 않습니다.
- `CascadeType.REMOVE`와 `ALL`은 `@ManyToOne`, `@ManyToMany`에 쓰지 않습니다.
- ID는 `SEQUENCE`를 기본으로 합니다. `allocationSize`는 DB 시퀀스의 `INCREMENT BY`와 맞춥니다.

## DTO와 검증

- 요청과 응답은 `data class`로 분리합니다. 엔티티를 API로 노출하지 않습니다.
- 필수 필드를 non-null 타입으로 두면 값이 없을 때 검증 전에 Jackson이 실패해서, `@NotNull` 메시지 대신 역직렬화 오류가 납니다. 필드별 검증 메시지가 필요하면 nullable 타입에 `@field:NotNull`을 씁니다.
- 프로젝트는 `-Xannotation-default-target=param-property` 설정으로 `@field:` 없이도 필드에 적용되지만, 이 옵션이 없는 환경으로 코드를 옮길 때는 명시해야 합니다.

```kotlin
data class CreateOrderRequest(
    @field:NotNull
    @field:Positive
    val customerId: Long?,
)
```

## 트랜잭션과 코루틴

- `@Transactional`은 서비스 경계에 두고 짧게 유지합니다. 트랜잭션 안에서 외부 API를 호출하지 않습니다.
- JPA 트랜잭션은 스레드에 묶이므로 코루틴을 따라가지 않습니다. JPA 코드는 `suspend`로 만들지 않습니다.
- 코루틴이 꼭 필요하면 스택 전체를 WebFlux와 R2DBC로 바꾸는 결정이므로, 먼저 사용자에게 묻습니다.

## 마이그레이션

- `ddl-auto`는 `validate`로 유지합니다.
- 이미 만든 마이그레이션은 고치지 않고 새 버전을 추가합니다.

## 로깅과 오류 처리

- 로거는 `LoggerFactory.getLogger(javaClass)`를 `private val`로 둡니다. 메시지는 `logger.info("Order {} created", id)`처럼 파라미터로 넘깁니다.
- 공통 오류 응답은 `@RestControllerAdvice`와 `ProblemDetail`로 처리합니다.
