# 프론트엔드 전달 문서

이 문서는 **무엇을 만드는지와 어떤 순서로 흘러가는지**를 담습니다.
요청·응답 필드의 정확한 모양은 서버를 띄우고 `http://localhost:8080/swagger-ui.html`을 봅니다.
실제로 호출해 보려면 `tools/api-tester/`를 씁니다 (실행 방법은 [README.md](../../README.md)).

## 1. 이 서비스

수업 시간에 쓰는 **복습 퀴즈** 서비스입니다.

교사가 차시별로 문제를 만들어 두고, 수업 중에 한 차시를 "엽니다". 학생은 자기 기기에서 그 차시의 문제를 **한 문제씩** 풉니다. 문제마다 제한 시간이 있고 되돌아갈 수 없습니다. 교사는 푸는 동안 반의 진행 상황을 보고, 끝나면 차시를 마감합니다. 마감해야 학생에게 정답과 해설이 공개됩니다.

## 2. 핵심 개념

이 다섯 가지가 서로 어떻게 걸려 있는지만 잡으면 나머지는 따라옵니다.

```
학교
└── 수업        교사 한 명 + 학년 + 과목     "2학년 과학"
    ├── 차시     수업에 속한 회차            "1차시 광합성"
    │   └── 문제  차시에 속함. 제한 시간·해설 보유
    └── 반       수업에 속한 교실            "1반", "2반"
        └── 학생  합류 코드로 연결
```

**문제는 반이 아니라 수업에 속합니다.** 그래서 2학년 과학의 1차시 문제를 1반과 2반이 함께 씁니다. 교사가 같은 문제를 반마다 다시 만들 필요가 없습니다.

**반대로 "지금 몇 차시를 열었는가"는 반마다 다릅니다.** 1반은 3차시를 푸는 중이고 2반은 아직 2차시일 수 있습니다. 한 반에서 동시에 열 수 있는 차시는 하나뿐입니다.

여기에 하나 더 있습니다.

**응시** — 한 학생이 한 반에서 한 차시를 푼 기록입니다. 그 안에 문제별 응답이 쌓입니다. 점수는 어디에도 저장하지 않고 **읽을 때마다 셉니다.** 그래서 교사가 결과를 고치면 평균까지 바로 따라 움직입니다.

## 3. 화면은 교사와 학생으로 나뉩니다

**네, 크게 둘로 나누는 게 맞습니다.** 같은 서비스지만 사실상 다른 앱입니다.

| | 교사 | 학생 |
|---|---|---|
| 하는 일 | 만들고, 열고, 지켜보고, 관리한다 | 푼다, 본다 |
| 쓰는 시간 | 수업 준비 + 수업 중 | 수업 중 몇 분 |
| 화면 | 넓은 관리 화면, 표와 대시보드 | 한 번에 문제 하나 |
| 로그인 | 이메일 | 학번 |
| 경로 | `/api/v1/teacher/**` | `/api/v1/**` |

경로부터 갈라져 있고 권한도 서버가 분리해서 막습니다. 교사 경로는 교사 토큰이 아니면 아예 들어갈 수 없습니다. 코드도 두 묶음으로 나누는 편이 자연스럽습니다.

다만 **공통으로 쓰는 것**이 있습니다 — 로그인 처리, 에러 표시, 학교 검색. 이건 아래 4장입니다.

## 4. 공통

### 로그인은 서버가 하지 않습니다

회원가입·로그인·토큰 발급은 전부 **Supabase Auth**가 합니다. 프론트가 Supabase에 직접 요청해 토큰을 받고, 우리 서버에는 그 토큰을 헤더에 실어 보냅니다.

```
Authorization: Bearer <access_token>
```

서버는 토큰을 검증하고 그 안의 사용자 식별자만 꺼내 씁니다. 비밀번호는 서버에 없습니다.

**교사와 학생을 가르는 건 토큰 안의 `app_metadata.role`입니다.** 값이 `teacher`면 교사입니다. 이 값은 사용자가 바꿀 수 없는 영역에 있어야 합니다 — `user_metadata`에 넣으면 학생이 스스로 교사가 될 수 있습니다.

토큰은 1시간이면 만료됩니다. 갱신 처리가 필요합니다.

### 학생은 학번으로 로그인합니다

Supabase는 이메일로만 로그인을 받습니다. 그래서 **프론트가 학번을 이메일 모양으로 바꿔서** 보냅니다. 학생 화면에는 학번만 보입니다.

```
s{schoolId}-{학년}{반 2자리}{번호 2자리}@{도메인}
7번 학교 2학년 1반 1번  →  s7-20101@students.roma-frihet.app
```

- 학교를 넣는 이유는 학교가 다르면 학번이 겹치기 때문입니다.
- 도메인은 서버 설정과 **반드시 같아야 합니다**. 기본값은 `students.roma-frihet.app`입니다.
- 서버는 가입에 끼지 않지만, 가입 정보를 저장할 때 토큰의 계정이 적어낸 학번과 맞는지 확인합니다. 다르면 `SP004`로 거절합니다.

### 가입은 두 단계입니다

Supabase 계정을 만드는 것과, 우리 서버에 이름·학교·학번을 저장하는 것은 **별개 요청**입니다.

중간에 끊기면 계정만 있고 정보가 없는 상태가 됩니다. 이때 `GET /api/v1/student-profiles/me`의 `profile`이 `null`로 내려오므로, **로그인 직후 이 값을 확인해 비어 있으면 가입 정보 입력 화면으로 보냅니다.** 교사도 똑같이 `GET /api/v1/teacher/mypage`로 확인합니다.

### 에러는 한 가지 모양입니다

```json
{
  "status": 400,
  "title": "STUDENT_LOGIN_EMAIL_MISMATCH",
  "detail": "로그인 계정과 학번이 맞지 않습니다.",
  "code": "SP004",
  "instance": "/api/v1/student-profiles"
}
```

- `detail`은 **사용자에게 그대로 보여 줘도 되는 한국어 문장**입니다.
- `code`는 분기용입니다. 접두어로 도메인을 알 수 있습니다 — `C` 공통, `AU` 인증, `CL` 반·합류, `CO` 수업, `LS` 차시·문제·응시, `SP` 학생 가입 정보, `ST` 파일.
- 전체 목록은 `common/exception/ErrorCode.kt`에 있습니다.

### 학교 검색

`GET /api/v1/schools?keyword=다원` — 로그인 전에도 호출할 수 있습니다. 가입 화면에서 학교를 고를 때 씁니다.

지금 등록된 학교는 **다원중학교 하나**입니다. 학교를 늘리려면 서버 마이그레이션이 필요합니다.

## 5. 교사 여정

### 5-1. 처음 들어왔을 때

교사는 이메일로 가입하고, 이름과 학교를 저장합니다.

그다음 **수업이 하나도 없는 상태**로 시작합니다. 이게 정상입니다. 대시보드가 빈 화면이 아니라 **안내 화면**이 되어야 합니다. 서버가 어디까지 했는지 알려 줍니다.

```
GET /api/v1/teacher/courses/{courseId}/dashboard  →  setup
  hasClassroom      반을 만들었나
  hasLesson         차시를 만들었나
  hasQuestion       문제를 넣었나
  hasOpenedLesson   한 번이라도 차시를 열었나
```

이 네 개로 "다음에 할 일"을 짚어 주면 됩니다.

| | API |
|---|---|
| 내 정보 저장 | `PUT /api/v1/teacher/profile` |
| 마이페이지 | `GET /api/v1/teacher/mypage` |

### 5-2. 수업과 반 만들기

**수업은 학년 + 과목입니다.** 같은 과목이라도 학년이 다르면 다른 수업입니다. 한 교사가 1학년 공통과학과 2학년 지구과학을 동시에 맡을 수 있습니다.

과목은 2022 개정 교육과정 목록에서 고릅니다. 직접 입력이 아니라 **선택**입니다.

**반은 번호입니다.** "1반", "2반"처럼 숫자만 받습니다 (1~20). 반 이름을 자유 입력으로 두지 않습니다.

| | API |
|---|---|
| 과목 목록 | `GET /api/v1/teacher/subjects?schoolLevel=MIDDLE` |
| 수업 생성 | `POST /api/v1/teacher/courses` |
| 내 수업 목록 | `GET /api/v1/teacher/courses` |
| 수업 삭제 | `DELETE /api/v1/teacher/courses/{courseId}` |
| 반 생성 | `POST /api/v1/teacher/classrooms` |
| 내 반 목록 | `GET /api/v1/teacher/classrooms` |
| 반 삭제 | `DELETE /api/v1/teacher/classrooms/{classroomId}` |

**삭제는 비어 있을 때만 됩니다.** 수업에 반이나 문제가 있으면, 반에 학생이나 기록이 있으면 거절합니다. 삭제 버튼을 누르기 전에 막아 주거나, 거절 메시지를 그대로 보여 주면 됩니다.

### 5-3. 학생 모으기

교사가 **합류 코드**를 발급하면 화면에 코드가 뜹니다. 학생이 그 코드를 입력하면 바로 연결됩니다.

**승인 절차는 없습니다.** 코드를 아는 사람은 그대로 들어옵니다. 대신 코드가 **5분 뒤 만료**되고, 다시 발급하면 이전 코드는 즉시 무효가 됩니다. 수업 시작할 때 띄워 두는 용도입니다.

화면에 남은 시간을 보여 주는 게 좋습니다. 잘못 들어온 학생은 교사가 내보낼 수 있고, 그 반에서 푼 기록도 함께 지워집니다.

| | API |
|---|---|
| 코드 발급 | `POST /api/v1/teacher/classrooms/{classroomId}/join-code` |
| 코드 끄기 | `DELETE /api/v1/teacher/classrooms/{classroomId}/join-code` |
| 반 학생 목록 | `GET /api/v1/teacher/classrooms/{classroomId}/memberships` |
| 학생 내보내기 | `DELETE /api/v1/teacher/classrooms/{classroomId}/memberships/{membershipId}` |

### 5-4. 차시와 문제 만들기

차시 번호는 서버가 매깁니다. 추가하면 마지막 번호 다음입니다.

문제는 **객관식**과 **주관식** 두 종류입니다. 문제마다 이런 것들을 갖습니다.

- 제한 시간 (1~300초) — 이 문제 하나에 주는 시간입니다
- 해설 — 마감 후에만 학생에게 보입니다
- 이미지 — 문제 본문과 보기 각각에 붙일 수 있습니다

**이미 연 적 있는 차시에는 문제를 더하거나 지울 수 없습니다.** 점수의 분모가 달라지기 때문입니다. 내용 수정은 되지만 정답이나 보기를 바꾸는 건 막힙니다. 화면에서 해당 차시의 추가/삭제 버튼을 비활성화해 주세요.

| | API |
|---|---|
| 차시 목록 | `GET /api/v1/teacher/courses/{courseId}/lessons` |
| 차시 추가 | `POST /api/v1/teacher/courses/{courseId}/lessons` |
| 차시 이름 수정 | `PATCH /api/v1/teacher/courses/{courseId}/lessons/{lessonNumber}` |
| 문제 목록 | `GET /api/v1/teacher/courses/{courseId}/lessons/{lessonNumber}/questions` |
| 문제 추가 | `POST /api/v1/teacher/courses/{courseId}/lessons/{lessonNumber}/questions` |
| 문제 상세 | `GET /api/v1/teacher/questions/{questionId}` |
| 문제 수정 | `PUT /api/v1/teacher/questions/{questionId}` |
| 문제 삭제 | `DELETE /api/v1/teacher/questions/{questionId}` |
| 이미지 업로드 | `POST /api/v1/teacher/question-images` (multipart) |

이미지는 먼저 업로드해서 `imageId`를 받고, 문제를 만들 때 그 id를 넣습니다.

### 5-5. 수업 중

차시는 **열기 → 마감** 흐름입니다. 중간에 닫을 수도 있습니다.

| 동작 | 뜻 |
|---|---|
| 열기 | 학생이 풀 수 있게 됩니다. 닫았던 차시도 같은 API로 다시 엽니다 |
| 닫기 | 학생에게서 숨깁니다. 다시 열 수 있습니다 |
| 마감 | **되돌릴 수 없습니다.** 학생이 내 답과 정답, 해설을 볼 수 있게 됩니다 |

마감은 되돌릴 수 없으니 확인을 한 번 받는 게 좋습니다.

**닫거나 마감해도 이미 문제를 풀고 있던 학생은 그 문제를 끝까지 냅니다.** 새 문제를 시작하는 것만 막힙니다. 화면에서 "지금 n명이 풀고 있습니다"를 보여 주면 교사가 판단하기 좋습니다.

대시보드에는 지금 열려 있는 차시, 반별 진도와 평균, 그리고 **살펴볼 학생**이 올라옵니다.

- `notRespondedStudents` — 차시를 열었는데 아무 답도 하지 않은 학생
- `droppedStudents` — 직전 차시보다 정답률이 20%p 이상 떨어진 학생

| | API |
|---|---|
| 대시보드 | `GET /api/v1/teacher/courses/{courseId}/dashboard` |
| 반별 열림 현황 | `GET /api/v1/teacher/lesson-openings` |
| 차시 열기 | `POST /api/v1/teacher/classrooms/{classroomId}/lessons/{lessonNumber}/open` |
| 차시 닫기 | `POST .../close` |
| 차시 마감 | `POST .../finish` |

### 5-6. 학생 관리

| | API |
|---|---|
| 반 학생 목록·점수 | `GET /api/v1/teacher/classrooms/{classroomId}/students?keyword=` |
| 학생 상세 | `GET /api/v1/teacher/classrooms/{classroomId}/students/{membershipId}` |
| 점수 내보내기 | `GET /api/v1/teacher/courses/{courseId}/scores` (페이지) |
| 결과 덮어쓰기 | `PUT /api/v1/teacher/attempt-answers/{attemptAnswerId}/result` |
| 비밀번호 초기화 | `POST /api/v1/teacher/classrooms/{classroomId}/students/{membershipId}/password-reset` |

**결과 덮어쓰기** — 접속이 끊기는 등 사정이 있을 때 교사가 학생 이야기를 듣고 결과를 바꿉니다. 학생 상세에 문제별 `attemptAnswerId`가 들어 있고, 그걸로 지정합니다. 확정된 문제만 바꿀 수 있고 **학생이 시작조차 하지 않은 문제는 목록에 없습니다** — 교사도 손댈 수 없습니다. 원래 결과는 이력으로 남고, 바뀐 항목은 `isOverridden`으로 표시됩니다.

**비밀번호 초기화** — 임시 비밀번호가 **응답에 한 번만** 내려옵니다. 다시 조회할 수 없으니 교사가 받아 적을 수 있게 화면에 크게 보여 주세요. 초기화하면 그 학생은 로그아웃됩니다.

## 6. 학생 여정

### 6-1. 가입과 합류

학번으로 가입하고, 이름·학교·학년·반·번호를 저장합니다. 그다음 교사가 띄워 준 **합류 코드**를 입력하면 반에 연결됩니다.

승인을 기다리지 않습니다. 넣으면 바로 들어갑니다.

| | API |
|---|---|
| 가입 정보 저장 | `POST /api/v1/student-profiles` |
| 내 가입 정보 | `GET /api/v1/student-profiles/me` |
| 반 합류 | `POST /api/v1/classroom-memberships` |
| 내 반 목록 | `GET /api/v1/classroom-memberships` |

**임시 비밀번호 상태** — `GET /api/v1/student-profiles/me`의 `mustChangePassword`가 `true`면 교사가 초기화해 준 상태입니다. 로그인 직후 **비밀번호 변경 화면으로 보내세요.** 바꾸면 자동으로 풀립니다.

| | API |
|---|---|
| 비밀번호 변경 | `PUT /api/v1/student-profiles/me/password` |

### 6-2. 문제 풀기

여기가 이 서비스의 핵심이고, 규칙이 가장 빡빡한 곳입니다.

**한 문제씩, 되돌아갈 수 없습니다.**

```
문제 시작  →  (답 고르기, 수시로 임시 저장)  →  제출  →  바로 정답·해설  →  다음 문제 시작
```

- **시작하기 전에는 문제 본문이 내려오지 않습니다.** 개수와 제한 시간만 압니다. 미리 보고 준비하면 타이머가 의미가 없기 때문입니다.
- **시간은 "시작" 버튼을 누른 순간부터 흐릅니다.** 다음 문제의 시간은 그 문제를 시작할 때까지 흐르지 않습니다.
- 남은 시간은 `current.remainingMillis`로 내려옵니다. 이걸로 화면 타이머를 돌립니다. 기기 시계를 믿지 마세요.
- 같은 문제를 다시 시작해도 **시간이 늘어나지 않습니다.** 새로고침해도 안전합니다.
- 제출하면 **그 문제의 정답과 해설이 바로** 내려옵니다. 차시 전체 마감을 기다리지 않습니다.
- 시간을 넘기면 **임시 저장해 둔 답으로 채점**됩니다. 아무것도 없으면 미응답입니다.
- 네트워크가 끊겨 같은 제출이 두 번 가도 안전합니다. 결과가 바뀌지 않습니다.

그래서 화면은 **답을 고를 때마다 임시 저장을 보내는 게 좋습니다.** 학생이 답을 골라 놓고 제출을 못 누른 채 시간이 지나도 그 답이 인정됩니다.

| | API |
|---|---|
| 내 반의 차시 목록 | `GET /api/v1/classrooms/{classroomId}/lessons` |
| 응시 상태 | `GET /api/v1/classrooms/{classroomId}/lessons/{lessonNumber}/attempt` |
| 문제 시작 | `POST .../attempt/questions/{questionIndex}/start` |
| 답 임시 저장 | `PUT .../attempt/questions/{questionIndex}/draft` |
| 제출 | `POST .../attempt/questions/{questionIndex}/submit` |
| 마감 차시 결과 | `GET /api/v1/classrooms/{classroomId}/lessons/{lessonNumber}/result` |

**화면 복원은 응시 상태 하나로 됩니다.** 앱을 껐다 켜도 `GET .../attempt`가 지금 어디인지 알려 줍니다.

| `phase` | 뜻 | 화면 |
|---|---|---|
| `READY` | 다음 문제를 시작할 수 있음 | "시작" 버튼 |
| `IN_QUESTION` | 문제 푸는 중 | 문제 + 타이머 + 임시 저장된 내 답 |
| `DONE` | 이 차시 끝 | 결과 요약 |

`nextIndex`가 다음에 시작할 문제 번호입니다. `start`에는 반드시 이 번호를 넣습니다. 다른 번호를 넣으면 거절됩니다.

### 6-3. 리포트

학생이 자기 기록을 보는 화면입니다.

- 차시별 **내 점수와 반 평균** — 추이 그래프로 그리기 좋습니다
- 지금 열린 차시와 내 상태
- **마감한 차시에서 틀린 문제** — 정답과 해설 포함

**마감 전 차시의 틀린 문제는 내려오지 않습니다.** 마감 전에 정답을 공개하지 않는다는 규칙과 같은 이유입니다.

**반 평균은 주지만 다른 학생이 누구인지는 주지 않습니다.** 이름도 학번도 점수도 내려오지 않습니다. 교사용 응답과 섞어 쓰지 않도록 주의하세요.

| | API |
|---|---|
| 내 학습 리포트 | `GET /api/v1/classrooms/{classroomId}/report` |
| 틀린 문제 모음 | `GET /api/v1/classrooms/{classroomId}/report/wrong-questions` |

## 7. 자주 헷갈리는 것

**점수가 나중에 바뀔 수 있습니다.** 학생이 문제를 열어 두고 돌아오지 않으면, 그 응답은 교사가 화면을 열어 보는 시점에 "시간 초과"로 확정됩니다. 그래서 같은 차시의 평균이 조금 뒤에 달라질 수 있습니다. 캐싱할 때 염두에 두세요.

**응답하지 않은 학생은 평균에서 빠집니다.** 0점으로 세지 않습니다. 점수가 `null`로 내려오면 "미응시"로 표시하고, 0점과 구분해 주세요.

**`membershipId`와 학생은 다릅니다.** 교사 API에서 학생을 가리킬 때는 `membershipId`(그 반에 속한 기록의 id)를 씁니다. 같은 학생이 다른 반에 있으면 다른 값입니다.

**이미지 URL은 만료됩니다.** 서명된 임시 주소로 내려옵니다. 오래 들고 있지 말고 화면을 열 때 다시 받으세요.

## 8. 아직 없는 것

- **학습 피드백** — 리포트에 LLM 기반 코멘트를 붙일 예정입니다. 지금은 자리가 비어 있습니다.
- **학교 추가** — 지금은 다원중학교 하나뿐이고, 늘리려면 서버 작업이 필요합니다.
