/**
 * 网球场预约平台 — MCP Server（stdio）
 *
 * 通过环境变量连接 Spring Boot：
 * - TENNIS_API_BASE_URL  默认 http://localhost:8080
 * - TENNIS_API_JWT       调用需登录接口时必填（Bearer）
 *
 * Cursor / Claude Desktop：在 MCP 配置中指向本脚本，并设置 env。
 */
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";

const BASE = process.env.TENNIS_API_BASE_URL?.replace(/\/$/, "") ?? "http://localhost:8080";
const JWT = process.env.TENNIS_API_JWT ?? "";

async function apiGet(path: string, needAuth: boolean): Promise<{ ok: boolean; status: number; body: string }> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (needAuth) {
    if (!JWT) {
      return { ok: false, status: 401, body: "缺少环境变量 TENNIS_API_JWT，无法访问需登录接口" };
    }
    headers.Authorization = `Bearer ${JWT}`;
  }
  const res = await fetch(`${BASE}${path}`, { headers });
  const body = await res.text();
  return { ok: res.ok, status: res.status, body };
}

const server = new McpServer({
  name: "tennis-court-booking",
  version: "1.0.0",
});

server.registerTool(
  "tennis_backend_ping",
  {
    description: "检查后端是否可达（调用公开接口 GET /api/coupons/activities，无需 JWT）",
  },
  async () => {
    const r = await apiGet("/api/coupons/activities", false);
    return {
      content: [{ type: "text", text: `HTTP ${r.status}\n${r.body.slice(0, 8000)}` }],
    };
  },
);

server.registerTool(
  "tennis_list_courts",
  {
    description: "获取场地列表（GET /api/courts，需在环境变量 TENNIS_API_JWT 中配置登录用户的 JWT）",
  },
  async () => {
    const r = await apiGet("/api/courts", true);
    return {
      content: [{ type: "text", text: `HTTP ${r.status}\n${r.body.slice(0, 8000)}` }],
    };
  },
);

server.registerResource(
  "platform_help_hint",
  "tennis://docs/help",
  {
    description: "平台帮助与 RAG 说明（静态提示，非实时数据库）",
    mimeType: "text/plain",
  },
  async (_uri, _extra) => ({
    contents: [
      {
        uri: "tennis://docs/help",
        mimeType: "text/plain",
        text:
          "场地列表、预约、支付等以 Spring Boot 业务 API 与 LangChain 工具为准；" +
          "RAG 文档位于后端 classpath:rag/。需要实时数据请使用工具 tennis_list_courts 或 tennis_backend_ping。",
      },
    ],
  }),
);

async function main() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
