import { readFile } from "node:fs/promises";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";
import type { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { toToolText } from "./utils.js";

const currentDir = dirname(fileURLToPath(import.meta.url));
const protocolPath = join(currentDir, "../../config/api-protocol.json");

export function registerProtocolTools(server: McpServer): void {
  server.tool(
    "get_mcp_api_protocol",
    "MCP가 사용할 수 있는 Backend API 범위, tool별 위험도, 확인 필요 여부, dryRun 정책을 조회합니다.",
    {},
    async () => {
      const protocol = JSON.parse(await readFile(protocolPath, "utf8")) as unknown;
      return toToolText(protocol);
    }
  );
}
