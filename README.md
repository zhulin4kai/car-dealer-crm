# Car Dealer CRM

![GitHub stars](https://img.shields.io/github/stars/zhulin4kai/car-dealer-crm?style=flat-square)
![GitHub forks](https://img.shields.io/github/forks/zhulin4kai/car-dealer-crm?style=flat-square)
![GitHub issues](https://img.shields.io/github/issues/zhulin4kai/car-dealer-crm?style=flat-square)
![GitHub last commit](https://img.shields.io/github/last-commit/zhulin4kai/car-dealer-crm?style=flat-square)
![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green?style=flat-square)
![Vue.js](https://img.shields.io/badge/Vue.js-3-brightgreen?style=flat-square)
![License](https://img.shields.io/badge/License-BSD%203--Clause-blue?style=flat-square)

汽车经销商客户关系管理系统 —— 覆盖从线索获取到交易完成的全流程业务。

## 功能特性

| 模块 | 功能 |
|------|------|
| **数据驾驶舱** | 实时销售数据、市场活动 ROI、关键指标可视化 |
| **线索管理** | 线索录入、导入、跟进、转化，全流程状态追踪 |
| **客户管理** | 360° 客户视图，跟进记录，交易历史，Excel 导出 |
| **市场活动** | 活动创建、效果评估、线索关联，支持批量操作 |
| **商品管理** | 商品分类、库存预警、促销管理、价格策略 |
| **交易管理** | 订单创建、审批流程、结算开票、状态流转 |
| **AI 业务助手** | 多轮会话、业务问答、工具调用、业务卡片、人工确认 Proposal、模型 Provider 配置 |
| **统计报表** | 销售漏斗、来源分析、多维度数据统计 |
| **系统管理** | 用户、角色、权限（RBAC）、数据字典、系统监控 |

## 技术栈

**后端**

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.0 | 应用框架 |
| Spring Security | - | 认证授权（JWT + RBAC） |
| MyBatis | 3.0.3 | ORM 框架 |
| PageHelper | 1.4.7 | 分页插件 |
| EasyExcel | 3.3.3 | Excel 导入导出 |
| Auth0 Java JWT | 4.4.0 | Token 签发与验证 |
| Redis | - | Token 缓存、数据字典缓存 |
| MariaDB / MySQL | - | 关系型数据库 |
| HikariCP | - | 数据库连接池 |
| Spring Validation | - | 请求参数校验 |
| Jackson JSR310 | - | Java 时间类型序列化 |
| OSHI | 6.4.8 | 系统监控信息采集 |
| JUnit 5 / Mockito / MockMvc | - | 后端测试 |
| H2 | - | 后端测试数据库 |
| JaCoCo | 0.8.12 | 测试覆盖率报告 |

**AI 编排服务**

| 技术 | 版本 | 说明 |
|------|------|------|
| Python | 3.13.x | `dealer-ai` 运行环境 |
| FastAPI | 0.138.x | 内部 AI 编排接口 |
| Uvicorn | 0.49.x | ASGI 服务 |
| Pydantic / pydantic-settings | 2.13.x / 2.14.x | 请求模型与配置 |
| HTTPX | 0.28.x | 服务间 HTTP 调用 |
| LangGraph | 1.2.x | AI 编排内核 |
| uv | 0.11.x | Python 本地工具链 |

**前端**

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| TypeScript | 5.9.x | 类型系统 |
| Vite | 6.3.x | 构建工具 |
| Tailwind CSS | 4.3.x | 样式系统 |
| shadcn-vue | 2.7.x | UI 组件体系 |
| reka-ui | 2.9.x | 无样式基础组件 |
| @lucide/vue | 1.17.x | 图标库 |
| Pinia | 3.0.x | 状态管理 |
| ECharts | 5.6.x | 数据可视化 |
| Vue Router | 4.5.x | 前端路由 |
| Axios | 1.9.x | HTTP 客户端 |
| TanStack Vue Table | 8.21.x | 表格能力 |
| vee-validate / Zod | 4.15.x / 3.25.x | 表单校验 |
| markdown-it / DOMPurify | 14.x / 3.x | AI Markdown 安全渲染 |
| Vitest | 4.1.x | 前端测试 |
| ESLint / Prettier | 9.x / 3.8.x | 代码检查与格式化 |

## 项目结构

```
car-dealer-crm/
├── dealer-ai/                  # 内部 AI 编排服务（FastAPI + LangGraph）
│   ├── app/
│   │   ├── api/                # 内部路由与健康检查
│   │   ├── core/               # 配置、错误、日志、安全边界
│   │   ├── orchestrator/       # LangGraph 编排
│   │   ├── providers/          # OpenAI-compatible / Anthropic Provider 适配
│   │   ├── schemas/            # 内部请求与事件模型
│   │   └── tools/              # Spring Boot 内部 Tool API 客户端
│   ├── tests/                  # Python 测试
│   └── pyproject.toml
├── dealer-web/                 # 前端 Vue 3 SPA
│   ├── src/
│   │   ├── app/                # 应用入口、插件、全局指令
│   │   ├── components/         # UI 组件
│   │   ├── layouts/            # 布局组件
│   │   ├── modules/            # 业务模块 API、类型、组件
│   │   ├── pages/              # 路由页面
│   │   ├── router/             # 路由配置与守卫
│   │   ├── shared/             # HTTP、storage、utils、通用类型与 UI
│   │   ├── stores/             # Pinia 状态
│   │   └── assets/             # 静态资源
│   ├── tests/                  # 前端测试 (Vitest)
│   └── package.json
├── dealer-server/              # 后端 Spring Boot
│   ├── src/main/java/com/autodealer/crm/
│   │   ├── web/                # Controller 层
│   │   ├── service/            # Service 层
│   │   ├── mapper/             # MyBatis Mapper
│   │   ├── model/              # 数据实体
│   │   ├── query/              # 查询参数对象
│   │   ├── dto/                # 数据传输对象
│   │   ├── config/             # 配置（安全、CORS、转换器）
│   │   ├── aspect/             # AOP 切面
│   │   ├── enums/              # 业务枚举
│   │   ├── manager/            # 业务管理器
│   │   ├── util/               # 工具类
│   │   └── result/             # 统一响应封装
│   ├── src/main/resources/
│   │   ├── mapper/             # MyBatis XML
│   │   ├── application.yml     # 应用配置
│   │   └── CarDealerCRM.sql      # 数据库脚本
│   ├── src/test/               # 后端测试 (JUnit 5)
│   └── pom.xml
├── docs/                       # 项目文档、审计清单与规格文档
└── README.md
```

## 服务职责与启动入口

| 服务 | 职责 | 默认入口 |
|------|------|----------|
| `dealer-web` | 浏览器端 CRM 和 AI 工作台，只调用 Spring Boot API，不直连 `dealer-ai` | `http://localhost:5173`（本地开发）/ `http://localhost:8080`（演示容器） |
| `dealer-server` | 业务控制面，负责认证、权限、数据范围、事务、审计、Provider 配置、AI Conversation / Run / Tool / Proposal / Workflow 持久化 | `http://localhost:8089` |
| `dealer-ai` | 内部 AI 编排面，只供 `dealer-server` 调用，负责 LangGraph 编排、模型 Provider 适配和 Spring Boot 内部 Tool API 调用 | `http://localhost:8091` |
| MySQL / MariaDB | 业务关系型数据库 | `localhost:3306`（本地）/ `localhost:13306`（演示容器） |
| Redis | JWT、缓存和会话辅助数据 | `localhost:6379`（本地）/ `localhost:16379`（演示容器） |

AI 业务助手的正式模型配置由管理员在系统内维护。API Key 由 `dealer-server` 加密入库，`dealer-ai` 只在单次内部请求中接收运行时配置，不从前端或本地环境变量读取正式模型密钥。

## 快速开始

### 一键运行

一键运行只要求脚本能够安装或检测到 Docker / Docker Desktop。Maven、JDK、Node.js、MySQL、Redis 都在容器中构建或运行，不要求用户电脑预装。

当前一键演示脚本启动核心 CRM 服务：`dealer-web`、`dealer-server`、MySQL 和 Redis。完整 AI 助手能力需要额外启动 `dealer-ai`，并确保 `dealer-server` 的 `DEALER_AI_BASE_URL` 指向该服务。

macOS / Linux / fish：

```bash
bash scripts/demo-bootstrap.sh
fish scripts/demo-bootstrap.fish
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-bootstrap.ps1
```

脚本会检测系统和 Shell，按菜单引导安装或启动 Docker，测试 Docker Hub 镜像源，拉取 MySQL/Redis 镜像，构建后端与前端镜像，并在启动前检查端口冲突。

默认访问地址：

```text
前端：http://localhost:8080
后端：http://localhost:8089
MySQL：localhost:13306
Redis：localhost:16379
```

默认容器环境变量见 `.env.demo`：

```text
DB_USERNAME=root
DB_PASSWORD=123456
JWT_SECRET=car-dealer-crm-local-secret
```

### 清理与卸载

如果只是试运行，或运行后不想保留项目残留，可以执行清理脚本：

macOS / Linux / fish：

```bash
bash scripts/demo-cleanup.sh
fish scripts/demo-cleanup.fish
```

Windows PowerShell：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/demo-cleanup.ps1
```

清理脚本会先列出即将删除的项目，再让用户选择全部删除或部分删除，最后要求输入 `DELETE` 二次确认。可删除内容包括：

- 当前项目的容器、网络、数据卷和本地构建镜像。
- 启动脚本拉取过的 MySQL / Redis 镜像。
- 启动脚本主动安装过的 Docker / Docker Desktop / Docker Compose。
- 启动脚本主动写入过的 Linux Docker daemon 镜像配置。
- 安装记录目录 `~/.car-dealer-crm-demo`。

Docker 本体只会在启动脚本记录到“由脚本主动安装”时才出现在卸载列表中。

### 本地开发启动

如果要做本地开发，而不是容器一键运行，需要准备：

- JDK 17+
- Node.js 18+
- Python 3.13+
- uv
- MariaDB / MySQL
- Redis

建议本地启动顺序：

1. MySQL / Redis。
2. `dealer-ai`。
3. `dealer-server`。
4. `dealer-web`。

AI 编排服务启动：

```bash
cd dealer-ai

uv sync

export DEALER_AI_INTERNAL_TOKEN='dev-internal-token'
export DEALER_AI_SPRING_TOOL_BASE_URL='http://localhost:8089/internal/ai'
export DEALER_AI_SPRING_TOOL_TOKEN='dev-internal-token'

uv run uvicorn app.main:app --reload --port 8091
```

后端启动：

```bash
cd dealer-server

mysql -u root -p < src/main/resources/CarDealerCRM.sql

export DB_USERNAME='your-username'
export DB_PASSWORD='your-local-password'
export JWT_SECRET='replace-with-a-long-random-local-secret'
export DEALER_AI_BASE_URL='http://localhost:8091'
export DEALER_AI_INTERNAL_TOKEN='dev-internal-token'
export DEALER_AI_TOOL_TOKEN='dev-internal-token'

./mvnw spring-boot:run
```

前端启动：

```bash
cd dealer-web

npm install
npm run dev
```

本地 AI 模型 Provider 不在 `dealer-ai` 的 `.env` 中配置。启动后进入系统的 AI 模型配置页面，由管理员创建、测试并启用 Provider 配置。

### 测试

```bash
# 后端测试
cd dealer-server && ./mvnw test

# AI 编排服务测试
cd dealer-ai && uv run ruff check .
cd dealer-ai && uv run pytest

# 前端测试
cd dealer-web && npm test

# 覆盖率报告
cd dealer-server && ./mvnw jacoco:report
# 报告位于 target/site/jacoco/index.html
```

## 架构说明

**认证流程**

```
前端登录 → POST /api/login → Spring Security 验证
  → 签发 JWT → 存入 Redis (TTL)
  → 后续请求携带 Authorization: Bearer <token>
  → TokenVerifyFilter 拦截 → JWT 解析 → Redis 校验
```

**数据权限**

系统基于 RBAC 模型实现细粒度权限控制：
- 用户 → 角色 → 权限（菜单 + 按钮）
- 数据范围过滤（DataScope AOP 切面）
- 前端 `v-hasPermission` 指令控制按钮可见性

**响应格式**

```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

## 开源协议

本项目基于 [BSD 3-Clause License](LICENSE) 开源。

## 致谢

- [尚硅谷](https://www.atguigu.com/) - 原始动力云客系统
- [shadcn-vue](https://www.shadcn-vue.com/) - UI 组件体系
- [reka-ui](https://reka-ui.com/) - 无样式基础组件
- [Tailwind CSS](https://tailwindcss.com/) - 样式系统
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
