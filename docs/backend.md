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
- [7. 市场活动模块](#7-市场活动模块)
- [8. 商品管理模块](#8-商品管理模块)
- [9. 字典管理模块](#9-字典管理模块)
- [11. 统计报表模块](#11-统计报表模块)
- [13. 安全配置](#13-安全配置)
- [14. AOP 切面](#14-aop-切面)
- [15. 工具类](#15-工具类)
- [16. 数据字典缓存机制](#16-数据字典缓存机制)
- [17. Mapper XML SQL 汇总](#17-mapper-xml-sql-汇总)
- [18. 数据库表汇总](#18-数据库表汇总)

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
   → RedisService.setValue() 存储 JWT 到 Redis（key: cdrm:user:login:{userId}）
   → 设置过期时间：rememberMe=true → 7天，否则 30分钟
   → 返回 JWT 给前端
6. 登录失败 → MyAuthenticationFailureHandler → 返回错误信息
```

#### Token 验证流程（TokenVerifyFilter）
```
1. 请求进入 Filter
2. 判断是否为 /api/login 请求 → 放行
3. 从 Header 或参数中获取 Authorization token
4. token 为空 → 返回 TOKEN_IS_EMPTY
5. JWTUtils.verifyJWT() 验证签名 → 返回 TOKEN_IS_ERROR
6. JWTUtils.parseUserFromJWT() 解析用户信息
7. Redis 查询 token → 不存在返回 TOKEN_IS_EXPIRED
8. Redis token 与请求 token 不匹配 → 返回 TOKEN_IS_NONE_MATCH
9. 验证通过 → 设置 SecurityContext
10. 异步刷新 token 过期时间（线程池）
```

#### 退出流程
```
1. 前端 GET /api/logout
2. MyLogoutSuccessHandler
   → 删除 Redis 中的 JWT
   → 返回退出成功信息
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
| DELETE /api/clue/{id} | `clue:delete` |
| POST /api/clue/batch | `clue:delete` |
| GET /api/users | `user:list` |
| GET /api/user/{id} | `user:view` |
| POST /api/user | `user:add` |
| PUT /api/user | `user:edit` |
| DELETE /api/user/{id} | `user:delete` |
| DELETE /api/user | `user:delete` |
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
- **接口**: `GET /api/users?current=1`
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

#### 删除用户
- **接口**: `DELETE /api/user/{id}`
- **权限**: `@PreAuthorize("hasAuthority('user:delete')")`
- **流程**: `UserController.delUser()` → `UserServiceImpl.delUserById()` → `TUserMapper.deleteByPrimaryKey()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除用户
- **接口**: `DELETE /api/user`
- **权限**: `@PreAuthorize("hasAuthority('user:delete')")`
- **流程**: `UserController.batchDelUser()` → `UserServiceImpl.batchDelUserIds()` → `TUserMapper.deleteByIds()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取负责人列表
- **接口**: `GET /api/owner`
- **流程**: `UserController.owner()` → `UserServiceImpl.getOwnerList()`
  - **Redis 缓存**: key=`cdrm:user:owner`，List 结构存储
  - 使用 `CacheUtils.getCacheData()` 先查 Redis，未命中查数据库并缓存

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
| Mapper | `mapper/TClueMapper.java`, `mapper/TClueRemarkMapper.java` |
| XML | `resources/mapper/TClueMapper.xml`, `resources/mapper/TClueRemarkMapper.xml` |
| Model | `model/TClue.java`, `model/TClueRemark.java` |
| Query | `query/ClueQuery.java`, `query/ClueRemarkQuery.java` |
| Excel | `result/ClueExcel.java`, `config/converter/ClueExcelConverter.java`, `config/listener/UploadDataListener.java` |

### 4.2 接口方法及业务流程

#### 线索列表分页查询
- **接口**: `GET /api/clues?current=1`
- **权限**: `@PreAuthorize("hasAuthority('clue:list')")`
- **流程**: `ClueController.cluePage()` → `ClueServiceImpl.getClueByPage()` → `TClueMapper.selectClueByPage()`
- **SQL**: 多表 LEFT JOIN 关联查询用户、活动、字典值、产品

#### Excel 导入线索
- **接口**: `POST /api/importExcel`
- **权限**: `@PreAuthorize("hasAuthority('clue:import')")`
- **流程**: `ClueController.importExcel()` → `ClueServiceImpl.importExcel()`
  - EasyExcel 读取 Excel
  - `UploadDataListener` 监听器逐行处理
  - `ClueExcelConverter` 转换 Excel 数据
  - 使用 `DlykServerApplication.cacheMap` 缓存字典数据进行转换
  - `TClueMapper.saveClue()` 批量插入

#### 手机号查重
- **接口**: `GET /api/clue/{phone}`
- **流程**: `ClueController.checkPhone()` → `ClueServiceImpl.checkPhone()` → `TClueMapper.selectByCount()`

#### 新增线索
- **接口**: `POST /api/clue`
- **权限**: `@PreAuthorize("hasAuthority('clue:add')")`
- **流程**: `ClueController.addClue()` → `ClueServiceImpl.saveClue()`
  - 手机号查重
  - `BeanUtils.copyProperties()` 复制属性
  - `JWTUtils.parseUserFromJWT()` 解析创建人
  - `TClueMapper.insertSelective()` 插入
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

#### 删除线索
- **接口**: `DELETE /api/clue/{id}`
- **权限**: `@PreAuthorize("hasAuthority('clue:delete')")`
- **流程**: `ClueController.delClue()` → `ClueServiceImpl.delClueById()` → `TClueMapper.deleteByPrimaryKey()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除线索
- **接口**: `POST /api/clue/batch`
- **权限**: `@PreAuthorize("hasAuthority('clue:delete')")`
- **流程**: `ClueController.batchDelClue()` → `ClueServiceImpl.batchDelClueByIds()` → `TClueMapper.batchDeleteByIds()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 线索备注
- **新增**: `POST /api/clue/remark` → `ClueRemarkServiceImpl.saveClueRemark()` → `TClueRemarkMapper.insertSelective()`
- **分页查询**: `GET /api/clue/remark?current=1&clueId=` → `ClueRemarkServiceImpl.getClueRemarkByPage()` → `TClueRemarkMapper.selectClueRemarkByPage()`
- **事务**: 新增方法有 `@Transactional(rollbackFor = Exception.class)`

### 4.3 涉及数据库表
- `t_clue` - 线索表
- `t_clue_remark` - 线索跟踪记录表
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
| Mapper | `mapper/TCustomerMapper.java`, `mapper/TClueMapper.java` |
| XML | `resources/mapper/TCustomerMapper.xml` |
| Model | `model/TCustomer.java`, `model/CustomerOption.java` |
| Query | `query/CustomerQuery.java` |
| Excel | `result/CustomerExcel.java` |

### 5.2 接口方法及业务流程

#### 客户列表（带查询条件）
- **接口**: `GET /api/customer/list?page=1&size=10`
- **流程**: `CustomerController.list()` → `CustomerServiceImpl.getCustomerList()` → `TCustomerMapper.selectByQuery()`
- **支持查询条件**: 客户名称、产品ID、创建人

#### 客户选项（下拉选择）
- **接口**: `GET /api/customer/options`
- **流程**: `CustomerController.options()` → `CustomerServiceImpl.getCustomerOptions()` → `TCustomerMapper.selectCustomerOptions()`

#### 客户详情
- **接口**: `GET /api/customer/{id}`
- **流程**: `CustomerController.detail()` → `CustomerServiceImpl.getCustomerById()` → `TCustomerMapper.selectByPrimaryKey()`

#### 线索转客户（核心业务）
- **接口**: `POST /api/clue/customer`
- **流程**: `CustomerController.convertCustomer()` → `CustomerServiceImpl.convertCustomer()` → `CustomerManager.convertCustomer()`
  1. 验证线索是否已转客户（state == -1 则已转）
  2. `TCustomerMapper.insertSelective()` 插入客户记录
  3. `TClueMapper.updateByPrimaryKeySelective()` 更新线索状态为 -1
  4. 自动创建交易记录：`TranService.createTransaction()`
  5. 关联产品信息
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 客户分页查询（旧版）
- **接口**: `GET /api/customers?current=1`
- **流程**: `CustomerController.cluePage()` → `CustomerServiceImpl.getCustomerByPage()` → `TCustomerMapper.selectCustomerPage()`

#### 导出 Excel
- **接口**: `GET /api/exportExcel?ids=1,2,3`
- **流程**: `CustomerController.exportExcel()` → `CustomerServiceImpl.getCustomerByExcel()` → `TCustomerMapper.selectCustomerByExcel()`
  - EasyExcel 写入 Excel
  - 关联查询线索、用户、活动、字典值、产品信息

### 5.3 涉及数据库表
- `t_customer` - 客户表
- `t_clue` - 线索表（关联）
- `t_tran` - 交易表（自动创建）
- `t_tran_product` - 交易产品关联表

---

## 6. 交易管理模块

### 6.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/TranController.java` |
| Service | `service/TranService.java` → `service/impl/TranServiceImpl.java` |
| Mapper | `mapper/TTranMapper.java`, `mapper/TTranProductMapper.java`, `mapper/TTranInvoiceMapper.java`, `mapper/TTranApproveMapper.java`, `mapper/TTranRemarkMapper.java`, `mapper/ProductMapper.java` |
| XML | `resources/mapper/TTranMapper.xml`, `resources/mapper/TTranProductMapper.xml`, `resources/mapper/TTranInvoiceMapper.xml`, `resources/mapper/TTranApproveMapper.xml`, `resources/mapper/TTranRemarkMapper.xml` |
| Model | `model/TTran.java`, `model/TTranProduct.java`, `model/TTranInvoice.java`, `model/TTranApprove.java`, `model/TTranRemark.java`, `model/TranCreateRequest.java` |
| Query | `query/TranQuery.java`, `query/TranProductQuery.java` |

### 6.2 交易状态流转

```
待报价(41) → 待审批(42) → 已审批(43) → 待收款(45) → 已完成(46)
                                    ↘ 丢失关闭(21)
```

### 6.3 接口方法及业务流程

#### 交易列表
- **接口**: `GET /api/tran/list?page=1&size=10`
- **流程**: `TranController.list()` → `TranServiceImpl.getTransactionList()` → `TTranMapper.selectByQuery()`
- **支持查询条件**: 交易编号、客户ID、客户名称、阶段、金额范围、日期范围、产品、发票状态

#### 交易详情
- **接口**: `GET /api/tran/{id}`
- **流程**: `TranController.detail()` → `TranServiceImpl.getTransactionById()` → `TTranMapper.selectByPrimaryKey()`

#### 创建交易
- **接口**: `POST /api/tran/create`
- **流程**: `TranController.create()` → `TranServiceImpl.createTransaction()`
  1. 生成交易编号（TN + 年月日 + 6位随机数）
  2. `TTranMapper.insertSelective()` 插入交易
  3. 遍历产品列表：`TTranProductMapper.insertSelective()` 插入产品关联
  4. `ProductMapper.updateStock()` 扣减库存
  5. 清除 Redis 缓存
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 更新交易
- **接口**: `PUT /api/tran/update`
- **流程**: `TranController.update()` → `TranServiceImpl.updateTransaction()`
  - 更新交易基本信息
  - 删除旧产品关联（恢复库存）→ 插入新产品关联（扣减库存）

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
  2. `TTranInvoiceMapper.insertSelective()` 插入发票
  3. 更新交易状态为待收款(45)
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取发票列表
- **接口**: `GET /api/tran/invoice/{tranId}`
- **流程**: → `TranServiceImpl.getTranInvoices()` → `TTranInvoiceMapper.selectByTranId()`

#### 更新发票状态
- **接口**: `PUT /api/tran/invoice/{invoiceId}/status`
- **流程**: → `TranServiceImpl.updateTranInvoiceStatus()`
  - 发票状态变为 ISSUED 时，更新交易状态为已完成(46)
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 获取交易备注
- **接口**: `GET /api/tran/remarks/{tranId}`
- **流程**: → `TranServiceImpl.getTransactionRemarks()` → `TTranRemarkMapper.selectByTranId()`

#### 获取交易产品详情
- **接口**: `GET /api/tran/products/{id}`
- **流程**: → `TranServiceImpl.getTransactionProductDetails()` → `TTranMapper.selectTranProductsByTranId()`

#### 删除交易
- **接口**: `DELETE /api/tran/{id}`
- **流程**: → `TranServiceImpl.deleteTransaction()`
  1. 恢复产品库存
  2. 删除交易产品关联
  3. 删除交易备注
  4. 删除交易主记录
  5. 清除缓存
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除交易
- **接口**: `POST /api/tran/batch-delete`
- **流程**: → `TranServiceImpl.batchDeleteTransactions()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

### 6.4 Redis 缓存

| 缓存 Key | 用途 | 过期时间 |
|----------|------|---------|
| `cdrm:tran:detail:{tranId}` | 交易详情 | 24小时 |
| `cdrm:tran:list:*` | 交易列表 | 24小时 |
| `cdrm:tran:products:{tranId}` | 交易产品 | 24小时 |
| `cdrm:tran:production:{tranId}` | 交易生产 | 24小时 |
| `cdrm:tran:invoices:{tranId}` | 交易发票 | 24小时 |

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

## 7. 市场活动模块

### 7.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/ActivityController.java`, `web/ActivityRemarkController.java` |
| Service | `service/ActivityService.java` → `service/impl/ActivityServiceImpl.java`, `service/ActivityRemarkService.java` → `service/impl/ActivityRemarkServiceImpl.java` |
| Mapper | `mapper/TActivityMapper.java`, `mapper/TActivityRemarkMapper.java` |
| XML | `resources/mapper/TActivityMapper.xml`, `resources/mapper/TActivityRemarkMapper.xml` |
| Model | `model/TActivity.java`, `model/TActivityRemark.java` |
| Query | `query/ActivityQuery.java`, `query/ActivityRemarkQuery.java` |

### 7.2 接口方法及业务流程

#### 活动列表分页查询
- **接口**: `GET /api/activitys?current=1`
- **流程**: `ActivityController.activityPage()` → `ActivityServiceImpl.getActivityByPage()` → `TActivityMapper.selectActivityByPage()`
- **支持查询条件**: 所属人、名称、时间范围、预算、创建时间
- **SQL 包含**: `${filterSQL}` 数据权限过滤

#### 新增活动
- **接口**: `POST /api/activity`
- **流程**: → `ActivityServiceImpl.saveActivity()` → `TActivityMapper.insertSelective()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 活动详情
- **接口**: `GET /api/activity/{id}`
- **流程**: → `ActivityServiceImpl.getActivityById()` → `TActivityMapper.selectDetailByPrimaryKey()`

#### 编辑活动
- **接口**: `PUT /api/activity`
- **流程**: → `ActivityServiceImpl.updateActivity()` → `TActivityMapper.updateByPrimaryKeySelective()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 删除活动
- **接口**: `DELETE /api/activity/{id}`
- **流程**: → `ActivityServiceImpl.deleteActivity()` → `TActivityMapper.deleteByPrimaryKey()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 批量删除活动
- **接口**: `POST /api/activity/batch`
- **流程**: → `ActivityServiceImpl.batchDeleteActivities()` → `TActivityMapper.batchDeleteByIds()`
- **事务**: `@Transactional(rollbackFor = Exception.class)`

#### 活动备注
- **新增**: `POST /api/activity/remark` → `ActivityRemarkServiceImpl.saveActivityRemark()` → `TActivityRemarkMapper.insertSelective()`
- **分页查询**: `GET /api/activity/remark?current=1&activityId=` → `TActivityRemarkMapper.selectActivityRemarkByPage()`
- **详情**: `GET /api/activity/remark/{id}` → `TActivityRemarkMapper.selectByPrimaryKey()`
- **编辑**: `PUT /api/activity/remark` → `TActivityRemarkMapper.updateByPrimaryKeySelective()`
- **删除**: `DELETE /api/activity/remark/{id}` → 逻辑删除（设置 deleted=1）

### 7.3 涉及数据库表
- `t_activity` - 市场活动表
- `t_activity_remark` - 活动备注表
- `t_user` - 用户表（关联查询）

---

## 8. 商品管理模块

### 8.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/ProductController.java`, `web/ProductCategoryController.java`, `web/ProductPromotionController.java`, `web/ProductStockController.java` |
| Service | `service/ProductService.java` → `service/impl/ProductServiceImpl.java`, `service/ProductCategoryService.java` → `service/impl/ProductCategoryServiceImpl.java`, `service/ProductPromotionService.java` → `service/impl/ProductPromotionServiceImpl.java`, `service/ProductStockRecordService.java` → `service/impl/ProductStockRecordServiceImpl.java` |
| Mapper | `mapper/ProductMapper.java`, `mapper/ProductCategoryMapper.java`, `mapper/ProductPromotionMapper.java`, `mapper/ProductStockRecordMapper.java` |
| XML | `resources/mapper/TProductMapper.xml`, `resources/mapper/TProductCategoryMapper.xml`, `resources/mapper/TProductPromotionMapper.xml`, `resources/mapper/TProductStockRecordMapper.xml` |
| Model | `model/Product.java`, `model/ProductCategory.java`, `model/ProductPromotion.java`, `model/ProductStockRecord.java`, `model/TProduct.java` |

### 8.2 接口方法及业务流程

#### 产品管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/products` | 获取产品列表 | 无 |
| `GET /api/products/{id}` | 获取产品详情 | 无 |
| `POST /api/products` | 新增产品 | `@Transactional` |
| `PUT /api/products/{id}` | 更新产品 | `@Transactional` |
| `DELETE /api/products/{id}` | 删除产品 | `@Transactional` |
| `GET /api/products/stockalerts` | 库存预警列表 | 无 |
| `POST /api/products/stock/restock` | 入库补货 | `@Transactional` |

#### 产品分类管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/product-categories` | 分类列表 | 无 |
| `GET /api/product-categories/{id}` | 分类详情 | 无 |
| `POST /api/product-categories` | 新增分类 | `@Transactional` |
| `PUT /api/product-categories/{id}` | 更新分类 | `@Transactional` |
| `DELETE /api/product-categories/{id}` | 删除分类 | `@Transactional` |

#### 产品促销管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `GET /api/product-promotions` | 促销列表 | 无 |
| `GET /api/product-promotions/{id}` | 促销详情 | 无 |
| `POST /api/product-promotions` | 新增促销 | `@Transactional` |
| `PUT /api/product-promotions/{id}` | 更新促销 | `@Transactional` |
| `DELETE /api/product-promotions/{id}` | 删除促销 | `@Transactional` |

#### 库存管理
| 接口 | 方法 | 事务 |
|------|------|------|
| `POST /api/productstock/restock` | 入库 | `@Transactional` |
| `GET /api/productstock/records/{productId}` | 库存变动记录 | 无 |

### 8.3 入库业务流程
```
ProductStockController.restock()
→ ProductServiceImpl.restock()
  → ProductMapper.updateStock() 更新库存
  → ProductStockRecordMapper.insert() 记录库存变动
```

### 8.4 涉及数据库表
- `t_product` - 产品表
- `t_product_category` - 产品分类表
- `t_product_promotion` - 产品促销表
- `t_product_stock_record` - 库存变动记录表

---

## 9. 字典管理模块

### 9.1 文件路径

| 层级 | 文件路径 |
|------|----------|
| Controller | `web/DicController.java` |
| Service | `service/DicService.java` → `service/impl/DicServiceImpl.java` |
| Mapper | `mapper/DicMapper.java`, `mapper/TDicTypeMapper.java`, `mapper/TDicValueMapper.java` |
| XML | `resources/mapper/DicMapper.xml` |
| Model | `model/TDicType.java`, `model/TDicValue.java` |
| Query | `query/DicQuery.java` |

### 9.2 接口方法及业务流程

#### 字典类型管理
| 接口 | 方法 | 事务 | 缓存 |
|------|------|------|------|
| `GET /api/dict/types` | 分页查询字典类型 | 无 | 无 |
| `GET /api/dict/type/get/{id}` | 获取字典类型详情 | 无 | Redis: `dic:type:{id}` |
| `POST /api/dict/type/create` | 新增字典类型 | `@Transactional` | 清除 `dic:types:*` |
| `PUT /api/dict/type/update/{id}` | 更新字典类型 | `@Transactional` | 清除相关缓存 |
| `DELETE /api/dict/type/delete/{id}` | 删除字典类型 | `@Transactional` | 清除所有 dic 缓存 |
| `DELETE /api/dict/types/batch` | 批量删除字典类型 | `@Transactional` | 清除所有 dic 缓存 |

#### 字典值管理
| 接口 | 方法 | 事务 | 缓存 |
|------|------|------|------|
| `GET /api/dict/values` | 分页查询字典值 | 无 | 无 |
| `GET /api/dict/value/get/{id}` | 获取字典值详情 | 无 | Redis: `dic:value:{id}` |
| `POST /api/dict/value/create` | 新增字典值 | `@Transactional` | 清除 `dic:values:*` |
| `PUT /api/dict/value/update/{id}` | 更新字典值 | `@Transactional` | 清除相关缓存 |
| `DELETE /api/dict/value/delete/{id}` | 删除字典值 | `@Transactional` | 清除所有 dic 缓存 |
| `DELETE /api/dict/value/batch` | 批量删除字典值 | `@Transactional` | 清除所有 dic 缓存 |

#### 缓存管理
| 接口 | 方法 | 权限 |
|------|------|------|
| `GET /api/dict/clear?forceRefresh=true` | 清除缓存 | `@PreAuthorize("hasAuthority('admin')")` |
| `GET /api/dict/refresh?type=type\|value` | 刷新缓存 | 无 |

### 9.3 删除字典类型的级联逻辑
```
deleteDicType(id):
  1. 获取字典类型代码 typeCode
  2. 获取关联的字典值ID列表
  3. 删除关联的交易备注记录 (t_tran_remark.note_way)
  4. 删除字典值 (t_dic_value)
  5. 删除字典类型 (t_dic_type)
```

### 9.4 涉及数据库表
- `t_dic_type` - 字典类型表
- `t_dic_value` - 字典值表
- `t_tran_remark` - 交易备注表（级联删除）

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

#### 汇总数据
- **接口**: `GET /api/summary/data`
- **流程**: `StatisticController.summaryData()` → `StatisticServiceImpl.loadSummaryData()` → `StatisticManager.loadSummaryData()`
- **返回数据**:
  - 有效市场活动数
  - 总市场活动数
  - 线索总数
  - 客户总数
  - 成功交易额
  - 总交易额

#### 销售漏斗
- **接口**: `GET /api/saleFunnel/data`
- **流程**: → `StatisticManager.loadSaleFunnelData()`
- **返回数据**: 线索→客户→交易→成交 的数量漏斗

#### 来源饼图
- **接口**: `GET /api/sourcePie/data`
- **流程**: → `StatisticManager.loadSourcePieData()` → `TClueMapper.selectBySource()`
- **返回数据**: 按线索来源分组统计

### 11.3 涉及数据库表
- `t_activity` - 市场活动表
- `t_clue` - 线索表
- `t_customer` - 客户表
- `t_tran` - 交易表

---

## 13. 安全配置

### 13.1 SecurityConfig
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

### 13.2 TokenVerifyFilter
**路径**: `config/filter/TokenVerifyFilter.java`

**执行逻辑**:
1. 登录请求放行
2. 从 Header 或参数获取 token
3. 验证 token 非空、签名有效、Redis 中存在且匹配
4. 设置 SecurityContext
5. 异步刷新 token 过期时间

### 13.3 Handler 处理器

| Handler | 路径 | 功能 |
|---------|------|------|
| MyAuthenticationSuccessHandler | `config/handler/MyAuthenticationSuccessHandler.java` | 登录成功：生成 JWT、存入 Redis、返回 token |
| MyAuthenticationFailureHandler | `config/handler/MyAuthenticationFailureHandler.java` | 登录失败：返回错误信息 |
| MyLogoutSuccessHandler | `config/handler/MyLogoutSuccessHandler.java` | 退出成功：删除 Redis 中的 JWT |
| MyAccessDeniedHandler | `config/handler/MyAccessDeniedHandler.java` | 权限不足：返回 ACCESS_DENIED |
| GlobalExceptionHandler | `config/handler/GlobalExceptionHandler.java` | 全局异常处理：统一返回错误 |

### 13.4 GlobalExceptionHandler 异常处理

| 异常类型 | 处理方式 |
|----------|---------|
| `AccessDeniedException` | 返回 ACCESS_DENIED(520) |
| `DataAccessException` | 返回 DATA_ACCESS_EXCEPTION(521) |
| `HttpRequestMethodNotSupportedException` | 返回不支持的请求方法 |
| `MethodArgumentNotValidException` | 返回参数校验失败 |
| `HttpMessageNotReadableException` | 返回请求体格式错误 |
| `Exception` | 返回通用错误信息 |

### 13.5 CorsConfig
**路径**: `config/CorsConfig.java`

- 允许所有源（`addAllowedOriginPattern("*")`）
- 允许所有请求头
- 允许 GET/POST/PUT/DELETE/OPTIONS 方法
- 允许携带 Cookie
- 预检缓存 30 分钟

---

## 14. AOP 切面

### 14.1 DataScopeAspect
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

### 14.2 DataScope 注解
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

## 15. 工具类

### 15.1 JWTUtils
**路径**: `util/JWTUtils.java`

| 方法 | 功能 |
|------|------|
| `createJWT(String userJSON)` | 生成 JWT，负载为用户 JSON，过期时间 24 小时 |
| `verifyJWT(String jwt)` | 验证 JWT 签名是否有效 |
| `parseUserFromJWT(String jwt)` | 从 JWT 解析用户信息（TUser 对象） |

**密钥**: 从环境变量 `JWT_SECRET` 获取；未配置时应用启动失败，避免使用可预测的默认签名密钥。

### 15.2 CacheUtils
**路径**: `util/CacheUtils.java`

| 方法 | 功能 |
|------|------|
| `getCacheData(Supplier cacheSelector, Supplier databaseSelector, Consumer cacheSave)` | 通用缓存查询：先查缓存，未命中查数据库并缓存 |
| `generateKey(Object... params)` | 生成缓存 key |

### 15.3 JSONUtils
**路径**: `util/JSONUtils.java`

| 方法 | 功能 |
|------|------|
| `toJSON(Object object)` | Java 对象转 JSON 字符串 |
| `toBean(String json, Class<T> clazz)` | JSON 字符串转 Java 对象 |

### 15.4 ResponseUtils
**路径**: `util/ResponseUtils.java`

| 方法 | 功能 |
|------|------|
| `write(HttpServletResponse response, String result)` | 将 JSON 结果写入 HttpServletResponse |

### 15.5 RedisManager
**路径**: `manager/RedisManager.java`

| 方法 | 功能 |
|------|------|
| `get(String key)` | 获取缓存值 |
| `set(String key, Object value, long seconds)` | 设置缓存值（带过期时间） |
| `delete(String key)` | 删除缓存 |
| `deletePattern(String pattern)` | 模式匹配删除缓存 |
| `getValue(String key)` | 获取 List 类型缓存 |
| `setValue(String key, Collection<T> data)` | 设置 List 类型缓存 |

---

## 16. 数据字典缓存机制

### 16.1 缓存架构

```
DicController
    ↓
DicServiceImpl
    ↓
CacheUtils.getCacheData()
    ↓
RedisManager (Redis)  ←→  DicMapper (MySQL)
```

### 16.2 缓存策略

| 缓存 Key 模式 | 内容 | 过期时间 |
|---------------|------|---------|
| `dic:type:{id}` | 字典类型详情 | 24 小时 |
| `dic:type:code:{typeCode}` | 按类型代码查询 | 24 小时 |
| `dic:value:{id}` | 字典值详情 | 24 小时 |
| `dic:values:type:{typeId}` | 按类型ID查询字典值列表 | 24 小时 |

### 16.3 缓存刷新逻辑

#### refreshTypeCache()
```java
1. 删除所有 dic:type:* 缓存
2. 查询所有字典类型
3. 遍历写入 Redis：dic:type:{typeCode} → TDicType
```

#### refreshValueCache()
```java
1. 删除所有 dic:value:* 缓存
2. 查询所有字典值
3. 遍历写入 Redis：dic:value:{typeCode}:{id} → TDicValue
```

#### clearCache()
```java
删除所有 dic:type:*, dic:value:*, dic:list:* 缓存
```

### 16.4 Excel 导入时的字典缓存

**DlykServerApplication.cacheMap**:
- 应用启动时加载字典数据到内存 Map
- Excel 导入时通过 Converter 从 cacheMap 查询字典值 ID

**Converter 列表**:
| Converter | 转换内容 |
|-----------|---------|
| `AppellationConverter` | 称呼：先生/女士 → ID |
| `SourceConverter` | 线索来源：车展会/网络广告 → ID |
| `IntentionStateConverter` | 意向状态：意向不明/有意向 → ID |
| `NeedLoanConverter` | 是否需要贷款：需要/不需要 → ID |
| `StateConverter` | 线索状态：已联系/未联系 → ID |
| `IntentionProductConverter` | 意向产品：产品名 → ID |
| `ClueExcelConverter` | Excel 行数据 → TClue 对象 |

---

## 17. Mapper XML SQL 汇总

### 17.1 TClueMapper.xml (t_clue)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectClueByPage` | SELECT | 线索分页查询（多表关联） |
| `selectByCount` | SELECT | 按手机号查重 |
| `selectClueByCount` | SELECT | 线索总数统计 |
| `selectDetailById` | SELECT | 线索详情（多表关联） |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectBySource` | SELECT | 按来源分组统计（饼图） |
| `deleteByPrimaryKey` | DELETE | 删除线索 |
| `saveClue` | INSERT | 批量保存线索（Excel 导入） |
| `insert` | INSERT | 插入线索 |
| `insertSelective` | INSERT | 选择性插入线索 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新线索 |
| `updateByPrimaryKey` | UPDATE | 全字段更新线索 |
| `batchDeleteByIds` | DELETE | 批量删除线索 |

### 17.2 TCustomerMapper.xml (t_customer)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectCustomerPage` | SELECT | 客户分页查询（多表关联） |
| `selectCustomerByExcel` | SELECT | 导出 Excel 查询 |
| `selectByCount` | SELECT | 客户总数统计 |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByQuery` | SELECT | 按条件查询客户 |
| `selectCustomerOptions` | SELECT | 客户选项（下拉框） |
| `deleteByPrimaryKey` | DELETE | 删除客户 |
| `insert` | INSERT | 插入客户 |
| `insertSelective` | INSERT | 选择性插入客户 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新客户 |
| `updateByPrimaryKey` | UPDATE | 全字段更新客户 |

### 17.3 TTranMapper.xml (t_tran)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByQuery` | SELECT | 交易列表查询（多表关联+动态条件） |
| `selectBySuccessTranAmount` | SELECT | 成功交易总额 |
| `selectByTotalTranAmount` | SELECT | 总交易额 |
| `selectByTotalTranCount` | SELECT | 交易客户数 |
| `selectBySuccessTranCount` | SELECT | 成交客户数 |
| `selectByPrimaryKey` | SELECT | 交易详情 |
| `selectTranProductsByTranId` | SELECT | 交易产品列表 |
| `selectTranWithApproveByTranId` | SELECT | 交易+审批联合查询 |
| `deleteByPrimaryKey` | DELETE | 删除交易 |
| `deleteByIds` | DELETE | 批量删除交易 |
| `insert` | INSERT | 插入交易 |
| `insertSelective` | INSERT | 选择性插入交易 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新交易 |
| `updateByPrimaryKey` | UPDATE | 全字段更新交易 |

### 17.4 TUserMapper.xml (t_user)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByLoginAct` | SELECT | 按登录账号查询（登录用） |
| `selectUserByPage` | SELECT | 用户分页查询（支持数据权限） |
| `selectDetailById` | SELECT | 用户详情（关联创建人/编辑人） |
| `selectByOwner` | SELECT | 查询所有用户（负责人列表） |
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `deleteByPrimaryKey` | DELETE | 删除用户 |
| `deleteByIds` | DELETE | 批量删除用户 |
| `insert` | INSERT | 插入用户 |
| `insertSelective` | INSERT | 选择性插入用户 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新用户 |
| `updateByPrimaryKey` | UPDATE | 全字段更新用户 |

### 17.5 TActivityMapper.xml (t_activity)

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

### 17.6 TProductMapper.xml (t_product)

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

### 17.7 DicMapper.xml (t_dic_type, t_dic_value)

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
| `deleteRemarksByDicValueId` | DELETE | 删除关联的交易备注 |
| `deleteRemarksByDicValueIds` | DELETE | 批量删除关联的交易备注 |

### 17.8 TTranProductMapper.xml (t_tran_product)

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

### 17.9 TTranInvoiceMapper.xml (t_tran_invoice)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询发票列表 |
| `deleteByPrimaryKey` | DELETE | 删除发票 |
| `insert` | INSERT | 插入发票 |
| `insertSelective` | INSERT | 选择性插入发票 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新发票 |
| `updateByPrimaryKey` | UPDATE | 全字段更新发票 |

### 17.10 TTranApproveMapper.xml (t_tran_approve)

| SQL ID | 类型 | 用途 |
|--------|------|------|
| `selectByPrimaryKey` | SELECT | 按主键查询 |
| `selectByTranId` | SELECT | 按交易ID查询审批记录 |
| `insert` | INSERT | 插入审批记录 |
| `insertSelective` | INSERT | 选择性插入审批记录 |
| `updateByPrimaryKeySelective` | UPDATE | 选择性更新 |
| `updateByPrimaryKey` | UPDATE | 全字段更新 |

### 17.11 TTranRemarkMapper.xml (t_tran_remark)

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

## 18. 数据库表汇总

| 表名 | 说明 | 主要字段 |
|------|------|---------|
| `t_user` | 用户表 | id, login_act, login_pwd, name, phone, email, account_enabled |
| `t_role` | 角色表 | id, role, role_name |
| `t_user_role` | 用户角色关联表 | id, user_id, role_id |
| `t_permission` | 权限表 | id, name, code, url, type, parent_id |
| `t_role_permission` | 角色权限关联表 | id, role_id, permission_id |
| `t_clue` | 线索表 | id, owner_id, activity_id, full_name, phone, state, source |
| `t_clue_remark` | 线索跟踪记录表 | id, clue_id, note_way, note_content |
| `t_customer` | 客户表 | id, clue_id, product, description |
| `t_customer_remark` | 客户跟踪记录表 | id, customer_id, note_way, note_content |
| `t_tran` | 交易表 | id, tran_no, customer_id, money, stage |
| `t_tran_product` | 交易产品关联表 | id, tran_id, product_id, quantity, price |
| `t_tran_invoice` | 交易发票表 | id, tran_id, invoice_no, amount, status |
| `t_tran_approve` | 交易审批表 | id, tran_id, approve_result, approve_comment |
| `t_tran_remark` | 交易跟踪记录表 | id, tran_id, note_way, note_content |
| `t_activity` | 市场活动表 | id, owner_id, name, start_time, end_time, cost |
| `t_activity_remark` | 活动备注表 | id, activity_id, note_content |
| `t_product` | 产品表 | id, sku, name, category, price, stock, min_stock, status |
| `t_product_category` | 产品分类表 | id, name, code, description, sort, status |
| `t_product_promotion` | 产品促销表 | id, name, type, discount, start_time, end_time, status |
| `t_product_stock_record` | 库存变动记录表 | id, product_id, quantity, type, remark |
| `t_dic_type` | 字典类型表 | id, type_code, type_name, remark |
| `t_dic_value` | 字典值表 | id, type_code, type_value, order, remark |

---

## 附录：常量定义

**路径**: `constant/Constants.java`

| 常量 | 值 | 用途 |
|------|-----|------|
| `LOGIN_URI` | `/api/login` | 登录接口 |
| `REDIS_JWT_KEY` | `cdrm:user:login:` | JWT Redis Key 前缀 |
| `REDIS_OWNER_KEY` | `cdrm:user:owner` | 负责人列表 Redis Key |
| `EXPIRE_TIME` | `7 * 24 * 60 * 60L` | JWT 过期时间（7天） |
| `DEFAULT_EXPIRE_TIME` | `30 * 60L` | JWT 默认过期时间（30分钟） |
| `PAGE_SIZE` | `10` | 分页每页条数 |
| `CACHE_EXPIRE_TIME` | `24 * 60 * 60L` | 缓存过期时间（1天） |
| `CACHE_KEY_TRAN` | `cdrm:tran:detail:` | 交易详情缓存前缀 |
| `CACHE_KEY_TRAN_LIST` | `cdrm:tran:list:` | 交易列表缓存前缀 |
| `CACHE_KEY_TRAN_PRODUCTS` | `cdrm:tran:products:` | 交易产品缓存前缀 |
| `CACHE_KEY_TRAN_INVOICES` | `cdrm:tran:invoices:` | 交易发票缓存前缀 |
