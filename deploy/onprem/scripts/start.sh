#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ONPREM_DIR"

if [ ! -f .env ]; then
  echo "Missing .env. Run ./scripts/install.sh first or copy .env.example to .env."
  exit 1
fi

docker compose --env-file .env up -d
