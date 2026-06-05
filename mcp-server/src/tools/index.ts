import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import type { DashboardApiClient } from "../client/dashboardApi.js";
import { registerAuthTools } from "./auth.js";
import { registerDashboardTools } from "./dashboard.js";
import { registerEquipmentTools } from "./equipment.js";
import { registerHealthTools } from "./health.js";
import { registerProtocolTools } from "./protocol.js";
import { registerSensorTools } from "./sensor.js";
import { registerWidgetTools } from "./widget.js";

export function registerTools(server: McpServer, api: DashboardApiClient): void {
  registerProtocolTools(server);
  registerAuthTools(server, api);
  registerHealthTools(server, api);
  registerDashboardTools(server, api);
  registerEquipmentTools(server, api);
  registerSensorTools(server, api);
  registerWidgetTools(server, api);
}
