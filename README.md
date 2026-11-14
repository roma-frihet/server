# roma-frihet server

Spring Boot 4.1 + Kotlin 2.3 + JPA(QueryDSL) + Supabase(PostgreSQL, Auth) 백엔드.

## 요구 사항

개발은 WSL Ubuntu에서 한다. 아래 도구는 모두 WSL 안에서 쓸 수 있어야 한다.

| 도구 | 버전 | 비고 |
|---|---|---|
| JDK | 21 | `sudo apt install openjdk-21-jdk-headless` |
| Docker | 최신 | 로컬 Supabase 실행용. Testcontainers 테스트에도 필요하다 |
| Node.js | 최신 LTS | `npx supabase`로 Supabase CLI를 실행한다 |

Gradle은 설치할 필요 없다. 저장소의 Gradle Wrapper(`gradlew`)를 쓴다.

## 처음 실행하기

### 1. 환경 변수 파일 만들기

```bash
cp .env.example .env
```

`.env`는 git에 올라가지 않는다. 값 설명은 `.env.example` 주석을 본다.

### 2. Supabase 띄우기 — 둘 중 하나

**A. 로컬 Supabase (권장, Auth와 Storage까지 로컬에서 동작)**
```bash
npx supabase init     # 최초 1회, supabase/ 폴더가 없을 때만
npx supabase start    # Postgres: localhost:54322, API/Auth: http://127.0.0.1:54321, Studio: http://127.0.0.1:54323
npx supabase stop     # 끝날 때, 데이터는 남는다
```
서버는 DB와 Auth를 모두 Supabase에 의존하므로, 개발 중에는 Supabase를 켜 둔다. PC를 재부팅했거나 Docker가 꺼졌다면 다시 `start`한다.

`.env.example` 기본값이 이 구성에 맞춰져 있다. 로컬 Supabase는 ES256으로 토큰에 서명하므로 `SUPABASE_JWT_SECRET`은 비워 둔다. 키는 `npx supabase status`로 확인한다.

**B. 원격 Supabase 프로젝트에 직접 연결**
대시보드 → Project Settings → Database → Connection string에서 JDBC 주소를 가져와 `.env`의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SUPABASE_URL`을 바꾼다.
- Direct connection 또는 Session pooler(5432)를 쓴다. Transaction pooler(6543)는 `?prepareThreshold=0`이 필요하다.
- 프로젝트가 레거시 JWT secret을 쓰면 Project Settings → API에서 값을 복사해 `SUPABASE_JWT_SECRET`에 넣는다. 새 JWT Signing Keys(비대칭)를 쓰면 비워 둔다.

### 3. 실행

```bash
./gradlew bootRun
```
IntelliJ에서는 `ServerApplication.kt`의 ▶ 버튼으로 실행한다(작업 디렉터리가 프로젝트 루트여야 `.env`를 읽는다).

- 헬스 체크: http://localhost:8080/actuator/health
- API 문서: http://localhost:8080/swagger-ui.html

## 자주 쓰는 명령

| 명령 | 설명 |
|---|---|
| `./gradlew build` | 컴파일 + 테스트 + ktlint + detekt + ArchUnit 검사 |
| `./gradlew test` | 테스트 |
| `./gradlew ktlintFormat` | 코드 스타일 자동 정리 |
| `./gradlew kaptKotlin` | QueryDSL Q클래스 생성 (`build/generated/source/kapt`) |

## 인증

- 로그인·회원가입·토큰 발급은 **Supabase Auth**가 담당한다. 서버는 `Authorization: Bearer <access_token>`의 JWT를 검증만 한다.
- 검증 방식: `SUPABASE_JWT_SECRET`이 있으면 HS256, 없으면 `{SUPABASE_URL}/auth/v1/.well-known/jwks.json` 공개키(ES256/RS256). `aud = authenticated`도 확인한다.
- Controller에서는 `BaseController.getUserId()`로 사용자 ID(UUID, = `auth.users.id`)를 꺼낸다.
- 인증 없이 열려 있는 경로: `/actuator/health`, Swagger. `/api/v1/teacher/**`는 `ROLE_TEACHER`가 필요하고, 나머지는 모두 인증 필요 (`security/SecurityConfig.kt`).
- `ROLE_TEACHER`는 JWT의 `app_metadata.role`이 `teacher`일 때만 부여한다 (`security/SupabaseJwtAuthenticationConverter.kt`).

## DB 마이그레이션

- Flyway가 스키마의 기준이다. `src/main/resources/db/migration/V{major}.{minor}.{patch}__{설명}.sql`로 추가한다. 첫 버전은 `1.0.0`이고 이후 `1.0.1`처럼 올린다.
- 이미 적용된 파일은 수정하지 않는다. `ddl-auto`는 `validate`.
- Supabase 대시보드나 `supabase migration`으로 같은 테이블을 따로 만들지 않는다(두 군데서 스키마를 관리하지 않는다).

## 패키지 구조

```
com.romafrihet.server
├── common/
│   ├── entity/                  # BaseTimeEntity
│   ├── exception/               # BusinessException, ErrorCode, assertCheck, GlobalExceptionHandler
│   └── web/                     # BaseController, ApiPaths, PageInfo
├── config/                      # JPA(QueryDSL), OpenAPI
├── security/                    # Supabase JWT 검증, SecurityConfig
└── {feature}/                   # Controller, Service, Entity, Repository, command/, dto/, event/, projection/, rqrs/
```

코드 규칙은 [CLAUDE.md](CLAUDE.md)를 따른다.
