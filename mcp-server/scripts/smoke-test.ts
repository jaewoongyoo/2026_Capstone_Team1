import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

function requireEnv(name: string): string {
  const value = process.env[name];
  if (!value) {
    throw new Error(`${name} 환경변수가 필요합니다.`);
  }
  return value;
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
  name: "capstone-dashboard-mcp-smoke-test",
  version: "0.1.0"
});

try {
  await client.connect(transport);

  const tools = await client.listTools();
  console.log("등록된 MCP tools:");
  console.log(tools.tools.map((tool) => tool.name).sort().join("\n"));

  await callTool(client, "check_backend_health");
  await callTool(client, "login", { username, password });
  await callTool(client, "get_me");
  await callTool(client, "get_dashboards");
  await callTool(client, "get_current_equipment_all");
  await callTool(client, "check_recent_telemetry", { maxAgeSeconds: 120 });
  await callTool(client, "generate_setup_summary", { maxTelemetryAgeSeconds: 120 });

  console.log("\nMCP smoke test 완료");
} finally {
  await client.close();
}
