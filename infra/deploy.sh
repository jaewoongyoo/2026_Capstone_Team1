#!/bin/bash
# =============================================================================
# deploy.sh — EC2 배포 스크립트
# 사용법: ./infra/deploy.sh ec2-user@{EC2-IP} {키파일.pem}
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

EC2_USER_HOST=$1   # 예: ec2-user@13.125.xxx.xxx
PEM_KEY=$2         # 예: ~/.ssh/my-key.pem

if [ -z "$EC2_USER_HOST" ] || [ -z "$PEM_KEY" ]; then
  echo "사용법: ./infra/deploy.sh ec2-user@{EC2-IP} {키파일.pem}"
  exit 1
fi

echo "========================================================"
echo "  EC2 배포 시작 → $EC2_USER_HOST"
echo "========================================================"

# ── 1. EC2에 디렉토리 생성 ────────────────────────────────────────
echo "[1/4] EC2 디렉토리 생성..."
ssh -i "$PEM_KEY" "$EC2_USER_HOST" "mkdir -p ~/simulator/infra/mosquitto/config ~/simulator/infra/mosquitto/data ~/simulator/infra/mosquitto/log"

# ── 2. 파일 전송 ──────────────────────────────────────────────────
echo "[2/4] 파일 전송 중..."
scp -i "$PEM_KEY" -r \
  "$ROOT_DIR/edge" \
  "$ROOT_DIR/simulator" \
  "$ROOT_DIR/common" \
  "$ROOT_DIR/requirements.txt" \
  "$EC2_USER_HOST:~/simulator/"

scp -i "$PEM_KEY" \
  "$SCRIPT_DIR/docker-compose.yml" \
  "$EC2_USER_HOST:~/simulator/infra/"

scp -i "$PEM_KEY" \
  "$SCRIPT_DIR/mosquitto/config/mosquitto.conf" \
  "$EC2_USER_HOST:~/simulator/infra/mosquitto/config/"

# ── 3. EC2에 Docker 설치 확인 및 컨테이너 실행 ───────────────────
echo "[3/4] EC2에서 Docker 확인 및 컨테이너 실행..."
ssh -i "$PEM_KEY" "$EC2_USER_HOST" << 'REMOTE'
  set -e

  # Docker 없으면 설치
  if ! command -v docker &> /dev/null; then
    echo "Docker 설치 중..."
    if command -v apt-get &> /dev/null; then
      sudo apt-get update -y
      sudo apt-get install -y docker.io docker-compose-plugin
    elif command -v yum &> /dev/null; then
      sudo yum update -y
      sudo yum install -y docker
    elif command -v dnf &> /dev/null; then
      sudo dnf install -y docker
    else
      echo "지원하지 않는 OS입니다. Docker를 수동 설치해주세요."
      exit 1
    fi
    sudo systemctl enable docker
    sudo systemctl start docker
    sudo usermod -aG docker $USER
    echo "Docker 설치 완료 — 재접속 필요할 수 있음"
  fi

  # Docker Compose 확인
  if docker compose version &> /dev/null; then
    COMPOSE="docker compose"
  elif command -v docker-compose &> /dev/null; then
    COMPOSE="docker-compose"
  else
    echo "Docker Compose 설치 중..."
    mkdir -p ~/.docker/cli-plugins
    ARCH="$(uname -m)"
    case "$ARCH" in
      x86_64) COMPOSE_ARCH="x86_64" ;;
      aarch64|arm64) COMPOSE_ARCH="aarch64" ;;
      *)
        echo "지원하지 않는 아키텍처입니다: $ARCH"
        exit 1
        ;;
    esac
    curl -SL "https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-${COMPOSE_ARCH}" -o ~/.docker/cli-plugins/docker-compose
    chmod +x ~/.docker/cli-plugins/docker-compose
    COMPOSE="docker compose"
  fi

  cd ~/simulator/infra

  # 기존 컨테이너 정리
  $COMPOSE down 2>/dev/null || true
  docker rm -f edge-gateway mqtt-broker 2>/dev/null || true

  # 빌드 및 실행
  $COMPOSE up -d --build

  echo ""
  echo "실행 중인 컨테이너:"
  docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
REMOTE

echo "[4/4] 배포 완료!"
echo ""
echo "========================================================"
echo "  확인 명령어"
echo "  docker logs edge-gateway -f"
echo "  docker logs mqtt-broker -f"
echo ""
echo "  MQTT 구독 테스트:"
echo "  docker exec -it mqtt-broker mosquitto_sub -t 'factory/equipment/#' -v"
echo ""
echo "  로컬 시뮬레이터 실행:"
echo "  EDGE_GATEWAY_HOST=${EC2_USER_HOST#*@} python3 simulator/secsgem_device.py"
echo "========================================================"
