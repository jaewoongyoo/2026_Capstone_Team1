import { useCallback, useEffect, useState, useRef } from "react";
import {
  applyEquipmentDiscovery,
  createDashboardWidget,
  deleteDashboardWidget,
  getMyDashboards,
  getMyWidgets,
  updateWidgetLayouts,
} from "../api/client";
import type {
  DashboardItem,
  SelectedData,
} from "../types/dashboard";
import type { Layout, ResponsiveLayouts } from "react-grid-layout";
import { useDashboardMeta } from "./useDashboardMeta";
import { useEquipmentRegistry } from "./useEquipmentRegistry";
import { useWidgetBuilder } from "./useWidgetBuilder";
import {
  DASHBOARD_AUTOSAVE_INTERVAL_MS,
  DASHBOARD_BREAKPOINT_KEYS,
  DASHBOARD_COLS,
  DEFAULT_WIDGET_BACKGROUND,
  WIDGET_COLOR_CLASSES,
  buildSensorDataKey,
  buildWidgetCreateRequest,
  compactWidgets,
  getBaseLayout,
  getServerWidgetId,
  loadWidgetAppearanceOverrides,
  mapAppliedEquipmentToMaster,
  mapWidgetResponseToDashboardItem,
  mergeLayoutMetadata,
  saveWidgetColorOverride,
  toNumberId,
  type DashboardBreakpoint,
  type DashboardLayouts,
  type UseDashboardStateParams,
} from "./dashboardStateUtils";

export {
  DASHBOARD_BREAKPOINTS,
  DASHBOARD_COLS,
  mapCurrentResponseToEquipment,
  mapWidgetResponseToDashboardItem,
  type DashboardBreakpoint,
  type DashboardLayouts,
} from "./dashboardStateUtils";

export function useDashboardState({
  alertsData,
}: UseDashboardStateParams) {

  const [alerts] = useState(alertsData);
  const [time, setTime] = useState(new Date());
  const [isLoadingDashboardWidgets, setIsLoadingDashboardWidgets] = useState(false);
  const {
    dashboardId,
    dashboardName,
    isDashboardPublic,
    dashboardShareToken,
    setDashboardFromResponse,
    enableShareLink,
    disableShareLink,
    updateDashboardTitle,
  } = useDashboardMeta();

  const [responsiveLayouts, setResponsiveLayouts] = useState<DashboardLayouts>({ lg: [] });
  const [currentBreakpoint, setCurrentBreakpoint] = useState<DashboardBreakpoint>("lg");
  const layouts = getBaseLayout(responsiveLayouts);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [autoArrange, setAutoArrange] = useState(true);
  const [isDashboardDirty, setIsDashboardDirty] = useState(false);
  const [isSavingDashboard, setIsSavingDashboard] = useState(false);
  const [lastDashboardSavedAt, setLastDashboardSavedAt] = useState<Date | null>(null);
  const [dashboardSaveError, setDashboardSaveError] = useState<string | null>(null);
  const [tempSelection, setTempSelection] = useState({ eqId: "", sensorId: "" });
  const [searchTerm, setSearchTerm] = useState("");
  const skipNextLayoutChange = useRef(false);
  const pendingDeletedWidgetIds = useRef<Set<number>>(new Set());

  const updateLayouts = useCallback((
    updater: (
      items: DashboardItem[],
      breakpoint: DashboardBreakpoint,
    ) => DashboardItem[],
  ) => {
    setIsDashboardDirty(true);
    setResponsiveLayouts((prev) => {
      const base = getBaseLayout(prev);
      const next: DashboardLayouts = {};

      DASHBOARD_BREAKPOINT_KEYS.forEach((breakpoint) => {
        next[breakpoint] = updater(prev[breakpoint] ?? base, breakpoint);
      });

      return next;
    });
  }, []);

  const {
    allEquipments,
    equipment,
    equipmentById,
    isEqModalOpen,
    isNetworkScanning,
    loadingSensorEquipmentId,
    setAllEquipments,
    setEquipment,
    setIsEqModalOpen,
    loadInitialEquipmentCurrent,
    loadEquipmentCurrent,
    loadEquipmentSensors,
    selectEquipmentForDiscovery,
    startNetworkScan,
    closeEquipmentModal,
    applyEquipmentRegistration,
    removeEquipment,
  } = useEquipmentRegistry({
    dashboardId,
    tempSelection,
    setTempSelection,
    updateLayouts,
  });

  const {
    newWidgetConfig,
    builderStep,
    selectedDataCart,
    setNewWidgetConfig,
    setBuilderStep,
    setSelectedDataCart,
    resetWidgetBuilder,
    addSelectedSensorToCart,
    removeSelectedSensorFromCart,
    goToBuilderStep2,
  } = useWidgetBuilder({
    allEquipments,
    closeWidgetModal: () => setIsModalOpen(false),
    tempSelection,
    setTempSelection,
    setSearchTerm,
  });


  const loadDashboardWidgets = useCallback(async () => {
    setIsLoadingDashboardWidgets(true);
    setDashboardSaveError(null);

    try {
      const dashboardsResponse = await getMyDashboards();
      const dashboard = dashboardsResponse.data?.[0];

      if (!dashboard) {
        return;
      }

      setDashboardFromResponse(dashboard);

      const widgetsResponse = await getMyWidgets();
      const widgets = widgetsResponse.data ?? [];

      const appearanceOverrides = loadWidgetAppearanceOverrides(dashboard.dashboardId);
      const serverLayout = widgets.map((widget) => {
        const item = mapWidgetResponseToDashboardItem(widget);
        return {
          ...item,
        color: WIDGET_COLOR_CLASSES.has(appearanceOverrides[item.i]) ? appearanceOverrides[item.i] : item.color,
        };
      });
      setResponsiveLayouts({ lg: serverLayout });
      setIsDashboardDirty(false);
      pendingDeletedWidgetIds.current.clear();
    } catch (error) {
      console.error("[Dashboard Load] Failed to load dashboard widgets", error);
      setDashboardSaveError(error instanceof Error ? error.message : "대시보드 위젯을 불러오지 못했습니다.");
    } finally {
      setIsLoadingDashboardWidgets(false);
    }
  }, [setDashboardFromResponse]);

  // 헤더 시계를 최신 상태로 유지
  useEffect(() => {
    const timer = window.setInterval(() => setTime(new Date()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    void loadDashboardWidgets();
  }, [loadDashboardWidgets]);

  useEffect(() => {
    if (!dashboardId) {
      return undefined;
    }

    void loadInitialEquipmentCurrent();

    const timer = window.setInterval(() => {
      void loadInitialEquipmentCurrent();
    }, 5000);

    return () => window.clearInterval(timer);
  }, [dashboardId, loadInitialEquipmentCurrent]);

  const saveDashboardState = useCallback(async () => {
    setIsSavingDashboard(true);
    setDashboardSaveError(null);

    try {
      const deletedWidgetIds = Array.from(pendingDeletedWidgetIds.current);

      for (const widgetId of deletedWidgetIds) {
        await deleteDashboardWidget(widgetId);
      }

      const layoutsForSave = getBaseLayout(responsiveLayouts);

      const layoutItems = layoutsForSave
        .map((widget) => {
          const widgetId = getServerWidgetId(widget);

          if (!widgetId) return null;

          return {
            widgetId,
            posX: widget.x,
            posY: Number.isFinite(widget.y) ? widget.y : 0,
            width: widget.w,
            height: widget.h,
          };
        })
        .filter((item): item is NonNullable<typeof item> => item !== null);

      console.info("[Dashboard Save] Saving widget layouts", {
        layoutCount: layoutItems.length,
        skippedLocalWidgetCount: layoutsForSave.length - layoutItems.length,
        deletedWidgetCount: deletedWidgetIds.length,
      });

      if (layoutItems.length === 0) {
        deletedWidgetIds.forEach((widgetId) => pendingDeletedWidgetIds.current.delete(widgetId));
        setIsDashboardDirty(false);
        setLastDashboardSavedAt(new Date());
        return true;
      }

      await updateWidgetLayouts({ layouts: layoutItems });
      deletedWidgetIds.forEach((widgetId) => pendingDeletedWidgetIds.current.delete(widgetId));
      setIsDashboardDirty(false);
      setLastDashboardSavedAt(new Date());
      return true;
    } catch (error) {
      console.error("[Dashboard Save] Failed to save widget layout", error);
      setDashboardSaveError(error instanceof Error ? error.message : "대시보드 저장에 실패했습니다.");
      return false;
    } finally {
      setIsSavingDashboard(false);
    }
  }, [responsiveLayouts]);

  useEffect(() => {
    if (!isDashboardDirty || isSavingDashboard) return;

    const timer = window.setTimeout(() => {
      void saveDashboardState();
    }, DASHBOARD_AUTOSAVE_INTERVAL_MS);

    return () => window.clearTimeout(timer);
  }, [isDashboardDirty, isSavingDashboard, saveDashboardState]);

  const setLayouts = (
    value: DashboardItem[] | ((previous: DashboardItem[]) => DashboardItem[]),
  ) => {
    setIsDashboardDirty(true);
    setResponsiveLayouts((prev) => {
      const previous = getBaseLayout(prev);
      const nextLg = typeof value === "function" ? value(previous) : value;
      return { ...prev, lg: nextLg };
    });
  };

  const removeWidget = (widgetId: string) => {
    const widget = layouts.find((item) => item.i === widgetId);
    const serverWidgetId = widget ? getServerWidgetId(widget) : null;

    if (serverWidgetId) {
      pendingDeletedWidgetIds.current.add(serverWidgetId);
    }

    updateLayouts((items, breakpoint) => {
      const next = items.filter((item) => item.i !== widgetId);
      return autoArrange ? compactWidgets(next, DASHBOARD_COLS[breakpoint]) : next;
    });
  };
  
  const applyLayout = (
    currentLayout: Layout,
    shouldCompact = false,
    breakpoint: DashboardBreakpoint = currentBreakpoint,
  ) => {
    setIsDashboardDirty(true);
    setResponsiveLayouts((prev) => {
      const base = getBaseLayout(prev);
      const source = prev[breakpoint] ?? base;
      const updated = mergeLayoutMetadata(source, currentLayout);

      return {
        ...prev,
        [breakpoint]: shouldCompact
          ? compactWidgets(updated, DASHBOARD_COLS[breakpoint])
          : updated,
      };
    });
  };

  const handleLayoutChange = (
    currentLayout: Layout,
    allLayouts?: ResponsiveLayouts<DashboardBreakpoint>,
  ) => {
    if (skipNextLayoutChange.current) {
      skipNextLayoutChange.current = false;
      return;
    }

    if (allLayouts) {
      setIsDashboardDirty(true);
      setResponsiveLayouts((prev) => {
        const base = getBaseLayout(prev);
        const next: DashboardLayouts = { ...prev };

        DASHBOARD_BREAKPOINT_KEYS.forEach((breakpoint) => {
          const layout = allLayouts[breakpoint];
          if (!layout) return;
          next[breakpoint] = mergeLayoutMetadata(prev[breakpoint] ?? base, layout);
        });

        return next;
      });
      return;
    }

    applyLayout(currentLayout, false);
  };

  const arrangeWidgets = () => {
    updateLayouts((items, breakpoint) => compactWidgets(items, DASHBOARD_COLS[breakpoint]));
  };

  const ensureSelectedDataEntityIds = async (items: SelectedData[]) => {
    const hasEntityIds = items.every((item) => toNumberId(item.eqId) && toNumberId(item.sensorId));

    if (hasEntityIds) {
      return items;
    }

    if (!dashboardId) {
      throw new Error("대시보드를 불러온 뒤 위젯을 추가할 수 있습니다.");
    }

    const assets = Array.from(
      items.reduce((map, item) => {
        const equipment = allEquipments.find((candidate) => candidate.id === item.eqId);
        const current = map.get(item.eqName) ?? {
          equipmentName: item.eqName,
          field: equipment?.type,
          tags: [] as { sensorName: string }[],
        };
        const sensorName = item.sensorLabel ?? item.sensorId;

        if (!current.tags.some((tag) => tag.sensorName === sensorName)) {
          current.tags.push({ sensorName });
        }

        map.set(item.eqName, current);
        return map;
      }, new Map<string, { equipmentName: string; field?: string; tags: { sensorName: string }[] }>()),
      ([, asset]) => asset,
    );

    const response = await applyEquipmentDiscovery({
      dashboardId,
      assets,
    });
    const appliedEquipment = response.data?.equipment ?? [];
    const nextEquipments = appliedEquipment.map(mapAppliedEquipmentToMaster);

    if (nextEquipments.length > 0) {
      setAllEquipments((prev) => {
        const byId = new Map(prev.map((equipment) => [equipment.id, equipment]));
        nextEquipments.forEach((equipment) => byId.set(equipment.id, equipment));
        return Array.from(byId.values());
      });
    }

    return items.map((item) => {
      const applied = appliedEquipment.find(
        (candidate) => candidate.equipment.equipmentName === item.eqName,
      );
      const sensor = applied?.sensors.find(
        (candidate) => candidate.sensorName === (item.sensorLabel ?? item.sensorId),
      );

      if (!applied || !sensor) {
        throw new Error(`"${item.eqName} - ${item.sensorLabel ?? item.sensorId}" 엔티티 ID를 확인할 수 없습니다.`);
      }

      const eqId = String(applied.equipment.equipmentId);
      const sensorId = String(sensor.sensorId);

      return {
        ...item,
        eqId,
        sensorId,
        sensorKey: buildSensorDataKey(eqId, sensorId),
      };
    });
  };

  const addWidgetToDashboard = async () => {
    if (selectedDataCart.length === 0) return;
    if (!dashboardId) {
      setDashboardSaveError("대시보드를 불러온 뒤 위젯을 추가할 수 있습니다.");
      return;
    }

    const newId = `widget-${Date.now()}`;
    let selectedDataForCreate = selectedDataCart;

    try {
      selectedDataForCreate = await ensureSelectedDataEntityIds(selectedDataCart);
      setSelectedDataCart(selectedDataForCreate);
    } catch (error) {
      console.error("[Dashboard Widget] Failed to resolve entity ids", error);
      setDashboardSaveError(error instanceof Error ? error.message : "장비/센서 엔티티 ID를 확인할 수 없습니다.");
      return;
    }

    const keysToSave =
      selectedDataForCreate.length > 1
        ? selectedDataForCreate.map((item) => item.sensorKey ?? buildSensorDataKey(item.eqId, item.sensorId))
        : selectedDataForCreate[0].sensorKey ?? buildSensorDataKey(selectedDataForCreate[0].eqId, selectedDataForCreate[0].sensorId);

    const newItem: DashboardItem = {
      i: newId,
      type: newWidgetConfig.type,
      dataKey: keysToSave,
      title:
        selectedDataForCreate.length > 1
          ? `다중 비교 (${selectedDataForCreate.length}개)`
          : `${selectedDataForCreate[0].eqName} - ${selectedDataForCreate[0].sensorLabel ?? selectedDataForCreate[0].sensorId}`,
      color: "bg-indigo-500",
      backgroundColor: DEFAULT_WIDGET_BACKGROUND,
      x: (layouts.length * 4) % 12,
      y: Infinity,
      w: newWidgetConfig.type === "TREND" ? 8 : 4,
      h: 2,
    };

    setIsSavingDashboard(true);
    setDashboardSaveError(null);

    try {
      const createRequest = buildWidgetCreateRequest(dashboardId, newItem, selectedDataForCreate);
      console.info("[Dashboard Widget] Creating widget", createRequest);

      const response = await createDashboardWidget(
        dashboardId,
        createRequest,
      );

      if (!response.data) {
        throw new Error("위젯 생성 응답이 비어 있습니다.");
      }

      const createdItem = mapWidgetResponseToDashboardItem(response.data);
      updateLayouts((items) => [...items, createdItem]);
      setLastDashboardSavedAt(new Date());
      setIsDashboardDirty(false);
      resetWidgetBuilder();
    } catch (error) {
      console.error("[Dashboard Widget] Failed to create widget", error);
      setDashboardSaveError(error instanceof Error ? error.message : "위젯 생성에 실패했습니다.");
    } finally {
      setIsSavingDashboard(false);
    }
  };


  // 위젯 고정/고정 해제 함수
  const togglePinWidget = (widgetId: string) => {
    skipNextLayoutChange.current = true; // 다음 레이아웃 변경 이벤트를 무시하도록 설정

    updateLayouts((items) =>
      items.map((widget) =>
        widget.i === widgetId
          ? {
            ...widget,
            pinned: !widget.pinned,
            static: !widget.pinned,
          } : widget
      )
    );
  };

  const updateWidgetColor = (widgetId: string, color: string) => {
    saveWidgetColorOverride(dashboardId, widgetId, color);
    updateLayouts((items) =>
      items.map((widget) =>
        widget.i === widgetId
          ? {
            ...widget,
            color,
          }
          : widget,
      ),
    );
  };

  return {
    allEquipments,
    alerts,
    autoArrange,
    time,
    dashboardId,
    dashboardName,
    isDashboardPublic,
    dashboardShareToken,
    equipment,
    equipmentById,
    layouts,
    responsiveLayouts,
    currentBreakpoint,
    isModalOpen,
    isEqModalOpen,
    isNetworkScanning,
    isDashboardDirty,
    isSavingDashboard,
    isLoadingDashboardWidgets,
    loadingSensorEquipmentId,
    lastDashboardSavedAt,
    dashboardSaveError,
    newWidgetConfig,
    builderStep,
    selectedDataCart,
    tempSelection,
    searchTerm,

    setEquipment,
    setAutoArrange,
    setIsModalOpen,
    setIsEqModalOpen,
    setNewWidgetConfig,
    setBuilderStep,
    setTempSelection,
    setSearchTerm,
    setLayouts,
    setResponsiveLayouts,
    setCurrentBreakpoint,

    handleLayoutChange,
    saveDashboardState,
    enableShareLink,
    disableShareLink,
    updateDashboardTitle,
    removeWidget,
    resetWidgetBuilder,
    addSelectedSensorToCart,
    removeSelectedSensorFromCart,
    goToBuilderStep2,
    addWidgetToDashboard,
    loadEquipmentCurrent,
    loadEquipmentSensors,
    selectEquipmentForDiscovery,
    startNetworkScan,
    closeEquipmentModal,
    applyEquipmentRegistration,
    removeEquipment,
    togglePinWidget,
    updateWidgetColor,
    arrangeWidgets,
    compactWidgets,
    applyLayout,
  };
}

export type DashboardState = ReturnType<typeof useDashboardState>;
