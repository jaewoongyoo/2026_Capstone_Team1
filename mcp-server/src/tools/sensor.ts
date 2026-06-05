import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type { Sensor } from "../types/api.js";
import { limitArray, toToolText } from "./utils.js";

export function registerSensorTools(server: McpServer, api: DashboardApiClient): void {
  server.tool(
    "get_sensors_by_equipment_id",
    "장비 엔티티 ID로 센서 목록을 조회합니다.",
    {
      equipmentEntityId: z.number().int().positive()
    },
    async ({ equipmentEntityId }) => {
      const result = await api.get<Sensor[]>(`/api/sensors/equipment/${equipmentEntityId}`);
      return toToolText(result);
    }
  );

  server.tool(
    "get_sensors_by_equipment_name",
    "장비명으로 센서 목록을 조회합니다. 예: CVD-CHAMBER-01, ETCHER-01",
    {
      equipmentName: z.string().min(1)
    },
    async ({ equipmentName }) => {
      const result = await api.get<Sensor[]>(
        `/api/sensors/equipment-name/${encodeURIComponent(equipmentName)}`
      );
      return toToolText(result);
    }
  );

  server.tool(
    "search_sensors",
    "현재 로그인 사용자의 센서를 검색합니다. keyword가 없으면 전체 센서 후보 중 기본 최대 20개만 반환합니다.",
    {
      keyword: z.string().optional().default(""),
      limit: z.number().int().min(1).max(100).optional().default(20)
    },
    async ({ keyword, limit }) => {
      const params = new URLSearchParams({ keyword });
      const result = await api.get<Sensor[]>(`/api/sensors/search?${params.toString()}`);
      return toToolText(limitArray(result, limit));
    }
  );

  server.tool(
    "search_equipment_sensors",
    "특정 장비 안에서 센서명을 검색합니다.",
    {
      equipmentEntityId: z.number().int().positive(),
      keyword: z.string().optional().default(""),
      limit: z.number().int().min(1).max(100).optional().default(20)
    },
    async ({ equipmentEntityId, keyword, limit }) => {
      const params = new URLSearchParams({ keyword });
      const result = await api.get<Sensor[]>(
        `/api/sensors/equipment/${equipmentEntityId}/search?${params.toString()}`
      );
      return toToolText(limitArray(result, limit));
    }
  );
}
