# Environment Setup Guide

팀원이 같은 레포를 clone한 뒤 동일한 환경에서 실행하기 위한 세팅 문서입니다.

이 문서는 로컬 개발 PC에서 다음 구성으로 테스트하는 것을 기준으로 합니다.

```text
Frontend: local Vite dev server
MCP HTTP Bridge: local Node server
Simulator: local Python process
Backend API: deployed EC2 backend
Edge Gateway / MQTT: deployed EC2 containers
```

## 1. Clone

```bash
git clone https://github.com/jaewoongyoo/2026_Capstone_Team1.git
cd 2026_Capstone_Team1
```

## 2. Required Software

### Node.js

권장 버전:

```text
Node.js >= 20
npm >= 10
```

확인:

```bash
node -v
npm -v
```

### Python

권장 버전:

```text
Python >= 3.11
```

확인:

```bash
python3 --version
```

### Docker

로컬에서 Edge Gateway/MQTT를 직접 띄울 때 필요합니다.

현재 기본 통합 테스트는 EC2 Edge Gateway/MQTT를 사용하므로 Docker는 필수는 아니지만, 인프라 테스트 담당자는 설치해야 합니다.

확인:

```bash
docker --version
docker compose version
```

### Ollama

선택 사항입니다.

프론트 채팅 UI는 `mcp-server/src/http.ts`의 규칙 기반 HTTP Bridge를 사용하므로 Ollama가 없어도 테스트할 수 있습니다. 다만 `npm run ollama-chat` CLI 테스트를 하려면 Ollama가 필요합니다.

확인:

```bash
ollama --version
```

권장 모델:

```bash
ollama pull qwen2.5:3b
```

## 3. Environment Variables

### Shared Integration Values

현재 팀 테스트 기준값입니다.

```text
DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io
VITE_API_BASE_URL=https://api.43.201.141.9.nip.io
VITE_WS_URL=https://api.43.201.141.9.nip.io/ws-stomp
VITE_MCP_CHAT_URL=http://localhost:3333/api/mcp/chat
MCP_HTTP_PORT=3333
EDGE_GATEWAY_HOST=43.201.141.9
```

### Secrets

아래 값은 Git에 올리지 않습니다.

```text
테스트 계정 ID/PW
EC2 PEM key
DB password
JWT secret
운영 MQTT username/password
```

필요한 값은 팀 내부 채널로 별도 공유받아 사용하세요.

## 4. Install Packages

### MCP Server

```bash
cd mcp-server
npm ci
```

검증:

```bash
npm run typecheck
```

### Frontend

```bash
cd frontend
npm ci
```

검증:

```bash
npm run build
```

### Simulator

현재 시뮬레이터는 표준 라이브러리 중심으로 실행됩니다.

MQTT publish를 로컬에서 직접 테스트하거나 Edge Gateway를 로컬로 띄우는 경우 `paho-mqtt`가 필요할 수 있습니다.

```bash
python3 -m pip install paho-mqtt
```

가상환경을 쓰는 경우:

```bash
python3 -m venv venv
source venv/bin/activate
python3 -m pip install -r requirements.txt
python3 -m pip install paho-mqtt
```

## 5. Run Order

통합 테스트는 터미널 3개를 사용합니다.

### Terminal 1: MCP HTTP Bridge

```bash
cd mcp-server

DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io \
DASHBOARD_TLS_REJECT_UNAUTHORIZED=false \
MCP_HTTP_PORT=3333 \
npm run http
```

정상 로그:

```text
MCP HTTP bridge listening on http://localhost:3333
```

### Terminal 2: Frontend

```bash
cd frontend

VITE_API_BASE_URL=https://api.43.201.141.9.nip.io \
VITE_WS_URL=https://api.43.201.141.9.nip.io/ws-stomp \
VITE_MCP_CHAT_URL=http://localhost:3333/api/mcp/chat \
npm run dev
```

접속:

```text
http://localhost:5173
```

### Terminal 3: Simulator

```bash
cd 2026_Capstone_Team1

EDGE_GATEWAY_HOST=43.201.141.9 python3 simulator/secsgem_device.py
```

## 6. Login

프론트에서 테스트 계정으로 로그인합니다.

테스트 계정 정보는 Git 문서에 적지 않습니다. 팀 내부 채널에서 공유받아 사용하세요.

## 7. MCP Chat Test Commands

프론트 우측 하단 `Nexus AI` 채팅에서 실행합니다.

```text
장비 목록 보여줘
```

```text
대시보드 2의 CVD 장비 센서 목록 보여줘
```

```text
대시보드 2의 ETCHER 장비 센서 목록 보여줘
```

```text
현재 대시보드 세팅 상태 점검해줘
```

```text
지원하는 위젯 종류 보여줘
```

```text
대시보드 2의 CVD 장비 Chamber_Pressure 센서를 세로 막대그래프로 위젯 구성해줘
```

```text
대시보드 2의 ETCHER 장비 Chamber_Pressure 센서를 도넛 그래프로 위젯 구성해줘
```

## 8. Port Usage

```text
3333: Local MCP HTTP Bridge
5173: Local Vite frontend
8080: Backend API server
1883: MQTT Broker
9001: MQTT WebSocket
5000: CVD Edge Gateway TCP
5001: ETCHER Edge Gateway TCP
```

포트 충돌 확인:

```bash
lsof -i :3333
lsof -i :5173
```

프로세스 종료:

```bash
kill -9 <PID>
```

## 9. Backend Local Run

기본 통합 테스트는 배포된 백엔드를 사용합니다.

백엔드를 로컬에서 직접 실행해야 할 때만 아래를 사용합니다.

```bash
cd backend
./gradlew bootRun
```

로컬 백엔드를 쓰는 경우 프론트/MCP 환경변수도 로컬 주소로 바꿔야 합니다.

```text
DASHBOARD_API_BASE_URL=http://localhost:8080
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws-stomp
```

## 10. EC2 Infrastructure

EC2에 Edge Gateway/MQTT를 다시 배포할 때:

```bash
./infra/deploy.sh ec2-user@<EC2_PUBLIC_IP> <PEM_KEY_PATH>
```

주의:

- PEM key는 Git에 올리지 않습니다.
- 보안 그룹 포트가 열려 있어야 합니다.
- 1883, 5000, 5001은 외부 접근 대상입니다.

## 11. Common Errors

### `EADDRINUSE: address already in use :::3333`

이미 MCP 서버가 실행 중입니다.

```bash
lsof -i :3333
kill -9 <PID>
```

### MCP 채팅이 fallback만 반환

- MCP 서버 재시작 여부 확인
- `VITE_MCP_CHAT_URL` 확인
- 프론트 로그인 여부 확인
- 명령어에 장비명 또는 dashboardId가 포함되어 있는지 확인

### 장비 데이터가 오래됨

- 시뮬레이터 실행 여부 확인
- `EDGE_GATEWAY_HOST=43.201.141.9` 확인
- EC2 Edge Gateway/MQTT 상태 확인

### 장비 ID가 문서와 다름

`equipmentEntityId`는 백엔드 DB에서 생성되는 숫자 ID입니다. 장비를 삭제 후 재등록하면 바뀔 수 있습니다.

항상 먼저 아래 명령으로 현재 값을 확인하세요.

```text
장비 목록 보여줘
```

## 12. Clean Install Checklist

새 작업자가 처음 세팅할 때 아래 순서로 확인합니다.

```text
[ ] Node.js 20 이상 설치
[ ] Python 3.11 이상 설치
[ ] Git clone 완료
[ ] mcp-server npm ci 완료
[ ] frontend npm ci 완료
[ ] MCP HTTP Bridge 실행 성공
[ ] Frontend dev server 실행 성공
[ ] 프론트 로그인 성공
[ ] Simulator 실행 성공
[ ] 장비 목록 조회 성공
[ ] 센서 목록 조회 성공
[ ] 위젯 생성 계획 확인 성공
[ ] 적용 버튼으로 위젯 변경 성공
```

