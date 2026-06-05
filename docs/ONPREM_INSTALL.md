# On-Premise Install Guide

이 문서는 고객사 내부망 서버에 범용 장비 대시보드를 설치하는 온프레미스 패키지 가이드입니다.

## 1. 제공 방식

온프레미스 MVP는 Docker Compose 기반 패키지로 제공합니다.

```text
deploy/onprem/
├── docker-compose.yml
├── .env.example
├── mosquitto/
├── nginx/
└── scripts/
```

고객사는 코드별 명령어를 직접 실행하지 않고 아래 스크립트만 사용합니다.

```bash
./scripts/install.sh
./scripts/start.sh
./scripts/stop.sh
./scripts/status.sh
./scripts/logs.sh
```

## 2. 서비스 구성

```text
장비
  -> edge-gateway
  -> mqtt-broker
  -> backend
  -> postgres / redis
  -> frontend-nginx
  -> mcp-server
```

Compose 서비스명:

```text
frontend
backend
mcp-server
edge-gateway
mqtt-broker
postgres
redis
```

컨테이너 이름은 Docker Compose 프로젝트명에 따라 자동 생성됩니다.
기존 개발용 컨테이너와 이름이 충돌하지 않도록 고정 컨테이너명은 사용하지 않습니다.

## 3. DB 구성

온프레미스에서는 PostgreSQL 컨테이너를 고객사 내부 서버에 함께 설치합니다.

```text
backend -> postgres:5432
```

테이블은 Spring Boot Backend가 시작될 때 Flyway migration으로 자동 생성됩니다.

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
backend/src/main/resources/db/migration/V2__add_dashboard_share_token.sql
```

저장 데이터:

```text
users
dashboards
equipment
sensors
dashboard_widgets
sensor_numeric_history
sensor_string_history
refresh_tokens
audit_logs
```

PostgreSQL 데이터는 Docker volume으로 보존합니다.

```text
postgres_data
```

## 4. 설치 전 요구사항

- Docker
- Docker Compose v2
- Git
- Linux 서버 또는 VM 권장

확인:

```bash
docker --version
docker compose version
```

## 5. 설치

```bash
git clone https://github.com/jaewoongyoo/2026_Capstone_Team1.git
cd 2026_Capstone_Team1/deploy/onprem
cp .env.example .env
```

`.env`를 열어 비밀번호와 JWT secret을 변경합니다.

```env
POSTGRES_PASSWORD=change-this-postgres-password
JWT_SECRET=change-this-to-a-very-long-random-secret-at-least-64-characters-for-hs512-signing
```

운영 환경에서는 반드시 고객사별로 다른 강한 값을 사용해야 합니다.
현재 백엔드는 HS512 서명을 사용하므로 `JWT_SECRET`은 최소 64바이트 이상이어야 합니다.

설치:

```bash
./scripts/install.sh
```

접속:

```text
http://<CUSTOMER_SERVER_IP>
```

초기 사용자 생성:

```bash
./scripts/create-user.sh operator Test1234! operator@example.com "Operator"
```

로그인 확인:

```bash
curl -fsS \
  -X POST http://<CUSTOMER_SERVER_IP>/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"operator","password":"Test1234!"}'
```

로컬 검증처럼 80번 대신 다른 포트를 쓰는 경우에는 `.env`의 `FRONTEND_PORT`를 기준으로 접속합니다.
예를 들어 `FRONTEND_PORT=8088`이면 `http://127.0.0.1:8088`로 접속합니다.

## 6. 실행/중지/상태 확인

시작:

```bash
./scripts/start.sh
```

중지:

```bash
./scripts/stop.sh
```

상태:

```bash
./scripts/status.sh
```

전체 로그:

```bash
./scripts/logs.sh
```

특정 서비스 로그:

```bash
./scripts/logs.sh backend
./scripts/logs.sh frontend
./scripts/logs.sh mcp-server
./scripts/logs.sh edge-gateway
./scripts/logs.sh mqtt-broker
```

Compose 서비스명은 다음과 같습니다.

```text
postgres
redis
mqtt-broker
backend
mcp-server
edge-gateway
frontend
```

## 7. 포트

기본 포트:

```text
80:   frontend-nginx
1883: MQTT
9001: MQTT WebSocket
5000: CVD SECS/GEM TCP
5001: ETCHER SECS/GEM TCP
```

내부 전용:

```text
8080: backend
3333: mcp-server
5432: postgres
6379: redis
```

외부에는 기본적으로 `80`, `1883`, `5000`, `5001`만 열면 됩니다.

## 8. Frontend Routing

프론트는 Nginx에서 정적 파일로 제공합니다.

Nginx가 내부 서비스로 프록시합니다.

```text
/api/*        -> backend:8080
/ws-stomp    -> backend:8080
/api/mcp/chat -> mcp-server:3333
```

따라서 고객사 IP나 도메인이 바뀌어도 프론트 코드를 수정하지 않아도 됩니다.

## 9. MCP MVP 동작 방식

온프레미스 MVP는 외부 LLM 없이 동작하는 규칙 기반 MCP HTTP Bridge를 사용합니다.

```text
Frontend Nexus AI
  -> mcp-server
  -> api-protocol.json 정책
  -> Backend API
```

DB 변경 작업은 바로 실행하지 않고 계획을 먼저 보여준 뒤 사용자 확인 후 적용합니다.

향후 확장:

```text
MCP_MODE=rules
MCP_MODE=ollama
MCP_MODE=external
```

MVP 기본은 rules 모드입니다.

## 10. 장비 연결

고객사 실제 장비는 Edge Gateway의 TCP 포트로 접속합니다.

```text
CVD-like equipment    -> <server-ip>:5000
ETCHER-like equipment -> <server-ip>:5001
```

현재 시뮬레이터 기준 포트이며, 실제 고객사 장비가 늘어나면 Edge Gateway 장비 포트/매핑 설정을 확장해야 합니다.

로컬 검증에서 기본 포트가 이미 사용 중이면 `.env`에서 외부 포트를 바꿀 수 있습니다.

```env
EDGE_GATEWAY_CVD_PORT=15000
EDGE_GATEWAY_ETCHER_PORT=15001
```

이 경우 장비 시뮬레이터는 같은 포트 환경변수를 넘겨 실행합니다.

```bash
EDGE_GATEWAY_HOST=127.0.0.1 \
EDGE_GATEWAY_CVD_PORT=15000 \
EDGE_GATEWAY_ETCHER_PORT=15001 \
python3 simulator/secsgem_device.py
```

## 11. 백업/복구

DB 백업:

```bash
./scripts/backup-db.sh
```

백업 파일은 다음 위치에 생성됩니다.

```text
deploy/onprem/backups/dashboard_YYYYMMDD_HHMMSS.sql
```

복구:

```bash
./scripts/restore-db.sh backups/dashboard_YYYYMMDD_HHMMSS.sql
```

## 12. 업데이트

새 버전을 배포할 때:

```bash
git pull
cd deploy/onprem
docker compose --env-file .env build
docker compose --env-file .env up -d
```

중요 업데이트 전에는 DB 백업을 먼저 수행합니다.

```bash
./scripts/backup-db.sh
```

## 13. 초기 통합 테스트

설치 후 확인:

```bash
./scripts/status.sh
```

브라우저 접속:

```text
http://<CUSTOMER_SERVER_IP>
```

MCP 채팅 테스트:

```text
장비 목록 보여줘
현재 대시보드 세팅 상태 점검해줘
지원하는 위젯 종류 보여줘
```

HTTP로 직접 MCP를 테스트할 때는 로그인 후 받은 JWT를 `Authorization` 헤더로 전달합니다.

```bash
TOKEN=$(curl -fsS \
  -X POST http://<CUSTOMER_SERVER_IP>/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"operator","password":"Test1234!"}' \
  | node -e 'let s="";process.stdin.on("data",d=>s+=d);process.stdin.on("end",()=>console.log(JSON.parse(s).data.accessToken))')

curl -fsS \
  -X POST http://<CUSTOMER_SERVER_IP>/api/mcp/chat \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d '{"message":"장비 목록 보여줘"}'
```

시뮬레이터로 장비 데이터 테스트:

```bash
EDGE_GATEWAY_HOST=<CUSTOMER_SERVER_IP> python3 simulator/secsgem_device.py
```

## 14. Troubleshooting

### 컨테이너가 뜨지 않음

```bash
./scripts/status.sh
./scripts/logs.sh backend
```

### DB 연결 실패

확인:

```bash
./scripts/logs.sh postgres
./scripts/logs.sh backend
```

`.env`의 DB 값 확인:

```env
POSTGRES_DB
POSTGRES_USER
POSTGRES_PASSWORD
```

### 프론트는 뜨지만 API 실패

Nginx 프록시 확인:

```bash
./scripts/logs.sh frontend
./scripts/logs.sh backend
```

Health 확인:

```bash
curl http://localhost/api/public/health
```

### MCP 채팅 실패

```bash
./scripts/logs.sh mcp-server
curl http://localhost/api/public/health
```

프론트 요청 경로:

```text
/api/mcp/chat
```

### MQTT 수신 실패

```bash
./scripts/logs.sh mqtt-broker
./scripts/logs.sh edge-gateway
./scripts/logs.sh backend
```

포트 확인:

```bash
nc -vz <server-ip> 1883
nc -vz <server-ip> 5000
nc -vz <server-ip> 5001
```

## 15. 운영 체크리스트

```text
[ ] .env 비밀번호 변경
[ ] JWT_SECRET 변경
[ ] Docker Compose 기동 성공
[ ] Frontend 접속 성공
[ ] Backend health 성공
[ ] MQTT 포트 접근 가능
[ ] Edge Gateway 포트 접근 가능
[ ] 장비 또는 시뮬레이터 연결 성공
[ ] 실시간 위젯 갱신 확인
[ ] MCP 채팅 조회 성공
[ ] DB 백업 스크립트 성공
```
