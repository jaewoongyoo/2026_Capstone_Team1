#!/usr/bin/env python3
"""
================================================================================
secsgem_device.py — SECS/GEM 장비 시뮬레이터 (TCP 소켓 클라이언트)
================================================================================
역할:
  - 실제 반도체 장비(CVD, ETCHER)를 흉내내는 독립 TCP 클라이언트
  - 엣지 게이트웨이로 TCP 접속 → 핸드셰이크 → 주기적 S6F11 전송
  - 4가지 데이터 타입 혼합: FLOAT / BOOLEAN / INTEGER / STRING
 
메시지 흐름:
  게이트웨이 → 장비 : S1F13  (연결 요청)
  장비 → 게이트웨이 : S1F14  (연결 승인)
  게이트웨이 → 장비 : S1F11  (센서 이름 목록 요청)
  장비 → 게이트웨이 : S1F12  (센서 이름/타입/단위 응답)
  장비 → 게이트웨이 : S6F11  (1초마다 센서 데이터 전송) ← 반복
 
실행:
  EDGE_GATEWAY_HOST={엣지게이트웨이 IP} python3 simulator/secsgem_device.py
================================================================================
"""
 
import asyncio
import json
import math
import random
import sys
from datetime import datetime, timezone
from pathlib import Path
 
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from common.config import (
    EDGE_GATEWAY_HOST,
    DEVICES,
    PUBLISH_INTERVAL_SEC,
    SENSOR_DEFINITIONS,
    BASE_VALUES,
    SCENARIO,
)
 
 
# =============================================================================
# 센서 데이터 생성기
# =============================================================================
 
class SensorDataGenerator:
    """
    시나리오 기반 센서값 생성기.
 
    SCENARIO 를 순서대로 실행하며 각 스텝의 지속 틱이 끝나면 다음 스텝으로 이동.
    스텝 진입 시 Log_Message 를 갱신하고, Equipment_State 도 함께 바뀜.
    FLOAT 은 기준값 + 사인파 + 가우시안 노이즈로 자연스러운 흔들림 표현.
    """
 
    def __init__(self, equipment_id: str, equipment_type: str):
        self.equipment_id   = equipment_id
        self.equipment_type = equipment_type
        self.sensor_defs    = SENSOR_DEFINITIONS[equipment_type]
        self.base           = dict(BASE_VALUES[equipment_type])
        self.scenario       = SCENARIO[equipment_type]
 
        self.tick           = 0
        self.wafer_count    = 0
        self.step_index     = 0
        self.step_tick      = 0
 
        # 현재 스텝 진입 시 로그 세팅
        self._enter_step()
 
    # ------------------------------------------------------------------
    def _enter_step(self):
        state, duration, log_tpl = self.scenario[self.step_index]
        self.current_state    = state
        self.current_duration = duration
        self.current_log      = log_tpl.format(
            eq    = self.equipment_id,
            wafer = self.wafer_count,
        )
        self.step_tick = 0
 
        # 스텝 진입 로그 출력
        print(f"  [{self.equipment_id}] 📋 {self.current_log}")
 
    # ------------------------------------------------------------------
    def _next_step(self):
        """다음 시나리오 스텝으로 이동. 마지막 스텝이면 처음으로 순환."""
        prev_state = self.current_state
 
        self.step_index = (self.step_index + 1) % len(self.scenario)
 
        # RUNNING → COOLDOWN 전환 시 웨이퍼 카운트 증가
        if prev_state == "RUNNING" and self.scenario[self.step_index][0] == "COOLDOWN":
            self.wafer_count += 1
 
        self._enter_step()
 
    # ------------------------------------------------------------------
    def next(self) -> dict:
        self.tick      += 1
        self.step_tick += 1
 
        # 현재 스텝 지속 틱 초과 → 다음 스텝
        if self.step_tick >= self.current_duration:
            self._next_step()
 
        result = {}
        for sensor in self.sensor_defs:
            sid   = sensor["id"]
            dtype = sensor["dataType"]
 
            if sid == "Equipment_State":
                result[sid] = self.current_state
 
            elif sid == "Log_Message":
                # 스텝 진입 첫 틱에만 로그 전송, 이후엔 빈 문자열
                result[sid] = self.current_log if self.step_tick == 1 else ""
 
            elif sid == "Wafer_Processed_Count":
                result[sid] = self.wafer_count
 
            elif sid == "Step_Time":
                result[sid] = self.step_tick
 
            elif dtype == "FLOAT":
                base  = self.base[sid]
                wave  = math.sin(self.tick / 30.0) * base * 0.01
                noise = random.gauss(0, base * 0.005)
                result[sid] = round(base + wave + noise, 2)
 
            elif dtype == "BOOLEAN":
                # 상태에 따라 일부 BOOLEAN 자동 변환
                if sid == "Slit_Valve_State":
                    result[sid] = 1 if self.current_state == "RUNNING" else 0
                elif sid == "Chuck_State":
                    result[sid] = 1 if self.current_state in ("RUNNING", "SETUP") else 0
                else:
                    result[sid] = int(self.base[sid])
 
            elif dtype == "INTEGER":
                result[sid] = int(self.base[sid])
 
        return result
 
 
# =============================================================================
# 메시지 빌더
# =============================================================================
 
def build_message(stream: int, function: int, body: dict) -> bytes:
    """JSON + 줄바꿈으로 직렬화 (줄바꿈이 메시지 구분자)"""
    msg = {"stream": stream, "function": function, **body}
    return (json.dumps(msg, ensure_ascii=False) + "\n").encode("utf-8")
 
 
def build_s1f12(equipment_id: str, equipment_type: str) -> bytes:
    """S1F12 — 센서 이름/타입/단위 목록 응답"""
    variables = [
        {
            "svid"    : idx,
            "name"    : s["id"],
            "unit"    : s["unit"],
            "dataType": s["dataType"],
        }
        for idx, s in enumerate(SENSOR_DEFINITIONS[equipment_type], start=1)
    ]
    return build_message(1, 12, {
        "equipmentId"  : equipment_id,
        "equipmentType": equipment_type,
        "variables"    : variables,
    })
 
 
def build_s6f11(equipment_id: str, equipment_type: str, raw: dict) -> bytes:
    """S6F11 — 주기적 센서 데이터 이벤트"""
    variables = [
        {
            "svid"    : idx,
            "name"    : s["id"],
            "value"   : raw[s["id"]],
            "unit"    : s["unit"],
            "dataType": s["dataType"],
        }
        for idx, s in enumerate(SENSOR_DEFINITIONS[equipment_type], start=1)
        if s["id"] in raw
    ]
    return build_message(6, 11, {
        "equipmentId"  : equipment_id,
        "equipmentType": equipment_type,
        "timestamp"    : datetime.now(timezone.utc).isoformat(),
        "variables"    : variables,
    })
 
 
# =============================================================================
# 단일 장비 TCP 클라이언트
# =============================================================================
 
class DeviceServer:
    RECONNECT_DELAY = 5.0

    def __init__(self, equipment_id: str, equipment_type: str, port: int):
        self.equipment_id   = equipment_id
        self.equipment_type = equipment_type
        self.port           = port
        self.generator      = SensorDataGenerator(equipment_id, equipment_type)
 
    def log(self, msg: str):
        ts = datetime.now().strftime("%H:%M:%S")
        print(f"[{ts}][{self.equipment_id}] {msg}")
 
    # ------------------------------------------------------------------
    async def start(self):
        while True:
            try:
                self.log(f"엣지게이트웨이 접속 시도 → {EDGE_GATEWAY_HOST}:{self.port}")
                reader, writer = await asyncio.open_connection(
                    EDGE_GATEWAY_HOST, self.port
                )
                await self._run_session(reader, writer)
            except Exception as e:
                self.log(f"⚠ 접속 오류: {e}")

            self.log(f"재접속 대기 {self.RECONNECT_DELAY}초...")
            await asyncio.sleep(self.RECONNECT_DELAY)
 
    # ------------------------------------------------------------------
    async def _run_session(self,
                           reader: asyncio.StreamReader,
                           writer: asyncio.StreamWriter):
        peer = writer.get_extra_info("peername")
        self.log(f"엣지게이트웨이 연결 성공 → {peer}")
 
        try:
            # ── S1F13 수신 (연결 요청) ─────────────────────────────────
            raw = await asyncio.wait_for(reader.readline(), timeout=10.0)
            msg = json.loads(raw.decode().strip())
            assert msg["stream"] == 1 and msg["function"] == 13
            self.log("S1F13 수신 — 연결 요청")
 
            # ── S1F14 송신 (연결 승인) ─────────────────────────────────
            writer.write(build_message(1, 14, {
                "equipmentId": self.equipment_id,
                "commAck"    : 0,
            }))
            await writer.drain()
            self.log("S1F14 송신 — 연결 승인")
 
            # ── S1F11 수신 (센서 이름 요청) ───────────────────────────
            raw = await asyncio.wait_for(reader.readline(), timeout=10.0)
            msg = json.loads(raw.decode().strip())
            assert msg["stream"] == 1 and msg["function"] == 11
            self.log("S1F11 수신 — 센서 이름 목록 요청")
 
            # ── S1F12 송신 (센서 이름 응답) ───────────────────────────
            writer.write(build_s1f12(self.equipment_id, self.equipment_type))
            await writer.drain()
            self.log("S1F12 송신 — 센서 목록 전송 완료")
            self.log("━" * 40)
            self.log("S6F11 데이터 스트리밍 시작")
 
            # ── S6F11 주기 전송 루프 ──────────────────────────────────
            tick = 0
            while True:
                raw_data = self.generator.next()
                writer.write(build_s6f11(
                    self.equipment_id, self.equipment_type, raw_data
                ))
                await writer.drain()
 
                tick += 1
                # 5틱마다 요약 콘솔 출력
                if tick % 5 == 0:
                    state = raw_data.get("Equipment_State", "?")
                    log   = raw_data.get("Log_Message", "")
                    wafer = raw_data.get("Wafer_Processed_Count", 0)
                    log_str = f" | {log}" if log else ""
                    self.log(
                        f"tick={tick:04d} | {state} | wafer={wafer}{log_str}"
                    )
 
                await asyncio.sleep(PUBLISH_INTERVAL_SEC)
 
        except asyncio.TimeoutError:
            self.log("⚠ 핸드셰이크 타임아웃 — 연결 종료")
        except (ConnectionResetError, BrokenPipeError,
                asyncio.IncompleteReadError):
            self.log("연결 끊김")
        except AssertionError:
            self.log("⚠ 예상치 못한 메시지 수신")
        except Exception as e:
            self.log(f"⚠ 오류: {e}")
        finally:
            writer.close()
            await writer.wait_closed()
            self.log(f"연결 해제 → {peer}")
 
 
# =============================================================================
# 메인
# =============================================================================
 
async def main():
    print("""
╔══════════════════════════════════════════════════════════╗
║  SECS/GEM Device Simulator Client                       ║
║  CVD-CHAMBER-01 (:5000)  /  ETCHER-01 (:5001)          ║
║  FLOAT · BOOLEAN · INTEGER · STRING 혼합 전송           ║
╚══════════════════════════════════════════════════════════╝
""")
    servers = [
        DeviceServer(eq_id, cfg["type"], cfg["port"])
        for eq_id, cfg in DEVICES.items()
    ]
    await asyncio.gather(*[s.start() for s in servers])
 
 
if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        print("\n[INFO] 장비 시뮬레이터 종료")
