import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { DashboardApiClient } from "./client/dashboardApi.js";
import { loadConfig } from "./config/env.js";
import { registerTools } from "./tools/index.js";

const config = loadConfig();
const api = new DashboardApiClient(config);

const server = new McpServer({
  name: "capstone-dashboard-mcp-server",
  version: "0.1.0"
});

registerTools(server, api);

const transport = new StdioServerTransport();
await server.connect(transport);
