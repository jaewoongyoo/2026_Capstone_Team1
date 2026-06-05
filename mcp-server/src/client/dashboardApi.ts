import { Agent, request } from "node:https";
import { request as httpRequest } from "node:http";
import { URL } from "node:url";
import type { ApiResponse } from "../types/api.js";
import type { AppConfig } from "../config/env.js";

export class DashboardApiClient {
  private token?: string;
  private readonly httpsAgent: Agent;

  constructor(private readonly config: AppConfig) {
    this.token = config.dashboardApiToken;
    this.httpsAgent = new Agent({
      rejectUnauthorized: config.rejectUnauthorized
    });
  }

  setToken(token: string): void {
    this.token = token;
  }

  async get<T>(path: string): Promise<T> {
    return this.request<T>("GET", path);
  }

  async post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>("POST", path, body);
  }

  async put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>("PUT", path, body);
  }

  async delete<T>(path: string): Promise<T> {
    return this.request<T>("DELETE", path);
  }

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const url = new URL(path, this.config.dashboardApiBaseUrl);
    const payload = body === undefined ? undefined : JSON.stringify(body);

    const headers: Record<string, string> = {
      Accept: "application/json"
    };

    if (payload) {
      headers["Content-Type"] = "application/json";
      headers["Content-Length"] = Buffer.byteLength(payload).toString();
    }

    if (this.token) {
      headers.Authorization = `Bearer ${this.token}`;
    }

    const raw = await this.send(url, method, headers, payload);
    const parsed = raw ? JSON.parse(raw) : undefined;

    if (parsed && typeof parsed === "object" && "success" in parsed && "data" in parsed) {
      const response = parsed as ApiResponse<T>;
      if (!response.success) {
        throw new Error(response.message || `Dashboard API request failed: ${method} ${path}`);
      }
      return response.data;
    }

    return parsed as T;
  }

  private send(
    url: URL,
    method: string,
    headers: Record<string, string>,
    payload?: string
  ): Promise<string> {
    const isHttps = url.protocol === "https:";
    const transport = isHttps ? request : httpRequest;

    return new Promise((resolve, reject) => {
      const req = transport(
        url,
        {
          method,
          headers,
          agent: isHttps ? this.httpsAgent : undefined
        },
        (res) => {
          const chunks: Buffer[] = [];
          res.on("data", (chunk) => chunks.push(Buffer.from(chunk)));
          res.on("end", () => {
            const text = Buffer.concat(chunks).toString("utf8");
            if (res.statusCode && res.statusCode >= 400) {
              reject(new Error(`Dashboard API ${method} ${url.pathname} failed (${res.statusCode}): ${text}`));
              return;
            }
            resolve(text);
          });
        }
      );

      req.on("error", reject);
      if (payload) {
        req.write(payload);
      }
      req.end();
    });
  }
}
