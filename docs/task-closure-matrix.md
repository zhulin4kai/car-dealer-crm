# 任务关闭矩阵与二次审计证据

更新时间：2026-06-26

## 1. 基线

最近 6 个分类提交作为本轮继续整改的已完成基线：

| Commit | 范围 | 关闭依据 |
|---|---|---|
| `68e1a8f3 fix(core): 对齐认证审计与字典治理` | 认证、HTTP 401/403、Redis 会话、字典缓存、审计基础 | `docs/spec/用户权限/**`、`docs/spec/数据字典/**`、`docs/spec/审计日志/**`、对应 plan/task |
| `7f83d0cc feat(customer): 对齐活动线索与试驾流程` | 线索、客户、市场活动、试驾 | `docs/spec/线索管理/**`、`docs/spec/客户管理/**`、`docs/spec/市场活动/**`、`docs/spec/试驾管理/**` |
| `8fe09c8f feat(product): 对齐促销报价与库存边界` | 商品、促销、报价、订单、库存 | `docs/spec/车辆商品/**`、`docs/spec/促销政策/**`、`docs/spec/报价订单/**`、`docs/spec/库存管理/**` |
| `a65d033a fix(tran): 对齐交易履约与收退款发票` | 交易履约、收款退款、发票、完成聚合 | `docs/spec/交易履约/**`、`docs/spec/收款退款/**`、`docs/spec/发票管理/**` |
| `c56d5630 feat(follow): 落地跟进任务与沟通记录` | 跟进任务、沟通记录 | `docs/spec/跟进任务/**`、对应 plan/task |
| `bc11ce56 docs(contract): 同步接口契约与数据库结构` | OpenAPI、Schema、顶层技术文档 | `docs/api/**`、`docs/backend.md`、`docs/frontend.md`、`docs/integration.md` |

本次继续整改新增证据：

| 范围 | 文件 | 证据 |
|---|---|---|
| 旧接口删除 | `ActivityController`、`CustomerController`、`TranController`、前端 module API、`openapi.yaml` | 删除 `/api/activitys`、`/api/customer/list`、`/api/tran/create`、`/api/tran/batch-delete`、直接退款旧接口和交易物理删除入口 |
| 旧分页参数删除 | `UserController`、`ClueController`、`AuditLogController`、备注 Controller、前端 module API、`openapi.yaml` | 外部请求统一 `page/size`；`current/pageSize` 仅作为后端内部 PageHelper 字段，不再是 API 契约 |
| 前端旧 HTTP 入口删除 | `dealer-web/src/shared/api/http-client.ts` | 删除 `doGet/doPost/doPut/doDelete`，业务只能走 `httpClient` 和领域 module API |
| 契约测试 | `ApiEndpointContractTest`、`OpenApiSpecificSchemaContractTest`、`api-endpoints.test.ts` | 负向断言旧路径、`deprecated: true`、旧分页参数和旧前端调用不再出现 |
| 真实环境验证入口 | `ExternalMysqlSchemaVerificationTest`、`ExternalRedisVerificationTest` | 通过环境变量启用真实 MySQL/MariaDB 和 Redis 验证；无外部服务时默认跳过，不伪造结果 |

## 2. Task 关闭矩阵

| 业务目录 | Task 数 | Spec | Plan | Rule | 主要代码/契约落点 | 当前状态 |
|---|---:|---|---|---|---|---|
| 用户权限 | 8 | `docs/spec/用户权限/**` | `docs/plan/用户权限/**` | `docs/rule/05-API认证与错误响应规范.md`、`docs/rule/06-业务一致性与事务规范.md` | Security、User、Redis、权限测试、OpenAPI | 仓库测试已覆盖；Redis 真实失败语义需外部 Redis 复核 |
| 数据字典 | 8 | `docs/spec/数据字典/**` | `docs/plan/数据字典/**` | `docs/rule/04-数据库与MyBatis规范.md`、`docs/rule/06-业务一致性与事务规范.md` | Dict、Redis 缓存、Schema、字典前端 | 仓库测试已覆盖；Redis TTL/失效需外部 Redis 复核 |
| 经营分析 | 13 | `docs/spec/经营分析/**` | `docs/plan/经营分析/**` | `docs/rule/05-API认证与错误响应规范.md`、`docs/rule/07-测试编写执行与验收规范.md` | Statistic、OpenAPI、分页、时间格式、前端 API | 已补旧接口与分页收口；性能需真实数据量压测 |
| 车辆商品 | 4 | `docs/spec/车辆商品/**` | `docs/plan/车辆商品/**` | `docs/rule/04-数据库与MyBatis规范.md` | Product、Category、库存约束、前端表单 | 仓库测试已覆盖；生产数据迁移前需真实 Schema 复核 |
| 库存管理 | 7 | `docs/spec/库存管理/**` | `docs/plan/库存管理/**` | `docs/rule/06-业务一致性与事务规范.md` | ProductStock、车辆实例、库存流水、并发测试 | 仓库测试已覆盖；并发锁语义需真实 MySQL 复核 |
| 促销政策 | 5 | `docs/spec/促销政策/**` | `docs/plan/促销政策/**` | `docs/rule/06-业务一致性与事务规范.md` | Promotion、报价快照、预算并发、前端选择 | 仓库测试已覆盖；预算并发需真实 MySQL 复核 |
| 报价订单 | 8 | `docs/spec/报价订单/**` | `docs/plan/报价订单/**` | `docs/rule/06-业务一致性与事务规范.md` | Quote、Order、Tran 订单成立、OpenAPI | 仓库测试已覆盖；锁车并发需真实 MySQL 复核 |
| 收款退款 | 7 | `docs/spec/收款退款/**` | `docs/plan/收款退款/**` | `docs/rule/06-业务一致性与事务规范.md` | Payment、RefundRequest、幂等、审批、前端弹窗 | 仓库测试已覆盖；资金幂等并发需真实 MySQL 复核 |
| 发票管理 | 6 | `docs/spec/发票管理/**` | `docs/plan/发票管理/**` | `docs/rule/06-业务一致性与事务规范.md` | Invoice、红冲、重开、脱敏、完成聚合 | 仓库测试已覆盖；并发开票需真实 MySQL 复核 |
| 交易履约 | 4 | `docs/spec/交易履约/**` | `docs/plan/交易履约/**` | `docs/rule/06-业务一致性与事务规范.md` | Tran、状态机、取消、关闭、历史 | 仓库测试已覆盖；交易服务跨领域职责仍是 P2 技术债 |
| 交付管理 | 6 | `docs/spec/交付管理/**` | `docs/plan/交付管理/**` | `docs/rule/06-业务一致性与事务规范.md` | Delivery、签收、出库联动、前端页面 | 仓库测试已覆盖；交付/库存并发需真实 MySQL 复核 |
| 线索管理 | 5 | `docs/spec/线索管理/**` | `docs/plan/线索管理/**` | `docs/rule/04-数据库与MyBatis规范.md` | Clue、导入、转客户、责任历史、删除保护 | 仓库测试已覆盖；手机号唯一并发需真实 MySQL 复核 |
| 客户管理 | 7 | `docs/spec/客户管理/**` | `docs/plan/客户管理/**` | `docs/rule/05-API认证与错误响应规范.md` | Customer、主档、合并、归属、脱敏、导出 | 仓库测试已覆盖；已删除旧 `/api/customer/list` |
| 市场活动 | 5 | `docs/spec/市场活动/**` | `docs/plan/市场活动/**` | `docs/rule/05-API认证与错误响应规范.md` | Activity、归因、ROI、导出、备注 | 仓库测试已覆盖；已删除旧 `/api/activitys` |
| 商机管理 | 6 | `docs/spec/商机管理/**` | `docs/plan/商机管理/**` | `docs/rule/06-业务一致性与事务规范.md` | Opportunity、阶段、输赢单、前端页面 | 仓库测试已覆盖 |
| 试驾管理 | 6 | `docs/spec/试驾管理/**` | `docs/plan/试驾管理/**` | `docs/rule/06-业务一致性与事务规范.md` | TestDrive、改期、取消、签到、完成 | 仓库测试已覆盖 |
| 跟进任务 | 2 | `docs/spec/跟进任务/**` | `docs/plan/跟进任务/**` | `docs/rule/12-日志与审计规范.md` | FollowTask、CommunicationRecord | 仓库测试已覆盖 |
| 审计日志 | 10 | `docs/spec/审计日志/**` | `docs/plan/审计日志/**` | `docs/rule/12-日志与审计规范.md` | AuditLog、登录记录、操作记录、前端页面、OpenAPI | 仓库测试已覆盖；导出数据量需真实环境抽样 |

## 3. P0/P1 二次关闭状态

| 审计项 | 当前状态 | 关闭证据 |
|---|---|---|
| P0-01 客户 ID 越权 | 已代码整改 | 交易创建/更新通过服务端数据范围校验，前端不提交可信归属字段，权限测试覆盖越权拒绝 |
| P0-02 经理/财务跨团队越权 | 已代码整改，需真实组织数据复核 | 交易列表、详情、命令接口统一数据范围；需用真实团队/门店授权数据抽样 |
| P0-03 统计全局越权 | 已代码整改 | Statistic 使用当前用户数据范围，测试覆盖明细反算 |
| P0-04 报价扣真实库存 | 已代码整改 | 报价仅保存快照，订单/锁车才生成库存占用流水 |
| P0-05 商品编辑覆盖库存 | 已代码整改 | 商品编辑 DTO 不允许改库存，库存变更走库存命令和流水 |
| P0-06 收款无幂等 | 已代码整改，需真实 MySQL 并发复核 | 收款引用号/幂等键约束与重复请求测试；真实库并发由外部测试入口复核 |
| P0-07 收齐款自动完成交易 | 已代码整改 | 收款确认只更新资金子状态，交易完成由聚合条件触发 |
| P0-08 退款直接取消并恢复库存 | 已代码整改，需真实 MySQL 并发复核 | 退款申请、审批、执行拆分；不直接取消交易或恢复库存 |
| P0-09 Redis 会话撤销失败吞掉 | 已代码整改，需真实 Redis 复核 | Redis 写删失败不伪成功；新增真实 Redis 验证入口 |
| P0-10 生产/测试 Schema 不等价 | 已代码整改，需真实 MySQL 复核 | 生产和测试 schema 已同步；新增真实 MySQL 约束验证入口 |
| P1-01 ~ P1-04 线索/客户主档 | 已代码整改 | 手机号唯一、删除保护、转客户不自动交易、客户主档独立 |
| P1-05 独立业务对象缺失 | 已代码整改 | 商机、试驾、报价订单、交付、跟进任务已有独立对象和 API |
| P1-06 ~ P1-08 发票/收款/审批历史 | 已代码整改 | 发票部分开票、红冲重开；收款登记/确认拆分；旧交易删除入口已删除 |
| P1-09 ~ P1-14 库存/字典/Redis | 已代码整改，需真实 Redis/MySQL 复核 | 库存流水、恢复幂等、历史保护、字典缓存 TTL/失效、负责人缓存 TTL |
| P1-15 ~ P1-16 HTTP 与登录 Redis | 已代码整改，需真实 Redis 复核 | 401/403 状态和登录会话写入失败处理 |
| P1-17 经营指标口径 | 已代码整改 | 指标按文档口径和数据范围计算，测试覆盖明细反算 |
| P1-18 OpenAPI 泛化 | 已代码整改 | 具体 schema 契约测试；旧路径和旧分页参数负向测试 |
| P1-19 分页和时间格式 | 已代码整改 | 外部列表请求统一 `page/size`；时间解析统一配置 |
| P1-20 中文 msg 分支 | 已代码整改 | 前端按 HTTP 状态和稳定 code 分支 |
| P1-21 关键约束缺失 | 已代码整改，需真实 MySQL 复核 | 生产/test schema 约束同步，真实数据库验证入口已补 |
| P1-22 审计查询闭环 | 已代码整改 | 登录/操作审计后端、权限、前端页面和 OpenAPI 已补 |

## 4. 尚需真实环境验证

以下不是文档冲突，也不是代码阻塞，而是 H2/单元测试无法等价证明的外部设施语义：

| 项目 | 命令 |
|---|---|
| MySQL/MariaDB Schema 约束 | `cd dealer-server && CRM_REAL_MYSQL_URL=... CRM_REAL_MYSQL_USERNAME=... CRM_REAL_MYSQL_PASSWORD=... ./mvnw -Dtest=ExternalMysqlSchemaVerificationTest test` |
| Redis TTL/删除/Pattern 失效 | `cd dealer-server && CRM_REAL_REDIS_HOST=127.0.0.1 CRM_REAL_REDIS_PORT=6379 ./mvnw -Dtest=ExternalRedisVerificationTest test` |
| 收款/退款/库存/发票并发 | 使用真实 MySQL profile 跑对应 service/integration 并发测试，检查唯一约束、CAS 和事务回滚语义 |

## 5. 当前 P2/P3 技术债

| 项目 | 状态 |
|---|---|
| `TranServiceImpl` 跨领域职责过重 | 交易、资金、票据、库存和交付规则入口已稳定，但服务拆分仍是 P2 结构性技术债，不影响当前旧接口收口 |
| 真实数据量性能 | 列表稳定排序和索引评估已补文档/SQL，仍需真实数据量压测 |
| 历史兼容路径 | 本次已按最新要求删除，不再保留 deprecated 兼容窗口 |
