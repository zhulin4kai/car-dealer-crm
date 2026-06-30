# Proposal 工作流确认任务

## 任务目标

整理低风险 Proposal、人工确认、幂等执行、Workflow 等待确认节点和前端确认卡片，保证模型不能直接写入业务事实。

## 前置阅读

- docs/plan/AI业务助手/04-Proposal工作流与人工确认方案.md
- docs/plan/AI业务助手/05-运行追踪审计与数据保留方案.md
- docs/rule/13-AI业务助手开发规范.md

## 允许修改范围

- dealer-server/src/main/java/com/autodealer/crm/ai/proposal/
- dealer-server/src/main/java/com/autodealer/crm/ai/workflow/
- dealer-server/src/main/java/com/autodealer/crm/ai/service/impl/AiProposalServiceImpl.java
- dealer-server/src/main/java/com/autodealer/crm/ai/service/impl/AiWorkflowServiceImpl.java
- dealer-web/src/modules/ai/components/AiProposalCard.vue
- dealer-web/src/modules/ai/components/AiWorkflowPanel.vue
- dealer-web/src/modules/ai/model/ai.types.ts
- dealer-web/src/modules/ai/api/ai-api.ts

## 禁止修改范围

- 不得让前端提交业务参数覆盖 `normalizedParams`。
- 不得让等待确认节点执行业务写入。
- 不得把高风险业务动作降级为低风险 Proposal。

## 执行步骤

1. 校验 Proposal 状态流转。
2. 校验参数哈希和幂等。
3. 校验并发确认。
4. 校验权限变化、数据范围变化和业务状态变化。
5. Workflow 等待确认节点只输出事件。
6. 前端确认卡片只提交 proposalNo 和 decision。
7. 补齐 Run trace 恢复 Proposal、Approval、Workflow 和 ExecutionEvent。

## 完成条件

- 未确认不写业务。
- 确认时不重新请求模型生成参数。
- 重复确认不重复写入。
- 失败可追踪。
- 前端刷新后状态可恢复。

## 验证命令

- `cd dealer-server && ./mvnw test`
- `cd dealer-web && npm run test`
- `rg -n "normalizedParams|paramsHash|proposalNo|decision" dealer-server/src dealer-web/src docs`

## 交付说明

完成后说明：

- Proposal 状态流转。
- 幂等和并发确认方式。
- Workflow 如何等待用户确认。
