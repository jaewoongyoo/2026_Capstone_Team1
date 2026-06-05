import { z } from "zod";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import type { LoginResponse } from "../types/api.js";
import { toToolText } from "./utils.js";

export function registerAuthTools(server: McpServer, api: DashboardApiClient): void {
  server.tool(
    "login",
    "Dashboard API에 로그인하고 이후 MCP tool 호출에 사용할 access token을 저장합니다.",
    {
      username: z.string().min(1),
      password: z.string().min(1)
    },
    async ({ username, password }) => {
      const result = await api.post<LoginResponse>("/api/auth/login", { username, password });
      api.setToken(result.accessToken);

      return toToolText({
        authenticated: true,
        tokenType: result.tokenType ?? "Bearer",
        expiresIn: result.expiresIn
      });
    }
  );

  server.tool("get_me", "현재 인증된 사용자 정보를 조회합니다.", {}, async () => {
    const result = await api.get<unknown>("/api/auth/me");
    return toToolText(result);
  });
}
