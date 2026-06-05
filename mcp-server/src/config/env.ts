export type AppConfig = {
  dashboardApiBaseUrl: string;
  dashboardApiToken?: string;
  rejectUnauthorized: boolean;
  ollamaBaseUrl: string;
  ollamaModel: string;
};

export function loadConfig(): AppConfig {
  const dashboardApiBaseUrl =
    process.env.DASHBOARD_API_BASE_URL ?? "https://api.43.201.141.9.nip.io";

  return {
    dashboardApiBaseUrl: dashboardApiBaseUrl.replace(/\/$/, ""),
    dashboardApiToken: process.env.DASHBOARD_API_TOKEN || undefined,
    rejectUnauthorized: process.env.DASHBOARD_TLS_REJECT_UNAUTHORIZED !== "false",
    ollamaBaseUrl: (process.env.OLLAMA_BASE_URL ?? "http://localhost:11434").replace(/\/$/, ""),
    ollamaModel: process.env.OLLAMA_MODEL ?? "qwen2.5:7b"
  };
}
