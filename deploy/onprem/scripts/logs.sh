#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"
SERVICE="${1:-}"

cd "$ONPREM_DIR"

if [ -n "$SERVICE" ]; then
  docker compose --env-file .env logs -f "$SERVICE"
else
  docker compose --env-file .env logs -f
fi
