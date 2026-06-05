import { useCallback, useEffect, useMemo, useState } from "react";
import { Responsive, useContainerWidth } from "react-grid-layout";
import { useSearchParams } from "react-router-dom";

import {
  getPublicDashboard,
  getPublicDashboardWidgets,
  type EquipmentCurrentResponse,
  type PublicDashboardResponse,
  type WidgetResponseDto,
} from "../api/client";
import { WidgetRenderer } from "../components/WidgetRenderer";
import {
  DASHBOARD_BREAKPOINTS,
  DASHBOARD_COLS,
  mapCurrentResponseToEquipment,
  mapWidgetResponseToDashboardItem,
  type DashboardBreakpoint,
  type DashboardLayouts,
} from "../hooks/useDashboardState";
import type { DashboardItem } from "../types/dashboard";
import type { UniversalEquipment } from "../types/equipment";

import "react-grid-layout/css/styles.css";
import "react-resizable/css/styles.css";

const EMPTY_EQUIPMENT: UniversalEquipment = {
  id: "",
  name: "No equipment",
  type: "",
  status: "IDLE",
  lastUpdate: "",
  metrics: {
    oee: 0,
    availability: 0,
    performance: 0,
    quality: 0,
  },
  sensors: [],
};

function getPublicWidgets(data: PublicDashboardResponse) {
  return data.widgets ?? data.dashboardWidgets ?? [];
}

function getPublicEquipment(data: PublicDashboardResponse): EquipmentCurrentResponse[] {
  return data.equipment ?? data.equipments ?? data.currentEquipment ?? data.equipmentCurrent ?? data.currentData ?? [];
}

function getPublicDashboardName(data: PublicDashboardResponse) {
  return data.dashboard?.dashboardName ?? data.dashboardName ?? "Shared dashboard";
}

function getPublicDashboardId(data: PublicDashboardResponse) {
  return data.dashboard?.dashboardId ?? data.dashboardId;
}

export default function PublicDashboardPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const { containerRef, width, mounted } = useContainerWidth();
  const [layouts, setLayouts] = useState<DashboardLayouts>({ lg: [] });
  const [equipment, setEquipment] = useState<UniversalEquipment>(EMPTY_EQUIPMENT);
  const [equipmentById, setEquipmentById] = useState<Record<string, UniversalEquipment>>({});
  const [dashboardName, setDashboardName] = useState("Shared dashboard");
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState("");
  const [emptyMessage, setEmptyMessage] = useState("");
  const baseLayout = useMemo(() => layouts.lg ?? [], [layouts]);

  const loadPublicDashboard = useCallback(async () => {
    if (!token) {
      setErrorMessage("공유 토큰이 없습니다.");
      setIsLoading(false);
      return;
    }

    try {
      const response = await getPublicDashboard(token);

      if (!response.success || !response.data) {
        throw new Error(response.message ?? "만료되었거나 존재하지 않는 공유 링크입니다.");
      }

      const data = response.data;
      const dashboardId = getPublicDashboardId(data);
      let rawWidgets: WidgetResponseDto[] = getPublicWidgets(data);

      if (rawWidgets.length === 0 && dashboardId) {
        try {
          const widgetsResponse = await getPublicDashboardWidgets(dashboardId);
          rawWidgets = widgetsResponse.data ?? [];
        } catch (widgetError) {
          console.warn("[Public Dashboard] Public widget fallback failed", widgetError);
        }
      }

      const widgets = rawWidgets.map(mapWidgetResponseToDashboardItem);
      const currentEquipment = getPublicEquipment(data);
      const nextEquipmentById: Record<string, UniversalEquipment> = {};
      let primaryEquipment = EMPTY_EQUIPMENT;

      currentEquipment.forEach((item, index) => {
        const previous = nextEquipmentById[String(item.equipmentId)] ?? EMPTY_EQUIPMENT;
        const mapped = mapCurrentResponseToEquipment(item, previous);
        nextEquipmentById[mapped.id] = mapped;

        if (index === 0) {
          primaryEquipment = mapped;
        }
      });

      setDashboardName(getPublicDashboardName(data));
      setLayouts({ lg: widgets });
      setEquipment(primaryEquipment);
      setEquipmentById(nextEquipmentById);
      setErrorMessage("");
      setEmptyMessage(
        widgets.length === 0
          ? "공유 API에서 위젯 목록을 받지 못했습니다. 백엔드 public 응답에 widgets 배열이 포함되어야 대시보드를 렌더링할 수 있습니다."
          : "",
      );
    } catch (error) {
      console.error("[Public Dashboard] Failed to load shared dashboard", error);
      setErrorMessage(error instanceof Error ? error.message : "만료되었거나 존재하지 않는 공유 링크입니다.");
    } finally {
      setIsLoading(false);
    }
  }, [token]);

  useEffect(() => {
    void loadPublicDashboard();
  }, [loadPublicDashboard]);

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0B0F1A] text-sm font-semibold text-slate-300">
        Loading shared dashboard...
      </div>
    );
  }

  if (errorMessage) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[#0B0F1A] px-6 text-center text-slate-300">
        <div>
          <h1 className="text-2xl font-black text-white">Shared dashboard unavailable</h1>
          <p className="mt-3 text-sm text-slate-500">
            {errorMessage || "만료되었거나 존재하지 않는 공유 링크입니다."}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0B0F1A] text-slate-200">
      <header className="border-b border-slate-800/70 bg-[#0D1117]/90 px-4 py-5 backdrop-blur md:px-6">
        <div className="mx-auto flex max-w-[1800px] flex-col gap-1">
          <p className="text-[11px] font-black uppercase tracking-[0.18em] text-cyan-400">
            Read-only shared dashboard
          </p>
          <h1 className="text-2xl font-black tracking-tight text-white">{dashboardName}</h1>
        </div>
      </header>

      <main className="mx-auto max-w-[1800px] p-4">
        {emptyMessage ? (
          <div className="rounded-lg border border-slate-800 bg-[#111827] p-6 text-sm text-slate-400">
            <p className="font-bold text-white">No widgets to display</p>
            <p className="mt-2 leading-6">{emptyMessage}</p>
          </div>
        ) : null}

        <div ref={containerRef}>
          {mounted && (
            <Responsive<DashboardBreakpoint>
              className="layout"
              layouts={layouts}
              breakpoints={DASHBOARD_BREAKPOINTS}
              cols={DASHBOARD_COLS}
              rowHeight={140}
              width={width}
              margin={[20, 20]}
              containerPadding={[0, 0]}
              dragConfig={{ enabled: false }}
              resizeConfig={{ enabled: false }}
            >
              {baseLayout.map((widget: DashboardItem) => (
                <div
                  key={widget.i}
                  className="relative flex flex-col overflow-hidden rounded-3xl border border-slate-800 bg-[#161B26] p-6 shadow-xl"
                >
                  <h3 className="mb-4 flex cursor-default select-none items-center gap-2 text-[10px] font-bold uppercase tracking-widest text-slate-500">
                    <div className={`h-3 w-1 rounded-full ${widget.color}`} />
                    {widget.title}
                  </h3>

                  <div className="flex flex-grow flex-col overflow-hidden">
                    <WidgetRenderer
                      widget={widget}
                      equipment={equipment}
                      equipmentById={equipmentById}
                      alerts={[]}
                    />
                  </div>
                </div>
              ))}
            </Responsive>
          )}
        </div>
      </main>
    </div>
  );
}
