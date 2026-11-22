# Stop: API 경로나 Request/Response를 바꾼 브랜치가 API 테스터를 함께 고치지 않으면 한 번 멈춘다.
# CLAUDE.md 규칙이었지만 check로 잡히지 않아 두 번 빠뜨렸다(docs/failures/2026-11-15-api-테스터-동반-수정-누락.md).
# 테스터와 상관없는 변경(예: 컨트롤러 안의 주석)일 수 있으므로, 같은 변경 묶음에는 한 번만 멈춘다.
. "$(dirname -- "$0")/common.sh"

cd "$PROJECT_ROOT" || exit 0
command -v git >/dev/null 2>&1 || exit 0

base=$(git merge-base HEAD origin/develop 2>/dev/null || git merge-base HEAD develop 2>/dev/null)
[ -n "$base" ] || exit 0

# 커밋한 것, 아직 커밋하지 않은 것, 새로 만든 파일을 모두 본다.
changed=$( { git diff --name-only "$base"; git ls-files --others --exclude-standard; } | sort -u)

api_changed=$(printf '%s\n' "$changed" | grep -E '^src/main/kotlin/.*(/rqrs/.*\.kt|Controller\.kt)$' | head -n 20)
[ -n "$api_changed" ] || exit 0
printf '%s\n' "$changed" | grep -qx 'tools/api-tester/index.html' && exit 0

state_dir="$PROJECT_ROOT/build/harness"
marker="$state_dir/api-tester-warned"
mkdir -p "$state_dir"
signature=$(printf '%s\n' "$api_changed" | cksum)
if [ -f "$marker" ] && [ "$(cat "$marker")" = "$signature" ]; then
    exit 0
fi
printf '%s' "$signature" >"$marker"

stop_with_feedback "API 테스터를 함께 고치지 않았습니다. 이 브랜치에서 바뀐 API 파일:
$api_changed
CLAUDE.md 규칙대로 tools/api-tester/index.html과 README의 API 테스터 설명, docs/frontend/README.md의 엔드포인트 표를 함께 고치세요.
테스터와 상관없는 변경이라면 그 이유를 사용자에게 말하고 그대로 끝내면 됩니다(같은 변경으로는 다시 멈추지 않습니다)."
