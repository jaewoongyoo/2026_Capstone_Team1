import { getAccessToken } from "../utils/Auth";

const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();
const API_BASE_URL = configuredApiBaseUrl || window.location.origin;
const configuredMcpChatUrl = import.meta.env.VITE_MCP_CHAT_URL?.trim();
const MCP_CHAT_URL = configuredMcpChatUrl || "http://localhost:3333/api/mcp/chat";

export type ApiResponse<T> = {
  success: boolean;
  message?: string;
  data?: T;
  statusCode?: number;
  errorCode?: number;
  errorDetail?: string;
  timestamp?: string;
  path?: string;
};

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: unknown;
  accessToken?: string;
  skipAuth?: boolean;
};

export type LoginRequest = {
  username: string;
  password: string;
};

export type SignupRequest = {
  username: string;
  email: string;
  password: string;
  fullName: string;
};

export type LoginResponse = {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: number;
  username: string;
  role: string;
};

export type UserInfoResponse = {
  userId: number;
  username: string;
  email?: string;
  fullName?: string;
  role: string;
  isActive?: boolean;
  createdAt?: string;
  updatedAt?: string;
};

export type DashboardResponse = {
  dashboardId: number;
  dashboardName: string;
  description?: string;
  userId: number;
  isPublic?: boolean;
  shareToken?: string | null;
  createdAt?: string;
  updatedAt?: string;
};

export type DashboardShareResponse = {
  dashboardId: number;
  dashboardName: string;
  isPublic: boolean;
  shareToken: string | null;
};

export type DashboardUpdateRequest = {
  dashboardName: string;
  description?: string;
};

export type EquipmentResponse = {
  equipmentId: number;
  equipmentName: string;
  field?: string;
  dashboardId: number;
};

export type SensorResponse = {
  sensorId: number;
  sensorName: string;
  equipmentId: number;
};

export type DiscoveredTag = {
  sensorName: string;
};

export type DiscoveredEquipment = {
  equipmentName: string;
  field?: string;
  tags?: DiscoveredTag[];
};

export type DiscoveryApplyRequest = {
  dashboardId: number;
  assets: DiscoveredEquipment[];
};

export type AppliedEquipment = {
  equipment: EquipmentResponse;
  sensors: SensorResponse[];
};

export type DiscoveryApplyResponse = {
  dashboardId: number;
  equipmentCount: number;
  sensorCount: number;
  equipment: AppliedEquipment[];
};

export type SensorDetails = {
  sensorId?: string;
  sensorName?: string;
  name?: string;
  dataType?: "FLOAT" | "DOUBLE" | "BOOLEAN" | "INTEGER" | "INT" | "STRING";
  value?: unknown;
  currentValue?: unknown;
  numericValue?: unknown;
  unit?: string;
};

export type SensorDataPayload = {
  equipmentEntityId?: number;
  equipmentId?: string;
  timestamp?: string;
  status?: string;
  sensors: SensorDetails[];
};

export type EquipmentCurrentResponse = {
  equipmentId: number;
  equipmentName: string;
  field?: string;
  dashboardId: number;
  current?: SensorDataPayload;
};

export type PublicDashboardResponse = DashboardResponse & {
  dashboard?: DashboardResponse;
  widgets?: WidgetResponseDto[];
  dashboardWidgets?: WidgetResponseDto[];
  equipment?: EquipmentCurrentResponse[];
  equipments?: EquipmentCurrentResponse[];
  currentEquipment?: EquipmentCurrentResponse[];
  equipmentCurrent?: EquipmentCurrentResponse[];
  currentData?: EquipmentCurrentResponse[];
};

export type WidgetResponseDto = {
  id: number;
  userId: number;
  dashboardId?: number;
  dashboardName?: string;
  equipmentId?: string;
  equipmentEntityId?: number;
  equipmentName?: string;
  widgetType: string;
  title: string;
  sensorId?: string;
  sensorEntityId?: number;
  sensorName?: string;
  chartType?: string;
  dataType?: string;
  unit?: string;
  posX: number;
  posY: number;
  width: number;
  height: number;
  configJson?: string;
  createdAt?: string;
  updatedAt?: string;
};

export type WidgetRequestDto = {
  dashboardId?: number;
  equipmentId?: string;
  equipmentEntityId?: number;
  widgetType: string;
  title: string;
  sensorId?: string;
  sensorEntityId?: number;
  chartType?: string;
  dataType?: string;
  unit?: string;
  posX: number;
  posY: number;
  width: number;
  height: number;
  configJson?: string;
};

export type WidgetLayoutItem = {
  widgetId: number;
  posX: number;
  posY: number;
  width: number;
  height: number;
};

export type WidgetLayoutUpdateDto = {
  layouts: WidgetLayoutItem[];
};

export type McpChatRequest = {
  message: string;
};

export type McpChatResponse = {
  reply: string;
  action?: string;
  data?: unknown;
  requiresConfirmation?: boolean;
};

function buildHeaders(options: RequestOptions): Headers {
  const headers = new Headers(options.headers);

  if (!headers.has("Content-Type") && options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }

  const accessToken = options.skipAuth ? undefined : options.accessToken ?? getAccessToken();

  if (accessToken && !headers.has("Authorization")) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  return headers;
}

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { body, accessToken: _accessToken, skipAuth: _skipAuth, ...fetchOptions } = options;
  const url = new URL(path, API_BASE_URL);

  try {
    const response = await fetch(url, {
      ...fetchOptions,
      headers: buildHeaders(options),
      body: body === undefined ? undefined : JSON.stringify(body),
    });

    const contentType = response.headers.get("content-type") ?? "";
    const payload = contentType.includes("application/json") ? await response.json() : await response.text();

    if (!response.ok) {
      console.error("[API] Request failed", {
        url: url.toString(),
        status: response.status,
        payload,
      });
      const message =
        payload && typeof payload === "object" && "message" in payload
          ? [
            (payload as ApiResponse<unknown>).message,
            (payload as ApiResponse<unknown>).errorDetail,
          ].filter(Boolean).join(": ")
          : `API request failed: ${response.status} ${response.statusText}`;
      throw new Error(message);
    }

    return payload as T;
  } catch (error) {
    console.error("[API] Network or CORS error", {
      url: url.toString(),
      error,
    });
    throw error;
  }
}

export const apiClient = {
  get: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "GET" }),
  post: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "POST", body }),
  put: <T>(path: string, body?: unknown, options?: RequestOptions) =>
    request<T>(path, { ...options, method: "PUT", body }),
  delete: <T>(path: string, options?: RequestOptions) => request<T>(path, { ...options, method: "DELETE" }),
};

export function loginWithPassword(body: LoginRequest) {
  return apiClient.post<ApiResponse<LoginResponse>>("/api/auth/login", body);
}

export function signup(body: SignupRequest) {
  return apiClient.post<ApiResponse<UserInfoResponse>>("/api/auth/signup", body);
}

export function refreshAccessToken(refreshToken: string) {
  return apiClient.post<ApiResponse<LoginResponse>>("/api/auth/refresh", { refreshToken });
}

export function logoutSession(accessToken?: string) {
  return apiClient.post<ApiResponse<void>>("/api/auth/logout", undefined, { accessToken });
}

export function getMe(accessToken?: string) {
  return apiClient.get<ApiResponse<UserInfoResponse>>("/api/auth/me", { accessToken });
}

export function getMyDashboards(accessToken?: string) {
  return apiClient.get<ApiResponse<DashboardResponse[]>>("/api/dashboards", { accessToken });
}

export function getDashboard(dashboardId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<DashboardResponse>>(`/api/dashboards/${dashboardId}`, { accessToken });
}

export function updateDashboard(dashboardId: number | string, body: DashboardUpdateRequest, accessToken?: string) {
  return apiClient.put<ApiResponse<DashboardResponse>>(`/api/dashboards/${dashboardId}`, body, { accessToken });
}

export function enableDashboardShare(dashboardId: number | string, accessToken?: string) {
  return apiClient.post<ApiResponse<DashboardShareResponse>>(`/api/dashboards/${dashboardId}/share/enable`, undefined, {
    accessToken,
  });
}

export function disableDashboardShare(dashboardId: number | string, accessToken?: string) {
  return apiClient.post<ApiResponse<DashboardShareResponse>>(`/api/dashboards/${dashboardId}/share/disable`, undefined, {
    accessToken,
  });
}

export function getPublicDashboard(shareToken: string) {
  const params = new URLSearchParams({ token: shareToken });
  return apiClient.get<ApiResponse<PublicDashboardResponse>>(`/api/public/dashboards?${params.toString()}`, {
    skipAuth: true,
  });
}

export function getPublicDashboardWidgets(dashboardId: number | string) {
  return apiClient.get<ApiResponse<WidgetResponseDto[]>>(`/api/dashboards/${dashboardId}/widgets`, {
    skipAuth: true,
  });
}

export function getDashboardWidgets(dashboardId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<WidgetResponseDto[]>>(`/api/dashboards/${dashboardId}/widgets`, { accessToken });
}

export function getMyWidgets(accessToken?: string) {
  return apiClient.get<ApiResponse<WidgetResponseDto[]>>("/api/dashboard/widgets", { accessToken });
}

export function getEquipmentWidgets(equipmentId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<WidgetResponseDto[]>>(`/api/equipment/${equipmentId}/widgets`, { accessToken });
}

export function getDashboardWidgetsByEquipment(equipmentId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<WidgetResponseDto[]>>(`/api/dashboard/widgets/equipment/${equipmentId}`, { accessToken });
}

export function createDashboardWidget(dashboardId: number | string, body: WidgetRequestDto, accessToken?: string) {
  return apiClient.post<ApiResponse<WidgetResponseDto>>(`/api/dashboards/${dashboardId}/widgets`, body, { accessToken });
}

export function deleteDashboardWidget(widgetId: number | string, accessToken?: string) {
  return apiClient.delete<ApiResponse<void>>(`/api/dashboard/widgets/${widgetId}`, { accessToken });
}

export function updateWidgetLayouts(body: WidgetLayoutUpdateDto, accessToken?: string) {
  return apiClient.put<ApiResponse<WidgetResponseDto[]>>("/api/dashboard/widgets/layout", body, { accessToken });
}

export async function sendMcpChatMessage(body: McpChatRequest, accessToken?: string) {
  const response = await fetch(MCP_CHAT_URL, {
    method: "POST",
    headers: buildHeaders({
      body,
      accessToken,
    }),
    body: JSON.stringify(body),
  });
  const payload = await response.json() as McpChatResponse;

  if (!response.ok) {
    throw new Error(payload.reply || `MCP chat failed: ${response.status}`);
  }

  return payload;
}

export function getDashboardEquipment(dashboardId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<EquipmentResponse[]>>(`/api/equipment/dashboard/${dashboardId}`, { accessToken });
}

export function searchMyEquipment(keyword = "", accessToken?: string) {
  const params = new URLSearchParams();

  if (keyword) {
    params.set("keyword", keyword);
  }

  const query = params.toString();
  return apiClient.get<ApiResponse<EquipmentResponse[]>>(`/api/equipment/search${query ? `?${query}` : ""}`, {
    accessToken,
  });
}

export function applyEquipmentDiscovery(body: DiscoveryApplyRequest, accessToken?: string) {
  return apiClient.post<ApiResponse<DiscoveryApplyResponse>>("/api/equipment/discovery/apply", body, {
    accessToken,
  });
}

export function getEquipment(equipmentId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<EquipmentResponse>>(`/api/equipment/${equipmentId}`, { accessToken });
}

export function deleteEquipment(equipmentId: number | string, accessToken?: string) {
  return apiClient.delete<ApiResponse<void>>(`/api/equipment/${equipmentId}`, { accessToken });
}

export function getEquipmentSensors(equipmentId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<SensorResponse[]>>(`/api/sensors/equipment/${equipmentId}`, { accessToken });
}

export function searchEquipmentSensors(equipmentId: number | string, keyword = "", accessToken?: string) {
  const params = new URLSearchParams();

  if (keyword) {
    params.set("keyword", keyword);
  }

  const query = params.toString();
  return apiClient.get<ApiResponse<SensorResponse[]>>(
    `/api/sensors/equipment/${equipmentId}/search${query ? `?${query}` : ""}`,
    { accessToken },
  );
}

export function getMyEquipmentCurrent(accessToken?: string) {
  return apiClient.get<ApiResponse<EquipmentCurrentResponse[]>>("/api/equipment/current", { accessToken });
}

export function getEquipmentCurrent(equipmentId: number | string, accessToken?: string) {
  return apiClient.get<ApiResponse<EquipmentCurrentResponse>>(`/api/equipment/${equipmentId}/current`, { accessToken });
}
