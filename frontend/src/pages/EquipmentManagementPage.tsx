import { useMemo, useState, type ReactNode } from "react";
import { useOutletContext } from "react-router-dom";

import type { DashboardState } from "../hooks/useDashboardState";

type MainLayoutContext = DashboardState & {
  canEditDashboard: boolean;
};

const statusLabels = {
  ready: "등록 가능",
  empty: "센서 없음",
  loading: "불러오는 중",
};

function IconButton({
  children,
  label,
  onClick,
  disabled,
  tone = "slate",
}: {
  children: ReactNode;
  label: string;
  onClick: () => void;
  disabled?: boolean;
  tone?: "slate" | "cyan" | "emerald" | "rose";
}) {
  const toneClass = {
    slate: "border-slate-700 bg-slate-900 text-slate-300 hover:border-slate-500 hover:text-white",
    cyan: "border-cyan-500/40 bg-cyan-500/10 text-cyan-200 hover:bg-cyan-500/20",
    emerald: "border-emerald-500/40 bg-emerald-500/10 text-emerald-200 hover:bg-emerald-500/20",
    rose: "border-rose-500/40 bg-rose-500/10 text-rose-200 hover:bg-rose-500/20",
  };

  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      className={[
        "flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        toneClass[tone],
      ].join(" ")}
    >
      {children}
    </button>
  );
}

export default function EquipmentManagementPage() {
  const state = useOutletContext<MainLayoutContext>();
  const [equipmentSearch, setEquipmentSearch] = useState("");
  const [sensorSearch, setSensorSearch] = useState("");

  const selectedEquipment = state.allEquipments.find(
    (equipment) => equipment.id === state.tempSelection.eqId,
  );
  const selectedLiveEquipment = selectedEquipment
    ? state.equipmentById[selectedEquipment.id]
    : undefined;
  const selectedSensors = selectedLiveEquipment?.sensors.map((sensor) => ({
    id: sensor.sensorId ?? sensor.label,
    label: sensor.label,
    unit: sensor.unit,
    dataType: sensor.dataType,
  })) ?? selectedEquipment?.sensors ?? [];

  const filteredEquipments = useMemo(() => {
    const keyword = equipmentSearch.trim().toLowerCase();

    if (!keyword) return state.allEquipments;

    return state.allEquipments.filter((equipment) =>
      [equipment.name, equipment.type, equipment.id].some((value) =>
        String(value ?? "").toLowerCase().includes(keyword),
      ),
    );
  }, [equipmentSearch, state.allEquipments]);

  const filteredSensors = useMemo(() => {
    const keyword = sensorSearch.trim().toLowerCase();
    const sensors = selectedSensors;

    if (!keyword) return sensors;

    return sensors.filter((sensor) =>
      [sensor.label, sensor.id, sensor.unit, sensor.dataType].some((value) =>
        String(value ?? "").toLowerCase().includes(keyword),
      ),
    );
  }, [selectedSensors, sensorSearch]);

  const handleSelectEquipment = (equipmentId: string) => {
    setSensorSearch("");
    state.setTempSelection({ eqId: equipmentId, sensorId: "" });
    void state.loadEquipmentCurrent(equipmentId);
  };

  const handleDeleteEquipment = (equipmentId: string, equipmentName: string) => {
    const confirmed = window.confirm(
      `${equipmentName} 장비를 대시보드에서 제거할까요? 장비와 센서 데이터는 보존되고, 연결된 위젯만 화면에서 제거됩니다.`,
    );

    if (!confirmed) return;

    void state.removeEquipment(equipmentId);
  };

  const totalSensorCount = state.allEquipments.reduce(
    (sum, equipment) => sum + (state.equipmentById[equipment.id]?.sensors.length ?? equipment.sensors.length),
    0,
  );
  const selectedStatus =
    state.loadingSensorEquipmentId === selectedEquipment?.id
      ? statusLabels.loading
      : selectedEquipment && selectedSensors.length > 0
        ? statusLabels.ready
        : statusLabels.empty;

  return (
    <div className="min-h-full bg-[#0B0F1A] px-4 py-5 md:px-6">
      <div className="mb-5 flex flex-col gap-4 border-b border-slate-800 pb-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="min-w-0">
          <p className="text-[10px] font-black uppercase tracking-wider text-cyan-300">
            Equipment Registry
          </p>
          <h2 className="mt-2 text-2xl font-black text-white">설비 관리</h2>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-400">
            네트워크에서 감지한 설비를 등록하고, 센서 목록을 확인하며, 사용하지 않는 장비를 화면에서 정리합니다.
          </p>
        </div>

        {state.canEditDashboard && (
          <div className="flex flex-wrap gap-2">
            <button
              type="button"
              onClick={state.startNetworkScan}
              disabled={state.isNetworkScanning}
              className="rounded-lg bg-cyan-600 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-cyan-500/20 transition-colors hover:bg-cyan-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {state.isNetworkScanning ? "스캔 중..." : "네트워크 스캔"}
            </button>
            <button
              type="button"
              onClick={() => void state.applyEquipmentRegistration()}
              disabled={state.isNetworkScanning || state.allEquipments.length === 0}
              className="rounded-lg bg-emerald-600 px-4 py-2 text-xs font-bold text-white shadow-lg shadow-emerald-600/20 transition-colors hover:bg-emerald-500 disabled:cursor-not-allowed disabled:opacity-60"
            >
              등록 및 동기화
            </button>
          </div>
        )}
      </div>

      <div className="mb-5 grid gap-3 sm:grid-cols-3">
        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-4 py-3">
          <div className="text-[10px] font-bold uppercase text-slate-500">Equipment</div>
          <div className="mt-1 text-2xl font-black text-white">{state.allEquipments.length}</div>
        </div>
        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-4 py-3">
          <div className="text-[10px] font-bold uppercase text-slate-500">Sensors</div>
          <div className="mt-1 text-2xl font-black text-white">{totalSensorCount}</div>
        </div>
        <div className="rounded-lg border border-slate-800 bg-slate-900/50 px-4 py-3">
          <div className="text-[10px] font-bold uppercase text-slate-500">Selected</div>
          <div className="mt-1 truncate text-lg font-black text-white">
            {selectedEquipment?.name ?? "없음"}
          </div>
        </div>
      </div>

      <div className="grid min-h-[560px] gap-5 xl:grid-cols-[0.95fr_1.45fr]">
        <section className="flex min-h-0 flex-col overflow-hidden rounded-lg border border-slate-800 bg-[#0D1117]">
          <header className="border-b border-slate-800 p-4">
            <div className="flex items-center justify-between gap-3">
              <h3 className="text-sm font-black text-white">장비 목록</h3>
              <span className="rounded-md bg-cyan-500/10 px-2 py-1 text-[10px] font-bold text-cyan-300">
                {filteredEquipments.length} EA
              </span>
            </div>
            <input
              type="search"
              value={equipmentSearch}
              onChange={(event) => setEquipmentSearch(event.target.value)}
              placeholder="장비명, 타입, ID 검색"
              className="mt-4 h-10 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm text-white outline-none placeholder:text-slate-600 focus:border-cyan-400"
            />
          </header>

          <div className="min-h-0 flex-1 space-y-2 overflow-y-auto p-3">
            {filteredEquipments.length === 0 ? (
              <div className="flex h-full min-h-72 items-center justify-center px-6 text-center text-sm text-slate-500">
                등록된 장비가 없습니다.
              </div>
            ) : (
              filteredEquipments.map((equipment) => {
                const isSelected = equipment.id === state.tempSelection.eqId;
                const isLoading = state.loadingSensorEquipmentId === equipment.id;

                return (
                  <div
                    key={equipment.id}
                    className={[
                      "rounded-lg border p-3 transition-colors",
                      isSelected
                        ? "border-cyan-400 bg-cyan-500/10"
                        : "border-slate-800 bg-slate-900/50 hover:border-slate-700",
                    ].join(" ")}
                  >
                    <div className="flex items-start justify-between gap-3">
                      <button
                        type="button"
                        onClick={() => handleSelectEquipment(equipment.id)}
                        className="min-w-0 flex-1 text-left"
                      >
                        <div className="truncate text-sm font-black text-white">{equipment.name}</div>
                        <div className="mt-1 flex flex-wrap gap-2 text-[10px] font-bold uppercase text-slate-500">
                          <span>{equipment.type || "UNKNOWN"}</span>
                          <span>{equipment.id}</span>
                          <span>{isLoading ? "Loading" : `${state.equipmentById[equipment.id]?.sensors.length ?? equipment.sensors.length} Tags`}</span>
                        </div>
                      </button>

                      {state.canEditDashboard && (
                        <IconButton
                          label={`${equipment.name} 대시보드에서 제거`}
                          onClick={() => handleDeleteEquipment(equipment.id, equipment.name)}
                          disabled={state.isNetworkScanning}
                          tone="rose"
                        >
                          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                            <path d="M6 7h12M10 11v6M14 11v6M9 7l1-2h4l1 2M8 7l1 13h6l1-13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                          </svg>
                        </IconButton>
                      )}
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </section>

        <section className="flex min-h-0 flex-col overflow-hidden rounded-lg border border-slate-800 bg-[#0D1117]">
          <header className="border-b border-slate-800 p-4">
            <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
              <div className="min-w-0">
                <p className="text-[10px] font-bold uppercase text-slate-500">Tag Explorer</p>
                <h3 className="mt-1 truncate text-lg font-black text-white">
                  {selectedEquipment?.name ?? "장비를 선택하세요"}
                </h3>
                <p className="mt-1 text-xs font-semibold text-emerald-400">{selectedStatus}</p>
              </div>

              {selectedEquipment && state.canEditDashboard && (
                <div className="flex gap-2">
                  <IconButton
                    label="센서 새로고침"
                    onClick={() => void state.loadEquipmentCurrent(selectedEquipment.id)}
                    disabled={state.loadingSensorEquipmentId === selectedEquipment.id}
                    tone="cyan"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M20 12a8 8 0 0 1-14.2 5M4 12A8 8 0 0 1 18.2 7M18 3v4h-4M6 21v-4h4" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </IconButton>
                  <IconButton
                    label={`${selectedEquipment.name} 대시보드에서 제거`}
                    onClick={() => handleDeleteEquipment(selectedEquipment.id, selectedEquipment.name)}
                    disabled={state.isNetworkScanning}
                    tone="rose"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                      <path d="M6 7h12M10 11v6M14 11v6M9 7l1-2h4l1 2M8 7l1 13h6l1-13" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
                    </svg>
                  </IconButton>
                </div>
              )}
            </div>

            <input
              type="search"
              value={sensorSearch}
              onChange={(event) => setSensorSearch(event.target.value)}
              placeholder="센서명, 타입, 단위 검색"
              disabled={!selectedEquipment}
              className="mt-4 h-10 w-full rounded-lg border border-slate-700 bg-slate-900 px-3 text-sm text-white outline-none placeholder:text-slate-600 focus:border-cyan-400 disabled:cursor-not-allowed disabled:opacity-50"
            />
          </header>

          <div className="min-h-0 flex-1 overflow-y-auto p-4">
            {!selectedEquipment ? (
              <div className="flex h-full min-h-96 items-center justify-center text-center text-sm text-slate-500">
                왼쪽 목록에서 장비를 선택하면 센서가 표시됩니다.
              </div>
            ) : state.loadingSensorEquipmentId === selectedEquipment.id ? (
              <div className="flex h-full min-h-96 items-center justify-center text-center text-sm text-slate-500">
                센서 목록을 불러오는 중입니다.
              </div>
            ) : filteredSensors.length === 0 ? (
              <div className="flex h-full min-h-96 items-center justify-center text-center text-sm text-slate-500">
                표시할 센서가 없습니다.
              </div>
            ) : (
              <div className="grid gap-3 md:grid-cols-2 2xl:grid-cols-3">
                {filteredSensors.map((sensor) => (
                  <div
                    key={sensor.id}
                    className="rounded-lg border border-slate-800 bg-slate-900/60 p-4"
                  >
                    <div className="truncate text-sm font-bold text-white">{sensor.label}</div>
                    <div className="mt-2 flex flex-wrap gap-2 text-[10px] font-bold uppercase text-slate-500">
                      <span className="rounded bg-slate-950/50 px-2 py-1">{sensor.id}</span>
                      <span className="rounded bg-slate-950/50 px-2 py-1">{sensor.dataType ?? "FLOAT"}</span>
                      {sensor.unit && <span className="rounded bg-slate-950/50 px-2 py-1">{sensor.unit}</span>}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
