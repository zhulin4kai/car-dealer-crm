# Car Dealer CRM 后端业务逻辑文档

> 基于 Spring Boot + Spring Security + MyBatis + Redis 的汽车经销商CRM系统

---

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
├── web/                    # Controller 层（REST API 入口）
├── service/                # Service 接口
│   └── impl/               # Service 实现
├── mapper/                 # MyBatis Mapper 接口
├── model/                  # 实体类
├── manager/                # Manager 层（复杂业务编排）
├── query/                  # 查询参数对象
├── result/                 # 返回结果对象
├── dto/                    # 数据传输对象
├── config/                 # 配置类
│   ├── handler/            # Security Handler
│   ├── filter/             # Security Filter
│   ├── converter/          # Excel 转换器
│   └── listener/           # Excel 监听器
├── aspect/                 # AOP 切面
├── commons/                # 自定义注解
├── constant/               # 常量
└── util/                   # 工具类
```

---

## 2. 认证授权模块

### 2.1 模块概述
基于 Spring Security + JWT + Redis 的无状态认证授权体系。

### 2.2 文件路径

| 层级 | 文件路径 |
|------|----------|
| Config | `config/SecurityConfig.java` |
| Filter | `config/filter/TokenVerifyFilter.java` |
| Handler | `config/handler/MyAuthenticationSuccessHandler.java` |
| Handler | `config/handler/MyAuthenticationFailureHandler.java` |
| Handler | `config/handler/MyLogoutSuccessHandler.java` |
| Handler | `config/handler/MyAccessDeniedHandler.java` |
| Handler | `config/handler/GlobalExceptionHandler.java` |
| Model | `model/TUser.java`, `model/TRole.java`, `model/TPermission.java`, `model/TUserRole.java`, `model/TRolePermission.java` |
| Service | `service/UserService.java` → `service/impl/UserServiceImpl.java` |
| Mapper | `mapper/TUserMapper.java`, `mapper/TRoleMapper.java`, `mapper/TPermissionMapper.java`, `mapper/TUserRoleMapper.java`, `mapper/TRolePermissionMapper.java` |
| XML | `resources/mapper/TUserMapper.xml`, `resources/mapper/TRoleMapper.xml`, `resources/mapper/TPermissionMapper.xml`, `resources/mapper/TUserRoleMapper.xml`, `resources/mapper/TRolePermissionMapper.xml` |

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
   → JWTUtils.createJWT() 生成 JWT
   → RedisManager.set() 存储 JWT 到 Redis（key: cdrm:user:login:{userId}）
   → 设置过期时间：rememberMe=true → 7天，否则 4小时
   → Redis 写入成功后写入 t_login_log 成功登录记录
   → 登录审计写入成功后返回 JWT 给前端
6. Redis 写入失败 → 返回 HTTP 500 和 SYSTEM_ERROR，不返回 JWT
7. 登录审计写入失败 → 删除已写 Redis 会话，返回 HTTP 500 和 SYSTEM_ERROR，不返回 JWT
8. 登录失败 → MyAuthenticationFailureHandler → 写入失败登录记录，返回 HTTP 401 和稳定错误码
```

#### Token 验证流程（TokenVerifyFilter）
```
1. 请求进入 Filter
2. 判断是否为 /api/login 请求 → 放行
3. 从 `Authorization: Bearer <token>` 请求头读取 JWT，不接受 URL 参数或裸 token
4. 请求头缺失或 Bearer 后 token 为空 → 返回 HTTP 401 和 TOKEN_IS_EMPTY
5. JWTUtils.verifyJWT() 验证签名失败 → 返回 HTTP 401 和 TOKEN_IS_ERROR
6. JWTUtils.parseUserFromJWT() 解析用户信息
7. Redis 查询 token → 不存在返回 HTTP 401 和 TOKEN_IS_EXPIRED
8. Redis token 与请求 token 不匹配 → 返回 HTTP 401 和 TOKEN_IS_NONE_MATCH
9. 验证通过 → 设置 SecurityContext
10. 用户已被停用、锁定或删除时，必须删除 Redis 会话；删除失败返回 HTTP 500 和 SYSTEM_ERROR
```

#### 退出流程
```
1. 前端 POST /api/logout
2. MyLogoutSuccessHandler
   → 删除 Redis 中的 JWT
   → 删除成功后返回退出成功信息
   → 删除失败返回 HTTP 500 和 SYSTEM_ERROR
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
| GET /api/user/{id} | `user:view` |
| POST /api/user | `user:add` |
| PUT /api/user | `user:edit` |
| PUT /api/user/{id}/disable | `user:status` |
| PUT /api/user/{id}/enable | `user:status` |
| PUT /api/user/{id}/lock | `user:status` |
| PUT /api/user/{id}/unlock | `user:status` |
| PUT /api/users/batch-disable | `user:status` |
| PUT /api/user/{id}/roles | `user:role` |
| PUT /api/user/{id}/password | `user:password` |
| PUT /api/user/{id}/handover | `user:status` |
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
| Controller | `web/UserController.java` |
| Service | `service/UserService.java` → `service/impl/UserServiceImpl.java` |
| Mapper | `mapper/TUserMapper.java` |
| XML | `resources/mapper/TUserMapper.xml` |
| Model | `model/TUser.java` |
| Query | `query/UserQuery.java` |

### 3.2 接口方法及业务流程

#### 获取登录人信息
- **接口**: `GET /api/login/info`
- **流程**: 从 SecurityContext 获取当前用户信息

#### 免登录检测
- **接口**: `GET /api/login/free`
- **流程**: 验证 token 是否有效，返回 OK

#### 用户列表分页查询
- **接口**: `GET /api/users?page=1&size=10`
- **权限**: `@PreAuthorize("hasAuthority('user:list')")`
- **流程**: `UserController.userPage()` → `UserServiceImpl.getUserByPage()` → `TUserMapper.selectUserByPage()`
- **事务**: 无

#### 用户详情
- **接口**: `GET /api/user/{id}`
- **权限**: `@PreAuthorize("hasAuthority('user:view')")`
- **流程**: `UserController.userDetail()` → `UserServiceImpl.getUserById()` → `TUserMapper.selectDetailById()`

#### 新增用户
- **接口**: `POST /api/user`
- **权限**: `@PreAuthorize("hasAuthority('user:add')")`
- **流程**: `UserController.addUser()` → `UserServiceImpl.saveUser()`
  - `BCryptPasswordEncoder` 密码加密
  - `JWTUtils.parseUserFromJWT()` 解析创建人
  - `TUserMapper.insertSelective()` 插入
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 编辑用户
- **接口**: `PUT /api/user`
- **权限**: `@PreAuthorize("hasAuthority('user:edit')")`
- **流程**: `UserController.editUser()` → `UserServiceImpl.updateUser()`
  - 密码非空时才加密更新
  - `TUserMapper.updateByPrimaryKeySelective()` 更新
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 账号启禁用与锁定
- **接口**: `PUT /api/user/{id}/disable`、`PUT /api/user/{id}/enable`、`PUT /api/user/{id}/lock`、`PUT /api/user/{id}/unlock`
- **权限**: `@PreAuthorize("hasAuthority('user:status')")`
- **流程**: `UserController` → `UserServiceImpl.disableUser()/enableUser()/lockUser()/unlockUser()`
  - 禁用和锁定必须保护内置管理员与最后一个有效管理员。
  - 禁用前检查当前活动、线索和未合并客户责任引用；仍有引用时要求先交接。
  - 禁用、锁定、角色分配和密码修改会删除 Redis 登录会话，删除失败返回 `SYSTEM_ERROR`。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量禁用用户
- **接口**: `PUT /api/users/batch-disable`
- **权限**: `@PreAuthorize("hasAuthority('user:status')")`
- **流程**: `UserController.batchDisableUsers()` → `UserServiceImpl.batchDisableUsers()` → `TUserMapper.disableByIds()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 责任交接
- **接口**: `PUT /api/user/{id}/handover`
- **权限**: `@PreAuthorize("hasAuthority('user:status')")`
- **请求**: `HandoverUserResponsibilitiesRequest{targetUserId, reason}`
- **流程**: `UserController.handoverResponsibilities()` → `UserServiceImpl.handoverResponsibilities()`
  - 原负责人来自路径 ID，目标负责人和原因来自请求体。
  - 目标负责人必须启用、未锁定且具备销售顾问或销售经理角色。
  - 当前实现整体转移 `t_activity.owner_id`、`t_clue.owner_id`、`t_customer.owner_id`；线索和客户分别写入 `t_clue_owner_history`、`t_customer_owner_history`。
  - 每类对象更新行数必须等于转移前查询数量，不一致返回业务失败并回滚。
  - 成功后写 `USER_HANDOVER` 操作审计。
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取负责人列表
- **接口**: `GET /api/owner`
- **流程**: `UserController.owner()` → `UserServiceImpl.getOwnerList()`
  - **Redis 缓存**: key=`cdrm:user:owner`，单 value 存储负责人列表并设置 300 秒 TTL
  - 未命中时查询启用、未锁定且具备销售负责人资格的账号；账号状态或角色变化后删除缓存，失败时记录并重试

### 3.3 涉及数据库表
- `t_user` - 用户表
- `t_role` - 角色表
- `t_user_role` - 用户角色关联表
- `t_permission` - 权限表
- `t_role_permission` - 角色权限关联表

---

## 4. 线索管理模块

### 4.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/ClueController.java`, `web/ClueRemarkController.java` |
| Service | `service/ClueService.java` → `service/impl/ClueServiceImpl.java`, `service/ClueRemarkService.java` → `service/impl/ClueRemarkServiceImpl.java` |
| Manager | `manager/CustomerManager.java`（线索转客户） |
| Mapper | `mapper/TClueMapper.java`, `mapper/TClueRemarkMapper.java`, `mapper/TClueOwnerHistoryMapper.java` |
| XML | `resources/mapper/TClueMapper.xml`, `resources/mapper/TClueRemarkMapper.xml`, `resources/mapper/TClueOwnerHistoryMapper.xml` |
| Model | `model/TClue.java`, `model/TClueRemark.java`, `model/TClueOwnerHistory.java` |
| Query | `query/ClueQuery.java`, `query/ClueRemarkQuery.java` |
| Excel | `result/ClueExcelRaw.java`, `service/ClueImportValidator.java`, `dto/ImportResult.java` |

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
| Controller | `web/CustomerController.java` |
| Service | `service/CustomerService.java` → `service/impl/CustomerServiceImpl.java` |
| Manager | `manager/CustomerManager.java` |
| Mapper | `mapper/TCustomerMapper.java`, `mapper/TCustomerOwnerHistoryMapper.java`, `mapper/TClueMapper.java` |
| XML | `resources/mapper/TCustomerMapper.xml`, `resources/mapper/TCustomerOwnerHistoryMapper.xml` |
| Model | `model/TCustomer.java`, `model/TCustomerOwnerHistory.java` |
| Query | `query/CustomerQuery.java` |
| Excel | `result/CustomerExcel.java` |

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
| Controller | `web/TranController.java` |
| Service | `service/TranService.java` → `service/impl/TranServiceImpl.java`, `service/TransactionCompletionService.java` → `service/impl/TransactionCompletionServiceImpl.java` |
| Mapper | `mapper/TTranMapper.java`, `mapper/TTranProductMapper.java`, `mapper/TTranInvoiceMapper.java`, `mapper/TTranApproveMapper.java`, `mapper/TTranRemarkMapper.java`, `mapper/TPaymentMapper.java`, `mapper/TRefundRequestMapper.java`, `mapper/TDeliveryMapper.java`, `mapper/TProductStockRecordMapper.java`, `mapper/ProductMapper.java` |
| XML | `resources/mapper/TTranMapper.xml`, `resources/mapper/TTranProductMapper.xml`, `resources/mapper/TTranInvoiceMapper.xml`, `resources/mapper/TTranApproveMapper.xml`, `resources/mapper/TTranRemarkMapper.xml`, `resources/mapper/TPaymentMapper.xml`, `resources/mapper/TRefundRequestMapper.xml`, `resources/mapper/TDeliveryMapper.xml`, `resources/mapper/TProductStockRecordMapper.xml` |
| Model | `model/TTran.java`, `model/TTranProduct.java`, `model/TTranInvoice.java`, `model/TTranApprove.java`, `model/TTranRemark.java`, `model/TranCreateRequest.java` |
| Query | `query/TranQuery.java`, `query/TranProductQuery.java` |

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
| Controller | `web/QuoteController.java` |
| Service | `service/QuoteService.java` → `service/impl/QuoteServiceImpl.java` |
| Mapper | `mapper/TQuoteMapper.java`, `mapper/TQuoteVersionMapper.java`, `mapper/TQuoteVersionItemMapper.java`, `mapper/TQuoteStatusHistoryMapper.java` |
| XML | `resources/mapper/TQuoteMapper.xml`, `resources/mapper/TQuoteVersionMapper.xml`, `resources/mapper/TQuoteVersionItemMapper.xml`, `resources/mapper/TQuoteStatusHistoryMapper.xml` |
| Model | `model/TQuote.java`, `model/TQuoteVersion.java`, `model/TQuoteVersionItem.java`, `model/TQuoteStatusHistory.java` |
| DTO | `dto/CreateQuoteRequest.java`, `dto/CreateQuoteVersionRequest.java`, `dto/UpdateQuoteStatusRequest.java`, `dto/QuoteDetailResponse.java` |
| Query | `query/QuoteQuery.java` |

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
| Controller | `web/ActivityController.java`, `web/ActivityRemarkController.java` |
| Service | `service/ActivityService.java` → `service/impl/ActivityServiceImpl.java`, `service/ActivityRemarkService.java` → `service/impl/ActivityRemarkServiceImpl.java` |
| Mapper | `mapper/TActivityMapper.java`, `mapper/TActivityRemarkMapper.java` |
| XML | `resources/mapper/TActivityMapper.xml`, `resources/mapper/TActivityRemarkMapper.xml` |
| Model | `model/TActivity.java`, `model/TActivityRemark.java` |
| Query | `query/ActivityQuery.java`, `query/ActivityRemarkQuery.java` |
| DTO/Result | `dto/CreateActivityRequest.java`, `dto/UpdateActivityRequest.java`, `dto/ReviewActivityRequest.java`, `dto/ActivityLifecycleRequest.java`, `dto/ActivityRoiResponse.java`, `result/ActivityExportRow.java` |
| Enum | `enums/ActivityStatus.java` |

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
| Controller | `web/ProductController.java`, `web/ProductCategoryController.java`, `web/ProductPromotionController.java`, `web/ProductStockController.java` |
| Service | `service/ProductService.java` → `service/impl/ProductServiceImpl.java`, `service/ProductCategoryService.java` → `service/impl/ProductCategoryServiceImpl.java`, `service/ProductPromotionService.java` → `service/impl/ProductPromotionServiceImpl.java`, `service/ProductStockRecordService.java` → `service/impl/ProductStockRecordServiceImpl.java`, `service/ProductVehicleService.java` → `service/impl/ProductVehicleServiceImpl.java` |
| Mapper | `mapper/ProductMapper.java`, `mapper/ProductCategoryMapper.java`, `mapper/ProductPromotionMapper.java`, `mapper/ProductStockRecordMapper.java`, `mapper/TProductVehicleMapper.java` |
| XML | `resources/mapper/TProductMapper.xml`, `resources/mapper/TProductCategoryMapper.xml`, `resources/mapper/TProductPromotionMapper.xml`, `resources/mapper/TProductStockRecordMapper.xml`, `resources/mapper/TProductVehicleMapper.xml` |
| Model | `model/Product.java`, `model/ProductCategory.java`, `model/ProductPromotion.java`, `model/ProductStockRecord.java`, `model/TProduct.java`, `model/TProductVehicle.java`, `model/TProductStockRecord.java` |

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
| Controller | `web/DicController.java` |
| Service | `service/DicService.java` → `service/impl/DicServiceImpl.java` |
| Mapper | `mapper/DicMapper.java`, `mapper/TDicTypeMapper.java`, `mapper/TDicValueMapper.java` |
| XML | `resources/mapper/DicMapper.xml` |
| Model | `model/TDicType.java`, `model/TDicValue.java` |
| Query | `query/DicQuery.java` |

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
| Controller | `web/StatisticController.java` |
| Service | `service/StatisticService.java` → `service/impl/StatisticServiceImpl.java` |
| Manager | `manager/StatisticManager.java` |
| Result | `result/SummaryData.java`, `result/NameValue.java` |

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
| Controller | `web/DeliveryController.java` |
| Service | `service/DeliveryService.java` → `service/impl/DeliveryServiceImpl.java` |
| Mapper | `mapper/TDeliveryMapper.java`, `mapper/TDeliveryCheckItemMapper.java`, `mapper/TProductVehicleMapper.java`, `mapper/TProductStockRecordMapper.java` |
| XML | `resources/mapper/TDeliveryMapper.xml`, `resources/mapper/TDeliveryCheckItemMapper.xml`, `resources/mapper/TProductVehicleMapper.xml`, `resources/mapper/TProductStockRecordMapper.xml` |
| Model | `model/TDelivery.java`, `model/TDeliveryCheckItem.java` |
| DTO | `dto/CreateDeliveryRequest.java`, `dto/UpdateDeliveryCheckItemRequest.java`, `dto/SignDeliveryRequest.java`, `dto/DeliveryExceptionRequest.java`, `dto/DeliveryCancelRequest.java` |
| Query | `query/DeliveryQuery.java` |

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
| Controller | `web/OpportunityController.java` |
| Service | `service/OpportunityService.java` → `service/impl/OpportunityServiceImpl.java` |
| Mapper | `mapper/TOpportunityMapper.java`, `mapper/TOpportunityStageHistoryMapper.java` |
| XML | `resources/mapper/TOpportunityMapper.xml`, `resources/mapper/TOpportunityStageHistoryMapper.xml` |
| Model | `model/TOpportunity.java`, `model/TOpportunityStageHistory.java` |
| DTO | `dto/CreateOpportunityRequest.java`, `dto/UpdateOpportunityRequest.java`, `dto/AdvanceOpportunityStageRequest.java`, `dto/OpportunityResultRequest.java` |
| Enum | `enums/OpportunityStage.java` |
| Query | `query/OpportunityQuery.java` |

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
| Controller | `web/TestDriveController.java` |
| Service | `service/TestDriveService.java` → `service/impl/TestDriveServiceImpl.java` |
| Mapper | `mapper/TTestDriveMapper.java`, `mapper/TTestDriveVehicleHoldMapper.java`, `mapper/TTestDriveStatusHistoryMapper.java` |
| XML | `resources/mapper/TTestDriveMapper.xml`, `resources/mapper/TTestDriveVehicleHoldMapper.xml`, `resources/mapper/TTestDriveStatusHistoryMapper.xml` |
| Model | `model/TTestDrive.java`, `model/TTestDriveVehicleHold.java`, `model/TTestDriveStatusHistory.java` |
| DTO | `dto/CreateTestDriveRequest.java`, `dto/RescheduleTestDriveRequest.java`, `dto/CancelTestDriveRequest.java`, `dto/CheckInTestDriveRequest.java`, `dto/CompleteTestDriveRequest.java` |
| Enum | `enums/TestDriveStatus.java` |
| Query | `query/TestDriveQuery.java` |

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
| Controller | `web/FollowTaskController.java`, `web/CommunicationRecordController.java` |
| Service | `service/FollowTaskService.java`, `service/CommunicationRecordService.java` |
| Service 实现 | `service/impl/FollowTaskServiceImpl.java`, `service/impl/CommunicationRecordServiceImpl.java`, `service/impl/FollowRelatedObjectResolver.java` |
| Mapper | `mapper/TFollowTaskMapper.java`, `mapper/TCommunicationRecordMapper.java` |
| XML | `resources/mapper/TFollowTaskMapper.xml`, `resources/mapper/TCommunicationRecordMapper.xml` |
| Model | `model/TFollowTask.java`, `model/TCommunicationRecord.java` |
| DTO | `dto/CreateFollowTaskRequest.java`, `dto/PostponeFollowTaskRequest.java`, `dto/CancelFollowTaskRequest.java`, `dto/CompleteFollowTaskRequest.java`, `dto/CreateCommunicationRecordRequest.java`, `dto/CorrectCommunicationRecordRequest.java`, `dto/VoidCommunicationRecordRequest.java` |
| Enum | `enums/FollowRelatedObjectType.java`, `enums/FollowTaskStatus.java`, `enums/FollowTaskType.java`, `enums/FollowTaskPriority.java`, `enums/CommunicationMethod.java`, `enums/CommunicationRecordStatus.java` |
| Query | `query/FollowTaskQuery.java`, `query/CommunicationRecordQuery.java` |

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
- 普通 Token 过期、权限不足和退出登录不作为第一阶段登录记录写入；这些边界以业务 Spec 为准。

### 16.2 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/AuditLogController.java` |
| Service | `service/AuditLogService.java` → `service/impl/AuditLogServiceImpl.java` |
| Recorder | `audit/LoginAuditRecorder.java`, `audit/OperationAuditRecorder.java` |
| Mapper | `mapper/TLoginLogMapper.java`, `mapper/TOperationLogMapper.java` |
| XML | `resources/mapper/TLoginLogMapper.xml`, `resources/mapper/TOperationLogMapper.xml` |
| Model | `model/TLoginLog.java`, `model/TOperationLog.java` |
| Query | `query/AuditLoginLogQuery.java`, `query/AuditOperationLogQuery.java` |

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
**路径**: `config/SecurityConfig.java`

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
**路径**: `config/filter/TokenVerifyFilter.java`

**执行逻辑**:
1. 登录请求放行
2. 仅从 `Authorization: Bearer <token>` 请求头获取 token
3. 验证 token 非空、Bearer 格式正确、签名有效、Redis 中存在且匹配
4. 设置 SecurityContext
5. 当前用户已停用、锁定或删除时删除 Redis 会话；删除失败返回 HTTP 500

### 17.3 Handler 处理器

| Handler | 路径 | 功能 |
|---------|------|------|
| MyAuthenticationSuccessHandler | `config/handler/MyAuthenticationSuccessHandler.java` | 登录成功：生成 JWT、存入 Redis、写登录审计、返回 token |
| MyAuthenticationFailureHandler | `config/handler/MyAuthenticationFailureHandler.java` | 登录失败：写失败登录审计并返回 401 |
| MyLogoutSuccessHandler | `config/handler/MyLogoutSuccessHandler.java` | 退出成功：删除 Redis 中的 JWT |
| MyAccessDeniedHandler | `config/handler/MyAccessDeniedHandler.java` | 权限不足：返回 ACCESS_DENIED |
| GlobalExceptionHandler | `config/handler/GlobalExceptionHandler.java` | 全局异常处理：统一返回错误 |

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
**路径**: `config/CorsConfig.java`

- 允许所有源（`addAllowedOriginPattern("*")`）
- 允许所有请求头
- 允许 GET/POST/PUT/DELETE/OPTIONS 方法
- 允许携带 Cookie
- 预检缓存 30 分钟

---

## 18. AOP 切面

### 18.1 DataScopeAspect
**路径**: `aspect/DataScopeAspect.java`

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
**路径**: `commons/DataScope.java`

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
**路径**: `util/JWTUtils.java`

| 方法 | 功能 |
|------|------|
| `createJWT(String userJSON)` | 生成 JWT，负载为用户 JSON，过期时间 24 小时 |
| `verifyJWT(String jwt)` | 验证 JWT 签名是否有效 |
| `parseUserFromJWT(String jwt)` | 从 JWT 解析用户信息（TUser 对象） |

**密钥**: 从环境变量 `JWT_SECRET` 获取；未配置时应用启动失败，避免使用可预测的默认签名密钥。

### 19.2 CacheUtils
**路径**: `util/CacheUtils.java`

| 方法 | 功能 |
|------|------|
| `getCacheData(Supplier cacheSelector, Supplier databaseSelector, Consumer cacheSave)` | 通用缓存查询：先查缓存，未命中查数据库并缓存 |
| `generateKey(Object... params)` | 生成缓存 key |

### 19.3 JSONUtils
**路径**: `util/JSONUtils.java`

| 方法 | 功能 |
|------|------|
| `toJSON(Object object)` | Java 对象转 JSON 字符串 |
| `toBean(String json, Class<T> clazz)` | JSON 字符串转 Java 对象 |

### 19.4 ResponseUtils
**路径**: `util/ResponseUtils.java`

| 方法 | 功能 |
|------|------|
| `write(HttpServletResponse response, String result)` | 将 JSON 结果写入 HttpServletResponse |

### 19.5 RedisManager
**路径**: `manager/RedisManager.java`

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
| `selectUserByPage` | SELECT | 用户分页查询（支持数据权限） |
| `selectDetailById` | SELECT | 用户详情（关联创建人/编辑人） |
| `selectByOwner` | SELECT | 查询所有用户（负责人列表） |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `countBusinessReferences` | SELECT | 禁用前统计当前责任引用 |
| `selectOwnedActivityIds` | SELECT | 查询待交接活动 ID |
| `selectOwnedClueIds` | SELECT | 查询待交接线索 ID |
| `selectOwnedCustomerIds` | SELECT | 查询待交接客户 ID |
| `transferOwnedActivities` | UPDATE | 批量交接活动负责人 |
| `transferOwnedClues` | UPDATE | 批量交接线索负责人 |
| `transferOwnedCustomers` | UPDATE | 批量交接客户负责人 |
| `deleteByPrimaryKey` | DELETE | 旧物理删除方法，不用于离职处理 |
| `deleteByIds` | DELETE | 旧批量物理删除方法，不用于离职处理 |
| `insert` | INSERT | 插入用户 |
| `insertSelective` | INSERT | 选择性插入用户 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新用户 |
| `updateByPrimaryKey` | UPDATE | 全字段更新用户 |

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

## 22. 数据库表汇总

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `t_user` | 用户表 | id, login_act, login_pwd, name, phone, email, account_enabled |
| `t_role` | 角色表 | id, role, role_name |
| `t_user_role` | 用户角色关联表 | id, user_id, role_id |
| `t_permission` | 权限表 | id, name, code, url, type, parent_id |
| `t_role_permission` | 角色权限关联表 | id, role_id, permission_id |
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

---

## 附录：常量定义

**路径**: `constant/Constants.java`、`constant/RedisKeys.java`

| 常量 | 值 | 用途 |
|------|-----|------|
| `LOGIN_URI` | `/api/login` | 登录接口 |
| `EXPIRE_TIME` | `7 * 24 * 60 * 60L` | JWT 过期时间（7天） |
| `DEFAULT_EXPIRE_TIME` | `4 * 60 * 60L` | JWT 默认过期时间（4小时） |
| `PAGE_SIZE` | `10` | 分页每页条数 |
| `CACHE_EXPIRE_TIME` | `24 * 60 * 60L` | 缓存过期时间（1天） |
| `RedisKeys.userLogin(userId)` | `cdrm:user:login:{userId}` | JWT Redis Key |
| `RedisKeys.ownerList()` | `cdrm:user:owner` | 负责人列表 Redis Key，单 value 序列化列表，300 秒 TTL |
