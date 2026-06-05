# MCP - Backend API Integration

이 문서는 Dashboard MCP Server가 Spring Backend API를 어떻게 감싸고 호출하는지 정리한 연동 문서입니다.

## 역할

MCP Server는 장비 데이터를 직접 수집하지 않습니다. MQTT, Edge Gateway, DB, WebSocket 처리는 Spring Backend가 담당하고, MCP는 백엔드 REST API를 호출해서 대시보드 세팅을 자동화합니다.

```text
사용자 자연어
  -> Ollama
  -> MCP Server
  -> Spring Backend REST API
  -> DB / Dashboard 설정 변경
  -> Frontend Dashboard 반영
```

## 환경 변수

```bash
DASHBOARD_API_BASE_URL=https://api.43.201.141.9.nip.io
DASHBOARD_API_TOKEN=
DASHBOARD_TLS_REJECT_UNAUTHORIZED=false
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_MODEL=qwen2.5:3b
```

온프레미스 패키지에서는 보통 다음처럼 내부 서비스명으로 연결합니다.

```bash
DASHBOARD_API_BASE_URL=http://backend:8080
```

`config/api-protocol.json`에는 데모/온프레미스 URL을 예시로만 기록하고, 실제 통신 주소는 항상 `DASHBOARD_API_BASE_URL` 환경변수로 주입합니다. 이렇게 해야 고객사별 IP나 도메인이 바뀌어도 MCP 규격 문서를 수정하지 않아도 됩니다.

## MCP API Protocol

MCP가 접근할 수 있는 Backend API 범위와 안전 정책은 JSON 프로토콜로 분리되어 있습니다.

```text
config/api-protocol.json
```

이 파일에는 tool별로 다음 정보가 정의됩니다.

- `allowed`: MCP 노출 허용 여부
- `risk`: `read`, `write`, `admin`, `internal`
- `requiresAuth`: 백엔드 인증 필요 여부
- `requiresConfirmation`: 실행 전 사용자 확인 필요 여부
- `dryRunSupported`: 계획 확인 모드 지원 여부
- `backendApis`: 실제 연결되는 백엔드 API 목록

MCP tool로도 조회할 수 있습니다.

```text
get_mcp_api_protocol
```

이 구조의 목적은 Swagger 전체를 AI에게 직접 열지 않고, 제품 기능에 필요한 API만 통제된 tool로 노출하는 것입니다.

## 식별자 규칙

백엔드 응답에는 비슷한 이름의 ID가 함께 등장합니다. MCP 자연어 라우터는 다음 기준으로 해석합니다.

| 이름 | 의미 | MCP 사용 원칙 |
| --- | --- | --- |
| `dashboardId` | 대시보드 DB PK | 대시보드 지정/필터링에 사용 |
| `equipmentEntityId` | 장비 DB PK | 장비 식별의 기본값 |
| `sensorEntityId` | 센서 DB PK | 위젯 생성 시 센서 엔티티 연결에 사용 |
| `widgetId` | 위젯 DB PK | 위젯 수정/삭제/레이아웃 변경에 사용 |
| `equipmentId` | telemetry 또는 일부 widget API의 장비 문자열 ID/장비명 | 기존 백엔드 API가 요구할 때만 사용 |
| `sensorId` | telemetry 센서 문자열 ID | 센서명 매칭과 위젯 dataKey에 사용 |

사용자가 숫자로 `equipmentEntityId 9`처럼 말하면 장비 DB PK로 처리하고, `CVD-CHAMBER-01`처럼 문자열로 말하면 장비명 후보로 매칭합니다. 같은 장비명이 여러 대시보드에 있으면 `dashboardId`, 대시보드명, 또는 `equipmentEntityId`로 좁히도록 안내합니다.

## 목록 제한과 페이지네이션 정책

장비/센서/위젯 수가 많아지면 한 번에 큰 JSON 배열을 LLM에 넘기는 것이 위험합니다. 현재 주요 목록 API는 명시적인 `page/size` 응답을 보장하지 않으므로, MCP tool에서 우선 기본 `limit`을 적용합니다.

| MCP tool | 기본 제한 |
| --- | --- |
| `get_equipment_list` | 20개 |
| `get_current_equipment_all` | 50개 |
| `search_sensors` | 20개 |
| `search_equipment_sensors` | 20개 |
| `get_widgets` | 50개 |

결과가 많거나 동명 장비가 있으면 MCP는 전체 JSON을 계속 출력하지 않고 `keyword`, `dashboardId`, `equipmentEntityId` 같은 추가 조건을 요청합니다. 백엔드에 정식 페이지네이션이 추가되면 다음 형태로 확장하는 것이 좋습니다.

```text
GET /api/equipment/search?keyword={keyword}&page={page}&size={size}
GET /api/sensors/search?keyword={keyword}&page={page}&size={size}
GET /api/dashboard/widgets?page={page}&size={size}
```

## 인증 흐름

MCP는 `login` tool을 통해 백엔드에 로그인하고, 응답으로 받은 access token을 이후 API 요청의 `Authorization: Bearer ...` 헤더에 붙입니다.

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `get_mcp_api_protocol` | MCP 내부 정책 JSON | - | MCP API 프로토콜과 안전 정책 조회 |
| `login` | `/api/auth/login` | `POST` | 로그인 후 access token 저장 |
| `get_me` | `/api/auth/me` | `GET` | 현재 사용자 정보 조회 |

## Health / 운영 점검

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `check_backend_health` | `/api/public/health` | `GET` | 백엔드 기본 health check |
| `check_backend_detailed_health` | `/api/public/health/detailed` | `GET` | 백엔드 상세 health check |
| `check_recent_telemetry` | `/api/equipment/current` | `GET` | 장비별 최신 telemetry timestamp 검사 |
| `generate_setup_summary` | `/api/public/health`, `/api/dashboards`, `/api/equipment/current`, `/api/dashboard/widgets` | `GET` | 설치/운영 상태 종합 요약 |

## Dashboard API

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `get_dashboards` | `/api/dashboards` | `GET` | 현재 사용자의 대시보드 목록 조회 |
| `get_dashboard` | `/api/dashboards/{dashboardId}` | `GET` | 대시보드 단건 조회 |

MCP 자연어 라우터는 사용자가 `dashboardId`를 몰라도 대시보드명을 기준으로 매칭할 수 있습니다.

예시:

```text
frontend_test2 Dashboard의 위젯 위치를 스마트 정렬해줘
현재 대시보드 위젯 위치를 스마트 정렬해줘
```

## Equipment API

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `get_equipment_list` | `/api/equipment/search?keyword={keyword}` | `GET` | 장비명/분류 기준 검색. MCP 기본 limit=20 |
| `get_dashboard_equipment` | `/api/equipment/dashboard/{dashboardId}` | `GET` | 특정 대시보드 장비 목록 조회 |
| `get_current_equipment_all` | `/api/equipment/current` | `GET` | 장비 최신 센서 스냅샷 조회. MCP 기본 limit=50 |
| `get_current_equipment` | `/api/equipment/{equipmentEntityId}/current` | `GET` | 특정 장비의 최신 센서 스냅샷 조회 |

MCP는 장비명을 고정하지 않고, 백엔드에서 조회한 `equipmentName`, `field`, 최신 telemetry의 `equipmentId`를 기준으로 동적 매칭합니다.

예시:

```text
CVD 장비 센서 목록 보여줘
equipmentEntityId 9 장비 센서 목록 보여줘
```

## Sensor API

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `get_sensors_by_equipment_id` | `/api/sensors/equipment/{equipmentEntityId}` | `GET` | 장비 엔티티 ID 기준 센서 목록 조회 |
| `get_sensors_by_equipment_name` | `/api/sensors/equipment-name/{equipmentName}` | `GET` | 장비명 기준 센서 목록 조회 |
| `search_sensors` | `/api/sensors/search?keyword={keyword}` | `GET` | 전체 센서 검색. MCP 기본 limit=20 |
| `search_equipment_sensors` | `/api/sensors/equipment/{equipmentEntityId}/search?keyword={keyword}` | `GET` | 특정 장비 내 센서 검색. MCP 기본 limit=20 |

자연어 라우터는 `압력`, `온도`, `가스`, `전력`, `상태`, `로그`, `웨이퍼` 같은 표현을 센서 후보 검색에 사용합니다.

예시:

```text
압력 관련 센서 후보 보여줘
```

## Widget API

| MCP tool | Backend API | Method | 설명 |
| --- | --- | --- | --- |
| `get_supported_widget_types` | MCP 내부 사전 | - | 생성 가능한 위젯/그래프 타입 조회 |
| `get_widgets` | `/api/dashboard/widgets` | `GET` | 위젯 목록 조회. MCP 기본 limit=50 |
| `get_widgets_by_equipment` | `/api/dashboard/widgets/equipment/{equipmentId}` | `GET` | 장비명 기준 위젯 목록 조회 |
| `get_widget` | `/api/dashboard/widgets/{widgetId}` | `GET` | 위젯 단건 조회 |
| `create_widget` | `/api/dashboard/widgets` | `POST` | 위젯 생성 |
| `update_widget` | `/api/dashboard/widgets/{widgetId}` | `PUT` | 위젯 수정 |
| `update_widget_layouts` | `/api/dashboard/widgets/layout` | `PUT` | 여러 위젯 위치/크기 일괄 수정 |
| `delete_widget` | `/api/dashboard/widgets/{widgetId}` | `DELETE` | 위젯 삭제 |
| `auto_create_widgets_for_equipment` | `/api/equipment/{id}/current`, `/api/sensors/equipment/{id}`, `/api/dashboard/widgets`, `/api/dashboard/widgets` | `GET/POST` | 장비 센서 기반 기본 위젯 자동 생성 |

### 지원 위젯 타입

| 표시 방식 | `widgetType` | `chartType` | 용도 |
| --- | --- | --- | --- |
| 게이지 | `GAUGE` | `line` | 단일 숫자 센서 현재값 |
| 선 그래프 | `TREND` | `line` | 시간 흐름에 따른 수치 변화 |
| 막대그래프 | `BAR_V` | `bar` | 수치 비교 또는 현재값 강조 |
| 가로 막대그래프 | `BAR_H` | `bar-horizontal` | 항목명이 긴 수치 비교 |
| 도넛 그래프 | `DONUT` | `donut` | 비율/점유율 형태 표현 |
| 상태 위젯 | `STATUS` | `status` | BOOLEAN/STRING 상태값 |
| 로그 위젯 | `LOG` | `log` | 문자열 로그 메시지 |
| 알림 위젯 | `ALERTS` | `alerts` | 이상/경고 이벤트 |
| 센서 목록 위젯 | `SENSORS` | `sensors` | 장비 센서 전체 목록 |
| OEE 위젯 | `OEE` | `oee` | 설비 종합 효율 |

사용자에게는 내부값인 `widgetType`, `chartType`을 직접 보여주지 않고 `게이지`, `도넛 그래프`, `막대그래프`처럼 표시합니다.

### 위젯 생성 방식

`auto_create_widgets_for_equipment`는 LLM이 임의의 JSON을 만들어내는 방식이 아닙니다. MCP 서버가 센서 타입과 센서명에 따라 결정적인 프리셋을 적용합니다.

| 센서 조건 | 기본 위젯 |
| --- | --- |
| `Log_Message` 또는 log 계열 문자열 | 로그 위젯 |
| `BOOLEAN` 또는 상태성 `STRING` | 상태 위젯 |
| 그 외 `FLOAT`/`INTEGER` | 게이지 |

사용자가 “막대그래프”, “도넛 그래프”처럼 표시 방식을 명시한 커스텀 요청은 `get_supported_widget_types`에 등록된 허용 타입으로만 매핑합니다. 허용되지 않은 위젯 타입이나 백엔드 스키마 밖 필드는 생성하지 않습니다.

## 자연어 자동화 흐름

### 1. 위젯 생성

```text
CVD_CHAMBER_-01 장비에 있는 CHAMBER_Pressure 센서는 막대그래프로 위젯 구성 계획만 보여줘
```

MCP 처리:

1. `get_current_equipment_all`
2. 장비명 느슨한 매칭
3. 센서명 매칭
4. 표시 방식 매핑
5. `create_widget` 요청 구성
6. `계획만`이면 실제 생성하지 않음
7. 실제 생성 명령이면 `yes/no` 확인 후 생성

### 2. 위젯 수정

```text
CVD 압력 위젯을 게이지에서 막대그래프로 바꿔줘
```

MCP 처리:

1. `get_current_equipment_all`
2. `get_widgets`
3. 장비/센서/기존 표시 방식 매칭
4. `update_widget` 요청 구성
5. `yes/no` 확인 후 수정

### 3. 위젯 삭제

```text
ETCHER 압력 위젯 삭제해줘
```

MCP 처리:

1. `get_current_equipment_all`
2. `get_widgets`
3. 장비/센서 매칭
4. `delete_widget` 요청 구성
5. `yes/no` 확인 후 삭제

### 4. 위젯 스마트 정렬

```text
현재 대시보드 위젯 위치를 스마트 정렬해줘
```

MCP 처리:

1. `get_dashboards`
2. 대시보드명 또는 기본 대시보드 매칭
3. `get_widgets`
4. 숫자형 위젯 상단, 상태 위젯 중간, 로그/알림 위젯 하단으로 배치 계획 생성
5. `update_widget_layouts` 요청 구성
6. `yes/no` 확인 후 적용

레이아웃 일괄 수정은 여러 위젯의 `x`, `y`, `width`, `height`가 한 번에 바뀌는 write 작업입니다. 따라서 자연어 흐름에서는 항상 “어떤 위젯이 어느 위치로 이동하는지”를 먼저 보여준 뒤 사용자 확인을 받아야 합니다.

## 안전장치

- 생성/수정/삭제/정렬처럼 DB 변경이 있는 작업은 실행 전 `yes/no` 확인을 받습니다.
- `계획만`, `미리`, `dryRun` 표현이 있으면 실제 API 변경 요청을 보내지 않습니다.
- 장비나 대시보드가 여러 개 매칭되면 후보를 보여주고 더 구체적인 입력을 요구합니다.
- `BOOLEAN`, `STRING` 센서를 숫자형 그래프로 요청하면 상태 위젯으로 보정합니다.

## 현재 데모 연결 구조

```text
Frontend(Vercel)
  -> Spring Backend API: https://api.43.201.141.9.nip.io

MCP Server(local)
  -> Spring Backend API: https://api.43.201.141.9.nip.io

Ollama(local)
  -> MCP 자연어 테스트
```

## 온프레미스 제공 구조

```text
고객사 내부망
  ├─ Frontend
  ├─ Spring Backend
  ├─ DB
  ├─ MQTT Broker
  ├─ Edge Gateway
  ├─ MCP Server
  └─ Ollama / Local LLM
```

MCP는 외부 공개 서버보다는 고객사 내부망에서 백엔드 API를 호출하는 자동화 계층으로 두는 구성이 권장됩니다.
