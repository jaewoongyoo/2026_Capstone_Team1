#!/usr/bin/env node

import { stdin, stdout, stderr } from "node:process";

const DEFAULT_OPENAPI_URL = "http://api.43.201.141.9.nip.io/api-docs";
const DEFAULT_API_BASE_URL = "http://api.43.201.141.9.nip.io";

const OPENAPI_URL = process.env.DASHBOARD_OPENAPI_URL || DEFAULT_OPENAPI_URL;
const API_BASE_URL = (process.env.DASHBOARD_API_BASE_URL || DEFAULT_API_BASE_URL).replace(/\/$/, "");
const DEFAULT_TOKEN = process.env.DASHBOARD_API_TOKEN || "";

let openApiCache;
let operationCache;

function write(message) {
  stdout.write(`${JSON.stringify(message)}\n`);
}

function logError(...args) {
  stderr.write(`${args.map(String).join(" ")}\n`);
}

function textContent(text) {
  return {
    content: [
      {
        type: "text",
        text: typeof text === "string" ? text : JSON.stringify(text, null, 2),
      },
    ],
  };
}

function jsonSchemaForCallApi() {
  return {
    type: "object",
    additionalProperties: false,
    properties: {
      operationId: {
        type: "string",
        description: "OpenAPI operationId. Use either operationId or method + path.",
      },
      method: {
        type: "string",
        enum: ["GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"],
      },
      path: {
        type: "string",
        description: "OpenAPI path such as /api/dashboards/{dashboardId}.",
      },
      pathParams: {
        type: "object",
        additionalProperties: true,
        description: "Values for path parameters.",
      },
      query: {
        type: "object",
        additionalProperties: true,
        description: "Query string parameters.",
      },
      body: {
        description: "JSON request body.",
      },
      headers: {
        type: "object",
        additionalProperties: { type: "string" },
        description: "Extra request headers.",
      },
      token: {
        type: "string",
        description: "Bearer token. If omitted, DASHBOARD_API_TOKEN is used.",
      },
    },
  };
}

const tools = [
  {
    name: "list_endpoints",
    description: "List Dashboard API endpoints from the live OpenAPI document.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        tag: { type: "string", description: "Optional tag filter, for example Dashboards." },
        search: { type: "string", description: "Optional text search across method, path, summary, and operationId." },
      },
    },
  },
  {
    name: "get_operation",
    description: "Show request parameters, request body schema, responses, and security for one API operation.",
    inputSchema: {
      type: "object",
      additionalProperties: false,
      properties: {
        operationId: { type: "string" },
        method: { type: "string" },
        path: { type: "string" },
      },
    },
  },
  {
    name: "call_api",
    description: "Call a Dashboard REST API endpoint described by the Swagger/OpenAPI document.",
    inputSchema: jsonSchemaForCallApi(),
  },
];

async function fetchOpenApi() {
  if (openApiCache) {
    return openApiCache;
  }

  const response = await fetch(OPENAPI_URL, {
    headers: {
      accept: "application/json",
    },
  });

  if (!response.ok) {
    throw new Error(`OpenAPI fetch failed: ${response.status} ${response.statusText}`);
  }

  openApiCache = await response.json();
  operationCache = indexOperations(openApiCache);
  return openApiCache;
}

function indexOperations(openApi) {
  const operations = [];
  const methods = new Set(["get", "post", "put", "patch", "delete", "head", "options"]);

  for (const [path, pathItem] of Object.entries(openApi.paths || {})) {
    for (const [method, operation] of Object.entries(pathItem || {})) {
      if (!methods.has(method)) {
        continue;
      }

      operations.push({
        method: method.toUpperCase(),
        path,
        operationId: operation.operationId || `${method}_${path.replace(/[^a-zA-Z0-9]+/g, "_")}`,
        tags: operation.tags || [],
        summary: operation.summary || "",
        description: operation.description || "",
        parameters: [...(pathItem.parameters || []), ...(operation.parameters || [])],
        requestBody: operation.requestBody,
        responses: operation.responses || {},
        security: operation.security ?? openApi.security ?? [],
        operation,
      });
    }
  }

  return operations;
}

async function getOperations() {
  await fetchOpenApi();
  return operationCache;
}

async function handleListEndpoints(args = {}) {
  const operations = await getOperations();
  const tag = args.tag?.toLowerCase();
  const search = args.search?.toLowerCase();

  const filtered = operations.filter((operation) => {
    const matchesTag = !tag || operation.tags.some((item) => item.toLowerCase() === tag);
    const haystack = [
      operation.method,
      operation.path,
      operation.operationId,
      operation.summary,
      operation.description,
      ...operation.tags,
    ]
      .join(" ")
      .toLowerCase();
    return matchesTag && (!search || haystack.includes(search));
  });

  return textContent(
    filtered.map(({ method, path, operationId, tags, summary }) => ({
      method,
      path,
      operationId,
      tags,
      summary,
    })),
  );
}

function findOperation(operations, args = {}) {
  if (args.operationId) {
    return operations.find((operation) => operation.operationId === args.operationId);
  }

  if (args.method && args.path) {
    return operations.find(
      (operation) =>
        operation.method.toLowerCase() === String(args.method).toLowerCase() &&
        operation.path === args.path,
    );
  }

  return undefined;
}

async function handleGetOperation(args = {}) {
  const operations = await getOperations();
  const operation = findOperation(operations, args);

  if (!operation) {
    throw new Error("Operation not found. Provide operationId, or method and path.");
  }

  return textContent({
    method: operation.method,
    path: operation.path,
    operationId: operation.operationId,
    tags: operation.tags,
    summary: operation.summary,
    description: operation.description,
    parameters: operation.parameters,
    requestBody: operation.requestBody,
    responses: operation.responses,
    security: operation.security,
  });
}

function buildPath(path, pathParams = {}) {
  return path.replace(/\{([^}]+)\}/g, (match, key) => {
    if (pathParams[key] === undefined || pathParams[key] === null) {
      throw new Error(`Missing path parameter: ${key}`);
    }
    return encodeURIComponent(String(pathParams[key]));
  });
}

function appendQuery(url, query = {}) {
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null) {
      continue;
    }

    if (Array.isArray(value)) {
      for (const item of value) {
        url.searchParams.append(key, String(item));
      }
      continue;
    }

    url.searchParams.set(key, String(value));
  }
}

async function handleCallApi(args = {}) {
  const operations = await getOperations();
  const operation = findOperation(operations, args);

  if (!operation) {
    throw new Error("Operation not found. Provide operationId, or method and path.");
  }

  const resolvedPath = buildPath(operation.path, args.pathParams || {});
  const url = new URL(`${API_BASE_URL}${resolvedPath}`);
  appendQuery(url, args.query || {});

  const headers = {
    accept: "application/json",
    ...(args.headers || {}),
  };

  const token = args.token || DEFAULT_TOKEN;
  if (token) {
    headers.authorization = token.toLowerCase().startsWith("bearer ") ? token : `Bearer ${token}`;
  }

  const init = {
    method: operation.method,
    headers,
  };

  if (args.body !== undefined && !["GET", "HEAD"].includes(operation.method)) {
    headers["content-type"] = headers["content-type"] || "application/json";
    init.body = typeof args.body === "string" ? args.body : JSON.stringify(args.body);
  }

  const response = await fetch(url, init);
  const contentType = response.headers.get("content-type") || "";
  const responseBody = contentType.includes("application/json") ? await response.json() : await response.text();

  return textContent({
    request: {
      method: operation.method,
      url: url.toString(),
    },
    response: {
      ok: response.ok,
      status: response.status,
      statusText: response.statusText,
      headers: Object.fromEntries(response.headers.entries()),
      body: responseBody,
    },
  });
}

async function handleToolCall(name, args) {
  if (name === "list_endpoints") {
    return handleListEndpoints(args);
  }
  if (name === "get_operation") {
    return handleGetOperation(args);
  }
  if (name === "call_api") {
    return handleCallApi(args);
  }
  throw new Error(`Unknown tool: ${name}`);
}

async function handleRequest(message) {
  if (message.method === "initialize") {
    return {
      protocolVersion: message.params?.protocolVersion || "2024-11-05",
      capabilities: {
        tools: {},
        resources: {},
      },
      serverInfo: {
        name: "dashboard-api-mcp",
        version: "1.0.0",
      },
    };
  }

  if (message.method === "tools/list") {
    return { tools };
  }

  if (message.method === "tools/call") {
    return handleToolCall(message.params?.name, message.params?.arguments || {});
  }

  if (message.method === "resources/list") {
    return {
      resources: [
        {
          uri: "dashboard-api://openapi",
          name: "Dashboard API OpenAPI document",
          mimeType: "application/json",
        },
      ],
    };
  }

  if (message.method === "resources/read") {
    if (message.params?.uri !== "dashboard-api://openapi") {
      throw new Error(`Unknown resource: ${message.params?.uri}`);
    }

    const openApi = await fetchOpenApi();
    return {
      contents: [
        {
          uri: "dashboard-api://openapi",
          mimeType: "application/json",
          text: JSON.stringify(openApi, null, 2),
        },
      ],
    };
  }

  if (message.method === "notifications/initialized") {
    return undefined;
  }

  throw new Error(`Unsupported method: ${message.method}`);
}

let buffer = "";

stdin.setEncoding("utf8");
stdin.on("data", (chunk) => {
  buffer += chunk;

  let newlineIndex;
  while ((newlineIndex = buffer.indexOf("\n")) >= 0) {
    const line = buffer.slice(0, newlineIndex).trim();
    buffer = buffer.slice(newlineIndex + 1);

    if (!line) {
      continue;
    }

    void processLine(line);
  }
});

async function processLine(line) {
  let message;
  try {
    message = JSON.parse(line);
  } catch (error) {
    logError("Invalid JSON-RPC message:", error.message);
    return;
  }

  if (!message.id && message.id !== 0) {
    try {
      await handleRequest(message);
    } catch (error) {
      logError(error.stack || error.message);
    }
    return;
  }

  try {
    const result = await handleRequest(message);
    write({
      jsonrpc: "2.0",
      id: message.id,
      result,
    });
  } catch (error) {
    write({
      jsonrpc: "2.0",
      id: message.id,
      error: {
        code: -32000,
        message: error.message,
      },
    });
  }
}
