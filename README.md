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
| JWT (JJWT) | 0.12.3 | Token 签发与验证 |
| Redis | - | Token 缓存、数据字典缓存 |
| MySQL | 8.x | 关系型数据库 |
| HikariCP | - | 数据库连接池 |
| OSHI | 6.4.8 | 系统监控信息采集 |

**前端**

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.3.x | 渐进式 JavaScript 框架 |
| Vite | 6.x | 构建工具 |
| Element Plus | 2.4.x | UI 组件库 |
| ECharts | 5.4.x | 数据可视化 |
| Vue Router | 4.2.x | 前端路由 |
| Axios | 1.6.x | HTTP 客户端 |

## 项目结构

```
car-dealer-crm/
├── dealer-web/                 # 前端 Vue 3 SPA
│   ├── src/
│   │   ├── api/                # API 接口模块
│   │   ├── view/               # 页面组件
│   │   ├── components/         # 公共组件
│   │   ├── router/             # 路由配置
│   │   ├── http/               # Axios 封装
│   │   ├── util/               # 工具函数
│   │   └── assets/             # 静态资源
│   ├── tests/                  # 前端测试 (Vitest)
│   └── package.json
├── dealer-server/              # 后端 Spring Boot
│   ├── src/main/java/com/bjpowernode/
│   │   ├── web/                # Controller 层
│   │   ├── service/            # Service 层
│   │   ├── mapper/             # MyBatis Mapper
│   │   ├── model/              # 数据实体
│   │   ├── config/             # 配置（安全、CORS、转换器）
│   │   ├── manager/            # 业务管理器
│   │   ├── util/               # 工具类
│   │   └── result/             # 统一响应封装
│   ├── src/main/resources/
│   │   ├── mapper/             # MyBatis XML
│   │   ├── application.yml     # 应用配置
│   │   └── CarDealerCRM.sql      # 数据库脚本
│   ├── src/test/               # 后端测试 (JUnit 5, 804 tests)
│   └── pom.xml
└── README.md
```

## 快速开始

**环境要求**

- JDK 17+
- Node.js 18+
- MySQL 8.x
- Redis

**后端启动**

```bash
cd dealer-server

# 创建数据库
mysql -u root -p < src/main/resources/CarDealerCRM.sql

# 配置数据库连接 (src/main/resources/application.yml)
# 设置环境变量或修改配置文件中的 DB_PASSWORD

# 启动
mvn spring-boot:run
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
# 后端测试 (804 tests)
cd dealer-server && mvn test

# 前端测试 (45 tests)
cd dealer-web && npm test

# 覆盖率报告
cd dealer-server && mvn jacoco:report
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

## 测试覆盖

| 指标 | 覆盖率 |
|------|--------|
| 行覆盖率 | 95.9% |
| 方法覆盖率 | 85.4% |
| 类覆盖率 | 99.2% |
| 测试总数 | 849 (后端 804 + 前端 45) |

## 开源协议

本项目基于 [BSD 3-Clause License](LICENSE) 开源。

## 致谢

- [尚硅谷](https://www.atguigu.com/) - 原始动力云客系统
- [Element Plus](https://element-plus.org/) - UI 组件库
- [Spring Boot](https://spring.io/projects/spring-boot) - 后端框架
