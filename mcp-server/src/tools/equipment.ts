import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type { Equipment, EquipmentCurrent } from "../types/api.js";
import { limitArray, toToolText } from "./utils.js";

export function registerEquipmentTools(server: McpServer, api: DashboardApiClient): void {
  server.tool(
    "get_equipment_list",
    "현재 로그인 사용자의 장비 목록을 조회합니다. keyword를 넣으면 장비명/분류 기준으로 검색하며, 기본적으로 최대 20개만 반환합니다.",
    {
      keyword: z.string().optional().default(""),
      limit: z.number().int().min(1).max(100).optional().default(20)
    },
    async ({ keyword, limit }) => {
      const params = new URLSearchParams({ keyword });
      const result = await api.get<Equipment[]>(`/api/equipment/search?${params.toString()}`);
      return toToolText(limitArray(result, limit));
    }
  );

  server.tool(
    "get_dashboard_equipment",
    "특정 대시보드에 속한 장비 목록을 조회합니다.",
    {
      dashboardId: z.number().int().positive()
    },
    async ({ dashboardId }) => {
      const result = await api.get<Equipment[]>(`/api/equipment/dashboard/${dashboardId}`);
      return toToolText(result);
    }
  );

  server.tool(
    "get_current_equipment_all",
    "현재 로그인 사용자의 장비 최신 센서 스냅샷을 조회합니다. 컨텍스트 보호를 위해 기본적으로 최대 50개만 반환합니다.",
    {
      limit: z.number().int().min(1).max(100).optional().default(50)
    },
    async ({ limit }) => {
      const result = await api.get<EquipmentCurrent[]>("/api/equipment/current");
      return toToolText(limitArray(result, limit));
    }
  );

  server.tool(
    "get_current_equipment",
    "장비 엔티티 ID로 최신 센서 스냅샷을 조회합니다.",
    {
      equipmentEntityId: z.number().int().positive()
    },
    async ({ equipmentEntityId }) => {
      const result = await api.get<EquipmentCurrent>(`/api/equipment/${equipmentEntityId}/current`);
      return toToolText(result);
    }
  );
}
