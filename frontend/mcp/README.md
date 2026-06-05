# Dashboard API MCP

This MCP server is generated from the Dashboard Swagger/OpenAPI document at:

```text
http://api.43.201.141.9.nip.io/api-docs
```

It exposes three tools:

- `list_endpoints`: search endpoints by tag or text.
- `get_operation`: inspect parameters, request body, responses, and auth for one operation.
- `call_api`: call an endpoint by `operationId` or by `method` + `path`.

## Run

```bash
npm run mcp:dashboard-api
```

## Client config

Example MCP client configuration:

```json
{
  "mcpServers": {
    "dashboard-api": {
      "command": "node",
      "args": [
        "C:\\Users\\so021\\Documents\\Doodles\\capstone-dashboard\\capstone-dashboard\\mcp\\dashboard-api-mcp.js"
      ],
      "env": {
        "DASHBOARD_OPENAPI_URL": "http://api.43.201.141.9.nip.io/api-docs",
        "DASHBOARD_API_BASE_URL": "http://api.43.201.141.9.nip.io",
        "DASHBOARD_API_TOKEN": ""
      }
    }
  }
}
```

Set `DASHBOARD_API_TOKEN` to a JWT access token when calling authenticated APIs. You can also pass `token` directly to the `call_api` tool.

## Tool examples

List dashboard endpoints:

```json
{
  "tag": "Dashboards"
}
```

Call login:

```json
{
  "operationId": "login",
  "body": {
    "username": "user@example.com",
    "password": "password"
  }
}
```

Call an authenticated endpoint:

```json
{
  "operationId": "getDashboards",
  "token": "ACCESS_TOKEN"
}
```
