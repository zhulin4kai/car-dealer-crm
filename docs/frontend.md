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
  modules/      # activity/clue/customer/dict/product/statistic/tran/user
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
  pageSize?: number
}
```

HTTP 层集中处理：

- `VITE_API_BASE_URL` / 默认 `http://localhost:8089`
- token 请求头注入为 `Authorization: Bearer <token>`
- `rememberMe` 请求头
- `code !== 200` 抛出 `ApiError`
- 会话失效（code 510-513）通过 `shared/auth/session-invalid-handler.ts` 单飞处理：应用启动时注册处理器，`notifySessionInvalid` 持有 `inFlight` Promise 守卫，并发失效只触发一次 `authStore.forceLogout()` + `permissionStore.clearPermissions()` + `router.replace({ name: 'login' })`
- 业务页只能按 `ApiError.code` 和 HTTP 状态处理稳定业务分支；商品删除等引用保护场景按 `RESOURCE_IN_USE(422)` 映射提示，不解析中文 `msg` 或数据库约束文案。
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
- `/dashboard/product/promotion`
- `/dashboard/product/stock`
- `/dashboard/quote`
- `/dashboard/tran`
- `/dashboard/tran/:id`
- `/dashboard/tran/approve/:id`
- `/dashboard/tran/invoice/:id`
- `/dashboard/delivery`
- `/dashboard/dict/type`
- `/dashboard/dict/value`

`DashboardLayout.vue` 只负责主框架布局、菜单、用户入口和退出登录。

`/dashboard/user` 使用 `modules/user/api/user-api.ts` 查询、新增、编辑、启禁用和交接用户责任。责任交接通过 `handoverUserResponsibilities` 提交目标负责人和交接原因，目标负责人列表来自同模块 `fetchOwnerList`；页面只按 `user.status` 权限控制按钮展示，提交期间禁用重复点击，失败按接口错误处理，不匹配中文 `msg`。

`/dashboard/clue` 使用 `modules/clue/api/clue-api.ts` 查询、创建、编辑、导入和删除线索。线索新增表单在提交前把手机号中的空格、横杠和括号归一化为 11 位规范手机号，异步查重也使用归一化值；导入错误、删除阻断等业务分支必须按稳定 code 和 HTTP 状态处理，不能匹配中文 `msg`。

`/dashboard/clue/detail/:id` 展示线索详情、跟踪记录和责任历史。转派线索必须通过 `transferClueOwner` 提交目标负责人和原因，详情页转派成功后重新加载当前负责人和 `getClueOwnerHistory` 返回的历史记录；关闭和恢复分别通过 `closeClue`、`restoreClue` 提交原因，按钮显示基于 `stateDO.valueCode` 的稳定状态 code，不匹配中文状态文本。前端权限只控制按钮显示，后端负责数据范围、目标负责人资格、状态迁移、重复活跃线索校验和并发旧状态校验。

`/dashboard/customer` 使用 `modules/customer/api/customer-api.ts` 查询客户主档列表和导出客户。列表展示客户状态、客户来源和负责人，字段来自客户主档响应；手机号、微信等敏感字段直接展示后端返回值，不在前端自行脱敏或还原。

`/dashboard/customer/:id` 使用 `fetchCustomerDetail` 查询客户详情，并通过模块 API 执行 `transferCustomerOwner`、`mergeCustomer`、`deleteCustomer`。归属转移和合并必须提交原因，高风险删除必须确认并按 `RESOURCE_IN_USE` 的 422 code 展示阻断；页面权限只控制按钮展示，不能替代后端权限和数据范围。

`/dashboard/product/stock` 使用 `modules/product/api/product-api.ts` 中的库存 API 查询商品库存汇总、库存流水和车辆实例。页面展示库存车辆实例状态时必须使用稳定编码映射中文文案，不能直接显示 `ORDER_RESERVED`、`RELEASE` 等后端枚举值；车辆入库、占用、释放命令统一走模块 API，不在页面临时拼装请求结构。

`/dashboard/tran` 使用 `modules/tran/api/tran-api.ts` 查询交易列表并触发交易终态命令。交易列表不再提供物理删除或批量删除入口；待报价、审批拒绝等未进入履约事实的交易可关闭，已审批、待收款和待交付交易可取消，操作必须填写原因并走 `cancelTran` 或 `closeTran` 模块 API。页面只按 `tran.cancel`、`tran.close` 权限做体验控制，后端仍负责最终授权、状态冲突和历史事实保留。

`/dashboard/tran/invoice/:id` 使用 `modules/tran/api/tran-api.ts` 中的发票 API 查询、创建、作废、红冲和重开发票。页面允许同一交易按可开票余额进行部分开票和多张发票；作废、红冲和重开都必须填写原因；发票状态使用稳定编码映射中文文案，不能直接显示 `VOIDED`、`RED_REVERSED` 等后端枚举值。税号、银行账号、地址和电话等敏感字段以服务端返回值为准，前端不得自行拼接未授权原文。

`/dashboard/quote` 使用 `modules/quote/api/quote-api.ts` 查询报价列表、详情、版本和状态变更。页面只能提交 `DRAFT`、`PENDING_APPROVAL`、`PENDING_CUSTOMER_CONFIRMATION` 等稳定报价状态编码，中文状态只做展示映射。

`/dashboard/delivery` 使用 `modules/delivery/api/delivery-api.ts` 查询、创建和处理交付记录。页面展示 `PENDING_PREPARE`、`PREPARING`、`COMPLETED`、`EXCEPTION`、`CANCELLED` 等稳定编码的中文文案；准备项只提交 `PENDING`、`COMPLETED`、`BLOCKED`；签收、异常、取消都必须处理 loading、重复提交和失败提示。签收只触发交付和库存出库接口，不在前端假设交易已经完成。

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
- `clue`
- `customer`
- `delivery`
- `dict`
- `product`
- `quote`
- `statistic`
- `tran`
- `user`

页面统一放在 `src/pages`，并通过路由懒加载。

`product` 模块商品状态提交值固定为 `ON_SALE`、`OFF_SALE`，页面只将其展示为“上架/下架”。

`tran` 模块收款、退款、发票和阶段状态统一提交稳定英文编码。交易详情页将 `PENDING`、`COMPLETED`、`PENDING_EXECUTION`、`FAILED` 等编码映射为中文展示；退款执行弹窗支持记录执行成功或执行失败，失败时必须填写失败原因，不向用户直接展示后端英文枚举值。

## 列表请求基础设施

`shared/composables/use-latest-request.ts` 提供 `useLatestRequest<T>()` composable，实现最后请求获胜：

- `run(factory)` 递增内部 `latestId`，只有当前 ID 可提交 `data`/`error`/`loading`
- 旧请求的 `finally` 不会提前关闭新请求的 `loading`
- `cancel()` 通过 `AbortController` 取消当前请求
- 组件卸载时自动取消，取消不展示错误
- factory 接收 `AbortSignal`，模块 API 可透传给 `httpClient.get(url, { signal })`
