# dealer-ai LangGraph 编排任务

## 任务目标

将 `dealer-ai` 收敛为 LangGraph 默认编排，接收 Spring Boot 下发的 `providerRuntimeConfig`，根据 Provider 格式选择 OpenAI-compatible 或 Anthropic Adapter，并输出稳定内部事件。

## 前置阅读

- docs/plan/AI业务助手/02-LangGraph编排与运行时方案.md
- docs/task/AI业务助手/02-AI模型供应商配置管理任务.md
- docs/rule/13-AI业务助手开发规范.md

## 允许修改范围

- dealer-ai/app/api/routes/runs.py
- dealer-ai/app/core/config.py
- dealer-ai/app/orchestrator/
- dealer-ai/app/providers/
- dealer-ai/app/schemas/chat.py
- dealer-ai/app/schemas/events.py
- dealer-ai/app/tools/
- dealer-ai/tests/
- dealer-ai/pyproject.toml
- dealer-ai/uv.lock
- dealer-ai/README.md

## 禁止修改范围

- 不得让 `dealer-ai` 连接数据库、Redis、Mapper、普通业务 API、文件系统写入或 Shell。
- 不得把 Provider runtime config 写日志。
- 不得保留目标架构下的 `simple/langgraph` 切换。
- 不得让 LangGraph 类型进入 Spring Boot 对外 API 或前端类型。

## 执行步骤

1. 将 LangGraph 编排设为默认唯一编排入口。
2. 移除目标架构中的 simple 编排选择逻辑。
3. 在 `ChatRunRequest` 中接收 `providerRuntimeConfig`。
4. 定义 OpenAI-compatible 和 Anthropic Provider Adapter。
5. 让 Provider Adapter 只读取本次请求的 runtime config。
6. 固定 LangGraph 节点图，禁止模型动态创建任意节点和工具。
7. 工具节点只能通过 ToolClient 调用 Spring Boot 内部 Tool API。
8. 等待确认节点只能输出事件，不执行业务写入。
9. 模型错误、工具错误和配置错误转为稳定内部事件。
10. 补齐 mock Provider、mock ToolClient 和危险工具测试。

## 完成条件

- `dealer-ai` 默认使用 LangGraph 编排。
- OpenAI-compatible 和 Anthropic Adapter 均可通过 mock 测试。
- `providerRuntimeConfig` 不被持久化和记录。
- 高风险工具和危险工具无法进入图。
- 内部事件可被 Spring Boot 转换和持久化。

## 验证命令

- `cd dealer-ai && uv run ruff check .`
- `cd dealer-ai && uv run pytest`
- 运行旧编排残留扫描，目标运行路径不得命中旧编排路线和运行时路线选择配置。
- `rg -n "run_sql|http_request|file_write|shell_exec" dealer-ai/app`
- `git diff --check`

## 交付说明

完成后说明：

- LangGraph 默认入口落点。
- Provider runtime config 如何传入和销毁。
- OpenAI-compatible 和 Anthropic Adapter 如何选择。
- 如何证明没有危险工具和密钥泄露。
