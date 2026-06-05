import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} 환경변수가 필요합니다.`);
  }
  return value;
}

function optionalInt(name: string, fallback: number): number {
  const value = process.env[name];
  if (!value) {
    return fallback;
  }

  const parsed = Number.parseInt(value, 10);
  if (!Number.isFinite(parsed)) {
    throw new Error(`${name}는 숫자여야 합니다.`);
  }
  return parsed;
}

function optionalBoolean(name: string, fallback: boolean): boolean {
  const value = process.env[name];
  if (!value) {
    return fallback;
  }
  return value.toLowerCase() === "true";
}

function parseToolText(result: Awaited<ReturnType<Client["callTool"]>>): unknown {
  const content = (result as { content?: Array<{ type: string; text?: string }> }).content;
  const first = content?.[0];
  if (!first || first.type !== "text") {
    return result;
  }

  try {
    return JSON.parse(first.text ?? "");
  } catch {
    return first.text ?? "";
  }
}

async function callTool(client: Client, name: string, args: Record<string, unknown> = {}) {
  console.log(`\n--- ${name} ---`);
  const result = await client.callTool({ name, arguments: args });
  const parsed = parseToolText(result);
  console.log(JSON.stringify(parsed, null, 2));
  return parsed;
}

const username = requireEnv("MCP_TEST_USERNAME");
const password = requireEnv("MCP_TEST_PASSWORD");
const equipmentEntityId = optionalInt("MCP_WIDGET_EQUIPMENT_ENTITY_ID", 9);
const dryRun = optionalBoolean("MCP_WIDGET_DRY_RUN", true);
const skipExisting = optionalBoolean("MCP_WIDGET_SKIP_EXISTING", true);
const startX = optionalInt("MCP_WIDGET_START_X", 0);
const startY = optionalInt("MCP_WIDGET_START_Y", 0);
const columns = optionalInt("MCP_WIDGET_COLUMNS", 12);

const transport = new StdioClientTransport({
  command: "npm",
  args: ["run", "dev", "--silent"],
  cwd: process.cwd(),
  env: {
    ...process.env,
    DASHBOARD_API_BASE_URL:
      process.env.DASHBOARD_API_BASE_URL ?? "https://api.43.201.141.9.nip.io",
    DASHBOARD_TLS_REJECT_UNAUTHORIZED:
      process.env.DASHBOARD_TLS_REJECT_UNAUTHORIZED ?? "false"
  },
  stderr: "pipe"
});

transport.stderr?.on("data", (chunk) => {
  process.stderr.write(`[mcp-server] ${chunk}`);
});

const client = new Client({
  name: "capstone-dashboard-mcp-widget-test",
  version: "0.1.0"
});

try {
  await client.connect(transport);

  await callTool(client, "login", { username, password });
  await callTool(client, "get_current_equipment", { equipmentEntityId });
  await callTool(client, "auto_create_widgets_for_equipment", {
    equipmentEntityId,
    startX,
    startY,
    columns,
    skipExisting,
    dryRun
  });

  if (dryRun) {
    console.log("\nDry run 완료: 실제 위젯은 생성되지 않았습니다.");
    console.log("실제 생성하려면 MCP_WIDGET_DRY_RUN=false 를 명시해서 다시 실행하세요.");
  } else {
    console.log("\n위젯 생성 완료: 프론트 대시보드에서 결과를 확인하세요.");
  }
} finally {
  await client.close();
}
