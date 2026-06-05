#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ONPREM_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${ONPREM_DIR}/.env"

if [[ -f "${ENV_FILE}" ]]; then
  # shellcheck disable=SC1090
  set -a
  source "${ENV_FILE}"
  set +a
fi

USERNAME="${1:-}"
PASSWORD="${2:-}"
EMAIL="${3:-}"
FULL_NAME="${4:-${USERNAME}}"
BASE_URL="${DASHBOARD_PUBLIC_URL:-http://127.0.0.1:${FRONTEND_PORT:-80}}"

if [[ -z "${USERNAME}" || -z "${PASSWORD}" || -z "${EMAIL}" ]]; then
  cat <<USAGE
Usage:
  ./scripts/create-user.sh <username> <password> <email> [fullName]

Example:
  ./scripts/create-user.sh operator Test1234! operator@example.com "Operator"

Environment:
  DASHBOARD_PUBLIC_URL can override the target URL.
  Default URL: ${BASE_URL}
USAGE
  exit 1
fi

curl -fsS \
  -X POST "${BASE_URL}/api/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${USERNAME}\",\"password\":\"${PASSWORD}\",\"email\":\"${EMAIL}\",\"fullName\":\"${FULL_NAME}\"}"

echo
