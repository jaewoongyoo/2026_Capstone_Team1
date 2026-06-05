#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ONPREM_DIR"
docker compose --env-file .env down
