import type { Layout } from "react-grid-layout";

import type {
  AppliedEquipment,
  EquipmentCurrentResponse,
  EquipmentResponse,
  SensorResponse,
  WidgetRequestDto,
  WidgetResponseDto,
} from "../api/client";
import type { UniversalEquipment } from "../types/equipment";
import type {
  DashboardItem,
  DashboardWidgetType,
  EquipmentMaster,
  AlertItem,
  SelectedData,
} from "../types/dashboard";

export const DASHBOARD_AUTOSAVE_INTERVAL_MS = 30000;
export type DashboardBreakpoint = "lg" | "md" | "sm";
export type DashboardLayouts = Partial<Record<DashboardBreakpoint, DashboardItem[]>>;

export const DASHBOARD_BREAKPOINTS: Record<DashboardBreakpoint, number> = {
  lg: 1200,
  md: 996,
  sm: 768,
};

export const DASHBOARD_COLS: Record<DashboardBreakpoint, number> = {
  lg: 12,
  md: 10,
  sm: 6,
};

export const DASHBOARD_BREAKPOINT_KEYS: DashboardBreakpoint[] = ["lg", "md", "sm"];
const WIDGET_APPEARANCE_STORAGE_KEY = "dashboard-widget-appearance";
export const DEFAULT_WIDGET_BACKGROUND = "bg-[#161B26]";
export const WIDGET_COLOR_CLASSES = new Set([
  "bg-indigo-500",
  "bg-cyan-500",
  "bg-emerald-500",
  "bg-amber-500",
  "bg-rose-500",
  "bg-violet-500",
  "bg-sky-500",
  "bg-pink-500",
]);

export type WidgetConfig = {
  type: DashboardWidgetType;
  dataKey: string;
  title: string;
};

export type UseDashboardStateParams = {
  alertsData: AlertItem[];
};

export const EMPTY_EQUIPMENT: UniversalEquipment = {
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

export const getBaseLayout = (layouts: DashboardLayouts) => {
  return layouts.lg ?? [];
};

export const getServerWidgetId = (widget: DashboardItem) => {
  if (typeof widget.serverWidgetId === "number") return widget.serverWidgetId;

  const numericId = Number(widget.i);
  return Number.isInteger(numericId) && numericId > 0 ? numericId : null;
};

const isDashboardWidgetType = (value: string): value is DashboardWidgetType => {
  return [
    "OEE",
    "SENSORS",
    "TREND",
    "ALERTS",
    "GAUGE",
    "DONUT",
    "STATUS",
    "LOG",
    "BAR_V",
    "BAR_H",
  ].includes(value);
};

const parseWidgetConfig = (configJson?: string) => {
  if (!configJson) return {};

  try {
    const parsed = JSON.parse(configJson);
    return parsed && typeof parsed === "object" ? parsed as Record<string, unknown> : {};
  } catch {
    return {};
  }
};

const getWidgetAppearanceStorageKey = (dashboardId: number) =>
  `${WIDGET_APPEARANCE_STORAGE_KEY}:${dashboardId}`;

export const loadWidgetAppearanceOverrides = (dashboardId: number) => {
  try {
    const raw = window.localStorage.getItem(getWidgetAppearanceStorageKey(dashboardId));
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed as Record<string, string> : {};
  } catch {
    return {};
  }
};

export const saveWidgetColorOverride = (
  dashboardId: number | null,
  widgetId: string,
  color: string,
) => {
  if (!dashboardId) return;

  const storageKey = getWidgetAppearanceStorageKey(dashboardId);
  const previous = loadWidgetAppearanceOverrides(dashboardId);
  const next = {
    ...previous,
    [widgetId]: color,
  };

  window.localStorage.setItem(storageKey, JSON.stringify(next));
};

export const buildSensorDataKey = (equipmentId: string, sensorId: string) => `${equipmentId}::${sensorId}`;

export const mapWidgetResponseToDashboardItem = (widget: WidgetResponseDto): DashboardItem => {
  const config = parseWidgetConfig(widget.configJson);
  const type = isDashboardWidgetType(widget.widgetType) ? widget.widgetType : "GAUGE";
  const configDataKey = config.dataKey;
  const serverDataKey = widget.equipmentEntityId && widget.sensorEntityId
    ? buildSensorDataKey(String(widget.equipmentEntityId), String(widget.sensorEntityId))
    : undefined;
  const dataKey =
    serverDataKey
      ? serverDataKey
      : Array.isArray(configDataKey)
      ? configDataKey.map(String)
      : typeof configDataKey === "string"
        ? configDataKey
        : widget.sensorId ?? widget.sensorName ?? widget.widgetType;

  return {
    i: String(widget.id),
    serverWidgetId: widget.id,
    equipmentEntityId: widget.equipmentEntityId,
    equipmentName: widget.equipmentName,
    sensorEntityId: widget.sensorEntityId,
    sensorId: widget.sensorId,
    sensorName: widget.sensorName,
    type,
    title: widget.title,
    dataKey,
    color: typeof config.color === "string" ? config.color : "bg-indigo-500",
    backgroundColor: typeof config.backgroundColor === "string" ? config.backgroundColor : DEFAULT_WIDGET_BACKGROUND,
    pinned: typeof config.pinned === "boolean" ? config.pinned : false,
    static: typeof config.pinned === "boolean" ? config.pinned : false,
    x: widget.posX,
    y: widget.posY,
    w: widget.width,
    h: widget.height,
  };
};

export const toNumberId = (value: string) => {
  const numericId = Number(value);
  return Number.isInteger(numericId) && numericId > 0 ? numericId : undefined;
};

const getChartType = (type: DashboardWidgetType) => {
  switch (type) {
    case "TREND":
      return "line";
    case "BAR_V":
    case "BAR_H":
      return "bar";
    case "DONUT":
      return "donut";
    case "GAUGE":
      return "gauge";
    default:
      return type.toLowerCase();
  }
};

export const buildWidgetCreateRequest = (
  dashboardId: number,
  item: DashboardItem,
  selectedData: SelectedData[],
): WidgetRequestDto => {
  const primaryData = selectedData[0];
  const equipmentEntityId = primaryData ? toNumberId(primaryData.eqId) : undefined;
  const sensorEntityId = primaryData ? toNumberId(primaryData.sensorId) : undefined;

  if (!equipmentEntityId || !sensorEntityId) {
    throw new Error("장비/센서 엔티티 ID를 확인할 수 없습니다.");
  }

  return {
    dashboardId,
    equipmentEntityId,
    widgetType: item.type,
    title: item.title,
    sensorEntityId,
    chartType: getChartType(item.type),
    dataType: primaryData?.dataType,
    posX: item.x,
    posY: Number.isFinite(item.y) ? item.y : 0,
    width: item.w,
    height: item.h,
    configJson: JSON.stringify({
      dataKey: item.dataKey,
      color: item.color,
      backgroundColor: item.backgroundColor ?? DEFAULT_WIDGET_BACKGROUND,
      pinned: item.pinned ?? false,
      selectedData,
    }),
  };
};

export const mergeLayoutMetadata = (
  sourceItems: DashboardItem[],
  layout: Layout,
): DashboardItem[] => {
  return sourceItems.map((widget) => {
    const found = layout.find((item) => item.i === widget.i);

    return found
      ? {
        ...widget,
        x: found.x,
        y: found.y,
        w: found.w,
        h: found.h,
        static: found.static ?? widget.static,
        isDraggable: found.isDraggable ?? widget.isDraggable,
        isResizable: found.isResizable ?? widget.isResizable,
      }
      : widget;
  });
};

export const compactWidgets = (items: DashboardItem[], cols = 12) => {
  const pinned = items.filter((item) => item.pinned);
  const movable = items.filter((item) => !item.pinned);

  const occupied = new Set<string>();

  const markOccupied = (x: number, y: number, w: number, h: number) => {
    for (let dy = 0; dy < h; dy++) {
      for (let dx = 0; dx < w; dx++) {
        occupied.add(`${x + dx},${y + dy}`);
      }
    }
  };

  const canPlace = (x: number, y: number, w: number, h: number) => {
    if (x + w > cols) return false;

    for (let dy = 0; dy < h; dy++) {
      for (let dx = 0; dx < w; dx++) {
        if (occupied.has(`${x + dx},${y + dy}`)) return false;
      }
    }
    return true;
  };

  pinned.forEach((item) => {
    markOccupied(item.x, item.y, item.w, item.h);
  });

  const reorderedMovable = [...movable]
    .sort((a, b) => (a.y - b.y) || (a.x - b.x))
    .map((item) => {
      for (let y = 0; y < 1000; y++) {
        for (let x = 0; x < cols; x++) {
          if (canPlace(x, y, item.w, item.h)) {
            const placed = { ...item, x, y };
            markOccupied(x, y, item.w, item.h);
            return placed;
          }
        }
      }
      return item;
    });

  return [...pinned, ...reorderedMovable];
};

export const mapEquipmentToMaster = (equipment: UniversalEquipment): EquipmentMaster => ({
  id: equipment.id,
  name: equipment.name,
  type: equipment.type,
  sensors: equipment.sensors.map((sensor, index) => ({
    id: sensor.sensorId ?? `${equipment.id}-sensor-${index}`,
    label: sensor.sensorId ?? sensor.label,
    unit: sensor.unit,
    dataType: sensor.dataType,
  })),
});

export const mapEquipmentResponseToMaster = (equipment: EquipmentResponse): EquipmentMaster => ({
  id: String(equipment.equipmentId),
  name: equipment.equipmentName,
  type: equipment.field ?? "UNKNOWN",
  sensors: [],
  sensorsLoaded: false,
});

export const mapSensorResponseToMeta = (sensor: SensorResponse) => ({
  id: String(sensor.sensorId),
  label: sensor.sensorName,
  unit: "",
});

export const mapAppliedEquipmentToMaster = (item: AppliedEquipment): EquipmentMaster => ({
  ...mapEquipmentResponseToMaster(item.equipment),
  sensors: item.sensors.map(mapSensorResponseToMeta),
  sensorsLoaded: true,
});

const normalizeSensorValue = (value: unknown, dataType?: string): number | string => {
  if (dataType === "STRING") return String(value ?? "");
  if (typeof value === "number") return value;
  if (typeof value === "boolean") return value ? 1 : 0;
  if (value && typeof value === "object") {
    const payload = value as Record<string, unknown>;

    return normalizeSensorValue(
      payload.value ?? payload.currentValue ?? payload.numericValue ?? payload.data,
      dataType,
    );
  }

  const numericText = String(value ?? "").replace(/,/g, "").match(/-?\d+(\.\d+)?/)?.[0] ?? "";
  const numericValue = Number(numericText);
  return Number.isFinite(numericValue) ? numericValue : String(value ?? "");
};

const getEquipmentStatus = (status?: string): UniversalEquipment["status"] => {
  if (status === "ERROR") return "DOWN";
  if (status === "IDLE") return "IDLE";
  if (status === "MAINTENANCE") return "MAINTENANCE";
  return "RUNNING";
};

const getSensorMergeKey = (sensor: { sensorId?: string; label: string }) => sensor.sensorId ?? sensor.label;

const mergeSensorData = (
  previousSensors: UniversalEquipment["sensors"],
  nextSensors: UniversalEquipment["sensors"],
) => {
  const sensorByKey = new Map(previousSensors.map((sensor) => [getSensorMergeKey(sensor), sensor]));

  nextSensors.forEach((sensor) => {
    sensorByKey.set(getSensorMergeKey(sensor), sensor);
  });

  return Array.from(sensorByKey.values());
};

export const mapCurrentResponseToEquipment = (
  response: EquipmentCurrentResponse,
  fallback: UniversalEquipment,
): UniversalEquipment => {
  const sensors = (response.current?.sensors ?? []).map((sensor, index) => {
    const sensorId = sensor.sensorId ?? sensor.sensorName ?? sensor.name ?? `sensor-${index}`;
    const rawValue = sensor.value ?? sensor.currentValue ?? sensor.numericValue;

    return {
      sensorId,
      label: sensor.sensorName ?? sensor.name ?? sensorId,
      value: normalizeSensorValue(rawValue, sensor.dataType),
      unit: sensor.unit ?? "",
      dataType: sensor.dataType,
      status: response.current?.status === "ERROR" ? "CRITICAL" as const : "NORMAL" as const,
    };
  });

  if (sensors.length === 0) {
    console.warn("[Equipment Current] Current response has no sensors", response);
  } else {
    console.debug("[Equipment Current] Sensor payload received", {
      equipmentId: response.equipmentId,
      sensorCount: sensors.length,
      sensorIds: sensors.map((sensor) => sensor.sensorId ?? sensor.label),
    });
  }

  return {
    ...fallback,
    id: String(response.equipmentId),
    name: response.equipmentName,
    type: response.field ?? fallback.type,
    status: getEquipmentStatus(response.current?.status),
    lastUpdate: response.current?.timestamp ?? new Date().toISOString(),
    sensors:
      fallback.id === String(response.equipmentId) || fallback.name === response.equipmentName
        ? mergeSensorData(fallback.sensors, sensors)
        : sensors,
  };
};

export const mapCurrentResponseToMaster = (response: EquipmentCurrentResponse): EquipmentMaster => {
  const currentEquipment = mapCurrentResponseToEquipment(response, EMPTY_EQUIPMENT);

  return {
    ...mapEquipmentToMaster(currentEquipment),
    sensorsLoaded: currentEquipment.sensors.length > 0,
  };
};
