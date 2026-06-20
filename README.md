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
| Vitest | 4.1.x | 前端测试 |
| ESLint / Prettier | 9.x / 3.8.x | 代码检查与格式化 |

## 项目结构

```
car-dealer-crm/
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

## 快速开始

**环境要求**

- JDK 17+
- Node.js 18+
- MariaDB / MySQL
- Redis

**后端启动**

```bash
cd dealer-server

# 创建数据库
mysql -u root -p < src/main/resources/CarDealerCRM.sql

# 配置数据库连接 (src/main/resources/application.yml)
# 设置环境变量或修改配置文件中的 DB_USERNAME / DB_PASSWORD
export DB_USERNAME='your-username'
export DB_PASSWORD='your-local-password'
export JWT_SECRET='replace-with-a-long-random-local-secret'

# 启动
./mvnw spring-boot:run
```

**前端启动**

```bash
cd dealer-web

# 安装依赖
npm install

# 启动开发服务器 (http://localhost:8081)
npm run dev
```

**测试**

```bash
# 后端测试
cd dealer-server && ./mvnw test

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
