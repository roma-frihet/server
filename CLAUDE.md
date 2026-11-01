# roma-frihet server

Spring Boot 4.1 + Kotlin 2.3 + JPA + QueryDSL + Flyway + Supabase(PostgreSQL, Auth) 백엔드입니다. 빌드는 Gradle Kotlin DSL, JDK는 21입니다.

실행 방법과 환경 변수는 [README.md](README.md), 코루틴, Spring AI, Kafka를 도입할 때의 규칙과 세부 규칙은 [docs/conventions/reference.md](docs/conventions/reference.md)를 봅니다.

## 명령

개발과 Claude Code 실행은 WSL Ubuntu에서만 합니다.

| 목적 | 명령 |
|---|---|
| 전체 검증 | `./gradlew check` |
| 포맷 | `./gradlew ktlintFormat` |
| 테스트 하나 | `./gradlew test --tests "*주문*"` |

## 구조

```
com.romafrihet.server
├── common/        # entity, exception, web(BaseController, ApiPaths, PageInfo)
├── config/
├── security/
└── {feature}/     # Controller, Service, Entity, Repository
    ├── command/
    ├── dto/
    ├── event/
    ├── projection/
    └── rqrs/
```

- 패키지는 레이어가 아니라 기능별로 나눕니다. 기능 패키지 사이에 순환 의존을 만들지 않습니다.
- 흐름은 Controller → Service → Repository입니다. Controller는 Repository를 쓰지 않고 엔티티를 반환하지 않습니다.
- Entity와 Command는 `rqrs`를 참조하지 않습니다.

## 하네스

- `.claude/settings.json`의 훅이 Kotlin 포맷, 위험한 명령과 파일 변경 차단, 종료 전 `check`를 처리합니다. 코드를 바꾼 턴은 `check`가 실패하면 끝낼 수 없습니다.
- 코드를 쓰기 전에 `spring-kotlin` 스킬을 확인합니다.
- 검사가 실패하면 규칙을 완화하지 말고 원인을 고칩니다. 규칙이 틀렸다고 판단되면 먼저 묻습니다.
- 사용자가 지적한 실수나 반복된 실수는 `docs/failures/`에 기록합니다. 같은 실수가 두 번 나오면 ArchUnit, detekt, 훅 중 하나의 검사로 승격합니다.
- Docker가 없으면 Testcontainers 테스트는 건너뜁니다. 이때 `check` 통과는 DB 동작까지 검증했다는 뜻이 아니므로 보고할 때 구분합니다.

## 작업 원칙

- 요청받은 범위만 수정합니다. 기존 코드의 위반은 고치지 않고 언급만 합니다.
- 작업 전 같은 도메인의 기존 코드를 열어 이름과 패키지 관례를 맞춥니다.

## 이름

- 클래스는 `{Feature}Controller`, `{Feature}Service`, `{Feature}Repository`, `{Feature}Request`, `{Feature}Response`, `{Feature}Command`, `{Feature}Dto`입니다.
- 관리자 대상은 `{Feature}AdminController`처럼 `Admin` 접미사로 분리합니다.
- 주석, 로그, `ErrorCode` 메시지는 한국어로 씁니다. 주석은 코드만으로 알 수 없는 이유가 있을 때만 답니다.

## Git

- 커밋 메시지는 `{type}: {한국어 요약}`이고, type은 `feat`, `fix`, `refactor`, `test`, `docs`, `chore`입니다.
- 커밋 하나에는 목적 하나만 담습니다.
- 브랜치는 `main`에서 `{type}/{kebab-case 설명}`으로 만듭니다.
- 사용자가 요청하기 전에는 커밋하거나 푸시하지 않습니다.

## Kotlin

- `build.gradle.kts`의 `allOpen`과 `-Xannotation-default-target=param-property`를 지우지 않습니다.
- 엔티티 프로퍼티는 `protected set`을 씁니다. `allOpen` 때문에 `private set`은 컴파일되지 않습니다.
- `!!`는 쓰지 않습니다. `?:`나 `assertCheckNotNull`로 대체합니다.
- enum과 sealed의 `when`에 `else`를 두지 않습니다.
- 로거는 `LoggerFactory.getLogger(javaClass)`를 `private val`로 둡니다. 개인정보나 Dto 전체를 남기지 않습니다.

## Entity / Flyway

- `data class`로 만들지 않습니다. 생성과 수정은 `create()`, `update()` 같은 의도 메서드로만 합니다.
- 시각 필드는 `LocalDateTime`만 씁니다.
- `@ManyToOne`, `@OneToOne`은 `LAZY`를 명시합니다. `CascadeType.REMOVE`, `ALL`은 `@ManyToOne`, `@ManyToMany`에 쓰지 않습니다.
- 카운터에 `@Version`을 쓰지 않습니다. 충돌이 잦으면 JPQL bulk update, 드물면 엔티티 메서드로 증감합니다.
- 스키마 변경은 `src/main/resources/db/migration`에 새 `V<major>.<minor>.<patch>__<설명>.sql`을 추가합니다. 첫 버전은 `1.0.0`이고 이후 `1.0.1`처럼 올립니다. 기존 파일은 고치지 않습니다.
- Entity는 Flyway DDL에 맞춥니다.

## DB 이름

- 테이블과 컬럼은 snake_case이고, 테이블은 복수형입니다.
- PK는 `id`, FK 컬럼은 `{단수 테이블명}_id`, boolean은 `is_`로 시작합니다.
- 제약 이름은 `uk_{테이블명}_{컬럼명}`, `fk_{테이블명}_{참조 테이블명}`입니다.
- 앱 테이블은 `created_at`, `updated_at`을 두고 Entity는 `BaseTimeEntity`를 상속합니다.

## 인덱스 / 쿼리

- 복합 인덱스 순서는 등치 → 범위 → `ORDER BY`이고, 이름은 `idx_{테이블명}_{컬럼명 조합}`입니다.
- 앞부분이 겹치는 중복 인덱스와 카디널리티가 낮은 컬럼의 단독 인덱스를 만들지 않습니다.
- 새 쿼리와 인덱스는 실제에 가까운 데이터로 `EXPLAIN (ANALYZE, BUFFERS)`를 확인합니다.
- Repository 메서드 접두어는 `find`, `exists`, `count`, `delete`(derived 쿼리), `query`(QueryDSL), `get`(존재 보장 확장 함수)입니다. `search`는 쓰지 않습니다.
- QueryDSL 커스텀은 `*RepositoryExtension`, `*RepositoryExtensionImpl`로 만듭니다.
- `FROM`에 없는 Q클래스를 `WHERE`에서 참조하지 않습니다.
- 컬렉션 `fetchJoin`은 2개 이상 쓰지 않고, 페이지네이션과 함께 쓰지 않습니다. 이 경우 BatchSize에 맡깁니다.
- 수정/삭제 경로에 Projection을 쓰지 않고, Projection에 `ToMany` 필드를 넣지 않습니다.
- 루프 안 개별 조회와 저장, 전체 로드 후 `.filter{}`를 쓰지 않습니다.

## Dto / Command

- 변환은 `Request.toCommand()`, `Response.of(dto)`, `ResultPage.of(page)`만 팩토리로 만듭니다. 그 외에는 Mapper 없이 생성자를 named argument로 호출합니다.
- Repository 반환 전용 클래스는 `{feature}/projection/`의 `~Projection`, 입력은 `~QueryDto`입니다.
- Dto는 필요한 필드를 모두 담고, 클라이언트에 보낼 필드는 Response를 만들 때 고릅니다.
- Command는 `{feature}/command/{Feature}Command.kt`에 두고, `class XCommand private constructor()` 안에 `Create`, `Update`, `Query`를 중첩합니다.
- Command를 Repository까지 넘기지 않습니다. 단, 조회된 엔티티의 `entity.update(command)`는 허용합니다.
- 파라미터가 3개 이상이면 Command로 묶고, `userId`와 path variable도 그 안에 담습니다. 인증 사용자는 내부에서 항상 `userId`로 부릅니다.
- Service와 엔티티는 자기 도메인 Command만 받습니다. 다른 도메인을 호출할 때는 호출 측이 상대 Command를 만들어 넘깁니다.

## Service

- 입력은 Command 또는 단일 값 타입만 받습니다. Dto는 받지 않습니다.
- 다른 Service나 다른 도메인 Repository 호출은 허용합니다. Facade 레이어와 `XServiceImpl` 분리는 만들지 않습니다.
- 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`을 둡니다.
- 트랜잭션 안에서 외부 API를 호출하지 않고, `suspend` 함수에 `@Transactional`을 쓰지 않습니다.
- 더티 체킹에 맡기고, 수정한 엔티티를 다시 `save()`하지 않습니다.

## Controller / rqrs

- 경로는 `ApiPaths` 상수로 쓰고, 복수형 명사와 kebab-case를 기본으로 합니다.
- 핸들러에 `@Operation`, 클래스에 `@Tag`를 붙입니다.
- 인증 정보는 `BaseController` 헬퍼로 꺼냅니다. `SecurityContextHolder`에 직접 접근하지 않습니다.
- Service만 주입하고, 엔드포인트 하나당 Service 메서드 하나만 호출합니다.
- Response를 직접 반환하고 상태코드는 `@ResponseStatus`로 지정합니다. `ResponseEntity`는 헤더 조작이 필요할 때만 씁니다. 전역 래퍼로 감싸지 않습니다.
- 쿼리 파라미터는 `@ParameterObject` + `Query` DTO로 받습니다.
- 권한 검사가 구현되기 전에는 `ADMIN_V1` 아래에 엔드포인트를 추가하지 않습니다.
- Request, Response는 `{feature}/rqrs/`에 두고, 바깥 클래스는 `private constructor()` 네임스페이스, 계약은 중첩 클래스로 만듭니다. `@Schema(name=...)`은 `바깥클래스명+중첩클래스명`입니다.
- 다른 기능의 rqrs를 참조하지 않습니다. 자식 → 부모 참조는 예외입니다.
- PATCH DTO에는 실제로 수정하는 필드만 선언하고, `null`이면 값을 비웁니다.
- 구문 검증은 Bean Validation, 비즈니스 검증은 Service의 `assertCheck`로 합니다.

## 예외

- 비즈니스 오류는 `BusinessException(errorCode)`로 던집니다.
- 사전조건은 `assertCheck`, `assertCheckNotNull`, 존재 보장 조회는 `getOrThrow`류 확장 함수를 씁니다.
- 오류 응답은 `GlobalExceptionHandler`가 `ProblemDetail`로 만듭니다. Controller와 Service에서 try-catch로 만들지 않습니다.
- `ErrorCode`는 도메인 접두어에 맞춰 추가하고, 새 접두어는 `ErrorCode.kt` 주석 표에 적습니다. 배포된 `code` 값은 바꾸지 않습니다.

## 이벤트

- 이벤트(`{Name}Event`, 과거형)는 발행 기능의 `{feature}/event/`, 리스너는 소비 기능의 패키지, 인프라 부수효과는 `common/listener/`에 둡니다.
- 이벤트에는 값 타입만 담습니다. 엔티티를 담지 않습니다.
- 리스너는 Service에 위임하고 Repository를 주입하지 않습니다.
- 외부 연동은 `@TransactionalEventListener(AFTER_COMMIT)`로 분리합니다. 이 안에서 DB에 쓰려면 `REQUIRES_NEW`나 비동기로 실행합니다.

## Supabase

- 스키마는 Flyway로만 바꾸고, 앱 테이블은 `public`에 만듭니다. `auth`, `storage`, `realtime` 스키마는 건드리지 않습니다.
- 사용자는 `user_id uuid` 값으로만 참조합니다. `auth.users`에 FK나 트리거를 만들지 않습니다.
- 새 테이블은 같은 마이그레이션에서 RLS를 켭니다. `FORCE ROW LEVEL SECURITY`는 쓰지 않고, 정책 추가는 개발자에게 확인받습니다.
- 서버는 RLS를 우회하므로 사용자별 소유권 검사는 Service에서 합니다.
- 회원가입, 로그인, 토큰 발급을 서버에 구현하지 않습니다.
- 권한은 `app_metadata`나 서버 DB 값으로 판단합니다. `user_metadata`는 사용자가 수정할 수 있어 쓰지 않습니다.
- `service_role` 키, DB 비밀번호, JWT secret은 `.env`로만 관리합니다.
- Flyway는 Transaction pooler(6543)로 실행하지 않습니다.
