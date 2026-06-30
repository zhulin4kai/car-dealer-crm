# Proposal 工作流与人工确认方案

## 关联 Spec

- docs/spec/AI业务助手/00-业务范围与边界.md

## 背景

AI 可以降低记录类工作成本，但不能让模型直接写入业务事实。所有低风险写入都必须先形成 Proposal，再由用户确认，确认时执行后端已保存的规范化参数。

## 目标

- Proposal 覆盖低风险记录类动作。
- 用户确认前不产生业务写入。
- 确认时不重新请求模型生成参数。
- 并发确认、重复确认、过期、权限变化、数据范围变化和业务状态变化均有明确处理。
- Proposal 与 Workflow、Run trace、前端确认卡片统一展示。

## 状态模型

- `PENDING_CONFIRMATION`。
- `CONFIRMED`。
- `REJECTED`。
- `EXPIRED`。
- `EXECUTED`。
- `FAILED`。

合法流转：

- `PENDING_CONFIRMATION -> CONFIRMED -> EXECUTED`。
- `PENDING_CONFIRMATION -> CONFIRMED -> FAILED`。
- `PENDING_CONFIRMATION -> REJECTED`。
- `PENDING_CONFIRMATION -> EXPIRED`。

## 参数规则

- `normalizedParams` 是业务执行的唯一参数来源。
- `paramsHash` 基于实际保存的 `normalizedParams` 计算。
- 前端确认请求只能提交 proposalNo 和 decision。
- `normalizedParams` 不进入前端展示、SSE 可展示 payload 或工具摘要。

## 事务与审计

- 业务写入成功后写现有操作审计。
- 业务写入失败时 Proposal 进入 `FAILED`，并可靠记录失败 ExecutionEvent。
- 已执行 Proposal 再确认必须幂等返回已执行结果，不重复写入。

## 验收标准

- 未确认 Proposal 不产生业务写入。
- 参数哈希不一致被拒绝。
- 并发确认只有一个成功。
- 已拒绝、已过期、已失败的 Proposal 不能确认。
- 前端刷新后能恢复 Proposal 状态和执行结果。
