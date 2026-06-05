#!/usr/bin/env sh
set -eu

SCRIPT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
ONPREM_DIR="$(dirname "$SCRIPT_DIR")"

cd "$ONPREM_DIR"

if [ ! -f .env ]; then
  cp .env.example .env
  echo "Created .env from .env.example"
  echo "Edit deploy/onprem/.env before production use."
fi

docker compose --env-file .env build
docker compose --env-file .env up -d

echo
echo "Install complete."
echo "Frontend: http://localhost:${FRONTEND_PORT:-80}"
echo "Status:   ./scripts/status.sh"
