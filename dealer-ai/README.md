# dealer-ai

`dealer-ai` 是汽车销售 CRM 的内部 AI 编排服务，只供 `dealer-server` 调用。

## 技术栈

- Python: `3.13.14`，见 `.python-version`
- uv: `0.11.25`，作为本地工具链要求，不作为项目依赖
- FastAPI: `0.138.1`
- Uvicorn: `0.49.0`
- Pydantic: `2.13.4`
- pydantic-settings: `2.14.2`
- HTTPX: `0.28.1`
- LangGraph: `1.2.6`，作为默认且唯一目标编排内核，不暴露给 Spring Boot、前端、数据库或 OpenAPI

## 本地命令

```bash
uv sync
uv run uvicorn app.main:app --reload --port 8091
uv run ruff check .
uv run pytest
```

## 服务边界

- 浏览器和 `dealer-web` 不直接调用本服务。
- 本服务不连接业务数据库、Redis 会话或 Mapper。
- 本服务不保存用户 Bearer Token、业务数据库账号、模型密钥原文或供应商原始响应。
- 本服务只访问 Spring Boot 内部 AI Tool API 和 Spring Boot 每次 Run 下发的 Provider runtime config 地址。
- 本服务不注册通用 SQL、HTTP、文件写入或 Shell 执行类危险工具。
- `LangGraphAgentOrchestrator` 是目标运行路径；不存在 simple/langgraph 双路线。
- Provider 配置真源在 Spring Boot 管理端，`dealer-ai` 不从进程环境变量读取正式模型密钥、模型名或 Base URL。

## 关键环境变量

- `DEALER_AI_INTERNAL_TOKEN`：`dealer-server` 调用 `dealer-ai` 的服务间令牌。
- `DEALER_AI_SPRING_TOOL_BASE_URL`：Spring Boot 内部 AI Tool API 基础地址。
- `DEALER_AI_SPRING_TOOL_TOKEN`：调用 Spring Boot 内部 AI Tool API 的服务间令牌，必须与 Spring Boot 的 `DEALER_AI_TOOL_TOKEN` 一致。
- `AI_PROVIDER_TIMEOUT_SECONDS`

本地 `local/dev/test/smoke` 环境默认内部令牌为 `dev-internal-token`。非本地环境必须显式配置 `DEALER_AI_INTERNAL_TOKEN` 和 `DEALER_AI_SPRING_TOOL_TOKEN`，禁止使用本地默认令牌启动。

真实模型配置通过 Spring Boot Provider 管理接口维护。API Key 由 Spring Boot 加密入库，`dealer-ai` 只在单次内部请求中接收 runtime config，禁止写入 Git、日志、测试快照或前端代码。
