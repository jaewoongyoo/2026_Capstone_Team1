#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_FILE="${1:-}"

if [ -z "$BACKUP_FILE" ]; then
  echo "Usage: ./scripts/restore-db.sh <backup.sql>"
  exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Backup file not found: $BACKUP_FILE"
  exit 1
fi

cd "$ONPREM_DIR"

set -a
. ./.env
set +a

docker compose --env-file .env exec -T postgres \
  psql -U "${POSTGRES_USER:-dashboard}" "${POSTGRES_DB:-dashboard}" < "$BACKUP_FILE"

echo "Restore complete: $BACKUP_FILE"
