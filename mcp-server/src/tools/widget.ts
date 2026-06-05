import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type {
  EquipmentCurrent,
  Sensor,
  SensorCurrent,
  Widget,
  WidgetLayoutUpdate,
  WidgetRequest
} from "../types/api.js";
import { limitArray, toToolText } from "./utils.js";

const widgetRequestSchema = {
  dashboardId: z.number().int().positive().optional(),
  equipmentId: z.string().min(1).optional(),
  equipmentEntityId: z.number().int().positive().optional(),
  widgetType: z.string().min(1),
  title: z.string().min(1),
  sensorId: z.string().min(1).optional(),
  sensorEntityId: z.number().int().positive().optional(),
  chartType: z.string().optional(),
  dataType: z.string().optional(),
  unit: z.string().optional(),
  posX: z.number().int().min(0),
  posY: z.number().int().min(0),
  width: z.number().int().min(1),
  height: z.number().int().min(1),
  configJson: z.string().optional()
};

export function registerWidgetTools(server: McpServer, api: DashboardApiClient): void {
  server.tool(
    "get_supported_widget_types",
    "MCP가 생성할 수 있는 대시보드 위젯/그래프 타입 목록을 조회합니다.",
    {},
    async () =>
      toToolText([
        { widgetType: "GAUGE", chartType: "line", label: "게이지", useCase: "단일 숫자 센서 현재값" },
        { widgetType: "TREND", chartType: "line", label: "선 그래프", useCase: "시간 흐름에 따른 수치 변화" },
        { widgetType: "BAR_V", chartType: "bar", label: "막대그래프", useCase: "수치 비교 또는 현재값 강조" },
        { widgetType: "BAR_H", chartType: "bar-horizontal", label: "가로 막대그래프", useCase: "항목명이 긴 수치 비교" },
        { widgetType: "DONUT", chartType: "donut", label: "도넛 그래프", useCase: "비율/점유율 형태의 수치 표현" },
        { widgetType: "STATUS", chartType: "status", label: "상태 위젯", useCase: "BOOLEAN/STRING 상태값" },
        { widgetType: "LOG", chartType: "log", label: "로그 위젯", useCase: "문자열 로그 메시지" },
        { widgetType: "ALERTS", chartType: "alerts", label: "알림 위젯", useCase: "이상/경고 이벤트" },
        { widgetType: "SENSORS", chartType: "sensors", label: "센서 목록 위젯", useCase: "장비 센서 전체 목록" },
        { widgetType: "OEE", chartType: "oee", label: "OEE 위젯", useCase: "설비 종합 효율" }
      ])
  );

  server.tool(
    "get_widgets",
    "현재 로그인 사용자의 위젯 목록을 조회합니다. 컨텍스트 보호를 위해 기본적으로 최대 50개만 반환합니다.",
    {
      limit: z.number().int().min(1).max(200).optional().default(50)
    },
    async ({ limit }) => {
      const result = await api.get<Widget[]>("/api/dashboard/widgets");
      return toToolText(limitArray(result, limit, 200));
    }
  );

  server.tool(
    "get_widgets_by_equipment",
    "장비명 기반 equipmentId로 위젯 목록을 조회합니다. 예: CVD-CHAMBER-01",
    {
      equipmentId: z.string().min(1)
    },
    async ({ equipmentId }) => {
      const result = await api.get<Widget[]>(
        `/api/dashboard/widgets/equipment/${encodeURIComponent(equipmentId)}`
      );
      return toToolText(result);
    }
  );

  server.tool(
    "get_widget",
    "위젯 ID로 단건 위젯을 조회합니다.",
    {
      widgetId: z.number().int().positive()
    },
    async ({ widgetId }) => {
      const result = await api.get<Widget>(`/api/dashboard/widgets/${widgetId}`);
      return toToolText(result);
    }
  );

  server.tool(
    "create_widget",
    "새 대시보드 위젯을 생성합니다. equipmentId 또는 equipmentEntityId 중 하나는 필수입니다.",
    widgetRequestSchema,
    async (request) => {
      const result = await api.post<Widget>("/api/dashboard/widgets", request as WidgetRequest);
      return toToolText(result);
    }
  );

  server.tool(
    "update_widget",
    "기존 대시보드 위젯을 수정합니다. request 필드는 create_widget과 동일합니다.",
    {
      widgetId: z.number().int().positive(),
      ...widgetRequestSchema
    },
    async ({ widgetId, ...request }) => {
      const result = await api.put<Widget>(
        `/api/dashboard/widgets/${widgetId}`,
        request as WidgetRequest
      );
      return toToolText(result);
    }
  );

  server.tool(
    "update_widget_layouts",
    "여러 위젯의 위치와 크기를 한 번에 저장합니다. 자연어 흐름에서는 반드시 변경 계획을 먼저 보여주고 사용자 확인 후 호출해야 합니다.",
    {
      layouts: z.array(
        z.object({
          widgetId: z.number().int().positive(),
          posX: z.number().int().min(0),
          posY: z.number().int().min(0),
          width: z.number().int().min(1),
          height: z.number().int().min(1)
        })
      ).min(1)
    },
    async (request) => {
      const result = await api.put<Widget[]>(
        "/api/dashboard/widgets/layout",
        request as WidgetLayoutUpdate
      );
      return toToolText(result);
    }
  );

  server.tool(
    "delete_widget",
    "위젯 ID로 대시보드 위젯을 삭제합니다.",
    {
      widgetId: z.number().int().positive()
    },
    async ({ widgetId }) => {
      await api.delete<unknown>(`/api/dashboard/widgets/${widgetId}`);
      return toToolText({ deleted: true, widgetId });
    }
  );

  server.tool(
    "auto_create_widgets_for_equipment",
    "장비의 현재 센서 스냅샷을 기준으로 기본 위젯 구성을 자동 생성합니다.",
    {
      equipmentEntityId: z.number().int().positive(),
      dashboardId: z.number().int().positive().optional(),
      startX: z.number().int().min(0).default(0),
      startY: z.number().int().min(0).default(0),
      columns: z.number().int().min(1).max(12).default(12),
      skipExisting: z.boolean().default(true),
      dryRun: z.boolean().default(false)
    },
    async ({ equipmentEntityId, dashboardId, startX, startY, columns, skipExisting, dryRun }) => {
      const equipment = await api.get<EquipmentCurrent>(`/api/equipment/${equipmentEntityId}/current`);
      const sensors = await api.get<Sensor[]>(`/api/sensors/equipment/${equipmentEntityId}`);
      const existingWidgets = await api.get<Widget[]>("/api/dashboard/widgets");

      const sensorEntityIdByName = new Map(
        sensors.map((sensor) => [sensor.sensorName, sensor.sensorId] as const)
      );

      const targetDashboardId = dashboardId ?? equipment.dashboardId;
      if (!targetDashboardId) {
        throw new Error("dashboardId를 찾을 수 없습니다. dashboardId를 직접 입력하세요.");
      }

      const equipmentName = equipment.equipmentName;
      const currentSensors = equipment.current?.sensors ?? [];
      const targetExisting = existingWidgets.filter(
        (widget) =>
          widget.equipmentEntityId === equipmentEntityId ||
          widget.equipmentId === equipmentName ||
          widget.equipmentName === equipmentName
      );

      const existingSensorKeys = new Set(
        targetExisting.map((widget) => widget.sensorName ?? widget.sensorId).filter(Boolean)
      );

      const plannedWidgets = currentSensors
        .filter((sensor) => sensor.sensorId)
        .filter((sensor) => !skipExisting || !existingSensorKeys.has(sensor.sensorId))
        .map((sensor, index) =>
          buildWidgetRequest({
            sensor,
            index,
            dashboardId: targetDashboardId,
            equipmentEntityId,
            equipmentName,
            sensorEntityId: sensorEntityIdByName.get(sensor.sensorId),
            startX,
            startY,
            columns
          })
        );

      if (dryRun) {
        return toToolText({
          dryRun: true,
          equipmentEntityId,
          equipmentName,
          dashboardId: targetDashboardId,
          skippedExisting: skipExisting,
          plannedCount: plannedWidgets.length,
          plannedWidgets
        });
      }

      const created: Widget[] = [];
      for (const widget of plannedWidgets) {
        created.push(await api.post<Widget>("/api/dashboard/widgets", widget));
      }

      return toToolText({
        dryRun: false,
        equipmentEntityId,
        equipmentName,
        dashboardId: targetDashboardId,
        createdCount: created.length,
        skippedCount: currentSensors.length - plannedWidgets.length,
        created
      });
    }
  );
}

function buildWidgetRequest(input: {
  sensor: SensorCurrent;
  index: number;
  dashboardId: number;
  equipmentEntityId: number;
  equipmentName: string;
  sensorEntityId?: number;
  startX: number;
  startY: number;
  columns: number;
}): WidgetRequest {
  const widgetType = inferWidgetType(input.sensor);
  const width = widgetType === "LOG" ? 6 : 3;
  const height = widgetType === "LOG" ? 3 : 3;
  const xSlots = Math.max(1, Math.floor(input.columns / width));
  const posX = input.startX + (input.index % xSlots) * width;
  const posY = input.startY + Math.floor(input.index / xSlots) * height;

  return {
    dashboardId: input.dashboardId,
    equipmentId: input.equipmentName,
    equipmentEntityId: input.equipmentEntityId,
    widgetType,
    title: `${input.equipmentName} ${input.sensor.sensorId}`,
    sensorId: input.sensor.sensorId,
    sensorEntityId: input.sensorEntityId,
    chartType: inferChartType(widgetType),
    dataType: input.sensor.dataType,
    unit: input.sensor.unit ?? "",
    posX,
    posY,
    width,
    height,
    configJson: JSON.stringify({
      dataKey: input.sensor.sensorId,
      equipmentId: input.equipmentName
    })
  };
}

function inferWidgetType(sensor: SensorCurrent): string {
  const sensorId = sensor.sensorId.toLowerCase();
  const dataType = sensor.dataType.toUpperCase();

  if (sensorId.includes("log")) {
    return "LOG";
  }

  if (dataType === "BOOLEAN" || dataType === "STRING" || sensorId.includes("state")) {
    return "STATUS";
  }

  return "GAUGE";
}

function inferChartType(widgetType: string): string {
  if (widgetType === "LOG") {
    return "log";
  }
  if (widgetType === "STATUS") {
    return "status";
  }
  return "line";
}
