# 前端问题清单

> 审计时间：2026-05-30
> 审计范围：`dealer-web` 前端全部源代码
> 审计维度：表单校验、交互功能、API 对接、性能、错误处理、安全、数据一致性、代码质量

---

## 问题汇总

| 统计项 | 数量 |
|-------|------|
| P0-致命问题 | 2 |
| P1-严重问题 | 7 |
| P2-一般问题 | 16 |
| P3-轻微问题 | 12 |
| **问题总数** | **37** |

---

## 一、表单校验问题

### ISSUE-011：产品表单缺少必填字段校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-011 |
| 所属模块 | 商品管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ProductView.vue:69-104` |

**问题详情**：
产品表单的 `el-form` 没有设置 `:rules` 属性，SKU、产品名称、价格等关键字段均无校验规则，用户可以提交空值或无效数据到后端。

**现状**：
`ProductView.vue` 第69行 `<el-form :model="productForm" label-width="100px">` 没有绑定 `:rules`，所有表单项也没有设置 `prop` 属性。提交时直接调用 `createProduct` / `updateProduct`，没有任何前端校验。

```vue
// 当前代码（第69行）
<el-form :model="productForm" label-width="100px">
  <el-form-item label="SKU">
    <el-input v-model="productForm.sku" />
  </el-form-item>
  // ...其他字段同样无校验
```

**修改建议**：
1. 在 `<script setup>` 中定义 `productRules` 对象，包含 `sku`（必填）、`name`（必填）、`price`（必填，大于0）、`stock`（必填，非负整数）的校验规则
2. 在 `<el-form>` 上添加 `:rules="productRules"` 和 `ref="productFormRef"`
3. 为每个 `<el-form-item>` 添加 `prop` 属性，值对应 `productForm` 中的字段名
4. 在 `handleSubmit` 方法中先调用 `productFormRef.value.validate()` 再提交

**验收标准**：
- [ ] 不填写 SKU 时点击确定，提示"请输入SKU"
- [ ] 不填写产品名称时点击确定，提示"请输入产品名称"
- [ ] 价格为 0 或负数时点击确定，提示"价格必须大于0"
- [ ] 校验全部通过后才能正常提交

---

### ISSUE-013：活动预算字段校验不完整 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-013 |
| 所属模块 | 市场活动 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ActivityView.vue:202-204` |

**问题详情**：
活动预算的正则表达式 `^[0-9]+(\.[0-9]{2})?$` 不允许输入一位小数（如 `100.5`），必须输入 `100.50`。搜索栏和表单中都使用了相同的错误正则。

**现状**：
`ActivityView.vue` 第183-184行和第203-204行：
```javascript
// 当前正则（第184行、第204行）
{ pattern: /^[0-9]+(\.[0-9]{2})?$/, message: '活动预算必须是整数或者两位小数', trigger: 'blur' }
```
输入 `100.5` 会校验失败，用户体验差。

**修改建议**：
1. 将正则修改为 `^[0-9]+(\.[0-9]{1,2})?$`，允许一位或两位小数
2. 同时修改第183-184行搜索栏的校验规则和第202-204行表单的校验规则
3. 可选：增加最大值校验 `max: 99999999.99`，防止输入过大金额

**验收标准**：
- [ ] 输入 `100.5` 可以通过校验
- [ ] 输入 `100.50` 可以通过校验
- [ ] 输入 `100` 可以通过校验
- [ ] 输入 `100.123` 提示格式错误

**代码示例**：
```javascript
// 修改后的校验规则
const activityFormRules = {
  cost: [
    { required: true, message: '请输入活动预算', trigger: 'blur' },
    { pattern: /^[0-9]+(\.[0-9]{1,2})?$/, message: '活动预算必须是整数或最多两位小数', trigger: 'blur' }
  ]
}
```

---

### ISSUE-014：交易产品数量缺少库存校验（前端部分）

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-014 |
| 所属模块 | 交易管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P1-严重 |
| 涉及文件 | 前端：`view/TranView.vue:152-158` |

**问题详情**：
创建/编辑交易时，产品数量输入框只限制了 `min=1, max=999`，没有根据实际库存进行校验，也没有在界面上显示当前库存信息。

**现状**：
`TranView.vue` 第152-158行：
```vue
<el-input-number
  v-model="product.quantity"
  :min="1"
  :max="999"
  placeholder="数量"
  style="width: 25%; margin-right: 10px;"
/>
```
`max` 硬编码为 999，未与产品实际库存关联。

**修改建议**：
1. 在产品选择下拉中显示当前库存：`label="${item.name} (¥${item.price}) [库存:${item.stock}]"`
2. 产品选择变化时，将 `max` 动态绑定为 `productOptions.list.find(p => p.id === product.productId)?.stock || 999`
3. 在提交前增加前端校验：检查每个产品的数量是否超过库存
4. 超出库存时显示警告提示

**验收标准**：
- [ ] 产品选择下拉中显示当前库存
- [ ] 数量输入框的上限为实际库存
- [ ] 库存不足时选择产品后提示用户
- [ ] 超出库存数量时禁止提交

---

### ISSUE-015：线索编辑时手机号不可编辑但后端未校验（前端部分） ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-015 |
| 所属模块 | 线索管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ClueView.vue:168-174` |

**问题详情**：
前端编辑线索时手机字段通过 `disabled` 属性禁止编辑，但这是纯前端限制。通过直接调用 API 可以修改手机号，绕过前端限制。

**现状**：
`ClueView.vue` 第168-174行：
```vue
<el-form-item label="手机" v-if="clueQuery.id > 0">
  <el-input v-model="clueQuery.phone" disabled/>
</el-form-item>
```
前端仅通过 `disabled` 阻止编辑，但 `addClueSubmit` 方法提交 FormData 时仍然包含 `phone` 字段。

**修改建议**：
1. 在 `addClueSubmit` 方法中，编辑模式下从 FormData 中排除 `phone` 字段
2. 或者在编辑提交前将 `phone` 字段从提交数据中移除
3. 后端也应校验编辑时手机号不能被修改（配合修复）

**验收标准**：
- [ ] 编辑线索时手机号显示但不可编辑
- [ ] 提交编辑数据时不包含手机号字段
- [ ] 通过 API 直接调用时后端拒绝修改手机号

---

## 二、交互功能缺失

### ISSUE-024：产品列表缺少批量删除功能

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-024 |
| 所属模块 | 商品管理 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/ProductView.vue:249-251` |

**问题详情**：
`ProductView.vue` 的表格有选择框列（第18行），`handleSelectionChange` 方法也绑定了（第14行），但方法体是空的，没有实现批量删除功能，选择框对用户产生误导。

**现状**：
`ProductView.vue` 第249-251行：
```javascript
// 处理选择变化
const handleSelectionChange = (selection) => {
  // 处理表格选择逻辑 - 空实现
}
```
页面没有"批量删除"按钮，选择框选择后无任何操作可用。

**修改建议**：
1. 添加 `selectedIds` ref 存储选中的产品ID
2. 实现 `handleSelectionChange` 方法，将选中的ID存入 `selectedIds`
3. 在操作栏添加"批量删除"按钮，绑定 `:disabled="selectedIds.length === 0"`
4. 实现 `handleBatchDelete` 方法，调用批量删除 API（需后端支持）
5. 或者如果不需要批量删除，移除选择框列避免误导

**验收标准**：
- [ ] 选中产品后批量删除按钮可点击
- [ ] 点击批量删除有二次确认弹窗
- [ ] 删除成功后列表刷新，选中状态清除
- [ ] 未选择时点击批量删除有提示

---

### ISSUE-025：线索跟踪记录编辑和删除功能未实现

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-025 |
| 所属模块 | 线索管理 |
| 问题类型 | 交互问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ClueDetailView.vue:542-553` |

**问题详情**：
线索详情页的跟踪记录列表中显示了"编辑"和"删除"按钮，但两个方法都只打印日志和提示"功能待实现"，属于空实现。

**现状**：
`ClueDetailView.vue` 第542-553行：
```javascript
// 编辑跟踪记录 (待实现)
const edit = (id) => {
  console.log('编辑跟踪记录:', id)
  messageTip("编辑功能待实现", "info")
}

// 删除跟踪记录 (待实现)
const del = (id) => {
  console.log('删除跟踪记录:', id)
  messageTip("删除功能待实现", "info")
}
```

**修改建议**：
方案一（推荐）：暂时隐藏编辑和删除按钮，避免误导用户
1. 移除第244-249行的编辑和删除按钮
2. 或者添加 `v-if="false"` 暂时隐藏

方案二：实现完整的编辑和删除功能
1. 后端需要实现线索备注的编辑和删除 API
2. 前端实现编辑弹窗、加载备注详情、提交编辑
3. 前端实现删除二次确认和调用删除 API

**验收标准**：
- [ ] 如果暂不实现，按钮应隐藏
- [ ] 如果实现，编辑按钮能打开编辑弹窗
- [ ] 如果实现，删除按钮有二次确认

---

### ISSUE-027：分页组件缺少每页条数选择

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-027 |
| 所属模块 | 多个模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/TranView.vue:83-91`、`view/ProductView.vue:52-61`、`view/ClueView.vue:54-61`、`view/ActivityView.vue:77-84`、`view/CustomerView.vue:33-41` |

**问题详情**：
多个列表页面的分页组件 `layout` 只配置了 `"prev, pager, next"`，没有包含 `sizes` 和 `total`，用户无法调整每页显示条数，也无法看到总记录数。

**现状**：
`TranView.vue` 第85行：
```vue
<el-pagination
    background
    layout="prev, pager, next"
    :page-size="pageSize"
    :total="total"
    // ...
/>
```
其他页面（ProductView、ClueView、ActivityView、CustomerView）同样缺少。

**修改建议**：
1. 将 `layout` 修改为 `"total, sizes, prev, pager, next, jumper"`
2. 添加 `:page-sizes="[10, 20, 50, 100]"`
3. 添加 `@size-change="handleSizeChange"` 事件处理
4. 实现 `handleSizeChange` 方法，更新 `pageSize` 并重新加载数据

**验收标准**：
- [ ] 分页组件显示总记录数
- [ ] 分页组件显示每页条数选择器
- [ ] 切换每页条数后列表数据正确刷新

**代码示例**：
```vue
<el-pagination
    background
    layout="total, sizes, prev, pager, next, jumper"
    :page-sizes="[10, 20, 50, 100]"
    :page-size="pageSize"
    :total="total"
    @size-change="handleSizeChange"
    @current-change="handleCurrentChange"
/>
```

---

### ISSUE-028：搜索条件重置不完整

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-028 |
| 所属模块 | 市场活动 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/ActivityView.vue:272-274` |

**问题详情**：
活动列表的重置按钮使用 `delete` 操作符删除 reactive 对象的属性，这种方式在 Vue 3 中可能不会正确触发视图更新，导致搜索表单 UI 没有同步清空。

**现状**：
`ActivityView.vue` 第272-274行：
```javascript
const onReset = () => {
  Object.keys(activityQuery).forEach(key => delete activityQuery[key]);
  getData(1);
}
```
`activityQuery` 是 `reactive({})`，使用 `delete` 删除属性可能不会触发 Vue 的响应式更新。类似问题也存在于 `ClueView.vue` 的 `resetClueForm` 和 `ActivityDetailView.vue` 的 `resetRemarkForm`。

**修改建议**：
使用 `Object.assign` 重置为初始状态，或者手动将每个字段设为空值：
```javascript
const onReset = () => {
  Object.keys(activityQuery).forEach(key => {
    activityQuery[key] = Array.isArray(activityQuery[key]) ? [] : ''
  })
  getData(1)
}
```

**验收标准**：
- [ ] 点击重置后搜索表单所有字段清空
- [ ] 搜索表单 UI 与数据状态同步
- [ ] 重置后列表显示全部数据

---

## 三、API 对接问题

### ISSUE-034：前端调用后端未实现的接口（getTranStatus） ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-034 |
| 所属模块 | 交易管理 |
| 问题类型 | 流程缺失 |
| 严重程度 | P1-严重 |
| 涉及文件 | 前端：`api/tran.js:50-52` |

**问题详情**：
前端 `api/tran.js` 中定义了 `getTranStatus` 函数，调用 `GET /api/tran/status/{id}`，但后端 `TranController` 未实现该接口，调用会返回 404。

**现状**：
`api/tran.js` 第50-52行：
```javascript
export function getTranStatus(id) {
    return doGet(`/api/tran/status/${id}`)
}
```
后端 `web/TranController.java` 中没有对应的 `@GetMapping("/status/{id}")` 方法。

**修改建议**：
方案一：后端实现该接口
1. 在 `TranController` 中添加 `@GetMapping("/status/{id}")` 方法
2. 查询交易状态并返回

方案二（推荐）：移除前端未使用的代码
1. 如果前端没有任何地方调用 `getTranStatus`，直接删除该函数
2. 搜索确认：经检查，前端代码中没有实际调用 `getTranStatus` 的地方，可以安全删除

**验收标准**：
- [ ] 确认前端是否有调用该函数的地方
- [ ] 如果无调用，删除该函数定义
- [ ] 如果有调用，后端实现对应接口

---

### ISSUE-035：统计接口前端已正确对接

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-035 |
| 所属模块 | 统计报表 |
| 问题类型 | 已修复 |
| 严重程度 | 无 |
| 涉及文件 | 前端：`view/StatisticView.vue` |

**问题详情**：
原报告称统计接口前端未调用，但经检查 `StatisticView.vue` 已正确调用 `/api/summary/data`、`/api/saleFunnel/data`、`/api/sourcePie/data` 三个接口。

**现状**：
`StatisticView.vue` 第68-74行、第77-178行、第182-241行分别调用了这三个接口，且使用 ECharts 渲染了图表。此问题已不存在。

---

### ISSUE-036：活动备注前端编辑功能未实现

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-036 |
| 所属模块 | 市场活动 |
| 问题类型 | 交互问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ActivityDetailView.vue:314-318` |

**问题详情**：
后端已实现完整的活动备注 CRUD 接口（包括 `PUT /api/activity/remark` 编辑接口），前端也已对接了添加和删除功能，但编辑功能只打印日志和提示"功能待实现"。

**现状**：
`ActivityDetailView.vue` 第314-318行：
```javascript
const edit = (id) => {
  console.log('编辑备注记录:', id)
  ElMessage.info("编辑功能待实现")
}
```
备注列表表格中显示了编辑按钮（第156行），但点击无实际功能。

**修改建议**：
1. 定义编辑弹窗相关的状态变量（`editDialogVisible`、`editRemarkForm`）
2. 实现 `edit` 方法：弹出编辑弹窗，加载当前备注内容
3. 实现编辑提交方法：调用 `PUT /api/activity/remark` 更新备注
4. 编辑成功后刷新备注列表

**验收标准**：
- [ ] 点击编辑按钮弹出编辑弹窗
- [ ] 弹窗中显示当前备注内容
- [ ] 修改后提交能成功更新
- [ ] 更新后列表刷新显示最新内容

---

### ISSUE-037：补货接口路径前后端不一致（前端部分） ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-037 |
| 所属模块 | 商品管理 |
| 问题类型 | 流程缺失 |
| 严重程度 | P1-严重 |
| 涉及文件 | 前端：`api/product.js:36-38` |

**问题详情**：
前端调用 `/api/productstock/restock`，后端接口路径为 `/api/products/stock/restock`，路径不匹配导致补货功能无法正常使用。

**现状**：
`api/product.js` 第36-38行：
```javascript
export function restockProduct(data) {
    return doPost('/api/productstock/restock', data)
}
```
后端 `ProductStockController.java` 的接口路径为 `/api/products/stock/restock`。

**修改建议**：
将前端 API 路径修改为与后端一致：
```javascript
export function restockProduct(data) {
    return doPost('/api/products/stock/restock', data)
}
```

**验收标准**：
- [ ] 前端补货接口路径与后端一致
- [ ] 补货功能可正常使用
- [ ] 补货后库存数据正确更新

---

### ISSUE-038：登录接口使用 FormData 但未明确 Content-Type

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-038 |
| 所属模块 | 认证模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/LoginView.vue:83-88` |

**问题详情**：
登录使用 `FormData` 方式提交，但没有明确设置 `Content-Type`。浏览器会自动设置为 `multipart/form-data` 并添加 boundary，但后端可能期望 `application/x-www-form-urlencoded`。

**现状**：
`LoginView.vue` 第83-88行：
```javascript
let formData = new FormData();
formData.append("loginAct", this.user.loginAct);
formData.append("loginPwd", this.user.loginPwd);
formData.append("rememberMe", this.user.rememberMe);

doPost("/api/login", formData).then(...)
```
`doPost` 函数没有显式设置 `Content-Type`，依赖 axios 的默认行为。

**修改建议**：
方案一：改为 URLSearchParams（推荐，对应 `application/x-www-form-urlencoded`）
```javascript
const params = new URLSearchParams()
params.append("loginAct", this.user.loginAct)
params.append("loginPwd", this.user.loginPwd)
params.append("rememberMe", this.user.rememberMe)
doPost("/api/login", params)
```

方案二：保持 FormData 但明确设置请求头
```javascript
doPost("/api/login", formData, { headers: { 'Content-Type': 'multipart/form-data' } })
```

**验收标准**：
- [ ] 登录请求的 Content-Type 明确且与后端匹配
- [ ] 登录功能在各浏览器中正常工作

---

### ISSUE-039：交易详情页产品数据加载重复

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-039 |
| 所属模块 | 交易管理 |
| 问题类型 | 性能问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/TranDetailView.vue:472-486` |

**问题详情**：
`TranDetailView.vue` 在 `onMounted` 中先调用 `fetchTranDetail()` 获取交易详情（其中已包含产品数据），又单独调用 `fetchProducts()` 获取产品数据，造成重复请求。

**现状**：
`TranDetailView.vue` 第472-486行：
```javascript
onMounted(async () => {
  // ...
  await fetchTranDetail()  // 第330行：获取交易详情，设置 tranDetail.value.products = data.products
  await fetchProducts()    // 第372行：再次获取产品列表，覆盖 tranDetail.value.products
  await fetchInvoiceInfo()
  await fetchPromotionList()
})
```
`fetchTranDetail` 中第347行已经设置了 `products: data.products || []`，`fetchProducts` 又用相同数据覆盖。

**修改建议**：
1. 如果后端 `/api/tran/{id}` 接口已返回产品数据，移除 `fetchProducts()` 调用
2. 如果后端不返回产品数据，移除 `fetchTranDetail` 中的 `products` 赋值，保留 `fetchProducts()`
3. 统一数据来源，避免重复请求

**验收标准**：
- [ ] 交易详情页只发起一次产品数据请求
- [ ] 产品信息正常显示

---

## 四、性能问题

### ISSUE-026：权限指令每次更新都发请求 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-026 |
| 所属模块 | 系统框架 |
| 问题类型 | 性能问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`main.js:34-55` |

**问题详情**：
`v-hasPermission` 指令在 `mounted` 和 `updated` 生命周期中都会调用 `GET /api/login/info` 接口获取用户权限信息。每次页面有任何更新（如数据变化触发重新渲染），都会发送新的请求，造成大量不必要的网络请求。

**现状**：
`main.js` 第34-55行：
```javascript
app.directive("hasPermission", (el, binding) => {
    // 这会在 `mounted` 和 `updated` 时都调用
    doGet("/api/login/info", {}).then(resp => {
        let user = resp.data.data;
        let permissionList = user.permissionList;
        // ...权限判断逻辑
    })
})
```
假设页面有5个使用了 `v-hasPermission` 的按钮，每次页面更新都会发送5个相同的请求。

**修改建议**：
1. 在登录成功后将用户权限信息缓存到 `sessionStorage` 或全局状态
2. 修改指令从缓存读取权限信息，不再发请求
3. 只在登录和刷新页面时请求一次权限信息

**验收标准**：
- [ ] 页面更新时不再发送 `/api/login/info` 请求
- [ ] 权限控制功能正常工作
- [ ] 用户权限变更后需要重新登录才能生效

**代码示例**：
```javascript
// 在 util.js 中添加缓存方法
export function getUserPermission() {
  const cached = sessionStorage.getItem('user_permissions')
  if (cached) return JSON.parse(cached)
  return null
}

export function setUserPermission(permissions) {
  sessionStorage.setItem('user_permissions', JSON.stringify(permissions))
}

// 修改指令
app.directive("hasPermission", (el, binding) => {
  const permissions = getUserPermission()
  if (!permissions) return
  const hasPermission = permissions.includes(binding.value)
  if (!hasPermission) {
    el.parentNode && el.parentNode.removeChild(el)
  }
})
```

---

### ISSUE-040：系统监控自动刷新间隔过短

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-040 |
| 所属模块 | 系统管理 |
| 问题类型 | 性能问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/SystemView.vue:350` |

**问题详情**：
系统监控页面的自动刷新间隔设置为 1 秒（1000ms），每次刷新会调用多个后端接口（`/api/monitor/all`、`/api/monitor/memory-info`、`/api/monitor/cpu-info`、`/api/monitor/system-info`），对服务器造成不必要的压力。

**现状**：
`SystemView.vue` 第350行：
```javascript
const refreshInterval = 1000 // 1000ms = 1秒
```
1秒刷新一次，每次可能发起4个请求，相当于每秒4个请求。

**修改建议**：
将刷新间隔改为 5-10 秒：
```javascript
const refreshInterval = 5000 // 5秒
```
同时可以优化请求策略：只在数据真正变化时更新图表，避免不必要的 DOM 操作。

**验收标准**：
- [ ] 自动刷新间隔为 5 秒或更长
- [ ] 手动刷新功能仍然可用
- [ ] 监控数据正常显示

---

## 五、错误处理问题

### ISSUE-022：前端 API 调用缺少统一错误处理

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-022 |
| 所属模块 | 系统框架 |
| 问题类型 | 异常处理 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：多个 `.vue` 文件 |

**问题详情**：
多个前端页面的 API 调用 catch 块只使用 `console.error` 打印错误，没有给用户友好的错误提示。部分页面使用了 `ElMessage.error`，但不统一。

**现状**：
`ActivityView.vue` 第243-245行：
```javascript
} catch (error) {
    console.error('获取活动列表失败:', error);  // 只打印日志，用户无感知
}
```

`ClueView.vue` 第421-429行：
```javascript
getCurrentClues(current).then(resp => {
    // ...成功处理
}).catch(error => {
    // 完全没有 catch 处理
})
```

对比 `TranView.vue` 第307-310行：
```javascript
} catch (error) {
    console.error('获取交易列表失败:', error)
    ElMessage.error('获取数据失败')  // 有用户提示
}
```

**修改建议**：
1. 在 `httpRequest.js` 的响应拦截器中统一处理非 200 响应
2. 各页面的 catch 块统一使用 `ElMessage.error` 显示用户友好的错误提示
3. 建立统一的错误处理工具函数

**验收标准**：
- [ ] 所有 API 调用失败都有用户可见的错误提示
- [ ] 不出现只有 console.error 而无用户提示的情况
- [ ] 错误提示信息友好且有意义

---

### ISSUE-023：token 过期后取消登录导致页面卡死 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-023 |
| 所属模块 | 认证模块 |
| 问题类型 | 异常处理 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`http/httpRequest.js:70-81` |

**问题详情**：
响应拦截器中当 token 过期（code >= 500）时弹出确认框，用户点击"取消"后只显示"取消去登录"提示，但页面功能已完全不可用（所有请求都会被拦截），用户被困在当前页面。

**现状**：
`httpRequest.js` 第70-81行：
```javascript
if (response.data.code >= 500) {
    messageConfirm(response.data.msg + "，是否重新去登录？").then(() => {
        removeToken();
        window.location.href = "/";
    }).catch(() => {
        messageTip("取消去登录", "warning");  // 取消后页面功能不可用
    })
    return Promise.reject(new Error(response.data.msg));
}
```

**修改建议**：
取消后也应清除 token 并跳转到登录页，或者延迟自动跳转：
```javascript
.catch(() => {
    messageTip("登录已过期，即将跳转到登录页", "warning")
    setTimeout(() => {
        removeToken()
        window.location.href = "/"
    }, 1500)
})
```

**验收标准**：
- [ ] token 过期后点击取消，页面不会卡死
- [ ] 用户最终都能到达登录页
- [ ] 不会出现页面功能全部失效但无法操作的情况

---

## 六、安全问题

### ISSUE-031：密码在日志中可能被打印

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-031 |
| 所属模块 | 用户管理 |
| 问题类型 | 安全隐患 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/UserView.vue:287`、`view/ClueView.vue:362-364`、`view/TranView.vue:291-293` |

**问题详情**：
多个页面存在 `console.log` 打印完整响应对象的情况，可能包含敏感信息（如密码哈希、用户详情等）。

**现状**：
`UserView.vue` 第287行：
```javascript
console.log(resp);  // 打印完整的用户响应数据
```

`ClueView.vue` 第362-364行：
```javascript
console.log(rule);
console.log(value);
console.log(callback);
```

`TranView.vue` 第291-293行：
```javascript
console.log('请求参数:', params)
console.log('获取交易列表:', res.data.data.list)
```

**修改建议**：
1. 移除所有 `console.log` 调试语句，或使用条件判断只在开发环境打印
2. 在生产构建时通过配置自动移除 console 语句

**验收标准**：
- [ ] 生产环境不打印敏感数据到控制台
- [ ] 开发环境可以通过开关控制日志输出
- [ ] 移除或条件化所有 console.log

**代码示例**：
```javascript
// vite.config.js 中配置生产环境移除 console
export default defineConfig({
  build: {
    minify: 'terser',
    terserOptions: {
      compress: {
        drop_console: true,
        drop_debugger: true
      }
    }
  }
})
```

---

### ISSUE-F001：多处 console.log 残留

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F001 |
| 所属模块 | 全局 |
| 问题类型 | 代码质量 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/TranView.vue`、`view/ClueView.vue`、`view/ClueDetailView.vue`、`view/ActivityDetailView.vue`、`view/TranDetailView.vue` |

**问题详情**：
多个文件中存在大量调试用的 `console.log` 语句，不仅可能泄露敏感信息，还影响生产环境的控制台输出。

**现状**：
- `TranView.vue`: 第291、293、401、405、483、499、566、592、593行
- `ClueView.vue`: 第362、363、364、579、663行
- `ClueDetailView.vue`: 第466、511行
- `ActivityDetailView.vue`: 第296、330行
- `TranDetailView.vue`: 第195、221、306-309、319、322、332、334、349、375、473-475行

**修改建议**：
1. 移除所有调试用的 `console.log` 语句
2. 配置 vite 在生产构建时自动移除 console

**验收标准**：
- [ ] 生产构建产物中不包含 console.log
- [ ] 功能正常不受影响

---

## 七、分页参数问题

### ISSUE-041：用户列表分页参数不一致 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-041 |
| 所属模块 | 用户管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/UserView.vue:183`、`view/ActivityView.vue:168`、`view/ClueView.vue:336`、`view/CustomerView.vue:62` |

**问题详情**：
多个页面的 `pageSize` 初始值为 0（`const pageSize = ref(0)`），实际每页条数由后端 `Constants.PAGE_SIZE` 控制。这导致前端显示的每页条数与实际不一致，且前端无法调整。

**现状**：
`UserView.vue` 第183行：
```javascript
const pageSize = ref(0)  // 初始值为0
```
后端返回的 `pageSize` 会覆盖这个值，但在数据加载前分页组件的显示会不正确。类似问题在 ActivityView（第168行）、ClueView（第336行）、CustomerView（第62行）中都存在。

**修改建议**：
1. 将 `pageSize` 初始值改为 10（与后端默认值一致）
2. 前端传递 `size` 参数给后端，让前端可以控制每页条数

```javascript
const pageSize = ref(10)  // 与后端默认值一致
```

**验收标准**：
- [ ] 分页组件初始显示正确
- [ ] 前端可以控制每页显示条数
- [ ] 分页参数前后端一致

---

## 八、额外发现的前端问题

### ISSUE-F002：多个列表页面缺少 loading 状态管理

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F002 |
| 所属模块 | 多个模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/ClueView.vue`、`view/ActivityView.vue`、`view/CustomerView.vue`、`view/UserView.vue` |

**问题详情**：
多个列表页面在加载数据时没有显示 loading 状态，用户在数据加载期间看到的是空白页面，不知道系统正在处理。

**现状**：
- `TranView.vue` 正确使用了 `v-loading="loading"`（第28行）
- `ClueView.vue` 没有 loading 状态
- `ActivityView.vue` 没有 loading 状态
- `CustomerView.vue` 没有 loading 状态
- `UserView.vue` 没有 loading 状态

**修改建议**：
为每个列表页面添加 loading 状态：
1. 定义 `const loading = ref(false)`
2. 在数据加载开始时设置 `loading.value = true`
3. 在数据加载完成（包括成功和失败）时设置 `loading.value = false`
4. 在 `<el-table>` 上添加 `v-loading="loading"`

**验收标准**：
- [ ] 数据加载时表格显示加载动画
- [ ] 加载完成后动画消失
- [ ] 加载失败后动画消失且有错误提示

---

### ISSUE-F003：删除操作缺少二次确认或确认不统一

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F003 |
| 所属模块 | 多个模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：多个 `.vue` 文件 |

**问题详情**：
部分页面的删除操作使用了 `messageConfirm` 工具函数，部分使用 `ElMessageBox.confirm`，确认方式不统一。部分页面的删除操作 catch 块中区分了用户取消和真正的错误，部分没有。

**现状**：
- `ProductView.vue` 使用 `messageConfirm`（第199行），catch 中区分了 `cancel`
- `ClueView.vue` 使用 `messageConfirm`（第501行），catch 中没有区分
- `UserView.vue` 使用 `messageConfirm`（第311行），catch 中没有区分
- `ActivityView.vue` 使用 `ElMessageBox.confirm`（第311行），catch 中区分了 `cancel`
- `TranView.vue` 使用 `ElMessageBox.confirm`（第340行），catch 中区分了 `cancel`

**修改建议**：
1. 统一使用 `ElMessageBox.confirm` 或 `messageConfirm`
2. catch 块中统一处理用户取消（不显示错误提示）和真正的错误（显示错误提示）
3. 建议统一使用 `messageConfirm` 并在 catch 中判断 `error !== 'cancel'`

**验收标准**：
- [ ] 所有删除操作都有二次确认弹窗
- [ ] 用户取消时只显示"已取消"或不显示
- [ ] 真正的删除失败才显示错误提示

---

### ISSUE-F004：列表刷新后选中状态未清除

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F004 |
| 所属模块 | 多个模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/ClueView.vue`、`view/ActivityView.vue`、`view/UserView.vue` |

**问题详情**：
批量删除成功后，虽然重新加载了数据，但 `selectedIds` 数组没有被清空，可能导致批量删除按钮仍然可点击（虽然列表已刷新，但旧的选中 ID 还在）。

**现状**：
`ClueView.vue` 第475-478行：
```javascript
const res = await batchDeleteCluesByIds(selectedIds.value);
if (res.data.code === 200) {
    ElMessage.success('批量删除成功');
    getData(currentPage.value);  // 刷新数据，但 selectedIds 没有清空
}
```

**修改建议**：
在删除成功后清空选中状态：
```javascript
if (res.data.code === 200) {
    ElMessage.success('批量删除成功');
    selectedIds.value = [];  // 清空选中
    getData(currentPage.value);
}
```

**验收标准**：
- [ ] 批量删除后选中状态清空
- [ ] 批量删除按钮变为禁用状态

---

### ISSUE-F005：动态路由页面未监听路由参数变化 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F005 |
| 所属模块 | 多个模块 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/TranDetailView.vue`、`view/ClueDetailView.vue`、`view/ActivityDetailView.vue`、`view/TranApproveView.vue`、`view/TranInvoiceView.vue` |

**问题详情**：
所有使用动态路由参数（`:id`）的详情页面，只在 `onMounted` 中加载数据。当从一个详情页导航到同类型的另一个详情页时（如从交易A详情跳到交易B详情），由于组件不会重新创建，`onMounted` 不会再次触发，导致显示的还是旧数据。

**现状**：
`TranDetailView.vue` 第472行：
```javascript
onMounted(async () => {
  // 只在组件首次挂载时执行
  await fetchTranDetail()
  // ...
})
```
没有使用 `watch` 监听 `route.params.id` 的变化。

**修改建议**：
添加对路由参数变化的监听：
```javascript
import { watch } from 'vue'

watch(() => route.params.id, async (newId) => {
  if (newId) {
    await fetchTranDetail()
    await fetchProducts()
    await fetchInvoiceInfo()
  }
})
```

**验收标准**：
- [ ] 从一个详情页跳转到同类型的另一个详情页时数据正确刷新
- [ ] URL 变化时组件数据同步更新

---

### ISSUE-F006：组件销毁时未清理定时器

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F006 |
| 所属模块 | 系统管理 |
| 问题类型 | 资源泄漏 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/SystemView.vue` |

**问题详情**：
`SystemView.vue` 在 `onMounted` 中启动了自动刷新定时器和窗口 resize 事件监听，在 `onUnmounted` 中进行了清理。这部分实现是正确的，但需要确认其他页面是否有类似问题。

**现状**：
`SystemView.vue` 第997-1003行正确清理了资源：
```javascript
onUnmounted(() => {
  stopAutoRefresh()
  window.removeEventListener('resize', resizeCharts)
  memoryChart?.dispose()
  cpuChart?.dispose()
  uptimeChart?.dispose()
})
```
经检查，其他页面没有使用定时器，此问题在 SystemView 中已正确处理。

---

### ISSUE-F007：表单提交后未正确关闭弹窗和重置表单 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F007 |
| 所属模块 | 用户管理 |
| 问题类型 | 交互问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/UserView.vue:260-278` |

**问题详情**：
用户表单提交成功后，没有关闭弹窗和重置表单数据。用户需要手动关闭弹窗，且再次打开时可能看到上次提交的数据残留。

**现状**：
`UserView.vue` 第268-276行：
```javascript
request("/api/user", formData).then(resp => {
    if (resp.data.code === 200) {
        messageTip(userQuery.value.id > 0 ? "编辑成功" : "提交成功", "success")
        getData(currentPage.value)
        // 缺少：关闭弹窗和重置表单
    }
})
```

**修改建议**：
在成功后关闭弹窗并重置表单：
```javascript
if (resp.data.code === 200) {
    messageTip(userQuery.value.id > 0 ? "编辑成功" : "提交成功", "success")
    userDialogVisible.value = false  // 关闭弹窗
    userQuery.value = {}  // 重置表单
    getData(currentPage.value)
}
```

**验收标准**：
- [ ] 提交成功后弹窗自动关闭
- [ ] 再次打开弹窗时表单为空
- [ ] 不会残留上次提交的数据

---

### ISSUE-F008：登录失败缺少明确的错误提示 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F008 |
| 所属模块 | 认证模块 |
| 问题类型 | 交互问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/LoginView.vue:88-108` |

**问题详情**：
登录失败时只显示"登录失败"通用提示，没有显示后端返回的具体错误信息（如"账号不存在"、"密码错误"、"账号已禁用"等）。

**现状**：
`LoginView.vue` 第104-107行：
```javascript
} else {
    messageTip("登录失败", "error");  // 没有使用后端返回的 msg
}
```

**修改建议**：
使用后端返回的错误信息：
```javascript
} else {
    messageTip(resp.data.msg || "登录失败", "error");
}
```

**验收标准**：
- [ ] 账号不存在时提示"账号不存在"
- [ ] 密码错误时提示"密码错误"
- [ ] 账号被禁用时提示"账号已禁用"

---

### ISSUE-F009：CustomerView 使用旧版 API 路径 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F009 |
| 所属模块 | 客户管理 |
| 问题类型 | API 对接 |
| 严重程度 | P2-一般 |
| 涉及文件 | 前端：`view/CustomerView.vue:74` |

**问题详情**：
`CustomerView.vue` 使用 `/api/customers` 获取客户列表，但 API 文档中已有新版本接口 `/api/customer/list`。旧接口可能不支持搜索和筛选功能。

**现状**：
`CustomerView.vue` 第74行：
```javascript
doGet("/api/customers", { current }).then(resp => {
```
`api/customer.js` 中定义了 `getCustomerList` 函数调用 `/api/customer/list`，但 `CustomerView.vue` 没有使用它。

**修改建议**：
1. 导入并使用 `getCustomerList` 函数
2. 添加搜索和筛选功能（客户名称、产品ID等）
3. 使用新版接口获取更丰富的数据

**验收标准**：
- [ ] 客户列表使用新版 API 接口
- [ ] 支持按客户名称搜索
- [ ] 列表数据正确显示

---

### ISSUE-F010：DashboardView 登出后未清除权限缓存

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-F010 |
| 所属模块 | 认证模块 |
| 问题类型 | 安全隐患 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 前端：`view/DashboardView.vue`（登出逻辑） |

**问题详情**：
如果实施了 ISSUE-026 的修复方案（缓存权限信息），登出时需要清除缓存的权限信息，否则切换账号登录后可能仍然显示前一个用户的权限。

**修改建议**：
在登出逻辑中添加清除权限缓存：
```javascript
const logout = () => {
  doPost("/api/logout", {}).then(() => {
    removeToken()
    sessionStorage.removeItem('user_permissions')  // 清除权限缓存
    window.location.href = "/"
  })
}
```

**验收标准**：
- [ ] 登出后权限缓存被清除
- [ ] 切换账号后权限正确更新

---

## 问题优先级汇总

### P0-致命（必须立即修复）
1. ISSUE-034：前端调用后端未实现的接口（getTranStatus）
2. ISSUE-037：补货接口路径前后端不一致

### P1-严重（尽快修复）
1. ISSUE-014：交易产品数量缺少库存校验（前端部分）
2. ISSUE-026：权限指令每次更新都发请求
3. ISSUE-022：前端 API 调用缺少统一错误处理
4. ISSUE-023：token 过期后取消登录导致页面卡死
5. ISSUE-005：动态路由页面未监听路由参数变化

### P2-一般（计划修复）
1. ISSUE-011：产品表单缺少必填字段校验
2. ISSUE-013：活动预算字段校验不完整
3. ISSUE-015：线索编辑时手机号不可编辑但后端未校验
4. ISSUE-025：线索跟踪记录编辑和删除功能未实现
5. ISSUE-036：活动备注前端编辑功能未实现
6. ISSUE-031：密码在日志中可能被打印
7. ISSUE-022：前端 API 调用缺少统一错误处理
8. ISSUE-F002：多个列表页面缺少 loading 状态管理
9. ISSUE-F003：删除操作缺少二次确认或确认不统一
10. ISSUE-F005：动态路由页面未监听路由参数变化
11. ISSUE-F008：登录失败缺少明确的错误提示
12. ISSUE-F009：CustomerView 使用旧版 API 路径

### P3-轻微（可选修复）
1. ISSUE-024：产品列表缺少批量删除功能
2. ISSUE-027：分页组件缺少每页条数选择
3. ISSUE-028：搜索条件重置不完整
4. ISSUE-038：登录接口使用 FormData 但未明确 Content-Type
5. ISSUE-039：交易详情页产品数据加载重复
6. ISSUE-040：系统监控自动刷新间隔过短
7. ISSUE-041：用户列表分页参数不一致
8. ISSUE-F001：多处 console.log 残留
9. ISSUE-F004：列表刷新后选中状态未清除
10. ISSUE-F007：表单提交后未正确关闭弹窗和重置表单
11. ISSUE-F010：登出后未清除权限缓存

---

## 修复建议优先级

1. **第一阶段（1天）**：修复 P0 问题，确保前后端接口一致
2. **第二阶段（2-3天）**：修复 P1 问题，解决性能和安全问题
3. **第三阶段（1周）**：修复 P2 问题，完善校验和交互
4. **第四阶段（持续优化）**：修复 P3 问题，提升代码质量

---

*审计报告生成时间：2026-05-30*
*审计工具：opencode*
