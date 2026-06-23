---
description: Vue 3 与 TypeScript 前端页面、组件、模块、Store、API、表单和路由代码规范。
globs: "dealer-web/src/**/*"
---

# Vue 与 TypeScript 前端代码风格

## 目录与文件职责

- `pages/` 只放路由页面，负责页面级数据加载、交互编排和业务组件组合。
- `modules/<domain>/api/` 只定义该领域的后端调用；`model/` 只定义领域类型、枚举和映射。
- `modules/<domain>/components/` 放仅属于该领域的可复用组件，禁止被其他领域直接依赖。
- `modules/<domain>/composables/` 放该领域可复用的状态逻辑和异步流程。
- `stores/` 只保存跨页面、跨路由生命周期的状态；页面局部数据不得进入 Store。
- `shared/` 只放与具体业务领域无关的 API、存储、类型、UI 和工具。
- `components/ui/` 是 shadcn-vue/reka-ui 基础组件层，禁止写入 CRM 业务规则。
- 禁止跨领域深层引用内部文件；确需共享时先提升到明确的 `shared` 能力或公开模块出口。

## 文件与标识符命名

- Vue 组件文件和组件类型使用 `PascalCase`，如 `ActivityFormDialog.vue`。
- TypeScript 文件使用 `kebab-case`；领域类型文件使用 `domain.types.ts`，Store 使用 `domain.store.ts`。
- 组件、类型、接口、枚举使用 `PascalCase`；变量、函数、参数、属性使用 `lowerCamelCase`。
- 常量使用 `UPPER_SNAKE_CASE`；布尔值使用 `is`、`has`、`can`、`should` 前缀。
- composable 使用 `useXxx`，Pinia Store 使用 `useXxxStore`，禁止无语义的 `useCommon`、`useData`。
- API 查询使用 `fetch/get/list`，命令使用 `create/update/delete/approve/refund`，禁止统一命名为 `requestData`。
- 用户操作入口使用 `handleXxx`；子组件发出的业务事件使用过去式或动作名，如 `saved`、`deleted`、`change`。

## TypeScript 类型

- 业务代码禁止 `any`；外部未知数据使用 `unknown` 并通过类型守卫或 Schema 收窄。
- API 请求、响应、分页、表单、组件 Props/Emits、Store 状态和 composable 返回值必须有明确类型。
- `null` 表示业务上明确的“无值”，`undefined` 表示未提供；同一字段禁止混用两种含义。
- 禁止使用 `as`、非空断言 `!` 或双重断言掩盖 API、路由参数和组件契约错误。
- 联合类型超过三个字符串值或需要跨层共享时，必须定义稳定常量映射或领域类型。
- 后端状态 code 和错误码集中在领域 `model` 或 `shared/api`，页面禁止重复定义。
- 禁止使用 `Record<string, any>`、无结构对象和位置数组充当跨模块契约。
- 导出类型使用 `export type` 或 `export interface`；纯类型 import 使用 `import type`。

## Vue 页面

- 页面必须使用 `<script setup lang="ts">`，业务页面顺序统一为 `template`、`script`、`style`。
- 页面只编排模块 API、composable、Store 和业务组件，禁止直接创建 Axios 实例或拼装通用请求配置。
- 列表页面必须明确维护查询条件、页码、加载状态、空状态和重新加载入口。
- 详情页面必须处理参数非法、数据不存在、无权限、加载中和加载失败状态。
- 页面离开后仍需保留的状态才进入 Store；筛选条件是否保留必须是明确产品行为。
- 页面文件逻辑超过约 150 行时，应优先提取领域组件或 composable，而不是堆积私有函数。
- 禁止在 template 中执行复杂计算、修改状态或调用可能抛异常的函数。

## 业务组件

- 组件只承担一个明确交互职责；组件名必须体现业务对象和用途。
- Props 是只读输入，禁止直接修改；需要双向绑定时显式使用 `defineModel` 或 `update:modelValue`。
- Props 必须定义必填、可选和默认值；对象、数组默认值不得共享可变实例。
- Emits 必须使用类型声明并表达事件数据，禁止依赖父组件读取子组件内部状态。
- 组件不得自行读取路由或全局 Store，除非其职责本身就是路由级或全局级组件。
- Dialog/Form 组件必须区分打开状态、初始值、提交状态和关闭结果，关闭时清理临时状态。
- 禁止通过 `$attrs`、全局变量或 DOM 查询绕过明确的 Props/Emits 契约。

## 基础 UI 组件

- `components/ui/` 遵守 shadcn-vue/reka-ui 的生成结构、Props 转发和可访问性契约。
- 修改生成组件前必须确认问题属于基础组件而非业务使用方式。
- 基础组件只提供视觉变体和通用交互，不包含权限码、API 调用、业务状态或中文业务文案。
- 图标优先使用 `@lucide/vue`；图标按钮必须提供可访问名称或 Tooltip。
- 禁止复制基础组件形成只改颜色的重复版本，应通过 variant、class 或组合扩展。

## Composable

- composable 用于复用状态逻辑和副作用管理，不用于包装一个无状态工具函数。
- composable 必须返回明确命名的 `ref/computed` 和动作函数，禁止返回位置数组。
- 异步 composable 必须在 `finally` 恢复 loading；重复请求必须处理覆盖、取消或最后响应获胜规则。
- composable 内注册的监听器、定时器和事件必须在组件卸载时清理。
- 禁止在模块顶层创建请求级可变状态；需要全局单例状态时使用 Pinia。
- `watch` 只处理必要副作用，能用 `computed` 表达的派生值禁止使用 `watch` 同步。

## Pinia Store

- Store ID 使用稳定小写领域名；文件命名为 `domain.store.ts`，导出 `useDomainStore`。
- Store 只保存认证、权限、全局 UI 等跨页面状态，不缓存所有接口响应。
- state 使用 `ref/reactive`，派生状态使用 `computed`，状态变化通过命名明确的 action 完成。
- Store action 必须维护自己的成功、失败和清理语义，禁止留下半更新状态。
- Token、权限等持久化必须通过 `shared/storage`，禁止页面直接访问 localStorage/sessionStorage。
- 登出和认证失效必须清理 Token、当前用户、权限及相关内存状态。
- Store 禁止保存 Vue 组件实例、DOM 节点、路由对象和不可序列化请求对象。

## API 与错误处理

- 页面和组件只能调用 `modules/<domain>/api` 导出的函数，禁止直接导入 Axios。
- 模块 API 必须复用 `shared/api/http-client.ts`，不得重复实现 Token、envelope 或 401 处理。
- 每个 API 函数必须声明请求和解包后的业务返回类型，禁止把 AxiosResponse 传播到页面。
- 查询参数使用结构化对象并移除未提供字段，禁止手工拼接 URL 查询字符串。
- 文件上传、下载和表单编码必须在模块 API 中明确 Content-Type、返回类型和文件名处理。
- 前端业务分支只能依据 HTTP 状态和稳定错误 code，禁止匹配 `message` 中文文案。
- 页面只展示可理解的用户反馈；原始异常、堆栈和服务端内部信息禁止直接展示。
- `catch` 必须恢复界面状态并明确继续抛出、转换或消费异常，禁止空 `catch`。

## 表单与校验

- 业务表单统一使用 vee-validate 与 Zod，Schema 是前端表单规则的唯一来源。
- 表单类型应从 Schema 推导或与其保持单一映射，禁止手写两套容易漂移的类型。
- 创建和编辑使用不同 Schema 或显式组合，禁止大量条件分支共用一个万能 Schema。
- 提交前进行格式转换，如空字符串转 `undefined`、日期格式化、数字解析；转换集中在提交边界。
- 服务端业务校验结果必须正常展示，前端校验不能替代后端权限、状态和唯一性校验。
- 提交期间禁用重复提交；成功后根据业务要求关闭、刷新或导航，失败时保留用户可修正输入。

## 路由与权限

- 路由集中定义在 `router/routes.ts`，守卫集中在 `router/guards.ts`，页面禁止动态注册全局守卫。
- 路由 name 使用稳定 `kebab-case`，路径参数必须在进入页面时解析和校验。
- 需要登录的路由必须声明 `meta.requiresAuth`；侧边栏定位使用明确的 `meta.activeMenu`。
- 路由守卫只处理认证会话、权限数据加载和导航，不执行领域写操作。
- 权限指令和按钮隐藏只改善体验，所有敏感操作仍必须由后端授权。
- 认证失效必须走统一清理和跳转流程，禁止多个页面分别实现退出逻辑。

## 异步状态与交互

- 每个异步动作必须有独立 loading 状态，禁止一个全局布尔值控制无关请求。
- 列表首次加载、局部刷新、提交和删除必须区分视觉状态，避免整个页面无必要锁定。
- 删除、退款、取消等不可逆或高风险动作必须二次确认，并在确认文案中指出业务对象。
- 请求成功后只更新受影响状态；无法可靠局部更新时重新请求服务端事实。
- 禁止乐观更新库存、支付、审批等强一致业务，除非有明确回滚策略。

## 样式、可访问性与文本

- 使用现有 Tailwind CSS、shadcn-vue 变体和 `cn()` 合并 class，禁止新增平行样式体系。
- 业务组件样式不得覆盖全局选择器；SFC 局部样式确有需要时使用 `scoped`。
- 表单控件必须关联 Label 和错误信息；仅图标控件必须有 `aria-label` 或可见 Tooltip。
- 颜色不能作为状态的唯一表达，错误、成功和禁用状态必须同时具有文本或图标语义。
- 页面文案面向业务用户，不展示变量名、错误堆栈、HTTP 实现或调试说明。
- 文案集中到稳定映射的位置；同一状态禁止在多个页面出现不同名称。

## 注释与格式

- 标识符使用英文，业务注释使用中文；注释解释约束和原因，不翻译 template 或类型。
- 导出的复杂 composable、类型转换和协议适配函数应使用 TSDoc 说明输入、输出和失败行为。
- 简单组件、显然的 Props 和普通事件处理器不写无意义注释。
- 遵循 ESLint 与 Prettier：2 空格、单引号、无分号、100 字符宽度、尾随逗号。
- import 按现有 ESLint 结果整理，类型 import 与运行时 import 不得混淆。
- 自动生成的 `components/ui/` 保持生成格式，禁止为统一 SFC 顺序进行无业务价值的重排。
