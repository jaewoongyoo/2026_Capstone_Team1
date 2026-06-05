# =============================================================================
# config.py — 전체 시뮬레이터 설정
# =============================================================================

# ── 네트워크 ──────────────────────────────────────────────────────────────────
import os
DEVICE_HOST = os.environ.get("DEVICE_HOST", "127.0.0.1")
EDGE_GATEWAY_BIND_HOST = os.environ.get("EDGE_GATEWAY_BIND_HOST", "0.0.0.0")
EDGE_GATEWAY_HOST = os.environ.get("EDGE_GATEWAY_HOST", "127.0.0.1")

DEVICES = {
    "CVD-CHAMBER-01": {
        "port": int(os.environ.get("EDGE_GATEWAY_CVD_PORT", "5000")),
        "type": "CVD",
    },
    "ETCHER-01": {
        "port": int(os.environ.get("EDGE_GATEWAY_ETCHER_PORT", "5001")),
        "type": "ETCHER",
    },
}

MQTT_HOST         = os.environ.get("MQTT_HOST", "127.0.0.1")
MQTT_PORT         = int(os.environ.get("MQTT_PORT", "1883"))
MQTT_TOPIC_PREFIX = "factory/equipment"
# 토픽 예: factory/equipment/CVD-CHAMBER-01/telemetry

PUBLISH_INTERVAL_SEC = 1.0

# ── 센서 정의 ─────────────────────────────────────────────────────────────────
SENSOR_DEFINITIONS = {
    "CVD": [
        # FLOAT — 라인 차트 / 게이지
        {"id": "Chamber_Pressure",      "dataType": "FLOAT",   "unit": "mTorr"},
        {"id": "Heater_Temp_Zone1",     "dataType": "FLOAT",   "unit": "degC"},
        {"id": "Heater_Temp_Zone2",     "dataType": "FLOAT",   "unit": "degC"},
        {"id": "Gas_Flow_Ar",           "dataType": "FLOAT",   "unit": "sccm"},
        {"id": "RF_Power_Forward",      "dataType": "FLOAT",   "unit": "W"},
        # BOOLEAN — LED 인디케이터
        {"id": "Slit_Valve_State",      "dataType": "BOOLEAN", "unit": "STATE"},
        {"id": "Vacuum_Pump_State",     "dataType": "BOOLEAN", "unit": "STATE"},
        {"id": "Chuck_State",           "dataType": "BOOLEAN", "unit": "STATE"},
        # INTEGER — 숫자 카드
        {"id": "Wafer_Processed_Count", "dataType": "INTEGER", "unit": "ea"},
        {"id": "Step_Time",             "dataType": "INTEGER", "unit": "sec"},
        # STRING — 상태 배지 / 로그 패널
        {"id": "Equipment_State",       "dataType": "STRING",  "unit": ""},
        {"id": "Log_Message",           "dataType": "STRING",  "unit": ""},
    ],
    "ETCHER": [
        # FLOAT
        {"id": "Chamber_Pressure",      "dataType": "FLOAT",   "unit": "mTorr"},
        {"id": "Bias_Power",            "dataType": "FLOAT",   "unit": "W"},
        {"id": "RF_Power_Source",       "dataType": "FLOAT",   "unit": "W"},
        {"id": "Gas_Flow_CF4",          "dataType": "FLOAT",   "unit": "sccm"},
        {"id": "Gas_Flow_O2",           "dataType": "FLOAT",   "unit": "sccm"},
        {"id": "Etch_Rate",             "dataType": "FLOAT",   "unit": "A/min"},
        # BOOLEAN
        {"id": "Slit_Valve_State",      "dataType": "BOOLEAN", "unit": "STATE"},
        {"id": "Vacuum_Pump_State",     "dataType": "BOOLEAN", "unit": "STATE"},
        # INTEGER
        {"id": "Wafer_Processed_Count", "dataType": "INTEGER", "unit": "ea"},
        {"id": "Step_Time",             "dataType": "INTEGER", "unit": "sec"},
        # STRING
        {"id": "Equipment_State",       "dataType": "STRING",  "unit": ""},
        {"id": "Log_Message",           "dataType": "STRING",  "unit": ""},
    ],
}

# ── 센서 초기값 ───────────────────────────────────────────────────────────────
BASE_VALUES = {
    "CVD": {
        "Chamber_Pressure":      15.0,
        "Heater_Temp_Zone1":    970.0,
        "Heater_Temp_Zone2":    968.0,
        "Gas_Flow_Ar":          200.0,
        "RF_Power_Forward":     500.0,
        "Slit_Valve_State":       0,
        "Vacuum_Pump_State":      1,
        "Chuck_State":            1,
        "Wafer_Processed_Count":  0,
        "Step_Time":              0,
        "Equipment_State":     "IDLE",
        "Log_Message":         "",
    },
    "ETCHER": {
        "Chamber_Pressure":      8.0,
        "Bias_Power":          300.0,
        "RF_Power_Source":     800.0,
        "Gas_Flow_CF4":         50.0,
        "Gas_Flow_O2":          10.0,
        "Etch_Rate":          3500.0,
        "Slit_Valve_State":      0,
        "Vacuum_Pump_State":     1,
        "Wafer_Processed_Count": 0,
        "Step_Time":             0,
        "Equipment_State":    "IDLE",
        "Log_Message":        "",
    },
}

# ── 장비 상태 시나리오 ────────────────────────────────────────────────────────
# 각 스텝: (Equipment_State, 지속 틱 수, 진입 시 로그 메시지)
# 마지막 스텝이 끝나면 처음으로 돌아가며 wafer 카운트 증가

SCENARIO = {
    "CVD": [
        ("IDLE",        10, "{eq} 장비 대기 중"),
        ("SETUP",       8,  "{eq} 작업 준비 중 — 레시피 로딩 (Recipe-CVD-A01)"),
        ("SETUP",       5,  "{eq} 챔버 진공 펌핑 시작"),
        ("SETUP",       5,  "{eq} 히터 예열 중... 목표 온도 970°C"),
        ("RUNNING",     5,  "{eq} 장비 가동 시작"),
        ("RUNNING",     10, "{eq} 웨이퍼 처리 중 (Wafer #{wafer})"),
        ("RUNNING",     10, "{eq} 증착 진행 중 — RF Power 정상"),
        ("RUNNING",     10, "{eq} 웨이퍼 처리 중 (Wafer #{wafer})"),
        ("RUNNING",     5,  "{eq} 웨이퍼 처리 완료 — 총 {wafer}장"),
        ("COOLDOWN",    8,  "{eq} 냉각 중... 챔버 온도 하강"),
        ("COOLDOWN",    5,  "{eq} 냉각 완료 — 다음 배치 대기"),
        ("ERROR",       6,  "{eq} ⚠ 챔버 압력 이상 감지 — 점검 필요"),
        ("MAINTENANCE", 8,  "{eq} 유지보수 모드 진입 — 엔지니어 확인 중"),
        ("IDLE",        5,  "{eq} 유지보수 완료 — 정상 복귀"),
    ],
    "ETCHER": [
        ("IDLE",        10, "{eq} 장비 대기 중"),
        ("SETUP",       8,  "{eq} 작업 준비 중 — 레시피 로딩 (Recipe-ETCH-B02)"),
        ("SETUP",       5,  "{eq} 챔버 진공 펌핑 시작"),
        ("SETUP",       5,  "{eq} RF 소스 파워 초기화 중"),
        ("RUNNING",     5,  "{eq} 장비 가동 시작"),
        ("RUNNING",     10, "{eq} 웨이퍼 식각 중 (Wafer #{wafer})"),
        ("RUNNING",     10, "{eq} 식각 진행 중 — Etch Rate 정상"),
        ("RUNNING",     10, "{eq} 웨이퍼 식각 중 (Wafer #{wafer})"),
        ("RUNNING",     5,  "{eq} 식각 완료 — 총 {wafer}장"),
        ("COOLDOWN",    8,  "{eq} 냉각 중... 바이어스 파워 오프"),
        ("COOLDOWN",    5,  "{eq} 냉각 완료 — 다음 배치 대기"),
        ("ERROR",       6,  "{eq} ⚠ Etch Rate 스펙 이탈 — 공정 중단"),
        ("MAINTENANCE", 8,  "{eq} 유지보수 모드 진입 — 챔버 클리닝 중"),
        ("IDLE",        5,  "{eq} 유지보수 완료 — 정상 복귀"),
    ],
}
