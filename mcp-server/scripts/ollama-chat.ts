import { createInterface } from "node:readline/promises";
import { stdin as input, stdout as output } from "node:process";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

type ToolCall = {
  name: string;
  args: Record<string, unknown>;
  description: string;
  mutates?: boolean;
};

type WidgetVisualization = {
  label: string;
  widgetType: string;
  chartType: string;
  width: number;
  height: number;
  aliases: string[];
};

type PlannedWidget = {
  equipment: EquipmentSnapshot;
  sensor: SensorSnapshot;
  visualization: WidgetVisualization;
  request: Record<string, unknown>;
};

type WidgetSnapshot = {
  id?: number;
  widgetId?: number;
  dashboardId?: number;
  equipmentId?: string;
  equipmentName?: string;
  equipmentEntityId?: number;
  widgetType: string;
  title: string;
  sensorId?: string;
  sensorName?: string;
  chartType?: string;
  dataType?: string;
  unit?: string;
  posX: number;
  posY: number;
  width: number;
  height: number;
  configJson?: string;
};

type DashboardSnapshot = {
  dashboardId: number;
  dashboardName: string;
  description?: string;
  isPublic?: boolean;
};

type PlannedOperation = {
  title: string;
  dryRun: boolean;
  calls: ToolCall[];
  lines: string[];
};

type WidgetLayoutPlan = {
  widget: WidgetSnapshot;
  layout: {
    widgetId: number;
    posX: number;
    posY: number;
    width: number;
    height: number;
  };
  group: string;
};

type EquipmentSnapshot = {
  equipmentId: number;
  equipmentName: string;
  field?: string;
  dashboardId?: number;
  current?: {
    equipmentEntityId?: number;
    equipmentId?: string;
    timestamp?: string;
    status?: string;
    sensors?: Array<{
      sensorId: string;
      dataType: string;
      value: unknown;
      unit?: string;
    }>;
  };
};

type SensorSnapshot = {
  sensorId: string;
  dataType: string;
  value: unknown;
  unit?: string;
};

type PlannedAction =
  | { kind: "tool"; call: ToolCall }
  | { kind: "multiTool"; calls: ToolCall[]; plan: PlannedWidget[]; dryRun: boolean }
  | { kind: "operation"; operation: PlannedOperation }
  | { kind: "message"; message: string };

const SUPPORTED_WIDGET_VISUALIZATIONS: WidgetVisualization[] = [
  {
    label: "게이지",
    widgetType: "GAUGE",
    chartType: "line",
    width: 3,
    height: 3,
    aliases: ["게이지", "gauge"]
  },
  {
    label: "선 그래프",
    widgetType: "TREND",
    chartType: "line",
    width: 6,
    height: 3,
    aliases: ["선그래프", "선 그래프", "라인", "라인그래프", "line", "trend", "추이"]
  },
  {
    label: "막대그래프",
    widgetType: "BAR_V",
    chartType: "bar",
    width: 4,
    height: 3,
    aliases: ["막대", "막대그래프", "세로막대", "바그래프", "bar", "bar_v"]
  },
  {
    label: "가로 막대그래프",
    widgetType: "BAR_H",
    chartType: "bar-horizontal",
    width: 4,
    height: 3,
    aliases: ["가로막대", "가로 막대", "horizontal bar", "bar_h"]
  },
  {
    label: "도넛 그래프",
    widgetType: "DONUT",
    chartType: "donut",
    width: 3,
    height: 3,
    aliases: ["도넛", "도넛그래프", "도넛 그래프", "donut"]
  },
  {
    label: "상태 위젯",
    widgetType: "STATUS",
    chartType: "status",
    width: 3,
    height: 3,
    aliases: ["상태", "상태위젯", "상태 위젯", "status"]
  },
  {
    label: "로그 위젯",
    widgetType: "LOG",
    chartType: "log",
    width: 6,
    height: 3,
    aliases: ["로그", "로그위젯", "로그 위젯", "log"]
  },
  {
    label: "알림 위젯",
    widgetType: "ALERTS",
    chartType: "alerts",
    width: 6,
    height: 3,
    aliases: ["알림", "경고", "알람", "alerts", "alert"]
  },
  {
    label: "센서 목록 위젯",
    widgetType: "SENSORS",
    chartType: "sensors",
    width: 6,
    height: 3,
    aliases: ["센서목록", "센서 목록", "sensors"]
  },
  {
    label: "OEE 위젯",
    widgetType: "OEE",
    chartType: "oee",
    width: 6,
    height: 3,
    aliases: ["oee", "종합효율", "설비종합효율"]
  }
];

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

async function callTool(client: Client, call: ToolCall): Promise<unknown> {
  console.log(`\n[MCP] ${call.name} 호출: ${call.description}`);
  const result = await client.callTool({ name: call.name, arguments: call.args });
  return parseToolText(result);
}

async function callToolWithAuthRetry(
  client: Client,
  call: ToolCall,
  credentials: { username: string; password: string }
): Promise<unknown> {
  try {
    return await callTool(client, call);
  } catch (error) {
    if (!isAuthError(error)) {
      throw error;
    }

    console.log("\n[MCP] 인증이 만료되어 다시 로그인한 뒤 재시도합니다.");
    await callTool(client, {
      name: "login",
      args: credentials,
      description: "만료된 JWT 갱신을 위한 재로그인"
    });
    return await callTool(client, call);
  }
}

function isAuthError(error: unknown): boolean {
  const message = error instanceof Error ? error.message : String(error);
  return message.includes("401") || message.toLowerCase().includes("jwt");
}

async function summarizeWithOllama(inputText: string, toolResult: unknown): Promise<string> {
  const deterministicSummary = buildDeterministicSummary(toolResult);
  if (deterministicSummary) {
    return deterministicSummary;
  }

  const baseUrl = (process.env.OLLAMA_BASE_URL ?? "http://localhost:11434").replace(/\/$/, "");
  const model = process.env.OLLAMA_MODEL ?? "qwen2.5:3b";

  const prompt = [
    "너는 캡스톤 장비 대시보드 세팅을 돕는 한국어 AI 어시스턴트야.",
    "반드시 한국어로만 답변해. 중국어, 영어 문장을 섞지 마.",
    "아래 사용자의 요청과 MCP tool 실행 결과를 보고 중요한 내용만 간결하게 설명해.",
    "센서명, 장비명, 단위, dataType은 번역하거나 의역하지 말고 JSON의 원문 그대로 써.",
    "괄호 안에 임의 설명을 추가하지 마. 예: Chamber_Pressure를 부정압 습도처럼 해석하지 마.",
    "목록 개수는 JSON 배열 길이 또는 규칙 기반 사전 해석에 있는 값만 사용해.",
    "dryRun이 true이면 실제 DB 변경이 없었다고 말해.",
    "dryRun이 false여도 createdCount가 0이면 새로 생성된 위젯은 없다고 말해.",
    "createdCount가 1 이상이면 실제 생성된 위젯 수를 분명히 말해.",
    "skippedCount가 있으면 이미 존재해서 제외된 수라고 설명해.",
    "추측하지 말고 JSON에 있는 값만 근거로 설명해.",
    "",
    `사용자 요청: ${inputText}`,
    "",
    "규칙 기반 사전 해석:",
    buildDeterministicNote(toolResult),
    "",
    "MCP 결과 JSON:",
    JSON.stringify(toolResult, null, 2)
  ].join("\n");

  const response = await fetch(`${baseUrl}/api/generate`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ model, prompt, stream: false })
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`Ollama 요청 실패 (${response.status}): ${text}`);
  }

  const data = (await response.json()) as { response?: string };
  return data.response?.trim() ?? "";
}

function buildDeterministicSummary(toolResult: unknown): string | null {
  if (!toolResult || typeof toolResult !== "object") {
    return null;
  }

  const result = toolResult as Record<string, unknown>;

  if (
    "totals" in result &&
    "equipment" in result &&
    "recommendations" in result
  ) {
    const totals = result.totals as Record<string, unknown>;
    const equipment = Array.isArray(result.equipment) ? result.equipment : [];
    const recommendations = Array.isArray(result.recommendations)
      ? result.recommendations
      : [];

    const lines = [
      "현재 대시보드 세팅 상태 점검 결과입니다.",
      "",
      `- 대시보드 수: ${totals.dashboards ?? 0}개`,
      `- 장비 수: ${totals.equipment ?? 0}개`,
      `- 위젯 수: ${totals.widgets ?? 0}개`,
      `- 최신 telemetry 장비: ${totals.freshEquipment ?? 0}개`,
      `- 오래된 telemetry 장비: ${totals.staleEquipment ?? 0}개`,
      "",
      "장비별 상태:"
    ];

    for (const item of equipment) {
      if (!item || typeof item !== "object") {
        continue;
      }
      const eq = item as Record<string, unknown>;
      lines.push(...formatEquipmentStatus(eq));
    }

    lines.push("", "권장 조치:");
    for (const recommendation of recommendations) {
      lines.push(`- ${String(recommendation)}`);
    }

    return lines.join("\n");
  }

  if ("current" in result && result.current && typeof result.current === "object") {
    const current = result.current as Record<string, unknown>;
    const sensors = Array.isArray(current.sensors) ? current.sensors : [];
    const equipmentName =
      typeof result.equipmentName === "string" ? result.equipmentName : "장비";

    const lines = [
      `${equipmentName} 최신 상태입니다.`,
      `- 상태: ${current.status ?? "unknown"}`,
      `- timestamp: ${current.timestamp ?? "unknown"}`,
      `- 센서 수: ${sensors.length}개`,
      "",
      "센서 목록:"
    ];

    for (const sensor of sensors) {
      if (!sensor || typeof sensor !== "object") {
        continue;
      }
      const s = sensor as Record<string, unknown>;
      lines.push(
        `- ${s.sensorId}: dataType=${s.dataType}, value=${String(s.value)}, unit=${s.unit ?? ""}`
      );
    }

    return lines.join("\n");
  }

  if ("dryRun" in result) {
    const dryRun = result.dryRun === true;
    const plannedWidgets = Array.isArray(result.plannedWidgets) ? result.plannedWidgets : [];
    const created = Array.isArray(result.created) ? result.created : [];
    const createdCount = typeof result.createdCount === "number" ? result.createdCount : created.length;
    const skippedCount = typeof result.skippedCount === "number" ? result.skippedCount : 0;

    if (dryRun) {
      const lines = [
        "자동 위젯 구성 계획입니다.",
        "- dryRun=true 이므로 실제 DB 변경은 없습니다.",
        `- 생성 예정 위젯 수: ${plannedWidgets.length}개`,
        "",
        "생성 예정 위젯:"
      ];
      for (const widget of plannedWidgets) {
        if (!widget || typeof widget !== "object") {
          continue;
        }
        const w = widget as Record<string, unknown>;
        lines.push(`- ${w.title}: 센서=${w.sensorId}, 표시 방식=${formatWidgetTypeLabel(String(w.widgetType ?? ""))}`);
      }
      return lines.join("\n");
    }

    return [
      "자동 위젯 구성을 실제 생성 모드로 실행했습니다.",
      `- 새로 생성된 위젯 수: ${createdCount}개`,
      `- 제외된 항목 수: ${skippedCount}개`,
      skippedCount > 0 ? "- 제외된 항목은 보통 이미 같은 센서 위젯이 존재해서 skipExisting 정책에 의해 건너뛴 것입니다." : ""
    ]
      .filter(Boolean)
      .join("\n");
  }

  if (Array.isArray(toolResult)) {
    if (toolResult.every(isSupportedWidgetTypeLike)) {
      const lines = ["MCP가 생성할 수 있는 위젯/그래프 종류입니다.", ""];
      for (const item of toolResult) {
        lines.push(
          `- ${String(item.label)}: ${String(item.useCase ?? "")}`
        );
      }
      return lines.join("\n");
    }

    if (toolResult.every(isEquipmentSnapshotLike)) {
      const lines = ["장비 목록입니다.", ""];
      for (const item of toolResult) {
        lines.push(
          `- ${item.equipmentName}: equipmentEntityId=${item.equipmentId}, dashboardId=${item.dashboardId ?? ""}, field=${item.field ?? ""}, status=${item.current?.status ?? "unknown"}, sensorCount=${item.current?.sensors?.length ?? 0}`
        );
      }
      return lines.join("\n");
    }

    if (toolResult.every(isWidgetLike)) {
      const lines = ["위젯 목록입니다.", ""];
      for (const item of toolResult) {
        lines.push(
          `- ${String(item.title)}: widgetId=${String(item.id)}, 장비=${String(item.equipmentName ?? item.equipmentId ?? "")}, 센서=${String(item.sensorName ?? item.sensorId ?? "")}, 표시 방식=${formatWidgetTypeLabel(String(item.widgetType ?? ""))}`
        );
      }
      return lines.join("\n");
    }

    return `조회 결과 ${toolResult.length}개입니다.`;
  }

  return null;
}

function formatEquipmentStatus(eq: Record<string, unknown>): string[] {
  const widgetCount = typeof eq.widgetCount === "number" ? eq.widgetCount : 0;
  const telemetryFresh = eq.telemetryFresh === true;

  return [
    `- ${eq.equipmentName} (대시보드 ${eq.dashboardId ?? "-"} / 장비 ${eq.equipmentEntityId ?? "-"})`,
    `  상태: ${eq.status ?? "unknown"}`,
    `  센서: ${eq.sensorCount ?? 0}개`,
    `  위젯: ${widgetCount > 0 ? `${widgetCount}개` : "없음"}`,
    `  데이터 수신: ${telemetryFresh ? "정상" : "오래됨"}`,
    ""
  ];
}

function isEquipmentSnapshotLike(value: unknown): value is EquipmentSnapshot {
  return (
    !!value &&
    typeof value === "object" &&
    "equipmentId" in value &&
    "equipmentName" in value &&
    "current" in value
  );
}

function isWidgetLike(value: unknown): value is Record<string, unknown> {
  return (
    !!value &&
    typeof value === "object" &&
    "widgetType" in value &&
    "title" in value
  );
}

function isSupportedWidgetTypeLike(value: unknown): value is Record<string, unknown> {
  return (
    !!value &&
    typeof value === "object" &&
    "widgetType" in value &&
    "chartType" in value &&
    "label" in value
  );
}

function buildDeterministicNote(toolResult: unknown): string {
  if (!toolResult || typeof toolResult !== "object") {
    return "구조화된 MCP 결과가 없습니다.";
  }

  const result = toolResult as Record<string, unknown>;

  if ("dryRun" in result) {
    const dryRun = result.dryRun === true;
    const plannedCount = typeof result.plannedCount === "number" ? result.plannedCount : undefined;
    const createdCount = typeof result.createdCount === "number" ? result.createdCount : undefined;
    const skippedCount = typeof result.skippedCount === "number" ? result.skippedCount : undefined;

    if (dryRun) {
      return `dryRun=true입니다. 실제 DB 변경은 없고 생성 계획 ${plannedCount ?? 0}개만 확인했습니다.`;
    }

    return [
      "dryRun=false입니다. 실제 생성 모드로 실행되었습니다.",
      `createdCount=${createdCount ?? 0}개입니다.`,
      skippedCount !== undefined ? `skippedCount=${skippedCount}개이며, 보통 이미 존재해서 제외된 항목입니다.` : ""
    ]
      .filter(Boolean)
      .join(" ");
  }

  if ("current" in result && result.current && typeof result.current === "object") {
    const current = result.current as Record<string, unknown>;
    const sensors = Array.isArray(current.sensors) ? current.sensors : [];
    const equipmentName =
      typeof result.equipmentName === "string" ? result.equipmentName : "장비";
    return `${equipmentName}의 최신 스냅샷 기준 센서는 ${sensors.length}개입니다. 센서명은 번역하지 말고 sensorId 원문 그대로 표시하세요.`;
  }

  if (Array.isArray(result)) {
    return `배열 결과 ${result.length}개입니다. 각 항목의 필드명과 값은 JSON 원문을 기준으로만 설명하세요.`;
  }

  return "일반 조회 또는 점검 결과입니다.";
}

async function planAction(client: Client, text: string): Promise<PlannedAction> {
  const normalized = text.toLowerCase();

  if (containsAny(normalized, ["상태 점검", "세팅 상태", "설치 상태", "점검", "리포트"])) {
    return {
      kind: "tool",
      call: {
        name: "generate_setup_summary",
        args: { maxTelemetryAgeSeconds: 120 },
        description: "대시보드 설치 및 운영 상태 요약"
      }
    };
  }

  if (containsAny(normalized, ["장비 목록", "장비 보여", "장비 조회"])) {
    return {
      kind: "tool",
      call: {
        name: "get_current_equipment_all",
        args: {},
        description: "전체 장비 목록과 최신 상태 조회"
      }
    };
  }

  if (containsAny(normalized, ["그래프 종류", "위젯 종류", "지원 위젯", "지원하는 위젯", "지원 그래프", "지원하는 그래프", "시각화 종류", "사용 가능한 위젯", "생성 가능한 위젯"])) {
    return {
      kind: "tool",
      call: {
        name: "get_supported_widget_types",
        args: {},
        description: "MCP가 생성 가능한 위젯/그래프 타입 목록 조회"
      }
    };
  }

  if (containsAny(normalized, ["위젯 목록", "위젯 보여", "위젯을 보여", "위젯 조회"])) {
    return {
      kind: "tool",
      call: {
        name: "get_widgets",
        args: {},
        description: "전체 위젯 목록 조회"
      }
    };
  }

  const requestedEquipmentEntityId = extractEquipmentEntityId(normalized);
  const requestedDashboardId = extractDashboardId(normalized);
  if (
    requestedEquipmentEntityId !== null &&
    containsAny(normalized, ["현재 상태", "장비 상태", "상태 확인", "센서 목록", "센서 보여", "센서 조회"])
  ) {
    return {
      kind: "tool",
      call: {
        name: "get_current_equipment",
        args: { equipmentEntityId: requestedEquipmentEntityId },
        description: `equipmentEntityId=${requestedEquipmentEntityId} 장비 최신 스냅샷 조회`
      }
    };
  }

  const sensorKeyword = inferSensorKeyword(normalized);
  const equipmentSnapshots = await getEquipmentSnapshots(client);
  const dashboardId = await resolveDashboardIdForText(client, normalized, false);
  const equipmentMatch = matchEquipment(normalized, equipmentSnapshots, {
    equipmentEntityId: requestedEquipmentEntityId,
    dashboardId: dashboardId ?? requestedDashboardId
  });

  const widgetMutationAction = await planWidgetMutationAction(client, text, normalized, equipmentSnapshots, dashboardId ?? requestedDashboardId);
  if (widgetMutationAction) {
    return widgetMutationAction;
  }

  const customWidgetAction = planCustomWidgetAction(text, normalized, equipmentSnapshots, dashboardId ?? requestedDashboardId);
  if (customWidgetAction) {
    return customWidgetAction;
  }

  if (equipmentMatch.kind === "ambiguous") {
    return { kind: "message", message: equipmentMatch.message };
  }

  if (containsAny(normalized, ["현재 상태", "장비 상태", "상태 확인"])) {
    if (equipmentMatch.kind === "matched") {
      return {
        kind: "tool",
        call: {
          name: "get_current_equipment",
          args: { equipmentEntityId: equipmentMatch.equipment.equipmentId },
          description: `${equipmentMatch.equipment.equipmentName} 최신 상태 조회`
        }
      };
    }
    return {
      kind: "tool",
      call: {
        name: "get_current_equipment_all",
        args: {},
        description: "전체 장비 최신 상태 조회"
      }
    };
  }

  if (containsAny(normalized, ["센서 목록", "센서 보여", "센서 조회"])) {
    if (equipmentMatch.kind === "matched") {
      return {
        kind: "tool",
        call: {
          name: "get_current_equipment",
          args: { equipmentEntityId: equipmentMatch.equipment.equipmentId },
          description: `${equipmentMatch.equipment.equipmentName} 최신 스냅샷 기준 센서 목록 조회`
        }
      };
    }
    return {
      kind: "message",
      message: buildEquipmentSelectionMessage("어느 장비의 센서 목록을 볼까요?", equipmentSnapshots)
    };
  }

  if (containsAny(normalized, ["위젯 구성", "위젯 생성", "위젯 만들어", "핵심 센서"])) {
    if (equipmentMatch.kind !== "matched") {
      return {
        kind: "message",
        message: buildEquipmentSelectionMessage("어느 장비의 위젯을 구성할까요?", equipmentSnapshots)
      };
    }

    const explicitlyDryRun = containsAny(normalized, ["계획만", "dryrun", "dry run", "실제 생성하지", "미리"]);
    const wantsCreate = containsAny(normalized, ["실제 생성", "생성해", "만들어줘", "적용해"]);
    const dryRun = explicitlyDryRun || !wantsCreate;

    return {
      kind: "tool",
      call: {
        name: "auto_create_widgets_for_equipment",
        args: { equipmentEntityId: equipmentMatch.equipment.equipmentId, dryRun, skipExisting: true },
        description: `${equipmentMatch.equipment.equipmentName} 자동 위젯 구성 ${dryRun ? "계획 확인" : "실제 생성"}`,
        mutates: !dryRun
      }
    };
  }

  if (sensorKeyword) {
    const matches = findSensorMatches(sensorKeyword, equipmentSnapshots);
    if (matches.length === 0) {
      return {
        kind: "message",
        message: `${sensorKeyword.label}에 해당하는 센서를 찾지 못했습니다. 장비/센서 목록을 먼저 확인해주세요.`
      };
    }

    return {
      kind: "message",
      message: [
        `${sensorKeyword.label} 관련 센서 후보입니다.`,
        ...matches.map(
          (match) =>
            `- ${match.equipment.equipmentName} (equipmentEntityId=${match.equipment.equipmentId}, dashboardId=${match.equipment.dashboardId ?? ""}) / ${match.sensor.sensorId} (${match.sensor.dataType}, unit=${match.sensor.unit ?? ""})`
        ),
        "위젯을 만들려면 장비명을 포함해서 다시 요청해주세요. 예: CVD 장비 압력 센서 위젯 구성 계획만 보여줘"
      ].join("\n")
    };
  }

  return {
    kind: "message",
    message: "지원하는 요청으로 해석하지 못했어요. 장비/센서/상태/위젯/점검 관련 문장으로 다시 입력해줘."
  };
}

async function getEquipmentSnapshots(client: Client): Promise<EquipmentSnapshot[]> {
  const result = await callTool(client, {
    name: "get_current_equipment_all",
    args: {},
    description: "동적 장비 매칭을 위한 전체 장비 조회"
  });

  return Array.isArray(result) ? (result as EquipmentSnapshot[]) : [];
}

async function getWidgetSnapshots(client: Client): Promise<WidgetSnapshot[]> {
  const result = await callTool(client, {
    name: "get_widgets",
    args: {},
    description: "위젯 수정/삭제/정렬 대상 조회"
  });

  return Array.isArray(result) ? (result as WidgetSnapshot[]) : [];
}

async function getDashboardSnapshots(client: Client): Promise<DashboardSnapshot[]> {
  const result = await callTool(client, {
    name: "get_dashboards",
    args: {},
    description: "대시보드 이름 기반 대상 매칭"
  });

  return Array.isArray(result) ? (result as DashboardSnapshot[]) : [];
}

async function resolveDashboardIdForText(
  client: Client,
  normalized: string,
  useDefaultWhenMissing: boolean
): Promise<number | null> {
  const explicitDashboardId = extractDashboardId(normalized);
  if (explicitDashboardId !== null) {
    return explicitDashboardId;
  }

  if (!containsAny(normalized, ["대시보드", "dashboard"])) {
    return null;
  }

  const dashboards = await getDashboardSnapshots(client);
  const match = resolveDashboard(normalized, dashboards, useDefaultWhenMissing);
  if (match.kind === "ambiguous") {
    return null;
  }

  return match.dashboard?.dashboardId ?? null;
}

async function planWidgetMutationAction(
  client: Client,
  text: string,
  normalized: string,
  equipmentSnapshots: EquipmentSnapshot[],
  resolvedDashboardId: number | null
): Promise<PlannedAction | null> {
  const isDelete = containsAny(normalized, ["삭제", "지워", "제거"]);
  const isUpdate = containsAny(normalized, ["수정", "변경", "바꿔", "바꾸", "교체"]);
  const isLayout = containsAny(normalized, ["정렬", "위치", "첫 번째 줄", "첫번째 줄", "첫 줄", "한 줄"]);

  if (!containsAny(normalized, ["위젯"]) && !isLayout) {
    return null;
  }
  if (!isDelete && !isUpdate && !isLayout) {
    return null;
  }

  const widgets = await getWidgetSnapshots(client);
  const dashboards = await getDashboardSnapshots(client);
  const dashboardMatch = resolvedDashboardId !== null
    ? { kind: "matched" as const, dashboard: dashboards.find((dashboard) => dashboard.dashboardId === resolvedDashboardId) ?? null }
    : resolveDashboard(normalized, dashboards, isLayout);
  if (dashboardMatch.kind === "ambiguous") {
    return { kind: "message", message: dashboardMatch.message };
  }
  const dashboardId = dashboardMatch.dashboard?.dashboardId ?? extractDashboardId(normalized);
  const equipmentEntityId = extractEquipmentEntityId(normalized);

  if (isLayout) {
    const targets = filterWidgets(normalized, widgets, equipmentSnapshots, { dashboardId, equipmentEntityId });
    if (targets.kind !== "matched") {
      return { kind: "message", message: targets.message };
    }

    const plannedLayouts = buildSmartWidgetLayout(targets.widgets);
    const layouts = plannedLayouts.map((item) => item.layout);

    return {
      kind: "operation",
      operation: {
        title: "위젯 스마트 정렬 계획입니다.",
        dryRun: containsAny(normalized, ["계획만", "미리", "실제 변경하지", "dryrun", "dry run"]),
        calls: [
          {
            name: "update_widget_layouts",
            args: { layouts },
            description: "여러 위젯 위치 일괄 정렬",
            mutates: true
          }
        ],
        lines: [
          dashboardMatch.dashboard
            ? `대상 대시보드: ${dashboardMatch.dashboard.dashboardName}`
            : "대상 대시보드: 전체 조건 기준",
          "정렬 정책: 숫자형 위젯 상단, 상태 위젯 중간, 로그/알림 위젯 하단",
          `대상 위젯: ${plannedLayouts.length}개`,
          ...plannedLayouts.map(
            ({ widget, layout, group }) =>
              `- [${group}] ${formatWidgetTarget(widget)} -> 위치 (${layout.posX}, ${layout.posY}), 크기 ${layout.width}x${layout.height}`
          )
        ]
      }
    };
  }

  const targets = filterWidgets(normalized, widgets, equipmentSnapshots, { dashboardId, equipmentEntityId });
  if (targets.kind !== "matched") {
    return { kind: "message", message: targets.message };
  }

  if (isDelete) {
    return {
      kind: "operation",
      operation: {
        title: "위젯 삭제 계획입니다.",
        dryRun: containsAny(normalized, ["계획만", "미리", "실제 삭제하지", "dryrun", "dry run"]),
        calls: targets.widgets.map((widget) => ({
          name: "delete_widget",
          args: { widgetId: getWidgetId(widget) },
          description: `${formatWidgetTarget(widget)} 삭제`,
          mutates: true
        })),
        lines: targets.widgets.map((widget) => `- 삭제 대상: ${formatWidgetTarget(widget)}`)
      }
    };
  }

  const visualization = findTargetVisualization(normalized);
  if (!visualization) {
    return {
      kind: "message",
      message: "어떤 표시 방식으로 바꿀지 찾지 못했습니다. 예: 게이지에서 막대그래프로 바꿔줘"
    };
  }

  const sourceVisualization = findSourceVisualization(normalized);
  const targetWidgets = sourceVisualization
    ? targets.widgets.filter((widget) => widget.widgetType === sourceVisualization.widgetType)
    : targets.widgets;

  if (targetWidgets.length === 0) {
    return {
      kind: "message",
      message: `${sourceVisualization?.label ?? "요청한 표시 방식"}에 해당하는 위젯을 찾지 못했습니다. 위젯 목록을 먼저 확인해주세요.`
    };
  }

  if (
    targetWidgets.length > 1 &&
    !containsAny(normalized, ["전체", "모든"]) &&
    extractWidgetId(normalized) === null
  ) {
    return {
      kind: "message",
      message: [
        "여러 위젯이 매칭되었습니다. 더 구체적으로 지정해주세요.",
        ...targetWidgets.slice(0, 10).map((widget) => `- ${formatWidgetTarget(widget)}`)
      ].join("\n")
    };
  }

  return {
    kind: "operation",
    operation: {
      title: "위젯 표시 방식 수정 계획입니다.",
      dryRun: containsAny(normalized, ["계획만", "미리", "실제 변경하지", "dryrun", "dry run"]),
      calls: targetWidgets.map((widget) => ({
        name: "update_widget",
        args: buildWidgetUpdateRequest(widget, visualization),
        description: `${formatWidgetTarget(widget)} 표시 방식을 ${visualization.label}로 수정`,
        mutates: true
      })),
      lines: targetWidgets.map(
        (widget) =>
          `- ${formatWidgetTarget(widget)}: ${formatWidgetTypeLabel(widget.widgetType)} -> ${visualization.label}`
      )
    }
  };
}

function planCustomWidgetAction(
  text: string,
  normalized: string,
  equipmentSnapshots: EquipmentSnapshot[],
  resolvedDashboardId: number | null
): PlannedAction | null {
  if (containsAny(normalized, ["삭제", "지워", "제거", "수정", "변경", "바꿔", "바꾸", "교체", "정렬", "위치"])) {
    return null;
  }

  if (!containsAny(normalized, ["그래프", "위젯", "차트", "세팅", "구성", "생성", "만들"])) {
    return null;
  }
  if (!findVisualization(normalized)) {
    return null;
  }

  const clauses = splitWidgetClauses(text).filter((clause) => findVisualization(clause.toLowerCase()));
  if (clauses.length === 0) {
    return null;
  }

  const dashboardId = resolvedDashboardId ?? extractDashboardId(normalized);
  const equipmentEntityId = extractEquipmentEntityId(normalized);
  const planned: PlannedWidget[] = [];
  const errors: string[] = [];

  clauses.forEach((clause, index) => {
    const clauseNormalized = clause.toLowerCase();
    const visualization = findVisualization(clauseNormalized);
    if (!visualization) {
      return;
    }

    const match = matchEquipment(clauseNormalized, equipmentSnapshots, {
      equipmentEntityId: clauses.length === 1 ? equipmentEntityId : extractEquipmentEntityId(clauseNormalized),
      dashboardId
    });

    if (match.kind === "ambiguous") {
      errors.push(`${clause.trim()}: 장비가 여러 개 매칭되었습니다. equipmentEntityId 또는 dashboardId를 포함해주세요.`);
      return;
    }

    if (match.kind !== "matched") {
      errors.push(`${clause.trim()}: 장비를 찾지 못했습니다.`);
      return;
    }

    const sensor = matchSensor(clauseNormalized, match.equipment.current?.sensors ?? []);
    if (!sensor) {
      errors.push(
        `${clause.trim()}: 센서를 찾지 못했습니다. 사용 가능한 센서: ${(match.equipment.current?.sensors ?? [])
          .map((item) => item.sensorId)
          .join(", ")}`
      );
      return;
    }

    const request = buildCustomWidgetRequest({
      equipment: match.equipment,
      sensor,
      visualization,
      index
    });

    planned.push({ equipment: match.equipment, sensor, visualization, request });
  });

  if (errors.length > 0) {
    return { kind: "message", message: ["커스텀 위젯 요청을 처리하지 못한 항목이 있습니다.", ...errors].join("\n") };
  }

  if (planned.length === 0) {
    return null;
  }

  const dryRun = containsAny(normalized, ["계획만", "미리", "dryrun", "dry run", "실제 생성하지"]);
  if (dryRun) {
    return { kind: "multiTool", calls: [], plan: planned, dryRun: true };
  }

  return {
    kind: "multiTool",
    dryRun: false,
    plan: planned,
    calls: planned.map((item) => ({
      name: "create_widget",
      args: item.request,
      description: `${item.equipment.equipmentName} ${item.sensor.sensorId} ${item.visualization.label} 위젯 생성`,
      mutates: true
    }))
  };
}

function splitWidgetClauses(text: string): string[] {
  return text
    .split(/\n|그리고|,|，|;|；/g)
    .map((clause) => clause.trim())
    .filter(Boolean);
}

function findVisualization(text: string): WidgetVisualization | null {
  const normalized = text.toLowerCase();
  const loose = normalizeLoose(normalized);
  const candidates = SUPPORTED_WIDGET_VISUALIZATIONS.flatMap((visualization) =>
    visualization.aliases.map((alias) => ({ visualization, alias }))
  ).sort((a, b) => normalizeLoose(b.alias).length - normalizeLoose(a.alias).length);

  const match = candidates.find(({ alias }) => {
    const aliasLower = alias.toLowerCase();
    return normalized.includes(aliasLower) || loose.includes(normalizeLoose(aliasLower));
  });

  return match?.visualization ?? null;
}

function matchSensor(normalized: string, sensors: SensorSnapshot[]): SensorSnapshot | null {
  const loose = normalizeLoose(normalized);
  const exact = sensors.find((sensor) => loose.includes(normalizeLoose(sensor.sensorId)));
  if (exact) {
    return exact;
  }

  const keyword = inferSensorKeyword(normalized);
  if (!keyword) {
    return null;
  }

  return (
    sensors.find((sensor) =>
      keyword.aliases.some((alias) => normalizeLoose(sensor.sensorId).includes(normalizeLoose(alias)))
    ) ?? null
  );
}

function buildCustomWidgetRequest(input: {
  equipment: EquipmentSnapshot;
  sensor: SensorSnapshot;
  visualization: WidgetVisualization;
  index: number;
}): Record<string, unknown> {
  const width = normalizeWidgetWidth(input.visualization, input.sensor);
  const height = input.visualization.height;
  const xSlots = Math.max(1, Math.floor(12 / width));
  const posX = (input.index % xSlots) * width;
  const posY = Math.floor(input.index / xSlots) * height;
  const equipmentEntityId = input.equipment.equipmentId;
  const equipmentName = input.equipment.equipmentName;

  return {
    dashboardId: input.equipment.dashboardId,
    equipmentId: equipmentName,
    equipmentEntityId,
    widgetType: normalizeWidgetTypeForSensor(input.visualization, input.sensor),
    title: `${equipmentName} ${input.sensor.sensorId}`,
    sensorId: input.sensor.sensorId,
    chartType: normalizeChartTypeForSensor(input.visualization, input.sensor),
    dataType: input.sensor.dataType,
    unit: input.sensor.unit ?? "",
    posX,
    posY,
    width,
    height,
    configJson: JSON.stringify({
      dataKey: input.sensor.sensorId,
      equipmentId: equipmentName,
      equipmentEntityId,
      visualization: input.visualization.label
    })
  };
}

function normalizeWidgetTypeForSensor(visualization: WidgetVisualization, sensor: SensorSnapshot): string {
  const dataType = sensor.dataType.toUpperCase();
  if ((dataType === "BOOLEAN" || dataType === "STRING") && !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)) {
    return "STATUS";
  }
  return visualization.widgetType;
}

function normalizeChartTypeForSensor(visualization: WidgetVisualization, sensor: SensorSnapshot): string {
  const dataType = sensor.dataType.toUpperCase();
  if ((dataType === "BOOLEAN" || dataType === "STRING") && !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)) {
    return "status";
  }
  return visualization.chartType;
}

function normalizeWidgetWidth(visualization: WidgetVisualization, sensor: SensorSnapshot): number {
  const dataType = sensor.dataType.toUpperCase();
  if ((dataType === "BOOLEAN" || dataType === "STRING") && !["LOG", "STATUS", "ALERTS"].includes(visualization.widgetType)) {
    return 3;
  }
  return visualization.width;
}

function summarizeCustomWidgetPlan(plan: PlannedWidget[], dryRun: boolean, createdResults: unknown[] = []): string {
  const lines = [
    dryRun ? "커스텀 위젯 구성 계획입니다." : "커스텀 위젯 생성을 완료했습니다.",
    dryRun ? "- 실제 DB 변경은 없습니다." : `- 생성 요청 수: ${createdResults.length}개`,
    "",
    dryRun ? "생성 예정 위젯:" : "생성한 위젯:"
  ];

  for (const item of plan) {
    lines.push(
      `- ${item.equipment.equipmentName} (대시보드 ${item.equipment.dashboardId ?? "-"} / 장비 ${item.equipment.equipmentId})`,
      `  센서: ${item.sensor.sensorId} (${item.sensor.dataType}, unit=${item.sensor.unit ?? ""})`,
      `  표시 방식: ${item.visualization.label}`
    );
  }

  return lines.join("\n");
}

function formatWidgetTypeLabel(widgetType: string): string {
  const found = SUPPORTED_WIDGET_VISUALIZATIONS.find((item) => item.widgetType === widgetType);
  return found?.label ?? widgetType;
}

function summarizeOperation(operation: PlannedOperation, executedResults: unknown[] = []): string {
  return [
    operation.dryRun ? operation.title : operation.title.replace("계획입니다", "완료했습니다"),
    operation.dryRun ? "- 실제 DB 변경은 없습니다." : `- 실행한 작업 수: ${executedResults.length}개`,
    "",
    ...operation.lines
  ].join("\n");
}

function filterWidgets(
  normalized: string,
  widgets: WidgetSnapshot[],
  equipmentSnapshots: EquipmentSnapshot[],
  filters: { dashboardId: number | null; equipmentEntityId: number | null }
):
  | { kind: "matched"; widgets: WidgetSnapshot[] }
  | { kind: "none"; message: string } {
  const sensorKeyword = inferSensorKeyword(normalized);
  const equipmentMatch = matchEquipment(normalized, equipmentSnapshots, filters);

  let candidates = widgets;
  if (filters.dashboardId !== null) {
    candidates = candidates.filter((widget) => widget.dashboardId === filters.dashboardId);
  }
  if (equipmentMatch.kind === "matched") {
    candidates = candidates.filter((widget) => widgetMatchesEquipment(widget, equipmentMatch.equipment));
  } else if (equipmentMatch.kind === "ambiguous") {
    const ambiguousEquipmentNames = equipmentSnapshots
      .filter((equipment) => scoreEquipment(normalized, equipment) > 0)
      .map((equipment) => equipment.equipmentName);
    candidates = candidates.filter((widget) =>
      ambiguousEquipmentNames.some(
        (equipmentName) => widget.equipmentName === equipmentName || widget.equipmentId === equipmentName
      )
    );
  }
  if (sensorKeyword) {
    candidates = candidates.filter((widget) => widgetMatchesSensorKeyword(widget, sensorKeyword));
  }

  const directWidgetId = extractWidgetId(normalized);
  if (directWidgetId !== null) {
    candidates = candidates.filter((widget) => getWidgetId(widget) === directWidgetId);
  }

  if (candidates.length === 0) {
    return {
      kind: "none",
      message: [
        "조건에 맞는 위젯을 찾지 못했습니다.",
        "위젯 목록을 먼저 확인하거나 dashboardId, equipmentEntityId, 센서명을 더 구체적으로 입력해주세요."
      ].join("\n")
    };
  }

  const canAffectMany =
    containsAny(normalized, ["전체", "모든", "정렬", "위치", "에서"]) || directWidgetId !== null;
  if (candidates.length > 1 && !canAffectMany) {
    return {
      kind: "none",
      message: [
        "여러 위젯이 매칭되었습니다. 더 구체적으로 지정해주세요.",
        ...candidates.slice(0, 10).map((widget) => `- ${formatWidgetTarget(widget)}`)
      ].join("\n")
    };
  }

  return { kind: "matched", widgets: candidates };
}

function resolveDashboard(
  normalized: string,
  dashboards: DashboardSnapshot[],
  useDefaultWhenMissing: boolean
):
  | { kind: "matched"; dashboard: DashboardSnapshot | null }
  | { kind: "ambiguous"; message: string } {
  const dashboardId = extractDashboardId(normalized);
  if (dashboardId !== null) {
    const dashboard = dashboards.find((item) => item.dashboardId === dashboardId);
    return { kind: "matched", dashboard: dashboard ?? null };
  }

  const matches = dashboards
    .map((dashboard) => ({
      dashboard,
      score: scoreDashboard(normalized, dashboard)
    }))
    .filter((entry) => entry.score > 0)
    .sort((a, b) => b.score - a.score);

  if (matches.length > 0) {
    const topScore = matches[0].score;
    const top = matches.filter((entry) => entry.score === topScore);
    if (top.length > 1) {
      return {
        kind: "ambiguous",
        message: [
          "대시보드가 여러 개 매칭되었습니다. 대시보드명을 더 구체적으로 입력해주세요.",
          ...top.map((entry) => `- ${entry.dashboard.dashboardName}`)
        ].join("\n")
      };
    }

    return { kind: "matched", dashboard: top[0].dashboard };
  }

  if (useDefaultWhenMissing && dashboards.length > 0) {
    return { kind: "matched", dashboard: chooseDefaultDashboard(dashboards) };
  }

  return { kind: "matched", dashboard: null };
}

function scoreDashboard(normalized: string, dashboard: DashboardSnapshot): number {
  const looseText = normalizeLoose(normalized);
  const looseName = normalizeLoose(dashboard.dashboardName);
  let score = 0;

  if (looseName.length >= 3 && looseText.includes(looseName)) {
    score += 100;
  }

  for (const token of tokenize(dashboard.dashboardName)) {
    const looseToken = normalizeLoose(token);
    if (token.length >= 2 && (normalized.includes(token) || looseText.includes(looseToken))) {
      score += token.length;
    }
  }

  return score;
}

function chooseDefaultDashboard(dashboards: DashboardSnapshot[]): DashboardSnapshot {
  return dashboards.find((dashboard) => dashboard.isPublic) ?? dashboards[0];
}

function widgetMatchesEquipment(widget: WidgetSnapshot, equipment: EquipmentSnapshot): boolean {
  return (
    widget.equipmentEntityId === equipment.equipmentId ||
    widget.equipmentName === equipment.equipmentName ||
    widget.equipmentId === equipment.equipmentName
  );
}

function widgetMatchesSensorKeyword(
  widget: WidgetSnapshot,
  keyword: { label: string; aliases: string[] }
): boolean {
  const sensorName = String(widget.sensorName ?? widget.sensorId ?? widget.title ?? "").toLowerCase();
  const looseSensor = normalizeLoose(sensorName);
  return keyword.aliases.some((alias) => {
    const looseAlias = normalizeLoose(alias);
    return sensorName.includes(alias.toLowerCase()) || looseSensor.includes(looseAlias);
  });
}

function buildWidgetUpdateRequest(widget: WidgetSnapshot, visualization: WidgetVisualization): Record<string, unknown> {
  const syntheticSensor: SensorSnapshot = {
    sensorId: String(widget.sensorName ?? widget.sensorId ?? ""),
    dataType: String(widget.dataType ?? ""),
    value: null,
    unit: widget.unit
  };

  return {
    widgetId: getWidgetId(widget),
    dashboardId: widget.dashboardId,
    equipmentId: widget.equipmentId ?? widget.equipmentName,
    equipmentEntityId: widget.equipmentEntityId,
    widgetType: normalizeWidgetTypeForSensor(visualization, syntheticSensor),
    title: widget.title,
    sensorId: widget.sensorId ?? widget.sensorName,
    chartType: normalizeChartTypeForSensor(visualization, syntheticSensor),
    dataType: widget.dataType,
    unit: widget.unit ?? "",
    posX: widget.posX,
    posY: widget.posY,
    width: normalizeWidgetWidth(visualization, syntheticSensor),
    height: visualization.height,
    configJson: mergeConfigJson(widget.configJson, {
      visualization: visualization.label
    })
  };
}

function mergeConfigJson(configJson: string | undefined, extra: Record<string, unknown>): string {
  if (!configJson) {
    return JSON.stringify(extra);
  }

  try {
    return JSON.stringify({ ...(JSON.parse(configJson) as Record<string, unknown>), ...extra });
  } catch {
    return JSON.stringify(extra);
  }
}

function formatWidgetTarget(widget: WidgetSnapshot): string {
  return `${widget.title} (widgetId=${getWidgetId(widget)}, 장비=${widget.equipmentName ?? widget.equipmentId ?? ""}, 센서=${widget.sensorName ?? widget.sensorId ?? ""}, 표시 방식=${formatWidgetTypeLabel(widget.widgetType)})`;
}

function buildSmartWidgetLayout(widgets: WidgetSnapshot[]): WidgetLayoutPlan[] {
  const sorted = [...widgets].sort((a, b) => (a.posY - b.posY) || (a.posX - b.posX));
  const numeric = sorted.filter((widget) => getLayoutGroup(widget) === "숫자");
  const status = sorted.filter((widget) => getLayoutGroup(widget) === "상태");
  const log = sorted.filter((widget) => getLayoutGroup(widget) === "로그");

  const plan: WidgetLayoutPlan[] = [];
  let cursorX = 0;
  let cursorY = 0;
  let rowHeight = 3;

  const place = (widget: WidgetSnapshot, group: string, width: number, height: number) => {
    if (cursorX + width > 12) {
      cursorX = 0;
      cursorY += rowHeight;
      rowHeight = height;
    }

    plan.push({
      widget,
      group,
      layout: {
        widgetId: getWidgetId(widget),
        posX: cursorX,
        posY: cursorY,
        width,
        height
      }
    });

    cursorX += width;
    rowHeight = Math.max(rowHeight, height);
  };

  for (const widget of numeric) {
    place(widget, "숫자", getSmartWidth(widget), 3);
  }

  if (status.length > 0) {
    cursorX = 0;
    cursorY += rowHeight;
    rowHeight = 3;
  }
  for (const widget of status) {
    place(widget, "상태", 3, 3);
  }

  if (log.length > 0) {
    cursorX = 0;
    cursorY += rowHeight;
    rowHeight = 3;
  }
  for (const widget of log) {
    place(widget, "로그", 12, Math.max(widget.height ?? 3, 3));
  }

  return plan;
}

function getLayoutGroup(widget: WidgetSnapshot): "숫자" | "상태" | "로그" {
  if (["LOG", "ALERTS"].includes(widget.widgetType)) {
    return "로그";
  }
  if (widget.widgetType === "STATUS" || String(widget.dataType ?? "").toUpperCase() === "BOOLEAN" || String(widget.dataType ?? "").toUpperCase() === "STRING") {
    return "상태";
  }
  return "숫자";
}

function getSmartWidth(widget: WidgetSnapshot): number {
  if (["TREND", "BAR_H", "SENSORS", "OEE"].includes(widget.widgetType)) {
    return 6;
  }
  if (widget.widgetType === "BAR_V") {
    return 4;
  }
  return 3;
}

function getWidgetId(widget: WidgetSnapshot): number {
  return Number(widget.id ?? widget.widgetId);
}

function extractWidgetId(normalized: string): number | null {
  const patterns = [
    /widgetid\s*=?\s*(\d+)/i,
    /widget\s*id\s*=?\s*(\d+)/i,
    /위젯\s*(?:id|아이디)?\s*(\d+)\s*번?/i,
    /(\d+)\s*번\s*위젯/i
  ];

  for (const pattern of patterns) {
    const match = normalized.match(pattern);
    if (match?.[1]) {
      return Number.parseInt(match[1], 10);
    }
  }

  return null;
}

function findTargetVisualization(text: string): WidgetVisualization | null {
  const normalized = text.toLowerCase();
  const loose = normalizeLoose(normalized);
  const candidates = SUPPORTED_WIDGET_VISUALIZATIONS.flatMap((visualization) =>
    visualization.aliases.map((alias) => {
      const aliasLower = alias.toLowerCase();
      const directIndex = normalized.lastIndexOf(aliasLower);
      const looseIndex = loose.lastIndexOf(normalizeLoose(aliasLower));
      return {
        visualization,
        index: Math.max(directIndex, looseIndex)
      };
    })
  )
    .filter((candidate) => candidate.index >= 0)
    .sort((a, b) => b.index - a.index);

  return candidates[0]?.visualization ?? null;
}

function findSourceVisualization(text: string): WidgetVisualization | null {
  const markerIndex = text.indexOf("에서");
  if (markerIndex <= 0) {
    return null;
  }
  return findVisualization(text.slice(0, markerIndex));
}

function matchEquipment(
  normalized: string,
  equipment: EquipmentSnapshot[],
  filters: { equipmentEntityId: number | null; dashboardId: number | null } = {
    equipmentEntityId: null,
    dashboardId: null
  }
):
  | { kind: "matched"; equipment: EquipmentSnapshot }
  | { kind: "none" }
  | { kind: "ambiguous"; message: string } {
  if (filters.equipmentEntityId !== null) {
    const exact = equipment.find((item) => item.equipmentId === filters.equipmentEntityId);
    return exact ? { kind: "matched", equipment: exact } : { kind: "none" };
  }

  const candidates =
    filters.dashboardId !== null
      ? equipment.filter((item) => item.dashboardId === filters.dashboardId)
      : equipment;

  const scored = candidates
    .map((item) => ({ item, score: scoreEquipment(normalized, item) }))
    .filter((entry) => entry.score > 0)
    .sort((a, b) => b.score - a.score);

  if (scored.length === 0) {
    return { kind: "none" };
  }

  const topScore = scored[0]?.score ?? 0;
  const top = scored.filter((entry) => entry.score === topScore);

  if (top.length > 1) {
    return {
      kind: "ambiguous",
      message: [
        "장비가 여러 개 매칭되었습니다. 더 구체적으로 장비명을 입력해주세요.",
        ...top.map((entry) => `- ${entry.item.equipmentName} (equipmentEntityId=${entry.item.equipmentId}, dashboardId=${entry.item.dashboardId ?? ""})`)
      ].join("\n")
    };
  }

  return { kind: "matched", equipment: scored[0].item };
}

function scoreEquipment(normalized: string, equipment: EquipmentSnapshot): number {
  const haystacks = [
    equipment.equipmentName,
    equipment.field ?? "",
    equipment.current?.equipmentId ?? ""
  ].map((value) => value.toLowerCase());

  let score = 0;
  const looseText = normalizeLoose(normalized);
  for (const value of haystacks) {
    const looseValue = normalizeLoose(value);
    if (looseValue.length >= 3 && looseText.includes(looseValue)) {
      score += 50;
    }

    for (const token of tokenize(value)) {
      const looseToken = normalizeLoose(token);
      if (
        token.length >= 2 &&
        (normalized.includes(token) || (looseToken.length >= 2 && looseText.includes(looseToken)))
      ) {
        score += token.length;
      }
    }
  }

  if (normalized.includes(String(equipment.equipmentId))) {
    score += 20;
  }

  return score;
}

function extractEquipmentEntityId(normalized: string): number | null {
  const patterns = [
    /equipmententityid\s*=?\s*(\d+)/i,
    /equipment\s*entity\s*id\s*=?\s*(\d+)/i,
    /장비\s*(?:id|아이디)?\s*(\d+)\s*번?/i,
    /(\d+)\s*번\s*장비/i
  ];

  for (const pattern of patterns) {
    const match = normalized.match(pattern);
    if (match?.[1]) {
      return Number.parseInt(match[1], 10);
    }
  }

  return null;
}

function extractDashboardId(normalized: string): number | null {
  const patterns = [
    /dashboardid\s*=?\s*(\d+)/i,
    /dashboard\s*id\s*=?\s*(\d+)/i,
    /대시보드\s*(?:id|아이디)?\s*(\d+)\s*번?/i,
    /(\d+)\s*번\s*대시보드/i
  ];

  for (const pattern of patterns) {
    const match = normalized.match(pattern);
    if (match?.[1]) {
      return Number.parseInt(match[1], 10);
    }
  }

  return null;
}

function tokenize(value: string): string[] {
  return value
    .split(/[^a-z0-9가-힣]+/i)
    .map((token) => token.trim().toLowerCase())
    .filter(Boolean);
}

function normalizeLoose(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9가-힣]+/gi, "");
}

function buildEquipmentSelectionMessage(title: string, equipment: EquipmentSnapshot[]): string {
  return [
    title,
    ...equipment.map(
      (item) =>
        `- ${item.equipmentName} (equipmentEntityId=${item.equipmentId}, dashboardId=${item.dashboardId ?? ""}, status=${item.current?.status ?? "unknown"})`
    )
  ].join("\n");
}

function inferSensorKeyword(normalized: string): { label: string; aliases: string[] } | null {
  const groups = [
    { label: "압력", aliases: ["압력", "pressure"] },
    { label: "온도", aliases: ["온도", "temp", "temperature", "heater"] },
    { label: "가스", aliases: ["가스", "gas", "flow"] },
    { label: "전력", aliases: ["전력", "power", "rf", "bias"] },
    { label: "상태", aliases: ["상태", "state", "status"] },
    { label: "로그", aliases: ["로그", "log", "message"] },
    { label: "웨이퍼", aliases: ["웨이퍼", "wafer"] }
  ];

  return groups.find((group) => containsAny(normalized, group.aliases)) ?? null;
}

function findSensorMatches(
  keyword: { label: string; aliases: string[] },
  equipment: EquipmentSnapshot[]
) {
  return equipment.flatMap((item) =>
    (item.current?.sensors ?? [])
      .filter((sensor) =>
        keyword.aliases.some((alias) => sensor.sensorId.toLowerCase().includes(alias.toLowerCase()))
      )
      .map((sensor) => ({ equipment: item, sensor }))
  );
}

function containsAny(text: string, needles: string[]): boolean {
  return needles.some((needle) => text.includes(needle));
}

async function question(prompt: string): Promise<string | null> {
  try {
    return await rl.question(prompt);
  } catch (error) {
    if (error instanceof Error && "code" in error && error.code === "ERR_USE_AFTER_CLOSE") {
      return null;
    }
    throw error;
  }
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
  name: "capstone-dashboard-ollama-chat",
  version: "0.1.0"
});

const rl = createInterface({ input, output });

try {
  await client.connect(transport);
  await callTool(client, {
    name: "login",
    args: { username, password },
    description: "채팅 세션 시작 전 자동 로그인"
  });

  console.log("\nOllama + MCP 자연어 테스트를 시작합니다.");
  console.log("예: CVD 장비 센서 목록 보여줘");
  console.log("예: 현재 대시보드 세팅 상태 점검해줘");
  console.log("종료하려면 exit 입력\n");

  while (true) {
    const answer = await question("> ");
    if (answer === null) {
      break;
    }
    const text = answer.trim();
    if (!text) {
      continue;
    }
    if (["exit", "quit", "종료"].includes(text.toLowerCase())) {
      break;
    }

    const action = await planAction(client, text);
    if (action.kind === "message") {
      console.log(action.message);
      continue;
    }

    if (action.kind === "multiTool") {
      if (action.dryRun) {
        console.log(`\n[Ollama]\n${summarizeCustomWidgetPlan(action.plan, true)}\n`);
        continue;
      }

      const confirmAnswer = await question("실제 변경 작업입니다. 실행할까요? (yes/no) ");
      const confirm = confirmAnswer?.trim() ?? "";
      if (confirm.toLowerCase() !== "yes") {
        console.log("실행을 취소했습니다.");
        continue;
      }

      const results: unknown[] = [];
      for (const toolCall of action.calls) {
        results.push(await callToolWithAuthRetry(client, toolCall, { username, password }));
      }
      console.log(`\n[Ollama]\n${summarizeCustomWidgetPlan(action.plan, false, results)}\n`);
      continue;
    }

    if (action.kind === "operation") {
      if (action.operation.dryRun) {
        console.log(`\n[Ollama]\n${summarizeOperation(action.operation)}\n`);
        continue;
      }

      console.log(`\n[Ollama]\n${summarizeOperation({ ...action.operation, dryRun: true })}\n`);
      const confirmAnswer = await question("실제 변경 작업입니다. 실행할까요? (yes/no) ");
      const confirm = confirmAnswer?.trim() ?? "";
      if (confirm.toLowerCase() !== "yes") {
        console.log("실행을 취소했습니다.");
        continue;
      }

      const results: unknown[] = [];
      for (const toolCall of action.operation.calls) {
        results.push(await callToolWithAuthRetry(client, toolCall, { username, password }));
      }
      console.log(`\n[Ollama]\n${summarizeOperation(action.operation, results)}\n`);
      continue;
    }

    const toolCall = action.call;
    if (toolCall.mutates) {
      const confirmAnswer = await question("실제 변경 작업입니다. 실행할까요? (yes/no) ");
      const confirm = confirmAnswer?.trim() ?? "";
      if (confirm.toLowerCase() !== "yes") {
        console.log("실행을 취소했습니다.");
        continue;
      }
    }

    const toolResult = await callToolWithAuthRetry(client, toolCall, { username, password });
    const summary = await summarizeWithOllama(text, toolResult);
    console.log(`\n[Ollama]\n${summary}\n`);
  }
} finally {
  rl.close();
  await client.close();
}
