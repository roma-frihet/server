# 컨벤션 참고 규칙

[CLAUDE.md](../../CLAUDE.md)는 항상 지키는 핵심 규칙만 담습니다. 이 문서는 해당 상황이 왔을 때 찾아보는 세부 규칙입니다. 두 문서가 어긋나면 `CLAUDE.md`를 따릅니다.

## 아직 도입하지 않은 기술

해당 기술을 프로젝트에 도입할 때 적용합니다.

### 코루틴

- 비동기 fire-and-forget은 주입받은 `applicationScope.launch`(SupervisorJob, 종료 시 cancel)로 실행합니다.
- `GlobalScope`를 쓰지 않고, 트랜잭션 안에서 `launch`하지 않습니다.
- 프로덕션 코드에서 `runBlocking`을 쓰지 않습니다. 테스트는 `runTest`를 씁니다.

### Spring AI / LLM

- 모델명, temperature 등은 `application.yml`에서 관리합니다. 코드에 하드코딩하거나 API Key를 커밋하지 않습니다.
- LLM 호출은 비동기(`applicationScope.async`) 또는 스트리밍으로 하고 타임아웃을 명시합니다.
- 요청 스레드에서 오래 블로킹하거나 `@Transactional` 안에서 호출하지 않습니다.
- RAG를 쓰면 `topK`와 유사도 임계값을 명시합니다.
- 프롬프트는 상수나 리소스 파일로 분리하고, 사용자 입력은 템플릿 파라미터로 주입합니다.

### Kafka

`CLAUDE.md`의 이벤트 규칙은 Spring Application Event 범위입니다. Kafka로 나가는 흐름에는 아래를 적용합니다.

- 흐름은 Service `publishEvent` → `common/listener/` Kafka 리스너(AFTER_COMMIT) → `KafkaMessageProducer` → Topic → `KafkaMessageConsumer` → 실패 시 `{topic}.DLT`입니다.
- Service가 `KafkaTemplate`을 직접 쓰지 않습니다.
- 토픽 이름은 `{service}.{domain}.{event}`이고, 프로젝트 접두어 표가 있으면 그것을 따릅니다.
- 토픽은 `application.yml`의 `kafka.topics.*`로 관리하고, 신규 토픽은 설정과 인프라 반영이 필요하다고 안내합니다.
- `@KafkaListener` 핸들러에서 예외를 삼키지 않습니다(재시도 후 DLT). 수동 커밋이면 `Acknowledgment.acknowledge()`를 명시 호출합니다. 처리는 멱등하게 작성합니다.
- 기존 메시지 DTO의 필드를 삭제하거나 이름, 타입을 바꾸지 않습니다. 필드 추가는 nullable이나 기본값으로만 합니다.

## Entity

- public 생성자와 public setter를 노출하지 않습니다.
- `equals`, `hashCode`가 필요하면 `id` 기준으로 직접 구현합니다. `data class`의 생성 메서드는 연관관계를 건드립니다.
- 숫자 ID는 `SEQUENCE`를 기본으로 하고 `allocationSize`를 DB 시퀀스의 `INCREMENT BY`와 맞춥니다.
- 동시 충돌이 잦은 카운터는 좋아요 수, 드문 카운터는 조회수를 기준으로 판단합니다.
- 페이지네이션 반환 타입은 `Page`로 통일합니다.
- `ddl-auto`는 `validate` 또는 `none`입니다.

## 인덱스 / 쿼리

- 인덱스 후보는 WHERE절에 반복되는 컬럼, JOIN의 FK 컬럼, ORDER BY/GROUP BY 대상, 카디널리티가 높은 컬럼입니다.
- 복합 인덱스의 범위 조건은 `BETWEEN`, `>`, `<`, `LIKE 'v%'`입니다.
- 앞부분이 겹치는 중복 인덱스는 짧은 쪽을 지웁니다. 단독 인덱스를 피할 저카디널리티 컬럼의 예는 `is_deleted`입니다.
- 인덱스를 바꾼 뒤 `pg_indexes` 또는 psql `\d {테이블}`로 정의를 확인합니다.
- `EXPLAIN`에서는 의도한 인덱스를 쓰는지, 큰 테이블에 `Seq Scan`이나 불필요한 `Sort`가 없는지 봅니다. 행이 적으면 PostgreSQL이 `Seq Scan`을 고를 수 있습니다.
- `@ManyToOne`, `@OneToOne`은 필요할 때 쿼리별로 fetchJoin합니다. 페이지네이션이 있는 `@OneToMany`, `@ManyToMany`는 `default_batch_fetch_size`에 맡깁니다.
- 페이지네이션 정렬에는 유일한 값을 포함해 순서를 고정합니다.
- 존재 확인은 `isNotEmpty()` 대신 EXISTS 쿼리를 씁니다. 묶음 처리는 `findAllById`, `saveAll`을 씁니다. Hot path의 N+1은 반드시 고칩니다.
- MySQL 전용 문법과 진단 명령을 쓰지 않습니다.

## Dto / Command

- 도메인 간 Command 번역과 Entity/Projection → Dto 변환은 생성자 호출로 처리합니다. `PageInfo.of()`만 공통 응답 예외입니다.
- Dto를 그대로 로깅하거나 외부로 전달하지 않습니다.
- Command는 Controller뿐 아니라 Service, EventListener 등 어디서든 만들 수 있습니다.
- 외부 계약의 사용자 이름이 `userId`와 다르면 `Request.toCommand()`에서 변환합니다.
- Command와 별도 인자를 병행하지 않습니다.

## Service

- 메서드는 동사(`find`, `get`, `create`, `update`, `delete`, `validate`, `is`)로 시작합니다.
- 순환 의존이 생기면 하위 Service로 분리하거나 이벤트로 해소합니다.
- 신규 생성의 `save()`는 더티 체킹 규칙의 예외입니다.

## Controller / rqrs

- CRUD로 표현되지 않는 상태 전이만 API Path 마지막 세그먼트에 동사를 허용합니다.
- 인증 정보는 `@AuthenticationPrincipal`이나 커스텀 리졸버로 꺼낼 수도 있습니다.
- Controller의 주입 제한에서 `Pageable`, `MultipartFile`, `Authentication`, 인증 헬퍼는 예외입니다.
- 생성은 `@ResponseStatus(CREATED)`, 삭제와 무본문 응답은 `NO_CONTENT`입니다.
- `GlobalExceptionHandler`는 `ResponseEntity` 규칙의 대상이 아닙니다.
- `ADMIN_V1`을 막아 둔 이유는 현재 `SecurityConfig`가 `authenticated()`만 검사해서, 로그인한 사용자 누구나 호출할 수 있기 때문입니다.
- 중첩 클래스 이름은 `Create`, `Update`, `Query`, `Result`, `DetailResult`, `ResultPage`를 씁니다.
- `from` 팩토리나 Spring `Page`를 그대로 반환하지 않습니다.
- PUT은 전체 수정, PATCH는 일부 수정입니다.
- 커스텀 검증 애노테이션은 `errorCode` 속성을 두고 `common/web/annotation/`에, 밸리데이터는 `common/web/validation/`에 둡니다.
- 필수 필드를 non-null 타입으로 두면 값이 없을 때 검증 전에 역직렬화가 실패합니다. 필드별 검증 오류가 필요하면 nullable 타입에 `@field:NotNull`을 씁니다.

## 예외 / Kotlin

- raw `RuntimeException`, `IllegalStateException`을 던지지 않습니다.
- 존재 보장 조회의 오류는 `BusinessException(ErrorCode.NOT_FOUND)`입니다.
- 에러 응답은 `ProblemDetail`로 통일하고 `code` 속성을 담습니다. 임의 Map이나 문자열, 스택트레이스를 노출하지 않습니다.
- `requireNotNull`, `checkNotNull`은 비즈니스 조건이 아닌 코드 내부 불변식에만 씁니다.
- 로깅은 SLF4J로 통일합니다. `println`, `printStackTrace`를 쓰지 않습니다.
- 의존성은 주 생성자의 `private val`로 주입합니다. 설정은 `@ConfigurationProperties`로 바인딩합니다.
- `allOpen` 설정을 지우지 않습니다. `plugin.jpa`는 인자 없는 생성자만 만들고 `open`으로 만들지 않습니다.

## 테스트

- 가장 좁은 범위로 시작합니다. Controller는 `@WebMvcTest`, JSON은 `@JsonTest`, 전체 흐름은 꼭 필요할 때만 `@SpringBootTest`를 씁니다.
- Controller 테스트의 mock은 `@MockkBean`, DB 테스트는 Testcontainers를 씁니다.
- 정상 경로 → 다른 경로와 경계값 → 오류 순으로 둡니다.

## Supabase

### 이유

- 대시보드(Table Editor, SQL Editor)나 `supabase migration`으로 같은 스키마를 바꾸지 않습니다.
- `auth.users`에 FK를 만들지 않는 이유는 순수 Postgres에 `auth` 스키마가 없어 마이그레이션이 실패하기 때문입니다. 사용자 삭제 후 데이터 정리는 서버가 맡습니다.
- 새 테이블의 RLS는 `ALTER TABLE {테이블} ENABLE ROW LEVEL SECURITY`로 켭니다. Supabase는 `public`을 Data API로 노출하므로 RLS가 없으면 공개 키로 접근될 수 있습니다.
- 서버는 테이블 소유자 계정으로 접속해 RLS를 우회합니다.
- RLS 정책은 프론트엔드가 Supabase에 직접 접근하는 테이블에만 필요합니다. `auth.uid()`와 `authenticated` 역할을 쓰는 정책은 순수 Postgres에서 실패합니다.
- 사용자 식별은 JWT `sub`(`getUserId()`)로만 합니다.
- 비밀값은 프론트엔드로 보내지 않습니다.

### 연결

- 기본 연결은 Direct connection 또는 Session pooler(5432)입니다.
- Transaction pooler(6543)를 쓰면 `?prepareThreshold=0`을 붙입니다. 커넥션을 세션 단위로 유지하지 않으므로 Flyway는 이 경로로 실행하지 않습니다.
- `hikari.maximum-pool-size`를 늘리기 전에 Supabase 플랜의 연결 한도를 확인합니다.
