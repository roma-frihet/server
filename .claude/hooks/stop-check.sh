# Stop: Claude cannot finish a turn that changed the code until `gradlew check` passes.
. "$(dirname -- "$0")/common.sh"

max_attempts=3
state_dir="$PROJECT_ROOT/build/harness"
success_marker="$state_dir/last-success"
attempts_file="$state_dir/attempts"

cd "$PROJECT_ROOT" || exit 1

# Directories are included because deleting a file only changes its parent's timestamp.
if [ -f "$success_marker" ]; then
    changed=$(find src config build.gradle.kts settings.gradle.kts -newer "$success_marker" 2>/dev/null | head -n 1)
    [ -n "$changed" ] || exit 0
fi

run_gradle check --continue --console=plain

if [ "$GRADLE_CODE" -eq 0 ]; then
    date >"$success_marker"
    rm -f "$attempts_file"
    exit 0
fi

attempts=1
if [ -f "$attempts_file" ]; then
    attempts=$(($(cat "$attempts_file") + 1))
fi

# Stops the loop when the failure is something Claude cannot fix alone.
if [ "$attempts" -ge "$max_attempts" ]; then
    rm -f "$attempts_file"
    printf '{"systemMessage":"Harness: gradlew check still fails after %s attempts. Log: %s"}\n' "$max_attempts" "$GRADLE_LOG"
    exit 0
fi

echo "$attempts" >"$attempts_file"
stop_with_feedback "gradlew check failed (attempt $attempts of $max_attempts). Fix the cause; do not weaken the rule.
$(failure_summary "$GRADLE_LOG")"
