#!/usr/bin/env bash
# Run MySQL CLI using credentials from .env (password never on command line).
# Usage: from trace_hq/ run:  scripts/mysql.sh [mysql-args...]
# e.g.   scripts/mysql.sh -e "SHOW DATABASES;"

set -e
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
TRACE_HQ_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
ENV_FILE="$TRACE_HQ_ROOT/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing .env at $ENV_FILE" >&2
  exit 1
fi

# Load .env (export so child and trap see them)
set -a
# shellcheck source=../.env
source "$ENV_FILE"
set +a

for var in DB_HOST DB_USER DB_PASSWORD DB_NAME; do
  if [[ -z "${!var}" ]]; then
    echo "Missing $var in .env" >&2
    exit 1
  fi
done

# Use a config file so password is never on the command line
TMPCFG=$(mktemp 2>/dev/null || mktemp -t mysql)
chmod 600 "$TMPCFG"
trap 'rm -f "$TMPCFG"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" > "$TMPCFG"

# Prefer mysql in PATH; fallback to Windows MySQL Server path when in Git Bash
MYSQL_CMD="mysql"
if ! command -v mysql &>/dev/null; then
  for p in "/c/Program Files/MySQL/MySQL Server 8.0/bin/mysql.exe" "/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe"; do
    if [[ -x "$p" ]]; then
      MYSQL_CMD="$p"
      break
    fi
  done
fi

exec "$MYSQL_CMD" --defaults-extra-file="$TMPCFG" "$DB_NAME" "$@"
