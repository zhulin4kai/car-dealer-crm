# dealer-web 前端迁移计划：Element Plus → TailwindCSS 4 + shadcn-vue

## 一、项目现状总结

### 当前技术栈

| 类别 | 当前 | 目标 |
|---|---|---|
| CSS 方案 | 原生 CSS（scoped） + Element Plus 内置样式 | TailwindCSS 4 |
| UI 组件库 | Element Plus 2.4（全局完整注册） | shadcn-vue |
| 图标库 | @element-plus/icons-vue | lucide-vue-next |
| 表单验证 | Element Plus 内置 rules（模板式） | VeeValidate + Zod |
| 表格 | el-table（声明式模板） | @tanstack/vue-table + shadcn Table |

### 涉及文件清单（共 23 个 Vue 文件 + 2 个 CSS 文件）

**全局配置层（4 个文件）**：
- `src/app/main.ts` — 入口，Element Plus 全局注册
- `src/app/plugins/element-plus.ts` — Element Plus 插件（需删除）
- `src/style.css` — Vite 脚手架遗留（未被引用，需删除）
- `src/assets/global.css` — 全局背景图（保留并迁移为 Tailwind）

**布局层（1 个文件）**：
- `src/layouts/DashboardLayout.vue` — 后台管理布局（侧边栏 + 顶栏 + 底栏）

**页面层（17 个文件）**：
- `src/pages/index.vue` — 登录页
- `src/pages/dashboard/index.vue` — 仪表盘首页
- `src/pages/dashboard/user.vue` — 用户管理
- `src/pages/dashboard/customer.vue` — 客户管理
- `src/pages/dashboard/system.vue` — 系统管理
- `src/pages/dashboard/activity/index.vue` — 活动列表
- `src/pages/dashboard/activity/[id].vue` — 活动详情
- `src/pages/dashboard/clue/index.vue` — 线索列表
- `src/pages/dashboard/clue/detail/[id].vue` — 线索详情
- `src/pages/dashboard/product/index.vue` — 产品管理
- `src/pages/dashboard/product/category.vue` — 产品分类
- `src/pages/dashboard/product/promotion.vue` — 促销管理
- `src/pages/dashboard/product/stock.vue` — 库存预警
- `src/pages/dashboard/tran/index.vue` — 交易列表
- `src/pages/dashboard/tran/[id].vue` — 交易详情
- `src/pages/dashboard/tran/approve/[id].vue` — 交易审批
- `src/pages/dashboard/tran/invoice/[id].vue` — 交易发票
- `src/pages/dashboard/dict/type.vue` — 字典类型
- `src/pages/dashboard/dict/value.vue` — 字典值

**共享组件层（1 个文件）**：
- `src/shared/ui/DataTablePagination.vue` — 分页组件

**模块组件层（1 个文件）**：
- `src/modules/activity/components/ActivityFormDialog.vue` — 活动表单弹窗

### Element Plus 组件使用统计（按频率排序）

| 组件 | 使用文件数 | 迁移难度 |
|---|---|---|
| el-button | 20 | 低 |
| el-table / el-table-column | 18 | **极高** |
| el-form / el-form-item | 18 | **高** |
| el-card | 18 | 低 |
| el-input | 16 | 低 |
| el-pagination | 15 | 中 |
| el-dialog | 14 | 中 |
| el-select / el-option | 13 | 中 |
| el-tag | 10 | 低 |
| el-input-number | 7 | 中 |
| el-date-picker | 7 | **高** |
| el-descriptions | 5 | 中（需自定义） |
| el-row / el-col | 5 | 低（改 Tailwind） |
| el-icon | 4 | 低 |
| el-menu / el-sub-menu / el-menu-item | 1 | **高** |
| el-dropdown | 1 | 中 |
| el-container / el-aside / el-header / el-main / el-footer | 3 | 低（改 Tailwind） |
| el-radio-group / el-radio | 2 | 低 |
| el-switch | 1 | 低 |
| el-alert | 1 | 低 |
| el-divider | 1 | 低 |
| el-progress | 1 | 低 |
| el-badge | 1 | 低（需自定义） |
| el-statistic | 1 | 低（需自定义） |
| el-checkbox | 1 | 低 |
| el-upload | 1 | 中（需自定义） |

### CSS 现状分析

**全局样式**：`src/assets/global.css` 仅设置了 body 背景图，`src/style.css` 是 Vite 遗留文件未被引用。

**组件样式特征**：所有 20 个含样式的 Vue 组件均使用了 `scoped`。大量使用 `:deep()` 覆盖 Element Plus 内部样式（14 个文件），内联样式泛滥（`style="width: 100%"` 出现 30+ 次），硬编码颜色值（无 CSS 变量管理），仅 4 个文件有响应式媒体查询。

**高频颜色**：`#409eff`（Element 蓝）、`#303133`（深色文字）、`#606266`（次要文字）、`#f5f7fa`（浅灰背景）、`#871d1f` / `#4b1011`（品牌深红，仅布局组件）。

**高频间距模式**：`padding: 20px`（页面容器）、`margin-bottom: 20px`（卡片间距）、`margin-top: 12px`（表格/分页间距）、`gap: 10px`（flex 子元素间距）。

---

## 二、迁移约束与规则

### 硬性约束

1. **TS 修改边界（核心红线）**：

   **允许修改的 TS（仅限与 UI 组件逻辑强绑定的代码）**：
   - 表单验证规则对象（`rules`，使用 EP 的 `FormRules` 类型 / `trigger: 'blur'` 格式）→ 改为 Zod schema
   - 表单 ref 的类型和调用（`formRef: ref<FormInstance>()`、`.validate()`、`.resetFields()`、`.clearValidate()`）→ 改为 shadcn-vue / vee-validate 对应方案
   - 自定义验证器函数签名（EP 三参数 `(rule, value, callback)` 格式）→ 改为 Zod `.refine()` / `.superRefine()`
   - 动态验证规则 computed（操作 EP 格式规则对象的）→ 改为 Zod 条件 schema
   - `ElMessage.xxx()` / `ElMessageBox.confirm()` 的直接调用 → 改为 toast / AlertDialog
   - Element Plus 类型导入（`FormInstance`、`FormRules`）→ 删除
   - Element Plus 图标组件导入（`EditPen`、`Refresh` 等）→ 改为 lucide-vue-next
   - EP Upload 专属 API（`.clearFiles()`、`.submit()`）→ 改为 HTML5 File API
   - `getTagType()` 类函数的返回值（给 `<el-tag type=...>` 用的 `'success'`/`'warning'` 字符串）→ 改为 shadcn Badge variant 映射

   **严禁修改的 TS（业务逻辑，碰都不碰）**：
   - 所有 API 调用（`doGet`/`doPost`/`doPut`/`doDelete`、各模块 API 函数）
   - 数据处理/转换逻辑（FormData 构建、日期格式化、金额计算、促销折扣计算）
   - 状态管理（Pinia store 操作：`authStore.login()`、`permissionStore.loadPermissions()` 等）
   - 路由导航（`router.push()`、`router.replace()`、`goBack()`）
   - 权限判断（`v-hasPermission` 指令逻辑、`permissionStore.hasPermission()`）
   - 所有响应式数据定义（`reactive`/`ref` 声明的业务数据模型）
   - ECharts 图表初始化和更新逻辑
   - Token 管理、Session 恢复、记住密码逻辑
   - 生命周期中的业务调用（`onMounted` 中的数据加载）

2. **不碰 dealer-server**：后端代码完全不涉及。
3. **保持 shadcn 原生样式**：不改变 shadcn-vue 组件的默认颜色、悬浮效果、圆角等设计语言。不用兼容层妥协，用 shadcn-vue 的正确方式来做。
4. **保持功能等价**：每个页面的功能行为必须与迁移前完全一致。
5. **保持 v-hasPermission 自定义指令**：权限指令的注册方式和使用方式不变，仅变更指令绑定的目标元素。

### 迁移原则

1. **逐文件迁移**：每次只迁移一个文件，确保可验证。
2. **先基础设施后业务**：先搭建 Tailwind + shadcn 基础设施，再逐页面迁移。
3. **先简单后复杂**：先从简单组件（Button、Input）开始，最后处理 Table、Form、Menu 等复杂组件。
4. **可回退**：每迁移一个阶段提交一次 Git commit，随时可回退。

---

## 三、基础设施搭建（Phase 0）

### 3.1 安装依赖

```bash
cd dealer-web

# 安装 TailwindCSS 4
npm install tailwindcss @tailwindcss/vite

# 安装 shadcn-vue 核心依赖
npm install reka-ui class-variance-authority clsx tailwind-merge
npm install -D tailwindcss-animate

# 安装图标库
npm install lucide-vue-next

# 安装表单验证
npm install vee-validate @vee-validate/zod zod

# 安装表格
npm install @tanstack/vue-table

# 安装日期处理（用于 DatePicker）
npm install @internationalized/date

# 卸载 Element Plus 相关（最终阶段执行）
# npm uninstall element-plus @element-plus/icons-vue
```

### 3.2 配置 TailwindCSS 4（Vite 插件方式）

修改 `vite.config.ts`：

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import tailwindcss from '@tailwindcss/vite'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    tailwindcss(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 8081,
    open: true,
  },
})
```

### 3.3 配置全局 CSS

创建 `src/assets/index.css`（Tailwind 入口文件）：

```css
@import "tailwindcss";

/* 从 global.css 迁移的背景 */
body {
  margin: 0;
  padding: 0;
  background-image: url('./background.png');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  min-height: 100vh;
}
```

### 3.4 配置 shadcn-vue 工具函数

创建 `src/shared/lib/utils.ts`：

```typescript
import { type ClassValue, clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}
```

### 3.5 安装 shadcn-vue 组件

需要安装的 shadcn-vue 组件列表（手动添加到 `src/components/ui/` 目录）：

```
button, input, select, dialog, card, badge, switch, checkbox,
radio-group, alert, separator, progress, textarea, label,
dropdown-menu, table, form, tooltip, popover, calendar,
number-field, sidebar, navigation-menu, sheet, avatar,
scroll-area, skeleton, tabs
```

### 3.6 修改入口文件

修改 `src/app/main.ts`：

```typescript
import { createApp } from 'vue'
import App from '@/App.vue'
import router from '@/router'
import { createPinia } from 'pinia'
import { installPermissionDirective } from '@/app/directives/has-permission'

// 替换: import '@/assets/global.css'
import '@/assets/index.css'

// 删除: import { installElementPlus } from './plugins/element-plus'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
// 删除: installElementPlus(app)
installPermissionDirective(app)
app.use(router)
app.mount('#app')
```

删除文件 `src/app/plugins/element-plus.ts`。
删除文件 `src/style.css`（遗留未引用）。

---

## 四、组件映射方案（逐组件详细说明）

### 4.1 el-button → shadcn Button

**使用文件数**：20 个

**映射规则**：

| Element Plus | shadcn-vue | 说明 |
|---|---|---|
| `<el-button type="primary">` | `<Button variant="default">` | shadcn default = primary |
| `<el-button type="danger">` | `<Button variant="destructive">` | |
| `<el-button type="info">` | `<Button variant="secondary">` | |
| `<el-button type="warning">` | `<Button variant="outline">` | 无直接对应，用 outline |
| `<el-button plain>` | `<Button variant="outline">` | |
| `<el-button link>` | `<Button variant="link">` | |
| `<el-button size="small">` | `<Button size="sm">` | |
| `<el-button :loading="true">` | `<Button :disabled="true"><Loader2 class="animate-spin" /></Button>` | 需手动组合 |
| `<el-button :disabled>` | `<Button :disabled>` | 直接对应 |
| `@click` | `@click` | 一致 |

**注意事项**：shadcn Button 没有内置 loading 状态，需要在 Button 内手动添加 Loader2 图标 + `animate-spin`。可封装为通用 LoadingButton 组件。

### 4.2 el-card → shadcn Card

**使用文件数**：18 个

**映射规则**：

| Element Plus | shadcn-vue |
|---|---|
| `<el-card>` | `<Card>` |
| `<template #header>` | `<CardHeader><CardTitle>...</CardTitle></CardHeader>` |
| 默认内容插槽 | `<CardContent>...</CardContent>` |
| `shadow="hover"` | 添加 `class="transition-shadow hover:shadow-lg"` |

### 4.3 el-input / el-input (textarea) → shadcn Input / Textarea

**使用文件数**：16 个

**映射规则**：

| Element Plus | shadcn-vue |
|---|---|
| `<el-input v-model="x" placeholder="...">` | `<Input v-model="x" placeholder="..." />` |
| `<el-input type="password">` | `<Input type="password" />` |
| `<el-input type="textarea" :rows="4">` | `<Textarea :rows="4" />`（Textarea 是独立组件） |
| `clearable` | 需手动添加清除按钮（或使用 InputGroup 包装） |
| `:disabled` | `:disabled` |
| `@keyup.enter` | `@keyup.enter` |
| `maxlength` + `show-word-limit` | 需手动实现字数统计 |

### 4.4 el-select / el-option → shadcn Select

**使用文件数**：13 个

**映射规则**：

```vue
<!-- Element Plus -->
<el-select v-model="value" placeholder="请选择" clearable filterable>
  <el-option :key="opt.value" :label="opt.label" :value="opt.value" />
</el-select>

<!-- shadcn-vue -->
<Select v-model="value">
  <SelectTrigger>
    <SelectValue placeholder="请选择" />
  </SelectTrigger>
  <SelectContent>
    <SelectItem v-for="opt in options" :key="opt.value" :value="opt.value">
      {{ opt.label }}
    </SelectItem>
  </SelectContent>
</Select>
```

**注意事项**：
- `clearable` → 需手动添加清除功能
- `filterable`（可搜索） → 改用 `Combobox` 组件
- `disabled` → 在 `<Select>` 上直接设置

### 4.5 el-form / el-form-item → shadcn Form + VeeValidate + Zod

**使用文件数**：18 个（涉及 14 个文件有验证规则）

**这是第二大工作量迁移项。不使用兼容层，直接用 shadcn-vue 的正统方式。**

**TS 修改边界**（这是必须改 TS 的核心场景）：

| 要改的 TS | 原写法 | 新写法 | 原因 |
|---|---|---|---|
| 类型导入 | `import type { FormInstance, FormRules } from 'element-plus'` | 删除 | EP 专属类型 |
| 表单 ref | `const formRef = ref<FormInstance>()` | `const { handleSubmit, errors, resetForm } = useForm({ validationSchema })` | EP 专属实例类型 |
| 验证规则对象 | `const rules: FormRules = { name: [{ required: true, message: '...', trigger: 'blur' }] }` | `const schema = toTypedSchema(z.object({ name: z.string().min(1, '...') }))` | EP 格式规则无法在 Zod 中使用 |
| 自定义验证器 | `const checkPhone = (rule, value, callback) => { ... callback(new Error('...')) }` | `z.string().refine(val => /^1\d{10}$/.test(val), { message: '...' })` | EP 三参数回调格式 → Zod refine |
| 表单提交 | `formRef.value.validate((valid) => { if (valid) { ... } })` | `const onSubmit = handleSubmit((values) => { ... })` | EP 回调式验证 → vee-validate 的 handleSubmit |
| 表单重置 | `formRef.value.resetFields()` | `resetForm()` | EP 专属 API |
| 清除验证 | `formRef.value.clearValidate()` | `resetForm({ values: initialValues })` 或 `setErrors({})` | EP 专属 API |
| 动态规则 | `const getDynamicRules = computed(() => { ... })` | `z.discriminatedUnion('type', [...])` 或 `.superRefine()` 条件验证 | EP 格式的动态规则 → Zod 条件 schema |

**页面中的迁移示例**：

```vue
<!-- ============ Element Plus 原写法 ============ -->
<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'  // ← 要改（EP 类型）

const formRef = ref<FormInstance>()                           // ← 要改（EP ref）

const rules: FormRules = {                                    // ← 要改（EP 规则格式）
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  phone: [{ validator: checkPhone, trigger: 'blur' }],
}

const checkPhone = (rule: any, value: string, callback: any) => {  // ← 要改（EP 验证器格式）
  if (!/^1\d{10}$/.test(value)) callback(new Error('手机号格式错误'))
  else callback()
}

async function submit() {
  const valid = await formRef.value?.validate()               // ← 要改（EP validate API）
  if (!valid) return
  await apiCall(formData)                                     // ← 严禁修改（业务逻辑）
}

function reset() {
  formRef.value?.resetFields()                                // ← 要改（EP resetFields）
}
</script>

<template>
  <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
    <el-form-item label="名称" prop="name">
      <el-input v-model="formData.name" />
    </el-form-item>
  </el-form>
</template>

<!-- ============ shadcn-vue 新写法 ============ -->
<script setup lang="ts">
import { useForm } from 'vee-validate'
import { toTypedSchema } from '@vee-validate/zod'
import * as z from 'zod'

const formSchema = toTypedSchema(z.object({                   // ← Zod schema（替代 EP rules）
  name: z.string().min(1, '请输入名称'),
  phone: z.string().refine(val => /^1\d{10}$/.test(val), { message: '手机号格式错误' }),
}))

const { handleSubmit, errors, resetForm, values } = useForm({ // ← vee-validate（替代 EP formRef）
  validationSchema: formSchema,
  initialValues: { name: '', phone: '' },
})

const onSubmit = handleSubmit(async (formData) => {           // ← vee-validate 提交
  await apiCall(formData)                                     // ← 严禁修改（业务逻辑，和上面完全一样）
})

function reset() {
  resetForm()                                                 // ← vee-validate 重置
}
</script>

<template>
  <form class="space-y-4" @submit.prevent="onSubmit">
    <FormField label="名称" name="name">
      <Input v-model="values.name" />
      <p v-if="errors.name" class="text-sm text-destructive">{{ errors.name }}</p>
    </FormField>
  </form>
</template>
```

**严禁修改的 TS 清单**（以 user.vue 为例）：
- `const userList = ref([])` — 业务数据
- `async function getData(current)` — API 调用
- `function handleSelectionChange(selection)` — 表格选择
- `await doPost('/api/user', formData)` — 业务提交
- `authStore.login(user)` — 认证调用
- `router.push('/dashboard')` — 路由导航

**特殊场景处理**：

1. **tran/invoice/[id].vue 的动态验证规则**：`getDynamicRules = computed(...)` 根据发票类型动态追加必填规则。改用 Zod 的 `z.discriminatedUnion('type', [vatSpecialSchema, normalSchema])` 或 `.superRefine()` 实现条件验证。

2. **clue/index.vue 的自定义验证器 `checkPhone`**：EP 三参数回调格式改为 Zod 的 `.refine()` / `.transform()`。

3. **多表单页面**（如 clue/detail/[id].vue 有两个表单 ref）：使用两个独立的 `useForm()` 实例，各自有自己的 `handleSubmit` 和 `resetForm`。

### 4.6 el-table / el-table-column → @tanstack/vue-table + shadcn Table

**使用文件数**：18 个

**这是迁移工作量最大的组件。**

Element Plus 的 Table 是声明式模板：
```vue
<el-table :data="tableData" stripe border @selection-change="handleSelectionChange">
  <el-table-column type="selection" width="55" />
  <el-table-column type="index" label="#" width="60" />
  <el-table-column prop="name" label="名称" show-overflow-tooltip />
  <el-table-column label="操作" fixed="right">
    <template #default="scope">
      <el-button @click="handleEdit(scope.row)">编辑</el-button>
    </template>
  </el-table-column>
</el-table>
```

shadcn-vue + TanStack Table 的等价写法：
```vue
<Table>
  <TableHeader>
    <TableRow v-for="headerGroup in table.getHeaderGroups()" :key="headerGroup.id">
      <TableHead v-for="header in headerGroup.headers" :key="header.id">
        <FlexRender :render="header.column.columnDef.header" :props="header.getContext()" />
      </TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    <TableRow v-for="row in table.getRowModel().rows" :key="row.id">
      <TableCell v-for="cell in row.getVisibleCells()" :key="cell.id">
        <FlexRender :render="cell.column.columnDef.cell" :props="cell.getContext()" />
      </TableCell>
    </TableRow>
  </TableBody>
</Table>
```

**关键差异**：
- 声明式 → 编程式：需要在 `<script>` 中定义 `ColumnDef[]` 数组
- 选择列：需要手动实现 checkbox 选择逻辑
- 行序号：需要自定义 index 列
- `show-overflow-tooltip`：需要用 shadcn Tooltip 包装
- `stripe`：给偶数行添加 `class="bg-muted/50"`
- `border`：给 Table 添加 `class="border"`
- `fixed="right"`：需要用 sticky 定位实现
- `v-loading`：需要用 shadcn Skeleton 或自定义 loading overlay

**迁移策略**：
封装一个通用 `DataTable` 组件，接收 `columns`、`data`、`selectable`、`loading` 等 props，内部使用 TanStack Table + shadcn Table 渲染。这样各页面只需定义列配置和传入数据，减少重复代码。

### 4.7 el-dialog → shadcn Dialog

**使用文件数**：14 个

**映射规则**：

```vue
<!-- Element Plus -->
<el-dialog v-model="dialogVisible" title="编辑" width="600px" center draggable :destroy-on-close>
  <div>内容</div>
  <template #footer>
    <el-button @click="dialogVisible = false">取消</el-button>
    <el-button type="primary" @click="handleSubmit">确定</el-button>
  </template>
</el-dialog>

<!-- shadcn-vue -->
<Dialog v-model:open="dialogVisible">
  <DialogContent class="max-w-[600px]">
    <DialogHeader>
      <DialogTitle>编辑</DialogTitle>
    </DialogHeader>
    <div>内容</div>
    <DialogFooter>
      <Button variant="outline" @click="dialogVisible = false">取消</Button>
      <Button @click="handleSubmit">确定</Button>
    </DialogFooter>
  </DialogContent>
</Dialog>
```

**注意事项**：
- `v-model` → `v-model:open`
- `width` → 用 Tailwind `class="max-w-[600px]"` 或 `class="sm:max-w-[600px]"`
- `center` → shadcn Dialog 默认居中
- `draggable` → shadcn 不支持拖拽，可忽略或用第三方库
- `:destroy-on-close` → shadcn Dialog 默认 unmount 时销毁
- `:append-to-body` → shadcn 使用 Portal/Teleport，自动处理

### 4.8 el-pagination → shadcn Pagination（需封装）

**使用文件数**：15 个

shadcn 的 Pagination 组件功能有限，缺少 Element Plus 的 `layout` 配置、`background` 模式等。

**迁移策略**：
增强现有的 `DataTablePagination.vue` 共享组件，内部使用 shadcn Pagination 的基础组件（PaginationRoot、PaginationList、PaginationListItem、PaginationFirst、PaginationPrev、PaginationNext、PaginationLast），加上自定义的页码信息展示。

```vue
<!-- 封装后的 DataTablePagination -->
<PaginationRoot v-model:page="currentPage" :total="total" :page-size="pageSize"
  :items-per-page="pageSize" :sibling-count="1">
  <PaginationList v-slot="{ items }" class="flex items-center gap-1">
    <PaginationFirst />
    <PaginationPrev />
    <template v-for="(item, index) in items">
      <PaginationListItem v-if="item.type === 'page'" :key="index" :value="item.value" as-child>
        <Button class="w-9 h-9 p-0" :variant="item.value === page ? 'default' : 'outline'">
          {{ item.value }}
        </Button>
      </PaginationListItem>
      <PaginationEllipsis v-else :key="item.type" :index="index" />
    </template>
    <PaginationNext />
    <PaginationLast />
  </PaginationList>
</PaginationRoot>
```

### 4.9 el-tag → shadcn Badge

**使用文件数**：10 个

| Element Plus | shadcn-vue |
|---|---|
| `<el-tag type="success">` | `<Badge variant="default" class="bg-green-500">` 或使用自定义 variant |
| `<el-tag type="warning">` | `<Badge variant="secondary">` |
| `<el-tag type="danger">` | `<Badge variant="destructive">` |
| `<el-tag type="info">` | `<Badge variant="outline">` |
| `size="small"` | 添加 `class="text-xs"` |

### 4.10 el-container / el-aside / el-header / el-main / el-footer → Tailwind 布局

**使用文件数**：3 个（DashboardLayout.vue、pages/index.vue、DashboardLayout 的子组件）

```vue
<!-- Element Plus -->
<el-container>
  <el-aside :width="sidebarWidth">侧边栏</el-aside>
  <el-container>
    <el-header>顶栏</el-header>
    <el-main><router-view /></el-main>
    <el-footer>底栏</el-footer>
  </el-container>
</el-container>

<!-- TailwindCSS -->
<div class="flex h-screen">
  <aside class="w-[200px]" :class="{ 'w-16': collapsed }">侧边栏</aside>
  <div class="flex flex-col flex-1">
    <header class="h-[35px]">顶栏</header>
    <main class="flex-1 overflow-auto"><router-view /></main>
    <footer>底栏</footer>
  </div>
</div>
```

### 4.11 el-menu / el-sub-menu / el-menu-item → 自定义侧边栏

**使用文件数**：1 个（DashboardLayout.vue）

**这是布局迁移的核心难点。**

Element Plus 的 Menu 组件内置了路由集成（`:router` prop）、折叠动画（`:collapse`）、唯一展开（`:unique-opened`）等能力。

**迁移策略**：使用 shadcn-vue 的 Collapsible 组件手动构建侧边栏菜单：

```vue
<!-- 简化示意 -->
<nav class="flex flex-col h-full">
  <template v-for="menu in menuList" :key="menu.path">
    <!-- 有子菜单 -->
    <Collapsible v-if="menu.children?.length" v-model:open="openMenus[menu.path]">
      <CollapsibleTrigger class="flex items-center w-full px-4 py-2">
        <component :is="getIcon(menu.icon)" class="w-4 h-4 mr-2" />
        {{ menu.name }}
      </CollapsibleTrigger>
      <CollapsibleContent>
        <router-link v-for="child in menu.children" :key="child.path"
          :to="child.path" class="pl-8 py-2 block">
          {{ child.name }}
        </router-link>
      </CollapsibleContent>
    </Collapsible>
    <!-- 无子菜单 -->
    <router-link v-else :to="menu.path" class="flex items-center px-4 py-2">
      <component :is="getIcon(menu.icon)" class="w-4 h-4 mr-2" />
      {{ menu.name }}
    </router-link>
  </template>
</nav>
```

### 4.12 el-dropdown → shadcn DropdownMenu

**使用文件数**：1 个（DashboardLayout.vue）

```vue
<!-- Element Plus -->
<el-dropdown :hide-on-click="false">
  <span>用户名 <arrow-down /></span>
  <template #dropdown>
    <el-dropdown-menu>
      <el-dropdown-item divided @click="handleLogout">退出登录</el-dropdown-item>
    </el-dropdown-menu>
  </template>
</el-dropdown>

<!-- shadcn-vue -->
<DropdownMenu>
  <DropdownMenuTrigger>
    <span>用户名 <ChevronDown class="w-4 h-4" /></span>
  </DropdownMenuTrigger>
  <DropdownMenuContent>
    <DropdownMenuSeparator />
    <DropdownMenuItem @click="handleLogout">退出登录</DropdownMenuItem>
  </DropdownMenuContent>
</DropdownMenu>
```

### 4.13 el-descriptions / el-descriptions-item → 自定义 Descriptions

**使用文件数**：5 个（详情页系列）

shadcn 没有直接对应组件，需要自定义封装：

```vue
<!-- 自定义 DescriptionList 组件 -->
<template>
  <dl class="divide-y">
    <div v-for="item in items" :key="item.label"
      class="grid grid-cols-[120px_1fr] py-3"
      :class="{ 'col-span-2': item.span === 2 }">
      <dt class="text-sm font-medium text-muted-foreground">{{ item.label }}</dt>
      <dd class="text-sm"><slot :name="item.label" /></dd>
    </div>
  </dl>
</template>
```

或者直接使用 shadcn Table 来实现描述列表效果。

### 4.14 el-input-number → shadcn NumberField 或 Input type="number"

**使用文件数**：7 个

```vue
<!-- Element Plus -->
<el-input-number v-model="count" :min="0" :precision="2" :step="0.01" />

<!-- shadcn-vue 方案 A（简单，用 Input） -->
<Input type="number" v-model.number="count" :min="0" step="0.01" />

<!-- shadcn-vue 方案 B（完整，用 NumberField） -->
<NumberField v-model="count" :min="0" :step="0.01">
  <NumberFieldContent>
    <NumberFieldDecrement />
    <NumberFieldInput />
    <NumberFieldIncrement />
  </NumberFieldContent>
</NumberField>
```

推荐方案 B，功能更完整。

### 4.15 el-date-picker → shadcn Popover + Calendar

**使用文件数**：7 个

**这是第三大工作量迁移项。**

```vue
<!-- Element Plus -->
<el-date-picker v-model="date" type="datetime" placeholder="选择日期" value-format="YYYY-MM-DD HH:mm:ss" />
<el-date-picker v-model="range" type="datetimerange" start-placeholder="开始" end-placeholder="结束" />

<!-- shadcn-vue -->
<Popover>
  <PopoverTrigger as-child>
    <Button variant="outline" class="w-full justify-start text-left font-normal">
      <CalendarIcon class="mr-2 h-4 w-4" />
      {{ formattedDate || "选择日期" }}
    </Button>
  </PopoverTrigger>
  <PopoverContent class="w-auto p-0">
    <Calendar v-model="dateValue" />
    <!-- datetime 需要额外加时间选择器 -->
  </PopoverContent>
</Popover>
```

**注意事项**：
- `type="datetime"` → 需要 Calendar + 自定义 Time Select
- `type="datetimerange"` → 需要 RangeCalendar + 时间选择器
- `type="date"` → 直接使用 Calendar
- `value-format` → 使用 `@internationalized/date` 的 `DateValue` 类型，在提交时格式化
- 可考虑封装 `DateTimePicker` 和 `DateTimeRangePicker` 通用组件

### 4.16 el-row / el-col → Tailwind Grid/Flex

**使用文件数**：5 个

```vue
<!-- Element Plus (24 列制) -->
<el-row :gutter="24">
  <el-col :span="8">内容1</el-col>
  <el-col :span="12">内容2</el-col>
  <el-col :span="6">内容3</el-col>
</el-row>

<!-- Tailwind CSS (12 列制) -->
<div class="grid grid-cols-12 gap-6">
  <div class="col-span-4">内容1</div>   <!-- 8/24 = 4/12 -->
  <div class="col-span-6">内容2</div>   <!-- 12/24 = 6/12 -->
  <div class="col-span-3">内容3</div>   <!-- 6/24 = 3/12 -->
</div>
```

**24 列 → 12 列转换公式**：`col-span-N = span / 2`

### 4.17 其他组件映射

| Element Plus | shadcn-vue / 替代方案 |
|---|---|
| `el-switch` | `Switch` — 直接对应，但 active-value/inactive-value 不支持，需在 v-model 处适配 |
| `el-radio-group` / `el-radio` | `RadioGroup` + `RadioGroupItem` + `Label` |
| `el-checkbox` | `Checkbox` + `Label` |
| `el-alert` | `Alert` + `AlertTitle` + `AlertDescription` — 仅 default/destructive variant |
| `el-divider` | `Separator` |
| `el-progress` | `Progress` — 仅线性进度条 |
| `el-badge`（角标模式） | 自定义 `<span class="relative"><slot /><span class="absolute -top-1 -right-1 w-2 h-2 rounded-full bg-destructive" /></span>` |
| `el-statistic` | 自定义 `<div><div class="text-2xl font-bold">{{ value }}</div><div class="text-muted-foreground">{{ title }}</div></div>` |
| `el-upload` | 自定义 `<Input type="file" />` + 上传逻辑保持 |

### 4.18 图标映射

| @element-plus/icons-vue | lucide-vue-next |
|---|---|
| `EditPen` | `Pencil` |
| `RefreshRight` | `RotateCw` |
| `Refresh` | `RefreshCw` |
| `MessageBox` | `MessageSquare` |
| `Fold` | `PanelLeftClose` |
| `ArrowDown` | `ChevronDown` |
| 动态菜单图标 | 需要建立后端图标名称 → lucide 组件的映射表 |

**动态菜单图标处理**：当前 DashboardLayout 中使用 `<component :is="menu.icon">` 渲染后端返回的图标名（Element Plus 图标名）。需要创建一个映射函数，将后端的 Element Plus 图标名转换为 lucide-vue-next 的组件。

### 4.19 ElMessage / ElMessageBox → shadcn Toast / AlertDialog

**使用文件数**：10 个文件直接调用 + 15 个文件通过 `feedback.ts` 间接调用

**这是全局性的基础设施改造，改完后所有间接调用的文件无需修改 TS。**

**重写 `src/shared/utils/feedback.ts`**：

```typescript
// ===== 原实现（删除） =====
// import { ElMessage, ElMessageBox } from 'element-plus'
// export function messageTip(message: string, type: MessageType = 'info') {
//   ElMessage({ showClose: true, center: true, duration: 3000, message, type })
// }
// export function messageConfirm(message: string) {
//   return ElMessageBox.confirm(message, '系统提醒', { ... })
// }

// ===== 新实现 =====
import { toast } from '@/components/ui/toast/use-toast'

// messageTip：签名保持不变，内部换实现
export function messageTip(message: string, type: MessageType = 'info') {
  const variantMap: Record<string, string> = {
    success: 'default',
    error: 'destructive',
    warning: 'default',
    info: 'default',
  }
  toast({ title: message, variant: variantMap[type] as any })
}

// messageConfirm：签名保持不变，内部换实现
// 需要一个全局 AlertDialog 实例（在 App.vue 中渲染）
import { openConfirmDialog } from '@/shared/ui/ConfirmDialog'

export function messageConfirm(message: string): Promise<boolean> {
  return openConfirmDialog({ title: '系统提醒', description: message })
}
```

**直接调用 ElMessage/ElMessageBox 的 10 个文件的 TS 改动**：

这些文件直接 `import { ElMessage, ElMessageBox } from 'element-plus'`，需要改为 `import { messageTip, messageConfirm } from '@/shared/utils/feedback'`（或直接用 toast）。

| 文件 | 改动详情 |
|---|---|
| `activity/index.vue` | 删除 `import { ElMessage, ElMessageBox }`；`ElMessage.warning(...)` → `messageTip(..., 'warning')`；`ElMessageBox.confirm(...)` → `messageConfirm(...)`；`ElMessage.success/error/info(...)` → `messageTip(...)` |
| `activity/[id].vue` | 同上模式 |
| `clue/index.vue` | 同上模式（此文件同时有直接调用和间接调用） |
| `dict/type.vue` | 删除 `import { ElMessage, ElMessageBox }`；替换所有 `ElMessage.xxx` 调用 |
| `dict/value.vue` | 同上 |
| `system.vue` | 同上（ElMessage 调用最多的文件之一，约 15 处） |
| `tran/index.vue` | 同上 |
| `tran/[id].vue` | 同上 |
| `tran/approve/[id].vue` | 同上 |
| `tran/invoice/[id].vue` | 同上 |

**TS 修改边界**：这些改动只替换了消息提示的调用方式，不涉及任何业务逻辑。`ElMessage.success('删除成功')` 改为 `messageTip('删除成功', 'success')` 是纯 UI 层面的替换，`deleteActivity(id)` 这样的 API 调用严禁触碰。

**通过 `messageTip`/`messageConfirm` 间接使用的 15 个文件**：由于 `feedback.ts` 的函数签名保持不变（`messageTip(message, type)` 和 `messageConfirm(message)`），这些文件的 TS **无需任何修改**，只需改 template 中的组件标签。

### 4.20 el-switch 的 active-value/inactive-value 处理

**使用文件数**：1 个（system.vue）

Element Plus 的 Switch 支持 `active-value` / `inactive-value`（非布尔值映射），shadcn Switch 只接受布尔值。

**TS 修改**：将 `<el-switch v-model="row.status" :active-value="1" :inactive-value="0">` 改为使用计算属性做映射：

```typescript
// 允许修改：这是与 Switch 组件值映射强绑定的 TS
const statusChecked = computed({
  get: () => row.status === 1,
  set: (val: boolean) => { row.status = val ? 1 : 0 },
})
```

```vue
<Switch v-model="statusChecked" @change="handleStatusChange(row)" />
```

---

## 五、CSS 迁移方案

### 5.1 通用 CSS 模式转换表

| 原 CSS 写法 | Tailwind CSS 等价 |
|---|---|
| `padding: 20px` | `p-5` |
| `margin-bottom: 20px` | `mb-5` |
| `margin-top: 12px` | `mt-3` |
| `gap: 10px` | `gap-2.5` |
| `gap: 6px` | `gap-1.5` |
| `display: flex` | `flex` |
| `flex-direction: column` | `flex-col` |
| `align-items: center` | `items-center` |
| `justify-content: space-between` | `justify-between` |
| `justify-content: center` | `justify-center` |
| `justify-content: flex-end` | `justify-end` |
| `width: 100%` | `w-full` |
| `min-height: 100vh` | `min-h-screen` |
| `text-align: center` | `text-center` |
| `font-size: 16px` | `text-base` |
| `font-size: 18px` | `text-lg` |
| `font-size: 30px` | `text-3xl` |
| `font-weight: 600` | `font-semibold` |
| `font-weight: bold` | `font-bold` |
| `border-radius: 6px` | `rounded-md` |
| `border-radius: 50%` | `rounded-full` |
| `box-shadow: 0 2px 12px 0 rgba(0,0,0,0.2)` | `shadow-lg` |
| `transition: all 0.3s ease` | `transition-all duration-300` |
| `cursor: pointer` | `cursor-pointer` |
| `overflow: hidden` | `overflow-hidden` |
| `text-decoration: line-through` | `line-through` |
| `white-space: nowrap` | `whitespace-nowrap` |
| `float: right` | `float-right`（建议改 flex） |
| `display: grid; grid-template-columns: repeat(3, 1fr)` | `grid grid-cols-3` |

### 5.2 颜色迁移策略

项目使用的硬编码颜色在 shadcn-vue 的 CSS 变量体系中有对应物：

| 硬编码颜色 | 语义 | Tailwind / shadcn 等价 |
|---|---|---|
| `#409eff` | Element 主色蓝 | `text-primary` / `border-primary` |
| `#303133` | 深色文字 | `text-foreground` |
| `#606266` | 次要文字 | `text-muted-foreground` |
| `#909399` | 辅助文字 | `text-muted-foreground/70` |
| `#f5f7fa` | 浅灰背景 | `bg-muted` |
| `#f8f9fa` | 更浅灰背景 | `bg-muted/50` |
| `#e4e7ed` / `#ebeef5` | 边框色 | `border` |
| `#f56c6c` | 错误/警告红 | `text-destructive` |
| `#67c23a` | 成功绿 | `text-green-500` |
| `#e6a23c` | 警告橙 | `text-amber-500` |
| `#871d1f` / `#4b1011` | 品牌深红 | 需要自定义 CSS 变量或直接硬编码 |
| `#d6a5a5` | 品牌浅粉 | 需要自定义 CSS 变量或直接硬编码 |

**品牌色处理**：布局组件使用的深红/暗红色调（`#871d1f`、`#4b1011`、`#d6a5a5`）不属于 shadcn 默认主题。由于约束要求使用 shadcn 原生样式，这些品牌色有两种处理方式：

1. **保留硬编码**：在布局组件中直接用 Tailwind arbitrary values，如 `bg-[#4b1011]`
2. **扩展 CSS 变量**：在 Tailwind 入口 CSS 中定义 `--sidebar-bg: #4b1011` 等自定义变量

推荐方式 1，保持简洁。

### 5.3 :deep() 覆盖的消除策略

当前项目中大量使用 `:deep()` 来覆盖 Element Plus 组件内部样式。迁移到 shadcn-vue 后，大部分 `:deep()` 不再需要，因为 shadcn-vue 组件本身就是用 Tailwind 写的，可以直接通过 `class` prop 定制样式。

| 原 :deep() 覆盖 | 迁移后处理方式 |
|---|---|
| `:deep(.el-table) { width: 100% !important }` | `<Table class="w-full">` |
| `:deep(.el-table th) { background: #f8f9fa }` | `<TableHead class="bg-muted">` |
| `:deep(.el-table tr:hover > td) { background: #f0f9ff }` | `<TableRow class="hover:bg-muted/50">` |
| `:deep(.el-card__header) { background: #f8f9fa }` | `<CardHeader class="bg-muted">` |
| `:deep(.el-form-item) { margin-bottom: 18px }` | `<FormField class="mb-4">` |
| `:deep(.el-pagination) { justify-content: center }` | 直接在组件上 `class="justify-center"` |
| `:deep(.el-dialog) { border-radius: 12px }` | `<DialogContent class="rounded-xl">` |
| `:deep(.el-button) { border-radius: 6px }` | `<Button class="rounded-md">` |
| `:deep(.el-input__wrapper) { border-radius: 6px }` | `<Input class="rounded-md">` |
| `:deep(.el-descriptions) { margin-bottom: 20px }` | 直接在容器 div 上 `class="mb-5"` |

### 5.4 内联样式消除策略

当前项目中有 30+ 处内联 `style="..."`，迁移时全部替换为 Tailwind class：

| 内联样式 | Tailwind 等价 |
|---|---|
| `style="width: 100%"` | `class="w-full"` |
| `style="width: 200px"` | `class="w-[200px]"` 或 `class="w-52"` |
| `style="width: 150px"` | `class="w-[150px]"` |
| `style="margin-top: 12px; width: 100%;"` | `class="mt-3 w-full"` |
| `style="margin-top: 20px; width: 100%;"` | `class="mt-5 w-full"` |
| `style="max-width: 95%;"` | `class="max-w-[95%]"` |
| `style="text-transform: uppercase"` | `class="uppercase"` |
| `style="display: inline-flex; align-items: center"` | `class="inline-flex items-center"` |
| `style="border-right: solid 0px;"` | 直接删除 |
| `style="margin-left: 50px"` | `class="ml-[50px]"` |
| `style="width: 100%; min-width: 1400px;"` | `class="w-full min-w-[1400px]"` |

---

## 5.5 逐文件 TS 修改边界明细

下表明确标注每个文件中哪些 TS 可以改、哪些严禁碰。"TS 改动量"指与组件逻辑绑定需要修改的部分。

| 文件 | TS 改动量 | 可修改的 TS（组件逻辑绑定） | 严禁修改的 TS（业务逻辑） |
|---|---|---|---|
| `pages/index.vue` | 中 | 删除 `FormInstance`/`FormRules` 类型导入；`loginRules` 改为 Zod schema；`loginRefForm.validate()` 改为 `handleSubmit` | `authStore.login()`、`router.push()`、`restoreRememberedSession()` |
| `pages/dashboard/index.vue` | **零** | 无 EP 类型/ref/规则，TS 无需改动 | `loadSummary()`、ECharts 初始化/更新 |
| `pages/dashboard/customer.vue` | **零** | 仅间接使用 `messageTip`（改 feedback.ts 即可） | `getData()`、`exportExcel()`、表格选择 |
| `pages/dashboard/user.vue` | 中 | `userRules` 改为 Zod schema；`userRefForm.validate()` 改为 `handleSubmit`；间接 `messageTip`/`messageConfirm` | `getData()`、`userSubmit()` 中的 FormData + API 调用、`del()`、`batchDel()` |
| `pages/dashboard/system.vue` | **高** | 删除 `import { ElMessage, ElMessageBox }`；~15 处 `ElMessage.xxx()` 替换；`formRef.validate()` 改为 `handleSubmit`；`rules` 改为 Zod schema；Switch 值映射 | ECharts 全部逻辑、`getLocalSystemInfo()`、定时器管理、API 调用、`handleStatusChange()` 中的状态回滚 |
| `dashboard/activity/index.vue` | 中高 | 删除 EP 导入；`activityFormRules` + `activityRules` 改为 Zod schema；`activityFormRef.validate()`/`.clearValidate()` 替换；~12 处 `ElMessage`/`ElMessageBox` 替换 | `getData()`、`batchDel()`、`del()` 中的 API 调用、`submitActivityForm()` 中的 FormData + API |
| `dashboard/activity/[id].vue` | 中高 | 删除 EP 导入；`activityRemarkRules` 改为 Zod schema；`activityRemarkRefForm.validate()`/`.resetFields()` 替换；~10 处 `ElMessage`/`ElMessageBox` 替换 | `loadActivityDetail()`、`activityRemarkSubmit()` 中的 API、`del()` 中的 API |
| `dashboard/clue/index.vue` | **高** | 删除 EP 导入；`clueRules` 改为 Zod schema（含 `checkPhone` 自定义验证器→Zod refine）；`clueRefForm.validate()`/`.resetFields()` 替换；`uploadRef.clearFiles()`/`.submit()` 改为 File API；~8 处 `ElMessage`/`ElMessageBox` 替换 | `getData()`、`uploadFile()` 中的 API、`addClueSubmit()` 中的 FormData + API、所有字典加载 |
| `dashboard/clue/detail/[id].vue` | 中高 | `clueRemarkRules` + `convertCustomerRules` 改为 Zod schema；`clueRemarkRefForm.validate()`/`.resetFields()` + `convertCustomerRefForm.validate()` 替换 | `loadClueDetail()`、`clueRemarkSubmit()` 中的 API、`convertCustomerSubmit()` 中的 API（线索转客户核心） |
| `dashboard/product/index.vue` | 中 | `productRules` 改为 Zod schema；`productFormRef.validate()` 替换；间接 `messageTip`/`messageConfirm` | `loadProducts()`、`handleSubmit()` 中的 API、`handleDelete()` 中的 API |
| `dashboard/product/category.vue` | **零** | 仅间接使用 `messageConfirm`/`messageTip`（改 feedback.ts 即可） | `loadCategories()`、`handleSubmit()` 中的 API |
| `dashboard/product/promotion.vue` | **极低** | `getPromotionTypeTag()`/`getStatusTag()` 返回值从 EP tag type 改为 Badge variant 映射；间接 `messageConfirm`/`messageTip` | `loadPromotions()`、`formatDateTime()`、`handleSubmit()` 中的 API |
| `dashboard/product/stock.vue` | 低 | 删除 `import { MessageBox, Refresh } from '@element-plus/icons-vue'` → 改 lucide；间接 `messageTip` | `loadStockAlerts()`、`handleRestockSubmit()` 中的 API |
| `dashboard/tran/index.vue` | **高** | 删除 EP 导入；`rules` 改为 Zod schema；`formRef.validate()` 替换；~15 处 `ElMessage`/`ElMessageBox` 替换 | `fetchData()`、`submitForm()` 中的数据格式化 + API、`handleDelete()`/`handleBatchDelete()` 中的 API、动态产品行管理 |
| `dashboard/tran/[id].vue` | 中 | 删除 EP 导入；~8 处 `ElMessage`/`ElMessageBox` 替换 | 促销折扣计算、`handleSettle()` 中的 API、所有 computed 属性 |
| `dashboard/tran/approve/[id].vue` | 中 | 删除 EP 导入；`rules` 改为 Zod schema；`formRef.validate()` 替换；~5 处 `ElMessage` 替换 | `fetchTranDetail()`、`submitForm()` 中的 API |
| `dashboard/tran/invoice/[id].vue` | **高** | 删除 EP 导入；`rules` 改为 Zod schema；`getDynamicRules` computed → Zod discriminatedUnion；`formRef.validate()` 替换；~10 处 `ElMessage` 替换 | `fetchTranDetail()`、`submitForm()` 中的 API、`markAsIssued()` 中的 API |
| `dashboard/dict/type.vue` | 中 | 删除 EP 导入；`rules` 改为 Zod schema；`formRef.validate()` 替换；~6 处 `ElMessage` 替换 | `loadData()`、`handleSubmit()` 中的 API、`handleDelete()` 中的 API |
| `dashboard/dict/value.vue` | 中 | 删除 EP 导入；`rules` 改为 Zod schema；`formRef.validate()` 替换；~8 处 `ElMessage`/`ElMessageBox` 替换 | `loadData()`、`handleSubmit()` 中的 API |
| `layouts/DashboardLayout.vue` | **零** | 仅间接使用 `messageTip`（改 feedback.ts 即可） | `logout()` 中的 authStore/permissionStore 操作、菜单渲染数据 |
| `shared/ui/DataTablePagination.vue` | **零** | props/emits 是纯 Vue 标准，无需改动 | — |

### TS 修改难度分级

**零 TS 修改（仅改模板 + 样式）**：`dashboard/index.vue`、`customer.vue`、`product/category.vue`、`DashboardLayout.vue`、`DataTablePagination.vue`

**极低（改 1-2 个函数返回值映射）**：`product/promotion.vue`、`product/stock.vue`

**中等（替换规则 + 表单验证 + 消息调用）**：`pages/index.vue`、`user.vue`、`product/index.vue`、`tran/[id].vue`、`tran/approve/[id].vue`、`dict/type.vue`、`dict/value.vue`

**高（大量消息替换 + 复杂表单/自定义验证器/动态规则）**：`system.vue`、`activity/index.vue`、`activity/[id].vue`、`clue/index.vue`、`clue/detail/[id].vue`、`tran/index.vue`、`tran/invoice/[id].vue`

---

## 六、逐文件迁移计划

### Phase 1：基础设施（预计 2-3 小时）

| 步骤 | 文件操作 | 说明 |
|---|---|---|
| 1.1 | 安装所有 npm 依赖 | 见 3.1 节 |
| 1.2 | 修改 `vite.config.ts` | 添加 Tailwind 插件 |
| 1.3 | 创建 `src/assets/index.css` | Tailwind 入口 + 全局样式 |
| 1.4 | 创建 `src/shared/lib/utils.ts` | shadcn cn() 工具函数 |
| 1.5 | 安装所有 shadcn-vue 组件 | 复制到 `src/components/ui/` |
| 1.6 | 创建通用组件 | LoadingButton、DescriptionList、DataTable、DateTimePicker、DateTimeRangePicker、ConfirmDialog |
| 1.7 | 修改 `src/app/main.ts` | 移除 Element Plus 注册 |
| 1.8 | 删除 `src/app/plugins/element-plus.ts` | 不再需要 |
| 1.9 | 删除 `src/style.css` | 遗留文件 |
| 1.10 | 修改 `src/shared/utils/feedback.ts` | ElMessage → Toast, ElMessageBox → AlertDialog |

### Phase 2：布局层（预计 2-3 小时）

| 步骤 | 文件 | 改动要点 |
|---|---|---|
| 2.1 | `src/layouts/DashboardLayout.vue` | el-container/aside/header/main/footer → Tailwind flex 布局；el-menu → 自定义侧边栏（Collapsible + router-link）；el-dropdown → DropdownMenu；图标替换；scoped CSS → Tailwind class |
| 2.2 | `src/pages/index.vue`（登录页） | el-container/aside/main → Tailwind flex；el-form → 原生 form + FormField；el-input → Input；el-button → Button；el-checkbox → Checkbox；scoped CSS → Tailwind class |

### Phase 3：共享组件（预计 1-2 小时）

| 步骤 | 文件 | 改动要点 |
|---|---|---|
| 3.1 | `src/shared/ui/DataTablePagination.vue` | el-pagination → shadcn Pagination 封装 |
| 3.2 | `src/modules/activity/components/ActivityFormDialog.vue` | 检查是否有 Element Plus 依赖（当前无，仅 `<slot />`），确认无需改动 |

### Phase 4：简单页面（预计 3-4 小时）

按从简到复杂顺序迁移：

| 步骤 | 文件 | Element Plus 组件 | 难度 |
|---|---|---|---|
| 4.1 | `dashboard/index.vue` | el-row/col, el-statistic | 低 |
| 4.2 | `dashboard/customer.vue` | el-card, el-button, el-table, el-pagination | 中 |
| 4.3 | `dashboard/user.vue` | el-card, el-button, el-table, el-pagination, el-dialog, el-form, el-input, el-select | 中 |
| 4.4 | `dashboard/dict/type.vue` | el-card, el-form, el-input, el-button, el-table, el-pagination, el-dialog | 中 |
| 4.5 | `dashboard/dict/value.vue` | 同 dict/type + el-select, el-input-number | 中 |

### Phase 5：产品模块页面（预计 3-4 小时）

| 步骤 | 文件 | Element Plus 组件 | 难度 |
|---|---|---|---|
| 5.1 | `dashboard/product/category.vue` | el-card, el-button, el-table, el-tag, el-pagination, el-dialog, el-form, el-input, el-input-number, el-select | 中 |
| 5.2 | `dashboard/product/index.vue` | 同 category + el-input-number(:precision) | 中 |
| 5.3 | `dashboard/product/promotion.vue` | 同 index + el-date-picker | 中高 |
| 5.4 | `dashboard/product/stock.vue` | el-card, el-button, el-icon, el-form, el-input, el-select, el-table, el-pagination, el-dialog, el-divider, el-input-number | 中高 |

### Phase 6：活动与线索模块（预计 3-4 小时）

| 步骤 | 文件 | 特殊点 | 难度 |
|---|---|---|---|
| 6.1 | `dashboard/activity/index.vue` | el-date-picker(datetimerange), el-dialog, 大量内联样式 | 中高 |
| 6.2 | `dashboard/activity/[id].vue` | 丰富的 scoped CSS, :deep() 覆盖最多(9个), 响应式媒体查询 | 高 |
| 6.3 | `dashboard/clue/index.vue` | el-upload（需自定义）, 双 el-dialog, v-hasPermission | 高 |
| 6.4 | `dashboard/clue/detail/[id].vue` | 与 activity/[id].vue 样式高度重复, 12 个 :deep() 覆盖 | 高 |

### Phase 7：交易模块（预计 3-4 小时）

| 步骤 | 文件 | 特殊点 | 难度 |
|---|---|---|---|
| 7.1 | `dashboard/tran/index.vue` | 复杂搜索表单, el-input-number, 产品行动态增删 | 高 |
| 7.2 | `dashboard/tran/[id].vue` | el-descriptions, 促销价格计算展示 | 中 |
| 7.3 | `dashboard/tran/approve/[id].vue` | el-descriptions, el-radio-group, 审批表单 | 中 |
| 7.4 | `dashboard/tran/invoice/[id].vue` | el-descriptions, el-alert, 动态验证规则, el-input-number | 高 |

### Phase 8：系统管理（预计 2-3 小时）

| 步骤 | 文件 | 特殊点 | 难度 |
|---|---|---|---|
| 8.1 | `dashboard/system.vue` | **样式最复杂的文件**：CSS Grid、渐变背景、CSS 动画（@keyframes）、el-descriptions、el-progress、el-badge、el-switch；11 个 :deep() 覆盖 | 极高 |

### Phase 9：清理与验证（预计 1-2 小时）

| 步骤 | 操作 | 说明 |
|---|---|---|
| 9.1 | 卸载 Element Plus 依赖 | `npm uninstall element-plus @element-plus/icons-vue` |
| 9.2 | 全局搜索残留引用 | 搜索 `el-`、`element-plus`、`@element-plus` 确保无残留 |
| 9.3 | TypeScript 编译检查 | `npm run typecheck` |
| 9.4 | ESLint 检查 | `npm run lint` |
| 9.5 | 构建测试 | `npm run build` |
| 9.6 | 单元测试 | `npm run test` |
| 9.7 | 手动功能验证 | 逐页面点击验证功能正常 |

---

## 七、需要创建的自定义/封装组件清单

| 组件名 | 用途 | 对应 Element Plus | 放置位置 |
|---|---|---|---|
| `LoadingButton.vue` | 带 loading 状态的按钮 | el-button(:loading) | `src/shared/ui/` |
| `DescriptionList.vue` | 描述列表（基于 CSS Grid） | el-descriptions | `src/shared/ui/` |
| `DescriptionItem.vue` | 描述列表项 | el-descriptions-item | `src/shared/ui/` |
| `DataTable.vue` | 通用数据表格（TanStack Table + shadcn Table） | el-table | `src/shared/ui/` |
| `DateTimePicker.vue` | 日期时间选择器（Popover + Calendar + TimeSelect） | el-date-picker(datetime) | `src/shared/ui/` |
| `DateTimeRangePicker.vue` | 日期时间范围选择器 | el-date-picker(datetimerange) | `src/shared/ui/` |
| `ConfirmDialog.vue` | 全局确认弹框（Promise 式，供 feedback.ts 调用） | ElMessageBox.confirm | `src/shared/ui/` |
| `IconMapper.ts` | Element Plus 图标名 → lucide 图标组件映射 | 动态菜单图标 | `src/shared/utils/` |

注意：**不再创建 FormField 兼容层组件**。表单验证直接使用 shadcn-vue 的 Form + VeeValidate + Zod 正统方案。

---

## 八、风险评估与注意事项

### 高风险项

1. **el-table 迁移（18 个文件）**：这是最大的工作量来源。el-table 的声明式模板与 TanStack Table 的编程式 API 差异极大。需要在每个文件的 `<script>` 中新增 `ColumnDef[]` 列定义（属于组件逻辑，允许修改），然后用 shadcn Table 组件渲染。特别注意：选择列（type="selection"）、行序号（type="index"）、固定列（fixed="right"）、条纹（stripe）、行悬停事件等功能的等价实现。

2. **el-form 验证迁移（14 个文件有规则）**：不用兼容层，直接用 VeeValidate + Zod。需要把每个文件的 EP 格式 `rules` 对象重写为 Zod schema，把 `formRef.validate()` 回调式调用改为 `handleSubmit`。自定义验证器（如 `checkPhone` 的三参数回调）和动态规则（如 invoice 的 `getDynamicRules` computed）是最大挑战。注意：只改验证相关 TS，API 调用等业务逻辑严禁触碰。

3. **el-date-picker 迁移（7 个文件）**：特别是 `type="datetimerange"` 模式，shadcn 的 Calendar 组件不内置时间选择，需要自行组合。

4. **DashboardLayout.vue 侧边栏菜单**：需要手动实现折叠动画、路由高亮、动态图标渲染，且菜单数据来自后端权限接口。TS 部分不需要改动（仅间接使用 `messageTip`），主要是模板重构。

5. **system.vue 的复杂样式 + 大量 ElMessage 调用**：CSS Grid + 渐变背景 + CSS 动画的组合需要仔细转换为 Tailwind 等价。同时有约 15 处 `ElMessage.xxx()` 需要替换，是消息替换最多的单文件。

### 注意事项

1. **v-hasPermission 自定义指令**：迁移后绑定的目标元素从 `el-button` 变为 `Button`，指令逻辑本身不需要改动。
2. **ECharts 集成**：`dashboard/index.vue` 中的 ECharts 图表容器使用内联样式（`width: 48%; height: 350px; float: left`），需改为 Tailwind class。ECharts 的初始化和更新逻辑严禁修改。
3. **动态图标映射**：后端返回的菜单图标名是 Element Plus 图标名（如 `User`、`Setting`），需要在 lucide-vue-next 中找到同名组件或建立映射表。好消息是两个库的图标名大部分一致（如 `User`、`Home`、`Settings`）。
4. **reka-ui 版本兼容**：shadcn-vue 底层使用 reka-ui（原 radix-vue），确保版本兼容。
5. **TailwindCSS 4 与 3 的差异**：TailwindCSS 4 使用 CSS-first 配置而非 `tailwind.config.js`，所有配置（如自定义颜色、字体）都在 CSS 文件中使用 `@theme` 指令定义。
6. **feedback.ts 改完后影响面**：`feedback.ts` 的函数签名（`messageTip(message, type)` 和 `messageConfirm(message)`）必须保持不变，这样 15 个间接使用的文件无需修改 TS。这是全局影响最大的单文件改动。
7. **Tag type 返回值映射**：`product/promotion.vue` 中的 `getPromotionTypeTag()` 和 `getStatusTag()` 返回 `'success'`/`'warning'`/`'danger'`/`'info'` 是给 `<el-tag>` 用的。迁移后需改为返回 shadcn Badge 的 variant 名或 CSS class。

---

## 九、预计总工时

| 阶段 | 预计时间 |
|---|---|
| Phase 1：基础设施搭建 | 2-3 小时 |
| Phase 2：布局层迁移 | 2-3 小时 |
| Phase 3：共享组件封装 | 1-2 小时 |
| Phase 4：简单页面迁移 | 3-4 小时 |
| Phase 5：产品模块迁移 | 3-4 小时 |
| Phase 6：活动与线索迁移 | 3-4 小时 |
| Phase 7：交易模块迁移 | 3-4 小时 |
| Phase 8：系统管理迁移 | 2-3 小时 |
| Phase 9：清理与验证 | 1-2 小时 |
| **合计** | **20-29 小时** |

---

## 十、迁移后的预期收益

1. **包体积减少**：移除 Element Plus 全量引入（~700KB CSS + ~500KB JS），替换为按需的 shadcn-vue 组件
2. **样式一致性**：Tailwind 的 utility-first 模式消除了散落在各文件中的重复 CSS
3. **更好的类型安全**：shadcn-vue 组件源码直接在项目中，TypeScript 支持更完善
4. **可定制性**：组件源码在 `src/components/ui/` 中，可以完全控制每个细节
5. **现代化**：TailwindCSS 4 的 CSS-first 配置 + Vite 原生支持，开发体验更好
