# Car Dealer CRM 后端业务逻辑文档

> 基于 Spring Boot + Spring Security + MyBatis + Redis 的汽车经销商CRM系统

---

## 账号凭证与个人资料边界

- `t_account_credential` 只保存 HMAC 摘要、用途、状态、到期和 CAS 版本；原始凭证只进入 `CredentialDeliveryPort`。
- 生产适配器只把原始凭证投递给通过 `CREDENTIAL_DELIVERY_WEBHOOK_URL` 和至少 32 字节 `CREDENTIAL_DELIVERY_BEARER_TOKEN` 显式配置的受信 HTTPS 通知服务；未配置、无联系方式或通知服务未接受时撤销本次凭证并以 625/HTTP 503 使管理命令整体回滚，不以 `accepted=false` 提交半完成状态。明文 HTTP 只在显式开启 `CREDENTIAL_DELIVERY_ALLOW_INSECURE_LOOPBACK` 且目标解析为回环地址时用于人工联调；`dev/test` 捕获器仅存在于对应 Profile，且没有 HTTP 读取入口。
- 忘记密码、联系方式验证、邀请重签、凭证消费和 break-glass 使用 Redis 事务计数器按账号或凭证摘要与来源地址分层限流。Redis 不可用时安全命令 fail-close；忘记密码仍返回同形 `QUEUED`，其他命令使用稳定错误码 `628`/HTTP 429，审计只保存限流范围和不可逆资源摘要。
- `t_password_history` 是不可变历史；激活、找回、本人改密、首次改密和管理员重置共用同一密码策略与近期历史检查。
- `t_login_identifier` 保存登录账号的永久归属事实；账号改名只退休旧标识，旧标识不得转给其他用户，但原用户可以按版本重新启用自己的退休标识。邀请创建和账号改名都必须先锁定 `LOGIN_IDENTIFIER_GUARD`，再检查当前表和历史表，避免并发 check-then-insert。
- `t_user.account_expires_at` 表达账号到期时间，`password_expires_at` 表达凭证到期时间；二者独立参与认证、普通管理员可用性和生命周期资格判断，不能再用一个时间字段同时表示两种语义。
- 人工锁定与登录失败自动锁定是两个独立事实；自动到期不得清除人工锁定。
- `t_employee` 是姓名、电话、邮箱和头像的资料权威；`t_user` 同事务维护兼容投影，`profile_version` 与员工生命周期 `version` 分离。
- `/api/profile` 只接受资料白名单，当前用户 ID 只能来自服务端登录态，不能提交授权字段。
- `/api/users/{id}/profile` 只允许有管理链和组织范围的上级修改姓名、电话、邮箱；目标 ID 来自路径，DTO 拒绝角色、权限、组织、岗位、账号状态等未知字段，并使用同一 `profile_version` CAS。
- 激活、重置和自动锁定等匿名安全流程通过匿名审计入口写入操作日志，不读取不存在的当前登录用户，也不保存原始凭证。

## 目录

- [1. 项目架构总览](#1-项目架构总览)
- [2. 认证授权模块](#2-认证授权模块)
- [3. 用户管理模块](#3-用户管理模块)
- [4. 线索管理模块](#4-线索管理模块)
- [5. 客户管理模块](#5-客户管理模块)
- [6. 交易管理模块](#6-交易管理模块)
- [7. 报价订单模块](#7-报价订单模块)
- [8. 市场活动模块](#8-市场活动模块)
- [9. 商品管理模块](#9-商品管理模块)
- [10. 字典管理模块](#10-字典管理模块)
- [11. 统计报表模块](#11-统计报表模块)
- [12. 交付管理模块](#12-交付管理模块)
- [13. 商机管理模块](#13-商机管理模块)
- [14. 试驾管理模块](#14-试驾管理模块)
- [15. 跟进任务模块](#15-跟进任务模块)
- [16. 审计日志模块](#16-审计日志模块)
- [17. 安全配置](#17-安全配置)
- [18. AOP 切面](#18-aop-切面)
- [19. 工具类](#19-工具类)
- [20. 数据字典缓存机制](#20-数据字典缓存机制)
- [21. Mapper XML SQL 汇总](#21-mapper-xml-sql-汇总)
- [22. 数据库表汇总](#22-数据库表汇总)

---

## 1. 项目架构总览

```
dealer-server/src/main/java/com/autodealer/crm/
├── bootstrap/                       # 应用入口、Spring 装配与 Security 组合根
├── shared/                          # 不依赖业务模块的稳定通用能力
│   ├── error/
│   ├── web/
│   ├── security/
│   ├── pagination/
│   └── infrastructure/
└── modules/
    ├── identity/
    ├── sales/
    │   ├── activity/
    │   ├── lead/
    │   ├── customer/
    │   ├── opportunity/
    │   ├── followup/
    │   └── testdrive/
    ├── commerce/
    │   ├── catalog/
    │   ├── promotion/
    │   ├── inventory/
    │   └── quote/
    ├── fulfillment/
    │   ├── transaction/
    │   ├── payment/
    │   ├── invoice/
    │   └── delivery/
    ├── analytics/
    ├── dictionary/
    ├── audit/
    └── ai/
```

每个业务模块按实际需要创建 `web`、`application.api`、`application.internal`、
`domain` 和 `persistence`。跨模块只能依赖目标模块的 `application.api`；公开数据模型和
数据端口分别位于 `application.api.model` 与 `application.api.port`，MyBatis Mapper
保留在所属模块的 `persistence.mapper` 并实现对应端口。`shared` 禁止反向依赖业务模块。
Mapper XML 按模块递归放置在 `src/main/resources/mapper/**`。

---

## AI 业务助手模块

AI 业务助手后端由 Spring Boot 作为业务控制面，负责 Conversation、Run、Message、Provider 配置、全局运行策略、ToolRegistry、Proposal、Workflow、主动提醒、追踪和审计。

- Conversation 是多轮对话容器，绑定用户，可选绑定业务对象。
- Run 是 Conversation 中的一次执行，负责模型调用、工具调用、SSE 和状态流转。
- Message 同时归属于 Conversation 和 Run，并通过不可变修订、上下文纳入标记和版本号支持编辑与撤回。
- Spring Boot 调用 `dealer-ai` 时下发 `conversationNo`、脱敏 `conversationSummary`、策略允许的最近 1 到 8 条活动 `messageHistory`、工具权限交集、运行策略和 Provider runtime config。
- Provider runtime config 只用于服务间请求，不进入 Conversation、Message、SSE、trace 或日志。
- 归档 Conversation 只隐藏用户侧列表，不删除底层 Run、Message、ToolCall、Proposal、Workflow、Approval 或 ExecutionEvent。
- 同一 Run 通过 CAS 只启动一次；脱敏事件写入 `t_ai_run_event`，SSE 重新订阅按 `afterSequence` 重放而不重复执行。

---

## 2. 认证授权模块

### 2.1 模块概述
基于 Spring Security + JWT + Redis 的无状态认证授权体系。

### 2.2 文件路径

| 层级 | 文件路径 |
|------|----------|
| Config | `bootstrap/security/SecurityConfig.java` |
| Filter | `bootstrap/security/TokenVerifyFilter.java` |
| Handler | `bootstrap/security/MyAuthenticationSuccessHandler.java` |
| Handler | `bootstrap/security/MyAuthenticationFailureHandler.java` |
| Handler | `bootstrap/security/MyLogoutSuccessHandler.java` |
| Handler | `bootstrap/security/MyAccessDeniedHandler.java` |
| Handler | `shared/web/GlobalExceptionHandler.java` |
| Model | `modules/identity/application/api/model/TUser.java`, `modules/identity/persistence/model/TLoginIdentifier.java`, `modules/identity/persistence/model/TUserSession.java`, `modules/identity/persistence/model/TRole.java`, `modules/identity/persistence/model/TPermission.java`, `modules/identity/persistence/model/TUserRole.java`, `modules/identity/persistence/model/TRolePermission.java` |
| Service | `modules/identity/application/api/UserService.java`, `modules/identity/application/api/UserSessionService.java` |
| Mapper | `modules/identity/persistence/mapper/TUserMapper.java`, `modules/identity/persistence/mapper/TLoginIdentifierMapper.java`, `modules/identity/persistence/mapper/TUserSessionMapper.java`, `modules/identity/persistence/mapper/TRoleMapper.java`, `modules/identity/persistence/mapper/TPermissionMapper.java`, `modules/identity/persistence/mapper/TUserRoleMapper.java`, `modules/identity/persistence/mapper/TRolePermissionMapper.java` |
| XML | `resources/mapper/identity/TUserMapper.xml`, `resources/mapper/identity/TLoginIdentifierMapper.xml`, `resources/mapper/identity/TRoleMapper.xml`, `resources/mapper/identity/TPermissionMapper.xml`, `resources/mapper/identity/TUserRoleMapper.xml`, `resources/mapper/identity/TRolePermissionMapper.xml` |

### 2.3 认证流程

#### 登录流程
```
1. 前端 POST /api/login (loginAct, loginPwd, rememberMe)
2. SecurityConfig 配置 formLogin 拦截 /api/login
3. Spring Security 调用 UserServiceImpl.loadUserByUsername()
   → TUserMapper.selectByLoginAct() 查询用户
   → TRoleMapper.selectByUserId() 查询角色
   → TPermissionMapper.selectMenuPermissionByUserId() 查询菜单权限
   → TPermissionMapper.selectButtonPermissionByUserId() 查询按钮权限
4. 密码校验（BCryptPasswordEncoder）
5. 登录成功 → MyAuthenticationSuccessHandler
   → UserSessionService 生成不可猜测 sessionId，并在 t_user_session 写入独立会话事实
   → JWTUtils.createSessionJWT() 只生成 userId、sessionId、authVersion、iat、exp 声明
   → RedisManager.set() 按 `cdrm:session:{sessionId}` 存储 JWT 的 HMAC 摘要，并维护 `cdrm:user:sessions:{userId}` 索引
   → rememberMe=true 使用 7 天绝对期限/24 小时空闲期限，否则使用 4 小时绝对期限/30 分钟空闲期限
   → 数据库、Redis 和登录审计全部成功后返回 JWT；每用户最多保留 5 个活动会话，超限确定性撤销最旧会话
6. 会话事实或 Redis 写入失败 → 返回系统错误且不返回 JWT
7. 登录审计写入失败 → 撤销刚创建的会话，不返回 JWT
8. 登录失败 → MyAuthenticationFailureHandler → 写入失败登录记录，返回 HTTP 401 和稳定错误码
```

#### Token 验证流程（TokenVerifyFilter）
```
1. 请求进入 Filter
2. 判断是否为 /api/login 请求 → 放行
3. 从 `Authorization: Bearer <token>` 请求头读取 JWT，不接受 URL 参数或裸 token
4. 请求头缺失或 Bearer 后 token 为空 → 返回 HTTP 401 和 TOKEN_IS_EMPTY
5. JWTUtils.verifyJWT() 验证签名失败 → 返回 HTTP 401 和 TOKEN_IS_ERROR
6. JWTUtils 解析 userId、sessionId、authVersion 和签发时间；缺少 sessionId 的旧 JWT 只有在显式兼容截止时间前才走旧精确键校验，默认禁用
7. `cdrm:session:{sessionId}` 中的摘要必须与请求 Token 的 HMAC 摘要一致
8. t_user_session 必须属于该用户、未撤销、未超过空闲/绝对期限且签发版本一致
9. 根据 userId 重新加载数据库用户，分别校验 `account_expires_at` 账号期限与 `password_expires_at` 凭证期限，并校验启用和锁定状态
10. JWT authVersion 必须与数据库权威版本一致
11. 校验通过后按节流窗口更新最后活动时间和 Redis TTL；任一基础设施校验异常按 401 失败关闭
12. 全部通过后设置 SecurityContext，并把当前 sessionId 放入认证详情
```

#### 退出流程
```
1. 前端 POST /api/logout
2. MyLogoutSuccessHandler
   → 只撤销当前认证详情中的 sessionId，不递增 authVersion
   → 数据库撤销事实提交后精确删除该会话 Redis Key 和用户会话索引
   → 其他设备会话保持有效；Redis 清理失败返回稳定会话缓存错误，数据库撤销事实仍保证当前 Token 失效
```

### 2.4 权限控制

使用 `@PreAuthorize` 注解进行方法级别的权限控制：

| 接口 | 权限标识符 |
|------|-----------|
| GET /api/clues | `clue:list` |
| POST /api/importExcel | `clue:import` |
| POST /api/clue | `clue:add` |
| GET /api/clue/detail/{id} | `clue:view` |
| PUT /api/clue | `clue:edit` |
| PUT /api/clue/{id}/owner | `clue:transfer` |
| GET /api/clue/{id}/owner-history | `clue:view` |
| PUT /api/clue/{id}/close | `clue:close` |
| PUT /api/clue/{id}/restore | `clue:restore` |
| DELETE /api/clue/{id} | `clue:delete` |
| POST /api/clue/batch | `clue:delete` |
| GET /api/users | `user:list` |
| GET /api/users/{id} | `user:view` + 目标管理范围 |
| POST /api/users | `user:add` + 委派上限 |
| PUT /api/users/{id}/profile | `user:edit` + 目标管理范围 |
| POST /api/users/{id}/status | `user:status` + 目标管理范围 |
| GET /api/users/{id}/authorization | `user:view` + 目标管理范围 |
| PUT /api/users/{id}/authorization/roles | `user:role` + 管理链与委派上限 |
| PUT /api/users/{id}/authorization/permissions | `user:permission` + 管理链与委派上限 |
| GET /api/users/{id}/history | `audit:operation:detail` + 目标管理范围 |
| POST /api/users/{id}/lifecycle/* | `user:status`，调岗/返聘另需任职权限及各责任域动作权限 |
| POST /api/users/{id}/password-reset | `user:password` + 目标管理范围 |
| GET /api/audit/login-logs | `audit:login:list` |
| GET /api/audit/login-logs/{id} | `audit:login:detail` |
| GET /api/audit/login-logs/export | `audit:login:export` |
| GET /api/audit/operation-logs | `audit:operation:list` |
| GET /api/audit/operation-logs/{id} | `audit:operation:detail` |
| GET /api/audit/operation-logs/export | `audit:operation:export` |
| GET /api/deliveries | `delivery:list` |
| POST /api/deliveries | `delivery:create` |
| GET /api/deliveries/{id} | `delivery:view` |
| GET /api/deliveries/{id}/check-items | `delivery:view` |
| GET /api/deliveries/tran/{tranId} | `delivery:view` |
| PUT /api/deliveries/check-items/{itemId} | `delivery:check` |
| POST /api/deliveries/{id}/sign | `delivery:sign` |
| POST /api/deliveries/{id}/exception | `delivery:exception` |
| POST /api/deliveries/{id}/cancel | `delivery:cancel` |
| GET /api/follow-tasks | `follow-task:list` |
| POST /api/follow-tasks | `follow-task:create` |
| GET /api/follow-tasks/{id} | `follow-task:view` |
| PUT /api/follow-tasks/{id}/start | `follow-task:update` |
| PUT /api/follow-tasks/{id}/postpone | `follow-task:update` |
| PUT /api/follow-tasks/{id}/cancel | `follow-task:cancel` |
| PUT /api/follow-tasks/{id}/complete | `follow-task:complete` |
| GET /api/communication-records | `communication-record:list` |
| POST /api/communication-records | `communication-record:create` |
| PUT /api/communication-records/{id}/correct | `communication-record:correct` |
| PUT /api/communication-records/{id}/void | `communication-record:void` |
| GET /api/dict/clear | `admin` |

---

## 3. 用户管理模块

### 3.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/identity/web/UserController.java`, `modules/identity/web/UserAuthorizationController.java`, `modules/identity/web/OrganizationController.java`, `modules/identity/web/RoleAccessController.java`, `modules/identity/web/ProfileController.java`, `modules/identity/web/CredentialController.java`, `modules/identity/web/UserSessionController.java`, `modules/identity/web/UserHistoryController.java`, `modules/identity/web/UserLifecycleController.java` |
| 受管账号 | `modules/identity/application/api/ManagedUserAccountService.java` → `modules/identity/application/internal/ManagedUserAccountServiceImpl.java`, `modules/identity/application/api/ManagedUserInvitationService.java` → `modules/identity/application/internal/ManagedUserInvitationServiceImpl.java` |
| 授权与角色 | `modules/identity/application/api/AuthorizationService.java` → `modules/identity/application/internal/AuthorizationServiceImpl.java`, `modules/identity/application/api/RoleAccessService.java` → `modules/identity/application/internal/RoleAccessServiceImpl.java`, `modules/identity/application/internal/UserAuthorizationPolicy.java` |
| 组织与生命周期 | `modules/identity/application/api/OrganizationService.java` → `modules/identity/application/internal/OrganizationServiceImpl.java`, `modules/identity/application/api/UserLifecycleService.java` → `modules/identity/application/internal/UserLifecycleServiceImpl.java` |
| 个人资料、凭证、会话与历史 | `modules/identity/application/api/ProfileService.java`, `modules/identity/application/api/CredentialService.java`, `modules/identity/application/api/UserSessionService.java`, `modules/identity/application/api/UserHistoryService.java` 及其 `impl` 实现 |
| 根命令观测 | `modules/identity/application/api/command/UserManagementCommand.java`, `modules/identity/application/internal/UserManagementCommandAspect.java` |
| 安全副作用 | `modules/identity/application/internal/UserSecurityMutationCoordinator.java`, `modules/identity/application/internal/OwnerCandidateCacheInvalidator.java` |
| 认证兼容 Service | `modules/identity/application/api/UserService.java` → `modules/identity/application/internal/UserServiceImpl.java`；保留登录读取和负责人候选能力，缺少版本、原因、管理边界的旧写入口统一 fail-close |
| Mapper | `TUserMapper`, `TLoginIdentifierMapper`, `TEmployeeMapper`, `TEmployeeAssignmentMapper`, `TEmployeeReportingMapper`, `TOrganizationUnitMapper`, `TPositionMapper`, `TRoleMapper`, `TRolePermissionMapper`, `TRoleOrganizationMapper`, `TUserRoleMapper`, `TUserPermissionMapper`, `TUserSessionMapper`, `TAccountCredentialMapper`, `TAuthorizationHistoryMapper`, `TUserLifecycleMapper` |
| Model/DTO | `TUser`, `TLoginIdentifier`, `TEmployee`, `TEmployeeAssignment`, `TEmployeeReporting`, `TOrganizationUnit`, `TPosition`, `TRole`, `TPermission`, `TUserRole`, `TUserPermission`, `TUserSession`, `TAccountCredential` 及 `dto/user/`, `dto/access/`, `dto/organization/`, `dto/profile/`, `dto/credential/` |

### 3.2 接口方法及业务流程

#### 命令上下文与安全副作用边界
- `@UserManagementCommand` 目前只用于低风险的本人资料更新试点。切面识别一次根调用并输出一条脱敏完成观测；嵌套标注调用不重复记录，观测不改变业务返回或异常。
- 切面不得承载权限判断、事务、锁、CAS、状态机、Mapper、历史或审计写入。上述规则继续由具体 Service 和显式领域策略执行，调用点可以直接定位。
- `UserSecurityMutationCoordinator` 只统一三类重复副作用：访问事实变化撤销会话并失效负责人候选缓存，认证事实变化只撤销会话，姓名/岗位等负责人资格变化只失效候选缓存。所有入口要求已有事务，实际 Redis 清理由 `UserSessionServiceImpl` 在数据库提交后执行。
- 不建立通用事件总线或可变安全计划对象；当前两个集中边界已经覆盖真实重复，继续抽象只会增加代码和隐藏安全语义。

#### 获取登录人信息
- **接口**: `GET /api/login/info`
- **流程**: 从 SecurityContext 取得当前账号，再聚合个人资料、角色和当前有效权限；本人普通资料通过 `ProfileService` 维护，不能提交组织、任职、角色或权限字段。

#### 免登录检测
- **接口**: `GET /api/login/free`
- **流程**: 该路径仍受认证过滤链保护，只用于确认当前 Token 和数据库会话事实仍有效。

#### 用户管理工作台
- **接口**: `GET /api/users?page=1&size=10`
- **权限**: `@PreAuthorize("hasAuthority('user:list')")`
- **流程**: `UserController.userPage()` → `ManagedUserAccountServiceImpl.list()` → `DataScopeResolver` → `TUserMapper.selectManagedUserPage()`。
- **规则**: 关键词、组织、岗位、直属管理者、角色、任职状态、账号状态和锁定状态均在 SQL 前确定范围；排序字段使用服务端白名单并追加用户主键。摘要不返回密码、凭证、手机号或邮箱。
- **辅助接口**: `GET /api/users/filter-options` 返回当前操作者可见筛选事实和创建用户时的可委派角色候选，两者不得混用。

#### 用户详情
- **接口**: `GET /api/users/{id}`；`GET /api/user/{id}` 仅作 deprecated 只读兼容
- **权限**: `@PreAuthorize("hasAuthority('user:view')")`
- **流程**: `ManagedUserAccountServiceImpl.getDetail()` 聚合账号、资料、员工、主要任职、直属管理者、角色摘要和对象级可执行动作。
- **版本**: 响应分别返回账号、资料、员工、授权和会话版本；联系方式及锁定原因还要求 `user:sensitive:view` 与目标管理关系。

#### 邀请创建与受管资料
- **接口**: `POST /api/users`、`PUT /api/users/{id}/profile`
- **权限**: 邀请创建使用 `user:add`，受管资料更新使用 `user:edit`；二者都继续执行目标对象级管理范围校验。
- **流程**: `ManagedUserInvitationServiceImpl.create()` 先锁定 `LOGIN_IDENTIFIER_GUARD` 并排除当前及历史账号冲突，再在同一事务创建账号、永久登录标识、员工、主任职、直属汇报、初始角色、授权历史、操作审计和单次邀请凭证；响应不返回密码、Token 或摘要。受管资料更新只接受姓名、电话、邮箱白名单和 `profileVersion`，不能改变本人或目标授权。

#### 登录账号与安全到期
- **接口**: `PUT /api/users/{id}/login-account`、`PUT /api/users/{id}/security-expiration`
- **权限与版本**: 登录账号使用 `user:edit`，安全到期使用 `user:status`；两者都要求目标管理范围、最新 `accountVersion` 和必填原因，任何用户不能借此修改本人或受保护账号。
- **永久归属**: 账号改名持有 `LOGIN_IDENTIFIER_GUARD`，按稳定顺序锁定新旧标识；旧标识改为 `RETIRED`，新标识只能新建或由同一用户重新启用。全局唯一约束使退休标识不能转给其他员工。
- **独立期限**: 请求分别提交 `accountExpiresAt` 与 `credentialExpiresAt`，后者映射数据库 `password_expires_at`。空值表示无对应到期时间；过去或到期时间立即使相应可用状态失效。不能使最后一个有效普通管理员的账号或凭证到期。
- **会话与审计**: 两类命令成功后都提升账号安全版本、撤销目标全部活动会话并记录前后状态；数据库提交后的旧 Token 不因 Redis 清理失败而恢复。

#### 账号启禁用与锁定
- **接口**: `POST /api/users/{id}/status`
- **权限**: `@PreAuthorize("hasAuthority('user:status')")`
- **流程**: `ManagedUserAccountServiceImpl.changeStatus()` 只接受 `ENABLE`、`DISABLE`、`LOCK`、`UNLOCK`、账号版本和必填原因。本人、同级、上级、跨范围及受保护目标被对象级策略拒绝。
- **并发与会话**: 管理员降级操作先锁定 `AVAILABLE_ADMIN_GUARD` 并重新计算可用管理员；成功写入账号安全事实和审计并提升数据库权威版本，提交后清理 Redis。Redis 失败不能让旧 Token 恢复有效。

#### 角色矩阵与个人授权
- **接口**: `/api/roles`、`/api/roles/{id}/permissions`、`/api/users/{id}/authorization/roles`、`/api/users/{id}/authorization/permissions`
- **流程**: `RoleAccessServiceImpl` 管理角色目录、适用组织、矩阵预览和版本化保存；`AuthorizationServiceImpl` 管理用户角色事实以及个人 `GRANT`/`DENY`、有效期和数据范围。
- **边界**: 任何人都不能修改自己的授权。普通管理者还必须覆盖目标管理链和组织范围，只能委派自己当前拥有、允许委派且不超过目标范围的角色和权限；有效权限按角色权限与个人 `GRANT` 合并后应用个人 `DENY`。

#### 调岗、离职、交接与返聘
- **接口**: `/api/users/{userId}/lifecycle` 下的 `transfer`、`departure/precheck`、`departure/start`、`departure/handover`、`departure/complete`、`rehire`
- **权限**: `@PreAuthorize("hasAuthority('user:status')")`
- **流程**: `UserLifecycleServiceImpl` 使用一次性离职预检快照、精确事实指纹、数据库图锁和事务状态机编排任职、汇报、授权、会话及责任交接。
- **直接责任域**: 只直接转移 Activity、Clue、Customer、Opportunity、FollowTask、TestDrive 六域的当前负责人；每项按预检时相同状态谓词、ID、状态和版本逐项 CAS 更新。
- **派生与历史**: Quote、Tran 只随 Customer 归属做派生计数和核验，不直接改负责人；待审批队列和 CommunicationRecord 历史负责人不得改写。试驾交接还需持有 `TEST_DRIVE_SCHEDULE_GUARD` 并重新检查接收人排期。
- **闭环**: 离职先进入 `HANDOVER`，完成全部交接后才能关闭当前及未来任职、汇报、角色和个人权限，禁用账号并提升安全版本；返聘创建新任职事实，不恢复旧授权。

#### 个人资料、凭证与会话
- **接口**: `/api/profile`、`/api/credentials/*`、`/api/me/sessions`、`/api/users/{userId}/sessions`
- **规则**: 本人可维护普通资料、修改密码和撤销本人会话，但不能调整自己的角色、权限、数据范围或任职。管理员凭证重置、下属会话管理仍需目标管理范围，单次凭证只保存 HMAC 摘要。

#### 用户历史
- **接口**: `GET /api/users/{id}/history`
- **流程**: `UserHistoryServiceImpl` 聚合 `t_authorization_history`、白名单 `t_operation_log` 和 `t_user_lifecycle_event` 三个来源，按发生时间、来源和主键稳定排序后过滤分页；操作白名单包含会话创建、联系方式验证、自动锁阻断和旧责任交接，动作筛选只接受真实可产生的主体/变化组合。
- **安全投影**: 要求 `audit:operation:detail` 和目标管理范围；响应使用事件时快照，不读取操作日志原始 detail 或 IP，并清理密码、哈希、digest、Token、Credential、Key、Signature、Salt、Nonce、完整联系方式和网络地址。限流主体摘要和恢复账号 break-glass 留在全局高危审计，不伪装成普通用户时间线。

#### 旧写入口
- `POST /api/user`、`PUT /api/user`、旧单数状态、角色、密码和三域交接路径缺少完整版本、原因、管理链或委派上限，当前统一 fail-close；不得再以 `UserServiceImpl` 作为用户域写入所有者。

#### 获取负责人列表
- **接口**: `GET /api/owner`
- **流程**: `UserController.owner()` → `UserServiceImpl.getOwnerList()`
  - **Redis 缓存**: key=`cdrm:user:owner`，单 value 存储负责人列表并设置 300 秒 TTL
  - 未命中时查询启用、未锁定且具备销售负责人资格的账号；账号状态或角色变化后删除缓存，失败时记录并重试

### 3.3 涉及数据库表
- 账号与员工：`t_user`、`t_login_identifier`、`t_employee`、`t_employee_assignment`、`t_employee_reporting`。
- 组织与岗位：`t_organization_unit`、`t_position`。
- 角色与授权：`t_role`、`t_permission`、`t_role_permission`、`t_role_organization`、`t_role_permission_organization`、`t_user_role`、`t_user_permission`、`t_user_permission_organization`、`t_authorization_history`、`t_authorization_graph_lock`。
- 凭证与会话：`t_account_credential`、`t_password_history`、`t_user_session`；登录标识不属于一次性凭证，永久保存在 `t_login_identifier`。
- 生命周期与历史：`t_user_lifecycle_snapshot`、`t_user_lifecycle_event`、`t_operation_log`。
- 迁移治理：`t_user_management_migration`、`t_user_management_migration_step`。

---

## 4. 线索管理模块

### 4.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/lead/web/ClueController.java`, `modules/sales/lead/web/ClueRemarkController.java` |
| Service | `modules/sales/lead/application/api/ClueService.java` → `modules/sales/lead/application/internal/ClueServiceImpl.java`, `modules/sales/lead/application/api/ClueRemarkService.java` → `modules/sales/lead/application/internal/ClueRemarkServiceImpl.java` |
| Manager | `modules/sales/customer/application/internal/CustomerManager.java`（线索转客户） |
| Mapper | `modules/sales/lead/persistence/mapper/TClueMapper.java`, `modules/sales/lead/persistence/mapper/TClueRemarkMapper.java`, `modules/sales/lead/persistence/mapper/TClueOwnerHistoryMapper.java` |
| XML | `resources/mapper/sales/lead/TClueMapper.xml`, `resources/mapper/sales/lead/TClueRemarkMapper.xml`, `resources/mapper/sales/lead/TClueOwnerHistoryMapper.xml` |
| Model | `modules/sales/lead/application/api/model/TClue.java`, `modules/sales/lead/application/api/model/TClueRemark.java`, `modules/sales/lead/application/api/model/TClueOwnerHistory.java` |
| Query | `modules/sales/lead/application/api/query/ClueQuery.java`, `modules/sales/lead/application/api/query/ClueRemarkQuery.java` |
| Excel | `modules/sales/lead/application/api/result/ClueExcelRaw.java`, `modules/sales/lead/application/internal/ClueImportValidator.java`, `modules/sales/lead/application/api/dto/ImportResult.java` |

### 4.2 接口方法及业务流程

#### 线索列表分页查询
- **接口**: `GET /api/clues?page=1&size=10`
- **权限**: `@PreAuthorize("hasAuthority('clue:list')")`
- **流程**: `ClueController.cluePage()` → `ClueServiceImpl.getClueByPage()` → `TClueMapper.selectClueByPage()`
- **SQL**: 多表 LEFT JOIN 关联查询用户、活动、字典值、产品

#### Excel 导入线索
- **接口**: `POST /api/importExcel`
- **权限**: `@PreAuthorize("hasAuthority('clue:import')")`
- **流程**: `ClueController.importExcel()` → `ClueServiceImpl.importExcel()`
  - EasyExcel 读取 `ClueExcelRaw` 到内存列表。
  - `ClueImportValidator` 逐行校验并转换，手机号会去除空格、横杠和括号后校验大陆手机号格式。
  - 同一文件中归一化后手机号重复会进入行级错误。
  - 写库前再次调用 `TClueMapper.selectExistingPhones()` 检查数据库重复。
  - 导入允许部分成功；格式错误或重复行保留行级错误，可导入行继续通过 `TClueMapper.saveClue()` 批量插入。
  - 已插入行写入初始责任历史，并记录 `CLUE_IMPORT` 审计动作。

#### 手机号查重
- **接口**: `GET /api/clue/{phone}`
- **流程**: `ClueController.checkPhone()` → `ClueServiceImpl.checkPhone()` → `TClueMapper.selectByCount()`
- **规则**: 服务端先归一化常见分隔符，再查询 `t_clue.phone`。

#### 新增线索
- **接口**: `POST /api/clue`
- **权限**: `@PreAuthorize("hasAuthority('clue:add')")`
- **流程**: `ClueController.addClue()` → `ClueServiceImpl.saveClue()`
  - 手机号先归一化常见分隔符，再按规范手机号查重。
  - 生产和测试 Schema 均通过 `uk_clue_phone` 唯一约束兜底。
  - `BeanUtils.copyProperties()` 复制属性
  - `CurrentUserProvider` 解析创建人和负责人
  - `TClueMapper.insertSelective()` 插入
  - 插入并发命中唯一约束时返回稳定 `DUPLICATE`，HTTP 409。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 线索详情
- **接口**: `GET /api/clue/detail/{id}`
- **权限**: `@PreAuthorize("hasAuthority('clue:view')")`
- **流程**: `ClueController.loadClue()` → `ClueServiceImpl.getClueById()` → `TClueMapper.selectDetailById()`

#### 编辑线索
- **接口**: `PUT /api/clue`
- **权限**: `@PreAuthorize("hasAuthority('clue:edit')")`
- **流程**: `ClueController.editClue()` → `ClueServiceImpl.updateClue()` → `TClueMapper.updateByPrimaryKeySelective()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 转派线索负责人
- **接口**: `PUT /api/clue/{id}/owner`
- **权限**: `@PreAuthorize("hasAuthority('clue:transfer')")`
- **流程**: `ClueController.transferOwner()` → `ClueServiceImpl.transferOwner()`
  - 先按当前用户数据范围校验线索可访问。
  - 目标负责人必须来自可用负责人列表。
  - 更新 `t_clue.owner_id` 时校验旧负责人，避免并发覆盖。
  - 写入 `t_clue_owner_history`，保留原负责人、新负责人、操作人、时间和原因。
  - 记录 `CLUE_TRANSFER` 审计动作。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 查询线索责任历史
- **接口**: `GET /api/clue/{id}/owner-history`
- **权限**: `@PreAuthorize("hasAuthority('clue:view')")`
- **流程**: `ClueController.getOwnerHistory()` → `ClueServiceImpl.getOwnerHistory()` → `TClueOwnerHistoryMapper.selectByClueId()`

#### 关闭线索
- **接口**: `PUT /api/clue/{id}/close`
- **权限**: `@PreAuthorize("hasAuthority('clue:close')")`
- **流程**: `ClueController.closeClue()` → `ClueServiceImpl.closeClue()` → `TClueMapper.updateStateAtomic()`
  - 请求体必须提交关闭原因。
  - 按当前用户数据范围校验线索可访问。
  - 按 `t_dic_value.value_code` 解析 `converted` 与 `closed` 状态，已转客户线索不得关闭。
  - 更新状态时校验旧状态，影响行数不是 1 时返回业务失败。
  - 记录 `CLUE_CLOSE` 审计动作，审计摘要包含原因。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 恢复线索
- **接口**: `PUT /api/clue/{id}/restore`
- **权限**: `@PreAuthorize("hasAuthority('clue:restore')")`
- **流程**: `ClueController.restoreClue()` → `ClueServiceImpl.restoreClue()` → `TClueMapper.updateStateAtomic()`
  - 请求体必须提交恢复原因。
  - 只有 `value_code=closed` 的线索可以恢复。
  - 恢复前通过 `TClueMapper.countActiveByPhoneExcludingId()` 校验相同手机号是否存在其他活跃线索。
  - 恢复目标状态按 `value_code=attempt_contact` 解析，不写死生产或测试字典 id。
  - 记录 `CLUE_RESTORE` 审计动作，审计摘要包含原因。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 删除线索
- **接口**: `DELETE /api/clue/{id}`
- **权限**: `@PreAuthorize("hasAuthority('clue:delete')")`
- **流程**: `ClueController.delClue()` → `ClueServiceImpl.delClueById()`
  - 先按数据范围校验线索可访问。
  - 再通过 `TCustomerMapper.countByClueId()` 检查客户引用。
  - 已转客户或存在客户引用时返回 `RESOURCE_IN_USE`，不删除线索备注和线索主体。
  - 未被引用时删除线索备注，再删除线索主体。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除线索
- **接口**: `POST /api/clue/batch`
- **权限**: `@PreAuthorize("hasAuthority('clue:delete')")`
- **流程**: `ClueController.batchDelClue()` → `ClueServiceImpl.batchDelClueByIds()`
  - 批量删除先对全部 ID 完成访问权限和客户引用检查。
  - 任一线索已转客户或存在客户引用时，整个批次返回 `RESOURCE_IN_USE`，不删除任何历史。
  - 全部可删除时删除备注并调用 `TClueMapper.batchDeleteByIds()`。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 线索备注
- **新增**: `POST /api/clue/remark` → `ClueRemarkServiceImpl.saveClueRemark()` → `TClueRemarkMapper.insertSelective()`
- **分页查询**: `GET /api/clue/remark?page=1&size=10&clueId=` → `ClueRemarkServiceImpl.getClueRemarkByPage()` → `TClueRemarkMapper.selectClueRemarkByPage()`
- **事务**: 新增方法有 `@Transactional(rollbackFor = Exception.class)`

### 4.3 涉及数据库表
- `t_clue` - 线索表
- `t_clue_remark` - 线索跟踪记录表
- `t_clue_owner_history` - 线索责任归属历史表
- `t_user` - 用户表（关联查询）
- `t_activity` - 活动表（关联查询）
- `t_dic_value` - 字典值表（关联查询）
- `t_product` - 产品表（关联查询）

---

## 5. 客户管理模块

### 5.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/customer/web/CustomerController.java` |
| Service | `modules/sales/customer/application/api/CustomerService.java` → `modules/sales/customer/application/internal/CustomerServiceImpl.java` |
| Manager | `modules/sales/customer/application/internal/CustomerManager.java` |
| Mapper | `modules/sales/customer/persistence/mapper/TCustomerMapper.java`, `modules/sales/customer/persistence/mapper/TCustomerOwnerHistoryMapper.java`, `modules/sales/lead/persistence/mapper/TClueMapper.java` |
| XML | `resources/mapper/sales/customer/TCustomerMapper.xml`, `resources/mapper/sales/customer/TCustomerOwnerHistoryMapper.xml` |
| Model | `modules/sales/customer/application/api/model/TCustomer.java`, `modules/sales/customer/application/api/model/TCustomerOwnerHistory.java` |
| Query | `modules/sales/customer/application/api/query/CustomerQuery.java` |
| Excel | `modules/sales/customer/application/api/result/CustomerExcel.java` |

### 5.2 接口方法及业务流程

#### 客户列表（带查询条件）
- **接口**: `GET /api/customers?page=1&size=10`
- **流程**: `CustomerController.list()` → `CustomerServiceImpl.getCustomerList()` → `TCustomerMapper.selectByQuery()`
- **支持查询条件**: 客户名称、产品ID。
- **数据来源**: 客户姓名、联系方式、来源、负责人和状态均来自 `t_customer` 主档字段；不再通过线索联表读取客户事实。
- **敏感字段**: 手机号、微信等由后端按 `customer:sensitive:view` 权限脱敏。

#### 客户选项（下拉选择）
- **接口**: `GET /api/customer/options`
- **流程**: `CustomerController.options()` → `CustomerServiceImpl.getCustomerOptions()` → `TCustomerMapper.selectCustomerOptions()`

#### 客户详情
- **接口**: `GET /api/customer/{id}`
- **流程**: `CustomerController.detail()` → `CustomerServiceImpl.getCustomerById()` → `TCustomerMapper.selectScopedById()`
- **敏感字段**: 手机号、微信、QQ、邮箱和地址由后端按权限脱敏。

#### 线索转客户（核心业务）
- **接口**: `POST /api/clue/customer`
- **流程**: `CustomerController.convertCustomer()` → `CustomerServiceImpl.convertCustomer()` → `CustomerManager.convertCustomer()`
  1. 按当前用户数据范围读取线索，客户主档复制姓名、电话、微信、来源、活动、意向、负责人等快照。
  2. 创建前按有效联系方式进行重复客户检查；重复命中返回 `DUPLICATE`，越权重复不泄露敏感信息。
  3. 按当前用户数据范围将线索状态更新为已转客户，重复转化或越权返回失败。
  4. `TCustomerMapper.insertSelective()` 插入客户主档，`createBy` 记录操作人，`ownerId` 继承线索负责人。
  5. 转客户只创建客户事实，不自动创建交易、报价、订单、收款、发票或库存占用。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 客户归属转移
- **接口**: `PUT /api/customer/{id}/owner`
- **流程**: `CustomerController.transferOwner()` → `CustomerServiceImpl.transferOwner()` → `TCustomerMapper.updateOwnerAtomic()` + `TCustomerOwnerHistoryMapper.insert()`
- **规则**: 目标负责人必须是有效账号；转移原因必填；写入原负责人、新负责人、操作人和时间。

#### 客户合并
- **接口**: `POST /api/customer/{id}/merge`
- **流程**: `CustomerController.mergeCustomer()` → `CustomerServiceImpl.mergeCustomer()`
- **规则**: 迁移客户跟进、交易和报价引用；被合并客户标记为 `MERGED`，保留合并目标、原因、时间和操作人。

#### 客户删除
- **接口**: `DELETE /api/customer/{id}`
- **规则**: 存在线索、跟进、报价、交易等业务关系时返回 `RESOURCE_IN_USE`，不物理删除。

#### 导出 Excel
- **接口**: `GET /api/exportExcel?ids=1,2,3`
- **流程**: `CustomerController.exportExcel()` → `CustomerServiceImpl.getCustomerByExcel()` → `TCustomerMapper.selectCustomerByExcel()`
  - EasyExcel 写入 Excel
  - 查询客户主档、用户、活动、字典值、产品信息
  - 导出范围受当前用户数据范围限制，数量上限为 10000 条，敏感字段按权限脱敏。

### 5.3 涉及数据库表
- `t_customer` - 客户主档表
- `t_customer_owner_history` - 客户归属转移历史表
- `t_customer_remark` - 客户跟进记录表
- `t_clue` - 线索表（只保留历史关联，不作为客户事实来源）

---

## 6. 交易管理模块

### 6.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/fulfillment/transaction/web/TranController.java` |
| Service | `modules/fulfillment/transaction/application/api/TranService.java` → `modules/fulfillment/transaction/application/internal/TranServiceImpl.java`, `modules/fulfillment/transaction/application/api/TransactionCompletionService.java` → `modules/fulfillment/transaction/application/internal/TransactionCompletionServiceImpl.java` |
| Mapper | `modules/fulfillment/transaction/persistence/mapper/TTranMapper.java`, `modules/fulfillment/transaction/persistence/mapper/TTranProductMapper.java`, `modules/fulfillment/invoice/persistence/mapper/TTranInvoiceMapper.java`, `modules/fulfillment/transaction/persistence/mapper/TTranApproveMapper.java`, `modules/fulfillment/transaction/persistence/mapper/TTranRemarkMapper.java`, `modules/fulfillment/payment/persistence/mapper/TPaymentMapper.java`, `modules/fulfillment/payment/persistence/mapper/TRefundRequestMapper.java`, `modules/fulfillment/delivery/persistence/mapper/TDeliveryMapper.java`, `modules/commerce/inventory/persistence/mapper/TProductStockRecordMapper.java`, `modules/commerce/catalog/persistence/mapper/TProductMapper.java` |
| XML | `resources/mapper/fulfillment/transaction/TTranMapper.xml`, `resources/mapper/fulfillment/transaction/TTranProductMapper.xml`, `resources/mapper/fulfillment/invoice/TTranInvoiceMapper.xml`, `resources/mapper/fulfillment/transaction/TTranApproveMapper.xml`, `resources/mapper/fulfillment/transaction/TTranRemarkMapper.xml`, `resources/mapper/fulfillment/payment/TPaymentMapper.xml`, `resources/mapper/fulfillment/payment/TRefundRequestMapper.xml`, `resources/mapper/fulfillment/delivery/TDeliveryMapper.xml`, `resources/mapper/commerce/inventory/TProductStockRecordMapper.xml` |
| Model/DTO | `modules/fulfillment/transaction/application/api/model/TTran.java`, `modules/fulfillment/transaction/application/api/model/TTranProduct.java`, `modules/fulfillment/invoice/application/api/model/TTranInvoice.java`, `modules/fulfillment/transaction/application/api/model/TTranApprove.java`, `modules/fulfillment/transaction/application/api/model/TTranRemark.java`, `modules/fulfillment/transaction/application/api/dto/CreateTranRequest.java` |
| Query | `modules/fulfillment/transaction/application/api/query/TranQuery.java`, `modules/fulfillment/transaction/application/api/query/TranProductQuery.java` |

### 6.2 交易状态流转

```
待报价(41) → 待审批(42) → 已审批(43) → 待交付(44) → 已完成(46)
                                    ↘ 丢失关闭(21)
```

`PAYMENT` 仍作为兼容的待收款阶段保留；发票开具不再把交易推入 `PAYMENT`，收款登记和财务确认可在 `APPROVED` 或 `PAYMENT` 阶段处理。

### 6.3 接口方法及业务流程

#### 交易列表
- **接口**: `GET /api/tran/list?page=1&size=10`
- **流程**: `TranController.list()` → `TranServiceImpl.getTransactionList()` → `TTranMapper.selectByQuery()`
- **支持查询条件**: 交易编号、客户ID、客户名称、阶段、金额范围、日期范围、产品、发票状态

#### 交易详情
- **接口**: `GET /api/tran/{id}`
- **流程**: `TranController.detail()` → `TranServiceImpl.getTransactionById()` → `TTranMapper.selectByPrimaryKey()`

#### 创建交易
- **接口**: `POST /api/transactions`
- **流程**: `TranController.create()` → `TranServiceImpl.createTransaction()`
  1. 生成交易编号（TN + 年月日 + 6位随机数）
  2. 服务端校验客户数据范围和商品可售状态，并按数据库商品价格计算报价金额
  3. `TTranMapper.insertSelective()` 插入待报价交易
  4. 遍历产品列表：`TTranProductMapper.insertSelective()` 插入商品快照关联
  5. 清除 Redis 缓存
- **库存边界**: 报价阶段不扣减、不占用、不恢复商品库存；库存占用由订单成立或明确锁车任务处理。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 更新交易
- **接口**: `PUT /api/tran/update`
- **流程**: `TranController.update()` → `TranServiceImpl.updateTransaction()`
  - 更新交易基本信息
  - 替换商品行项时删除旧产品关联、插入新产品关联并重算金额，不触发库存加减。

#### 结算交易
- **接口**: `POST /api/tran/{id}/settlement-preview`、`PUT /api/tran/{id}/settle`
- **流程**: `TranController.settlementPreview()/settle()` → `TranServiceImpl.getSettlementPreview()/settleTransaction()`
  - 服务端根据商品快照和可选促销计算金额，不接受客户端自报金额。
  - 确认结算必须提交预览返回的交易版本和计价指纹，并以 CAS 更新为待审批。

#### 审批交易
- **接口**: `PUT /api/tran/approve/{id}`
- **流程**: `TranController.approve()` → `TranServiceImpl.approveTran()`
  1. `TTranApproveMapper.insertSelective()` 插入审批记录
  2. 更新交易状态：通过→已审批(43)，拒绝→丢失关闭(21)
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取审批信息
- **接口**: `GET /api/tran/approve/info/{tranId}`
- **流程**: → `TranServiceImpl.getTranApprove()` → `TTranApproveMapper.selectByTranId()`

#### 创建发票
- **接口**: `POST /api/tran/invoice`
- **流程**: `TranController.createInvoice()` → `TranServiceImpl.createTranInvoice()`
  1. 生成发票号码（INV + 年月日 + 6位随机数）
  2. 锁定交易行后按可开票余额校验，允许同一交易部分开票和多张发票
  3. `TTranInvoiceMapper.insertSelective()` 插入待开具发票
  4. 发票创建只写发票事实，不更新交易阶段
  5. 可开票余额不足返回 `TRAN_STATE_CONFLICT`，响应 data 中包含 `availableAmount`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取发票列表
- **接口**: `GET /api/tran/invoice/{tranId}`
- **流程**: → `TranServiceImpl.getTranInvoices()` → `TTranInvoiceMapper.selectByTranId()`
- **权限**: 无 `tran:invoice:sensitive` 权限时，税号、银行账号、地址和电话由后端脱敏后返回。

#### 更新发票状态
- **接口**: `PUT /api/tran/invoice/{invoiceId}/status`
- **流程**: → `TranServiceImpl.updateTranInvoiceStatus()`
  - 发票状态变为 `ISSUED` 时，先更新发票为已开具，再触发 `TransactionCompletionService.tryComplete()` 尝试完成交易。
  - 发票状态变为 `FAILED` 或 `VOIDED` 时，必须记录原因，只更新发票事实。
  - 发票状态不得单独覆盖收款状态、交付状态或交易履约阶段；交易完成必须由完成聚合条件统一判定。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 红冲和重开
- **红冲接口**: `POST /api/tran/invoice/{invoiceId}/red-reversal`
- **重开接口**: `POST /api/tran/invoice/{invoiceId}/reissue`
- **流程**:
  - 红冲创建负数红字发票并通过 `original_invoice_id` 关联原票，原票状态标记为 `PARTIAL_RED_REVERSED` 或 `RED_REVERSED`。
  - 重开基于作废或红冲事实创建新的 `PENDING` 发票记录并关联来源发票。
  - 红冲恢复的可开票余额只来自已完成红冲负数发票。
  - 红冲或重开金额超过当前服务端可用余额时返回 `TRAN_STATE_CONFLICT`，响应 data 中包含 `availableAmount`。

#### 登记收款
- **接口**: `POST /api/tran/payment`
- **流程**: → `TranServiceImpl.recordPayment()`
  - 校验交易处于已审批或兼容待收款阶段，发票状态不得作为收款入口的前置状态机。
  - 服务端按交易应收和已确认收款计算本次登记金额，不信任客户端提交金额。
  - 写入 `PENDING` 收款记录；登记不等于财务确认到账。
  - 外部支付渠道必须提交外部流水号；现金或其他手工渠道由服务端生成幂等键。
  - `transaction_ref` 和 `idempotency_key` 均有唯一约束；重复相同请求返回已有收款，不同请求复用键返回冲突。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 确认收款
- **接口**: `PUT /api/tran/payment/{id}/confirm`
- **流程**: → `TranServiceImpl.confirmPayment()`
  - 仅 `PENDING` 收款可以确认或退回。
  - 确认成功后状态变为 `COMPLETED` 并记录确认时间。
  - 确认退回后状态变为 `FAILED` 并保留退回原因。
  - 已确认收款达到应收金额时，交易从当前已审批或兼容待收款阶段进入待交付，并触发 `TransactionCompletionService.tryComplete()`。
  - 收款确认不得单独完成交易；缺少票据、交付、出库或存在退款中事项时，聚合器保持当前交易阶段。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 交易完成条件聚合
- **入口**: `TransactionCompletionService.tryComplete(tranId, operatorId)`
- **触发点**: 财务确认收款、发票标记 `ISSUED`、交付签收完成并写入出库流水后触发。
- **完成条件**:
  - 当前交易处于 `DELIVERY`，已取消、已关闭、已丢失或已完成交易不会被重复推进。
  - `COMPLETED` 收款净额达到交易应收金额，负数退款流水会抵减已收金额。
  - `ISSUED` 发票净额达到交易金额；当前模型尚无“无需开票且财务确认”字段，因此不能跳过票据条件。
  - 至少存在一条 `COMPLETED` 交付记录，并且存在以该交付记录为来源的 `OUTBOUND` 库存流水。
  - 不存在 `PENDING_APPROVAL`、`PENDING_EXECUTION` 或 `EXECUTING` 退款申请。
- **完成动作**: 使用 `TTranMapper.updateStageAtomic()` 以 `DELIVERY -> COMPLETED` 做 CAS 更新，写入 `t_tran_history`，记录 `TRAN_COMPLETE` 审计，并清理交易相关 Redis 缓存。
- **未满足条件**: 返回未完成结果，不抛错、不回滚已经合法完成的子事实。

#### 退款申请、审批与执行
- **接口**: `POST /api/tran/payment/{id}/refund-requests`、`PUT /api/tran/refund-requests/{id}/approve`、`POST /api/tran/refund-requests/{id}/execute`
- **流程**: → `TranServiceImpl.createRefundRequest()/approveRefundRequest()/executeRefundRequest()`
  - 退款必须先基于已确认原收款创建申请，申请记录保留退款原因、金额、申请人和状态。
  - `PAYMENT`、`DELIVERY` 和 `CANCELLED` 交易允许基于原收款继续处理退款，退款动作本身不负责取消交易或释放库存。
  - 可退金额由已确认原收款、已完成退款和待审批/待执行/执行中冻结退款共同计算，超额返回 `TRAN_STATE_CONFLICT`，响应 data 中包含 `availableAmount`。
  - 审批通过后进入待执行，驳回保留申请和驳回原因。
  - 执行退款成功时先标记 `EXECUTING`，再新增负数退款流水并把退款申请标记为 `COMPLETED`。
  - 执行退款失败时把退款申请标记为 `FAILED` 并保留失败原因，不生成退款流水。
  - 原收款保留原始金额、渠道、凭证和 `COMPLETED` 确认状态；退款执行不直接取消交易、不释放库存。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取交易备注
- **接口**: `GET /api/tran/remarks/{tranId}`
- **流程**: → `TranServiceImpl.getTransactionRemarks()` → `TTranRemarkMapper.selectByTranId()`

#### 获取交易产品详情
- **接口**: `GET /api/tran/products/{id}`
- **流程**: → `TranServiceImpl.getTransactionProductDetails()` → `TTranMapper.selectTranProductsByTranId()`
- **历史展示**: 返回 `t_tran_product` 中保存的商品编码、名称、配置和指导价快照，不再联表读取当前 `t_product.name`。

#### 取消或关闭交易
- **接口**: `PUT /api/tran/{id}/cancel`、`PUT /api/tran/{id}/close`
- **流程**: → `TranServiceImpl.cancelTransaction()/closeTransaction()`
  1. 请求体必须提交取消或关闭原因。
  2. 使用交易行锁读取当前交易，并按旧状态做 CAS 更新。
  3. 已完成、已取消或已关闭交易不得再次进入其他终态。
  4. 存在已确认收款、待确认收款、处理中退款、待处理发票、已出库、已签收或待交付阶段关闭时返回状态冲突。
  5. 取消交易存在未出库订单占用车辆时，先引用原 `RESERVE` 流水写入 `RELEASE` 流水，把车辆从 `ORDER_RESERVED` 释放回 `AVAILABLE`，并恢复商品可售库存；释放 CAS 失败时不更新交易阶段。
  6. 写入 `t_tran_history.reason` 保留操作原因，并记录审计日志。
  7. 不删除商品快照、备注、审批、收款、退款、发票、交付或库存流水事实。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

### 6.4 Redis 缓存

| 缓存 Key | 用途 | 过期时间 |
|----------|------|---------|
| `cdrm:tran:products:{tranId}` | 交易商品列表，统一通过 `RedisKeys.transactionProducts()` 构造 | 24小时 |
| `cdrm:tran:invoices:{tranId}` | 交易发票列表，统一通过 `RedisKeys.transactionInvoices()` 构造 | 24小时 |

交易详情、交易列表、交易支付和交易生产缓存没有有效生产者/消费者，已移除，避免无收益的 Redis 失效和 SCAN 清理。

### 6.5 涉及数据库表
- `t_tran` - 交易表
- `t_tran_product` - 交易产品关联表
- `t_tran_invoice` - 交易发票表
- `t_tran_approve` - 交易审批表
- `t_tran_remark` - 交易跟踪记录表
- `t_product` - 产品表（库存操作）
- `t_customer` - 客户表（关联查询）
- `t_clue` - 线索表（关联查询客户名称）

---

## 7. 报价订单模块

### 7.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/commerce/quote/web/QuoteController.java` |
| Service | `modules/commerce/quote/application/api/QuoteService.java` → `modules/commerce/quote/application/internal/QuoteServiceImpl.java` |
| Mapper | `modules/commerce/quote/persistence/mapper/TQuoteMapper.java`, `modules/commerce/quote/persistence/mapper/TQuoteVersionMapper.java`, `modules/commerce/quote/persistence/mapper/TQuoteVersionItemMapper.java`, `modules/commerce/quote/persistence/mapper/TQuoteStatusHistoryMapper.java` |
| XML | `resources/mapper/commerce/quote/TQuoteMapper.xml`, `resources/mapper/commerce/quote/TQuoteVersionMapper.xml`, `resources/mapper/commerce/quote/TQuoteVersionItemMapper.xml`, `resources/mapper/commerce/quote/TQuoteStatusHistoryMapper.xml` |
| Model | `modules/commerce/quote/application/api/model/TQuote.java`, `modules/commerce/quote/application/api/model/TQuoteVersion.java`, `modules/commerce/quote/application/api/model/TQuoteVersionItem.java`, `modules/commerce/quote/application/api/model/TQuoteStatusHistory.java` |
| DTO | `modules/commerce/quote/application/api/dto/CreateQuoteRequest.java`, `modules/commerce/quote/application/api/dto/CreateQuoteVersionRequest.java`, `modules/commerce/quote/application/api/dto/UpdateQuoteStatusRequest.java`, `modules/commerce/quote/application/api/dto/QuoteDetailResponse.java` |
| Query | `modules/commerce/quote/application/api/query/QuoteQuery.java` |

### 7.2 报价状态流转

报价状态使用稳定英文编码，前端只能做中文展示映射，不能提交或判断中文状态：

```text
DRAFT → PENDING_SUBMIT → PENDING_APPROVAL → PENDING_CUSTOMER_CONFIRMATION
PENDING_APPROVAL → REJECTED → DRAFT
PENDING_CUSTOMER_CONFIRMATION → ACCEPTED / REFUSED / EXPIRED
ACCEPTED → CONVERTED_TO_ORDER
任一未终态报价可 VOIDED
```

### 7.3 接口方法及业务流程

#### 报价列表
- **接口**: `GET /api/quotes?page=1&size=10`
- **流程**: `QuoteController.list()` → `QuoteServiceImpl.getQuotePage()` → `TQuoteMapper.selectByQuery()`
- **数据范围**: 通过客户关联线索负责人过滤，未授权客户的报价不可见。

#### 报价详情
- **接口**: `GET /api/quotes/{id}`
- **流程**: `QuoteController.detail()` → `QuoteServiceImpl.getQuoteDetail()` → 报价、当前版本和版本行项查询。

#### 创建报价
- **接口**: `POST /api/quotes`
- **流程**: `QuoteController.create()` → `QuoteServiceImpl.createQuote()`
  1. 校验客户数据范围。
  2. 如提交 `opportunityId`，校验商机存在、同客户、同数据范围且未进入终态。
  3. 校验商品处于 `ON_SALE`。
  4. 服务端按商品主档价格计算总额并保存商品编码、名称、配置和价格快照。
  5. 创建报价主档、版本 1、版本行项和状态历史。
- **库存边界**: 创建报价不扣减、不占用库存。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 创建报价版本
- **接口**: `POST /api/quotes/{id}/versions`
- **流程**: `QuoteController.createVersion()` → `QuoteServiceImpl.createVersion()`
  - 草稿报价覆盖当前版本行项。
  - 非草稿且未转订单报价创建新版本，并把报价重置为 `DRAFT`。
  - 已转订单报价禁止继续改版。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 报价状态变更
- **接口**: `PUT /api/quotes/{id}/status`
- **流程**: `QuoteController.updateStatus()` → `QuoteServiceImpl.transitionStatus()`
  - 请求必须提交 `expectedStatus` 和 `targetStatus` 稳定编码。
  - 服务端校验当前状态、合法迁移和影响行数，使用 CAS 更新。
  - 状态变化写入 `t_quote_status_history`，并记录审计动作。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

### 7.4 涉及数据库表
- `t_quote` - 报价主档表
- `t_quote_version` - 报价版本表
- `t_quote_version_item` - 报价版本行项快照表
- `t_quote_status_history` - 报价状态历史表
- `t_customer` - 客户表
- `t_product` - 商品主档表

---

## 8. 市场活动模块

### 8.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/activity/web/ActivityController.java`, `modules/sales/activity/web/ActivityRemarkController.java` |
| Service | `modules/sales/activity/application/api/ActivityService.java` → `modules/sales/activity/application/internal/ActivityServiceImpl.java`, `modules/sales/activity/application/api/ActivityRemarkService.java` → `modules/sales/activity/application/internal/ActivityRemarkServiceImpl.java` |
| Mapper | `modules/sales/activity/persistence/mapper/TActivityMapper.java`, `modules/sales/activity/persistence/mapper/TActivityRemarkMapper.java` |
| XML | `resources/mapper/sales/activity/TActivityMapper.xml`, `resources/mapper/sales/activity/TActivityRemarkMapper.xml` |
| Model | `modules/sales/activity/application/api/model/TActivity.java`, `modules/sales/activity/application/api/model/TActivityRemark.java` |
| Query | `modules/sales/activity/application/api/query/ActivityQuery.java`, `modules/sales/activity/application/api/query/ActivityRemarkQuery.java` |
| DTO/Result | `modules/sales/activity/application/api/dto/CreateActivityRequest.java`, `modules/sales/activity/application/api/dto/UpdateActivityRequest.java`, `modules/sales/activity/application/api/dto/ReviewActivityRequest.java`, `modules/sales/activity/application/api/dto/ActivityLifecycleRequest.java`, `modules/sales/activity/application/api/dto/ActivityRoiResponse.java`, `modules/sales/activity/application/api/result/ActivityExportRow.java` |
| Enum | `modules/sales/activity/application/api/enums/ActivityStatus.java` |

### 8.2 接口方法及业务流程

#### 活动列表分页查询
- **接口**: `GET /api/activities?page=1&size=10`
- **流程**: `ActivityController.activityPage()` → `ActivityServiceImpl.getActivityByPage()` → `TActivityMapper.selectActivityByPage()`
- **支持查询条件**: 所属人、名称、状态、渠道、时间范围、预算、创建时间
- **数据范围**: `@DataScope(tableAlias = "ta", tableField = "owner_id")` 注入负责人范围过滤，SQL 自身保持稳定排序。

#### 新增活动
- **接口**: `POST /api/activity`
- **流程**: → `ActivityServiceImpl.saveActivity()` → `TActivityMapper.insertSelective()`
- **请求体**: `CreateActivityRequest` JSON；负责人、状态、创建人均由服务端生成，默认状态为 `DRAFT`。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 活动详情
- **接口**: `GET /api/activity/{id}`
- **流程**: → `ActivityServiceImpl.getActivityById()` → `TActivityMapper.selectDetailByPrimaryKey()`

#### 编辑活动
- **接口**: `PUT /api/activity`
- **流程**: → `ActivityServiceImpl.updateActivity()` → `TActivityMapper.updateByPrimaryKeySelective()`
- **规则**: `ENDED`、`REVIEWED`、`CLOSED`、`CANCELED` 锁定活动核心事实，不能再编辑预算、渠道、目标车型和活动时间。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 活动状态与复盘
- **发布/开始/结束**: `PUT /api/activity/{id}/publish|start|end`，服务端按旧状态执行 CAS 更新。
- **复盘**: `PUT /api/activity/{id}/review`，只能从 `ENDED` 进入 `REVIEWED`，写入实际成本、复盘结果、复盘结论、复盘人和复盘时间。
- **取消/关闭**: `PUT /api/activity/{id}/cancel|close`，原因必填，不反向修改线索、客户、商机或订单状态。
- **审计**: 创建、编辑、状态变更、复盘、删除草稿、导出均写 `OperationAuditRecorder`。

#### 活动 ROI 与导出
- **ROI 查询**: `GET /api/activity/{id}/roi`，从活动来源串起线索、客户、商机、试驾、报价和交易成交金额。
- **导出**: `GET /api/activity/export`，权限为 `activity:export`，导出使用同一 ROI 口径并写审计。

#### 删除活动
- **接口**: `DELETE /api/activity/{id}`
- **流程**: → `ActivityServiceImpl.deleteActivity()` → `TActivityMapper.deleteByPrimaryKey()`
- **规则**: 仅允许物理删除无业务引用的 `DRAFT` 活动；已有备注、线索、客户、商机、试驾、报价或交易引用时返回 `RESOURCE_IN_USE(422)`。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除活动
- **接口**: `POST /api/activity/batch`
- **流程**: → `ActivityServiceImpl.batchDeleteActivities()` → `TActivityMapper.batchDeleteByIds()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 活动备注
- **新增**: `POST /api/activity/remark` → `ActivityRemarkServiceImpl.saveActivityRemark()` → `TActivityRemarkMapper.insertSelective()`
- **分页查询**: `GET /api/activity/remark?page=1&size=10&activityId=` → `TActivityRemarkMapper.selectActivityRemarkByPage()`
- **详情**: `GET /api/activity/remark/{id}` → `TActivityRemarkMapper.selectByPrimaryKey()`
- **编辑**: `PUT /api/activity/remark` → `TActivityRemarkMapper.updateByPrimaryKeySelective()`
- **删除**: `DELETE /api/activity/remark/{id}` → 逻辑删除（设置 deleted=1）

### 8.3 涉及数据库表
- `t_activity` - 市场活动表，包含 `status`、`channel`、`target_model`、`actual_cost`、复盘、关闭和取消字段。
- `t_activity_remark` - 活动备注表
- `t_clue.activity_name_snapshot`、`t_customer.activity_name_snapshot` - 来源活动名称快照，避免活动改名或关闭影响历史归因。
- `t_user` - 用户表（关联查询）

---

## 9. 商品管理模块

### 9.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/commerce/catalog/web/ProductController.java`, `modules/commerce/catalog/web/ProductCategoryController.java`, `modules/commerce/promotion/web/ProductPromotionController.java`, `modules/commerce/inventory/web/ProductStockController.java` |
| Service | `modules/commerce/catalog/application/api/ProductService.java` → `modules/commerce/catalog/application/internal/ProductServiceImpl.java`, `modules/commerce/catalog/application/api/ProductCategoryService.java` → `modules/commerce/catalog/application/internal/ProductCategoryServiceImpl.java`, `modules/commerce/promotion/application/api/ProductPromotionService.java` → `modules/commerce/promotion/application/internal/ProductPromotionServiceImpl.java`, `modules/commerce/inventory/application/api/ProductStockRecordService.java` → `modules/commerce/inventory/application/internal/ProductStockRecordServiceImpl.java`, `modules/commerce/inventory/application/api/ProductVehicleService.java` → `modules/commerce/inventory/application/internal/ProductVehicleServiceImpl.java` |
| Mapper | `modules/commerce/catalog/persistence/mapper/TProductMapper.java`, `modules/commerce/catalog/persistence/mapper/TProductCategoryMapper.java`, `modules/commerce/promotion/persistence/mapper/TProductPromotionMapper.java`, `modules/commerce/inventory/persistence/mapper/TProductStockRecordMapper.java`, `modules/commerce/inventory/persistence/mapper/TProductVehicleMapper.java` |
| XML | `resources/mapper/commerce/catalog/TProductMapper.xml`, `resources/mapper/commerce/catalog/TProductCategoryMapper.xml`, `resources/mapper/commerce/promotion/TProductPromotionMapper.xml`, `resources/mapper/commerce/inventory/TProductStockRecordMapper.xml`, `resources/mapper/commerce/inventory/TProductVehicleMapper.xml` |
| Model | `modules/commerce/catalog/application/api/model/TProduct.java`, `modules/commerce/catalog/application/api/model/TProductCategory.java`, `modules/commerce/promotion/application/api/model/TProductPromotion.java`, `modules/commerce/inventory/application/api/model/TProductVehicle.java`, `modules/commerce/inventory/application/api/model/TProductStockRecord.java` |

### 9.2 接口方法及业务流程

#### 产品管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/products` | 获取产品列表 | 无 |
| `GET /api/products/{id}` | 获取产品详情 | 无 |
| `POST /api/products` | 新增产品 | `@Transactional` |
| `PUT /api/products/{id}` | 更新产品 | `@Transactional` |
| `DELETE /api/products/{id}` | 删除产品 | `@Transactional` |
| `GET /api/products/stockalerts` | 库存预警列表 | 无 |

商品新增可以设置初始库存；商品编辑只维护资料字段，不通过 `PUT /api/products/{id}` 修改库存数量。库存变动必须走库存命令接口并写入库存流水。

商品状态在接口和数据库中统一使用稳定编码 `ON_SALE`、`OFF_SALE`；中文“上架/下架”只用于前端展示。`TProductMapper.selectAllOnSale()` 只查询 `ON_SALE`，后端 DTO 和 `t_product` CHECK 约束都会拒绝中文状态或历史别名。

商品删除由 `ProductServiceImpl.deleteProduct()` 统一检查引用：交易商品快照、库存流水、促销、客户选购商品和线索意向商品任一存在时返回 `RESOURCE_IN_USE`，不执行物理删除；商品不存在时返回 `NOT_FOUND`。

#### 产品分类管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/product-categories` | 分类列表 | 无 |
| `GET /api/product-categories/{id}` | 分类详情 | 无 |
| `POST /api/product-categories` | 新增分类 | `@Transactional` |
| `PUT /api/product-categories/{id}` | 更新分类 | `@Transactional` |
| `DELETE /api/product-categories/{id}` | 删除分类 | `@Transactional` |

分类删除由 `ProductCategoryServiceImpl.deleteCategory()` 统一检查：分类不存在返回 `NOT_FOUND`；存在商品引用时返回 `RESOURCE_IN_USE`，不执行物理删除，确保历史商品和交易展示仍可追溯。

#### 产品促销管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/product-promotions` | 促销列表 | 无 |
| `GET /api/product-promotions/{id}` | 促销详情 | 无 |
| `POST /api/product-promotions` | 新增促销，默认 `DRAFT` | `@Transactional` |
| `PUT /api/product-promotions/{id}` | 更新促销规则，不接收状态 | `@Transactional` |
| `PUT /api/product-promotions/{id}/publish` | 发布促销 | `@Transactional` |
| `PUT /api/product-promotions/{id}/activate` | 生效或恢复促销 | `@Transactional` |
| `PUT /api/product-promotions/{id}/pause` | 暂停促销 | `@Transactional` |
| `PUT /api/product-promotions/{id}/end` | 结束促销 | `@Transactional` |
| `PUT /api/product-promotions/{id}/void` | 作废促销 | `@Transactional` |
| `DELETE /api/product-promotions/{id}` | 删除未引用草稿促销 | `@Transactional` |

促销状态使用稳定 code：`DRAFT`、`PENDING_EFFECTIVE`、`ACTIVE`、`PAUSED`、`ENDED`、`VOIDED`、`EXHAUSTED`。报价和交易结算通过 `ProductPromotionService.requireApplicablePromotion()` 校验状态、时间、商品、适用范围、预算和名额；交易结算通过 `reserveUsage()` 在同一事务内写入 `t_product_promotion_usage` 并原子扣减预算/名额。报价版本商品快照保留促销 code、名称、规则摘要、优惠金额和完整快照 JSON。

#### 库存管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `POST /api/productstock/restock` | 入库 | `@Transactional` |
| `GET /api/productstock/records/{productId}` | 库存变动记录 | 无 |
| `GET /api/productstock/vehicles` | 库存车辆实例列表 | 无 |
| `POST /api/productstock/vehicles` | 库存车辆实例入库 | `@Transactional` |
| `POST /api/productstock/vehicles/{vehicleId}/reserve` | 占用库存车辆实例 | `@Transactional` |
| `POST /api/productstock/vehicles/{vehicleId}/release` | 释放库存车辆实例占用 | `@Transactional` |

### 9.3 入库业务流程
```
ProductStockController.restock()
→ ProductServiceImpl.restock()
  → ProductMapper.updateStock() 更新库存
  → ProductStockRecordMapper.insert() 记录库存变动
```

库存车辆实例由 `ProductVehicleServiceImpl` 维护，车辆 VIN 使用唯一约束。实例入库会创建 `t_product_vehicle` 的 `AVAILABLE` 车辆、增加商品库存汇总并写入 `INBOUND` 流水；占用会在事务内锁定车辆实例，校验旧状态为 `AVAILABLE` 后通过 CAS 改为 `ORDER_RESERVED`、`TEST_DRIVE_RESERVED` 或 `SALES_LOCKED`，扣减商品库存汇总并写入 `RESERVE` 流水；释放必须引用原占用流水，重复释放按原流水幂等返回当前车辆，不重复增加库存。

### 9.4 涉及数据库表
- `t_product` - 产品表
- `t_product_category` - 产品分类表
- `t_product_promotion` - 产品促销表
- `t_product_vehicle` - 库存车辆实例表，记录 VIN、库位、实例状态和业务来源
- `t_product_stock_record` - 库存变动记录表，记录入库、占用、释放和调整流水，关联车辆实例与原占用流水

---

## 10. 字典管理模块

### 10.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/dictionary/web/DicController.java` |
| Service | `modules/dictionary/application/api/DicService.java` → `modules/dictionary/application/internal/DicServiceImpl.java` |
| Mapper | `modules/dictionary/persistence/mapper/DicMapper.java`, `modules/dictionary/persistence/mapper/TDicTypeMapper.java`, `modules/dictionary/persistence/mapper/TDicValueMapper.java` |
| XML | `resources/mapper/dictionary/DicMapper.xml` |
| Model | `modules/dictionary/application/api/model/TDicType.java`, `modules/dictionary/application/api/model/TDicValue.java` |
| Query | `modules/dictionary/application/api/query/DicQuery.java` |

### 10.2 接口方法及业务流程

#### 字典类型管理
| 接口 | 方法 | 事务 | 缓存 |
|------|------|------|------|
| `GET /api/dict/types` | 分页查询字典类型 | 无 | 无 |
| `GET /api/dict/type/get/{id}` | 获取字典类型详情 | 无 | Redis: `cdrm:dict:type:{id}` |
| `POST /api/dict/type/create` | 新增字典类型 | `@Transactional` | 清除统一字典缓存 |
| `PUT /api/dict/type/update/{id}` | 更新字典类型；稳定 `typeCode` 不可变，停用需原因 | `@Transactional` | 清除统一字典缓存 |
| `DELETE /api/dict/type/delete/{id}` | 删除未被引用且非内置的字典类型 | `@Transactional` | 清除统一字典缓存 |
| `DELETE /api/dict/types/batch` | 批量删除未被引用且非内置的字典类型 | `@Transactional` | 清除统一字典缓存 |

#### 字典值管理
| 接口 | 方法 | 事务 | 缓存 |
|------|------|------|------|
| `GET /api/dict/values` | 分页查询字典值 | 无 | 无 |
| `GET /api/dict/value/get/{id}` | 获取字典值详情 | 无 | Redis: `cdrm:dict:value:{id}` |
| `POST /api/dict/value/create` | 新增字典值 | `@Transactional` | 清除统一字典缓存 |
| `PUT /api/dict/value/update/{id}` | 更新字典值；稳定 `typeCode/valueCode` 不可变，停用需原因 | `@Transactional` | 清除统一字典缓存 |
| `DELETE /api/dict/value/delete/{id}` | 删除未被引用且非内置的字典值 | `@Transactional` | 清除统一字典缓存 |
| `DELETE /api/dict/value/batch` | 批量删除未被引用且非内置的字典值 | `@Transactional` | 清除统一字典缓存 |

#### 缓存管理
| 接口 | 方法 | 权限 |
|------|------|------|
| `GET /api/dict/clear?forceRefresh=true` | 清除缓存 | `@PreAuthorize("hasAuthority('admin')")` |
| `GET /api/dict/refresh?type=type\|value` | 刷新缓存 | 无 |

### 10.3 字典删除与缓存规则
```
deleteDicType(id):
  1. 获取字典类型并检查 built_in
  2. 获取关联字典值 ID 列表
  3. 检查交易备注、线索备注、客户备注、活动备注、线索主档和客户主档引用
  4. 存在引用时返回 RESOURCE_IN_USE，不删除任何业务备注或历史记录
  5. 无引用时删除字典值和字典类型
```

字典类型和值均保留 `typeCode/valueCode` 稳定编码、`enabled` 启停、`built_in` 内置保护、`applicable_module` 适用模块和停用原因/操作人/时间。字典变更后统一清理 `cdrm:dict:type:*`、`cdrm:dict:value:*` 和 `cdrm:dict:values:type:*`；Redis 删除失败返回业务失败，不伪装成功。

### 10.4 涉及数据库表
- `t_dic_type` - 字典类型表
- `t_dic_value` - 字典值表
- `t_tran_remark`、`t_clue_remark`、`t_customer_remark`、`t_activity_remark` - 字典引用检查，不做级联删除

---

## 11. 统计报表模块

### 11.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/analytics/web/StatisticController.java` |
| Service | `modules/analytics/application/api/StatisticService.java` → `modules/analytics/application/internal/StatisticServiceImpl.java` |
| Manager | `modules/analytics/application/internal/StatisticManager.java` |
| Result | `modules/analytics/application/api/result/SummaryData.java`, `modules/analytics/application/api/result/NameValue.java` |

### 11.2 接口方法及业务流程

统计接口由后端从当前登录用户解析数据范围：管理员查看全局数据，非管理员仅统计与其明细权限一致的活动、线索、客户和交易数据。前端不得传入范围参数扩大查询。

#### 汇总数据
- **接口**: `GET /api/summary/data`
- **流程**: `StatisticController.summaryData()` → `StatisticServiceImpl.loadSummaryData()` → `StatisticManager.loadSummaryData()` → `CurrentUserProvider` 提供数据范围 → Mapper 聚合
- **返回数据**:
  - 有效市场活动数
  - 总市场活动数
  - 线索总数
  - 客户总数
  - 成功交易额
  - 总交易额

#### 销售漏斗
- **接口**: `GET /api/saleFunnel/data`
- **流程**: → `StatisticManager.loadSaleFunnelData()` → 按当前用户数据范围聚合
- **返回数据**: 线索→客户→交易→成交 的数量漏斗

#### 来源饼图
- **接口**: `GET /api/sourcePie/data`
- **流程**: → `StatisticManager.loadSourcePieData()` → `TClueMapper.selectBySource(dataScopeUserId)`
- **返回数据**: 按线索来源分组统计

### 11.3 涉及数据库表
- `t_activity` - 市场活动表
- `t_clue` - 线索表
- `t_customer` - 客户表
- `t_tran` - 交易表

---

## 12. 交付管理模块

### 12.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/fulfillment/delivery/web/DeliveryController.java` |
| Service | `modules/fulfillment/delivery/application/api/DeliveryService.java` → `modules/fulfillment/delivery/application/internal/DeliveryServiceImpl.java` |
| Mapper | `modules/fulfillment/delivery/persistence/mapper/TDeliveryMapper.java`, `modules/fulfillment/delivery/persistence/mapper/TDeliveryCheckItemMapper.java`, `modules/commerce/inventory/persistence/mapper/TProductVehicleMapper.java`, `modules/commerce/inventory/persistence/mapper/TProductStockRecordMapper.java` |
| XML | `resources/mapper/fulfillment/delivery/TDeliveryMapper.xml`, `resources/mapper/fulfillment/delivery/TDeliveryCheckItemMapper.xml`, `resources/mapper/commerce/inventory/TProductVehicleMapper.xml`, `resources/mapper/commerce/inventory/TProductStockRecordMapper.xml` |
| Model | `modules/fulfillment/delivery/application/api/model/TDelivery.java`, `modules/fulfillment/delivery/application/api/model/TDeliveryCheckItem.java` |
| DTO | `modules/fulfillment/delivery/application/api/dto/CreateDeliveryRequest.java`, `modules/fulfillment/delivery/application/api/dto/UpdateDeliveryCheckItemRequest.java`, `modules/fulfillment/delivery/application/api/dto/SignDeliveryRequest.java`, `modules/fulfillment/delivery/application/api/dto/DeliveryExceptionRequest.java`, `modules/fulfillment/delivery/application/api/dto/DeliveryCancelRequest.java` |
| Query | `modules/fulfillment/delivery/application/api/query/DeliveryQuery.java` |

### 12.2 接口方法及业务流程

#### 创建交付记录
- **接口**: `POST /api/deliveries`
- **权限**: `@PreAuthorize("hasAuthority('delivery:create')")`
- **流程**: `DeliveryController.create()` → `DeliveryServiceImpl.createDelivery()`
  - 交易必须处于 `DELIVERY` 阶段，且当前用户具备交易客户数据访问权限。
  - 车辆必须处于 `ORDER_RESERVED`，占用来源必须是当前交易订单。
  - 默认生成车辆、资料、收款、发票和客户预约准备项；请求体也可以提交自定义准备项。
  - 同一交易只能保留一条交付事实记录，重复创建返回已有活跃交付记录。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 交付准备清单
- **接口**: `GET /api/deliveries/{id}/check-items`、`PUT /api/deliveries/check-items/{itemId}`
- **权限**: `delivery:view`、`delivery:check`
- **流程**: 准备项状态只能使用 `PENDING`、`COMPLETED`、`BLOCKED` 稳定编码；更新时锁定准备项和交付记录，交付终态后不得再改准备项。

#### 客户签收与库存出库
- **接口**: `POST /api/deliveries/{id}/sign`
- **权限**: `delivery:sign`
- **流程**: `DeliveryServiceImpl.signDelivery()`
  - 所有准备项必须为 `COMPLETED`。
  - 车辆仍需为当前交易的 `ORDER_RESERVED`，并存在未释放的订单占用流水。
  - 签收在同一事务内把车辆更新为 `OUTBOUND`，写入 `OUTBOUND` 库存流水并关联原占用流水。
  - 签收只写交付事实和库存出库事实，不直接把交易阶段改为 `COMPLETED`；签收完成后触发 `TransactionCompletionService.tryComplete()` 进行完整条件聚合。
  - 重复签收已完成交付时返回当前记录，不重复写出库流水。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 交付异常与取消
- **接口**: `POST /api/deliveries/{id}/exception`、`POST /api/deliveries/{id}/cancel`
- **权限**: `delivery:exception`、`delivery:cancel`
- **流程**: 异常和取消必须提交原因，保留交付记录和准备项历史，写入操作审计。
  - 取消交付会先校验原订单 `RESERVE` 库存占用流水。
  - 未签收交付取消时，同一事务内将车辆从 `ORDER_RESERVED` 释放回 `AVAILABLE`，恢复商品可售库存，并写入关联原占用流水的 `RELEASE` 流水。
  - 同一原占用已有 `RELEASE` 流水时，取消交付不重复恢复库存。
  - 已签收或已完成交付不得通过普通取消处理，后续应走退款、红冲和库存恢复等纠错流程。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

### 12.3 涉及数据库表
- `t_delivery` - 交付记录主表
- `t_delivery_check_item` - 交付准备清单表
- `t_product_vehicle` - 车辆实例状态
- `t_product_stock_record` - 库存占用、释放、出库流水
- `t_tran` - 交易阶段与客户数据范围

---

## 13. 商机管理模块

### 13.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/opportunity/web/OpportunityController.java` |
| Service | `modules/sales/opportunity/application/api/OpportunityService.java` → `modules/sales/opportunity/application/internal/OpportunityServiceImpl.java` |
| Mapper | `modules/sales/opportunity/persistence/mapper/TOpportunityMapper.java`, `modules/sales/opportunity/persistence/mapper/TOpportunityStageHistoryMapper.java` |
| XML | `resources/mapper/sales/opportunity/TOpportunityMapper.xml`, `resources/mapper/sales/opportunity/TOpportunityStageHistoryMapper.xml` |
| Model | `modules/sales/opportunity/application/api/model/TOpportunity.java`, `modules/sales/opportunity/application/api/model/TOpportunityStageHistory.java` |
| DTO | `modules/sales/opportunity/application/api/dto/CreateOpportunityRequest.java`, `modules/sales/opportunity/application/api/dto/UpdateOpportunityRequest.java`, `modules/sales/opportunity/application/api/dto/AdvanceOpportunityStageRequest.java`, `modules/sales/opportunity/application/api/dto/OpportunityResultRequest.java` |
| Enum | `modules/sales/opportunity/application/api/enums/OpportunityStage.java` |
| Query | `modules/sales/opportunity/application/api/query/OpportunityQuery.java` |

### 13.2 接口方法及业务流程

#### 创建和编辑商机
- **接口**: `POST /api/opportunities`、`PUT /api/opportunities/{id}`
- **权限**: `opportunity:create`、`opportunity:edit`
- **流程**: 商机基于已识别客户创建，不创建交易、订单、收款或发票；客户访问范围由服务端当前用户数据范围校验，负责人来自客户主档或当前登录人，前端不得提交可信负责人或数据范围。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 阶段推进和阶段历史
- **接口**: `PUT /api/opportunities/{id}/stage`、`GET /api/opportunities/{id}/stage-history`
- **权限**: `opportunity:advance`、`opportunity:view`
- **流程**: 阶段只允许使用 `INITIAL_CONTACT`、`NEEDS_ANALYSIS`、`VEHICLE_MATCHING`、`TEST_DRIVE_INVITED`、`QUOTING`、`NEGOTIATION`、`PENDING_APPROVAL`、`WON`、`LOST`、`SHELVED`、`CLOSED` 稳定编码。推进命令必须提交 `expectedStage`、`targetStage` 和原因，服务端按当前阶段做 CAS 更新并写入 `t_opportunity_stage_history`。
- **边界**: 商机阶段不承载收款、发票、库存出库或交付状态。

#### 赢单、输单、搁置和恢复
- **接口**: `PUT /api/opportunities/{id}/won`、`/lost`、`/shelve`、`/restore`
- **权限**: `opportunity:win`、`opportunity:lose`、`opportunity:shelve`、`opportunity:restore`
- **流程**: 赢单必须关联已成立交易；输单必须填写原因和可选竞品或反馈；搁置必须填写原因和下一步日期；恢复只允许搁置或输单商机重新进入需求确认，并保留原关闭事实和阶段历史。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

### 13.3 涉及数据库表
- `t_opportunity` - 商机主表
- `t_opportunity_stage_history` - 商机阶段历史表
- `t_customer` - 客户主档和数据范围
- `t_tran` - 赢单关联已成立交易

---

## 14. 试驾管理模块

### 14.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/testdrive/web/TestDriveController.java` |
| Service | `modules/sales/testdrive/application/api/TestDriveService.java` → `modules/sales/testdrive/application/internal/TestDriveServiceImpl.java` |
| Mapper | `modules/sales/testdrive/persistence/mapper/TTestDriveMapper.java`, `modules/sales/testdrive/persistence/mapper/TTestDriveVehicleHoldMapper.java`, `modules/sales/testdrive/persistence/mapper/TTestDriveStatusHistoryMapper.java` |
| XML | `resources/mapper/sales/testdrive/TTestDriveMapper.xml`, `resources/mapper/sales/testdrive/TTestDriveVehicleHoldMapper.xml`, `resources/mapper/sales/testdrive/TTestDriveStatusHistoryMapper.xml` |
| Model | `modules/sales/testdrive/application/api/model/TTestDrive.java`, `modules/sales/testdrive/application/api/model/TTestDriveVehicleHold.java`, `modules/sales/testdrive/application/api/model/TTestDriveStatusHistory.java` |
| DTO | `modules/sales/testdrive/application/api/dto/CreateTestDriveRequest.java`, `modules/sales/testdrive/application/api/dto/RescheduleTestDriveRequest.java`, `modules/sales/testdrive/application/api/dto/CancelTestDriveRequest.java`, `modules/sales/testdrive/application/api/dto/CheckInTestDriveRequest.java`, `modules/sales/testdrive/application/api/dto/CompleteTestDriveRequest.java` |
| Enum | `modules/sales/testdrive/application/api/enums/TestDriveStatus.java` |
| Query | `modules/sales/testdrive/application/api/query/TestDriveQuery.java` |

### 14.2 接口方法及业务流程

#### 预约与车辆时间占用
- **接口**: `GET /api/test-drives`、`POST /api/test-drives`
- **权限**: `test-drive:list`、`test-drive:create`
- **流程**: 预约基于客户和可用库存车辆创建；客户数据范围由服务端校验，负责销售来自客户主档或当前登录人。服务端锁定车辆行、校验车辆可用、检查车辆和负责销售时间段冲突，写入 `t_test_drive`、`t_test_drive_vehicle_hold` 和 `t_test_drive_status_history`。
- **边界**: 试驾时间占用不扣减商品库存，不把车辆状态改成订单占用，不自动创建报价、订单、收款或交付。

#### 改期、取消和爽约
- **接口**: `PUT /api/test-drives/{id}/reschedule`、`/cancel`、`/no-show`
- **权限**: `test-drive:reschedule`、`test-drive:cancel`
- **流程**: 改期先校验新车辆和新时段，再在同一事务内释放原占用并创建新占用；取消和爽约必须填写原因并释放未开始试驾的时间占用，记录历史事实。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 签到与完成
- **接口**: `PUT /api/test-drives/{id}/check-in`、`/complete`
- **权限**: `test-drive:check-in`、`test-drive:complete`
- **流程**: 签到记录到店时间、签到人和客户确认方式；完成必须已签到并完成安全确认，记录实际开始/结束、试驾结果、客户反馈和下一步动作，释放车辆时间占用。
- **边界**: 试驾完成只形成客户体验和后续动作事实，不自动赢单、不自动创建报价或交易。

### 14.3 涉及数据库表
- `t_test_drive` - 试驾预约与执行主表
- `t_test_drive_vehicle_hold` - 试驾车辆时间段占用表
- `t_test_drive_status_history` - 试驾状态历史表
- `t_product_vehicle` - 试驾车辆实例和车辆可用性
- `t_customer`、`t_opportunity` - 客户与商机关联及数据范围

---

## 15. 跟进任务模块

### 15.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/sales/followup/web/FollowTaskController.java`, `modules/sales/followup/web/CommunicationRecordController.java` |
| Service | `modules/sales/followup/application/api/FollowTaskService.java`, `modules/sales/followup/application/api/CommunicationRecordService.java` |
| Service 实现 | `modules/sales/followup/application/internal/FollowTaskServiceImpl.java`, `modules/sales/followup/application/internal/CommunicationRecordServiceImpl.java`, `modules/sales/followup/application/internal/FollowRelatedObjectResolver.java` |
| Mapper | `modules/sales/followup/persistence/mapper/TFollowTaskMapper.java`, `modules/sales/followup/persistence/mapper/TCommunicationRecordMapper.java` |
| XML | `resources/mapper/sales/followup/TFollowTaskMapper.xml`, `resources/mapper/sales/followup/TCommunicationRecordMapper.xml` |
| Model | `modules/sales/followup/application/api/model/TFollowTask.java`, `modules/sales/followup/application/api/model/TCommunicationRecord.java` |
| DTO | `modules/sales/followup/application/api/dto/CreateFollowTaskRequest.java`, `modules/sales/followup/application/api/dto/PostponeFollowTaskRequest.java`, `modules/sales/followup/application/api/dto/CancelFollowTaskRequest.java`, `modules/sales/followup/application/api/dto/CompleteFollowTaskRequest.java`, `modules/sales/followup/application/api/dto/CreateCommunicationRecordRequest.java`, `modules/sales/followup/application/api/dto/CorrectCommunicationRecordRequest.java`, `modules/sales/followup/application/api/dto/VoidCommunicationRecordRequest.java` |
| Enum | `modules/sales/followup/application/api/enums/FollowRelatedObjectType.java`, `modules/sales/followup/application/api/enums/FollowTaskStatus.java`, `modules/sales/followup/application/api/enums/FollowTaskType.java`, `modules/sales/followup/application/api/enums/FollowTaskPriority.java`, `modules/sales/followup/application/api/enums/CommunicationMethod.java`, `modules/sales/followup/application/api/enums/CommunicationRecordStatus.java` |
| Query | `modules/sales/followup/application/api/query/FollowTaskQuery.java`, `modules/sales/followup/application/api/query/CommunicationRecordQuery.java` |

### 15.2 跟进任务流程

- **接口**: `GET /api/follow-tasks`、`POST /api/follow-tasks`
- **权限**: `follow-task:list`、`follow-task:create`
- **流程**: 跟进任务独立于线索、客户、商机、试驾和订单主流程；创建时必须提供关联对象、负责人、计划时间、任务类型和优先级。服务端校验关联对象可见性、负责人有效性和数据范围，不信任客户端提交的创建人或数据范围。
- **逾期维护**: 列表查询前按当前时间把当前用户数据范围内的 `PENDING`、`IN_PROGRESS`、`POSTPONED` 到期任务标记为 `OVERDUE`，前端只展示稳定英文状态的中文 label。

### 15.3 状态迁移

- **开始**: `PUT /api/follow-tasks/{id}/start`，只允许非终态任务进入 `IN_PROGRESS`。
- **延期**: `PUT /api/follow-tasks/{id}/postpone`，必须记录延期原因、原计划时间和新计划时间，终态任务不可延期。
- **取消**: `PUT /api/follow-tasks/{id}/cancel`，必须记录取消原因，终态任务不可取消。
- **完成**: `PUT /api/follow-tasks/{id}/complete`，同一事务内写入沟通记录、更新任务为 `COMPLETED`，并回写线索、客户或商机最近跟进时间和摘要。
- **并发**: 状态迁移基于当前状态和影响行数判断，CAS 失败返回业务冲突，不把旧状态请求伪装为成功。

### 15.4 沟通记录

- **接口**: `GET /api/communication-records`、`POST /api/communication-records`
- **权限**: `communication-record:list`、`communication-record:create`
- **流程**: 沟通记录必须包含关联对象、沟通方式、沟通时间和摘要，可关联跟进任务；关联任务时必须与任务对象一致，服务端校验对象可见性。
- **更正**: `PUT /api/communication-records/{id}/correct` 将原记录置为 `CORRECTED` 并插入新 `ACTIVE` 记录，保留 `parentRecordId` 追溯关系。
- **作废**: `PUT /api/communication-records/{id}/void` 仅置为 `VOIDED` 并记录原因，不物理删除历史事实。

### 15.5 涉及数据库表

- `t_follow_task` - 跟进任务主表，记录负责人、关联对象、计划时间、状态和延期/取消/完成事实。
- `t_communication_record` - 沟通记录表，记录沟通方式、时间、摘要、反馈、下一步动作和更正/作废事实。
- `t_clue`、`t_customer`、`t_opportunity` - 最近跟进时间和摘要回写目标。
- `t_test_drive`、`t_tran` - 跟进任务和沟通记录可关联对象。

---

## 16. 审计日志模块

### 16.1 模块概述

审计日志分为登录记录和操作记录两类：

- 登录记录写入 `t_login_log`，由 `LoginAuditRecorder` 在登录成功和登录失败链路统一落库。
- 操作记录写入 `t_operation_log`，业务模块通过 `OperationAuditRecorder` 记录，调用方事务回滚时关键业务审计一同回滚。
- 普通 Token 过期、权限不足和退出登录不作为登录记录写入；这些边界以业务 Spec 为准。

### 16.2 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `modules/audit/web/AuditLogController.java` |
| Service | `modules/audit/application/api/AuditLogService.java` → `modules/audit/application/internal/AuditLogServiceImpl.java` |
| Recorder | `modules/audit/application/api/LoginAuditRecorder.java`, `modules/audit/application/api/OperationAuditRecorder.java` |
| Mapper | `modules/audit/persistence/mapper/TLoginLogMapper.java`, `modules/audit/persistence/mapper/TOperationLogMapper.java` |
| XML | `resources/mapper/audit/TLoginLogMapper.xml`, `resources/mapper/audit/TOperationLogMapper.xml` |
| Model | `modules/audit/persistence/model/TLoginLog.java`, `modules/audit/persistence/model/TOperationLog.java` |
| Query | `modules/audit/application/api/query/AuditLoginLogQuery.java`, `modules/audit/application/api/query/AuditOperationLogQuery.java` |

### 16.3 登录记录

- 登录成功：Redis 会话写入成功后写入 `t_login_log`，记录登录账号、用户 ID、用户名、结果、原因编码、IP、浏览器、操作系统、requestId 和时间。
- 登录审计写入失败：删除已写 Redis 会话，返回 HTTP 500，不向前端发 JWT。
- 登录失败：写入失败记录，`reason_code` 使用稳定编码，例如 `BAD_CREDENTIALS`、`ACCOUNT_LOCKED`；失败记录不保存密码、JWT、Cookie 或异常堆栈。
- 登录失败审计写入失败不改变原始 401 登录失败结果，只记录应用日志。

### 16.4 操作记录

- `TOperationLog` 保存 `action_code`、`module_name`、`object_type`、`resource_id`、`result`、`detail`、`ip`、`request_id` 和操作时间。
- `detail` 仅保存脱敏后的结构化摘要，敏感字段必须由调用方预先排除。
- 导出登录记录和操作记录本身也通过 `AUDIT_LOGIN_EXPORT`、`AUDIT_OPERATION_EXPORT` 写操作审计。

### 16.5 查询与导出接口

| 接口 | 权限 | 说明 |
|------|------|------|
| GET `/api/audit/login-logs` | `audit:login:list` | 分页查询登录记录，支持账号、姓名、结果、原因、IP、requestId、时间区间过滤 |
| GET `/api/audit/login-logs/{id}` | `audit:login:detail` | 查询登录记录详情 |
| GET `/api/audit/login-logs/export` | `audit:login:export` | 按过滤条件导出 UTF-8 CSV |
| GET `/api/audit/operation-logs` | `audit:operation:list` | 分页查询操作记录，支持用户、动作、模块、对象、结果、IP、requestId、时间区间过滤 |
| GET `/api/audit/operation-logs/{id}` | `audit:operation:detail` | 查询操作记录详情 |
| GET `/api/audit/operation-logs/export` | `audit:operation:export` | 按过滤条件导出 UTF-8 CSV |

## 17. 安全配置

### 17.1 SecurityConfig
**路径**: `bootstrap/security/SecurityConfig.java`

```java
@EnableMethodSecurity  // 开启方法级别权限检查
@Configuration
public class SecurityConfig {
    // 配置项：
    // 1. 密码编码器：BCryptPasswordEncoder
    // 2. 表单登录：/api/login
    // 3. 请求授权：/api/login 公开，OPTIONS 公开，其他需认证
    // 4. 禁用 CSRF
    // 5. 无状态 Session（STATELESS）
    // 6. 自定义 Filter：TokenVerifyFilter（在 LogoutFilter 之前）
    // 7. 退出登录：/api/logout
    // 8. 异常处理：MyAccessDeniedHandler
}
```

### 17.2 TokenVerifyFilter
**路径**: `bootstrap/security/TokenVerifyFilter.java`

**执行逻辑**:
1. 登录请求放行
2. 仅从 `Authorization: Bearer <token>` 请求头获取 token
3. 验证 token 非空、Bearer 格式正确、签名有效，并解析 userId 与 authVersion
4. 验证 Redis 中存在该 token 且值精确匹配，再从数据库重新加载用户并检查账号状态
5. 以数据库 authVersion 为权威校验 JWT；无 authVersion 的旧 JWT 仅在数据库版本为 0 时兼容
6. 账号状态无效或版本不匹配返回 HTTP 401；Redis 清理失败仅重试并告警
7. 验证通过后设置 SecurityContext

### 17.3 Handler 处理器

| Handler | 路径 | 功能 |
|---------|------|------|
| MyAuthenticationSuccessHandler | `bootstrap/security/MyAuthenticationSuccessHandler.java` | 登录成功：生成 JWT、存入 Redis、写登录审计、返回 token |
| MyAuthenticationFailureHandler | `bootstrap/security/MyAuthenticationFailureHandler.java` | 登录失败：写失败登录审计并返回 401 |
| MyLogoutSuccessHandler | `bootstrap/security/MyLogoutSuccessHandler.java` | 退出登录：先提交 authVersion 递增，再尽力清理 Redis 会话 |
| MyAccessDeniedHandler | `bootstrap/security/MyAccessDeniedHandler.java` | 已认证但权限不足：返回 HTTP 403 和 ACCESS_DENIED |
| GlobalExceptionHandler | `shared/web/GlobalExceptionHandler.java` | 全局异常处理：统一返回错误 |

### 17.4 GlobalExceptionHandler 异常处理

| 异常类型 | 处理方式 |
|----------|---------|
| `AccessDeniedException` | 返回 ACCESS_DENIED(520) |
| `DataAccessException` | 返回 DATA_ACCESS_EXCEPTION(521) |
| `HttpRequestMethodNotSupportedException` | 返回不支持的请求方法 |
| `MethodArgumentNotValidException` | 返回参数校验失败 |
| `HttpMessageNotReadableException` | 返回请求体格式错误 |
| `Exception` | 返回通用错误信息 |

### 17.5 CorsConfig
**路径**: `shared/web/CorsConfig.java`

- 允许所有源（`addAllowedOriginPattern("*")`）
- 允许所有请求头
- 允许 GET/POST/PUT/DELETE/OPTIONS 方法
- 允许携带 Cookie
- 预检缓存 30 分钟

---

## 18. AOP 切面

### 18.1 DataScopeAspect
**路径**: `modules/identity/application/internal/DataScopeAspect.java`

**功能**: 数据权限过滤，根据用户角色动态添加 SQL 过滤条件。

**实现逻辑**:
```java
@Aspect
@Component
public class DataScopeAspect {
    @Around("@annotation(commons.com.autodealer.crm.DataScope)")
    public Object process(ProceedingJoinPoint joinPoint) {
        // 1. 获取方法上的 @DataScope 注解
        // 2. 获取 tableAlias 和 tableField
        // 3. 从请求头获取 token，解析用户信息
        // 4. 判断用户角色：
        //    - admin 角色：不添加过滤条件，查所有数据
        //    - 普通用户：添加 "and {tableAlias}.{tableField} = {userId}"
        // 5. 将 filterSQL 设置到方法参数（BaseQuery）中
        // 6. 继续执行方法
    }
}
```

### 18.2 DataScope 注解
**路径**: `modules/identity/application/api/security/DataScope.java`

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScope {
    String tableAlias() default "";  // 表别名
    String tableField() default "";  // 表字段名
}
```

**使用示例**:
```java
@DataScope(tableAlias = "ta", tableField = "owner_id")
public PageInfo<TActivity> getActivityByPage(Integer current, ActivityQuery activityQuery) { ... }
```

---

## 19. 工具类

### 19.1 JWTUtils
**路径**: `shared/security/JWTUtils.java`

| 方法 | 功能 |
|------|------|
| `createJWT(Integer userId, String loginAct, Long authVersion, long expirationSeconds)` | 生成只携带 userId、loginAct、authVersion 和过期时间的 JWT |
| `createJWT(Integer userId, String loginAct, long expirationSeconds)` | 兼容旧调用，生成不含 authVersion 声明的 JWT |
| `verifyJWT(String jwt)` | 验证 JWT 签名是否有效 |
| `parseUserIdFromJWT(String jwt)` | 从 JWT 解析用户 ID |
| `parseLoginActFromJWT(String jwt)` | 从 JWT 解析登录账号 |
| `parseAuthVersionFromJWT(String jwt)` | 从 JWT 解析认证版本；旧 JWT 缺少该声明时返回 null |

**密钥**: 从环境变量 `JWT_SECRET` 获取；未配置时应用启动失败，避免使用可预测的默认签名密钥。

### 19.2 CacheUtils
**路径**: `shared/infrastructure/cache/CacheUtils.java`

| 方法 | 功能 |
|------|------|
| `getCacheData(Supplier cacheSelector, Supplier databaseSelector, Consumer cacheSave)` | 通用缓存查询：先查缓存，未命中查数据库并缓存 |
| `generateKey(Object... params)` | 生成缓存 key |

### 19.3 JSONUtils
**路径**: `shared/infrastructure/json/JSONUtils.java`

| 方法 | 功能 |
|------|------|
| `toJSON(Object object)` | Java 对象转 JSON 字符串 |
| `toBean(String json, Class<T> clazz)` | JSON 字符串转 Java 对象 |

### 19.4 ResponseUtils
**路径**: `shared/web/ResponseUtils.java`

| 方法 | 功能 |
|------|------|
| `write(HttpServletResponse response, String result)` | 将 JSON 结果写入 HttpServletResponse |

### 19.5 RedisManager
**路径**: `shared/infrastructure/cache/RedisManager.java`

| 方法 | 功能 |
|------|------|
| `get(String key)` | 获取缓存值 |
| `set(String key, Object value, long seconds)` | 设置缓存值（带过期时间） |
| `delete(String key)` | 删除缓存 |
| `deletePattern(String pattern)` | 模式匹配删除缓存 |

---

## 20. 数据字典缓存机制

### 20.1 缓存架构

```
DicController
    ↓
DicServiceImpl
    ↓
CacheUtils.getCacheData()
    ↓
RedisManager (Redis)  ←→  DicMapper (MySQL)
```

### 20.2 缓存策略

| 缓存 Key 模式 | 内容 | 过期时间 |
|---------------|------|---------|
| `cdrm:dict:type:{id}` | 字典类型详情 | 24 小时 |
| `cdrm:dict:type:code:{typeCode}` | 按类型代码查询 | 24 小时 |
| `cdrm:dict:value:{id}` | 字典值详情 | 24 小时 |
| `cdrm:dict:values:type:{typeId}` | 按类型ID查询启用字典值列表 | 24 小时 |

### 20.3 缓存刷新逻辑

#### refreshTypeCache()
```java
1. 删除所有 dic:type:* 缓存
2. 查询所有字典类型
3. 遍历写入 Redis：`cdrm:dict:type:code:{typeCode}` → TDicType
```

#### refreshValueCache()
```java
1. 删除所有 dic:value:* 缓存
2. 查询所有字典值
3. 遍历写入 Redis：`cdrm:dict:value:{id}` → TDicValue
```

#### clearCache()
```java
删除所有 `cdrm:dict:type:*`、`cdrm:dict:value:*`、`cdrm:dict:values:type:*` 缓存
```

### 20.4 Excel 导入时的数据解析

Excel 导入不再使用启动期全局 `cacheMap` 和 Converter 直接落库。当前流程由 `ClueServiceImpl.importExcel()` 使用 EasyExcel 读取 `ClueExcelRaw`，再由 `ClueImportValidator` 一次性加载字典、负责人、活动和商品映射，逐行做公式前缀拦截、手机号归一化、必填校验、字典转换、同文件查重和对象转换。

导入结果使用 `ImportResult` 返回总行数、有效行数、失败行数、成功导入行数和行级错误。存在失败行时接口返回 HTTP 422，但合法行可以已经导入成功，前端必须展示 `importedCount` 和错误明细。

---

## 21. Mapper XML SQL 汇总

### 21.1 TClueMapper.xml (t_clue)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectClueByPage` | SELECT | 线索分页查询（多表关联） |
| `selectByCount` | SELECT | 按手机号查重 |
| `selectClueByCount` | SELECT | 按当前数据范围统计线索总数 |
| `selectDetailById` | SELECT | 线索详情（多表关联） |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectScopedByPrimaryKey` | SELECT | 按主键和当前数据范围查询 |
| `selectBySource` | SELECT | 按当前数据范围和来源分组统计（饼图） |
| `deleteByPrimaryKey` | DELETE | 删除线索 |
| `saveClue` | INSERT | 批量保存线索（Excel 导入） |
| `insert` | INSERT | 插入线索 |
| `insertSelective` | INSERT | 选择性插入线索 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新线索 |
| `updateByPrimaryKey` | UPDATE | 全字段更新线索 |
| `batchDeleteByIds` | DELETE | 批量删除线索 |
| `updateStateToConverted` | UPDATE | 原子标记线索已转客户 |
| `updateOwnerAtomic` | UPDATE | 原子转派线索负责人 |
| `updateStateAtomic` | UPDATE | 原子迁移线索状态并校验旧状态 |
| `selectExistingPhones` | SELECT | 导入前批量查询数据库已有手机号 |
| `countActiveByPhoneExcludingId` | SELECT | 恢复线索前检查相同手机号活跃线索 |
| `countByIntentionProductId` | SELECT | 检查商品是否被线索意向引用 |

### 21.2 TCustomerMapper.xml (t_customer)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectCustomerPage` | SELECT | 客户分页查询（多表关联） |
| `selectCustomerByExcel` | SELECT | 导出 Excel 查询 |
| `selectByCount` | SELECT | 按当前数据范围统计客户总数 |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByQuery` | SELECT | 按条件查询客户 |
| `selectCustomerOptions` | SELECT | 客户选项（下拉框） |
| `countActiveDuplicateContacts` | SELECT | 有效联系方式重复检查 |
| `selectVisibleDuplicateSummaries` | SELECT | 可见范围内重复客户安全摘要 |
| `updateOwnerAtomic` | UPDATE | 原子转移客户负责人 |
| `markMerged` | UPDATE | 标记被合并客户 |
| `reassignCustomerRemarks` | UPDATE | 合并时迁移客户跟进 |
| `reassignTransactions` | UPDATE | 合并时迁移交易 |
| `reassignQuotes` | UPDATE | 合并时迁移报价 |
| `countBusinessReferences` | SELECT | 删除前业务引用计数 |
| `deleteByPrimaryKey` | DELETE | 删除客户 |
| `insert` | INSERT | 插入客户 |
| `insertSelective` | INSERT | 选择性插入客户 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新客户 |
| `updateByPrimaryKey` | UPDATE | 全字段更新客户 |

### 21.3 TTranMapper.xml (t_tran)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByQuery` | SELECT | 交易列表查询（多表关联+动态条件） |
| `selectBySuccessTranAmount` | SELECT | 成功交易总额 |
| `selectByTotalTranAmount` | SELECT | 总交易额 |
| `selectByTotalTranCount` | SELECT | 交易客户数 |
| `selectBySuccessTranCount` | SELECT | 成交客户数 |
| `selectByPrimaryKey` | SELECT | 交易详情 |
| `selectTranProductsByTranId` | SELECT | 交易产品历史快照列表 |
| `selectTranWithApproveByTranId` | SELECT | 交易+审批联合查询 |
| `deleteByPrimaryKey` | DELETE | 删除交易 |
| `deleteByIds` | DELETE | 批量删除交易 |
| `insert` | INSERT | 插入交易 |
| `insertSelective` | INSERT | 选择性插入交易 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新交易 |
| `updateByPrimaryKey` | UPDATE | 全字段更新交易 |

### 21.4 TUserMapper.xml (t_user)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByLoginAct` | SELECT | 按登录账号查询（登录用） |
| `selectManagedUserPage` | SELECT | 受管用户工作台分页；使用服务端数据范围、筛选和排序白名单 |
| `selectRoleNamesByUserIds` | SELECT | 批量加载列表角色摘要，避免逐用户查询 |
| `selectVisibleOrganizationOptions` / `selectVisiblePositionOptions` / `selectVisibleManagerOptions` / `selectVisibleRoleOptions` | SELECT | 返回当前操作者可见的筛选候选 |
| `selectAuthUserById` | SELECT | 加载认证和账号安全权威事实 |
| `selectEligibleOwners` | SELECT | 按账号、任职、权限和业务动作筛选负责人候选 |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `incrementAuthVersion` / `incrementAuthorizationVersionsByExpected` | UPDATE | 提升认证安全版本或授权配置版本并校验预期行数 |
| `incrementSessionRevisionByExpected` | UPDATE | 会话列表命令的独立 CAS 版本 |
| `updateAccountStatusByExpected` / `updateManualLockByExpected` | UPDATE | 受管账号状态和人工锁定 CAS 更新 |
| `updateLoginActByExpected` | UPDATE | 按账号版本更新当前登录账号并提升认证安全版本；必须与登录标识历史事务配套 |
| `updateSecurityExpirationByExpected` | UPDATE | 独立维护账号到期与凭证到期时间并提升认证安全版本 |
| `updateProfileProjection` / `updateSystemProfileByVersion` | UPDATE | 维护员工资料兼容投影或系统账号资料版本 |
| `deleteByPrimaryKey` | DELETE | 旧物理删除方法，不用于离职处理 |
| `deleteByIds` | DELETE | 旧批量物理删除方法，不用于离职处理 |
| 旧 `disableById`、三域 `selectOwned*` / `transferOwned*`、`deleteUserRoles` 等 | LEGACY | 仅供旧兼容实现留存；受控用户写入口和生命周期流程不得调用，旧 Controller 路径 fail-close |

#### TLoginIdentifierMapper.xml（登录账号永久归属）

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByLoginActForUpdate` | SELECT FOR UPDATE | 按规范化登录账号锁定当前或退休归属事实 |
| `selectActiveByUserIdForUpdate` | SELECT FOR UPDATE | 锁定用户唯一当前登录标识 |
| `insert` | INSERT | 为新账号建立不可转让的 ACTIVE 归属事实 |
| `retireByExpected` | UPDATE | 按标识版本退休旧账号，保留永久历史 |
| `reactivateByExpected` | UPDATE | 只允许原用户按标识版本重新启用自己的退休账号 |

#### TUserLifecycleMapper.xml（生命周期与六域交接）

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `lockEmployeeByUserId` / `lockUserById` | SELECT FOR UPDATE | 锁定生命周期目标员工和账号事实 |
| `selectActivities` / `selectClues` / `selectCustomers` | SELECT | 按精确状态谓词加载活动、线索、客户直接责任快照 |
| `selectOpportunities` / `selectFollowTasks` / `selectTestDrives` | SELECT | 按状态和版本加载商机、跟进任务、试驾直接责任快照 |
| `transferActivity` / `transferClue` / `transferCustomer` | UPDATE | 按 ID、原负责人和原状态逐项转移无版本直接域 |
| `transferOpportunity` / `transferFollowTask` / `transferTestDrive` | UPDATE | 按 ID、原负责人、原状态和版本逐项 CAS 转移 |
| `countActiveQuotesByOwner` / `countActiveTransactionsByOwner` | SELECT | 只核验随 Customer 归属派生的 Quote、Tran 影响，不直接更新 |
| `selectLifecycleFacts` | SELECT | 生成离职快照的精确责任、任职、汇报、授权、会话事实指纹 |
| `insertSnapshot` / `lockSnapshotByDigest` / `consumeSnapshot` | INSERT/SELECT/UPDATE | 保存并一次性消费有限期离职预检摘要 |
| `insertEvent` / `selectHistoryRows` | INSERT/SELECT | 写入不可变生命周期事件并作为用户历史第三来源读取 |

### 21.5 TActivityMapper.xml (t_activity)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectActivityByPage` | SELECT | 活动分页查询（支持数据权限） |
| `selectDetailByPrimaryKey` | SELECT | 活动详情 |
| `selecOngoingActivity` | SELECT | 进行中的活动 |
| `selectByCount` | SELECT | 活动总数 |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `deleteByPrimaryKey` | DELETE | 删除活动 |
| `batchDeleteByIds` | DELETE | 批量删除活动 |
| `insert` | INSERT | 插入活动 |
| `insertSelective` | INSERT | 选择性插入活动 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新活动 |
| `updateByPrimaryKey` | UPDATE | 全字段更新活动 |

### 21.6 TProductMapper.xml (t_product)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectList` | SELECT | 产品列表 |
| `selectCount` | SELECT | 产品总数 |
| `selectById` | SELECT | 按ID查询 |
| `selectBySku` | SELECT | 按SKU查询 |
| `selectStockAlerts` | SELECT | 库存预警列表 |
| `selectStockAlertsWithFilter` | SELECT | 带过滤的库存预警 |
| `selectStockAlertsCount` | SELECT | 库存预警数量 |
| `selectAllOnSale` | SELECT | 所有在售产品 |
| `insert` | INSERT | 插入产品 |
| `update` | UPDATE | 更新产品 |
| `deleteById` | DELETE | 删除产品 |
| `updateStock` | UPDATE | 更新库存（支持正负数） |

### 21.7 DicMapper.xml (t_dic_type, t_dic_value)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectDicTypes` | SELECT | 字典类型列表 |
| `selectDicValues` | SELECT | 字典值列表 |
| `selectDicTypeById` | SELECT | 按ID查询字典类型 |
| `selectDicValueById` | SELECT | 按ID查询字典值 |
| `selectDicTypeByCode` | SELECT | 按类型代码查询 |
| `selectDicValuesByTypeId` | SELECT | 按类型ID查询字典值 |
| `selectDicValueIdsByTypeCode` | SELECT | 按类型代码获取字典值ID |
| `selectTypeCodeById` | SELECT | 按ID获取类型代码 |
| `selectTypeCodesByIds` | SELECT | 批量获取类型代码 |
| `insertDicType` | INSERT | 插入字典类型 |
| `insertDicValue` | INSERT | 插入字典值 |
| `updateDicType` | UPDATE | 更新字典类型 |
| `updateDicValue` | UPDATE | 更新字典值 |
| `deleteDicType` | DELETE | 删除字典类型 |
| `deleteDicValue` | DELETE | 删除字典值 |
| `deleteDicTypesByIds` | DELETE | 批量删除字典类型 |
| `deleteDicValuesByIds` | DELETE | 批量删除字典值 |
| `selectRemarkCountByDicValueIds` | SELECT | 统计交易备注、线索备注、客户备注、活动备注、线索主档和客户主档引用 |

### 21.8 TTranProductMapper.xml (t_tran_product)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询产品列表 |
| `deleteByPrimaryKey` | DELETE | 删除 |
| `deleteByTranId` | DELETE | 按交易ID删除 |
| `insert` | INSERT | 插入 |
| `insertSelective` | INSERT | 选择性插入 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新 |
| `updateByPrimaryKey` | UPDATE | 全字段更新 |

### 21.9 TTranInvoiceMapper.xml (t_tran_invoice)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询发票列表 |
| `deleteByPrimaryKey` | DELETE | 删除发票 |
| `insert` | INSERT | 插入发票 |
| `insertSelective` | INSERT | 选择性插入发票 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新发票 |
| `updateByPrimaryKey` | UPDATE | 全字段更新发票 |

### 21.10 TTranApproveMapper.xml (t_tran_approve)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询审批记录 |
| `insert` | INSERT | 插入审批记录 |
| `insertSelective` | INSERT | 选择性插入审批记录 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新 |
| `updateByPrimaryKey` | UPDATE | 全字段更新 |

### 21.11 TTranRemarkMapper.xml (t_tran_remark)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询备注列表 |
| `deleteByPrimaryKey` | DELETE | 删除备注 |
| `deleteByTranId` | DELETE | 按交易ID删除所有备注 |
| `insert` | INSERT | 插入备注 |
| `insertSelective` | INSERT | 选择性插入备注 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新 |
| `updateByPrimaryKey` | UPDATE | 全字段更新 |

### 21.12 审计日志 Mapper

| Mapper | SQL ID | 类型 | 用途 |
|--------|--------|------|------|
| `TLoginLogMapper.xml` | `insert` | INSERT | 写入登录记录 |
| `TLoginLogMapper.xml` | `selectByQuery` | SELECT | 按过滤条件分页查询登录记录 |
| `TLoginLogMapper.xml` | `selectById` | SELECT | 查询登录记录详情 |
| `TLoginLogMapper.xml` | `selectForExport` | SELECT | 按过滤条件导出登录记录 |
| `TOperationLogMapper.xml` | `insert` | INSERT | 写入操作记录 |
| `TOperationLogMapper.xml` | `selectByQuery` | SELECT | 按过滤条件分页查询操作记录 |
| `TOperationLogMapper.xml` | `selectById` | SELECT | 查询操作记录详情 |
| `TOperationLogMapper.xml` | `selectForExport` | SELECT | 按过滤条件导出操作记录 |

## 22. AI 业务助手

AI 业务助手已落地为 Spring Boot + 独立 `dealer-ai` 的旁路增强架构。普通业务页面和普通业务 API 的主链路不依赖 AI，AI 只能通过 Spring Boot 受控入口查询、生成低风险 Proposal、展示受控工作流和生成主动提醒。

演示环境由 Docker Compose 统一托管 `dealer-server` 和 `dealer-ai`。Spring Boot 通过容器内网地址 `http://ai:8091` 调用编排服务，`dealer-ai` 不向宿主机发布端口；启动脚本必须等 `/ready` 健康检查通过后才能报告成功。单独运行 uvicorn 仅用于本地调试，不是演示环境的正式启动入口。

### 22.1 Spring Boot 模块

| 类型 | 落点 | 说明 |
|------|------|------|
| Controller | `modules/ai/web/AiRunController.java` | 创建 AI Run、查询 Run、查询可恢复追踪、输出 Spring Boot SSE |
| Controller | `modules/ai/web/AiInternalToolController.java` | `dealer-ai` 内部调用 ToolRegistry，使用 `X-Dealer-AI-Tool-Token` |
| Controller | `modules/ai/web/AiProposalController.java` | 确认或拒绝低风险 Proposal |
| Controller | `modules/ai/web/AiWorkflowController.java` | 创建、查询、暂停、恢复、取消、完成和失败处理受控工作流 |
| Controller | `modules/ai/web/AiProactiveController.java` | 创建、查询、暂停、恢复、取消主动提醒订阅，并查询或生成当前用户提醒事件 |
| Controller | `modules/ai/web/AiProviderConfigController.java` | 管理员创建、编辑、测试、启用、停用和轮换模型 Provider 配置 |
| Controller | `modules/ai/web/AiAssistantPolicyController.java` | 管理员查看和更新工具、安全、联网、上下文和运行时限策略 |
| Registry | `modules/ai/application/api/tool/ToolRegistry.java` | Spring Boot 最终工具白名单、权限、风险等级和审计边界 |
| Trace Service | `modules/ai/application/api/AiTraceService.java` | 写入 AI Run、Message、ToolCall、Proposal、Approval 和 ExecutionEvent |
| Provider Service | `modules/ai/application/api/AiProviderConfigService.java` | 加密保存 API Key、生成运行时 Provider 配置、执行连接测试和启用互斥 |
| Policy Service | `modules/ai/application/api/AiAssistantPolicyService.java` | 保存全局策略并用乐观锁防止管理员并发覆盖 |
| Run Event Store | `modules/ai/application/internal/AiRunEventStore.java` | 持久化脱敏 SSE 事件并按 Run 序号重放 |
| Proposal Service | `modules/ai/application/api/AiProposalService.java` | 保存规范化参数、参数哈希、影响说明和过期时间，并在确认时执行已保存参数 |
| Workflow Service | `modules/ai/application/api/AiWorkflowService.java` | 持久化工作流、步骤状态和工作流控制事件 |
| Proactive Service | `modules/ai/application/api/AiProactiveService.java` | 按当前用户权限和订阅规则生成主动提醒事件 |
| Tool Executors | `ai/tool/executor/*` | 第一批只读工具和两个低风险 Proposal 工具 |

业务枚举、状态、类型和值以 Spring Boot Java enum、后端 DTO 校验和 OpenAPI 契约为准。`dealer-ai` 只能生成后端已支持的值，不能在 Python 编排、Prompt 或 Pydantic 默认值中临时创造 CRM 业务值。

模型 Provider 配置真源在 Spring Boot。API Key 使用 AES-GCM 加密入库，响应只返回掩码；`providerRuntimeConfig` 只在 Spring Boot 到 `dealer-ai` 的服务间请求中出现，不进入前端、SSE、trace 或日志。`prod` 等非本地环境必须显式配置 `AI_PROVIDER_KEY_ENCRYPTION_SECRET`；`local`、`dev`、`test`、`smoke` 环境未配置时，后端自动生成并复用 `~/.car-dealer-crm/ai-provider-key.secret`。Compose 将该目录挂载到 `server-ai-secret` 命名卷；启动脚本只在卷内尚无密钥时迁移旧容器密钥，清理该卷后旧 Provider API Key 密文需要重新录入。

### 22.2 已实现工具

- `list_my_followups`
- `search_customers`
- `get_customer_profile`
- `resolve_vehicle_product`
- `get_inventory_alerts`
- `get_transaction_detail`
- `list_pending_transaction_approvals`
- `get_opportunity_detail`
- `get_quote_detail`
- `get_test_drive_detail`
- `get_delivery_detail`
- `get_business_overview`
- `create_communication_record_proposal`
- `create_follow_task_proposal`

`get_inventory_alerts` 复用 `ProductService.getStockAlerts`，不落到 `StatisticService`。

### 22.3 AI 追踪表

AI 追踪独立于业务操作审计。`dealer-ai` 不直接写数据库，所有 AI 追踪写入由 Spring Boot 控制。

| 表名 | 说明 |
|------|------|
| `t_ai_run` | AI Run 元数据和状态 |
| `t_ai_run_event` | 可按序号重放的脱敏 Run SSE 事件 |
| `t_ai_assistant_policy` | 全局工具、安全、联网、上下文和运行时限策略 |
| `t_ai_message` | 用户、助手、系统和工具的安全展示文本、修订状态和上下文标记 |
| `t_ai_tool_call` | 工具调用输入摘要、输出摘要、权限、风险和结果 |
| `t_ai_action_proposal` | 低风险 Proposal、规范化参数、参数哈希、影响说明和过期时间 |
| `t_ai_approval` | 用户确认、拒绝或过期记录；确认语义使用 `CONFIRMED` |
| `t_ai_workflow` | 受控工作流元数据、状态、当前步骤、上下文对象和过期时间 |
| `t_ai_workflow_step` | 工作流步骤状态、工具、Proposal 引用、输入输出摘要和错误码 |
| `t_ai_execution_event` | Proposal 执行事件、工作流控制事件和结果摘要 |
| `t_ai_proactive_subscription` | 当前用户主动提醒订阅、频率、静默时间、数量上限和重复合并窗口 |
| `t_ai_proactive_event` | 主动提醒事件、摘要、对象引用、严重程度、生成结果和失败码 |

Run trace 查询必须恢复 messages、toolCalls、proposals、approvals、workflows 和 executionEvents。ToolCall 成功和失败都写入脱敏摘要；成功工具调用还必须保存脱敏展示 payload，用于前端刷新、切换会话和展开独立页面后恢复业务卡片。Proposal 的确认、拒绝、过期、权限变化、哈希不一致、业务执行成功和业务执行失败都必须可追踪。业务写操作仍以现有操作审计证明业务事实变更，AI 追踪不能替代操作审计。

### 22.4 受控工作流与主动提醒

受控工作流只允许编排 Spring Boot 下发的只读工具和低风险 Proposal。工作流状态覆盖 `CREATED`、`RUNNING`、`PAUSED`、`WAITING_USER_CONFIRMATION`、`COMPLETED`、`FAILED`、`CANCELLED` 和 `EXPIRED`；步骤状态覆盖 `PENDING`、`RUNNING`、`WAITING_USER_CONFIRMATION`、`COMPLETED`、`FAILED`、`CANCELLED` 和 `EXPIRED`。暂停、恢复、取消和失败处理都由 Spring Boot 校验 run owner、当前状态、权限和数据范围；完成状态只能由后端运行结果或 LangGraph 事件推动。

主动提醒生成在 Spring Boot 内完成，不修改 `dealer-ai`。生成前恢复订阅 owner 的权限和数据范围，重新校验用户启用状态、订阅状态、频率、数量上限、静默时间和重复合并窗口。库存预警复用 `ProductService.getStockAlerts`；能力不足时只能新增只读查询或 AI 主动提醒适配器，不改变普通业务接口、统计口径、事务语义或权限语义。

### 22.5 组织、岗位、任职与汇报关系

组织管理由 `OrganizationController`、`OrganizationServiceImpl` 和组织专用 Mapper 组成。组织、岗位和员工版本字段用于 CAS；组织或岗位 code 创建后保持稳定。停用组织前必须检查有效下级和在职员工，停用岗位前必须检查有效任职。

员工组织更新把任职和汇报关系视为两个独立权限分区：只变更任职时校验 `employee:assignment`，只变更汇报关系时校验 `employee:reporting`，未变化分区不得被关闭重建。直属、代理关系不从角色或组织负责人推导；负责人仅是组织属性。本人、受保护账号、同级、上级和跨组织目标均由 Service 的对象级范围校验拒绝，前端候选列表不能替代后端校验。

全局组织范围包括受保护系统安全管理员和当前有效 `admin` 角色。普通管理者必须同时满足有效直接或间接汇报链与组织祖先范围；调整任职时新组织也必须处于操作者范围。岗位是全局目录，新增、编辑和启停仅允许全局组织管理员。

任职、汇报和组织目录变化在同一事务写入 `t_authorization_history` 与 `t_operation_log`。授权历史的操作者、发生时间和请求标识由统一审计组件从当前认证、可信时钟和请求上下文覆盖，业务调用方传值不受信任。

### 22.6 用户管理工作台

用户工作台由 `ManagedUserAccountService` 聚合账号、员工、当前主任职、直属管理者和有效角色。列表数据范围在 SQL 查询前通过 `DataScopeResolver(user:list)` 解析，Mapper 只接受服务端白名单排序列并始终追加用户主键，摘要不得投影密码、凭证、手机号或邮箱。详情把账号、资料、员工、授权和会话版本分开返回；联系方式和锁定原因还需同时满足 `user:sensitive:view` 与目标管理关系。

状态写命令只接受 `ENABLE`、`DISABLE`、`LOCK`、`UNLOCK`、账号版本和必填原因。人工锁定与登录失败自动锁定是独立事实。禁用或锁定管理员前必须先锁定 `AVAILABLE_ADMIN_GUARD` 共享数据库行，锁内重新加载目标、核对版本并重新计算有效管理员数量，防止不同管理员并发降级后系统失去最后管理员。

邀请创建在同一事务创建账号、员工档案、主任职、直属汇报、初始角色、不可变角色授权历史、操作审计、邀请凭证摘要和投递 Outbox。初始角色候选由委派策略按所选组织计算，和列表筛选使用的“可见角色事实”分离；任一角色越级、审计、摘要或 Outbox 写入失败都整体回滚。接口返回 HTTP 202/`QUEUED`，提交后 Worker 才派生和投递原始凭证；响应不返回密码、Token、凭证明文、摘要或派生 nonce。

人员生命周期由 `UserLifecycleServiceImpl` 统一编排调岗、离职预检、进入待交接、确认交接、完成离职和返聘。直接责任域固定为 Activity、Clue、Customer、Opportunity、FollowTask、TestDrive；Quote、Tran 只通过 Customer 归属做派生核验，待审批队列和 CommunicationRecord 历史不得改写。离职预检把责任 ID、状态、版本、任职、汇报、授权和会话事实生成摘要保存到 `t_user_lifecycle_snapshot`，确认时在图锁和业务锁内重新核对并一次性消费。成功动作写入 `t_user_lifecycle_event` 和操作审计。

用户历史由 `UserHistoryService` 只读聚合三个来源：`t_authorization_history`、动作白名单内的 `t_operation_log` 安全投影，以及 `t_user_lifecycle_event`。`GET /api/users/{id}/history` 必须同时具备 `audit:operation:detail` 和目标用户管理范围；本人、跨范围和受保护账号拒绝查询。投影按发生时间、来源和主键稳定排序，支持动作与时间过滤后分页；操作者、角色、权限、组织、岗位、管理者及角色矩阵受影响用户使用事件时快照，不在查询时关联当前成员重建历史。响应不选择操作日志 IP，不返回原始 detail，并在后端统一清理密码、摘要、Token、完整联系方式和网络地址。

审计 `requestId` 由 `AuditRequestIdProvider` 统一生成：外部请求头仅作为经清理的相关性前缀，不能成为最终可信标识；同一 HTTP 请求或同一非 HTTP 事务复用一个可信标识，事务结束后解除线程资源绑定。授权事实和配套操作审计写入失败时必须使业务事务回滚。

### 22.7 用户管理数据库迁移治理

用户管理升级不由应用启动流程自动执行，也不允许直接运行单份业务 SQL。唯一入口是 `scripts/database/user-management-migrate.sh`，迁移顺序、依赖、脚本 SHA-256、发布状态、恢复模式和对象 probe 由 `dealer-server/src/main/resources/migration/manifest.tsv` 管理。

- `plan/status` 只展示计划和账本状态；`apply APPLY` 执行尚无账本事实的迁移；`resume <migration_key> RESUME` 只恢复 `RUNNING/FAILED` 且 checksum 未漂移的同一迁移。
- `baseline BASELINE` 仅用于已由完整初始化脚本建立的数据库，必须逐项通过 manifest probe 后才能绑定 checksum；不能伪造业务迁移执行事实。
- 执行器使用数据库命名锁串行化执行，并要求脚本在业务 DDL、约束、种子和回填过程内再次验证当前连接持锁、迁移键、checksum 与 `RUNNING` 账本。Task10 的不可变审计触发器因 MySQL/MariaDB 方言限制保留为过程外唯一例外。
- `t_user_management_migration` 保存 `RUNNING/SUCCEEDED/FAILED`、checksum、开始/完成/失败时间、最后完成步骤、尝试次数、错误摘要和执行器版本；`t_user_management_migration_step` 保存可恢复步骤。只有脚本成功、对象 probe 通过且成功状态影响一行时才能标记完成。
- Task15 在受控 context 内新增 `account_expires_at`、登录标识表和 `LOGIN_IDENTIFIER_GUARD`，首次把全部现有 `t_user.login_act` 回填为 ACTIVE 永久归属。空账号或既有归属冲突必须中止；`LOGIN_IDENTIFIER_BACKFILL_READY` 已记录后不得用重放回填虚构丢失的历史。
- 真实数据库验收使用 `scripts/database/test-user-management-migrations-real.sh`，覆盖旧库首跑、中断恢复、重复执行、完整初始化库 baseline、不可变触发器和 `mysql --force` 防绕过；H2 或静态契约不能替代 MySQL/MariaDB 方言结果。

## 23. 数据库表汇总

角色权限目录由 `RoleAccessController`/`RoleAccessServiceImpl` 维护。权限目录只读，角色 code 创建后不可修改，角色和权限父对象不提供物理删除。角色矩阵采用版本 CAS 和原子替换；普通管理者只能新增自己当前有效拥有且可委派的权限，并必须覆盖角色全部适用组织和全部有效成员。目录变化与矩阵变化分别写 `ROLE_CATALOG_CHANGE` 和 `ROLE_MATRIX_CHANGE` 审计。影响有效授权的角色、状态、范围或矩阵变化会在事务内递增全部受影响用户 `auth_version`，提交后再清理 Redis 登录缓存。

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `t_user` | 当前登录账号、安全状态及兼容资料投影 | id, login_act, login_pwd, account_type, protected_account, account_status, manual_locked, auto_locked_until, account_expires_at, password_expires_at, auth_version, authorization_version, profile_version, session_revision, version |
| `t_login_identifier` | 当前及退休登录账号的永久归属历史 | id, user_id, login_act, status, active_marker, retired_at, changed_by, reason, version, create_time |
| `t_role` | 角色目录与委派上限 | id, role, role_name, protected_role, authorization_level, default_data_scope, scope_type, enabled, version |
| `t_user_role` | 可保留历史的用户角色事实 | id, user_id, role_id, granted_by, reason, effective_from, effective_to, active_marker, version |
| `t_permission` | 只读权限目录 | id, name, code, type, module, sensitivity_level, delegable, enabled, version |
| `t_role_permission` | 角色权限动作及运行时数据范围 | role_id, permission_id, delegable, data_scope_code |
| `t_role_organization` | 组织级角色适用组织 | role_id, organization_unit_id |
| `t_role_permission_organization` | 角色权限 `CUSTOM_ORGS` 指定组织 | role_id, permission_id, organization_unit_id |
| `t_organization_unit` | 组织树当前态 | id, code, type, parent_id, leader_employee_id, enabled, version |
| `t_position` | 岗位目录当前态 | id, code, position_level, built_in, enabled, version |
| `t_employee` | 员工档案、任职状态与独立资料版本 | id, user_id, employee_no, employment_status, hire_date, leave_date, profile_version, version |
| `t_employee_assignment` | 主要、兼任和代理任职事实 | id, employee_id, organization_unit_id, position_id, assignment_type, status, active_primary_marker, effective_from, effective_to, version |
| `t_employee_reporting` | 直属和代理汇报事实 | id, subordinate_employee_id, manager_employee_id, relation_type, status, active_direct_marker, effective_from, effective_to, version |
| `t_user_permission` | 用户个人 `GRANT/DENY` 当前事实 | id, user_id, permission_id, effect, data_scope_code, effective_from, effective_to, active_marker, version |
| `t_user_permission_organization` | 个人权限 `CUSTOM_ORGS` 指定组织 | user_permission_id, organization_unit_id |
| `t_authorization_history` | 不可变授权与组织变化历史 | subject_type, subject_id, change_type, target_user_id, role_id, permission_id, before_value, after_value, operator_id, occurred_time, request_id, affected_user_ids, affected_users_snapshot |
| `t_authorization_graph_lock` | 组织、汇报、授权成员、管理员保护、登录标识和试驾排期共享锁行 | lock_name |
| `t_account_credential` | 邀请、找回和管理员重置的单次凭证摘要 | id, user_id, purpose, token_digest, status, active_marker, expires_at, consumed_at, revoked_at, version |
| `t_password_history` | 不可变密码历史摘要 | id, user_id, password_hash, changed_by, change_reason, changed_at |
| `t_user_session` | 多设备数据库会话及撤销事实 | id, session_id, user_id, token_digest, issued_auth_version, last_activity_time, idle_expires_at, absolute_expires_at, revoked_at, version |
| `t_user_lifecycle_snapshot` | 一次性离职预检摘要 | id, token_digest, user_id, employee_id, employee_version, reason_digest, fact_digest, expires_at, consumed_at, version |
| `t_user_lifecycle_event` | 不可变调岗、离职、交接和返聘事件 | id, operation_id, request_id, action, user_id, employee_id, before_value, after_value, reason, operator_id, occurred_time |
| `t_user_management_migration` | 用户管理统一迁移账本 | migration_key, status, checksum_sha256, started_at, completed_at, failed_at, last_completed_step, attempt_count, error_summary, executor_version |
| `t_user_management_migration_step` | 用户管理迁移已完成步骤 | migration_key, step_code, completed_at |
| `t_clue` | 线索表 | id, owner_id, activity_id, activity_name_snapshot, full_name, phone, state, source |
| `t_clue_remark` | 线索跟踪记录表 | id, clue_id, note_way, note_content |
| `t_customer` | 客户主档表 | id, clue_id, owner_id, customer_name, phone, source, original_clue_source, customer_status, product, description |
| `t_customer_owner_history` | 客户归属转移历史表 | id, customer_id, from_owner_id, to_owner_id, reason, operator_id, transfer_time |
| `t_customer_remark` | 客户跟踪记录表 | id, customer_id, note_way, note_content |
| `t_opportunity` | 商机主表 | id, opportunity_no, customer_id, owner_id, product_id, stage, requirement, expected_amount, expected_close_date, order_tran_id |
| `t_opportunity_stage_history` | 商机阶段历史表 | id, opportunity_id, from_stage, to_stage, reason, operate_by, operate_time |
| `t_test_drive` | 试驾预约与执行表 | id, test_drive_no, customer_id, opportunity_id, vehicle_id, owner_id, planned_start_time, planned_end_time, status |
| `t_test_drive_vehicle_hold` | 试驾车辆时间占用表 | id, test_drive_id, vehicle_id, start_time, end_time, status, release_reason |
| `t_test_drive_status_history` | 试驾状态历史表 | id, test_drive_id, from_status, to_status, action_type, reason, operate_by, operate_time |
| `t_tran` | 交易表 | id, tran_no, customer_id, money, stage |
| `t_tran_product` | 交易产品关联表 | id, tran_id, product_id, quantity, price, product_sku, product_name, product_specification, guide_price |
| `t_tran_invoice` | 交易发票表 | id, tran_id, invoice_no, amount, status |
| `t_tran_approve` | 交易审批表 | id, tran_id, approve_result, approve_comment |
| `t_tran_remark` | 交易跟踪记录表 | id, tran_id, note_way, note_content |
| `t_activity` | 市场活动表 | id, owner_id, name, status, channel, target_model, start_time, end_time, cost, actual_cost |
| `t_activity_remark` | 活动备注表 | id, activity_id, note_content |
| `t_product` | 产品表 | id, sku, name, category, price, stock, min_stock, status |
| `t_product_category` | 产品分类表 | id, name, code, description, sort, status |
| `t_product_promotion` | 产品促销表 | id, product_id, code, name, type, discount, rule_summary, applicable_store, customer_type, applicable_channel, inventory_scope, stackable, budget_limit, used_budget, usage_limit, used_count, start_time, end_time, status |
| `t_product_promotion_usage` | 促销使用流水表 | id, promotion_id, source_type, source_id, discount_amount, create_time, create_by |
| `t_product_stock_record` | 库存变动记录表 | id, product_id, quantity, type, remark |
| `t_dic_type` | 字典类型表 | id, type_code, type_name, remark |
| `t_dic_value` | 字典值表 | id, type_code, type_value, order, remark |
| `t_login_log` | 登录审计日志表 | id, login_act, user_id, result, reason_code, ip, browser, os, request_id, create_time |
| `t_operation_log` | 操作审计日志表 | id, user_id, action_code, module_name, object_type, resource_id, result, detail, ip, request_id, create_time |
| `t_ai_run` | AI Run 表 | id, run_no, conversation_id, parent_run_id, turn_no, user_id, status, context_active, expires_time |
| `t_ai_run_event` | AI Run 事件表 | id, run_id, event_id, sequence_no, event_type, payload_json, occurred_time |
| `t_ai_assistant_policy` | AI 全局策略表 | id, enabled_tools, allowed_tool_names, safety_mode, network_mode, context_message_limit, version |
| `t_ai_message` | AI 消息表 | id, message_no, run_id, role, status, revision_no, included_in_context, version, content_summary |
| `t_ai_tool_call` | AI 工具调用表 | id, run_id, tool_name, permission_code, risk_level, output_summary, display_payload_json, result_status |
| `t_ai_action_proposal` | AI 动作提议表 | id, run_id, proposal_type, status, risk_level, normalized_params, params_hash |
| `t_ai_approval` | AI 确认表 | id, run_id, proposal_id, decision, result_status |
| `t_ai_workflow` | AI 受控工作流表 | id, run_id, workflow_no, workflow_type, status, current_step_no |
| `t_ai_workflow_step` | AI 工作流步骤表 | id, workflow_id, step_no, step_type, status, proposal_id |
| `t_ai_execution_event` | AI 执行事件表 | id, run_id, proposal_id, event_type, result_status |
| `t_ai_proactive_subscription` | AI 主动提醒订阅表 | id, subscription_no, user_id, subscription_type, status, frequency |
| `t_ai_proactive_event` | AI 主动提醒事件表 | id, event_no, subscription_id, event_type, status, summary |

---

## 附录：常量定义

**路径**: `shared/infrastructure/constants/Constants.java`、`shared/infrastructure/cache/RedisKeys.java`

| 常量 | 值 | 用途 |
|------|-----|------|
| `LOGIN_URI` | `/api/login` | 登录接口 |
| `EXPIRE_TIME` | `7 * 24 * 60 * 60L` | JWT 过期时间（7天） |
| `DEFAULT_EXPIRE_TIME` | `4 * 60 * 60L` | JWT 默认过期时间（4小时） |
| `PAGE_SIZE` | `10` | 分页每页条数 |
| `CACHE_EXPIRE_TIME` | `24 * 60 * 60L` | 缓存过期时间（1天） |
| `RedisKeys.userLogin(userId)` | `cdrm:user:login:{userId}` | JWT Redis Key |
| `RedisKeys.ownerList()` | `cdrm:user:owner` | 负责人列表 Redis Key，单 value 序列化列表，300 秒 TTL |
