import { createServer, type IncomingMessage, type ServerResponse } from "node:http";
import { DashboardApiClient } from "./client/dashboardApi.js";
import { loadConfig } from "./config/env.js";
import type {
  Dashboard,
  EquipmentCurrent,
  SensorCurrent,
  Widget,
  WidgetLayoutUpdate,
  WidgetRequest
} from "./types/api.js";

type ChatRequest = {
  message?: string;
};

type ChatResponse = {
  reply: string;
  action: string;
  data?: unknown;
  requiresConfirmation?: boolean;
};

type WidgetVisualization = {
  label: string;
  widgetType: string;
  chartType: string;
  width: number;
  height: number;
  aliases: string[];
};

type WidgetPlan = {
  equipment: EquipmentCurrent;
  sensor: SensorCurrent;
  visualization: WidgetVisualization;
  request: WidgetRequest;
  existingWidget?: Widget;
};

type LayoutPlan = {
  widget: Widget;
  layout: WidgetLayoutUpdate["layouts"][number];
  group: "숫자" | "상태" | "로그";
};

type PendingAction = {
  createdAt: number;
  execute: (api: DashboardApiClient) => Promise<ChatResponse>;
};

const PORT = Number(process.env.MCP_HTTP_PORT ?? 3333);
const CORS_ORIGIN = process.env.MCP_HTTP_CORS_ORIGIN ?? "*";
const SUPPORTED_WIDGET_VISUALIZATIONS: WidgetVisualization[] = [
  { label: "게이지", widgetType: "GAUGE", chartType: "gauge", width: 3, height: 3, aliases: ["게이지", "gauge"] },
  { label: "선 그래프", widgetType: "TREND", chartType: "line", width: 6, height: 3, aliases: ["선그래프", "선 그래프", "라인", "라인그래프", "line", "trend", "추이"] },
  { label: "막대그래프", widgetType: "BAR_V", chartType: "bar", width: 4, height: 3, aliases: ["막대", "막대그래프", "세로막대", "바그래프", "bar", "bar_v"] },
  { label: "가로 막대그래프", widgetType: "BAR_H", chartType: "bar-horizontal", width: 4, height: 3, aliases: ["가로막대", "가로 막대", "horizontal bar", "bar_h"] },
  { label: "도넛 그래프", widgetType: "DONUT", chartType: "donut", width: 3, height: 3, aliases: ["도넛", "도넛그래프", "도넛 그래프", "donut"] },
  { label: "상태 위젯", widgetType: "STATUS", chartType: "status", width: 3, height: 3, aliases: ["상태", "상태위젯", "상태 위젯", "status"] },
  { label: "로그 위젯", widgetType: "LOG", chartType: "log", width: 6, height: 3, aliases: ["로그", "로그위젯", "로그 위젯", "log"] },
  { label: "알림 위젯", widgetType: "ALERTS", chartType: "alerts", width: 6, height: 3, aliases: ["알림", "경고", "알람", "alerts", "alert"] },
  { label: "센서 목록 위젯", widgetType: "SENSORS", chartType: "sensors", width: 6, height: 3, aliases: ["센서목록", "센서 목록", "sensors"] },
  { label: "OEE 위젯", widgetType: "OEE", chartType: "oee", width: 6, height: 3, aliases: ["oee", "종합효율", "설비종합효율"] }
];
const SENSOR_KEYWORDS = [
  { aliases: ["압력", "pressure"], sensorAliases: ["pressure"] },
  { aliases: ["온도", "temperature", "temp"], sensorAliases: ["temp", "temperature"] },
  { aliases: ["유량", "flow"], sensorAliases: ["flow"] },
  { aliases: ["파워", "전력", "power"], sensorAliases: ["power"] },
  { aliases: ["상태", "state"], sensorAliases: ["state"] },
  { aliases: ["로그", "log", "message"], sensorAliases: ["log", "message"] }
];
const pendingActions = new Map<string, PendingAction>();
const PENDING_ACTION_TTL_MS = 5 * 60 * 1000;

function sendJson(response: ServerResponse, statusCode: number, payload: unknown): void {
  response.writeHead(statusCode, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": CORS_ORIGIN,
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS"
  });
  response.end(JSON.stringify(payload));
}

function getBearerToken(request: IncomingMessage): string | undefined {
  const authorization = request.headers.authorization;
  if (!authorization?.toLowerCase().startsWith("bearer ")) return undefined;
  return authorization.slice("bearer ".length).trim();
}

function readBody(request: IncomingMessage): Promise<unknown> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];

    request.on("data", (chunk) => {
      chunks.push(Buffer.from(chunk));
      if (Buffer.concat(chunks).byteLength > 64 * 1024) {
        reject(new Error("Request body is too large."));
        request.destroy();
      }
    });
    request.on("end", () => {
      const text = Buffer.concat(chunks).toString("utf8").trim();
      if (!text) {
        resolve({});
        return;
      }

      try {
        resolve(JSON.parse(text));
      } catch {
        reject(new Error("Request body must be JSON."));
      }
    });
    request.on("error", reject);
  });
}

function normalize(text: string): string {
  return text.toLowerCase().replace(/[^a-z0-9가-힣]+/gi, "");
}

function normalizeLoose(text: string): string {
  return text.toLowerCase().replace(/[^a-z0-9가-힣]+/gi, "");
}

function includesAny(text: string, keywords: string[]): boolean {
  return keywords.some((keyword) => text.includes(normalize(keyword)));
}

function getSessionKey(request: IncomingMessage, token?: string): string {
  if (token) return `token:${token.slice(-32)}`;
  return `ip:${request.socket.remoteAddress ?? "anonymous"}`;
}

function isApplyConfirmation(normalized: string): boolean {
  return includesAny(normalized, ["적용", "실행", "진행", "확인", "좋아", "승인"]);
}

function isCancelConfirmation(normalized: string): boolean {
  return includesAny(normalized, ["취소", "하지마", "멈춰", "아니", "중단"]);
}

function cleanupExpiredPendingActions(): void {
  const now = Date.now();
  for (const [key, action] of pendingActions.entries()) {
    if (now - action.createdAt > PENDING_ACTION_TTL_MS) {
      pendingActions.delete(key);
    }
  }
}

function withApplyPrompt(reply: string): string {
  return [
    reply,
    "",
    "이 작업을 적용할까요?"
  ].join("\n");
}

function storePendingAction(sessionKey: string, action: PendingAction): void {
  cleanupExpiredPendingActions();
  pendingActions.set(sessionKey, action);
}

function extractDashboardId(text: string): number | undefined {
  const match =
    text.match(/dashboard\s*id\s*=?\s*(\d+)/i) ??
    text.match(/dashboardId\s*=?\s*(\d+)/i) ??
    text.match(/대시보드\s*(?:id|아이디)?\s*(\d+)/i);

  return match ? Number(match[1]) : undefined;
}

function findVisualization(text: string): WidgetVisualization | null {
  const normalized = text.toLowerCase();
  const loose = normalizeLoose(normalized);
  const candidates = SUPPORTED_WIDGET_VISUALIZATIONS.flatMap((visualization) =>
    visualization.aliases.map((alias) => ({ visualization, alias }))
  ).sort((a, b) => normalizeLoose(b.alias).length - normalizeLoose(a.alias).length);
  const match = candidates.find(({ alias }) => {
    const normalizedAlias = alias.toLowerCase();
    return normalized.includes(normalizedAlias) || loose.includes(normalizeLoose(normalizedAlias));
  });

  return match?.visualization ?? null;
}

function isWidgetCreateRequest(text: string): boolean {
  const normalized = normalize(text);
  return Boolean(
    findVisualization(text) &&
    !includesAny(normalized, ["삭제", "지워", "제거", "수정", "변경", "바꿔", "바꾸", "교체", "정렬", "위치"]) &&
    includesAny(normalized, ["위젯", "그래프", "차트", "구성", "생성", "만들", "세팅"])
  );
}

function isWidgetMutationRequest(text: string): boolean {
  const normalized = normalize(text);
  const hasMutationIntent = includesAny(normalized, [
    "삭제",
    "지워",
    "제거",
    "수정",
    "변경",
    "바꿔",
    "바꾸",
    "교체",
    "정렬",
    "위치"
  ]);
  const hasWidgetTarget =
    normalized.includes("위젯") ||
    normalized.includes("그래프") ||
    normalized.includes("차트") ||
    Boolean(findVisualization(text));

  return hasMutationIntent && hasWidgetTarget;
}

function formatEquipmentList(equipment: EquipmentCurrent[]): string {
  if (equipment.length === 0) return "등록된 장비가 없습니다.";

  return [
    "장비 목록입니다.",
    "",
    ...equipment.map((item) => {
      const sensorCount = item.current?.sensors?.length ?? 0;
      const status = item.current?.status ?? "unknown";
      return `- ${item.equipmentName} (장비 ${item.equipmentId}, 대시보드 ${item.dashboardId ?? "-"}): 상태 ${status}, 센서 ${sensorCount}개`;
    })
  ].join("\n");
}

function findEquipment(text: string, equipment: EquipmentCurrent[]): EquipmentCurrent[] {
  const normalized = normalize(text);
  const explicitId = text.match(/equipmentEntityId\s*=?\s*(\d+)|장비\s*(\d+)/i)?.[1]
    ?? text.match(/equipmentEntityId\s*=?\s*(\d+)|장비\s*(\d+)/i)?.[2];

  if (explicitId) {
    return equipment.filter((item) => String(item.equipmentId) === explicitId);
  }

  const byName = equipment.filter((item) => {
    const name = normalize(item.equipmentName);
    const shortName = normalize(item.equipmentName.split("-")[0] ?? item.equipmentName);
    return normalized.includes(name) || normalized.includes(shortName);
  });

  return byName;
}

function findWidgetEquipment(
  text: string,
  equipment: EquipmentCurrent[],
  dashboardId?: number
): EquipmentCurrent[] {
  const normalized = normalize(text);
  const explicitEquipmentId =
    text.match(/equipmentEntityId\s*=?\s*(\d+)/i)?.[1] ??
    text.match(/장비\s*(?:id|아이디)\s*=?\s*(\d+)/i)?.[1];
  const candidates = dashboardId
    ? equipment.filter((item) => item.dashboardId === dashboardId)
    : equipment;

  if (explicitEquipmentId) {
    return candidates.filter((item) => String(item.equipmentId) === explicitEquipmentId);
  }

  return candidates.filter((item) => {
    const name = normalize(item.equipmentName);
    const shortName = normalize(item.equipmentName.split("-")[0] ?? item.equipmentName);
    return normalized.includes(name) || normalized.includes(shortName);
  });
}

function findWidgetSensor(text: string, sensors: SensorCurrent[]): SensorCurrent | null {
  const loose = normalizeLoose(text);
  const exact = sensors.find((sensor) => loose.includes(normalizeLoose(sensor.sensorId)));
  if (exact) return exact;

  const keyword = SENSOR_KEYWORDS.find((item) =>
    item.aliases.some((alias) => loose.includes(normalizeLoose(alias)))
  );
  if (!keyword) return null;

  return sensors.find((sensor) =>
    keyword.sensorAliases.some((alias) => normalizeLoose(sensor.sensorId).includes(normalizeLoose(alias)))
  ) ?? null;
}

function getRequestedSensorAliases(text: string): string[] {
  const loose = normalizeLoose(text);
  const exactAliases = SENSOR_KEYWORDS.find((item) =>
    item.aliases.some((alias) => loose.includes(normalizeLoose(alias)))
  )?.sensorAliases ?? [];
  const explicitSensorTokens = text
    .split(/\s+/)
    .map((token) => token.trim())
    .filter((token) => token.includes("_") || /[A-Za-z]+/.test(token))
    .map(normalizeLoose)
    .filter((token) => token.length >= 3);

  return Array.from(new Set([...exactAliases.map(normalizeLoose), ...explicitSensorTokens]));
}

function widgetMatchesRequestedSensor(widget: Widget, requestedAliases: string[]): boolean {
  if (requestedAliases.length === 0) return true;

  const widgetSensor = normalizeLoose(String(widget.sensorName ?? widget.sensorId ?? widget.title ?? ""));
  return requestedAliases.some((alias) =>
    widgetSensor.includes(alias) || alias.includes(widgetSensor)
  );
}

function normalizeWidgetTypeForSensor(
  visualization: WidgetVisualization,
  sensor: SensorCurrent
): string {
  const dataType = sensor.dataType.toUpperCase();
  if (
    (dataType === "BOOLEAN" || dataType === "STRING") &&
    !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)
  ) {
    return "STATUS";
  }
  return visualization.widgetType;
}

function normalizeChartTypeForSensor(
  visualization: WidgetVisualization,
  sensor: SensorCurrent
): string {
  const dataType = sensor.dataType.toUpperCase();
  if (
    (dataType === "BOOLEAN" || dataType === "STRING") &&
    !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)
  ) {
    return "status";
  }
  return visualization.chartType;
}

function normalizeWidgetWidth(visualization: WidgetVisualization, sensor: SensorCurrent): number {
  const dataType = sensor.dataType.toUpperCase();
  if (
    (dataType === "BOOLEAN" || dataType === "STRING") &&
    !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)
  ) {
    return 3;
  }
  return visualization.width;
}

function getNextWidgetPosition(widgets: Widget[], dashboardId?: number, width = 3, height = 3) {
  const dashboardWidgets = dashboardId
    ? widgets.filter((widget) => widget.dashboardId === dashboardId)
    : widgets;
  const maxBottom = dashboardWidgets.reduce(
    (max, widget) => Math.max(max, (widget.posY ?? 0) + (widget.height ?? 3)),
    0
  );

  return {
    posX: 0,
    posY: maxBottom,
    width,
    height
  };
}

function buildWidgetPlan(
  text: string,
  equipment: EquipmentCurrent[],
  widgets: Widget[]
): { plan?: WidgetPlan; error?: string } {
  const dashboardId = extractDashboardId(text);
  const visualization = findVisualization(text);
  if (!visualization) {
    return { error: "요청한 그래프/위젯 표시 방식을 찾지 못했습니다." };
  }

  const equipmentMatches = findWidgetEquipment(text, equipment, dashboardId);
  if (equipmentMatches.length === 0) {
    return { error: "장비를 찾지 못했습니다. 장비명 또는 equipmentEntityId를 포함해주세요." };
  }
  if (equipmentMatches.length > 1) {
    return {
      error: [
        "장비가 여러 개 매칭되었습니다. dashboardId 또는 equipmentEntityId를 포함해주세요.",
        "",
        ...equipmentMatches.map((item) => `- ${item.equipmentName} (equipmentEntityId=${item.equipmentId}, dashboardId=${item.dashboardId ?? "-"})`)
      ].join("\n")
    };
  }

  const targetEquipment = equipmentMatches[0];
  const sensor = findWidgetSensor(text, targetEquipment.current?.sensors ?? []);
  if (!sensor) {
    return {
      error: [
        "센서를 찾지 못했습니다. 센서명을 정확히 포함해주세요.",
        "",
        "사용 가능한 센서:",
        ...(targetEquipment.current?.sensors ?? []).map((item) => `- ${item.sensorId}`)
      ].join("\n")
    };
  }

  const width = normalizeWidgetWidth(visualization, sensor);
  const height = visualization.height;
  const position = getNextWidgetPosition(widgets, targetEquipment.dashboardId, width, height);
  const widgetType = normalizeWidgetTypeForSensor(visualization, sensor);
  const chartType = normalizeChartTypeForSensor(visualization, sensor);
  const existingWidget = widgets.find((widget) =>
    widgetMatchesEquipment(widget, targetEquipment) &&
    widgetMatchesSensor(widget, sensor) &&
    widget.widgetType === widgetType
  );
  const request: WidgetRequest = {
    dashboardId: targetEquipment.dashboardId,
    equipmentId: targetEquipment.equipmentName,
    equipmentEntityId: targetEquipment.equipmentId,
    widgetType,
    title: `${targetEquipment.equipmentName} ${sensor.sensorId}`,
    sensorId: sensor.sensorId,
    chartType,
    dataType: sensor.dataType,
    unit: sensor.unit ?? "",
    ...position,
    configJson: JSON.stringify({
      dataKey: sensor.sensorId,
      equipmentId: targetEquipment.equipmentName,
      equipmentEntityId: targetEquipment.equipmentId,
      visualization: visualization.label
    })
  };

  return {
    plan: {
      equipment: targetEquipment,
      sensor,
      visualization,
      request,
      existingWidget
    }
  };
}

function formatWidgetPlan(plan: WidgetPlan, created?: Widget): string {
  if (plan.existingWidget && !created) {
    return [
      "이미 처리된 위젯 구성입니다.",
      "",
      `- 장비: ${plan.equipment.equipmentName} (대시보드 ${plan.equipment.dashboardId ?? "-"} / 장비 ${plan.equipment.equipmentId})`,
      `- 센서: ${plan.sensor.sensorId}`,
      `- 표시 방식: ${plan.visualization.label}`,
      `- 기존 widgetId: ${getWidgetId(plan.existingWidget)}`,
      "",
      "같은 장비/센서/표시 방식의 위젯이 이미 존재해서 새로 만들지 않았습니다."
    ].join("\n");
  }

  return [
    created ? "위젯 생성을 완료했습니다." : "위젯 구성 계획입니다.",
    "",
    `- 장비: ${plan.equipment.equipmentName} (대시보드 ${plan.equipment.dashboardId ?? "-"} / 장비 ${plan.equipment.equipmentId})`,
    `- 센서: ${plan.sensor.sensorId} (${plan.sensor.dataType}, unit=${plan.sensor.unit ?? ""})`,
    `- 표시 방식: ${plan.visualization.label}`,
    `- 위치: (${plan.request.posX}, ${plan.request.posY}), 크기 ${plan.request.width}x${plan.request.height}`,
    created ? `- widgetId: ${created.id}` : ""
  ].filter(Boolean).join("\n");
}

function getWidgetId(widget: Widget): number {
  return Number(widget.id);
}

function widgetMatchesEquipment(widget: Widget, equipment: EquipmentCurrent): boolean {
  return (
    widget.equipmentEntityId === equipment.equipmentId ||
    widget.equipmentName === equipment.equipmentName ||
    widget.equipmentId === equipment.equipmentName
  );
}

function widgetMatchesSensor(widget: Widget, sensor: SensorCurrent): boolean {
  const widgetSensor = normalizeLoose(String(widget.sensorName ?? widget.sensorId ?? widget.title ?? ""));
  const targetSensor = normalizeLoose(sensor.sensorId);
  return widgetSensor.includes(targetSensor) || targetSensor.includes(widgetSensor);
}

function formatWidgetTypeLabel(widgetType?: string): string {
  const found = SUPPORTED_WIDGET_VISUALIZATIONS.find((item) => item.widgetType === widgetType);
  return found?.label ?? widgetType ?? "-";
}

function formatWidgetTarget(widget: Widget): string {
  return `${widget.title} (widgetId=${getWidgetId(widget)}, 장비=${widget.equipmentName ?? widget.equipmentId ?? ""}, 센서=${widget.sensorName ?? widget.sensorId ?? ""}, 표시 방식=${formatWidgetTypeLabel(widget.widgetType)})`;
}

function extractWidgetId(text: string): number | undefined {
  const match =
    text.match(/widget\s*id\s*=?\s*(\d+)/i) ??
    text.match(/widgetId\s*=?\s*(\d+)/i) ??
    text.match(/위젯\s*(?:id|아이디)?\s*(\d+)\s*번?/i) ??
    text.match(/(\d+)\s*번\s*위젯/i);

  return match ? Number(match[1]) : undefined;
}

function findWidgets(
  text: string,
  equipment: EquipmentCurrent[],
  widgets: Widget[]
): { widgets?: Widget[]; error?: string } {
  const dashboardId = extractDashboardId(text);
  const widgetId = extractWidgetId(text);
  const requestedSensorAliases = getRequestedSensorAliases(text);
  const equipmentMatches = findWidgetEquipment(text, equipment, dashboardId);

  let candidates = widgets;
  if (dashboardId) {
    candidates = candidates.filter((widget) => widget.dashboardId === dashboardId);
  }
  if (widgetId) {
    candidates = candidates.filter((widget) => getWidgetId(widget) === widgetId);
  }
  if (equipmentMatches.length === 1) {
    const targetEquipment = equipmentMatches[0];
    candidates = candidates.filter((widget) =>
      widget.equipmentEntityId === targetEquipment.equipmentId ||
      widget.equipmentName === targetEquipment.equipmentName ||
      widget.equipmentId === targetEquipment.equipmentName
    );
  } else if (equipmentMatches.length > 1) {
    const equipmentNames = new Set(equipmentMatches.map((item) => item.equipmentName));
    candidates = candidates.filter((widget) =>
      equipmentNames.has(widget.equipmentName ?? "") ||
      equipmentNames.has(widget.equipmentId ?? "")
    );
  }
  if (requestedSensorAliases.length > 0) {
    candidates = candidates.filter((widget) =>
      widgetMatchesRequestedSensor(widget, requestedSensorAliases)
    );
  }

  if (candidates.length === 0) {
    return {
      error: [
        "조건에 맞는 위젯을 찾지 못했습니다.",
        "위젯 목록을 먼저 확인하거나 dashboardId, equipmentEntityId, 센서명을 더 구체적으로 입력해주세요."
      ].join("\n")
    };
  }

  const normalized = normalize(text);
  const canAffectMany = includesAny(normalized, ["전체", "모든", "정렬", "위치"]) || Boolean(widgetId);
  if (candidates.length > 1 && !canAffectMany) {
    return {
      error: [
        "여러 위젯이 매칭되었습니다. 더 구체적으로 지정해주세요.",
        "",
        ...candidates.slice(0, 10).map((widget) => `- ${formatWidgetTarget(widget)}`)
      ].join("\n")
    };
  }

  return { widgets: candidates };
}

function mergeConfigJson(configJson: string | undefined, extra: Record<string, unknown>): string {
  if (!configJson) return JSON.stringify(extra);

  try {
    return JSON.stringify({
      ...(JSON.parse(configJson) as Record<string, unknown>),
      ...extra
    });
  } catch {
    return JSON.stringify(extra);
  }
}

function buildWidgetUpdateRequest(widget: Widget, visualization: WidgetVisualization): WidgetRequest {
  const syntheticSensor: SensorCurrent = {
    sensorId: String(widget.sensorName ?? widget.sensorId ?? ""),
    dataType: String(widget.dataType ?? ""),
    value: null,
    unit: widget.unit
  };

  return {
    dashboardId: widget.dashboardId,
    equipmentId: widget.equipmentId ?? widget.equipmentName,
    equipmentEntityId: widget.equipmentEntityId,
    widgetType: normalizeWidgetTypeForSensor(visualization, syntheticSensor),
    title: widget.title,
    sensorId: widget.sensorId ?? widget.sensorName,
    chartType: normalizeChartTypeForSensor(visualization, syntheticSensor),
    dataType: widget.dataType,
    unit: widget.unit ?? "",
    posX: widget.posX,
    posY: widget.posY,
    width: normalizeWidgetWidth(visualization, syntheticSensor),
    height: visualization.height,
    configJson: mergeConfigJson(widget.configJson, {
      visualization: visualization.label
    })
  };
}

function getLayoutGroup(widget: Widget): "숫자" | "상태" | "로그" {
  if (["LOG", "ALERTS"].includes(widget.widgetType)) return "로그";
  if (
    widget.widgetType === "STATUS" ||
    String(widget.dataType ?? "").toUpperCase() === "BOOLEAN" ||
    String(widget.dataType ?? "").toUpperCase() === "STRING"
  ) {
    return "상태";
  }
  return "숫자";
}

function getSmartWidth(widget: Widget): number {
  if (["TREND", "BAR_H", "SENSORS", "OEE"].includes(widget.widgetType)) return 6;
  if (widget.widgetType === "BAR_V") return 4;
  return 3;
}

function buildSmartLayout(widgets: Widget[]): LayoutPlan[] {
  const sorted = [...widgets].sort((a, b) => (a.posY - b.posY) || (a.posX - b.posX));
  const groups: Array<"숫자" | "상태" | "로그"> = ["숫자", "상태", "로그"];
  const ordered = groups.flatMap((group) => sorted.filter((widget) => getLayoutGroup(widget) === group));
  const plans: LayoutPlan[] = [];
  let cursorX = 0;
  let cursorY = 0;
  let rowHeight = 3;
  let previousGroup: "숫자" | "상태" | "로그" | null = null;

  for (const widget of ordered) {
    const group = getLayoutGroup(widget);
    const width = group === "로그" ? 12 : getSmartWidth(widget);
    const height = group === "로그" ? Math.max(widget.height ?? 3, 3) : 3;

    if (previousGroup && previousGroup !== group) {
      cursorX = 0;
      cursorY += rowHeight;
      rowHeight = height;
    }

    if (cursorX + width > 12) {
      cursorX = 0;
      cursorY += rowHeight;
      rowHeight = height;
    }

    plans.push({
      widget,
      group,
      layout: {
        widgetId: getWidgetId(widget),
        posX: cursorX,
        posY: cursorY,
        width,
        height
      }
    });

    cursorX += width;
    rowHeight = Math.max(rowHeight, height);
    previousGroup = group;
  }

  return plans;
}

function formatWidgetDeleteResult(widgets: Widget[], executed: boolean): string {
  return [
    executed ? "위젯 삭제를 완료했습니다." : "위젯 삭제 계획입니다.",
    executed ? `- 삭제한 위젯 수: ${widgets.length}개` : "- 실제 DB 변경은 없습니다.",
    "",
    ...widgets.map((widget) => `- 삭제 대상: ${formatWidgetTarget(widget)}`)
  ].join("\n");
}

function formatWidgetUpdateResult(
  widgets: Widget[],
  visualization: WidgetVisualization,
  executed: boolean,
  skippedWidgets: Widget[] = []
): string {
  if (widgets.length === 0 && skippedWidgets.length > 0) {
    return [
      "이미 처리된 위젯 수정 요청입니다.",
      "",
      ...skippedWidgets.map((widget) =>
        `- ${formatWidgetTarget(widget)}: 이미 ${visualization.label}입니다.`
      ),
      "",
      "변경할 위젯이 없어서 API를 호출하지 않았습니다."
    ].join("\n");
  }

  return [
    executed ? "위젯 표시 방식 수정을 완료했습니다." : "위젯 표시 방식 수정 계획입니다.",
    executed ? `- 수정한 위젯 수: ${widgets.length}개` : "- 실제 DB 변경은 없습니다.",
    "",
    ...widgets.map((widget) =>
      `- ${formatWidgetTarget(widget)}: ${formatWidgetTypeLabel(widget.widgetType)} -> ${visualization.label}`
    ),
    ...(
      skippedWidgets.length > 0
        ? [
          "",
          "이미 같은 표시 방식이라 건너뛴 위젯:",
          ...skippedWidgets.map((widget) => `- ${formatWidgetTarget(widget)}`)
        ]
        : []
    )
  ].join("\n");
}

function formatLayoutResult(plans: LayoutPlan[], executed: boolean): string {
  return [
    executed ? "위젯 스마트 정렬을 완료했습니다." : "위젯 스마트 정렬 계획입니다.",
    executed ? `- 정렬한 위젯 수: ${plans.length}개` : "- 실제 DB 변경은 없습니다.",
    "",
    "정렬 정책: 숫자형 위젯 상단, 상태 위젯 중간, 로그/알림 위젯 하단",
    ...plans.map(({ widget, layout, group }) =>
      `- [${group}] ${formatWidgetTarget(widget)} -> 위치 (${layout.posX}, ${layout.posY}), 크기 ${layout.width}x${layout.height}`
    )
  ].join("\n");
}

function formatSensorList(equipment: EquipmentCurrent): string {
  const sensors = equipment.current?.sensors ?? [];
  const numericSensors = sensors.filter((sensor) =>
    ["FLOAT", "DOUBLE", "INTEGER", "INT"].includes(sensor.dataType)
  );
  const logSensors = sensors.filter((sensor) => {
    const name = sensor.sensorId.toLowerCase();
    return name.includes("log") || name.includes("message");
  });
  const statusSensors = sensors.filter((sensor) => {
    if (logSensors.includes(sensor)) return false;
    return sensor.dataType === "BOOLEAN" || sensor.dataType === "STRING";
  });
  const formatValue = (value: unknown, unit?: string) => {
    const text = String(value ?? "").trim();
    return unit ? `${text} ${unit}` : text || "-";
  };
  const formatStatus = (value: unknown) => {
    if (typeof value === "boolean") return value ? "켜짐" : "꺼짐";
    if (String(value).toLowerCase() === "true") return "켜짐";
    if (String(value).toLowerCase() === "false") return "꺼짐";
    return String(value ?? "").trim() || "-";
  };
  const formatLog = (value: unknown) => {
    return String(value ?? "").trim() || "현재 메시지 없음";
  };

  return [
    `${equipment.equipmentName} 센서 목록입니다.`,
    "",
    `현재 상태: ${equipment.current?.status ?? "unknown"}`,
    `센서 수: ${sensors.length}개`,
    "",
    "수치 센서",
    ...(numericSensors.length > 0
      ? numericSensors.map((sensor) => `- ${sensor.sensorId}: ${formatValue(sensor.value, sensor.unit)}`)
      : ["- 없음"]),
    "",
    "상태 센서",
    ...(statusSensors.length > 0
      ? statusSensors.map((sensor) => `- ${sensor.sensorId}: ${formatStatus(sensor.value)}`)
      : ["- 없음"]),
    "",
    "로그 센서",
    ...(logSensors.length > 0
      ? logSensors.map((sensor) => `- ${sensor.sensorId}: ${formatLog(sensor.value)}`)
      : ["- 없음"])
  ].join("\n");
}

function formatSetupSummary(dashboards: Dashboard[], equipment: EquipmentCurrent[], widgets: Widget[]): string {
  const freshThresholdMs = 120_000;
  const now = Date.now();
  const freshCount = equipment.filter((item) => {
    const timestamp = item.current?.timestamp;
    return timestamp ? now - Date.parse(timestamp) <= freshThresholdMs : false;
  }).length;
  const widgetCountByEquipment = new Map<string, number>();

  widgets.forEach((widget) => {
    const key = String(widget.equipmentEntityId ?? widget.equipmentId ?? widget.equipmentName ?? "");
    widgetCountByEquipment.set(key, (widgetCountByEquipment.get(key) ?? 0) + 1);
  });

  const lines = [
    "현재 대시보드 세팅 상태입니다.",
    "",
    `- 대시보드: ${dashboards.length}개`,
    `- 장비: ${equipment.length}개`,
    `- 위젯: ${widgets.length}개`,
    `- 최근 데이터 수신 장비: ${freshCount}개`,
    `- 오래된 데이터 장비: ${equipment.length - freshCount}개`,
    "",
    "장비별 상태:"
  ];

  equipment.forEach((item) => {
    const widgetCount =
      widgetCountByEquipment.get(String(item.equipmentId)) ??
      widgetCountByEquipment.get(item.equipmentName) ??
      0;
    const timestamp = item.current?.timestamp;
    const isFresh = timestamp ? now - Date.parse(timestamp) <= freshThresholdMs : false;

    lines.push(
      `- ${item.equipmentName} (대시보드 ${item.dashboardId ?? "-"} / 장비 ${item.equipmentId})`,
      `  상태: ${item.current?.status ?? "unknown"}`,
      `  센서: ${item.current?.sensors?.length ?? 0}개`,
      `  위젯: ${widgetCount > 0 ? `${widgetCount}개` : "없음"}`,
      `  데이터 수신: ${isFresh ? "정상" : "오래됨"}`,
      ""
    );
  });

  if (freshCount < equipment.length) {
    lines.push("- 최근 telemetry가 오래된 장비가 있습니다. 시뮬레이터/EdgeGateway/MQTT 상태를 확인하세요.");
  }
  if (widgets.length === 0) {
    lines.push("- 위젯이 없습니다. 자동 위젯 구성 또는 수동 위젯 생성을 실행하세요.");
  }

  return lines.join("\n").trim();
}

function formatSupportedWidgets(): string {
  return [
    "MCP가 지원하는 위젯/그래프 종류입니다.",
    "",
    "- 게이지: 단일 숫자 센서 현재값",
    "- 선 그래프: 시간 흐름에 따른 수치 변화",
    "- 막대그래프: 수치 비교 또는 현재값 강조",
    "- 가로 막대그래프: 이름이 긴 항목 비교",
    "- 도넛 그래프: 비율/점유율 형태의 수치 표현",
    "- 상태 위젯: BOOLEAN/STRING 상태값",
    "- 로그 위젯: 문자열 로그 메시지",
    "- 알림 위젯: 이상/경고 이벤트",
    "- 센서 목록 위젯: 장비 센서 전체 목록",
    "- OEE 위젯: 설비 종합 효율"
  ].join("\n");
}

function formatFallback(): string {
  return [
    "아직 이 요청은 자동 실행 규칙으로 해석하지 못했어요.",
    "",
    "현재 지원하는 예시는 다음과 같습니다.",
    "- 장비 목록 보여줘",
    "- equipmentEntityId 9 장비 센서 목록 보여줘",
    "- 현재 대시보드 세팅 상태 점검해줘",
    "- 지원하는 위젯 종류 보여줘",
    "- CVD 압력 위젯을 막대그래프로 바꿔줘",
    "- ETCHER 압력 위젯 삭제 계획만 보여줘",
    "- dashboardId 2의 위젯 위치를 스마트 정렬해줘",
    "",
    "위젯 변경이 부담되면 문장에 '계획만 보여줘'를 붙여 먼저 확인할 수 있습니다."
  ].join("\n");
}

async function handleChat(request: IncomingMessage): Promise<ChatResponse> {
  const body = await readBody(request) as ChatRequest;
  const message = body.message?.trim();
  if (!message) {
    return {
      action: "empty",
      reply: "메시지를 입력해주세요."
    };
  }

  const token = getBearerToken(request);
  const api = new DashboardApiClient({
    ...loadConfig(),
    dashboardApiToken: token ?? loadConfig().dashboardApiToken
  });
  const normalized = normalize(message);
  const sessionKey = getSessionKey(request, token);

  cleanupExpiredPendingActions();

  if (isCancelConfirmation(normalized)) {
    if (pendingActions.delete(sessionKey)) {
      return {
        action: "pending_cancelled",
        reply: "대기 중이던 위젯 작업을 취소했습니다."
      };
    }

    return {
      action: "pending_not_found",
      reply: "취소할 대기 작업이 없습니다."
    };
  }

  if (isApplyConfirmation(normalized)) {
    const pending = pendingActions.get(sessionKey);
    if (!pending) {
      return {
        action: "pending_not_found",
        reply: "적용할 대기 작업이 없습니다. 먼저 위젯 생성/수정/삭제/정렬 명령을 입력해주세요."
      };
    }

    pendingActions.delete(sessionKey);
    return pending.execute(api);
  }

  if (normalized.includes("지원") && (normalized.includes("위젯") || normalized.includes("그래프"))) {
    return { action: "supported_widgets", reply: formatSupportedWidgets() };
  }

  const [dashboards, equipment, widgets] = await Promise.all([
    api.get<Dashboard[]>("/api/dashboards").catch(() => []),
    api.get<EquipmentCurrent[]>("/api/equipment/current").catch(() => []),
    api.get<Widget[]>("/api/dashboard/widgets").catch(() => [])
  ]);

  if (normalized.includes("점검") || normalized.includes("세팅상태") || normalized.includes("상태점검")) {
    return {
      action: "setup_summary",
      reply: formatSetupSummary(dashboards, equipment, widgets),
      data: { dashboards, equipment, widgets }
    };
  }

  if (isWidgetMutationRequest(message)) {
    const isDelete = includesAny(normalized, ["삭제", "지워", "제거"]);
    const isLayout = includesAny(normalized, ["정렬", "위치"]);
    const isUpdate = includesAny(normalized, ["수정", "변경", "바꿔", "바꾸", "교체"]);
    const target = findWidgets(message, equipment, widgets);

    if (!target.widgets) {
      return {
        action: "widget_mutation_failed",
        reply: target.error ?? "위젯 요청을 처리하지 못했습니다.",
        data: { equipment, widgets }
      };
    }

    if (isLayout) {
      const plans = buildSmartLayout(target.widgets);
      const body: WidgetLayoutUpdate = {
        layouts: plans.map((plan) => plan.layout)
      };

      storePendingAction(sessionKey, {
        createdAt: Date.now(),
        execute: async (pendingApi) => {
          const updated = await pendingApi.put<Widget[]>("/api/dashboard/widgets/layout", body);
          return {
            action: "widget_layout_updated",
            reply: formatLayoutResult(plans, true),
            data: updated
          };
        }
      });

      return {
        action: "widget_layout_plan",
        reply: withApplyPrompt(formatLayoutResult(plans, false)),
        requiresConfirmation: true,
        data: plans
      };
    }

    if (isDelete) {
      storePendingAction(sessionKey, {
        createdAt: Date.now(),
        execute: async (pendingApi) => {
          await Promise.all(target.widgets!.map((widget) => pendingApi.delete<unknown>(`/api/dashboard/widgets/${getWidgetId(widget)}`)));
          return {
            action: "widget_deleted",
            reply: formatWidgetDeleteResult(target.widgets!, true),
            data: target.widgets
          };
        }
      });

      return {
        action: "widget_delete_plan",
        reply: withApplyPrompt(formatWidgetDeleteResult(target.widgets, false)),
        requiresConfirmation: true,
        data: target.widgets
      };
    }

    if (isUpdate) {
      const visualization = findVisualization(message);
      if (!visualization) {
        return {
          action: "widget_update_failed",
          reply: "어떤 표시 방식으로 바꿀지 찾지 못했습니다. 예: 게이지에서 막대그래프로 바꿔줘",
          data: target.widgets
        };
      }

      const widgetsToUpdate = target.widgets.filter((widget) => widget.widgetType !== visualization.widgetType);
      const skippedWidgets = target.widgets.filter((widget) => widget.widgetType === visualization.widgetType);

      if (widgetsToUpdate.length === 0) {
        return {
          action: "widget_already_updated",
          reply: formatWidgetUpdateResult([], visualization, false, skippedWidgets),
          data: skippedWidgets
        };
      }

      storePendingAction(sessionKey, {
        createdAt: Date.now(),
        execute: async (pendingApi) => {
          const updated = await Promise.all(
            widgetsToUpdate.map((widget) =>
              pendingApi.put<Widget>(`/api/dashboard/widgets/${getWidgetId(widget)}`, buildWidgetUpdateRequest(widget, visualization))
            )
          );
          return {
            action: "widget_updated",
            reply: formatWidgetUpdateResult(widgetsToUpdate, visualization, true, skippedWidgets),
            data: { updated, skipped: skippedWidgets }
          };
        }
      });

      return {
        action: "widget_update_plan",
        reply: withApplyPrompt(formatWidgetUpdateResult(widgetsToUpdate, visualization, false, skippedWidgets)),
        requiresConfirmation: true,
        data: { update: widgetsToUpdate, skipped: skippedWidgets }
      };
    }
  }

  if (isWidgetCreateRequest(message)) {
    const { plan, error } = buildWidgetPlan(message, equipment, widgets);
    if (!plan) {
      return {
        action: "widget_create_failed",
        reply: error ?? "위젯 구성 요청을 처리하지 못했습니다.",
        data: { equipment, widgets }
      };
    }

    if (plan.existingWidget) {
      return {
        action: "widget_already_exists",
        reply: formatWidgetPlan(plan),
        data: plan.existingWidget
      };
    }

    storePendingAction(sessionKey, {
      createdAt: Date.now(),
      execute: async (pendingApi) => {
        const created = await pendingApi.post<Widget>("/api/dashboard/widgets", plan.request);
        return {
          action: "widget_created",
          reply: formatWidgetPlan(plan, created),
          data: created
        };
      }
    });

    return {
      action: "widget_create_plan",
      reply: withApplyPrompt(formatWidgetPlan(plan)),
      requiresConfirmation: true,
      data: plan
    };
  }

  if (normalized.includes("장비목록") || (normalized.includes("장비") && normalized.includes("보여"))) {
    const wantsSensors = normalized.includes("센서");
    if (!wantsSensors) {
      return { action: "equipment_list", reply: formatEquipmentList(equipment), data: equipment };
    }
  }

  if (normalized.includes("센서")) {
    const matches = findEquipment(message, equipment);
    if (matches.length === 1) {
      return { action: "sensor_list", reply: formatSensorList(matches[0]), data: matches[0] };
    }
    if (matches.length > 1) {
      return {
        action: "equipment_disambiguation",
        reply: [
          "장비가 여러 개 매칭되었습니다. equipmentEntityId 또는 대시보드 번호를 포함해서 다시 요청해주세요.",
          "",
          ...matches.map((item) => `- ${item.equipmentName} (equipmentEntityId=${item.equipmentId}, dashboardId=${item.dashboardId ?? "-"})`)
        ].join("\n"),
        data: matches
      };
    }
  }

  return { action: "fallback", reply: formatFallback() };
}

const server = createServer(async (request, response) => {
  if (request.method === "OPTIONS") {
    sendJson(response, 204, {});
    return;
  }

  if (request.method === "GET" && request.url === "/health") {
    sendJson(response, 200, { status: "UP", service: "capstone-dashboard-mcp-http" });
    return;
  }

  if (request.method === "POST" && request.url === "/api/mcp/chat") {
    try {
      sendJson(response, 200, await handleChat(request));
    } catch (error) {
      sendJson(response, 500, {
        action: "error",
        reply: error instanceof Error ? error.message : "MCP 채팅 처리 중 오류가 발생했습니다."
      });
    }
    return;
  }

  sendJson(response, 404, { message: "Not found" });
});

server.listen(PORT, () => {
  console.log(`MCP HTTP bridge listening on http://localhost:${PORT}`);
});
