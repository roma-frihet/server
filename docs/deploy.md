# 배포

GitHub Actions로 빌드하고 GCP Cloud Run에 배포한다. DB와 Auth는 Supabase를 쓴다.

| 워크플로 | 실행 시점 | 하는 일 |
|---|---|---|
| `ci.yml` | PR, `develop`/`release/**`/`production` push | `./gradlew check` |
| `deploy.yml` | Actions 탭 → Deploy → **Run workflow** | `check` → 이미지 빌드 → Artifact Registry 푸시 → Cloud Run 배포 → 헬스 체크 |

- `workflow_dispatch` 워크플로는 기본 브랜치(`develop`)에 파일이 있어야 Run workflow 버튼이 보인다. 실행할 때는 브랜치를 `production`으로 고른다.
- production 환경은 `production` 브랜치에서만 배포되고, GitHub Environment 승인을 받아야 진행된다.
- CLI로 실행: `gh workflow run deploy.yml --ref production -f environment=production`
- 릴리즈 순서: `develop`에서 `release/x.y.z`를 만들고 → `production`에 PR로 병합 → Deploy를 `production` 브랜치, `production` 환경으로 실행 → 승인

## 환경 구성

환경마다 GitHub Environment 하나, Cloud Run 서비스 하나, Supabase 프로젝트 하나를 둔다. 워크플로는 같고 Environment 변수만 다르다.

| | production | development (나중에) |
|---|---|---|
| GitHub Environment | `production` | `development` |
| Cloud Run 서비스 | `server-prod` | `server-dev` |
| 배포 브랜치 | `production` | `develop` |
| 승인 | 필요 | 불필요 |

development를 추가할 때는 아래 설정을 `server-dev` 이름으로 반복하고, `deploy.yml`의 `push` 주석을 푼다.

## 1. GCP 최초 설정 (환경마다 1회)

```bash
PROJECT_ID=<gcp-project-id>
PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format 'value(projectNumber)')
REGION=asia-northeast3          # 서울. Supabase 프로젝트도 같은 지역에 둔다
SERVICE=server-prod
GITHUB_REPO=roma-frihet/server

gcloud config set project "$PROJECT_ID"
gcloud services enable run.googleapis.com artifactregistry.googleapis.com \
  secretmanager.googleapis.com iamcredentials.googleapis.com

# 이미지 저장소 (환경끼리 공유해도 된다)
gcloud artifacts repositories create server --repository-format docker --location "$REGION"
```

### 서비스 계정

GitHub가 쓰는 배포용 계정과 Cloud Run 컨테이너가 쓰는 실행용 계정을 나눈다.

```bash
gcloud iam service-accounts create github-deployer
gcloud iam service-accounts create "$SERVICE-runtime"

DEPLOYER=github-deployer@$PROJECT_ID.iam.gserviceaccount.com
RUNTIME=$SERVICE-runtime@$PROJECT_ID.iam.gserviceaccount.com

# --allow-unauthenticated로 공개 IAM 정책을 거는 데 run.admin이 필요하다
gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:$DEPLOYER" --role roles/run.admin
gcloud projects add-iam-policy-binding "$PROJECT_ID" --member "serviceAccount:$DEPLOYER" --role roles/artifactregistry.writer
gcloud iam service-accounts add-iam-policy-binding "$RUNTIME" --member "serviceAccount:$DEPLOYER" --role roles/iam.serviceAccountUser
```

### Workload Identity Federation

서비스 계정 키 파일 없이 GitHub OIDC 토큰으로 인증한다. 이 저장소에서 온 토큰만 받는다.

```bash
gcloud iam workload-identity-pools create github --location global
gcloud iam workload-identity-pools providers create-oidc github \
  --location global --workload-identity-pool github \
  --issuer-uri https://token.actions.githubusercontent.com \
  --attribute-mapping "google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition "assertion.repository=='$GITHUB_REPO'"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOYER" \
  --role roles/iam.workloadIdentityUser \
  --member "principalSet://iam.googleapis.com/projects/$PROJECT_NUMBER/locations/global/workloadIdentityPools/github/attribute.repository/$GITHUB_REPO"
```

### 비밀값 (Secret Manager)

이름은 `{서비스명}-{키}`다. 값은 셸 기록에 남지 않게 `read -s`로 입력한다.

```bash
for key in db-password supabase-service-role-key; do
  read -rsp "$SERVICE-$key: " value; echo
  printf '%s' "$value" | gcloud secrets create "$SERVICE-$key" --data-file=-
  gcloud secrets add-iam-policy-binding "$SERVICE-$key" \
    --member "serviceAccount:$RUNTIME" --role roles/secretmanager.secretAccessor
done
```

값을 바꿀 때는 `gcloud secrets versions add`로 새 버전을 올리고 다시 배포한다.

## 2. Supabase 연결 주소

Cloud Run 기본 아웃바운드는 IPv4만 쓰는데, Supabase Direct connection은 IPv6 전용이다. **Session pooler(5432)** 주소를 쓴다. CLAUDE.md 규칙대로 Transaction pooler(6543)는 Flyway에 쓰지 않는다.

- 대시보드 → Connect → Session pooler
- `DB_URL`: `jdbc:postgresql://aws-0-<region>.pooler.supabase.com:5432/postgres`
- `DB_USERNAME`: `postgres.<project-ref>`

인스턴스마다 Hikari 연결이 최대 10개 열린다. `CLOUD_RUN_MAX_INSTANCES × 10`이 Supabase 요금제의 pooler 연결 한도를 넘지 않게 한다.

## 3. GitHub Environment 설정

Settings → Environments → **New environment** → `production`

- **Required reviewers**: 승인할 사람을 지정한다. public 저장소라 Free 요금제에서도 쓸 수 있다.
- **Deployment branches and tags**: Selected branches → `production`
- **Environment variables**:

| 이름 | 예시 |
|---|---|
| `GCP_PROJECT_ID` | `roma-frihet-prod` |
| `GCP_REGION` | `asia-northeast3` |
| `ARTIFACT_REPOSITORY` | `server` |
| `CLOUD_RUN_SERVICE` | `server-prod` |
| `CLOUD_RUN_MAX_INSTANCES` | `2` (생략하면 2) |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | `projects/<PROJECT_NUMBER>/locations/global/workloadIdentityPools/github/providers/github` |
| `GCP_DEPLOYER_SERVICE_ACCOUNT` | `github-deployer@<PROJECT_ID>.iam.gserviceaccount.com` |
| `CLOUD_RUN_RUNTIME_SERVICE_ACCOUNT` | `server-prod-runtime@<PROJECT_ID>.iam.gserviceaccount.com` |
| `DB_URL` | 2번의 Session pooler JDBC 주소 |
| `DB_USERNAME` | `postgres.<project-ref>` |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` |

비밀값은 GitHub에 넣지 않고 Secret Manager에만 둔다. Workload Identity Provider 경로와 서비스 계정 이메일은 비밀값이 아니다.

## 참고

- 원격 Supabase가 레거시 HS256 JWT secret을 쓴다면 `$SERVICE-supabase-jwt-secret` 시크릿을 만들고 `deploy.yml`의 `--set-secrets`에 `SUPABASE_JWT_SECRET`을 추가한다. 새 JWT Signing Keys(JWKS)를 쓰면 필요 없다.
- `min-instances 0`이라 요청이 없으면 인스턴스가 0으로 줄고, 첫 요청에 Spring 기동 시간(수 초)만큼 콜드스타트가 생긴다. 줄이려면 `--min-instances 1`로 바꾸되 상시 과금된다.
- Flyway 마이그레이션은 앱 기동 시 실행된다. 마이그레이션이 실패하면 새 리비전이 준비되지 않아 이전 리비전이 계속 서비스한다.
