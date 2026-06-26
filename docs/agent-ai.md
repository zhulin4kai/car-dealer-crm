# 插入式 Agent AI 能力总览

## 当前系统现状

Car Dealer CRM 是前后端分离的汽车经销商客户关系管理系统，当前主干能力覆盖从线索进入到交易完成的业务闭环。

已从仓库确认的技术结构：

- 后端位于 `dealer-server/`，使用 Spring Boot 3.2、Spring Security、JWT、Redis、MyBatis、PageHelper、MariaDB/MySQL。
- 后端 Java 包 `com.autodealer.crm` 已按 `web`、`service`、`service.impl`、`mapper`、`model`、`dto`、`query`、`result`、`config`、`aspect`、`audit`、`manager` 分层。
- 前端位于 `dealer-web/`，使用 Vue 3、TypeScript、Vite、Pinia、Vue Router、Axios、shadcn-vue/reka-ui、Tailwind CSS。
- 前端已按 `src/pages`、`src/modules`、`src/shared`、`src/stores`、`src/router`、`src/components/ui` 分层，模块 API 通过 `src/shared/api/http-client.ts` 统一调用后端。
- API 统一使用 `/api` 前缀、`Authorization: Bearer <token>`、`R` 响应 envelope 和 PageHelper `PageInfo` 分页结构。

已从代码和文档确认的业务模块：

- 销售获客与客户经营：市场活动、线索、客户、商机、跟进任务、沟通记录、试驾。
- 车辆与库存：车辆商品、商品分类、促销政策、库存预警、库存车辆实例、库存流水。
- 成交履约：报价、交易、审批、结算、收款、退款、发票、交付。
- 管理治理：用户、角色、权限、数据字典、登录记录、操作审计、经营统计。

已从代码和数据库脚本确认的安全与治理能力：

- Spring Security 负责认证入口和受保护请求拦截，`TokenVerifyFilter` 校验 JWT、Redis 会话和账号状态。
- `PermissionCodes` 定义稳定权限编码，Controller 使用 `@PreAuthorize` 执行后端功能授权。
- 前端使用 `router` 的 `meta.permission` 和 `v-has-permission` 控制菜单、页面和按钮展示，但不作为安全边界。
- `CurrentUserProvider` 统一提供当前用户、当前用户 ID、管理员判断和数据范围上下文。
- `DataScopeAspect` 与 Mapper 查询参数共同执行数据范围过滤，普通用户按负责人等数据范围受限。
- `OperationAuditRecorder` 写入 `t_operation_log`，`AuditActionEnum` 定义用户、线索、客户、交易、支付、退款、发票、库存、交付、审计导出等审计动作。
- 数据库脚本包含 `t_clue`、`t_customer`、`t_opportunity`、`t_follow_task`、`t_communication_record`、`t_test_drive`、`t_quote`、`t_tran`、`t_tran_approve`、`t_tran_invoice`、`t_payment`、`t_refund_request`、`t_product`、`t_product_vehicle`、`t_product_stock_record`、`t_delivery`、`t_operation_log`、`t_login_log`、`t_permission`、`t_role`、`t_user` 等核心表。

当前仓库未确认到以下 AI 相关实现：

- 未发现 `dealer-ai/` 独立 Agent 服务目录。
- 未发现 `/api/ai/*` 或 `/api/ai-tools/*` 后端接口。
- 未发现 `docs/api/openapi.yaml` 中已有 AI 接口契约。
- 未发现 AI Run、AI Message、AI Tool Call、AI Proposal、AI Approval 等专用持久化结构。

## AI 能力定位

Agent AI 是插入式、旁路式、受控式增强能力。

普通客户管理、商品管理、交易管理、活动管理、系统管理、审计管理等现有页面和业务接口继续保持原链路：

```text
Vue 普通页面 -> 普通业务 API -> Spring Controller -> Service -> Mapper -> DB
```

AI 只通过新增 AI 入口进入旁路链路：

```text
Vue AI 入口 -> /api/ai/* -> dealer-ai -> /api/ai-tools/* -> Spring Service -> DB
```

Agent 只负责理解用户意图、选择工具、编排步骤和生成可解释结果。Spring Boot 仍是权限、数据范围、事务、业务状态机、审计和数据库访问的最终边界。

## 两阶段目标

### 第一阶段：AI Copilot / Tool Calling

第一阶段只提供只读查询和低风险辅助：

- 通过 AI 入口发起对话。
- Agent 选择白名单只读工具。
- Spring Boot 校验当前用户、权限码和数据范围。
- 工具返回专用结构化 DTO。
- 前端展示结构化结果、引用对象和后续可点击操作。
- 不直接执行高风险写操作。

首批能力应优先贴合当前真实业务对象：今日跟进、客户搜索、客户摘要、车辆商品解析、库存预警、交易详情、待审批交易等。

### 第二阶段：可控 Agent

第二阶段支持计划、确认、执行、校验和审计：

- Agent 可以生成多步骤执行计划。
- 写操作先生成 Action Proposal。
- Proposal 由 Spring Boot 保存规范化参数、参数哈希、风险等级和过期时间。
- 前端展示确认卡片。
- 用户确认后，Spring Boot 执行已保存参数。
- 执行结果写入 AI 事件和现有操作审计。
- Agent 不重新生成确认后的执行参数。

## 总体链路

### 普通业务链路

```text
dealer-web 普通页面
  -> modules/<domain>/api
  -> shared/api/http-client.ts
  -> /api/<business>
  -> Spring Controller
  -> Service / Manager
  -> Mapper / Mapper XML
  -> DB
```

保持不变的现有链路示例：

- `/dashboard/customer` 继续调用 `modules/customer/api/customer-api.ts` 和 `/api/customers`、`/api/customer/{id}`。
- `/dashboard/follow` 继续调用 `modules/follow/api/follow-api.ts` 和 `/api/follow-tasks`、`/api/communication-records`。
- `/dashboard/product/stock` 继续调用 `modules/product/api/product-api.ts` 和 `/api/products/stockalerts`、`/api/productstock/*`。
- `/dashboard/tran` 和交易详情继续调用 `modules/tran/api/tran-api.ts` 和 `/api/tran/*`、`/api/transactions`。

### AI 聊天链路

```text
dealer-web AI 入口
  -> modules/ai/api/ai-api.ts
  -> /api/ai/chat
  -> Spring AI Chat Controller
  -> AI Run / Message 持久化
  -> dealer-ai
  -> 模型调用与工具选择
```

聊天链路只接收用户输入、对话上下文和可展示结果。前端不直接连接 Python Agent 服务。

### Agent 工具调用链路

```text
dealer-ai
  -> /api/ai-tools/{toolName}
  -> Spring AI Tool Controller
  -> Tool Registry
  -> 权限 / 委托令牌 / 数据范围 / 参数校验
  -> 现有 Service / Manager
  -> Mapper / DB
  -> 专用 Tool DTO
```

Agent 不直接连接数据库，不直接调用 Mapper，不自行解释 JWT，不自行拼接普通页面接口作为主设计。

### AI 写操作确认链路

```text
用户提出写操作意图
  -> Agent 生成候选动作
  -> Spring Boot 规范化参数并保存 Action Proposal
  -> 前端展示确认卡片
  -> 用户确认或拒绝
  -> Spring Boot 按已保存参数执行业务 Service
  -> 写入 AI 执行事件和操作审计
  -> 前端展示执行结果
```

确认后不得让模型重新生成参数再执行。高风险业务动作不能由 Agent 绕过确认直接调用。

## 边界说明

### 前端边界

- 普通页面继续使用现有 `modules/<domain>/api`。
- AI 页面或组件新增 `modules/ai`，只调用 `/api/ai/*`。
- 前端展示权限仍可使用 `v-has-permission` 和路由 `meta.permission`，但后端继续做最终鉴权。
- 前端展示 Tool 结果时按结构化 JSON 渲染，不直接渲染模型拼接的 HTML。

### Spring Boot 边界

- 保留现有 Controller、Service、Mapper 和业务状态机。
- 新增 AI Chat API 给前端使用。
- 新增 AI Tool API 给 Agent 服务使用。
- Spring Boot 负责最终鉴权、数据范围、参数校验、事务、业务状态校验、DTO 脱敏、审计和错误响应。
- Spring Boot 不把所有普通业务请求转发到 AI 层。

### Python / Agent 服务边界

- Agent 服务负责模型调用、工具选择、对话状态、任务计划和编排。
- Agent 服务只能调用 Spring Boot 暴露的 `/api/ai-tools/*`。
- Agent 服务不保存业务真源数据，不直接访问数据库，不持有数据库账号。
- Agent 服务不能信任模型输出中的用户 ID、租户 ID、权限范围或数据范围。

### AI Tool API 边界

- AI Tool API 是受控工具入口，不等同于普通页面 API。
- 每个工具必须有明确权限码、风险等级、输入输出 Schema、超时、最大返回量和审计动作。
- 工具内部调用现有 Service 或新增薄适配 Service，不绕过业务规则。
- 工具返回专用 DTO，不直接返回数据库实体。

### Service / DB 边界

- Service 继续承载业务规则、状态迁移、事务和并发控制。
- Mapper 继续只负责持久化读写。
- DB 是业务事实来源，Agent 和模型都不是业务真源。
- 写操作成功必须以 Service 事务提交和审计结果为准。

## 不做什么

- 不让 Agent 直连数据库。
- 不让 Agent 直接调用 Mapper。
- 不让 Agent 绕过 Spring Security、RBAC、DataScope、Audit、事务和业务状态机。
- 不把普通业务接口全部迁移到 AI 层。
- 不把 AI 中间层设计成普通业务请求的必经网关。
- 不开放任意 SQL。
- 不开放任意 HTTP。
- 不开放文件系统访问。
- 不开放 Shell 执行。
- 不把数据库实体原样返回给模型。
- 不让模型接触 JWT、Redis 会话值、数据库账号、外部 API Key 或其他密钥。
- 不直接执行删除客户、删除交易、批量删除、审批交易、确认收款、执行退款、开票、结算等高风险动作。
- 不让模型在用户确认后重新生成执行参数。

## 文档分层

- 架构和安全约束：`docs/spec/插入式AgentAI/00-架构边界与安全规格.md`。
- 分阶段落地方案：`docs/plan/插入式AgentAI/01-插入式AgentAI落地方案.md`。
- 可执行任务拆分：`docs/task/插入式AgentAI/00-任务拆分总览.md`。
