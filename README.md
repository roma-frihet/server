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

### 로컬 주소

| 이름 | 주소 | 용도 |
|---|---|---|
| Swagger | http://localhost:8080/swagger-ui.html | API 문서 |
| 헬스 체크 | http://localhost:8080/actuator/health | 서버 상태 확인 |
| API 테스터 | http://localhost:5500 | 브라우저에서 API 호출, 따로 실행 |
| Supabase Studio | http://127.0.0.1:54323 | DB, 사용자, Storage 관리 |
| Mailpit | http://127.0.0.1:54324 | 로컬에서 보낸 메일 확인 |
| Supabase API | http://127.0.0.1:54321 | 로그인, 토큰 발급 |
| Postgres | `localhost:54322`, DB·계정·비밀번호 `postgres` | DB 직접 연결 |

Supabase 주소는 `npx supabase start`를 실행해야 열린다. 값이 다르면 `npx supabase status`로 확인한다.

### 종료

| 대상 | 방법 |
|---|---|
| 서버 | 실행한 터미널에서 `Ctrl+C`, IntelliJ는 정지 버튼 |
| API 테스터 | 실행한 터미널에서 `Ctrl+C` |
| Supabase | `npx supabase stop` |

`npx supabase stop`은 컨테이너만 멈추며 데이터와 계정은 남는다. 다음에 `npx supabase start`로 이어서 쓴다. 로컬 데이터를 지우고 마이그레이션을 다시 적용하려면 `npx supabase db reset`을 쓴다.

## API 테스터

프론트 없이 브라우저에서 API를 눌러 보는 로컬 전용 화면이다. 교사와 학생으로 동시에 로그인해 한 화면에서 번갈아 테스트하고, 오른쪽에서 요청과 응답 JSON을 확인한다. 서버와 함께 켜지지 않으므로 따로 실행한다.

### 실행

Python 3와 Node.js가 필요하다. 추가 설치는 없다. 터미널을 나눠 순서대로 실행한다.

```bash
npx supabase start                # 1. Supabase
./gradlew bootRun                 # 2. 서버 (또는 IntelliJ 실행)
python3 tools/api-tester/serve.py # 3. 테스터
```

브라우저에서 http://localhost:5500 을 연다. 종료는 `Ctrl+C`다. 서버 코드를 바꾼 뒤에는 서버만 재시작하면 된다.

### 테스트 순서

| 탭 | 할 일 |
|---|---|
| 계정 | 처음이면 `교사 계정 만들기`, `학생 계정 만들기`를 누르고 로그인한다. 학교를 검색하고 교사 정보와 학생 정보를 저장한다 |
| 수업·반·코드 | 수업과 반을 만들어 선택하고 합류 코드를 발급한다. 코드는 3분간 유효하다 |
| 차시·문제 | 차시를 만들어 선택하고 문제를 추가한 뒤 선택한 반에 연다 |
| 합류·승인 | 학생이 코드를 입력하고 교사가 승인한다 |
| 학생 풀이 | 학생이 문제를 풀어 제출한다. 차시를 마감하면 결과를 볼 수 있다 |
| 대시보드·학생 관리 | 대시보드, 학생 관리, 학생 점수 조회, 마이페이지를 확인한다 |

대부분의 동작은 위쪽에 표시된 선택한 수업, 반, 차시에 적용된다. 이미지 업로드에는 `SUPABASE_SERVICE_ROLE_KEY`와 Private 버킷 `question-images`가 필요하다. 토큰은 1시간이면 만료되므로 그때 다시 로그인한다.

### 환경 변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `TESTER_PORT` | `5500` | 테스터 화면 포트 |
| `API_TARGET` | `http://localhost:8080` | 연결할 서버. `localhost`, `127.0.0.1`만 허용한다 |

### 안전

- `127.0.0.1`에만 바인딩하므로 다른 PC에서 접속할 수 없고, 원격 서버를 지정하면 실행을 거부한다. 운영 환경에서 쓰지 않는다.
- 키는 저장소에 없다. 실행할 때 `npx supabase status`로 로컬 키를 읽는다.
- 화면에 미리 채워진 `teacher@test.com`, `student@test.com`, `test1234!`는 로컬 테스트 전용 값이다.
- 서버 jar에 포함되지 않는다.

### 문제 해결

| 증상 | 조치 |
|---|---|
| `Supabase가 꺼져 있습니다` | `npx supabase start`를 실행한다 |
| 위쪽에 `서버 꺼짐` | 서버를 실행한다. 다른 포트면 `API_TARGET`을 지정한다 |
| 로그인 후 API가 `401` | `.env`의 `SUPABASE_JWT_SECRET`을 비우고 서버를 재시작한다 |
| 이미지 업로드가 `502` | 키와 `question-images` 버킷을 확인한다 |
| `5500` 포트 사용 중 | `TESTER_PORT=5501 python3 tools/api-tester/serve.py`로 바꾼다 |

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
