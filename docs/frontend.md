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
- token 请求头注入
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
- 退出登录同时清理 token 与权限缓存

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
- `/dashboard/tran`
- `/dashboard/tran/:id`
- `/dashboard/tran/approve/:id`
- `/dashboard/tran/invoice/:id`
- `/dashboard/dict/type`
- `/dashboard/dict/value`

`DashboardLayout.vue` 只负责主框架布局、菜单、用户入口和退出登录。

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
- `dict`
- `product`
- `statistic`
- `tran`
- `user`

页面统一放在 `src/pages`，并通过路由懒加载。

`product` 模块商品状态提交值固定为 `ON_SALE`、`OFF_SALE`，页面只将其展示为“上架/下架”。

## 列表请求基础设施

`shared/composables/use-latest-request.ts` 提供 `useLatestRequest<T>()` composable，实现最后请求获胜：

- `run(factory)` 递增内部 `latestId`，只有当前 ID 可提交 `data`/`error`/`loading`
- 旧请求的 `finally` 不会提前关闭新请求的 `loading`
- `cancel()` 通过 `AbortController` 取消当前请求
- 组件卸载时自动取消，取消不展示错误
- factory 接收 `AbortSignal`，模块 API 可透传给 `httpClient.get(url, { signal })`
