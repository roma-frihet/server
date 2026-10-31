# PostToolUse (Write, Edit): formats Kotlin after an edit and reports what ktlint cannot fix.
. "$(dirname -- "$0")/common.sh"

relative=$(relative_path "$(json_get file_path)")
case "$relative" in
    *.kt | *.kts) ;;
    *) exit 0 ;;
esac

run_gradle ktlintFormat --console=plain --quiet
if [ "$GRADLE_CODE" -ne 0 ]; then
    stop_with_feedback "ktlintFormat failed after editing $relative:
$(failure_summary "$GRADLE_LOG")"
fi
exit 0
