import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type { Dashboard } from "../types/api.js";
import { toToolText } from "./utils.js";

export function registerDashboardTools(server: McpServer, api: DashboardApiClient): void {
  server.tool("get_dashboards", "현재 로그인 사용자의 대시보드 목록을 조회합니다.", {}, async () => {
    const result = await api.get<Dashboard[]>("/api/dashboards");
    return toToolText(result);
  });

  server.tool(
    "get_dashboard",
    "대시보드 ID로 단건 대시보드를 조회합니다.",
    {
      dashboardId: z.number().int().positive()
    },
    async ({ dashboardId }) => {
      const result = await api.get<Dashboard>(`/api/dashboards/${dashboardId}`);
      return toToolText(result);
    }
  );
}
