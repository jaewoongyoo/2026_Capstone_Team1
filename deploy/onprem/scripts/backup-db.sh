#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"
BACKUP_DIR="$ONPREM_DIR/backups"
TIMESTAMP="$(date +%Y%m%d_%H%M%S)"

cd "$ONPREM_DIR"
mkdir -p "$BACKUP_DIR"

set -a
. ./.env
set +a

BACKUP_FILE="$BACKUP_DIR/dashboard_${TIMESTAMP}.sql"

docker compose --env-file .env exec -T postgres \
  pg_dump -U "${POSTGRES_USER:-dashboard}" "${POSTGRES_DB:-dashboard}" > "$BACKUP_FILE"

echo "Backup created: $BACKUP_FILE"
