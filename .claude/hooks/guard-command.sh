# PreToolUse (Bash): blocks commands that destroy work or skip the checks.
. "$(dirname -- "$0")/common.sh"

command_text=$(json_get command)

check() {
    if printf '%s\n' "$command_text" | grep -Eiq -- "$1"; then
        stop_with_feedback "Blocked by harness: $2. If this is really needed, ask the user to run it."
    fi
}

space='[[:space:]]'
check "git$space+push($space.*)?$space(--force|-f)($space|\$)" 'force push; use --force-with-lease after asking the user'
check "git$space+reset$space+--hard" 'git reset --hard discards work'
check "git$space+clean$space+-[a-z]*f" 'git clean deletes untracked files'
check '--no-verify' '--no-verify skips hooks; fix the cause instead'
check "flywayClean|flyway$space+clean|clean-disabled[=:]$space*false" 'Flyway clean drops every object in the schema'
check "drop$space+(database|schema)($space|;|\$)" 'dropping a database or schema is destructive'
check "docker$space+volume$space+(rm|prune)|compose$space+down$space.*(-v|--volumes)($space|\$)" 'removing Docker volumes deletes data'
check "gradle(w|\.ps1)?$space.*$space-x$space+:?(check|test|ktlint|detekt)" 'do not exclude verification tasks; fix the failure'
exit 0
