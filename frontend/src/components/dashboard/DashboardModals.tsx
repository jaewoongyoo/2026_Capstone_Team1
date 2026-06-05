import type { ReactNode } from "react";
import type { DashboardWidgetType, SelectedData } from "../../types/dashboard";
import type { DashboardState } from "../../hooks/useDashboardState";

type DashboardModalsProps = {
  state: DashboardState;
};

type ModalFrameProps = {
  title: string;
  subtitle?: string;
  maxWidth?: string;
  children: ReactNode;
  onClose: () => void;
};

const widgetTypes: DashboardWidgetType[] = [
  "GAUGE",
  "TREND",
  "DONUT",
  "STATUS",
  "LOG",
  "BAR_V",
  "BAR_H",
];

const widgetTypeLabels: Record<DashboardWidgetType, string> = {
  OEE: "KPI",
  SENSORS: "센서 그리드",
  TREND: "추세 차트",
  ALERTS: "알림 피드",
  GAUGE: "게이지",
  DONUT: "도넛",
  STATUS: "상태",
  LOG: "로그",
  BAR_V: "세로 막대",
  BAR_H: "가로 막대",
};

function getWidgetAvailability(type: DashboardWidgetType, selectedData: SelectedData[]) {
  const isMulti = selectedData.length > 1;
  const dataTypes = new Set(selectedData.map((item) => item.dataType ?? "FLOAT"));
  const numericTypes = new Set(["FLOAT", "DOUBLE", "INTEGER", "INT"]);
  const isBooleanOnly = dataTypes.size === 1 && dataTypes.has("BOOLEAN");
  const isNumericOnly = [...dataTypes].every((dataType) => numericTypes.has(dataType));
  const isMixed = !isBooleanOnly && !isNumericOnly;

  if (type === "GAUGE" && (isMulti || !isNumericOnly)) {
    return { disabled: true, recommended: false, reason: isMulti ? "단일 수치 전용" : "수치 데이터 전용" };
  }

  if (type === "STATUS" && (isMulti || !isBooleanOnly)) {
    return { disabled: true, recommended: false, reason: isMulti ? "단일 상태 전용" : "상태 데이터 전용" };
  }

  if ((type === "TREND" || type === "BAR_V" || type === "BAR_H") && !isNumericOnly) {
    return { disabled: true, recommended: false, reason: "수치 데이터 전용" };
  }

  if (type === "DONUT" && isMixed) {
    return { disabled: true, recommended: false, reason: "동일 타입 데이터 전용" };
  }

  const recommended =
    (isMulti && isNumericOnly && (type === "TREND" || type === "BAR_V" || type === "BAR_H")) ||
    (!isMulti && isNumericOnly && (type === "GAUGE" || type === "TREND")) ||
    (isBooleanOnly && (type === "STATUS" || type === "DONUT")) ||
    (isMixed && type === "LOG");

  return {
    disabled: false,
    recommended,
    reason: recommended ? "추천" : "선택 가능",
  };
}

function ModalFrame({
  title,
  subtitle,
  maxWidth = "max-w-3xl",
  children,
  onClose,
}: ModalFrameProps) {
  return (
    <div className="fixed inset-0 z-[120] flex items-center justify-center bg-black/80 p-4 backdrop-blur-md">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="dashboard-modal-title"
        className={`flex max-h-[90vh] w-full ${maxWidth} flex-col overflow-hidden rounded-lg border border-slate-700 bg-[#111827] shadow-2xl`}
      >
        <header className="flex items-start justify-between gap-4 border-b border-slate-800 px-6 py-5">
          <div>
            <h2 id="dashboard-modal-title" className="text-xl font-black text-white">
              {title}
            </h2>
            {subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
          </div>
          <button
            type="button"
            onClick={onClose}
            className="rounded-md px-2 py-1 text-sm font-bold text-slate-500 transition-colors hover:bg-slate-800 hover:text-white"
            aria-label="모달 닫기"
          >
            x
          </button>
        </header>
        {children}
      </section>
    </div>
  );
}

function WidgetBuilderModal({ state }: DashboardModalsProps) {
  const {
    allEquipments,
    builderStep,
    newWidgetConfig,
    selectedDataCart,
    tempSelection,
    loadingSensorEquipmentId,
    searchTerm,
    setBuilderStep,
    setNewWidgetConfig,
    setSearchTerm,
    setTempSelection,
    selectEquipmentForDiscovery,
    resetWidgetBuilder,
    addSelectedSensorToCart,
    removeSelectedSensorFromCart,
    goToBuilderStep2,
    addWidgetToDashboard,
  } = state;

  return (
    <ModalFrame
      title={`위젯 만들기 - ${builderStep}/2단계`}
      subtitle="데이터 소스를 고르고 시각화 유형을 선택합니다."
      onClose={resetWidgetBuilder}
    >
      <div className="min-h-0 flex-1 overflow-y-auto p-6">
        {builderStep === 1 && (
          <div className="grid gap-5 lg:grid-cols-2">
            <section className="flex h-[500px] min-h-0 flex-col rounded-lg border border-slate-800 bg-slate-900/50 p-5">
              <h3 className="mb-4 text-sm font-bold text-slate-300">1. 데이터 소스 선택</h3>

              <label className="mb-1 text-[10px] font-bold uppercase text-slate-500">
                Equipment
              </label>
              <select
                value={tempSelection.eqId}
                className="mb-4 rounded-lg border border-slate-700 bg-slate-800 p-3 text-sm text-white outline-none transition-colors focus:border-cyan-400"
                onChange={(event) => {
                  selectEquipmentForDiscovery(event.target.value);
                  setSearchTerm("");
                }}
              >
                <option value="">장비를 선택하세요</option>
                {allEquipments.map((equipment) => (
                  <option key={equipment.id} value={equipment.id}>
                    {equipment.name}
                  </option>
                ))}
              </select>

              <label className="mb-1 text-[10px] font-bold uppercase text-slate-500">
                Sensors
              </label>
              <div className="flex min-h-0 flex-1 flex-col">
                <div className="flex h-[260px] shrink-0 flex-col overflow-hidden rounded-lg border border-slate-700 bg-slate-800/50">
                  <input
                    type="search"
                    placeholder="센서 이름 검색"
                    value={searchTerm}
                    onChange={(event) => setSearchTerm(event.target.value)}
                    className="border-b border-slate-700 bg-slate-800 px-3 py-3 text-sm text-white outline-none placeholder:text-slate-500"
                  />
                  <div className="min-h-0 flex-1 space-y-1 overflow-y-auto p-2">
                    {!tempSelection.eqId ? (
                      <div className="flex h-full items-center justify-center text-center text-xs text-slate-500">
                        장비를 먼저 선택해주세요.
                      </div>
                    ) : loadingSensorEquipmentId === tempSelection.eqId ? (
                      <div className="flex h-full items-center justify-center text-center text-xs text-slate-500">
                        센서 목록을 불러오는 중입니다.
                      </div>
                    ) : (
                      allEquipments
                        .find((equipment) => equipment.id === tempSelection.eqId)
                        ?.sensors.filter((sensor) =>
                          sensor.label.toLowerCase().includes(searchTerm.toLowerCase()),
                        )
                        .map((sensor) => (
                          <button
                            type="button"
                            key={sensor.id}
                            onClick={() =>
                              setTempSelection({ ...tempSelection, sensorId: sensor.id })
                            }
                            className={[
                              "flex w-full items-center justify-between gap-3 rounded-md px-3 py-2 text-left text-sm transition-colors",
                              tempSelection.sensorId === sensor.id
                                ? "bg-cyan-600 text-white"
                                : "text-slate-300 hover:bg-slate-700",
                            ].join(" ")}
                          >
                            <span className="truncate">{sensor.label}</span>
                            <span className="shrink-0 rounded bg-slate-950/40 px-2 py-0.5 text-[10px] text-slate-400">
                              {sensor.dataType ?? "FLOAT"}
                            </span>
                          </button>
                        ))
                    )}
                  </div>
                </div>

                <button
                  type="button"
                  onClick={addSelectedSensorToCart}
                  className="mt-4 shrink-0 rounded-lg border border-cyan-500/50 bg-cyan-500/10 py-3 text-sm font-bold text-cyan-300 transition-colors hover:bg-cyan-600 hover:text-white"
                >
                  선택 목록에 추가
                </button>
              </div>
            </section>

            <section className="flex h-[500px] min-h-0 flex-col rounded-lg border border-slate-800 bg-slate-900/50 p-5">
              <h3 className="mb-4 text-sm font-bold text-slate-300">
                선택된 데이터 ({selectedDataCart.length})
              </h3>
              <div className="min-h-0 flex-1 space-y-2 overflow-y-auto">
                {selectedDataCart.length === 0 ? (
                  <div className="flex h-full items-center justify-center text-sm text-slate-600">
                    선택된 데이터가 없습니다.
                  </div>
                ) : (
                  selectedDataCart.map((item, index) => (
                    <div
                      key={`${item.eqId}-${item.sensorId}`}
                      className="flex items-center justify-between gap-3 rounded-lg border border-slate-700 bg-slate-800 p-3"
                    >
                      <div className="min-w-0">
                        <div className="truncate text-xs text-slate-400">{item.eqName}</div>
                        <div className="truncate text-sm font-bold text-white">{item.sensorLabel ?? item.sensorId}</div>
                        <div className="mt-1 text-[10px] font-bold uppercase text-slate-500">
                          {item.dataType ?? "FLOAT"}
                        </div>
                      </div>
                      <button
                        type="button"
                        onClick={() => removeSelectedSensorFromCart(index)}
                        className="rounded-md bg-rose-500/10 px-2 py-1 text-xs font-bold text-rose-400 transition-colors hover:bg-rose-500/20"
                      >
                        제거
                      </button>
                    </div>
                  ))
                )}
              </div>
            </section>
          </div>
        )}

        {builderStep === 2 && (
          <div>
            <h3 className="mb-4 text-sm font-bold text-slate-300">2. 시각화 유형 선택</h3>
            <div className="mb-6 flex flex-wrap gap-2 rounded-lg border border-slate-800 bg-slate-900/50 p-4">
              {selectedDataCart.map((item) => (
                <span
                  key={`${item.eqId}-${item.sensorId}`}
                  className="rounded-md border border-cyan-500/30 bg-cyan-500/10 px-3 py-1 text-xs text-cyan-200"
                >
                  {item.eqName} - {item.sensorLabel ?? item.sensorId}
                </span>
              ))}
            </div>

            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {widgetTypes.map((type) => {
                const availability = getWidgetAvailability(type, selectedDataCart);
                const isDisabled = availability.disabled;
                const isSelected = newWidgetConfig.type === type;

                return (
                  <button
                    type="button"
                    key={type}
                    disabled={isDisabled}
                    onClick={() => setNewWidgetConfig({ ...newWidgetConfig, type })}
                    className={[
                      "relative rounded-lg border p-5 text-left transition-all",
                      isDisabled
                        ? "cursor-not-allowed border-slate-800 bg-slate-900 text-slate-700"
                        : isSelected
                          ? "border-cyan-300 bg-cyan-500/15 text-white shadow-lg shadow-cyan-500/20 ring-2 ring-cyan-300/50"
                          : availability.recommended
                            ? "border-emerald-400 bg-emerald-500/10 text-white shadow-lg shadow-emerald-500/10 hover:border-cyan-400"
                            : "border-slate-700 bg-slate-900 text-slate-300 hover:border-cyan-500",
                    ].join(" ")}
                  >
                    {availability.recommended && !isDisabled && (
                      <span className="absolute right-3 top-3 rounded bg-emerald-500 px-2 py-0.5 text-[9px] font-black uppercase text-slate-950">
                        추천
                      </span>
                    )}
                    {isSelected && !isDisabled && (
                      <span className="absolute bottom-3 right-3 flex h-5 w-5 items-center justify-center rounded-full bg-cyan-300 text-[11px] font-black text-slate-950">
                        ✓
                      </span>
                    )}
                    <span className="block text-sm font-black">{widgetTypeLabels[type]}</span>
                    <span className="mt-1 block text-[10px] font-bold uppercase tracking-wider text-slate-500">
                      {type}
                    </span>
                    <span className="mt-2 block text-xs text-slate-500">
                      {availability.reason}
                    </span>
                  </button>
                );
              })}
            </div>
          </div>
        )}
      </div>

      <footer className="flex justify-end gap-3 border-t border-slate-800 px-6 py-5">
        {builderStep === 1 ? (
          <>
            <button
              type="button"
              onClick={resetWidgetBuilder}
              className="rounded-lg bg-slate-800 px-5 py-2 text-sm font-bold text-slate-400 transition-colors hover:bg-slate-700"
            >
              취소
            </button>
            <button
              type="button"
              onClick={goToBuilderStep2}
              className="rounded-lg bg-cyan-600 px-5 py-2 text-sm font-bold text-white transition-colors hover:bg-cyan-500 disabled:cursor-not-allowed disabled:bg-slate-700 disabled:text-slate-500"
              disabled={selectedDataCart.length === 0}
            >
              다음 단계
            </button>
          </>
        ) : (
          <>
            <button
              type="button"
              onClick={() => setBuilderStep(1)}
              className="rounded-lg bg-slate-800 px-5 py-2 text-sm font-bold text-slate-400 transition-colors hover:bg-slate-700"
            >
              이전
            </button>
            <button
              type="button"
              onClick={addWidgetToDashboard}
              className="rounded-lg bg-emerald-600 px-5 py-2 text-sm font-bold text-white transition-colors hover:bg-emerald-500"
            >
              대시보드에 추가
            </button>
          </>
        )}
      </footer>
    </ModalFrame>
  );
}

function EquipmentDiscoveryModal({ state }: DashboardModalsProps) {
  const {
    allEquipments,
    isNetworkScanning,
    loadingSensorEquipmentId,
    tempSelection,
    selectEquipmentForDiscovery,
    startNetworkScan,
    closeEquipmentModal,
    applyEquipmentRegistration,
  } = state;

  const selectedEquipment = allEquipments.find((equipment) => equipment.id === tempSelection.eqId);

  return (
    <ModalFrame
      title="자동 검색"
      subtitle="게이트웨이에 연결된 장치와 하위 태그를 확인합니다."
      maxWidth="max-w-5xl"
      onClose={closeEquipmentModal}
    >
      <div className="flex min-h-0 flex-1 flex-col gap-5 overflow-hidden p-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-[10px] font-bold uppercase tracking-wider text-slate-500">
              Network Status
            </p>
            <p className="mt-1 text-xs font-mono text-emerald-400">OPC-UA v2.1 Connected</p>
          </div>
          <button
            type="button"
            onClick={startNetworkScan}
            disabled={isNetworkScanning}
            className="rounded-lg bg-cyan-600 px-5 py-3 text-sm font-black text-white transition-colors hover:bg-cyan-500"
          >
            {isNetworkScanning ? "스캔 중..." : "네트워크 스캔 시작"}
          </button>
        </div>

        <div className="grid min-h-0 flex-1 gap-5 lg:grid-cols-[0.85fr_1.15fr]">
          <section className="flex min-h-[360px] flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40">
            <header className="flex items-center justify-between border-b border-slate-800 px-4 py-3">
              <span className="text-[10px] font-black uppercase tracking-wider text-slate-400">
                Detected Assets
              </span>
              <span className="rounded-md bg-cyan-500/10 px-2 py-1 text-[10px] font-bold text-cyan-300">
                {allEquipments.length} EA
              </span>
            </header>
            <div className="min-h-0 flex-1 space-y-1 overflow-y-auto p-2">
              {allEquipments.length === 0 ? (
                <div className="flex h-full items-center justify-center px-4 text-center text-xs text-slate-500">
                  스캔된 장비가 없습니다.
                </div>
              ) : allEquipments.map((equipment) => (
                <button
                  type="button"
                  key={equipment.id}
                  onClick={() => selectEquipmentForDiscovery(equipment.id)}
                  className={[
                    "flex w-full items-center justify-between gap-3 rounded-lg border px-4 py-3 text-left text-sm transition-colors",
                    tempSelection.eqId === equipment.id
                      ? "border-cyan-400 bg-cyan-500/10 text-white"
                      : "border-transparent text-slate-400 hover:bg-slate-800",
                  ].join(" ")}
                >
                  <span className="truncate">{equipment.name}</span>
                  <span className="shrink-0 rounded-md bg-slate-800 px-2 py-1 text-[9px] text-slate-400">
                    {equipment.sensorsLoaded ? equipment.sensors.length : "-"} Tags
                  </span>
                </button>
              ))}
            </div>
          </section>

          <section className="flex min-h-[360px] flex-col overflow-hidden rounded-lg border border-slate-800 bg-slate-900/40">
            <header className="border-b border-slate-800 px-4 py-3">
              <span className="text-[10px] font-black uppercase tracking-wider text-slate-400">
                Tag Explorer
              </span>
            </header>
            <div className="grid min-h-0 flex-1 content-start gap-3 overflow-y-auto p-4 sm:grid-cols-2">
              {!selectedEquipment ? (
                <div className="col-span-full flex min-h-72 items-center justify-center text-center text-sm text-slate-600">
                  장비를 선택하면 하위 태그가 표시됩니다.
                </div>
              ) : loadingSensorEquipmentId === selectedEquipment.id ? (
                <div className="col-span-full flex min-h-72 items-center justify-center text-center text-sm text-slate-600">
                  센서 목록을 불러오는 중입니다.
                </div>
              ) : selectedEquipment.sensors.length === 0 ? (
                <div className="col-span-full flex min-h-72 items-center justify-center text-center text-sm text-slate-600">
                  등록된 센서가 없습니다.
                </div>
              ) : (
                selectedEquipment.sensors.map((sensor) => (
                  <div
                    key={sensor.id}
                    className="rounded-lg border border-slate-700/70 bg-slate-800/50 p-4"
                  >
                    <div className="truncate text-xs font-mono text-slate-200">{sensor.label}</div>
                    <div className="mt-2 flex items-center gap-2 text-[10px] font-bold uppercase text-slate-500">
                      <span>{sensor.unit}</span>
                      <span className="h-1 w-1 rounded-full bg-slate-700" />
                      <span>Analog Input</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          </section>
        </div>
      </div>

      <footer className="flex flex-col gap-3 border-t border-slate-800 px-6 py-5 sm:flex-row sm:items-center sm:justify-between">
        <span className="text-xs text-slate-500">
          Ready to map {allEquipments.reduce((sum, item) => sum + item.sensors.length, 0)} tags
        </span>
        <div className="flex justify-end gap-3">
          <button
            type="button"
            onClick={closeEquipmentModal}
            className="rounded-lg bg-slate-800 px-5 py-2 text-sm font-bold text-slate-400 transition-colors hover:bg-slate-700"
          >
            취소
          </button>
          <button
            type="button"
            onClick={applyEquipmentRegistration}
            className="rounded-lg bg-emerald-600 px-5 py-2 text-sm font-bold text-white transition-colors hover:bg-emerald-500"
          >
            적용 및 등록
          </button>
        </div>
      </footer>
    </ModalFrame>
  );
}

export default function DashboardModals({ state }: DashboardModalsProps) {
  return (
    <>
      {state.isModalOpen && <WidgetBuilderModal state={state} />}
      {state.isEqModalOpen && <EquipmentDiscoveryModal state={state} />}
    </>
  );
}
