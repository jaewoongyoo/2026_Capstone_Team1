import { useState } from "react";
import { Responsive, useContainerWidth } from "react-grid-layout";
import { useOutletContext } from "react-router-dom";

import { WidgetRenderer } from "../../components/WidgetRenderer";
import {
  DASHBOARD_BREAKPOINTS,
  DASHBOARD_COLS,
  type DashboardBreakpoint,
  type DashboardState,
} from "../../hooks/useDashboardState";
import type { DashboardItem } from "../../types/dashboard";

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

const widgetColorOptions = [
  { label: "Indigo", value: "bg-indigo-500", hex: "#818cf8" },
  { label: "Cyan", value: "bg-cyan-500", hex: "#06b6d4" },
  { label: "Emerald", value: "bg-emerald-500", hex: "#10b981" },
  { label: "Amber", value: "bg-amber-500", hex: "#f59e0b" },
  { label: "Rose", value: "bg-rose-500", hex: "#f43f5e" },
  { label: "Violet", value: "bg-violet-500", hex: "#8b5cf6" },
  { label: "Sky", value: "bg-sky-500", hex: "#0ea5e9" },
  { label: "Pink", value: "bg-pink-500", hex: "#ec4899" },
];

type DashboardOutletContext = DashboardState & {
  canEditDashboard: boolean;
};

export default function DashboardBody() {
  const { containerRef, width, mounted } = useContainerWidth();
  const [openSettingsWidgetId, setOpenSettingsWidgetId] = useState<string | null>(null);

  const {
    alerts,
    autoArrange,
    equipment,
    equipmentById,
    layouts,
    responsiveLayouts,
    togglePinWidget,
    updateWidgetColor,
    handleLayoutChange,
    removeWidget,
    applyLayout,
    setCurrentBreakpoint,
    canEditDashboard,
  } = useOutletContext<DashboardOutletContext>();

  return (
    <main className="mx-auto max-w-[1800px] p-4">
      <div ref={containerRef}>
        {mounted && (
          <Responsive<DashboardBreakpoint>
            className="layout"
            layouts={responsiveLayouts}
            breakpoints={DASHBOARD_BREAKPOINTS}
            cols={DASHBOARD_COLS}
            rowHeight={140}
            width={width}
            margin={[20, 20]}
            containerPadding={[0, 0]}
            dragConfig={{
              enabled: canEditDashboard,
              handle: ".drag-handle",
              cancel: ".no-drag",
              threshold: 3,
            }}
            resizeConfig={{
              enabled: canEditDashboard,
              handles: ["se"],
            }}
            onBreakpointChange={(breakpoint) => {
              setCurrentBreakpoint(breakpoint);
            }}
            onDragStop={(currentLayout) => {
              if (!canEditDashboard) return;
              if (!autoArrange) return;
              applyLayout(currentLayout, true);
            }}
            onResizeStop={(currentLayout) => {
              if (!canEditDashboard) return;
              if (!autoArrange) return;
              applyLayout(currentLayout, true);
            }}
            onLayoutChange={(currentLayout, allLayouts) => {
              if (!canEditDashboard) return;
              handleLayoutChange(currentLayout, allLayouts);
            }}
          >
            {layouts.map((widget: DashboardItem) => {
              const isSettingsOpen = openSettingsWidgetId === widget.i;

              return (
                <div
                  key={widget.i}
                  className={`${widget.backgroundColor ?? "bg-[#161B26]"} relative flex flex-col overflow-hidden rounded-3xl border border-slate-800 p-6 shadow-xl group`}
                >
                  {canEditDashboard && (
                    <div
                      className={[
                        "absolute right-0 top-0 z-20 flex items-center gap-1.5 p-4 transition-opacity",
                        isSettingsOpen ? "opacity-100" : "opacity-0 group-hover:opacity-100",
                      ].join(" ")}
                    >
                      <button
                        type="button"
                        onClick={() =>
                          setOpenSettingsWidgetId((current) =>
                            current === widget.i ? null : widget.i,
                          )
                        }
                        className="no-drag rounded-md p-1 text-slate-500 transition-colors hover:bg-slate-900/70 hover:text-white"
                        title="위젯 설정"
                        aria-label="위젯 설정"
                      >
                        <svg
                          width="15"
                          height="15"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          aria-hidden="true"
                        >
                          <path d="M12 15.5A3.5 3.5 0 1 0 12 8a3.5 3.5 0 0 0 0 7.5Z" />
                          <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.87l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06A1.7 1.7 0 0 0 15 19.36a1.7 1.7 0 0 0-1 .16 1.7 1.7 0 0 0-1 1.55V21a2 2 0 0 1-4 0v-.09a1.7 1.7 0 0 0-1-1.55 1.7 1.7 0 0 0-1-.16 1.7 1.7 0 0 0-1.87.34l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.7 1.7 0 0 0 4.64 15a1.7 1.7 0 0 0-.16-1 1.7 1.7 0 0 0-1.55-1H3a2 2 0 0 1 0-4h.09a1.7 1.7 0 0 0 1.55-1 1.7 1.7 0 0 0 .16-1 1.7 1.7 0 0 0-.34-1.87l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.7 1.7 0 0 0 9 4.64a1.7 1.7 0 0 0 1-.16A1.7 1.7 0 0 0 11 2.93V3a2 2 0 0 1 4 0v-.07a1.7 1.7 0 0 0 1 1.55 1.7 1.7 0 0 0 1 .16 1.7 1.7 0 0 0 1.87-.34l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.7 1.7 0 0 0 19.36 9c.07.34.12.68.12 1s-.05.66-.08 1Z" />
                        </svg>
                      </button>

                      <button
                        type="button"
                        onClick={() => removeWidget(widget.i)}
                        className="no-drag p-1 text-slate-500 transition-colors hover:text-rose-500"
                        title="위젯 삭제"
                        aria-label="위젯 삭제"
                      >
                        <svg
                          width="14"
                          height="14"
                          viewBox="0 0 24 24"
                          fill="none"
                          stroke="currentColor"
                          strokeWidth="2.5"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                          aria-hidden="true"
                        >
                          <line x1="18" y1="6" x2="6" y2="18" />
                          <line x1="6" y1="6" x2="18" y2="18" />
                        </svg>
                      </button>

                      <button
                        type="button"
                        onClick={() => togglePinWidget(widget.i)}
                        className={`no-drag p-1 text-xs transition-colors ${
                          widget.pinned ? "text-yellow-400" : "text-slate-500 hover:text-white"
                        }`}
                        title={widget.pinned ? "고정 해제" : "고정"}
                        aria-label={widget.pinned ? "고정 해제" : "고정"}
                      >
                        Pin
                      </button>

                      {isSettingsOpen && (
                        <div className="no-drag absolute right-4 top-12 w-56 rounded-lg border border-slate-700 bg-slate-950/95 p-3 shadow-2xl shadow-black/50 backdrop-blur">
                          <div className="mb-3 flex items-center justify-between gap-3">
                            <span className="text-[10px] font-black uppercase tracking-wider text-slate-400">
                              Chart color
                            </span>
                            <button
                              type="button"
                              onClick={() => setOpenSettingsWidgetId(null)}
                              className="rounded px-1.5 py-0.5 text-xs font-bold text-slate-500 transition-colors hover:bg-slate-800 hover:text-white"
                              aria-label="설정 닫기"
                            >
                              x
                            </button>
                          </div>
                          <div className="grid grid-cols-4 gap-2">
                            {widgetColorOptions.map((option) => {
                              const isSelected = widget.color === option.value;

                              return (
                                <button
                                  type="button"
                                  key={option.value}
                                  onClick={() => {
                                    updateWidgetColor(widget.i, option.value);
                                    setOpenSettingsWidgetId(null);
                                  }}
                                  className={[
                                    "flex h-9 items-center justify-center rounded-md border transition-colors",
                                    isSelected
                                      ? "border-cyan-300 ring-2 ring-cyan-400/40"
                                      : "border-slate-700 hover:border-slate-500",
                                  ].join(" ")}
                                  title={option.label}
                                  aria-label={`${option.label} 그래프 색상`}
                                >
                                  <span
                                    className="h-5 w-5 rounded-full border border-white/10"
                                    style={{ backgroundColor: option.hex }}
                                  />
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      )}
                    </div>
                  )}

                  <h3
                    className={`${canEditDashboard ? "drag-handle cursor-move" : "cursor-default"} mb-4 flex select-none items-center gap-2 text-[10px] font-bold uppercase tracking-widest text-slate-500`}
                  >
                    <div className={`h-3 w-1 rounded-full ${widget.color}`} />
                    {widget.title}
                  </h3>

                  <div className="flex flex-grow flex-col overflow-hidden">
                    <WidgetRenderer
                      widget={widget}
                      equipment={equipment}
                      equipmentById={equipmentById}
                      alerts={alerts}
                    />
                  </div>
                </div>
              );
            })}
          </Responsive>
        )}
      </div>
    </main>
  );
}
