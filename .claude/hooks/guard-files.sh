# PreToolUse (Write, Edit): protects migrations and keeps secrets and unsafe config out of the repo.
. "$(dirname -- "$0")/common.sh"

path=$(json_get file_path)
relative=$(relative_path "$path")
[ -n "$relative" ] || exit 0

# Pattern lists live in the hooks, docs may describe bad examples, and .env is where secrets belong.
case "$relative" in
    .claude/* | docs/* | .env) exit 0 ;;
esac

case "$relative" in
    src/main/resources/db/migration/*V*.sql)
        if [ -e "$path" ]; then
            stop_with_feedback "Blocked by harness: $relative already exists and Flyway checksums make it immutable. Add a new V<next>__<description>.sql instead."
        fi
        ;;
esac

text=$(json_get content; json_get new_string)

# The last pattern is a signed JWT, which is what Supabase anon and service_role keys look like.
for pattern in \
    'AKIA[0-9A-Z]{16}' \
    '-----BEGIN [A-Z ]*PRIVATE KEY-----' \
    'gh[pousr]_[A-Za-z0-9]{36}' \
    'sk-[A-Za-z0-9_-]{20,}' \
    'eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'; do
    if printf '%s\n' "$text" | grep -Eq -- "$pattern"; then
        stop_with_feedback "Blocked by harness: the content of $relative looks like a secret. Keep it in .env."
    fi
done

case "$relative" in
    src/main/resources/application*.yml | src/main/resources/application*.yaml | src/main/resources/application*.properties)
        if printf '%s\n' "$text" | grep -Eiq "^[[:space:]]*[a-z0-9_.-]*(password|secret)[[:space:]]*[:=][[:blank:]]*['\"]?[^[:space:]'\"#\$]"; then
            stop_with_feedback "Blocked by harness: hardcoded secret in $relative. Use \${ENV_VAR}."
        fi
        if printf '%s\n' "$text" | grep -Eiq 'ddl-auto[[:space:]]*[:=][[:space:]]*(update|create|create-drop)([^a-z-]|$)'; then
            stop_with_feedback "Blocked by harness: ddl-auto must stay validate in $relative. Change the schema with a Flyway migration."
        fi
        ;;
esac
exit 0
