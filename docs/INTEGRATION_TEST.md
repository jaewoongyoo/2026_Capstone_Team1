# Integration Test Guide

이 문서는 팀원이 같은 절차로 대시보드 통합 테스트를 진행하기 위한 가이드입니다.

## 1. 테스트 범위

통합 테스트는 다음 파이프라인을 확인합니다.

```text
장비 시뮬레이터
  -> EC2 Edge Gateway
  -> MQTT Broker
  -> Spring Backend
  -> WebSocket
  -> Frontend Dashboard
  -> MCP Chat UI
```

확인해야 하는 항목은 다음과 같습니다.

- 장비가 네트워크 스캔 또는 장비 목록에 표시되는지
- 장비별 센서가 12개씩 표시되는지
- 실시간 센서 값이 프론트 위젯에서 갱신되는지
- MCP 채팅에서 장비/센서/위젯 조회가 되는지
- MCP 채팅에서 위젯 생성/수정/삭제/정렬 계획을 보여주는지
- 사용자가 `적용` 버튼을 눌렀을 때 실제 위젯 변경이 반영되는지

## 2. 사전 준비

### 필수

- Node.js 20 이상
- npm
- Python 3.11 이상
- Git
- 테스트 계정

### 선택

- Docker
- Ollama

Ollama는 `mcp-server/scripts/ollama-chat.ts` CLI 테스트용입니다. 프론트의 MCP 채팅 UI 테스트에는 필수는 아닙니다.

## 3. Backend / Edge / MQTT 상태 확인

기본 통합 테스트는 이미 배포된 서버를 사용합니다.

```text
Backend API: https://api.43.201.141.9.nip.io
Swagger:     https://api.43.201.141.9.nip.io/swagger-ui/index.html
MQTT:        43.201.141.9:1883
Edge CVD:    43.201.141.9:5000
Edge ETCHER: 43.201.141.9:5001
```

포트 연결 확인:

```bash
nc -vz 43.201.141.9 1883
nc -vz 43.201.141.9 5000
nc -vz 43.201.141.9 5001
```

API Health 확인:

```bash
curl -fsS https://api.43.201.141.9.nip.io/api/public/health
```

## 4. MCP HTTP Bridge 실행

터미널 1:

```bash
cd mcp-server
npm ci
DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io \
DASHBOARD_TLS_REJECT_UNAUTHORIZED=false \
MCP_HTTP_PORT=3333 \
npm run http
```

성공 로그:

```text
MCP HTTP bridge listening on http://localhost:3333
```

## 5. Frontend 실행

터미널 2:

```bash
cd frontend
npm ci
VITE_API_BASE_URL=https://api.43.201.141.9.nip.io \
VITE_WS_URL=https://api.43.201.141.9.nip.io/ws-stomp \
VITE_MCP_CHAT_URL=http://localhost:3333/api/mcp/chat \
npm run dev
```

브라우저:

```text
http://localhost:5173
```

로그인 후 대시보드 페이지로 이동합니다.

## 6. 장비 시뮬레이터 실행

터미널 3:

```bash
cd 2026_Capstone_Team1
EDGE_GATEWAY_HOST=43.201.141.9 python3 simulator/secsgem_device.py
```

정상 로그 예시:

```text
[CVD-CHAMBER-01] 엣지게이트웨이 연결 성공
[ETCHER-01] 엣지게이트웨이 연결 성공
S6F11 데이터 스트리밍 시작
```

## 7. Frontend 확인 항목

### 장비 스캔

설비/장비 관리 화면에서 다음 장비가 보여야 합니다.

```text
CVD-CHAMBER-01
ETCHER-01
```

각 장비의 센서는 12개가 기준입니다.

```text
CVD-CHAMBER-01: 12 sensors
ETCHER-01: 12 sensors
```

두 장비의 센서가 한 목록에 섞여서 24개처럼 보이면 프론트 장비/센서 병합 로직 또는 백엔드 응답 매칭을 확인해야 합니다.

### 실시간 값

대시보드 위젯에서 다음 값들이 주기적으로 변경되어야 합니다.

- Chamber_Pressure
- Heater_Temp_Zone1 / Heater_Temp_Zone2
- Gas_Flow 계열
- RF/Bias Power 계열
- Equipment_State
- Step_Time
- Wafer_Processed_Count

## 8. MCP Chat UI 확인 항목

프론트 우측 하단의 `Nexus AI` 채팅에서 테스트합니다.

### 조회 명령

```text
장비 목록 보여줘
equipmentEntityId 9 장비 센서 목록 보여줘
현재 대시보드 세팅 상태 점검해줘
지원하는 위젯 종류 보여줘
압력 관련 센서 후보 보여줘
```

### 위젯 생성 명령

```text
dashboardId 2의 CVD 장비 Chamber_Pressure 센서를 막대그래프로 위젯 구성해줘
dashboardId 2의 ETCHER 장비 Chamber_Pressure 센서를 도넛 그래프로 위젯 구성해줘
```

예상 동작:

1. MCP가 먼저 생성 계획을 보여줍니다.
2. 채팅 응답 하단에 `적용`, `취소` 버튼이 표시됩니다.
3. `적용`을 누르면 실제 위젯이 생성됩니다.
4. 이미 같은 위젯이 있으면 중복 생성하지 않고 이미 처리된 상태를 안내합니다.

### 위젯 수정 명령

```text
CVD 압력 위젯을 게이지에서 막대그래프로 바꾸는 계획만 보여줘
CVD Chamber_Pressure 위젯을 가로 막대그래프로 바꿔줘
```

### 위젯 삭제 명령

```text
ETCHER 압력 위젯 삭제 계획만 보여줘
```

### 레이아웃 정렬 명령

```text
dashboardId 2의 위젯 위치를 스마트 정렬해줘
```

## 9. MCP CLI 테스트

HTTP UI 외에 MCP 서버 자체 동작을 확인할 수 있습니다.

```bash
cd mcp-server
MCP_TEST_USERNAME=<username> MCP_TEST_PASSWORD=<password> npm run smoke
```

위젯 dry-run:

```bash
cd mcp-server
MCP_TEST_USERNAME=<username> \
MCP_TEST_PASSWORD=<password> \
MCP_WIDGET_EQUIPMENT_ENTITY_ID=9 \
npm run widget-test
```

실제 생성:

```bash
cd mcp-server
MCP_TEST_USERNAME=<username> \
MCP_TEST_PASSWORD=<password> \
MCP_WIDGET_EQUIPMENT_ENTITY_ID=9 \
MCP_WIDGET_DRY_RUN=false \
npm run widget-test
```

## 10. Troubleshooting

### MCP 채팅이 응답하지 않음

- `mcp-server`가 `localhost:3333`에서 실행 중인지 확인합니다.
- 프론트 실행 시 `VITE_MCP_CHAT_URL=http://localhost:3333/api/mcp/chat`가 들어갔는지 확인합니다.
- 브라우저 개발자 도구 Network 탭에서 `/api/mcp/chat` 요청 실패 여부를 확인합니다.

### 401 또는 JWT 오류

- 프론트에 로그인되어 있는지 확인합니다.
- 백엔드 API 토큰이 만료되었으면 새로 로그인합니다.
- MCP HTTP Bridge는 프론트에서 전달된 Bearer token을 사용합니다.

### 장비 값이 오래됨

- 장비 시뮬레이터가 실행 중인지 확인합니다.
- `EDGE_GATEWAY_HOST=43.201.141.9`로 실행했는지 확인합니다.
- EC2 Edge Gateway/MQTT 컨테이너가 실행 중인지 확인합니다.

### 포트 충돌

`simulator/run_simulator.py`는 로컬 Edge Gateway까지 띄우므로 로컬 5000/5001 포트를 사용합니다. EC2 Edge Gateway 테스트에서는 `simulator/secsgem_device.py`를 사용하세요.

### 위젯 변경이 바로 보이지 않음

- 대시보드 페이지 새로고침
- 현재 선택된 dashboardId 확인
- 같은 장비명이 여러 대시보드에 존재할 수 있으므로 `dashboardId` 또는 `equipmentEntityId`를 명령에 포함

## 11. 완료 기준

통합 테스트는 다음 조건을 만족하면 통과로 봅니다.

- 프론트 로그인 성공
- 장비 목록에서 CVD/ETCHER 확인
- 각 장비 센서 12개 확인
- 시뮬레이터 실행 중 실시간 값 갱신 확인
- MCP 채팅에서 장비/센서/상태 조회 성공
- MCP 채팅에서 위젯 생성 계획 확인
- `적용` 버튼으로 위젯 생성 또는 수정 성공
- `현재 대시보드 세팅 상태 점검해줘` 명령이 정상 요약 반환

