# 테스트

github/awesome-copilot의 `spring-boot-testing`, `java-testcontainers`를 Kotlin과 Gradle 기준으로 옮겼습니다.

## 어떤 테스트를 쓸지

| 대상 | 방식 |
|---|---|
| 서비스의 비즈니스 로직 | JUnit + MockK, Spring 컨텍스트 없음 |
| Controller와 HTTP 동작 | `@WebMvcTest` + `MockMvcTester` + `@MockkBean` |
| Repository 쿼리 | `@DataJpaTest` + Testcontainers |
| 외부 API 클라이언트 | `@RestClientTest` |
| JSON 매핑 | `@JsonTest` |
| 전체 흐름 | `@SpringBootTest`, 꼭 필요할 때만 |

가장 좁은 범위로 시작합니다. H2로 PostgreSQL을 대신하지 않습니다.

## Kotlin 주의점

- Mockito의 `any()`는 null을 반환해 non-null 파라미터에서 실패합니다. MockK와 `@MockkBean`(springmockk)을 씁니다. `@MockitoBean`과 섞지 않습니다.
- 테스트 이름은 백틱으로 시나리오를 적습니다: ``fun `재고가 없으면 주문을 거절한다`()``.
- 순서는 정상 경로, 다른 경로와 경계값, 오류 순으로 둡니다.

```kotlin
@WebMvcTest(OrderController::class)
class OrderControllerTest(
    @Autowired private val mvc: MockMvcTester,
) {
    @MockkBean
    private lateinit var orderService: OrderService

    @Test
    fun `주문을 조회한다`() {
        every { orderService.find(1L) } returns OrderResponse(id = 1L)

        assertThat(mvc.get().uri("/orders/1")).hasStatusOk()
    }
}
```

## Testcontainers

DB가 필요한 테스트에는 `@Testcontainers(disabledWithoutDocker = true)`와 `@Import(TestcontainersConfiguration::class)`를 붙입니다. Docker가 없으면 건너뛰므로, 결과를 보고할 때 skipped 수를 함께 확인합니다.

`@DataJpaTest`는 기본으로 내장 DB로 교체하려 하므로 `@AutoConfigureTestDatabase(replace = Replace.NONE)`을 함께 씁니다.
