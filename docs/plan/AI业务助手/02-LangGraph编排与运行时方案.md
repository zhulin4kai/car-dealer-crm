# LangGraph 编排与运行时方案

## 关联 Spec

- docs/spec/AI业务助手/00-业务范围与边界.md

## 背景

当前目标架构统一以 LangGraph 承载 AI 编排，不保留旧编排路线选择。所有运行入口、测试和文档都必须围绕 LangGraph 唯一路径收敛。

## 目标

- LangGraph 是默认编排内核。
- `dealer-ai` 只暴露稳定内部事件，不暴露 LangGraph 内部类型。
- 编排节点只能使用 Provider Adapter、ToolClient、ToolSchemaRegistry。
- 高风险业务动作只能生成提醒或低风险提议，不直接执行。
- 编排结果可被 Spring Boot 转换为 SSE、trace、Proposal、Workflow 和主动事件。

## 编排模型

LangGraph 应包含固定节点：

- 输入规整节点。
- Provider 配置校验节点。
- 工具候选节点。
- 工具执行节点。
- Proposal 生成节点。
- 用户确认等待节点。
- 工作流状态节点。
- 模型摘要节点。
- 失败处理节点。

模型不得动态创建任意节点、任意工具或任意外部请求。

## 运行时输入

Spring Boot 传入：

- runNo。
- 用户问题。
- 页面上下文。
- Tool Schema。
- 是否允许低风险 Proposal。
- Provider runtime config。

`dealer-ai` 不能接收用户权限、角色、数据范围、审计操作者等可信上下文字段作为工具参数。

## 事件输出

`dealer-ai` 输出内部稳定事件：

- run started。
- message delta。
- tool call started。
- tool call completed。
- proposal created。
- workflow started。
- workflow step started。
- workflow waiting user confirmation。
- workflow step completed。
- workflow failed。
- workflow completed。
- run completed。
- error。

Spring Boot 负责转换为外部 SSE 和 trace。

## 风险与约束

- LangGraph 类型不得进入 Spring Boot 对外 API、数据库、前端类型或 OpenAPI。
- Provider 原始响应不得进入事件 payload。
- 工具失败不能伪装成功。
- 用户确认节点不得在 `dealer-ai` 内部执行业务写入。

## 验收标准

- 不存在目标架构下的 `simple/langgraph` 路线选择。
- LangGraph 编排能调用 mock Provider 和 mock ToolClient。
- 危险工具名进入图时被拒绝。
- 内部事件能被 Spring Boot 转换、持久化和恢复。
- Provider 配置错误能返回稳定错误。
