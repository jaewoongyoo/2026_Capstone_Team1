# Dashboard MCP Server

Dashboard의 백엔드 API를 MCP tool로 감싸서 장비, 센서, 대시보드, 위젯, health check 세팅을 자동화하는 서버입니다.

백엔드 API와 MCP tool의 상세 매핑은 [API_INTEGRATION.md](./API_INTEGRATION.md)를 참고하세요.
MCP가 허용하는 API 범위와 안전 정책은 [config/api-protocol.json](./config/api-protocol.json)에 정의되어 있습니다.

## MVP 범위

- 인증 토큰 발급
- 대시보드 조회
- 장비/센서/현재 상태 조회
- 위젯 조회/생성/수정
- 백엔드 health check

## 실행

```bash
cd mcp-server
cp .env.example .env
npm install
npm run dev
```

## 환경 변수

```bash
DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io
DASHBOARD_API_TOKEN=
DASHBOARD_TLS_REJECT_UNAUTHORIZED=false
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
```

온프레미스 패키지에서는 `DASHBOARD_API_BASE_URL=http://backend:8080` 형태로 바꾸면 됩니다.
MCP 규격 문서에는 URL을 예시로만 남기고, 실제 호출 주소는 이 환경변수로 주입합니다.

## Ollama + MCP 자연어 테스트

터미널 1:

```bash
ollama serve
```

터미널 2:

```bash
cd mcp-server
MCP_TEST_USERNAME=frontend_test2 MCP_TEST_PASSWORD=Test1234! OLLAMA_MODEL=qwen2.5:3b npm run ollama-chat
```

예시 입력:

```text
CVD 장비 센서 목록 보여줘
ETCHER 장비 현재 상태 확인해줘
현재 대시보드 세팅 상태 점검해줘
CVD 장비 핵심 센서로 위젯 구성 계획만 보여줘
압력 관련 센서 후보 보여줘
equipmentEntityId 9 장비 센서 목록 보여줘
frontend_test2 Dashboard의 CVD 장비 센서 목록 보여줘
지원하는 그래프 종류 보여줘
frontend_test2 Dashboard의 CVD_CHAMBER_-01 장비에 있는 CHAMBER_Pressure 센서는 막대그래프로, ETCHER-01 장비에 있는 Chamber_Pressure 센서는 도넛 그래프로 위젯 세팅해줘
equipmentEntityId 9 장비의 Chamber_Pressure 센서를 막대그래프로 위젯 구성 계획만 보여줘
CVD 압력 위젯을 게이지에서 막대그래프로 바꾸는 계획만 보여줘
ETCHER 압력 위젯 삭제 계획만 보여줘
현재 대시보드 위젯 위치를 스마트 정렬해줘
```

실제 위젯 생성처럼 변경이 발생하는 요청은 실행 전 확인을 한 번 더 받습니다.

자연어 라우터는 특정 장비 ID를 코드에 고정하지 않고, 백엔드에 등록된 장비의 `equipmentName`, `field`, 최신 telemetry 정보를 조회해서 동적으로 매칭합니다. 장비가 여러 개 매칭되면 후보 목록을 보여주고 더 구체적인 장비명을 요청합니다.

커스텀 위젯 구성은 `장비 -> 센서 -> 수치/타입 -> 위젯` 순서로 해석합니다. 장비명은 `CVD_CHAMBER_-01`처럼 하이픈/언더스코어가 섞여도 느슨하게 매칭하고, 센서명은 최신 telemetry의 `sensorId` 기준으로 찾습니다. 숫자 센서는 게이지/선 그래프/막대/도넛으로 만들 수 있고, `BOOLEAN`/`STRING` 센서를 숫자형 그래프로 요청하면 안전하게 상태 위젯으로 보정합니다.

위젯 위치 정렬은 숫자형 위젯을 상단, 상태 위젯을 중간, 로그/알림 위젯을 하단으로 배치하는 기본 스마트 정렬 정책을 사용합니다. 사용자가 `dashboardId`를 몰라도 `현재 대시보드`, `내 대시보드`, 또는 대시보드명으로 대상을 지정할 수 있습니다.

목록 조회 tool은 LLM 컨텍스트 초과를 막기 위해 기본 반환 개수를 제한합니다. 장비/센서/위젯이 많으면 전체 JSON을 한 번에 보여주기보다 `keyword`, `dashboardId`, `equipmentEntityId` 같은 조건으로 좁혀서 조회합니다.

## 현재 등록된 Tools

### Auth

- `get_mcp_api_protocol`
- `login`
- `get_me`

### Health

- `check_backend_health`
- `check_backend_detailed_health`
- `check_recent_telemetry`
- `generate_setup_summary`

### Dashboard

- `get_dashboards`
- `get_dashboard`

### Equipment

- `get_equipment_list`
- `get_dashboard_equipment`
- `get_current_equipment_all`
- `get_current_equipment`

### Sensor

- `get_sensors_by_equipment_id`
- `get_sensors_by_equipment_name`
- `search_sensors`
- `search_equipment_sensors`

### Widget

- `get_supported_widget_types`
- `get_widgets`
- `get_widgets_by_equipment`
- `get_widget`
- `create_widget`
- `update_widget`
- `update_widget_layouts`
- `delete_widget`
- `auto_create_widgets_for_equipment`
