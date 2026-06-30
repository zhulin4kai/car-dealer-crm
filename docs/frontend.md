# 前端架构说明

项目：`dealer-web`
技术栈：Vue 3 + Vite + TypeScript + Pinia + Vue Router + Element Plus + Axios + ECharts

## 架构总览

前端已经从 JavaScript 分层目录重构为“业务模块 + 共享层”的 TypeScript 架构：

```text
src/
  app/          # 应用入口、插件、全局指令
  layouts/      # 页面布局
  pages/        # 路由页面
  router/       # 路由表、守卫、meta 类型
  stores/       # Pinia 全局状态
  shared/       # HTTP、storage、utils、基础类型、通用 UI
  modules/      # activity/audit/clue/customer/dict/follow/opportunity/product/quote/statistic/test-drive/tran/user
```

旧目录 `src/view`、`src/api`、`src/http`、`src/util` 已移除。入口文件为 `src/app/main.ts`，Vite 配置为 `vite.config.ts`。

## 工程标准

核心命令：

```bash
npm run typecheck
npm run lint
npm run test
npm run build
npm run check
```

TypeScript 配置启用严格模式：

- `strict`
- `noImplicitAny`
- `noUncheckedIndexedAccess`
- `exactOptionalPropertyTypes`

测试使用 Vitest，测试入口为 `tests/setup.ts`，测试文件统一为 `.test.ts`。

## 应用入口

`src/app/main.ts` 负责：

- 创建 Vue 应用
- 注册 Pinia
- 注册 Element Plus 与图标
- 注册 `v-has-permission`
- 注册 Vue Router
- 加载全局样式 `src/assets/global.css`

Element Plus 插件位于 `src/app/plugins/element-plus.ts`，Pinia 实例位于 `src/app/plugins/pinia.ts`。

## HTTP 与 API

统一 HTTP 客户端位于 `src/shared/api/http-client.ts`。

后端响应统一建模：

```ts
interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
}
```

业务页面和模块 API 不直接使用 Axios；模块 API 调用 `httpClient` 并返回已经解包的领域数据。

分页响应统一建模：

```ts
interface PageResult<T> {
  list: T[]
  total: number
  pageSize: number
  pageNum: number
  pages: number
  size: number
}
```

HTTP 层集中处理：

- `VITE_API_BASE_URL` / 默认 `http://localhost:8089`
- token 请求头注入为 `Authorization: Bearer <token>`
- `rememberMe` 请求头
- `code !== 200` 抛出 `ApiError`
- 会话失效（code 510-513）通过 `shared/auth/session-invalid-handler.ts` 单飞处理：应用启动时注册处理器，`notifySessionInvalid` 持有 `inFlight` Promise 守卫，并发失效只触发一次 `authStore.forceLogout()` + `permissionStore.clearPermissions()` + `router.replace({ name: 'login' })`
- 业务页只能按 `ApiError.code` 和 HTTP 状态处理稳定业务分支；商品删除等引用保护场景按 `RESOURCE_IN_USE(422)` 映射提示，不解析中文 `msg` 或数据库约束文案。
- 跟进任务页面使用 `modules/follow/api/follow-api.ts` 和 `modules/follow/model/follow.types.ts`；任务状态、任务类型、对象类型、沟通方式和记录状态均按稳定英文 code 分支，中文只作为 label 展示。
- 403（code 520）和网络错误不清会话
- `ApiError` 携带 `isSessionInvalid` 标志，页面可据此跳过重复提示
- 文件下载使用 `httpClient.download()`，返回 `{ blob, filename }`，自动解析 `Content-Disposition`；Blob 错误响应转换为 `ApiError` 并进入统一会话失效流程
- `saveBlob(blob, filename)` 负责浏览器端下载触发和对象 URL 清理

## 状态管理

全局状态使用 Pinia：

- `stores/auth.store.ts`：token、rememberMe、当前用户、登录、退出、会话恢复
- `stores/permission.store.ts`：权限码、菜单权限、权限判断、权限缓存
- `stores/app.store.ts`：侧边栏折叠、当前菜单、全局 loading

token 存储策略：

- 勾选“记住我”：写入 `localStorage`
- 未勾选：写入 `sessionStorage`
- 退出登录只有在后端确认 Redis 会话删除成功后清理 token 与权限缓存；退出接口返回错误时保留本地会话并提示失败

## 路由

路由拆分为：

- `router/routes.ts`
- `router/guards.ts`
- `router/route-meta.ts`
- `router/index.ts`

现有 URL 保持兼容：

- `/`
- `/dashboard`
- `/dashboard/user`
- `/dashboard/activity`
- `/dashboard/activity/:id`
- `/dashboard/clue`
- `/dashboard/clue/detail/:id`
- `/dashboard/customer`
- `/dashboard/customer/:id`
- `/dashboard/product`
- `/dashboard/product/category`
- `/dashboard/product/promotion`：使用稳定促销状态 code 映射中文展示；表单只维护规则字段，发布、生效、暂停、结束和作废走独立状态按钮，暂停/结束/作废必须填写原因。
- `/dashboard/product/stock`
- `/dashboard/opportunity`
- `/dashboard/test-drive`
- `/dashboard/quote`
- `/dashboard/tran`
- `/dashboard/tran/:id`
- `/dashboard/tran/approve/:id`
- `/dashboard/tran/invoice/:id`
- `/dashboard/delivery`
- `/dashboard/dict/type`
- `/dashboard/dict/value`
- `/dashboard/audit/login`
- `/dashboard/audit/operation`
- `/dashboard/ai`

`DashboardLayout.vue` 只负责主框架布局、菜单、用户入口和退出登录。

`/dashboard/user` 使用 `modules/user/api/user-api.ts` 查询、新增、编辑、启禁用和交接用户责任。责任交接通过 `handoverUserResponsibilities` 提交目标负责人和交接原因，目标负责人列表来自同模块 `fetchOwnerList`；页面只按 `user.status` 权限控制按钮展示，提交期间禁用重复点击，失败按接口错误处理，不匹配中文 `msg`。

`/dashboard/clue` 使用 `modules/clue/api/clue-api.ts` 查询、创建、编辑、导入和删除线索。线索新增表单在提交前把手机号中的空格、横杠和括号归一化为 11 位规范手机号，异步查重也使用归一化值；导入错误、删除阻断等业务分支必须按稳定 code 和 HTTP 状态处理，不能匹配中文 `msg`。

`/dashboard/clue/detail/:id` 展示线索详情、跟踪记录和责任历史。转派线索必须通过 `transferClueOwner` 提交目标负责人和原因，详情页转派成功后重新加载当前负责人和 `getClueOwnerHistory` 返回的历史记录；关闭和恢复分别通过 `closeClue`、`restoreClue` 提交原因，按钮显示基于 `stateDO.valueCode` 的稳定状态 code，不匹配中文状态文本。前端权限只控制按钮显示，后端负责数据范围、目标负责人资格、状态迁移、重复活跃线索校验和并发旧状态校验。

`/dashboard/activity` 使用 `modules/activity/api/activity-api.ts` 查询活动列表、创建/编辑活动、执行发布、开始、结束、复盘、取消、关闭和导出 ROI。页面只提交 `DRAFT`、`PLANNED`、`ONGOING`、`ENDED`、`REVIEWED`、`CLOSED`、`CANCELED` 等稳定活动状态编码，中文只做展示；创建和编辑表单提交 JSON，不提交负责人、状态、复盘人或金额汇总。已结束、已复盘、已关闭和已取消活动的核心字段在页面只读，后端仍做最终锁定校验。导出按钮按 `activity:export` 显示并走 `httpClient.download()`。

`/dashboard/activity/:id` 使用 `fetchActivityById`、`fetchActivityRoi` 展示活动核心事实、复盘结果和 ROI 指标，活动备注仍走活动备注模块 API。ROI 展示只读取后端聚合结果，不在前端重新反算客户、商机、试驾、报价或交易金额。

`/dashboard/dict/type` 和 `/dashboard/dict/value` 展示稳定编码、展示名称、启停状态、内置标识、适用模块和停用原因。编辑时 `typeCode`、`valueCode` 只读，停用必须填写原因；新增业务选择默认过滤停用字典类型。删除和批量删除失败时按 `RESOURCE_IN_USE` 的 422 code 展示引用/内置保护提示，不解析中文 `msg`。

`/dashboard/audit/login` 和 `/dashboard/audit/operation` 使用 `modules/audit/api/audit-api.ts` 查询、查看详情和导出审计日志。页面展示中文结果和原因文案，业务分支仍以稳定 `result`、`reasonCode`、权限 code 和 HTTP 状态为准；导出复用 `httpClient.download()` 和 `saveBlob()`，按钮按 `audit:login:export`、`audit:operation:export` 控制显示。

`/dashboard/customer` 使用 `modules/customer/api/customer-api.ts` 查询客户主档列表和导出客户。列表展示客户状态、客户来源和负责人，字段来自客户主档响应；手机号、微信等敏感字段直接展示后端返回值，不在前端自行脱敏或还原。

`/dashboard/customer/:id` 使用 `fetchCustomerDetail` 查询客户详情，并通过模块 API 执行 `transferCustomerOwner`、`mergeCustomer`、`deleteCustomer`。归属转移和合并必须提交原因，高风险删除必须确认并按 `RESOURCE_IN_USE` 的 422 code 展示阻断；页面权限只控制按钮展示，不能替代后端权限和数据范围。

`/dashboard/product/stock` 使用 `modules/product/api/product-api.ts` 中的库存 API 查询商品库存汇总、库存流水和车辆实例。页面展示库存车辆实例状态时必须使用稳定编码映射中文文案，不能直接显示 `ORDER_RESERVED`、`RELEASE` 等后端枚举值；车辆入库、占用、释放命令统一走模块 API，不在页面临时拼装请求结构。

`/dashboard/opportunity` 使用 `modules/opportunity/api/opportunity-api.ts` 查询商机列表、详情、阶段历史并执行创建、编辑、阶段推进、赢单、输单、搁置和恢复命令。页面只提交 `INITIAL_CONTACT`、`NEEDS_ANALYSIS`、`QUOTING`、`WON`、`LOST`、`SHELVED` 等稳定商机阶段编码，中文阶段只做展示；商机表单只提交客户、需求、意向车型、预计金额、预计成交日期和下一步日期，不提交交易、收款、发票、库存或交付字段。

`/dashboard/test-drive` 使用 `modules/test-drive/api/test-drive-api.ts` 查询试驾列表、详情、状态历史并执行预约、改期、取消、爽约、签到和完成命令。页面只提交 `SCHEDULED`、`RESCHEDULED`、`CHECKED_IN`、`COMPLETED`、`CANCELED`、`NO_SHOW` 等稳定试驾状态编码，中文状态只做展示；预约表单只提交客户、可用车辆、可选商机、预约时间和联系方式，不提交负责人、库存汇总、交易、报价、订单或交付字段。

`/dashboard/tran` 使用 `modules/tran/api/tran-api.ts` 查询交易列表并触发交易终态命令。交易列表不再提供物理删除或批量删除入口；待报价、审批拒绝等未进入履约事实的交易可关闭，已审批、待收款和待交付交易可取消，操作必须填写原因并走 `cancelTran` 或 `closeTran` 模块 API。页面只按 `tran.cancel`、`tran.close` 权限做体验控制，后端仍负责最终授权、状态冲突和历史事实保留。
审批拒绝后的重新提交只把交易回到待报价，旧审批记录保留；前端确认文案不得暗示会清除审批历史。

`/dashboard/tran/invoice/:id` 使用 `modules/tran/api/tran-api.ts` 中的发票 API 查询、创建、作废、红冲和重开发票。页面允许同一交易按可开票余额进行部分开票和多张发票；作废、红冲和重开都必须填写原因；发票状态使用稳定编码映射中文文案，不能直接显示 `VOIDED`、`RED_REVERSED` 等后端枚举值。税号、银行账号、地址和电话等敏感字段以服务端返回值为准，前端不得自行拼接未授权原文。

`/dashboard/quote` 使用 `modules/quote/api/quote-api.ts` 查询报价列表、详情、版本和状态变更。页面只能提交 `DRAFT`、`PENDING_APPROVAL`、`PENDING_CUSTOMER_CONFIRMATION` 等稳定报价状态编码，中文状态只做展示映射。

`/dashboard/delivery` 使用 `modules/delivery/api/delivery-api.ts` 查询、创建和处理交付记录。页面展示 `PENDING_PREPARE`、`PREPARING`、`COMPLETED`、`EXCEPTION`、`CANCELLED` 等稳定编码的中文文案；准备项只提交 `PENDING`、`COMPLETED`、`BLOCKED`；签收、异常、取消都必须处理 loading、重复提交和失败提示。签收只触发交付和库存出库接口，不在前端假设交易已经完成。

`/dashboard/ai` 是独立 AI 助手页面。AI 前端模块归属 `modules/ai`，通过 Spring Boot AI 接口创建和恢复 AI Conversation，在 Conversation 中创建 AI Run、查询 Run 追踪并订阅 Spring Boot SSE；前端不直接调用独立 AI 服务，不保存模型密钥或供应商原始响应。

AI 前端展示状态固定为 `closed`、`sidebar`、`page`。`closed` 只表示右侧栏隐藏，不清空当前 Run、消息或上下文；`sidebar` 表示在当前 dashboard 页面右侧打开 AI 对话栏并压缩主内容；`page` 表示进入独立 AI 页面并隐藏右下角悬浮按钮。

`DashboardLayout.vue` 负责 AI 顶部入口、右下角悬浮入口和右侧栏容器。顶部入口是浅色胶囊形“AI 助手”按钮，右下角入口是 48px 圆形图标按钮；两个入口都打开同一个 `AiSidePanel.vue`。独立 AI 页面不展示右下角悬浮按钮，其他登录后的 dashboard 页面在有 AI 权限时展示悬浮按钮。独立 AI 页面上的模型配置入口只能是右上角紧凑设置按钮，不能占用整条导航栏。右侧栏必须作为布局列压缩主内容，不做 Dialog、普通弹窗或遮罩覆盖。

AI 前端模块按当前代码结构约束落地：`modules/ai/api/ai-api.ts` 是 Spring Boot AI API 入口，`modules/ai/model/ai.types.ts` 是 AI 类型入口。`AiSidePanel.vue` 和独立 AI 页面复用同一套 `AiAssistantPanel.vue`，不得并行保留两个职责重复的对话面板、API 文件或类型文件。侧栏 header 包含 AI 图标、标题、副标题、展开按钮和关闭按钮；关闭只隐藏面板，展开进入 `/dashboard/ai` 并携带当前 `conversationNo` 或可恢复上下文。消息区独立滚动，输入区固定在底部，空状态展示 AI 标识、欢迎语和推荐问题卡片。

AI Conversation 是多轮对话容器，AI Run 是 Conversation 中的一次执行。独立 AI 页面必须展示会话列表、新建会话、重命名和归档入口；右侧栏保持轻量，只提供当前会话、新对话和切换入口。前端发送问题时携带 `conversationNo`，未携带时由后端按当前用户和业务对象上下文解析默认会话。刷新和切换会话后必须以 Conversation detail 的 `turns` 恢复每一轮用户问题、AI 回答、业务结果卡片、Proposal、Workflow 和处理过程，不能只恢复单个 Run trace 或最新 Run。

AI Provider 配置页使用项目通用数据页风格，列表使用表格，新增、编辑和轮换 API Key 使用 Dialog。表单字段必须有可见 Label，不得只依赖 placeholder。前端内置千问、DeepSeek、MiniMax 和自定义 Provider 预设；已知 Provider 默认自动填充协议格式、Base URL、模型名和推荐参数，管理员只需要选择厂商、区域或模型并填写 API Key。Base URL、模型名、timeout、max output tokens 和 temperature 属于高级配置。高级配置数值范围必须与后端 DTO 校验一致，并在提交前显示字段错误；后端返回的 Provider 配置错误必须在页面中提示给用户。

AI Provider 配置页必须提供返回 AI 工作台的明确按钮。配置列表中启用状态和测试状态使用状态徽标与颜色区分；测试、启用、停用作为行内主操作，编辑和轮换 API Key 放入更多菜单或同等低频操作区，禁止五个按钮无区分地挤在同一行。本地、开发、测试和 smoke 环境缺少 `AI_PROVIDER_KEY_ENCRYPTION_SECRET` 时，后端会自动生成并复用 `~/.car-dealer-crm/ai-provider-key.secret`；生产环境仍必须显式配置加密主密钥。

前端上下文只传对象类型、对象标识、入口和脱敏摘要，不传 userId、角色、权限、数据范围或审计操作者。上下文推荐问题根据 `CUSTOMER`、`CLUE`、`OPPORTUNITY`、`TRANSACTION`、`PRODUCT`、`INVENTORY`、`FOLLOW_TASK` 等对象类型变化；没有上下文时展示四个通用问题。推荐问题只作为快捷输入，后端仍校验对象权限和数据范围。

工具结果必须使用结构化 UI 展示，包括消息气泡、表格结果、指标卡、对象引用卡、状态徽标、错误块、加载状态和空状态。工具结果卡片必须来自实时 SSE payload 或 Run trace 中持久化的 `displayPayload`，切换会话和刷新后不得消失。前端不展示内部工具 JSON，不渲染模型返回的 HTML；对象引用卡只展示脱敏摘要和跳转入口，跳转后的普通业务页面仍按自身 API 和权限规则加载。

AI 对话主视图必须业务结果优先。普通用户默认看到回答摘要、业务结果卡片、当前建议动作和业务风险提示；工具调用、Workflow 步骤、运行追踪和恢复信息只能放入“查看处理过程”折叠区，默认关闭。CRM 对象必须按对象类型渲染专用中文字段，禁止直接显示接口字段 key。交易结果至少映射为交易编号、客户、金额、状态、创建时间和建议动作；金额使用人民币格式，时间使用业务用户可读格式，状态使用中文业务文案。对话主视图不得展示 `workflowNo`、`stepType`、`tranNo`、`stageLabel`、工具 code、Provider runtime config 或原始 JSON。工作流按钮和动作入口必须来自当前问题和当前结果，不得在每次回答中固定展示无关能力菜单。

AI 回答 Markdown 必须通过专用安全组件渲染。该组件必须禁用模型 HTML，并在插入页面前清理标签和属性；除该组件外，AI 模块不得使用 `v-html`。

AI 面板展示消息、工具调用状态、工具结果、错误、Run 恢复、Proposal 卡片、Proposal 确认/拒绝、过期、状态恢复和执行结果。前端确认请求只能提交 `proposalId` 和确认/拒绝动作，不提交业务参数。

工作流展示由 `AiWorkflowPanel.vue` 承载，嵌入 `AiAssistantPanel.vue`，与消息、工具结果和 Proposal 共用同一个对话面板。前端从 Spring Boot SSE 更新 `workflow_*` 事件，也从 Run trace 恢复 `workflows` 和步骤列表。暂停、恢复、取消只调用 `ai-api.ts` 中的 Spring Boot 工作流接口；前端不得提供手动完成工作流入口，不得改写业务事实或提交工作流步骤业务参数。

主动提醒展示由 `AiProactivePanel.vue` 承载，提供订阅跟进提醒、订阅库存预警、暂停、恢复、取消、生成当前用户到期提醒、提醒列表和详情摘要展示。主动提醒生成、频率限制、静默时间、数量上限、重复合并、权限和数据范围校验都由 Spring Boot 执行；前端只展示结构化摘要和对象引用，不直接调用 `dealer-ai`。

## 业务模块

每个业务模块按以下方式组织：

```text
modules/<module>/
  api/<module>-api.ts
  model/<module>.types.ts
  composables/
  components/
```

已建立模块：

- `activity`
- `ai`
- `clue`
- `customer`
- `delivery`
- `dict`
- `opportunity`
- `product`
- `quote`
- `statistic`
- `test-drive`
- `tran`
- `user`

页面统一放在 `src/pages`，并通过路由懒加载。

`product` 模块商品状态提交值固定为 `ON_SALE`、`OFF_SALE`，页面只将其展示为“上架/下架”。

`tran` 模块收款、退款、发票和阶段状态统一提交稳定英文编码。交易详情页将 `PENDING`、`COMPLETED`、`PENDING_EXECUTION`、`FAILED` 等编码映射为中文展示；退款执行弹窗支持记录执行成功或执行失败，失败时必须填写失败原因，不向用户直接展示后端英文枚举值。交易详情页基于原收款、已完成退款和处理中退款申请展示可申请退款余额，已取消交易的已确认原收款仍可进入退款申请，但最终额度仍由后端校验。

## 列表请求基础设施

`shared/composables/use-latest-request.ts` 提供 `useLatestRequest<T>()` composable，实现最后请求获胜：

- `run(factory)` 递增内部 `latestId`，只有当前 ID 可提交 `data`/`error`/`loading`
- 旧请求的 `finally` 不会提前关闭新请求的 `loading`
- `cancel()` 通过 `AbortController` 取消当前请求
- 组件卸载时自动取消，取消不展示错误
- factory 接收 `AbortSignal`，模块 API 可透传给 `httpClient.get(url, { signal })`
