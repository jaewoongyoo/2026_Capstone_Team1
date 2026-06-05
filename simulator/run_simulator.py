#!/usr/bin/env python3
"""
================================================================================
run_simulator.py — 시뮬레이터 전체 실행 진입점
================================================================================
역할:
  - edge/edge_gateway.py      (게이트웨이 서버)
  - simulator/secsgem_device.py (장비 시뮬레이터 클라이언트)
  두 프로세스를 동시에 실행하고 Ctrl+C 로 함께 종료

실행:
  python3 simulator/run_simulator.py

개별 실행도 가능:
  python3 edge/edge_gateway.py
  python3 simulator/secsgem_device.py
================================================================================
"""

import asyncio
import subprocess
import sys
import time
from pathlib import Path

BASE_DIR = Path(__file__).parent
ROOT_DIR = BASE_DIR.parent


def print_banner():
    print("""
╔══════════════════════════════════════════════════════════════╗
║  Semiconductor Equipment Simulator                          ║
║                                                            ║
║  [Device]   CVD-CHAMBER-01 (→ :5000)                      ║
║             ETCHER-01      (:5001)                        ║
║  [Gateway]  SECS/GEM TCP Server → JSON → MQTT             ║
║  [Broker]   localhost:1883                                 ║
║                                                            ║
║  Ctrl+C 로 전체 종료                                        ║
╚══════════════════════════════════════════════════════════════╝
""")


def main():
    print_banner()

    python = sys.executable  # 현재 가상환경의 python 경로

    # ── 프로세스 시작 ──────────────────────────────────────────────────
    print("[RUN] 게이트웨이 서버 시작...")
    gateway_proc = subprocess.Popen(
        [python, str(ROOT_DIR / "edge" / "edge_gateway.py")],
        stdout=sys.stdout,
        stderr=sys.stderr,
    )

    # 게이트웨이가 포트를 열 때까지 잠깐 대기
    time.sleep(1.5)

    print("[RUN] 장비 시뮬레이터 시작...")
    device_proc = subprocess.Popen(
        [python, str(BASE_DIR / "secsgem_device.py")],
        stdout=sys.stdout,
        stderr=sys.stderr,
    )

    print("[RUN] ✅ 전체 시뮬레이터 실행 중 — Ctrl+C 로 종료\n")

    # ── 종료 대기 ──────────────────────────────────────────────────────
    try:
        while True:
            # 프로세스가 예상치 않게 죽으면 알림
            if device_proc.poll() is not None:
                print("[WARN] 장비 시뮬레이터가 종료됨 — 게이트웨이도 중지합니다")
                break
            if gateway_proc.poll() is not None:
                print("[WARN] 게이트웨이 서버가 종료됨")
                break
            time.sleep(1)

    except KeyboardInterrupt:
        print("\n[INFO] 종료 신호 수신 — 프로세스 정리 중...")

    finally:
        for name, proc in [("장비 시뮬레이터", device_proc),
                           ("게이트웨이", gateway_proc)]:
            if proc.poll() is None:
                proc.terminate()
                try:
                    proc.wait(timeout=3)
                    print(f"[INFO] {name} 정상 종료")
                except subprocess.TimeoutExpired:
                    proc.kill()
                    print(f"[INFO] {name} 강제 종료")

        print("[INFO] 시뮬레이터 전체 종료 완료")


if __name__ == "__main__":
    main()
