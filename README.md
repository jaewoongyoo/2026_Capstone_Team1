# Capstone Team 1 Dashboard Workspace

범용 장비 대시보드 통합 워크스페이스입니다.

이 레포는 프론트엔드, 백엔드, MCP 서버, 장비 시뮬레이터, 엣지 게이트웨이 배포 파일을 한 번에 공유하기 위한 상위 프로젝트입니다.

## Project Structure

```text
.
├── backend/      # Spring Boot backend
├── frontend/     # React + Vite frontend
├── mcp-server/   # Dashboard MCP server / HTTP bridge
├── simulator/    # SECS/GEM 장비 시뮬레이터
├── edge/         # Edge Gateway source
├── infra/        # EC2 배포용 Docker Compose / Mosquitto config
├── common/       # Simulator / Edge shared config
└── docs/         # 통합 테스트 및 운영 문서
```

## Default Integration Target

현재 팀 통합 테스트는 배포된 백엔드와 EC2 Edge Gateway/MQTT를 기준으로 합니다.

```text
Backend API: https://api.43.201.141.9.nip.io
Swagger:     https://api.43.201.141.9.nip.io/swagger-ui/index.html
WebSocket:   https://api.43.201.141.9.nip.io/ws-stomp
MCP local:   http://localhost:3333/api/mcp/chat
```

테스트 계정, EC2 PEM, 운영 비밀값은 Git에 커밋하지 않습니다. 팀 내부 채널로 별도 공유받아 사용하세요.

## Requirements

- Node.js 20 이상
- npm
- Python 3.11 이상 권장
- Docker, Docker Compose
- Ollama는 선택 사항입니다. 로컬 CLI 자연어 테스트에만 필요합니다.

## Quick Start

### 1. Clone

```bash
git clone https://github.com/jaewoongyoo/2026_Capstone_Team1.git
cd 2026_Capstone_Team1
```

### 2. MCP HTTP Bridge 실행

프론트 채팅 UI가 호출하는 로컬 MCP HTTP 서버입니다.

```bash
cd mcp-server
npm ci
DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io \
DASHBOARD_TLS_REJECT_UNAUTHORIZED=false \
MCP_HTTP_PORT=3333 \
npm run http
```

성공하면 다음 로그가 출력됩니다.

```text
MCP HTTP bridge listening on http://localhost:3333
```

### 3. Frontend 실행

새 터미널에서 실행합니다.

```bash
cd frontend
npm ci
VITE_API_BASE_URL=https://api.43.201.141.9.nip.io \
VITE_WS_URL=https://api.43.201.141.9.nip.io/ws-stomp \
VITE_MCP_CHAT_URL=http://localhost:3333/api/mcp/chat \
npm run dev
```

브라우저에서 접속합니다.

```text
http://localhost:5173
```

### 4. 장비 시뮬레이터 실행

EC2에 올라간 Edge Gateway로 장비 데이터를 전송합니다.

```bash
cd 2026_Capstone_Team1
EDGE_GATEWAY_HOST=43.201.141.9 python3 simulator/secsgem_device.py
```

주의: `simulator/run_simulator.py`는 로컬 Edge Gateway까지 함께 띄우는 통합 실행 파일입니다. 현재 팀 테스트 구조에서는 EC2 Edge Gateway를 사용하므로 `secsgem_device.py` 실행을 기본으로 사용합니다.

## Useful MCP Chat Commands

프론트 우측 하단의 `Nexus AI` 채팅에서 테스트할 수 있습니다.

```text
장비 목록 보여줘
equipmentEntityId 9 장비 센서 목록 보여줘
현재 대시보드 세팅 상태 점검해줘
지원하는 위젯 종류 보여줘
dashboardId 2의 CVD 장비 Chamber_Pressure 센서를 막대그래프로 위젯 구성해줘
dashboardId 2의 ETCHER 장비 Chamber_Pressure 센서를 도넛 그래프로 위젯 구성해줘
CVD 압력 위젯을 게이지에서 막대그래프로 바꾸는 계획만 보여줘
dashboardId 2의 위젯 위치를 스마트 정렬해줘
```

생성, 수정, 삭제, 정렬처럼 DB 변경이 있는 명령은 먼저 계획을 보여준 뒤 `적용` / `취소` 버튼으로 확정합니다.

## Verification

```bash
cd mcp-server
npm run typecheck
```

```bash
cd frontend
npm run build
```

백엔드 단위/통합 테스트:

```bash
cd backend
./gradlew test
```

## More Docs

- [환경 세팅 가이드](docs/ENVIRONMENT_SETUP.md)
- [통합 테스트 절차](docs/INTEGRATION_TEST.md)
- [MCP API 연동 문서](mcp-server/API_INTEGRATION.md)
- [MCP 서버 README](mcp-server/README.md)
- [Frontend README](frontend/README.md)
