# CRM Dashboard 飞书风格 UI 设计文档

## 文档定位

本文是 `/dashboard` 工作台首页的 UI 设计迁移说明，用于把 React 设计稿转换为本项目 Vue 3 可落地的设计方案。

- 本文不修改业务 Spec，不定义后端业务规则，不替代执行计划。
- 目标实现技术栈是 Vue 3 + TypeScript + shadcn-vue + TailwindCSS。
- 参考项目使用 React，仅作为视觉、布局和组件结构参考，禁止直接照搬 JSX、React state 或 Recharts 实现。
- AI 对话侧栏是可选功能模块，首期 Dashboard 不应强依赖 AI 能力上线。

参考输入：

- 外部 Dashboard 视觉稿中的设计 token、组件层次和 AI 侧栏交互。
- 关键参考范围：设计 token、全局壳层、侧边栏、顶部栏、指标卡片、最新线索表、业务动态、图表卡片和 AI 对话侧栏。
- 用户提示词：参考设计稿文件，用 Vue 3 + shadcn-vue + TailwindCSS 重新实现 CRM Dashboard；设计 token 在 `styles.css` 中定义，组件结构参考各 `.jsx` 文件；保持飞书风格，主色 `#3370FF`，背景 `#F5F6F7`，字体 `PingFang SC`；AI 对话侧栏作为可选功能模块。

## 当前项目事实

当前前端项目已经具备 Vue 3、Vite、TypeScript、Pinia、Vue Router、shadcn-vue、TailwindCSS、lucide 图标和 ECharts。

- Dashboard 路由入口：`dealer-web/src/router/routes.ts` 的 `/dashboard`。
- Dashboard 布局：`dealer-web/src/layouts/DashboardLayout.vue`。
- Dashboard 首页：`dealer-web/src/pages/dashboard/index.vue`。
- 现有统计 API：`fetchSummaryData()`、`fetchSaleFunnelData()`、`fetchSourcePieData()`。
- 当前首页只包含四个概览卡片和两个 ECharts 图表，布局密度、右侧业务洞察、最新线索工作区和飞书风格顶部栏尚未形成。

## 设计目标

Dashboard 首页应从“简单统计页”调整为“销售工作台概览”：

- 第一屏呈现左侧导航、顶部工作台栏、主工作区、右侧业务洞察栏。
- 主视觉保持飞书/Lark 风格：轻背景、白色面板、细边框、低阴影、高密度业务信息。
- 主色固定为 `#3370FF`，页面背景固定为 `#F5F6F7`。
- 字体优先使用 `PingFang SC`，并提供系统字体 fallback。
- 业务内容围绕汽车销售 CRM 的线索、客户、交易、活动、产品和库存，不使用通用项目管理文案。
- 首页重点工作流是查看经营概览、查看最新线索、快速录入线索、观察漏斗和来源分布。

## 页面信息架构

目标页面结构：

```text
DashboardLayout.vue
  左侧导航 Sidebar
    品牌区
    业务管理
    产品中心
    系统
    底部当前用户

  主区域 Shell
    DashboardHeader
      标题 + 日期
      全局搜索入口
      AI 助手入口（可选）
      通知入口
      用户菜单

    DashboardWorkspace
      主工作区
        MetricCards
        RecentCluesTable

      右侧洞察栏
        ActivityFeed
        SalesFunnelCard
        SourceDistributionCard

      AI Assistant Panel（可选，打开后占用右侧独立宽度）
```

## 设计 Token

Token 应落在 `dealer-web/src/assets/index.css` 的 CSS 变量层，页面组件通过 Tailwind class 和 CSS 变量消费，不从 React 稿迁移内联 style。

| 类型 | Token | 值 | 用途 |
| --- | --- | --- | --- |
| 主色 | `--crm-primary` | `#3370FF` | 主按钮、活动菜单、链接、关键图表 |
| 主色 Hover | `--crm-primary-hover` | `#2860E1` | 主按钮 hover |
| 主色浅底 | `--crm-primary-light` | `#E1EAFF` | 当前菜单、头像底色 |
| 主色极浅底 | `--crm-primary-subtle` | `#F0F4FF` | AI 建议项 hover、轻提示 |
| 页面背景 | `--crm-bg-page` | `#F5F6F7` | 工作台背景 |
| 面板背景 | `--crm-bg-surface` | `#FFFFFF` | 卡片、侧栏、顶部栏 |
| 弱背景 | `--crm-bg-muted` | `#F0F1F2` | 标签、kbd、表头弱底 |
| 一级文本 | `--crm-text-primary` | `#1F2329` | 标题、核心数字 |
| 二级文本 | `--crm-text-secondary` | `#646A73` | 表格正文、说明 |
| 三级文本 | `--crm-text-tertiary` | `#8F959E` | 辅助说明、时间 |
| 边框 | `--crm-border` | `#DEE0E3` | 输入框、按钮边框 |
| 浅边框 | `--crm-border-light` | `#EBEDF0` | 卡片分隔、布局分隔 |
| 成功 | `--crm-success` | `#34C759` | 成交、增长、正向指标 |
| 警告 | `--crm-warning` | `#FF9500` | 跟进中、交易漏斗 |
| 危险 | `--crm-danger` | `#FF3B30` | 已流失、异常提醒 |
| 信息 | `--crm-info` | `#007AFF` | 信息状态、普通提示 |

字体：

```css
--crm-font-sans: -apple-system, BlinkMacSystemFont, "PingFang SC",
  "Helvetica Neue", "Microsoft YaHei", sans-serif;
```

字号应保持后台产品密度：

| 层级 | 值 | 场景 |
| --- | --- | --- |
| `12px` | 辅助信息 | 表头、时间、标签 |
| `13px` | 小正文 | 顶栏搜索、按钮、动态说明 |
| `14px` | 正文 | 表格、菜单、卡片正文 |
| `16px` | 小标题 | 卡片标题、模块标题 |
| `20px` | 页面标题 | 工作台概览 |
| `28px` | 指标数字 | 统计卡主数值 |

布局尺寸：

| Token | 值 | 用途 |
| --- | --- | --- |
| `--crm-sidebar-width` | `240px` | 展开侧栏 |
| `--crm-sidebar-collapsed-width` | `64px` | 折叠侧栏 |
| `--crm-header-height` | `56px` | 顶部栏 |
| `--crm-right-panel-width` | `340px` | 右侧洞察栏 |
| `--crm-ai-panel-width` | `420px` | AI 侧栏 |
| `--crm-radius-card` | `8px` | 卡片、按钮、菜单项 |
| `--crm-shadow-card` | `0 1px 3px rgba(0, 0, 0, 0.04), 0 0 0 1px rgba(0, 0, 0, 0.02)` | 卡片阴影 |

## 组件迁移映射

| React 参考组件 | Vue 目标组件建议 | 说明 |
| --- | --- | --- |
| `App.jsx` | `DashboardLayout.vue` + `pages/dashboard/index.vue` | Layout 负责壳层，首页负责工作台内容 |
| `sidebar.jsx` | `DashboardLayout.vue` 内侧栏，或提取 `DashboardSidebar.vue` | 必须继续使用后端权限菜单，不写死演示菜单 |
| `header.jsx` | `DashboardHeader.vue` | 标题、日期、搜索、AI 入口、通知、用户菜单 |
| `stats-cards.jsx` | `DashboardMetricCards.vue` | 四个核心指标卡，支持趋势和环比 |
| `project-table.jsx` | `DashboardRecentCluesTable.vue` | 改名为最新线索表，不沿用 Project 命名 |
| `activity-feed.jsx` | `DashboardActivityFeed.vue` | 业务动态列表，可先由现有业务数据组合 |
| `charts.jsx` | `DashboardInsightCharts.vue` | Vue 侧优先继续使用 ECharts，不引入 Recharts |
| `ai-chat.jsx` | `DashboardAiAssistant.vue` + `AiTriggerButton.vue` | 可选模块，默认关闭 |

## 左侧导航设计

左侧导航沿用项目当前权限菜单来源，不应改成设计稿里的本地 `navGroups` 静态数组。

视觉要求：

- 宽度展开 `240px`，折叠 `64px`。
- 背景为白色，右侧边框 `#EBEDF0`。
- 顶部品牌区高度 `56px`，左侧为蓝色汽车图标块，右侧为“汽车销售管理系统”。
- 菜单分组显示为“业务管理”、“产品中心”、“系统”。
- 菜单项高度约 `40px`，图标 `18px`，圆角 `8px`。
- 当前菜单背景 `#E1EAFF`，文字和图标为 `#3370FF`。
- 非当前菜单文字为 `#646A73`，hover 背景 `#F5F6F7`。
- 数字 badge 用于待处理数量，例如线索 6、交易 2；首期没有真实待办数量时不展示假 badge。
- 底部用户区显示当前用户头像缩写、姓名和角色；数据来自 `authStore.currentUser` 和权限信息。

交互要求：

- 折叠状态保留图标，图标按钮必须有 Tooltip 或 `aria-label`。
- 侧栏折叠状态继续由 `stores/app.store.ts` 维护。
- 菜单高亮继续依据 `route.meta.activeMenu` 和当前路由。

## 顶部栏设计

顶部栏是全局工作台操作区，高度 `56px`。

左侧：

- 标题固定为“工作台概览”。
- 日期以当前日期显示，如“6月22日”，使用本地日期格式化，不写死设计稿日期。

右侧：

- 全局搜索入口：占位文案“搜索线索、客户、交易...”，右侧显示 `⌘K` 或 Windows 下 `Ctrl K`。
- AI 助手入口：可选显示。没有 AI 功能开关时不显示，或以禁用态进入后续版本。
- 通知入口：铃铛图标，可显示红点。没有通知接口时只保留视觉占位需谨慎，避免误导用户。
- 用户菜单：头像缩写、用户名、下拉箭头，继续承载退出登录。

实现要求：

- 图标使用 `@lucide/vue`。
- 搜索入口首期可以作为按钮打开全局搜索 Dialog；若未实现搜索，不应展示无法使用的输入框。
- 顶栏用户菜单继续使用 shadcn-vue `DropdownMenu`。

## 主工作区

主工作区在桌面端占据中间弹性宽度，padding `24px`，模块间距 `20px`。

### 指标卡片

四张卡片横向排列：

1. 市场活动：有效活动数 / 总活动数。
2. 线索总数：总线索和本月新增。
3. 客户总数：总客户和本月新增。
4. 交易总额：已成交金额和交易总额。

每张卡包含：

- 左上图标块，背景使用对应浅色。
- 右上小型趋势折线，可以用 SVG polyline 实现，不需要引入新图表库。
- 指标名称。
- 主数值和单位。
- 增量 badge，正向使用绿色；若接口没有增量字段，隐藏 badge，不写静态假数据。
- 口径说明，例如“有效/总数 · 本月新增”。

当前 `SummaryData` 已能支撑部分数据，但类型定义较宽松。实施时需要按后端实际返回补充明确字段，而不是继续依赖 `LooseRecord` 隐式读取。

### 最新线索表

“最新线索”是首页核心工作区，应从设计稿的 `ProjectTable` 语义调整为 CRM 线索表。

表头：

- 姓名
- 手机
- 负责人
- 所属活动
- 意向状态
- 意向产品
- 来源
- 下次联系
- 操作

数据来源：

- 优先复用 `fetchCurrentClues(1)` 获取第一页线索，首页展示前 6 条。
- 负责人使用 `ownerDO.name`。
- 所属活动使用 `activityDO.name`。
- 意向状态使用 `intentionStateDO.typeValue`。
- 意向产品使用 `intentionProductDO.name`。
- 来源使用 `sourceDO.typeValue`。
- 下次联系使用 `nextContactTime`。

视觉要求：

- 表格所在卡片使用白底、浅边框、低阴影。
- 卡片头部左侧显示“最新线索”和总数 badge。
- 卡片头部右侧提供“录入线索”和“查看全部”。
- “录入线索”应复用线索页已有录入能力或跳转 `/dashboard/clue` 后打开录入入口；没有跨页弹窗能力时先跳转列表页。
- 手机号展示可做脱敏，遵守后端返回和现有数据权限。
- 意向状态使用带圆点的 pill badge，不能只靠颜色表达状态。

状态色建议：

| 状态 | 文字色 | 背景色 |
| --- | --- | --- |
| 有意向 | `#007AFF` | `#E5F1FF` |
| 跟进中 | `#FF9500` | `#FFF4E5` |
| 已成交 | `#34C759` | `#E8F8ED` |
| 已流失 | `#FF3B30` | `#FFEBE9` |
| 待跟进 | `#9B59B6` | `#F3E8FF` |

## 右侧洞察栏

右侧洞察栏宽度 `340px`，位于桌面端右侧，背景同页面背景，内部垂直排列业务动态、销售漏斗、线索来源分布。

### 业务动态

业务动态用于展示近期 CRM 操作，而不是系统日志详情页。

建议展示内容：

- 新线索录入。
- 成交交易。
- 电话或备注跟进。
- 意向产品变更。
- 审批提交。
- 线索转客户或成交。

数据来源分级：

1. 若后端已有统一操作记录接口，优先使用可展示给业务用户的操作摘要。
2. 若没有统一接口，首期可以由最近线索、最近交易、最近备注组合生成动态，但文档和代码中必须标记为组合视图。
3. 不使用纯静态 mock 数据上线。

视觉要求：

- 每条动态包含操作者头像缩写、动作图标、动作句子、业务对象、相对时间。
- 支持滚动，卡片头部固定“业务动态”和“查看全部”。

### 销售漏斗

销售漏斗沿用当前 `fetchSaleFunnelData()`，但视觉需改为右侧卡片中的紧凑图。

阶段：

- 线索
- 客户
- 交易
- 成交

要求：

- 使用 ECharts `funnel` 实现，不迁移 React 设计稿的 Recharts。
- 阶段颜色依次为蓝、浅蓝、橙、绿。
- 图下展示三段转化率：线索到客户、客户到交易、交易到成交。
- 转化率由接口数据计算；无法计算时隐藏，不写固定百分比。

### 线索来源分布

线索来源分布沿用当前 `fetchSourcePieData()`。

要求：

- 使用 ECharts `pie` 或 `doughnut` 样式。
- 左侧为环形图，右侧为来源图例和数量。
- 色板建议：蓝、绿、橙、紫、信息蓝、灰。
- 图例名称以接口返回 `name` 为准。

## AI 助手可选模块

AI 助手不是 Dashboard 首期必须项，应作为独立可开关模块设计。

入口：

- 顶栏“AI 助手”按钮。
- 关闭状态下可选显示右下角浮动按钮。

模式：

- `closed`：不占用布局空间。
- `sidebar`：右侧打开 `420px` 侧栏。
- `fullscreen`：占据主内容区，隐藏普通工作台内容。

能力范围：

- 查询今日待跟进客户。
- 分析本月线索转化率。
- 汇总最近一周待联系线索。
- 生成销售周报摘要。

落地边界：

- 没有真实 AI 接口前，不应让 mock 对话伪装成可用生产能力。
- 可以先实现静态空状态、建议问题和输入框禁用态。
- 后续接入 AI 时，必须明确数据权限、脱敏、审计和错误提示。
- AI 回答需提示“基于系统数据生成，仅供参考”，但不能替代审批、成交、退款等强业务操作。

## 响应式规则

桌面端：

- `>= 1440px`：完整三栏布局，左侧 `240px`，右侧 `340px`。
- `1200px - 1439px`：右侧洞察栏保留，主工作区表格允许横向滚动。
- `< 1200px`：右侧洞察栏移动到主工作区下方，变为两列或单列卡片。

平板端：

- 侧栏默认折叠为 `64px`。
- 指标卡片变为两列。
- 最新线索表保留横向滚动。

移动端：

- 侧栏改为抽屉或隐藏，不占据固定宽度。
- 顶栏搜索收起为图标按钮。
- 指标卡片单列。
- 右侧洞察模块按业务动态、漏斗、来源依次堆叠。
- AI 只支持 fullscreen，不打开右侧固定侧栏。

## Vue 实现约束

实现时必须遵守当前前端规则：

- 页面使用 `<script setup lang="ts">`。
- 业务页面放在 `dealer-web/src/pages/dashboard/index.vue`。
- 可复用 Dashboard 业务组件可放在 `dealer-web/src/modules/statistic/components/` 或 `dealer-web/src/pages/dashboard/components/`，不要放进 `components/ui/`。
- 基础 UI 使用 `dealer-web/src/components/ui` 下的 shadcn-vue 组件。
- 图标使用 `@lucide/vue`，不复制 React 图标代码。
- 图表继续使用 `echarts`，不新增 Recharts。
- 样式使用 Tailwind class、CSS 变量和项目 `cn()` 工具，不迁移 React 稿内联 style。
- API 调用必须通过模块 API 文件，不在页面直接调用 Axios。
- 异步加载需要区分 summary、clue list、funnel、source pie、activity feed 的 loading 和 error。
- 不使用静态 mock 数据替代真实接口结果；暂未接入的数据要显示空状态或隐藏模块。

## 待确认接口和数据口径

以下数据若现有接口不足，实施前需要确认是否新增后端字段或前端组合计算：

- 市场活动有效数、总数、本月新增。
- 线索本月新增、客户本月新增、交易本月新增或成交金额环比。
- 首页最新线索总数与列表排序规则。
- 业务动态来源和业务用户可见文案。
- 线索到客户、客户到交易、交易到成交的转化口径。
- AI 助手是否启用、面向哪些角色开放、是否记录问答审计。

## 验收标准

实现完成后应满足：

- `/dashboard` 首屏与参考图保持同一信息架构：左侧导航、顶部栏、主指标区、最新线索、右侧洞察。
- 主色、背景、字体、卡片密度和状态色符合本文 token。
- React 参考项目只作为视觉参考，目标代码中不出现 React、JSX、Recharts 依赖。
- 现有权限菜单、登录用户、退出登录和路由高亮行为不退化。
- 没有真实数据的模块不展示假业务数字。
- 桌面、平板、移动视口下文字不溢出、控件不重叠、表格可滚动。
- 验证命令至少包括 `npm run typecheck`、`npm run lint`、`npm run build`。
- 视觉验收建议补充浏览器截图：`1440x900`、`1920x1080`、`768x1024`、`375x812`。
