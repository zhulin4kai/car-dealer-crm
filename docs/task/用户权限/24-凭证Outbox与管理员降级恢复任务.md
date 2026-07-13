# 凭证 Outbox 与管理员降级恢复任务

## 关联业务缺口

- UM-30：同步 Webhook 在数据库提交前取得原始凭证，回滚或超时会产生不可用凭证并长期占用事务锁。
- UM-31：首次初始化后普通管理员全部失效时，恢复账号既不能常规治理也不能重复 bootstrap，系统可能永久锁死。
- UM-32：凭证拒绝审计、三维限流、并发重发冷却和 HUMAN 权威联系方式没有形成失败可追溯且防轰炸的闭环。

## 任务依据

- Spec：`docs/spec/用户权限/01-流程规则与验收规格.md` 的凭证提交后投递、四态门禁和普通管理员降级恢复规则。
- Plan：`docs/plan/用户权限/01-用户权限与数据范围治理方案.md` 的事务 Outbox、幂等投递、nonce 清除和 `DEGRADED` 恢复方案。
- 事务规则：`docs/rule/06-业务一致性与事务规范.md` 禁止事务内不可控网络调用和提交前外部成功通知。

## 前置依赖

- 单向前置固定为 Task 03—20 → Task 22 → Task 23；Task 24 复用已经收口的四态门禁、管理员保护、完整 DIRECT/ACTING 汇报图和固定锁序。
- 完成本任务后只进入 Task 21 全链路验收；完整主链为 Task 03—20 → Task 22 → Task 23 → Task 24 → Task 21，Task 21 不构成本任务的完成前置。

## 目标

1. 业务事务只原子保存 64 字符不可逆预交付承诺与待投递消息，不生成原始凭证，接口返回 HTTP 202/`QUEUED`。
2. 提交后 Worker 使用独立密钥确定性派生原始凭证，以 CAS 将预交付承诺绑定为消费端共用的实际 HMAC 摘要；稳定 `messageId` 支持超时后的幂等重试。
3. 数据库、日志、审计和 API 永不保存或返回原始凭证；Outbox 终态清除 nonce。
4. 初始化门禁完整区分 `UNINITIALIZED`、`PENDING_FIRST_CHANGE`、`READY` 和 `DEGRADED`。
5. 零可用普通管理员时，通过无目标选择的窄命令恢复原有普通管理员入口，不重复首次 bootstrap。

## 允许修改范围

- 后端凭证服务、投递端口、Outbox Worker、门禁、降级恢复服务、Mapper、模型、审计动作和控制器。
- Fresh Schema、H2 Schema、Task 24 迁移和 migration manifest。
- OpenAPI、后端/集成测试、前端受理状态展示与契约测试。

## 禁止修改范围

- 不修改凭证、用户管理、管理员恢复以外的业务模块，也不实现 ERP、财务、人事、SSO 或 MFA 对接。
- 不在业务事务、图锁或数据库锁内派生、返回或投递原始凭证，不恢复同步 Webhook。
- 不允许请求指定降级恢复目标，不借恢复命令新增角色、权限、组织任职或普通账号。
- 不把固定恢复账号的专用联系方式计入普通管理员 `READY`，也不因其个人中心例外开放常规治理、日常业务或普通找回密码。
- 不以删除断言、跳过并发场景、放宽最后管理员保护或仅运行定向测试宣告模块完成。

## 执行步骤

1. 建立 `t_credential_delivery_outbox`：稳定 messageId、credentialId、用户、用途、随机 nonce、联系方式摘要、状态、租约、重试次数、CAS 版本和终态时间。
2. 签发时先做本地渠道和联系方式校验，再由 `messageId`、用途和随机 nonce 计算域分隔的预交付承诺，在同一事务写承诺与 `PENDING` Outbox；签发路径不得调用原始凭证派生，并删除事务内同步 Webhook。
3. Worker 以 CAS 认领到期消息，提交后派生原始凭证，使用与消费端完全相同的 HMAC 摘要算法把预交付承诺 CAS 绑定为实际 `token_digest` 后才允许事务外投递；Webhook 携带 `Idempotency-Key`，超时和 5xx 使用同一消息重试。已绑定同一摘要视为幂等恢复，摘要不匹配时 fail-close。
4. 成功后独立事务写 `DELIVERED`、清除 nonce 和成功审计；永久 4xx 或达到最大次数时撤销未消费凭证、写 `FAILED`、清除 nonce 和失败审计。
5. 可用管理员计数加入 `must_change_password=0` 和至少一个当前已验证员工联系方式；待验证当前恢复渠道的管理员仍属于 `PENDING_FIRST_CHANGE`，可访问本人资料和联系方式验证窄入口。
6. 增加 `/api/recovery/admin-access`：仅固定恢复账号、仅 PENDING/DEGRADED、再次校验外部恢复密钥；请求不得携带目标 ID。
7. 降级恢复按 `AUTHORIZATION_MEMBERSHIP_GUARD → ORGANIZATION_HIERARCHY → REPORTING_GRAPH → AVAILABLE_ADMIN_GUARD` 加锁，只选择保留有效 admin 角色、有效任职和至少一个当前员工联系方式的普通 HUMAN 账号；无联系方式候选必须跳过。
8. 同步 OpenAPI 202 契约、Spec/Plan/Task、前端状态和数据库迁移。
9. 凭证拒绝、限流、错误恢复密钥和门禁拒绝使用独立事务写脱敏安全审计，业务事务回滚不得抹除失败事实。
10. HUMAN 投递只比对员工档案当前联系方式；签发事务使用用户行锁和最近签发事实实现用途族冷却，防止并发重签和投递失败后立即轰炸。
11. 本人资料和受管资料共用 `AVAILABLE_ADMIN_GUARD`，禁止并发替换或清空最后一个可恢复管理员的唯一已验证渠道；`INVITED` 降级候选在重发前以 CAS 清理锁定、过期和失败计数但保持邀请状态。
12. 固定恢复账号在四态中只保留本人非授权资料和独立专用联系方式的个人中心例外；该入口不得修改账号身份、角色、权限、组织、任职或普通管理员就绪事实。

## 自动验证

- 业务事务强制回滚：凭证表和 Outbox 均无记录，外部投递次数为零。
- 提交成功：只出现预交付承诺和 `PENDING`，业务签发路径没有调用原始凭证派生。
- Worker 首次处理把承诺 CAS 绑定为消费端可查询的实际摘要；重试和租约恢复接受已绑定同一摘要，篡改承诺或摘要时撤销凭证并且外部投递次数为零。
- Worker 崩溃、租约过期、接收后超时、重复 messageId、永久失败和最大重试。
- `PENDING/PROCESSING/RETRY` 必须有 nonce，`DELIVERED/FAILED` 必须无 nonce。
- 首个管理员邀请、激活、首次改密、当前联系方式验证的状态迁移；唯一管理员密码到期后的 DEGRADED 恢复。
- 降级恢复不能指定目标、不能新增角色或任职，错误恢复密钥和非固定恢复账号均拒绝。
- 第一候选无联系方式而后续候选可投递时必须跳过；全部候选不可投递时写独立失败审计且无安全状态、会话、凭证或 Outbox 残留。

### 验证矩阵

| 业务不变量 | 自动化证据 |
| --- | --- |
| 业务签发不派生原始凭证，回滚时无凭证、Outbox 或外部投递 | `CredentialServiceImplTest`、`CredentialOutboxIntegrationTest` |
| Worker 首次 CAS 绑定实际摘要；崩溃、租约恢复和重试稳定；篡改承诺不投递 | `CredentialDeliveryOutboxWorkerTest`、`CredentialOutboxIntegrationTest` |
| 四态门禁、固定恢复账号个人中心例外及日常业务隔离 | `UserManagementAccessGateTest`、`UserControllerH2IntegrationTest` |
| 降级恢复不能指定目标，错误身份/密钥/状态失败且无部分事实 | `AdminAccessRecoveryServiceTest`、`ManagedUserAccountSecurityIntegrationTest` |
| 最后恢复渠道、最后管理员和并发授权变化不能被清空 | `AvailableAdminAuthorizationConcurrencyTest`、`ManagedUserAccountSecurityIntegrationTest` |
| 限流、错误密钥、门禁拒绝和失败回滚仍写脱敏安全审计 | `SecurityFailureAuditIntegrationTest`、`GlobalExceptionHandlerTest` |
| Schema、迁移顺序、摘要长度和 Outbox 状态约束一致 | `SchemaConstraintsTest`、`UserManagementMigrationRunnerContractTest` |

## 验证命令

```bash
cd dealer-server
./mvnw -q -Dtest=CredentialServiceImplTest,CredentialDeliveryOutboxWorkerTest,CredentialOutboxIntegrationTest,AdminAccessRecoveryServiceTest,UserManagementAccessGateTest,UserControllerH2IntegrationTest,ManagedUserAccountSecurityIntegrationTest,AvailableAdminAuthorizationConcurrencyTest,SecurityFailureAuditIntegrationTest,GlobalExceptionHandlerTest,SchemaConstraintsTest,UserManagementMigrationRunnerContractTest test
```

- `cd dealer-web && npm test -- tests/unit/modules/credential-api.test.ts tests/unit/modules/credential-pages.test.ts tests/unit/modules/profile-page.test.ts tests/unit/modules/user-credential-profile-openapi-contract.test.ts tests/unit/modules/user-management-openapi-error-contract.test.ts`
- `cd dealer-web && npm run check`
- `scripts/database/test-user-management-migrations-real.sh`，分别验证受支持的 MySQL 与 MariaDB。
- `git diff --check`

## 手工验收

1. 配置受信回环 Webhook、Bearer Token、独立派生密钥和 break-glass 密钥。
2. 创建待激活管理员，确认 HTTP 202/QUEUED，数据库事务提交前 Webhook 未收到消息，提交后收到带稳定 messageId 的凭证。
3. 模拟通知方已接收但客户端超时，确认重试 messageId 和原始凭证一致且通知方只发送一次。
4. 完成激活但不首次改密，确认日常业务仍被 641 阻断；首次改密但未验证联系方式时仍保持 PENDING，验证至少一个当前员工渠道后进入 READY。
5. 使唯一普通管理员密码到期，确认进入 DEGRADED；恢复账号只能调用 `/api/recovery/admin-access`，恢复后目标必须完成重置、首次改密并验证至少一个当前员工联系方式才能回到 READY。
6. 在四态中使用固定恢复账号进入个人中心，确认只能维护本人非授权资料和专用联系方式；普通管理员 `READY` 计数不变化，常规用户治理和日常业务仍被拒绝。

## 完成标准

- 不存在事务内外部凭证投递。
- 不存在外部已经收到但数据库回滚导致凭证永久无效的窗口。
- 不存在首次初始化后零管理员无法恢复的死锁。
- Task 24 的定向自动化、真实 MySQL/MariaDB 场景和人工场景全部通过；模块级后端全量、前端全量和全链路联调由后续 Task 21 统一执行并宣告。
