import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type { Dashboard, EquipmentCurrent, Widget } from "../types/api.js";
import { toToolText } from "./utils.js";

export function registerHealthTools(server: McpServer, api: DashboardApiClient): void {
  server.tool("check_backend_health", "백엔드 기본 health check를 수행합니다.", {}, async () => {
    const result = await api.get<unknown>("/api/public/health");
    return toToolText(result);
  });

  server.tool("check_backend_detailed_health", "백엔드 상세 health check를 수행합니다.", {}, async () => {
    const result = await api.get<unknown>("/api/public/health/detailed");
    return toToolText(result);
  });

  server.tool(
    "check_recent_telemetry",
    "장비별 최신 telemetry timestamp를 확인해서 최근 데이터 수신 여부를 점검합니다.",
    {
      maxAgeSeconds: z.number().int().positive().default(60)
    },
    async ({ maxAgeSeconds }) => {
      const equipment = await api.get<EquipmentCurrent[]>("/api/equipment/current");
      const now = Date.now();

      const result = equipment.map((item) => {
        const timestamp = item.current?.timestamp;
        const ageSeconds = timestamp ? Math.round((now - Date.parse(timestamp)) / 1000) : null;
        const isFresh = ageSeconds !== null && ageSeconds <= maxAgeSeconds;

        return {
          equipmentEntityId: item.equipmentId,
          equipmentName: item.equipmentName,
          status: item.current?.status ?? null,
          timestamp: timestamp ?? null,
          ageSeconds,
          isFresh,
          sensorCount: item.current?.sensors?.length ?? 0
        };
      });

      return toToolText({
        maxAgeSeconds,
        totalEquipment: result.length,
        freshEquipment: result.filter((item) => item.isFresh).length,
        staleEquipment: result.filter((item) => !item.isFresh).length,
        equipment: result
      });
    }
  );

  server.tool(
    "generate_setup_summary",
    "대시보드/장비/센서 스냅샷/위젯 구성을 종합해서 설치 및 운영 점검 요약을 생성합니다.",
    {
      maxTelemetryAgeSeconds: z.number().int().positive().default(60)
    },
    async ({ maxTelemetryAgeSeconds }) => {
      const [health, dashboards, currentEquipment, widgets] = await Promise.all([
        api.get<unknown>("/api/public/health"),
        api.get<Dashboard[]>("/api/dashboards"),
        api.get<EquipmentCurrent[]>("/api/equipment/current"),
        api.get<Widget[]>("/api/dashboard/widgets")
      ]);

      const now = Date.now();
      const equipmentSummaries = currentEquipment.map((equipment) => {
        const sensors = equipment.current?.sensors ?? [];
        const timestamp = equipment.current?.timestamp;
        const ageSeconds = timestamp ? Math.round((now - Date.parse(timestamp)) / 1000) : null;
        const stringSensors = sensors.filter((sensor) => sensor.dataType === "STRING").length;
        const numericSensors = sensors.length - stringSensors;
        const widgetCount = widgets.filter(
          (widget) =>
            widget.equipmentEntityId === equipment.equipmentId ||
            widget.equipmentId === equipment.equipmentName
        ).length;

        return {
          equipmentEntityId: equipment.equipmentId,
          equipmentName: equipment.equipmentName,
          dashboardId: equipment.dashboardId ?? null,
          field: equipment.field ?? null,
          status: equipment.current?.status ?? null,
          latestTelemetryAt: timestamp ?? null,
          telemetryAgeSeconds: ageSeconds,
          telemetryFresh: ageSeconds !== null && ageSeconds <= maxTelemetryAgeSeconds,
          sensorCount: sensors.length,
          numericSensors,
          stringSensors,
          widgetCount
        };
      });

      const staleEquipment = equipmentSummaries.filter((equipment) => !equipment.telemetryFresh);
      const equipmentWithoutWidgets = equipmentSummaries.filter((equipment) => equipment.widgetCount === 0);
      const equipmentWithoutSensors = equipmentSummaries.filter((equipment) => equipment.sensorCount === 0);

      const recommendations: string[] = [];
      if (dashboards.length === 0) {
        recommendations.push("대시보드가 없습니다. create_dashboard로 기본 대시보드를 생성하세요.");
      }
      if (currentEquipment.length === 0) {
        recommendations.push("등록되었거나 현재 telemetry를 가진 장비가 없습니다. EdgeGateway/MQTT 연동 상태를 확인하세요.");
      }
      if (staleEquipment.length > 0) {
        recommendations.push("최근 telemetry가 오래된 장비가 있습니다. 시뮬레이터/EdgeGateway/MQTT 상태를 확인하세요.");
      }
      if (equipmentWithoutSensors.length > 0) {
        recommendations.push("센서 스냅샷이 없는 장비가 있습니다. metadata 스캔 또는 수집 파이프라인을 확인하세요.");
      }
      if (equipmentWithoutWidgets.length > 0) {
        recommendations.push("위젯이 없는 장비가 있습니다. create_widget 또는 자동 위젯 구성을 실행하세요.");
      }
      if (recommendations.length === 0) {
        recommendations.push("기본 설치 점검 항목이 정상입니다.");
      }

      return toToolText({
        checkedAt: new Date().toISOString(),
        maxTelemetryAgeSeconds,
        backendHealth: health,
        totals: {
          dashboards: dashboards.length,
          equipment: currentEquipment.length,
          widgets: widgets.length,
          freshEquipment: equipmentSummaries.filter((equipment) => equipment.telemetryFresh).length,
          staleEquipment: staleEquipment.length
        },
        dashboards,
        equipment: equipmentSummaries,
        recommendations
      });
    }
  );
}
