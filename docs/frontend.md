# 前端业务逻辑分析文档

> 项目：`dealer-web` — 徐州工程学院汽车销售管理系统前端
> 技术栈：Vue 3 + Element Plus + Vue Router + Axios + ECharts

---

## 目录

1. [项目入口与全局配置](#1-项目入口与全局配置)
2. [路由配置](#2-路由配置)
3. [公共工具函数](#3-公共工具函数)
4. [HTTP 请求封装](#4-http-请求封装)
5. [全局指令](#5-全局指令)
6. [业务模块分析](#6-业务模块分析)
   - [6.1 登录认证模块](#61-登录认证模块)
   - [6.2 仪表盘/主框架模块](#62-仪表盘主框架模块)
   - [6.3 统计报表模块](#63-统计报表模块)
   - [6.4 用户管理模块](#64-用户管理模块)
   - [6.5 市场活动模块](#65-市场活动模块)
   - [6.6 线索管理模块](#66-线索管理模块)
   - [6.7 客户管理模块](#67-客户管理模块)
   - [6.8 交易管理模块](#68-交易管理模块)
   - [6.9 商品管理模块](#69-商品管理模块)
   - [6.10 字典管理模块](#610-字典管理模块)
   - [6.11 系统管理模块](#611-系统管理模块)
7. [API 接口清单](#7-api-接口清单)
8. [页面跳转关系图](#8-页面跳转关系图)

---

## 1. 项目入口与全局配置

**入口文件**：`src/main.js`

- 创建 Vue 3 应用并挂载到 `#app`
- 注册 **Element Plus** 组件库（中文语言包 `zh-cn`）
- 注册 **所有 Element Plus 图标**（`@element-plus/icons-vue`）
- 注册 **Vue Router**
- 注册全局自定义指令 `v-hasPermission`
- 全局样式：`src/assets/global.css`

**根组件**：`src/App.vue`

- 仅包含 `<router-view/>`，作为路由出口渲染各页面组件

---

## 2. 路由配置

**文件**：`src/router/router.js`

### 路由表

| 路径 | 组件 | 说明 |
|------|------|------|
| `/` | `LoginView.vue` | 登录页 |
| `/dashboard` | `DashboardView.vue` | 主框架（含左侧菜单、顶栏） |
| `/dashboard/` (默认子路由) | `StatisticView.vue` | 统计概览/仪表盘首页 |
| `/dashboard/user` | `UserView.vue` | 用户管理 |
| `/dashboard/activity` | `ActivityView.vue` | 市场活动列表 |
| `/dashboard/activity/:id` | `ActivityDetailView.vue` | 活动详情（动态路由） |
| `/dashboard/clue` | `ClueView.vue` | 线索列表 |
| `/dashboard/clue/detail/:id` | `ClueDetailView.vue` | 线索详情（动态路由） |
| `/dashboard/customer` | `CustomerView.vue` | 客户列表 |
| `/dashboard/product` | `ProductView.vue` | 产品列表 |
| `/dashboard/product/category` | `ProductCategoryView.vue` | 产品分类管理 |
| `/dashboard/product/promotion` | `ProductPromotionView.vue` | 促销管理 |
| `/dashboard/product/stock` | `ProductStockAlertView.vue` | 库存预警 |
| `/dashboard/tran` | `TranView.vue` | 交易列表 |
| `/dashboard/tran/:id` | `TranDetailView.vue` | 交易详情（动态路由） |
| `/dashboard/tran/approve/:id` | `TranApproveView.vue` | 交易审批（动态路由） |
| `/dashboard/tran/invoice/:id` | `TranInvoiceView.vue` | 交易开票（动态路由） |
| `/dashboard/dict/type` | `DictTypeView.vue` | 字典类型管理 |
| `/dashboard/dict/value` | `DictValueView.vue` | 字典值管理 |
| `/dashboard/system` | `SystemView.vue` | 系统管理 |
| `/:pathMatch(.*)*` | 重定向到 `/dashboard` | 兜底路由 |

### 路由守卫

```javascript
router.beforeEach((to, from, next) => {
    // 从 sessionStorage 或 localStorage 获取 token
    // 如果访问 /dashboard 开头的路由且无 token，跳转到登录页 /
    // 否则放行
})
```

---

## 3. 公共工具函数

**文件**：`src/util/util.js`

| 函数名 | 参数 | 返回值 | 功能说明 |
|--------|------|--------|----------|
| `messageTip(msg, type)` | `msg: string`, `type: 'success'\|'warning'\|'info'\|'error'` | `void` | 调用 `ElMessage` 显示 3 秒消息提示，居中显示，带关闭按钮 |
| `getTokenName()` | 无 | `string` | 返回 token 在浏览器存储中的 key 名：`"dlyk_token"` |
| `removeToken()` | 无 | `void` | 从 `sessionStorage` 和 `localStorage` 中删除 token |
| `messageConfirm(msg)` | `msg: string` | `Promise<MessageBoxData>` | 调用 `ElMessageBox.confirm` 弹出确认对话框（确定/取消），返回 Promise |
| `goBack()` | 无 | `void` | 调用 `window.history.back()` 返回上一页 |
| `getToken()` | 无 | `string\|undefined` | 从 sessionStorage 优先获取 token，不存在则从 localStorage 获取；若为空弹出确认是否重新登录 |

---

## 4. HTTP 请求封装

**文件**：`src/http/httpRequest.js`

### 基础配置

- 基于 **axios** 封装
- 默认 base URL：`import.meta.env.VITE_API_BASE_URL || "http://localhost:8089"`
- 所有请求响应类型为 `json`

### 导出函数

| 函数 | HTTP 方法 | 参数 | 说明 |
|------|-----------|------|------|
| `doGet(url, params)` | `GET` | `url: string`, `params: object` | GET 请求，参数拼接在 URL query |
| `doPost(url, data)` | `POST` | `url: string`, `data: object` | POST 请求，数据在请求体 |
| `doPut(url, data)` | `PUT` | `url: string`, `data: object` | PUT 请求，数据在请求体 |
| `doDelete(url, data)` | `DELETE` | `url: string`, `data: object` | DELETE 请求，数据在请求体 |

### 请求拦截器

- 从 `sessionStorage` 或 `localStorage` 获取 token
- 将 token 放入请求头 `Authorization`
- 若 token 来自 `localStorage`，额外设置 `rememberMe: true` 请求头

### 响应拦截器

- 若响应 `code >= 500`（token 验证失败）：
  - 弹出确认框"是否重新去登录？"
  - 确定 → 删除 token → 跳转到 `/`
  - 取消 → 提示"取消去登录"
- 否则正常返回响应

---

## 5. 全局指令

### `v-hasPermission`

**注册位置**：`src/main.js`

**实现逻辑**：

```javascript
app.directive("hasPermission", (el, binding) => {
    // 每次 mounted 和 updated 时触发
    doGet("/api/login/info", {}).then(resp => {
        let user = resp.data.data;
        let permissionList = user.permissionList;
        let flag = false;
        for (let key in permissionList) {
            if (permissionList[key] === binding.value) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            // 没有权限，直接从 DOM 中删除该元素
            el.parentNode && el.parentNode.removeChild(el);
        }
    })
})
```

**使用方式**：

```html
<el-button v-hasPermission="'user:add'">添加用户</el-button>
<el-button v-hasPermission="'clue:delete'">删除线索</el-button>
```

**权限标识列表**（从代码中提取）：

| 模块 | 权限标识 | 功能 |
|------|----------|------|
| 用户管理 | `user:add` | 添加用户 |
| 用户管理 | `user:view` | 查看用户详情 |
| 用户管理 | `user:edit` | 编辑用户 |
| 用户管理 | `user:delete` | 删除用户 |
| 线索管理 | `clue:add` | 录入线索 |
| 线索管理 | `clue:import` | 导入线索 |
| 线索管理 | `clue:view` | 查看线索详情 |
| 线索管理 | `clue:edit` | 编辑线索 |
| 线索管理 | `clue:delete` | 删除线索 |

---

## 6. 业务模块分析

### 6.1 登录认证模块

**入口文件**：`src/view/LoginView.vue`
**关联 API**：`src/api/clue.js`（`getLoginInfo`）、`src/http/httpRequest.js`（`doPost`、`doGet`）

#### 核心业务功能

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **登录** | 用户输入账号密码，POST 请求 `/api/login`，成功后存储 JWT token 到 sessionStorage 或 localStorage（取决于"记住我"），跳转到 `/dashboard` | `POST /api/login`（FormData: loginAct, loginPwd, rememberMe） |
| **记住我** | 勾选后 token 存入 `localStorage`，关闭浏览器后仍然有效 | - |
| **免登录** | 页面加载时检查 localStorage 中是否有 token，若有则调用 `/api/login/free` 验证，通过则自动跳转 `/dashboard` | `GET /api/login/free` |
| **表单验证** | 账号必填，密码必填且 6-16 位 | - |

#### 使用的 Element Plus 组件

`el-container`, `el-aside`, `el-main`, `el-form`, `el-form-item`, `el-input`, `el-button`, `el-checkbox`

#### 数据流向

```
用户输入 → FormData → POST /api/login → 响应 token → 存储到 Storage → 跳转 /dashboard
```

---

### 6.2 仪表盘/主框架模块

**入口文件**：`src/view/DashboardView.vue`
**关联 API**：`src/http/httpRequest.js`（`doGet`, `doPost`）

#### 核心业务功能

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载登录用户信息** | 页面加载时获取当前登录用户信息，显示用户名和头像（首字母） | `GET /api/login/info` |
| **动态菜单渲染** | 根据用户 `menuPermissionList` 动态渲染左侧菜单，包含一级菜单和二级菜单 | - |
| **菜单折叠/展开** | 点击折叠按钮切换左侧菜单宽度 | - |
| **退出登录** | 点击下拉菜单"退出登录"，调用 `/api/logout`，清除 token，跳转登录页 | `POST /api/logout` |
| **路由导航** | 菜单项点击后通过 Vue Router 的 `router` 模式自动跳转 | - |
| **当前路由高亮** | 根据当前路径自动高亮对应的菜单项 | - |

#### 使用的 Element Plus 组件

`el-container`, `el-aside`, `el-header`, `el-main`, `el-footer`, `el-menu`, `el-sub-menu`, `el-menu-item`, `el-icon`, `el-dropdown`, `el-dropdown-menu`, `el-dropdown-item`

#### 数据流向

```
GET /api/login/info → user 对象 → 渲染菜单 + 用户名 + 头像
```

---

### 6.3 统计报表模块

**入口文件**：`src/view/StatisticView.vue`
**关联 API**：`src/http/httpRequest.js`（`doGet`）

#### 核心业务功能

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载概览统计** | 显示市场活动数、线索总数、客户总数、交易总额 | `GET /api/summary/data` |
| **加载销售漏斗图** | 使用 ECharts 渲染漏斗图，展示线索→客户→交易→成交的转化漏斗 | `GET /api/saleFunnel/data` |
| **加载线索来源饼图** | 使用 ECharts 渲染饼图，展示各线索来源占比 | `GET /api/sourcePie/data` |

#### 使用的 Element Plus 组件

`el-row`, `el-col`, `el-statistic`

#### 数据流向

```
GET /api/summary/data → summaryData → el-statistic 展示
GET /api/saleFunnel/data → ECharts 漏斗图
GET /api/sourcePie/data → ECharts 饼图
```

---

### 6.4 用户管理模块

**入口文件**：`src/view/UserView.vue`
**关联 API**：`src/http/httpRequest.js`（直接调用 `doGet`, `doPost`, `doPut`, `doDelete`）

#### 核心业务功能

| 操作 | 功能说明 | API 调用 | 权限标识 |
|------|----------|----------|----------|
| **加载用户列表** | 分页展示用户列表（账号、姓名、手机、邮箱、创建时间） | `GET /api/users?current=N` | - |
| **添加用户** | 弹窗表单，填写账号/密码/姓名/手机/邮箱/账号状态，提交新增 | `POST /api/user` (FormData) | `user:add` |
| **编辑用户** | 弹窗加载用户数据，修改后提交（密码显示为 `******`） | `PUT /api/user` (FormData) | `user:edit` |
| **查看用户详情** | 弹窗展示用户全部信息（含创建人、编辑人、最近登录时间） | `GET /api/user/{id}` | `user:view` |
| **删除用户** | 确认后删除单个用户 | `DELETE /api/user/{id}` | `user:delete` |
| **批量删除** | 勾选多个用户后批量删除 | `DELETE /api/user` (body: ids数组) | `user:delete` |
| **分页** | 点击分页器切换页码 | - | - |

#### 表单验证规则

- 账号：必填
- 密码：必填，6-16 位
- 姓名：必填，必须中文
- 手机：必填，格式 `^1[3-9]\d{9}$`
- 邮箱：必填，邮箱格式校验
- 账号状态：必填（是/否下拉选择）

#### 使用的 Element Plus 组件

`el-card`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-form`, `el-form-item`, `el-input`, `el-select`, `el-option`

---

### 6.5 市场活动模块

**入口文件**：`src/view/ActivityView.vue`（列表）、`src/view/ActivityDetailView.vue`（详情）
**关联 API**：`src/api/activity.js`

#### ActivityView — 活动列表

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载活动列表** | 分页展示活动列表（负责人、活动名称、开始/结束时间、预算、创建时间） | `GET /api/activitys?current=N&ownerId=&name=&startTime=&endTime=&cost=&createTime=` |
| **加载负责人下拉** | 获取所有可选负责人 | `GET /api/owner` |
| **搜索** | 按负责人、活动名称、活动时间范围、最低预算、创建时间筛选 | - |
| **重置搜索** | 清空搜索条件重新加载 | - |
| **录入市场活动** | 弹窗表单录入（负责人、活动名称、开始/结束时间、预算、描述） | `POST /api/activity` (FormData) |
| **编辑市场活动** | 弹窗加载活动数据编辑 | `PUT /api/activity` (FormData) |
| **查看详情** | 跳转到 `/dashboard/activity/{id}` | - |
| **删除活动** | 确认后删除单个活动 | `DELETE /api/activity/{id}` |
| **批量删除** | 勾选多个活动后批量删除 | `POST /api/activity/batch` (body: ids数组) |

#### ActivityDetailView — 活动详情

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载活动详情** | 展示活动全部信息（含负责人、创建人、编辑人） | `GET /api/activity/{id}` |
| **提交活动备注** | 填写备注内容提交 | `POST /api/activity/remark` (body: activityId, noteContent) |
| **加载备注列表** | 分页展示活动备注记录 | `GET /api/activity/remark?current=N&activityId={id}` |
| **编辑备注** | 功能待实现 | - |
| **删除备注** | 确认后删除备注 | `DELETE /api/activity/remark/{id}` |
| **返回** | 返回活动列表页 | - |

#### 使用的 Element Plus 组件

`el-card`, `el-form`, `el-form-item`, `el-input`, `el-select`, `el-date-picker`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-tag`, `el-row`, `el-col`

---

### 6.6 线索管理模块

**入口文件**：`src/view/ClueView.vue`（列表）、`src/view/ClueDetailView.vue`（详情）
**关联 API**：`src/api/clue.js`, `src/api/activity.js`, `src/api/dict.js`, `src/api/product.js`

#### ClueView — 线索列表

| 操作 | 功能说明 | API 调用 | 权限标识 |
|------|----------|----------|----------|
| **加载线索列表** | 分页展示线索（负责人、所属活动、姓名、称呼、手机、微信、贷款、意向状态、意向产品、线索状态、来源、下次联系时间） | `GET /api/clues?current=N` | - |
| **录入线索** | 弹窗表单录入（负责人、所属活动、姓名、称呼、手机、微信、QQ、邮箱、年龄、职业、年收入、住址、贷款、意向状态、意向产品、线索状态、来源、描述、下次联系时间） | `POST /api/clue` (FormData) | `clue:add` |
| **编辑线索** | 弹窗加载线索数据编辑（手机字段不可编辑） | `PUT /api/clue` (FormData) | `clue:edit` |
| **查看线索详情** | 跳转到 `/dashboard/clue/detail/{id}` | - | `clue:view` |
| **删除线索** | 确认后删除 | `DELETE /api/clue/{id}` | `clue:delete` |
| **批量删除** | 勾选多个后批量删除 | `POST /api/clue/batch` (body: ids数组) | `clue:delete` |
| **导入线索(Excel)** | 弹窗上传 Excel 文件导入 | `POST /api/importExcel` (FormData: file) | `clue:import` |
| **手机号唯一校验** | 录入时异步校验手机号是否已存在 | `GET /api/clue/{phone}` | - |

#### 数据加载流程（录入/编辑时）

```
loadData() → 并行加载:
  ├── loadDicValue('appellation') → GET /api/dict/values?typeCode=appellation → 称呼下拉
  ├── loadDicValue('needLoan')    → GET /api/dict/values?typeCode=needLoan    → 贷款下拉
  ├── loadDicValue('intentionState') → 意向状态下拉
  ├── loadDicValue('clueState')   → 线索状态下拉
  ├── loadDicValue('source')      → 线索来源下拉
  ├── loadActivityAndProduct()    → 活动下拉 + 产品下拉
  └── loadOwner()                 → 负责人下拉
```

#### ClueDetailView — 线索详情

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载线索详情** | 展示线索全部信息（基本信息 + 线索状态） | `GET /api/clue/detail/{id}` |
| **提交跟踪记录** | 选择跟踪方式 + 填写跟踪内容提交 | `POST /api/clue/remark` (body: clueId, noteContent, noteWay) |
| **加载跟踪记录列表** | 分页展示跟踪记录 | `GET /api/clue/remark?current=N&clueId={id}` |
| **转换客户** | 弹窗选择意向产品、客户描述、下次跟踪时间，将线索转换为客户 | `POST /api/clue/customer` (body: clueId, product, description, nextContactTime) |
| **返回** | 返回线索列表页 | - |

#### 使用的 Element Plus 组件

`el-card`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-form`, `el-form-item`, `el-input`, `el-select`, `el-option`, `el-date-picker`, `el-upload`, `el-tag`

---

### 6.7 客户管理模块

**入口文件**：`src/view/CustomerView.vue`
**关联 API**：`src/http/httpRequest.js`（直接调用 `doGet`）

#### 核心业务功能

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载客户列表** | 分页展示客户列表（负责人、所属活动、姓名、称呼、手机、微信、贷款、意向状态、线索状态、来源、意向产品、下次联系时间） | `GET /api/customers?current=N` |
| **全部导出(Excel)** | 导出所有客户数据为 Excel | `GET /api/exportExcel?Authorization={token}`（通过 iframe 下载） |
| **选择导出(Excel)** | 导出勾选的客户数据为 Excel | `GET /api/exportExcel?Authorization={token}&ids={ids}`（通过 iframe 下载） |

#### 使用的 Element Plus 组件

`el-card`, `el-button`, `el-table`, `el-table-column`, `el-pagination`

---

### 6.8 交易管理模块

**入口文件**：`src/view/TranView.vue`（列表）、`src/view/TranDetailView.vue`（详情）、`src/view/TranApproveView.vue`（审批）、`src/view/TranInvoiceView.vue`（开票）
**关联 API**：`src/api/tran.js`, `src/api/product.js`, `src/api/customer.js`

#### TranView — 交易列表

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载交易列表** | 分页展示交易（交易编号、客户名称、金额、状态、创建时间） | `GET /api/tran/list?page=N&size=M&tranNo=&customerName=&stage=` |
| **搜索** | 按交易编号、客户名称、状态筛选 | - |
| **新增交易** | 弹窗选择客户 + 选择产品（支持多产品，含数量）+ 填写描述 + 预计交付日期 | `POST /api/tran/create` |
| **编辑交易** | 弹窗加载交易详情编辑（仅 QUOTATION 状态可编辑） | `PUT /api/tran/update` |
| **查看交易详情** | 跳转到 `/dashboard/tran/{id}` | - |
| **审批交易** | 跳转到 `/dashboard/tran/approve/{id}`（仅 PENDING 状态可操作） | - |
| **开票** | 跳转到 `/dashboard/tran/invoice/{id}`（仅 APPROVED 状态可操作） | - |
| **删除交易** | 确认后删除（仅 QUOTATION 状态可操作） | `DELETE /api/tran/{id}` |
| **批量删除** | 勾选多个后批量删除 | `POST /api/tran/batch-delete` (body: { ids }) |

#### 交易状态流转

```
QUOTATION(待报价/41) → PENDING(待审批/42) → APPROVED(已审批/43) → COMPLETED(已完成/46)
```

#### TranDetailView — 交易详情

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载交易详情** | 展示交易基本信息 | `GET /api/tran/{id}` |
| **加载交易产品** | 展示产品列表（名称、数量、单价、小计） | `GET /api/tran/products/{id}` |
| **加载发票信息** | 展示已开发票列表 | `GET /api/tran/invoice/{id}` |
| **加载促销列表** | 获取可用促销活动供选择 | `GET /api/product-promotions` |
| **选择促销** | 选择促销后自动计算折扣价格（支持折扣/满减/直降） | - |
| **结算交易** | 确认结算，可传入折扣后金额 | `PUT /api/tran/settle/{id}` 或 `PUT /api/tran/settle/{id}` (body: { amount }) |
| **审批** | 跳转审批页面 | - |
| **开票** | 跳转开票页面 | - |

#### TranApproveView — 交易审批

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载交易详情** | 展示交易基本信息 + 产品列表 | `GET /api/tran/{id}` + `GET /api/tran/products/{id}` |
| **提交审批** | 选择通过/拒绝 + 填写审批意见（≥5字符）提交 | `PUT /api/tran/approve/{id}` (body: { approved, comment }) |

#### TranInvoiceView — 交易开票

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载交易详情** | 展示交易信息 + 产品列表 | `GET /api/tran/{id}` + `GET /api/tran/products/{id}` |
| **加载发票列表** | 获取已有发票记录 | `GET /api/tran/invoice/{id}` |
| **开具发票** | 填写发票信息（类型、抬头、税号、开户行、银行账号、地址、电话、金额、备注），一个交易只能开一次票 | `POST /api/tran/invoice` → `PUT /api/tran/invoice/{id}/status` (status: ISSUED) → `PUT /api/tran/update` (stage: 46) |

#### 发票类型

- `VAT_NORMAL`：增值税普通发票
- `VAT_SPECIAL`：增值税专用发票（需填写开户行、银行账号、注册地址、注册电话）

#### 使用的 Element Plus 组件

`el-card`, `el-form`, `el-form-item`, `el-input`, `el-select`, `el-option`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-tag`, `el-descriptions`, `el-descriptions-item`, `el-input-number`, `el-date-picker`, `el-radio-group`, `el-radio`, `el-alert`

---

### 6.9 商品管理模块

**入口文件**：`src/view/ProductView.vue`（产品列表）、`src/view/ProductCategoryView.vue`（分类）、`src/view/ProductPromotionView.vue`（促销）、`src/view/ProductStockAlertView.vue`（库存预警）
**关联 API**：`src/api/product.js`

#### ProductView — 产品列表

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载产品列表** | 分页展示产品（SKU、名称、分类、规格、价格、库存、状态） | `GET /api/products?page=N&size=M` |
| **新增产品** | 弹窗填写 SKU、名称、分类（下拉）、规格、价格、库存、最低库存、状态 | `POST /api/products` |
| **编辑产品** | 弹窗加载产品数据编辑 | `PUT /api/products/{id}` |
| **删除产品** | 确认后删除 | `DELETE /api/products/{id}` |
| **分类管理** | 跳转到 `/dashboard/product/category` | - |
| **促销设置** | 跳转到 `/dashboard/product/promotion` | - |
| **库存预警** | 跳转到 `/dashboard/product/stock` | - |

#### ProductCategoryView — 分类管理

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载分类列表** | 分页展示分类（名称、编码、描述、排序、状态） | `GET /api/product-categories?page=N&size=M` |
| **新增分类** | 弹窗填写名称、编码、描述、排序、状态 | `POST /api/product-categories` |
| **编辑分类** | 弹窗加载分类数据编辑 | `PUT /api/product-categories/{id}` |
| **删除分类** | 确认后删除 | `DELETE /api/product-categories/{id}` |
| **返回** | 返回产品列表页 | - |

#### ProductPromotionView — 促销管理

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载促销列表** | 分页展示促销（名称、类型、折扣/金额、开始/结束时间、状态） | `GET /api/product-promotions?page=N&size=M` |
| **新增促销** | 弹窗填写名称、类型（折扣/满减/直降）、折扣值、开始/结束时间、状态（未开始/进行中/已结束） | `POST /api/product-promotions` |
| **编辑促销** | 弹窗加载促销数据编辑 | `PUT /api/product-promotions/{id}` |
| **删除促销** | 确认后删除 | `DELETE /api/product-promotions/{id}` |
| **返回** | 返回产品列表页 | - |

#### ProductStockAlertView — 库存预警

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载库存预警列表** | 分页展示低于最低库存的产品（SKU、名称、分类、规格、当前库存、最低库存、更新时间） | `GET /api/products/stockalerts?page=N&size=M&sku=&name=&category=` |
| **筛选** | 按 SKU、产品名称、分类筛选 | - |
| **补货** | 弹窗填写补货数量和备注提交 | `POST /api/productstock/restock` (body: { productId, quantity, remark }) |
| **查看库存变动记录** | 弹窗展示库存变动历史 | `GET /api/productstock/records/{id}?page=N&size=M` |
| **刷新数据** | 手动刷新列表 | - |
| **返回** | 返回产品列表页 | - |

#### 使用的 Element Plus 组件

`el-card`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-form`, `el-form-item`, `el-input`, `el-input-number`, `el-select`, `el-option`, `el-date-picker`, `el-tag`, `el-divider`

---

### 6.10 字典管理模块

**入口文件**：`src/view/DictTypeView.vue`（字典类型）、`src/view/DictValueView.vue`（字典值）
**关联 API**：`src/api/dict.js`

#### DictTypeView — 字典类型管理

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载字典类型列表** | 分页展示字典类型（类型代码、类型名称、备注） | `GET /api/dict/types?page=N&size=M&typeCode=&typeName=` |
| **搜索** | 按类型代码、类型名称搜索 | - |
| **新增字典类型** | 弹窗填写类型代码、类型名称、备注 | `POST /api/dict/type/create` |
| **编辑字典类型** | 弹窗加载数据编辑 | `PUT /api/dict/type/update/{id}` |
| **删除字典类型** | 确认后删除 | `DELETE /api/dict/type/delete/{id}` |
| **批量删除** | 勾选多个后批量删除 | `DELETE /api/dict/types/batch` (body: ids数组) |

#### DictValueView — 字典值管理

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **加载字典值列表** | 分页展示字典值（字典类型、字典值、排序、备注） | `GET /api/dict/values?page=N&size=M&typeCode=&typeValue=` |
| **加载字典类型下拉** | 获取所有字典类型用于下拉选择 | `GET /api/dict/types?page=1&size=100` |
| **搜索** | 按字典类型、字典值搜索 | - |
| **新增字典值** | 弹窗选择字典类型、填写字典值、排序、备注 | `POST /api/dict/value/create` |
| **编辑字典值** | 弹窗加载数据编辑 | `PUT /api/dict/value/update/{id}` |
| **删除字典值** | 确认后删除 | `DELETE /api/dict/value/delete/{id}` |
| **批量删除** | 勾选多个后批量删除 | `DELETE /api/dict/value/batch` (body: ids数组) |

#### 字典数据被其他模块引用

线索管理模块通过 `GET /api/dict/values?typeCode={typeCode}` 加载以下字典数据：
- `appellation` — 称呼
- `needLoan` — 是否贷款
- `intentionState` — 意向状态
- `clueState` — 线索状态
- `source` — 线索来源
- `noteWay` — 跟踪方式

#### 使用的 Element Plus 组件

`el-card`, `el-form`, `el-form-item`, `el-input`, `el-select`, `el-option`, `el-button`, `el-table`, `el-table-column`, `el-pagination`, `el-dialog`, `el-input-number`

---

### 6.11 系统管理模块

**入口文件**：`src/view/SystemView.vue`
**关联 API**：`src/api/system.js`

#### 核心业务功能

| 操作 | 功能说明 | API 调用 |
|------|----------|----------|
| **系统监控大屏** | 展示系统运行状态（操作系统、CPU、内存、运行时间等），使用 ECharts 渲染内存饼图、CPU 核心分布图、运行时间仪表盘 | `GET /api/monitor/all`, `GET /api/monitor/memory-info`, `GET /api/monitor/cpu-info`, `GET /api/monitor/system-info` |
| **自动刷新** | 每 1 秒自动刷新系统监控数据，可手动开启/关闭 | - |
| **手动刷新** | 点击按钮手动刷新系统信息 | - |
| **加载系统管理信息列表** | 展示系统配置列表（系统代码、名称、标题、描述、版本、状态） | `GET /api/system/list` |
| **新增系统信息** | 弹窗填写系统代码、名称、标题、网址、描述、Logo、快捷图标、联系电话、微信、邮箱、版本、地址、关闭提示、状态 | `POST /api/system/create` |
| **编辑系统信息** | 弹窗加载数据编辑 | `PUT /api/system/{id}` |
| **删除系统信息** | 确认后删除 | `DELETE /api/system/{id}` |
| **批量删除** | 勾选多个后批量删除 | `DELETE /api/system/batch` (body: ids数组) |
| **切换系统状态** | Switch 开关切换系统开启/关闭 | `PUT /api/system/{id}/status` (body: { isopen }) |

#### 监控数据来源优先级

1. **后端真实数据**（`/api/monitor/*`）— 优先
2. **浏览器 Web API**（`navigator`, `performance`）— 后端不可用时回退

#### 使用的 Element Plus 组件

`el-card`, `el-form`, `el-form-item`, `el-input`, `el-button`, `el-table`, `el-table-column`, `el-dialog`, `el-switch`, `el-descriptions`, `el-descriptions-item`, `el-tag`, `el-progress`, `el-badge`, `el-row`, `el-col`

---

## 7. API 接口清单

### src/api/user.js — 用户管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getUserList(params)` | GET | `/api/users` | 获取用户列表 |
| `getUserDetail(id)` | GET | `/api/user/{id}` | 获取用户详情 |
| `createUser(data)` | POST | `/api/user` | 新增用户 |
| `updateUser(data)` | PUT | `/api/user` | 编辑用户 |
| `deleteUser(id)` | DELETE | `/api/user/{id}` | 删除用户 |
| `batchDeleteUsers(ids)` | DELETE | `/api/user` | 批量删除用户 |

### src/api/clue.js — 线索管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getCurrentClues(current)` | GET | `/api/clues` | 获取线索分页列表 |
| `getClueDetail(id)` | GET | `/api/clue/detail/{id}` | 获取线索详情 |
| `addClue(formData)` | POST | `/api/clue` | 录入线索 |
| `updateClue(formData)` | PUT | `/api/clue` | 编辑线索 |
| `delClueById(id)` | DELETE | `/api/clue/{id}` | 删除线索 |
| `batchDeleteCluesByIds(ids)` | POST | `/api/clue/batch` | 批量删除线索 |
| `checkPhoneIsExist(phone)` | GET | `/api/clue/{phone}` | 校验手机号是否存在 |
| `importExcelAPI(file)` | POST | `/api/importExcel` | 导入 Excel |
| `getLoginInfo()` | GET | `/api/login/info` | 获取登录用户信息 |
| `addClueRemark(clueId, noteContent, noteWay)` | POST | `/api/clue/remark` | 添加线索跟踪记录 |
| `getClueRemarkList(current, clueId)` | GET | `/api/clue/remark` | 获取线索跟踪记录列表 |
| `convertClueToCustomer(clueId, product, description, nextContactTime)` | POST | `/api/clue/customer` | 线索转换为客户 |

### src/api/customer.js — 客户管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getCustomerList(params)` | GET | `/api/customer/list` | 获取客户列表 |
| `getCustomerOptions()` | GET | `/api/customer/options` | 获取客户选项（下拉用） |

### src/api/activity.js — 市场活动

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getActivityList(params)` | GET | `/api/activitys` | 获取活动列表 |
| `getOwnerList()` | GET | `/api/owner` | 获取负责人列表 |
| `getActivityById(id)` | GET | `/api/activity/{id}` | 获取活动详情 |
| `createActivity(formData)` | POST | `/api/activity` | 创建活动 |
| `updateActivity(formData)` | PUT | `/api/activity` | 更新活动 |
| `deleteActivity(id)` | DELETE | `/api/activity/{id}` | 删除活动 |
| `batchDeleteActivities(ids)` | POST | `/api/activity/batch` | 批量删除活动 |

### src/api/tran.js — 交易管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getTranList(params)` | GET | `/api/tran/list` | 获取交易列表 |
| `getTranDetail(id)` | GET | `/api/tran/{id}` | 获取交易详情 |
| `getTranProducts(id)` | GET | `/api/tran/products/{id}` | 获取交易产品详情 |
| `createTran(data)` | POST | `/api/tran/create` | 创建交易 |
| `updateTran(data)` | PUT | `/api/tran/update` | 更新交易 |
| `settleTran(id, amount)` | PUT | `/api/tran/settle/{id}` | 结算交易 |
| `approveTran(id, data)` | PUT | `/api/tran/approve/{id}` | 审批交易 |
| `getTranApprove(tranId)` | GET | `/api/tran/approve/info/{tranId}` | 获取交易审批信息 |
| `getTranStatus(id)` | GET | `/api/tran/status/{id}` | 获取交易状态 |
| `createInvoice(data)` | POST | `/api/tran/invoice` | 创建发票 |
| `getTranInvoiceList(tranId)` | GET | `/api/tran/invoice/{tranId}` | 获取交易发票列表 |
| `updateInvoiceStatus(invoiceId, status)` | PUT | `/api/tran/invoice/{invoiceId}/status` | 更新发票状态 |
| `deleteTran(id)` | DELETE | `/api/tran/{id}` | 删除交易 |
| `batchDeleteTran(ids)` | POST | `/api/tran/batch-delete` | 批量删除交易 |

### src/api/product.js — 商品管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getProductList(params)` | GET | `/api/products` | 获取产品列表 |
| `getProductDetail(id)` | GET | `/api/products/{id}` | 获取产品详情 |
| `createProduct(data)` | POST | `/api/products` | 新增产品 |
| `updateProduct(id, data)` | PUT | `/api/products/{id}` | 编辑产品 |
| `deleteProduct(id)` | DELETE | `/api/products/{id}` | 删除产品 |
| `getStockAlerts(params)` | GET | `/api/products/stockalerts` | 获取库存预警列表 |
| `restockProduct(data)` | POST | `/api/productstock/restock` | 补货 |
| `getStockRecords(id, params)` | GET | `/api/productstock/records/{id}` | 获取库存变动记录 |
| `getPromotionList(params)` | GET | `/api/product-promotions` | 获取促销列表 |
| `createPromotion(data)` | POST | `/api/product-promotions` | 新增促销 |
| `updatePromotion(id, data)` | PUT | `/api/product-promotions/{id}` | 编辑促销 |
| `deletePromotion(id)` | DELETE | `/api/product-promotions/{id}` | 删除促销 |
| `getCategoryList(params)` | GET | `/api/product-categories` | 获取分类列表 |
| `createCategory(data)` | POST | `/api/product-categories` | 新增分类 |
| `updateCategory(id, data)` | PUT | `/api/product-categories/{id}` | 编辑分类 |
| `deleteCategory(id)` | DELETE | `/api/product-categories/{id}` | 删除分类 |

### src/api/dict.js — 字典管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getDictTypeList(params)` | GET | `/api/dict/types` | 获取字典类型列表 |
| `getDictTypeDetail(id)` | GET | `/api/dict/type/get/{id}` | 获取字典类型详情 |
| `createDictType(data)` | POST | `/api/dict/type/create` | 新增字典类型 |
| `updateDictType(id, data)` | PUT | `/api/dict/type/update/{id}` | 编辑字典类型 |
| `deleteDictType(id)` | DELETE | `/api/dict/type/delete/{id}` | 删除字典类型 |
| `batchDeleteDictTypes(ids)` | DELETE | `/api/dict/types/batch` | 批量删除字典类型 |
| `getDictValueList(params)` | GET | `/api/dict/values` | 获取字典值列表 |
| `getDictValueDetail(id)` | GET | `/api/dict/value/get/{id}` | 获取字典值详情 |
| `createDictValue(data)` | POST | `/api/dict/value/create` | 新增字典值 |
| `updateDictValue(id, data)` | PUT | `/api/dict/value/update/{id}` | 编辑字典值 |
| `deleteDictValue(id)` | DELETE | `/api/dict/value/delete/{id}` | 删除字典值 |
| `batchDeleteDictValues(ids)` | DELETE | `/api/dict/value/batch` | 批量删除字典值 |
| `clearCache()` | GET | `/api/dict/clear` | 清除字典缓存 |

### src/api/system.js — 系统管理

| 函数名 | HTTP 方法 | 路径 | 说明 |
|--------|-----------|------|------|
| `getSystemList()` | GET | `/api/system/list` | 获取系统信息列表 |
| `getSystemDetail(id)` | GET | `/api/system/{id}` | 获取系统信息详情 |
| `createSystem(data)` | POST | `/api/system/create` | 创建系统信息 |
| `updateSystem(id, data)` | PUT | `/api/system/{id}` | 更新系统信息 |
| `deleteSystem(id)` | DELETE | `/api/system/{id}` | 删除系统信息 |
| `batchDeleteSystems(ids)` | DELETE | `/api/system/batch` | 批量删除系统信息 |
| `toggleSystemStatus(id, isopen)` | PUT | `/api/system/{id}/status` | 切换系统状态 |
| `getSystemMonitorInfo()` | GET | `/api/monitor/system-info` | 获取系统基本信息 |
| `getMemoryInfo()` | GET | `/api/monitor/memory-info` | 获取内存信息 |
| `getCpuInfo()` | GET | `/api/monitor/cpu-info` | 获取 CPU 信息 |
| `getDiskInfo()` | GET | `/api/monitor/disk-info` | 获取磁盘信息 |
| `getJvmInfo()` | GET | `/api/monitor/jvm-info` | 获取 JVM 信息 |
| `getNetworkInfo()` | GET | `/api/monitor/network-info` | 获取网络信息 |
| `getAllMonitorData()` | GET | `/api/monitor/all` | 获取所有监控数据 |

---

## 8. 页面跳转关系图

```
/login (/)
  │
  ├── [登录成功/免登录] ──→ /dashboard
  │
  └── /dashboard (DashboardView.vue — 主框架)
        │
        ├── /dashboard/ (默认) → StatisticView.vue (统计概览)
        │
        ├── /dashboard/user → UserView.vue (用户管理)
        │
        ├── /dashboard/activity → ActivityView.vue (活动列表)
        │     └── /dashboard/activity/:id → ActivityDetailView.vue (活动详情)
        │
        ├── /dashboard/clue → ClueView.vue (线索列表)
        │     └── /dashboard/clue/detail/:id → ClueDetailView.vue (线索详情)
        │           └── [转换客户] → 创建客户记录
        │
        ├── /dashboard/customer → CustomerView.vue (客户列表)
        │
        ├── /dashboard/product → ProductView.vue (产品列表)
        │     ├── /dashboard/product/category → ProductCategoryView.vue (分类管理)
        │     ├── /dashboard/product/promotion → ProductPromotionView.vue (促销管理)
        │     └── /dashboard/product/stock → ProductStockAlertView.vue (库存预警)
        │
        ├── /dashboard/tran → TranView.vue (交易列表)
        │     ├── /dashboard/tran/:id → TranDetailView.vue (交易详情)
        │     │     ├── [结算] → 调用 API 更新状态
        │     │     ├── [审批] → /dashboard/tran/approve/:id
        │     │     └── [开票] → /dashboard/tran/invoice/:id
        │     ├── /dashboard/tran/approve/:id → TranApproveView.vue (交易审批)
        │     └── /dashboard/tran/invoice/:id → TranInvoiceView.vue (交易开票)
        │
        ├── /dashboard/dict/type → DictTypeView.vue (字典类型)
        ├── /dashboard/dict/value → DictValueView.vue (字典值)
        │
        └── /dashboard/system → SystemView.vue (系统管理 + 系统监控)
```
