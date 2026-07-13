# API 统一约定

## 文档分工

- `docs/api/openapi.yaml` 记录当前对外接口契约：路径、方法、参数、请求体、返回体和错误响应。
- `docs/api/api-conventions.md` 记录所有接口必须遵守的统一规则，不逐个重复接口定义。

## 路径规则

- 所有业务接口必须使用 `/api` 前缀。
- 新增接口资源名使用小写复数名词，例如 `/api/customers`、`/api/products`、`/api/transactions`。
- 资源详情使用路径 ID，例如 `GET /api/customers/{id}`。
- 状态迁移、审批、退款、刷新缓存等命令使用明确子资源，例如 `PUT /api/transactions/{id}/approve`。
- 新接口禁止使用实现细节、中文、动词堆叠或大小写混用路径。
- 旧接口不得继续保留兼容路径；确认替代契约后必须同步删除 Controller、前端调用、OpenAPI、测试和联调文档。

## HTTP 方法

- `GET` 只用于查询，不改变业务状态。
- `POST` 用于创建资源、导入文件、批量命令或业务命令；涉及收款、退款、导入、转换等重复提交风险时必须提供客户端幂等键、外部参考号或服务端可稳定推导的幂等键。
- `PUT` 用于整体更新、状态迁移或明确命令。
- `PATCH` 只用于局部更新，必须明确字段缺省与显式 `null` 的语义。
- `DELETE` 用于删除、关闭、停用或作废入口；业务上不能物理删除的对象必须在后端转为关闭、停用或作废。

## 认证与权限

- 受保护接口统一使用 `Authorization: Bearer <token>`。
- Token 禁止出现在 URL、请求体、日志、错误响应或下载文件名中。
- 未登录、Token 无效或 Token 过期返回 `401`。
- 已登录但功能权限或数据权限不足返回 `403`。
- 新 JWT 只携带 `userId`、不可猜测 `sessionId`、`authVersion`、`iat` 和 `exp`；受保护请求必须同时满足签名、Redis 会话摘要、数据库会话事实、账号状态和认证安全版本校验。
- 数据库 `t_user_session` 是会话撤销、空闲期限和绝对期限的权威事实；Redis 只保存按 `sessionId` 精确定位的摘要缓存和用户会话索引，禁止扫描或模式删除 Key。
- 缺少 `sessionId` 的旧 JWT 仅在显式配置的兼容截止时间之前按旧 Redis 精确键校验；默认禁用旧 Token 兼容，超过截止时间一律返回 `401`。
- 登录签发以数据库会话事实、Redis 会话摘要和登录审计全部成功为前提；任一步失败都不得返回 JWT。
- 普通登出只撤销当前 `sessionId`，不递增 `authVersion`，不影响其他设备。密码修改、账号状态、角色、权限和任职等安全事实变化必须在业务事务内递增 `authVersion` 并撤销该用户全部活动会话事实；提交后按已知 `sessionId` 精确清理 Redis，清理失败不得恢复旧 Token 的有效性。
- 当前用户可管理自己的设备会话；管理者只有同时满足管理链、组织范围和相应权限时，才能查看或撤销下属会话。用户不能用会话接口修改自己的权限。
- 前端权限只用于菜单、按钮和页面展示，不能作为安全边界。

## 请求格式

- JSON 接口默认使用 `Content-Type: application/json`。
- 文件上传使用 `multipart/form-data`，字段名、大小限制、文件类型和部分失败语义必须写入 `openapi.yaml`。
- 业务写接口必须使用用途明确的 Request DTO；表单编码仅限登录、文件上传等已明确的基础设施入口。
- 客户端不得提交可信操作人、数据权限范围、汇总金额、服务端状态和审计字段。
- 金额使用十进制字符串或 `number`，后端按 `BigDecimal` 接收，禁止使用浮点近似语义。
- 枚举字段必须在 `openapi.yaml` 写清允许值；不能让前端依赖中文文案判断业务。
- 库存车辆实例状态、库存流水类型、交易阶段、收款退款类型等业务枚举必须作为稳定编码传输；前端只负责把稳定编码映射为中文展示。
- 审计日志的 `result`、`reasonCode` 和 `actionCode` 是稳定编码；前端必须映射为中文展示，不能把内部编码原样作为主要用户文案。

## 查询与分页

- 列表接口统一使用 `page` 和 `size`。
- 旧分页参数 `current` 和 `pageSize` 不再作为业务接口契约保留；业务列表请求只能使用 `page`/`size`。
- 页码从 `1` 开始。
- 默认页大小为 `10`，当前统一上限为 `100`；超过后端限制时返回 `400`。
- 筛选条件使用查询参数；复杂查询应使用结构化 Query DTO，但字段仍必须可在契约中说明。

## 响应格式

当前 JSON API 统一使用 `R` envelope：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

- `code` 是业务结果码，类型为整数。
- `msg` 是用户可见提示文案，前端不得用中文文案驱动业务分支。
- `data` 是业务数据；无数据成功响应可以省略或为 `null`。
- 文件下载不套 `R` envelope，必须返回正确的 `Content-Type`、`Content-Disposition` 和文件名。
- 新接口不得新增另一套 `message`、`result`、`success` envelope。
- 发票、税务和票据类响应中的税号、银行账号、地址、电话等敏感字段必须由后端按权限脱敏；前端不得通过隐藏列或本地规则替代后端脱敏。
- 客户列表、详情、选项和导出必须以客户主档字段为准，不得通过线索联表返回客户姓名、联系方式、来源或负责人。
- 客户手机号、微信、QQ、邮箱、地址等敏感字段必须由后端按 `customer:sensitive:view` 权限脱敏。
- 客户重复检查命中时返回 HTTP 409 和稳定 code `DUPLICATE`；可见范围内只返回脱敏客户摘要，越权重复只返回 `hiddenConflict=true`。
- 客户存在关联线索、跟进、报价、交易等业务关系时，删除返回 HTTP 422 和稳定 code `RESOURCE_IN_USE`，不得物理删除历史。

## 分页响应

当前分页响应使用 PageHelper `PageInfo`：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "pages": 10,
    "list": []
  }
}
```

- 新增接口如需采用 `records/total/page/size`，必须先完成统一迁移方案，不得和 `PageInfo` 在同一模块随意混用。
- `PageInfo` 响应必须至少包含 `pageNum`、`pageSize`、`total`、`pages` 和 `list`，前端共享 `PageResult<T>` 必须把这些字段声明为必填。
- 空列表返回 `[]`，不得返回 `null`。

## HTTP 状态与业务码

- HTTP 状态表示协议层结果，业务 `code` 表示可识别业务原因。
- `200`：查询、创建、更新、删除或命令成功。
- `400`：请求参数错误、JSON 格式错误、Bean Validation 失败。
- `401`：未认证、Token 无效、Token 过期。
- `403`：功能权限或数据权限不足。
- `404`：允许披露的资源不存在。
- `409`：唯一约束、重复提交、并发版本、非法状态迁移。
- `422`：请求格式正确但业务对象无法处理，例如导入部分失败、资源被引用、可退金额不足。
- `500`：未预期系统异常。

## 错误响应

失败响应仍使用 `R` envelope：

```json
{
  "code": 501,
  "msg": "请求参数格式有误",
  "data": null
}
```

- 稳定错误原因必须定义在后端 `CodeEnum`。
- 账号凭证生命周期使用 `620` 凭证无效、`621` 凭证过期、`622` 已使用或撤销、`623` 密码策略不满足、`624` 近期密码重复、`625` 投递失败；任何错误响应均不得携带原始凭证、密码或摘要。
- 资料与账号并发冲突分别使用 `626` 和 `627`，均返回 HTTP `409`。
- 会话生命周期使用 `631` 会话不存在、`632` 已撤销、`633` 已过期、`634` 会话版本冲突、`635` 会话缓存失败；对应 HTTP 状态依次为 `404`、`409`、`410`、`409`、`503`。
- 用户管理初始化门禁使用 `641`，表示首个真实任职普通管理员尚未完成初始化；受保护恢复账号试图进入日常业务接口时使用 `642`。两者均返回 HTTP `403`，前端必须按稳定业务码展示引导或阻断，不得按中文文案分支。
- 人员生命周期使用 `636` 状态或版本冲突、`637` 预检快照过期、`638` 接收资格漂移、`639` 交接计数不一致、`640` 试驾排期冲突；除 `637` 返回 HTTP `410` 外，其余均返回 HTTP `409`。
- 用户工作台非法筛选、未知排序列、非法排序方向和超过 100 的 page size 返回 `400`；账号版本冲突使用 `627/409`。不存在用户返回 `404`，存在但不在管理范围或属于受保护目标返回 `403`。
- 本人资料和受管资料分别使用 `/api/profile` 与 `/api/users/{id}/profile`；两者都以 `profileVersion` 做 CAS，受管资料仅接受姓名、电话、邮箱，不得复用授权或任职 DTO。
- 同一业务原因在不同接口必须使用同一 `code`。
- 前端业务分支只能依赖 HTTP 状态和 `code`。
- `msg` 可以优化文案，但不能改变 `code` 的业务含义。
- 错误响应不得暴露 SQL、堆栈、内部类名、Redis Key、Token、密码或权限实现细节。
- 数据字典删除、停用或改码冲突必须返回稳定业务 `code`：被业务引用或内置保护使用 `RESOURCE_IN_USE`/422；`typeCode`、`valueCode` 是稳定编码，更新接口不得把它们作为改码入口。

## AI 业务助手接口约定

- 浏览器只调用 Spring Boot 的 `/api/ai/**` 接口，不直接调用独立 AI 服务。
- AI Conversation 是多轮业务对话容器，AI Run 是一次执行；前端不得把单次 Run trace 当成会话恢复。
- AI Conversation 查询、新建、重命名和归档统一使用 `/api/ai/conversations/**`。
- 前端发送问题时应携带 `conversationNo`；未携带时 Spring Boot 按当前用户和业务对象上下文解析或创建默认 Conversation。
- AI Run 创建、查询和 Proposal 确认接口继续使用 `R` envelope。
- AI Run 刷新恢复通过 `GET /api/ai/runs/{runNo}/trace` 查询，返回单次 Run 元数据、消息摘要、工具调用摘要、Proposal、Approval、执行事件和受控工作流。
- AI Conversation 刷新恢复通过 `GET /api/ai/conversations/{conversationNo}` 查询，返回会话、消息、最近 Run、兼容 Run trace 和按轮次组织的 `turns`。
- `turns` 是 Conversation 恢复主契约，每个 turn 必须包含对应 Run、用户消息、AI 回答、工具结果、Proposal、Workflow、Approval 和 ExecutionEvent。
- AI ToolCall trace 必须返回脱敏展示字段 `displayPayload`，用于前端切换会话和刷新后恢复业务卡片；只返回 `outputSummary` 不满足恢复契约。
- AI SSE 只通过 `GET /api/ai/runs/{runNo}/events` 输出 `text/event-stream`，事件 data 为 `AiSseEventResponse` JSON，不返回模型供应商原始响应、Token、Cookie、Authorization Header、内部堆栈或数据库错误细节。
- AI SSE 必须是真流式，Spring Boot 逐帧接收 `dealer-ai` 内部 SSE 并立即转发给浏览器；不得先等待完整模型响应再一次性返回。
- 前端停止生成必须先中断当前 SSE fetch，再调用 `POST /api/ai/runs/{runNo}/cancel`。用户主动停止后的 Run 状态为 `CANCELLED`，已生成 assistant 文本保留为部分结果，不按系统错误提示。
- 当前已实现 SSE 事件类型为 `run_started`、`message_delta`、`message_completed`、`tool_call_started`、`tool_call_completed`、`proposal_created`、`workflow_started`、`workflow_step_started`、`workflow_step_completed`、`workflow_waiting_user_confirmation`、`workflow_paused`、`workflow_resumed`、`workflow_cancelled`、`workflow_expired`、`workflow_failed`、`workflow_completed`、`error`、`run_completed` 和 `run_cancelled`。
- Provider runtime config 只允许存在于 Spring Boot 调用 `dealer-ai` 的服务间请求中，不得进入前端响应、SSE payload、trace、日志、OpenAPI 前端 schema 或测试快照。
- Provider API Key 由 Spring Boot 使用 AES-GCM 加密入库，响应只返回 `hasApiKey` 和 `maskedApiKey`；前端不得保存、展示或日志输出明文 API Key。
- Spring Boot 内部 Tool API 使用 `/internal/ai/tools/{toolName}/execute`，只允许 `dealer-ai` 使用 `X-Dealer-AI-Tool-Token` 调用；该接口不得暴露给浏览器作为普通业务 API。
- Tool API 请求体只允许 `runNo` 和 `arguments`；`arguments` 不得包含 `userId`、角色、权限、数据范围、组织范围、审计操作者等可信上下文字段。
- ToolRegistry 的 Java DTO 和 Bean Validation 是最终输入校验，`dealer-ai` 的 Pydantic 校验只作为模型侧边界。
- AI 工具参数和 Proposal 参数中的 CRM 业务枚举、状态、类型和值必须来自 Spring Boot Java enum、后端 DTO 校验和 OpenAPI enum，`dealer-ai` 不得提交临时发明的业务值。
- ToolCall 成功和失败都必须写入可恢复 trace；Run trace 返回的 toolCalls 只能包含脱敏输入摘要、脱敏输出摘要、对象引用、结果、耗时和错误码。
- Spring Boot 调用 `dealer-ai` 的服务间请求可以包含 `conversationNo`、`conversationSummary` 和最多最近 8 条用户可见 `messageHistory`；这些字段必须先脱敏和限量。
- 低风险 Proposal 确认只允许提交 Proposal ID，不允许前端提交新的业务参数覆盖后端已保存参数。
- AI Proposal 确认决定使用 `CONFIRMED`、`REJECTED`、`EXPIRED`，禁止复用交易审批的 `APPROVED` 语义。
- Proposal 状态、Approval decision、数据库约束、Java enum、OpenAPI enum 和前端类型必须一致；确认、拒绝、过期、权限变化、哈希不一致和业务执行失败都必须可追踪。
- 受控工作流接口只允许编排 Spring Boot 下发的只读工具和低风险 Proposal；暂停、恢复、取消和失败都必须由 Spring Boot 校验 run owner、权限、数据范围和当前状态。前端不得公开“完成工作流”入口。
- 主动提醒接口只对当前用户订阅生效；生成提醒前必须重新校验用户启用状态、权限、数据范围、频率、数量上限、静默时间和重复合并规则。
- 已实现 AI 稳定错误码包括 `AI_RUN_NOT_FOUND`、`AI_RUN_FINISHED`、`AI_SSE_FAILED`、`AI_PROVIDER_FAILED`、`AI_TOOL_NOT_FOUND`、`AI_TOOL_FORBIDDEN`、`AI_TOOL_ARGUMENT_INVALID`、`AI_PROPOSAL_EXPIRED`、`AI_PROPOSAL_HASH_MISMATCH`、`AI_WORKFLOW_NOT_FOUND`、`AI_WORKFLOW_STATE_CONFLICT`、`AI_PROACTIVE_SUBSCRIPTION_NOT_FOUND`、`AI_PROACTIVE_EVENT_NOT_FOUND`、`AI_PROACTIVE_STATE_CONFLICT`、`AI_PROACTIVE_FORBIDDEN`、`AI_PROVIDER_CONFIG_NOT_FOUND`、`AI_PROVIDER_CONFIG_DISABLED`、`AI_PROVIDER_CONFIG_REQUIRED`、`AI_PROVIDER_CONFIG_TEST_FAILED`、`AI_PROVIDER_KEY_ENCRYPTION_FAILED`、`AI_PROVIDER_KEY_DECRYPTION_FAILED`、`AI_RUN_CANCELLED`、`AI_RUN_CANCEL_CONFLICT`、`AI_DEALER_AI_UNAVAILABLE` 和 `AI_PROVIDER_UNSUPPORTED_FORMAT`。

## 时间格式

- 新增或重构接口的目标时间格式为 ISO-8601 带时区字符串，例如 `2026-06-24T10:30:00+08:00`。
- 仅表达业务日期而非具体时刻的字段使用 `date` 格式 `yyyy-MM-dd`，例如商机预计成交日期和下一步日期；不得用交付时间、开票时间等 `date-time` 语义替代。
- 试驾预约开始/结束、签到到店、实际开始/结束等具体时刻使用 `date-time`；不得降级为 `date` 或中文时间文本。
- 跟进任务计划时间、提醒时间、完成时间、沟通时间和下一次跟进时间使用 `date-time`；接口兼容 ISO 本地时间和 `yyyy-MM-dd HH:mm:ss`，前端模块类型按 ISO 字符串建模。
- 市场活动开始/结束、复盘时间等具体时刻使用 `date-time`；当前兼容格式为 `yyyy-MM-dd HH:mm:ss`，前端须通过统一时间工具从 `datetime-local` 转换后提交。
- 促销政策开始/结束时间使用 `date-time`；当前兼容格式为 `yyyy-MM-dd HH:mm:ss`，前端须通过统一时间工具从 `datetime-local` 转换后提交。促销状态只允许稳定 code，中文仅作为展示 label。
- 历史字段如果仍返回 `yyyy-MM-dd HH:mm:ss`，必须在 `openapi.yaml` 标注当前兼容格式。
- 服务端请求体和查询参数解析必须同时兼容 ISO-8601 带时区、ISO 本地时间、`yyyy-MM-dd HH:mm:ss` 和日期-only；旧格式仅用于兼容，不得作为新增接口的唯一格式。
- 同一接口的请求时间和响应时间格式必须一致。
- 前端不得在页面内临时猜测时间格式，必须通过模块 API 或统一工具转换。

## 破坏性变更规则

- 删除字段、重命名字段、改变类型、改变枚举含义、改变默认分页参数都属于破坏性变更。
- 破坏性变更必须同时更新 `openapi.yaml`、前端模块 API、TypeScript 类型、测试和联调说明。
- 旧接口迁移完成后必须删除旧路径，不使用 `deprecated` 兼容窗口承载业务流量。
- 当前正式替代路径包括：`GET /api/activities` 替代旧 `/api/activitys`，`POST /api/transactions` 替代旧 `/api/tran/create`，`GET /api/customers` 替代旧 `/api/customer/list`。
- 删除旧接口时必须补充契约测试，证明旧路径不再由 Controller 暴露，防止旧路径绕过权限、状态机或数据校验。

## 更新要求

- 新增接口：先更新 `openapi.yaml`，再实现 Controller、前端 API 和测试。
- 修改请求字段：同步 Request DTO、前端类型、`openapi.yaml` 和契约测试。
- 修改响应字段：同步 Response DTO、前端类型、页面消费点、`openapi.yaml` 和契约测试。
- 修改错误码：同步 `CodeEnum`、前端错误码映射、`openapi.yaml` 和错误响应测试。
- 修改认证、权限或数据范围：同步规则文档、接口契约和未登录/无权限/越权场景测试。
