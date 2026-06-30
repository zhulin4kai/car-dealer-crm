# ToolRegistry 与业务工具任务

## 任务目标

整理 Spring Boot ToolRegistry 和 `dealer-ai` ToolSchemaRegistry，保证工具白名单、权限、风险等级、输入输出边界和 ToolCall trace 与整体 AI 架构一致。

## 前置阅读

- docs/plan/AI业务助手/03-ToolRegistry与业务工具治理方案.md
- docs/rule/13-AI业务助手开发规范.md

## 允许修改范围

- dealer-server/src/main/java/com/autodealer/crm/ai/Tool*.java
- dealer-server/src/main/java/com/autodealer/crm/ai/tool/
- dealer-server/src/main/java/com/autodealer/crm/ai/dto/tool/
- dealer-ai/app/tools/
- dealer-ai/app/schemas/tools.py
- dealer-server/src/test/java/com/autodealer/crm/ai/
- dealer-ai/tests/

## 禁止修改范围

- 不得新增危险工具。
- 不得让 `dealer-ai` 调普通业务 API。
- 不得绕过 Spring Boot 权限和数据范围。

## 执行步骤

1. 审查现有工具定义。
2. 明确工具风险等级、权限码、输入、输出、最大结果数和确认要求。
3. 拒绝可信上下文字段。
4. 工具执行统一写 ToolCall trace。
5. `dealer-ai` 只保留模型侧工具 Schema。
6. 补齐工具成功、失败、越权、参数污染、危险工具扫描测试。

## 完成条件

- ToolRegistry 是最终工具白名单。
- ToolSchemaRegistry 不作为权限来源。
- 工具结果结构化、脱敏、限量。
- 前端可恢复展示工具结果。

## 验证命令

- `cd dealer-server && ./mvnw test`
- `cd dealer-ai && uv run pytest`
- `rg -n "run_sql|http_request|file_write|shell_exec" dealer-server/src dealer-ai/app`

## 交付说明

完成后说明：

- 工具清单。
- 权限和数据范围如何恢复。
- 危险工具如何被阻断。
