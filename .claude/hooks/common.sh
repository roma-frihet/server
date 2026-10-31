# Shared helpers for hooks. These run inside WSL Ubuntu.

PROJECT_ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/../.." && pwd)
HOOK_INPUT=$(cat)

# Prints a string field from the hook input, or nothing when it is absent.
json_get() {
    if command -v jq >/dev/null 2>&1; then
        printf '%s' "$HOOK_INPUT" | jq -r ".tool_input.$1 // empty"
    elif command -v python3 >/dev/null 2>&1; then
        printf '%s' "$HOOK_INPUT" | python3 -c '
import json, sys
print(json.load(sys.stdin).get("tool_input", {}).get(sys.argv[1]) or "")' "$1"
    else
        echo "Harness hook needs jq or python3." >&2
        exit 1
    fi
}

# Exit code 2 blocks the action and shows the message to Claude.
stop_with_feedback() {
    printf '%s\n' "$1" >&2
    exit 2
}

# Prints the path relative to the project root, or nothing when it is outside.
relative_path() {
    case "$1" in
        "$PROJECT_ROOT"/*) printf '%s' "${1#"$PROJECT_ROOT"/}" ;;
    esac
}

# Runs Gradle with output in a file and sets GRADLE_LOG and GRADLE_CODE.
# Logs stay out of build/ so an open file cannot make `gradlew clean` fail.
run_gradle() {
    log_dir="${TMPDIR:-/tmp}/roma-frihet-harness"
    mkdir -p "$log_dir" "$PROJECT_ROOT/build/harness"
    rm -f "$log_dir"/gradle-*.log
    GRADLE_LOG="$log_dir/gradle-$$.log"
    if [ ! -f "$PROJECT_ROOT/gradlew" ]; then
        echo "Gradle wrapper not found" >"$GRADLE_LOG"
        GRADLE_CODE=1
        return
    fi
    (cd "$PROJECT_ROOT" && sh ./gradlew "$@") >"$GRADLE_LOG" 2>&1 </dev/null
    GRADLE_CODE=$?
}

# Keeps the lines that explain a failure and drops Gradle's boilerplate.
failure_summary() {
    grep -Ev '^(\* Try:|> Run with|> Get more help|Deprecated Gradle|You can use|For more on this|\[Incubating\]|Successfully generated|[[:space:]]*$)' "$1" |
        tail -n 40
}
