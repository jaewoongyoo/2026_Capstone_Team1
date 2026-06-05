#!/usr/bin/env python3
"""
================================================================================
edge_gateway.py — 엣지 게이트웨이 서버
================================================================================
역할:
  - 각 장비별 TCP 포트를 열고 장비 접속을 수락
  - 핸드셰이크 (S1F13 → S1F14 → S1F11 → S1F12) 수행
  - S1F12 로 수신한 센서 이름/타입/단위를 메타데이터로 저장
  - S6F11 수신 → 표준 JSON 변환 → MQTT 발행

발행 토픽:
  factory/equipment/{equipmentId}/telemetry   ← 센서 데이터 (매 틱)
  factory/equipment/{equipmentId}/metadata    ← 센서 목록 (최초 1회)

실행:
  python3 edge/edge_gateway.py
================================================================================
"""

import asyncio
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

try:
    import paho.mqtt.client as mqtt
    MQTT_AVAILABLE = True
except ImportError:
    MQTT_AVAILABLE = False
    print("[WARN] paho-mqtt 없음 — pip install paho-mqtt")

from common.config import (
    EDGE_GATEWAY_BIND_HOST,
    DEVICES,
    MQTT_HOST,
    MQTT_PORT,
    MQTT_TOPIC_PREFIX,
    PUBLISH_INTERVAL_SEC,
)


# =============================================================================
# MQTT 클라이언트
# =============================================================================

class MqttClient:
    """
    게이트웨이 전체가 공유하는 단일 MQTT 클라이언트.
    연결 실패 시에도 게이트웨이는 계속 동작 (콘솔 출력만).
    """

    def __init__(self):
        self.connected = False
        self._client: Optional[mqtt.Client] = None

    # ------------------------------------------------------------------
    def connect(self):
        if not MQTT_AVAILABLE:
            print("[MQTT] 라이브러리 없음 — 콘솔 출력만 진행")
            return

        self._client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2)
        self._client.on_connect    = self._on_connect
        self._client.on_disconnect = self._on_disconnect

        try:
            print(f"[MQTT] 브로커 연결 시도 → {MQTT_HOST}:{MQTT_PORT}")
            self._client.connect(MQTT_HOST, MQTT_PORT, keepalive=60)
            self._client.loop_start()
        except Exception as e:
            print(f"[MQTT] 연결 실패: {e} — 콘솔 출력만 진행")

    def _on_connect(self, client, userdata, flags, rc, properties=None):
        if rc == 0:
            self.connected = True
            print("[MQTT] ✅ 브로커 연결 성공")
        else:
            print(f"[MQTT] ❌ 연결 실패 (rc={rc})")

    def _on_disconnect(self, client, userdata, rc, properties=None, reason_code=None):
        self.connected = False
        print("[MQTT] 브로커 연결 해제")

    # ------------------------------------------------------------------
    def publish(self, topic: str, payload: str, qos: int = 1):
        if self.connected and self._client:
            self._client.publish(topic, payload, qos=qos)

    def disconnect(self):
        if self._client:
            self._client.loop_stop()
            self._client.disconnect()


# =============================================================================
# JSON 변환기
# =============================================================================

class SecsMessageParser:
    """
    장비에서 수신한 SECS 메시지를 표준 JSON 페이로드로 변환.

    S1F12 (메타데이터):
      {
        "equipmentId"  : "CVD-CHAMBER-01",
        "equipmentType": "CVD",
        "sensors": [
          {"svid": 1, "name": "Chamber_Pressure", "dataType": "FLOAT", "unit": "mTorr"},
          ...
        ]
      }

    S6F11 (텔레메트리):
      {
        "equipmentId": "CVD-CHAMBER-01",
        "timestamp"  : "2026-04-17T10:00:00+00:00",
        "status"     : "RUNNING",
        "sensors": [
          {"svid": 1, "name": "Chamber_Pressure", "dataType": "FLOAT",
           "value": 15.2, "unit": "mTorr"},
          ...
        ]
      }
    """

    @staticmethod
    def parse_s1f12(msg: dict) -> dict:
        """센서 메타데이터 페이로드 생성"""
        return {
            "equipmentId"  : msg["equipmentId"],
            "equipmentType": msg["equipmentType"],
            "sensors"      : [
                {
                    "svid"    : v["svid"],
                    "name"    : v["name"],
                    "dataType": v["dataType"],
                    "unit"    : v["unit"],
                }
                for v in msg.get("variables", [])
            ],
        }

    @staticmethod
    def parse_s6f11(msg: dict) -> dict:
        """센서 텔레메트리 페이로드 생성"""
        variables = msg.get("variables", [])

        # Equipment_State 를 최상위 status 로 추출
        status = next(
            (v["value"] for v in variables if v["name"] == "Equipment_State"),
            "UNKNOWN"
        )

        return {
            "equipmentId": msg["equipmentId"],
            "timestamp"  : msg.get("timestamp",
                           datetime.now(timezone.utc).isoformat()),
            "status"     : status,
            "sensors"    : [
                {
                    "svid"    : v["svid"],
                    "name"    : v["name"],
                    "dataType": v["dataType"],
                    "value"   : v["value"],
                    "unit"    : v["unit"],
                }
                for v in variables
            ],
        }


# =============================================================================
# 단일 장비 연결 핸들러
# =============================================================================

class DeviceConnection:
    """
    하나의 장비 접속 포트를 열고, 장비가 접속하면 데이터를 수신해서 MQTT 로 발행.
    """

    def __init__(self, equipment_id: str, equipment_type: str,
                 port: int, mqtt: MqttClient):
        self.equipment_id   = equipment_id
        self.equipment_type = equipment_type
        self.port           = port
        self.mqtt           = mqtt
        self.parser         = SecsMessageParser()
        self.tick           = 0

    def log(self, msg: str):
        ts = datetime.now().strftime("%H:%M:%S")
        print(f"[{ts}][GW:{self.equipment_id}] {msg}")

    # ------------------------------------------------------------------
    async def run_forever(self):
        """장비 접속을 받는 TCP 서버 실행"""
        server = await asyncio.start_server(
            self._handle_device, EDGE_GATEWAY_BIND_HOST, self.port
        )
        self.log(f"장비 접속 대기 → {EDGE_GATEWAY_BIND_HOST}:{self.port}")
        async with server:
            await server.serve_forever()

    # ------------------------------------------------------------------
    async def _handle_device(self, reader: asyncio.StreamReader,
                             writer: asyncio.StreamWriter):
        peer = writer.get_extra_info("peername")
        self.log(f"장비 접속 ← {peer}")

        try:
            await self._handshake(reader, writer)
            await self._stream_loop(reader)
        except Exception as e:
            self.log(f"⚠ 오류: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            self.log(f"연결 종료 ← {peer}")

    # ------------------------------------------------------------------
    async def _handshake(self, reader: asyncio.StreamReader,
                          writer: asyncio.StreamWriter):

        # ── S1F13 송신 (연결 요청) ─────────────────────────────────────
        s1f13 = json.dumps({
            "stream": 1, "function": 13,
            "equipmentId": self.equipment_id,
        }) + "\n"
        writer.write(s1f13.encode())
        await writer.drain()
        self.log("S1F13 송신 — 연결 요청")

        # ── S1F14 수신 (연결 승인) ─────────────────────────────────────
        raw = await asyncio.wait_for(reader.readline(), timeout=10.0)
        msg = json.loads(raw.decode().strip())
        assert msg["stream"] == 1 and msg["function"] == 14
        assert msg.get("commAck") == 0, "연결 거부됨"
        self.log("S1F14 수신 — 연결 승인")

        # ── S1F11 송신 (센서 이름 요청) ───────────────────────────────
        s1f11 = json.dumps({
            "stream": 1, "function": 11,
            "equipmentId": self.equipment_id,
        }) + "\n"
        writer.write(s1f11.encode())
        await writer.drain()
        self.log("S1F11 송신 — 센서 이름 목록 요청")

        # ── S1F12 수신 (센서 이름 응답) ───────────────────────────────
        raw = await asyncio.wait_for(reader.readline(), timeout=10.0)
        msg = json.loads(raw.decode().strip())
        assert msg["stream"] == 1 and msg["function"] == 12
        self.log("S1F12 수신 — 센서 목록 수신 완료")

        # 메타데이터 MQTT 발행 (최초 1회)
        metadata = self.parser.parse_s1f12(msg)
        self._publish_metadata(metadata)
        self.log("━" * 40)
        self.log("데이터 수신 시작")

    # ------------------------------------------------------------------
    async def _stream_loop(self, reader: asyncio.StreamReader):
        """S6F11 수신 루프"""
        while True:
            raw = await reader.readline()
            if not raw:
                raise ConnectionResetError("장비 연결 끊김")

            msg = json.loads(raw.decode().strip())

            if msg.get("stream") == 6 and msg.get("function") == 11:
                telemetry = self.parser.parse_s6f11(msg)
                self._publish_telemetry(telemetry)
                self.tick += 1

                # 5틱마다 콘솔 요약 출력
                if self.tick % 5 == 0:
                    status = telemetry["status"]
                    log_sensor = next(
                        (s for s in telemetry["sensors"]
                         if s["name"] == "Log_Message" and s["value"]),
                        None
                    )
                    log_str = f" | {log_sensor['value']}" if log_sensor else ""
                    self.log(f"tick={self.tick:04d} | {status}{log_str}")

    # ------------------------------------------------------------------
    def _publish_metadata(self, payload: dict):
        topic = f"{MQTT_TOPIC_PREFIX}/{self.equipment_id}/metadata"
        body  = json.dumps(payload, ensure_ascii=False)

        self.mqtt.publish(topic, body)
        self.log(f"MQTT 발행 → {topic}")
        self.log(f"  센서 {len(payload['sensors'])}개 등록")
        for s in payload["sensors"]:
            self.log(f"    svid={s['svid']:02d} | {s['name']:<28} {s['dataType']}")

    # ------------------------------------------------------------------
    def _publish_telemetry(self, payload: dict):
        topic = f"{MQTT_TOPIC_PREFIX}/{self.equipment_id}/telemetry"
        body  = json.dumps(payload, ensure_ascii=False)
        self.mqtt.publish(topic, body)


# =============================================================================
# 메인
# =============================================================================

async def main():
    print("""
╔══════════════════════════════════════════════════════════╗
║  Edge Gateway Server                                    ║
║  SECS/GEM → JSON → MQTT                                 ║
╚══════════════════════════════════════════════════════════╝
""")

    # MQTT 브로커 연결 (공유 클라이언트)
    mqtt_client = MqttClient()
    mqtt_client.connect()

    # 브로커 연결 안정화 대기
    await asyncio.sleep(1.5)

    # 각 장비별 연결 핸들러 동시 실행
    connections = [
        DeviceConnection(eq_id, cfg["type"], cfg["port"], mqtt_client)
        for eq_id, cfg in DEVICES.items()
    ]

    try:
        await asyncio.gather(*[c.run_forever() for c in connections])
    finally:
        mqtt_client.disconnect()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[INFO] 엣지 게이트웨이 종료")
