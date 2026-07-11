# AI 业务助手文档总览

## 定位

AI 业务助手是汽车经销商 CRM 的整体业务辅助能力，用于在不替代普通业务流程的前提下，提供业务查询、上下文摘要、工具调用、低风险提议、受控工作流、主动提醒、周期性摘要和前端 AI 工作台。

AI 不是业务真源，不是普通业务页面的替代入口，也不直接执行交易、资金、票据、库存、交付、客户合并、删除等高风险业务事实。所有业务写入、权限、数据范围、事务、状态机和审计仍由 Spring Boot 业务系统控制。

## 文档链路

业务规格：

- docs/spec/AI业务助手/00-业务范围与边界.md

落地计划：

- docs/plan/AI业务助手/00-AI助手整体架构与边界方案.md
- docs/plan/AI业务助手/01-模型供应商配置与密钥治理方案.md
- docs/plan/AI业务助手/02-LangGraph编排与运行时方案.md
- docs/plan/AI业务助手/03-ToolRegistry与业务工具治理方案.md
- docs/plan/AI业务助手/04-Proposal工作流与人工确认方案.md
- docs/plan/AI业务助手/05-运行追踪审计与数据保留方案.md
- docs/plan/AI业务助手/06-主动提醒与周期性摘要方案.md
- docs/plan/AI业务助手/07-前端AI工作台与管理入口方案.md
- docs/plan/AI业务助手/08-现有AI实现审查与收敛方案.md
- docs/plan/AI业务助手/09-AI会话上下文管理方案.md
- docs/plan/AI业务助手/10-AI运行策略与消息版本治理方案.md

执行任务：

- docs/task/AI业务助手/00-任务总览与文件所有权.md
- docs/task/AI业务助手/01-现有AI实现审查任务.md
- docs/task/AI业务助手/02-AI模型供应商配置管理任务.md
- docs/task/AI业务助手/03-dealer-ai LangGraph编排任务.md
- docs/task/AI业务助手/04-SpringBoot AI运行控制任务.md
- docs/task/AI业务助手/05-ToolRegistry与业务工具任务.md
- docs/task/AI业务助手/06-Proposal工作流确认任务.md
- docs/task/AI业务助手/07-主动提醒摘要任务.md
- docs/task/AI业务助手/08-前端AI工作台与配置管理任务.md
- docs/task/AI业务助手/09-端到端验收与文档同步任务.md
- docs/task/AI业务助手/10-AI会话管理落地任务.md
- docs/task/AI业务助手/11-AI运行策略与消息版本任务.md

## 整体架构决策

- 前端只调用 Spring Boot 的 `/api/ai/**`，不得直连 `dealer-ai`。
- Spring Boot 是 AI 业务控制面，负责登录用户、权限、数据范围、Provider 配置、ToolRegistry、Proposal、Workflow、主动提醒、追踪和审计。
- `dealer-ai` 是 AI 编排面，负责 LangGraph 图编排、模型供应商适配、工具选择和内部事件输出。
- LangGraph 是默认编排内核；目标架构不保留 `simple/langgraph` 双路线。
- Provider 配置由管理员在系统内维护，`dealer-ai` 不以本地 env 作为正式模型来源。
- Spring Boot 调用 `dealer-ai` 时必须下发本次 Run 的 `providerRuntimeConfig`。
- Spring Boot 是 Conversation、Run、Message 和 Run trace 的唯一真源；Conversation 是多轮业务对话容器，Run 是 Conversation 中的一次执行。
- `providerRuntimeConfig` 只在服务间请求中存在，不进入前端响应、SSE、Run trace、日志、Prompt 或模型上下文。
- Provider 协议格式固定支持 `OPENAI_COMPATIBLE` 和 `ANTHROPIC`。
- DeepSeek 是 OpenAI-compatible 的一个配置实例，不是系统唯一模型供应商。
- API Key 加密入库，响应只返回 `hasApiKey` 和 `maskedApiKey`，永远不返回明文。
- 管理员可以创建、测试、启用、停用和轮换 Provider 配置；同一时间只有一个启用配置。
- Provider 配置之外存在独立的全局 AI 运行策略，管理员可以收紧工具、Proposal、安全、联网、上下文和运行时限。
- 同一 Run 只能从 `CREATED` 原子启动一次；SSE 事件由 Spring Boot 持久化并支持按序号重放，重新订阅不会重新调用模型或工具。

## 核心能力

- AI 工作台：顶部入口、右下角悬浮入口、右侧栏、独立 AI 页面和上下文推荐问题。
- 多轮会话：同一业务对象默认复用一个活跃 Conversation，无业务对象时使用通用 Conversation；用户可新建、重命名、归档会话，并编辑或撤回自己的消息。
- 业务对话：围绕客户、线索、商机、跟进、试驾、报价、交易、收款、退款、发票、库存、交付和经营分析回答问题。
- 业务工具：通过 Spring Boot ToolRegistry 调用白名单工具，工具参数和结果必须结构化、限量、脱敏。
- 上下文分支：编辑或撤回让原轮次及后续轮次退出活动上下文，旧 Run 仍可审计，已执行的业务事实不回滚。
- 低风险 Proposal：只允许生成需要用户确认的沟通记录和跟进任务等低风险提议，确认前不产生业务写入。
- 受控工作流：通过 LangGraph 编排多步骤计划、工具调用、等待确认、失败处理、恢复和最终结果。
- 主动提醒：支持跟进提醒、异常交易提醒、库存预警、每日摘要和周期性销售分析。
- 运行追踪：记录 Conversation、Run、Message、ToolCall、Proposal、Approval、Workflow、主动事件和执行结果。
- 审计治理：AI 写操作必须进入现有操作审计，AI 追踪不得替代业务审计。

## 安全红线

- `dealer-ai` 不连接数据库、Redis 会话或 Mapper。
- `dealer-ai` 不接收浏览器请求，不保存用户 Bearer Token，不保存 API Key。
- 禁止任意 SQL、通用 HTTP 工具、文件写入和 Shell 工具。
- Provider 地址只能来自管理员保存的配置，不能来自用户输入、模型输出或工具参数。
- 工具参数不得包含 `userId`、角色、权限、数据范围、审计操作者等可信上下文字段。
- Spring Boot 内部 Tool API 必须根据 Run owner 恢复业务用户上下文。
- 模型可见工具固定为管理员允许、ToolRegistry 注册和 Run owner 当前权限的交集；内部执行时再次校验策略、权限、数据范围、Run 状态和调用次数。
- 模型上下文只能由 Spring Boot 下发脱敏会话摘要、最近用户可见消息和当前业务对象引用；禁止把权限、数据范围、API Key、Provider runtime config 或原始 JSON 放入模型上下文。
- 禁止以服务账号、管理员账号或 `dealer-ai` token 自身权限执行业务工具。
- 模型输出不能作为可信业务参数直接落库。
- 模型供应商原始响应、Token、Cookie、Authorization Header、API Key、内部堆栈和数据库错误不得进入前端响应、SSE、trace 或日志。

## 业务值来源优先级

1. 后端 enum、DTO、Service 校验、数据库约束是可执行真源。
2. OpenAPI 必须与后端可执行真源一致；不一致时视为契约错误。
3. 前端类型必须从 OpenAPI 或后端稳定枚举同步。
4. `dealer-ai` 只能使用“后端已接受 + OpenAPI 已声明”的交集值。
5. 如果后端、OpenAPI、前端、`dealer-ai` 之间不一致，禁止在 `dealer-ai` 侧临时补字符串，必须先修后端契约和文档。

## 真实模型测试约束

- 真实模型调用只能在 mock、单元、集成、前端和契约验证基本通过后执行。
- 真实调用必须限次数、限 token、限超时、禁止循环和并发重试。
- 跑出一个有效结果就停止。
- 真实 API Key 只能出现在本地未跟踪 env 文件或运行环境中，不得进入 Git、文档、日志、trace、测试快照或前端代码。

## 当前实现基线

- `dealer-ai/app/api/routes/runs.py` 固定创建 `LangGraphAgentOrchestrator`，不保留 simple/langgraph 双运行路线。
- `dealer-ai/app/core/config.py` 只保留服务运行参数和 Tool API 配置，正式 Provider 配置由 Spring Boot 下发 `providerRuntimeConfig`。
- OpenAI-compatible 和 Anthropic Provider Adapter 从 Run 级 `providerRuntimeConfig` 读取模型配置。
- `DealerAiRunRequest` 和 `ChatRunRequest` 已包含 Provider runtime config 服务间契约。
- `DealerAiRunRequest` 和 `ChatRunRequest` 同时携带本次 Run 的全局策略快照；`dealer-ai` 固定多节点图执行计划、工具、确认或总结、失败和完成节点。
- Spring Boot AI 模块已提供 Provider 配置表、加密服务、管理 API、权限码和前端配置入口。
- Spring Boot 已提供 AI 策略、不可变消息修订、活动上下文分支和 Run 事件重放表。
- 前端 AI 模块以 Conversation turns 恢复多轮消息、动态业务区块、Proposal、Workflow 和处理过程；工具结果来自实时 SSE payload 或持久化 `displayPayload`，不能依赖页面临时内存或回退显示原始 JSON。
