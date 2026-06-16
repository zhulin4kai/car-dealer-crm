# API契约DTO与异常响应

来源：[后端模型与服务审查索引](../2026-06-16-后端模型与服务审查.md)

修复目标：统一请求 DTO、Query、响应包装和异常体系，减少 controller 继续直接接收 model 或 Map。

## 建议修复顺序

1. 43. Controller 参数绑定风格不统一，缺少系统性校验

2. 44. 统一响应包装类重复

3. 46. `TranCreateRequest` 缺少字段级校验，控制器只能靠手写判断兜底

4. 47. `CustomerQuery` 同时承担查询和转换命令，且不继承 `BaseQuery`

5. 50. `UserQuery` 暴露账号状态字段且缺少新增/编辑校验

6. 51. 备注 Query 缺少父对象和内容校验

7. 55. `Result` 与 `R` 两套响应包装并存，`Result` 当前没有实际使用价值

8. 59. 缺少分层明确的请求 DTO，导致 controller 直接接收 model 或共用 Query

9. 61. 缺少统一业务异常类型，当前大量使用裸 `RuntimeException`


## 问题详情

### 43. Controller 参数绑定风格不统一，缺少系统性校验

类型：编码规范 / 健壮性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/web/ClueController.java:51`
- `dealer-server/src/main/java/com/autodealer/crm/web/ClueController.java:67`
- `dealer-server/src/main/java/com/autodealer/crm/web/ActivityController.java:39`
- `dealer-server/src/main/java/com/autodealer/crm/web/ProductController.java:32`
- `dealer-server/src/main/java/com/autodealer/crm/web/SystemController.java:29`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:57`

问题：
有些写接口使用表单对象隐式绑定，有些使用 `@RequestBody`，只有少数使用 `@Valid`。请求 DTO 与数据库 model 混用明显，如产品和系统配置直接接收 model。

影响：
- 前端调用方式不统一。
- 字段级校验分散，容易把不该由客户端控制的字段写入数据库。
- 错误信息不可预测。

建议：
- 写接口统一使用 Request DTO + `@Valid`。
- model 不直接作为外部请求入参。
- Controller 只做参数校验和身份获取，业务规则放 service。


### 44. 统一响应包装类重复

类型：编码规范 / API 契约

位置：
- `dealer-server/src/main/java/com/autodealer/crm/result/R.java:10`
- `dealer-server/src/main/java/com/autodealer/crm/result/Result.java:6`

问题：
项目中同时存在 `R` 和 `Result` 两套返回包装类，字段语义几乎一致，但静态方法、默认文案和使用习惯不同。

影响：
- 新接口容易选错响应类型。
- 前端 API 契约和全局异常处理难以统一。

建议：
- 只保留一套响应包装。
- 对历史接口做兼容迁移，禁止新增代码使用另一套。


### 46. `TranCreateRequest` 缺少字段级校验，控制器只能靠手写判断兜底

类型：DTO 契约 / 请求校验 / 业务健壮性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:11`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:16`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:22`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:25`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:31`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:39`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:42`
- `dealer-server/src/main/java/com/autodealer/crm/dto/TranCreateRequest.java:45`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:57`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:81`

问题：
交易创建/更新请求 DTO 没有 `@NotNull`、`@NotEmpty`、`@Positive`、`@DecimalMin` 等校验。`expectedDeliveryDate` 使用字符串，让 controller 手动 `SimpleDateFormat` 解析。

影响：
- 空产品列表、空客户、非法数量、非法金额都可能进入业务层或触发 NPE。
- DTO 没有表达交易请求的最小合法形态，导致校验散落在 controller/service 中。

建议：
- DTO 中直接使用日期类型，并声明格式化/JSON 规则。
- 创建交易和更新交易拆成不同 Request DTO。
- `products` 至少 1 项，`quantity > 0`，`price >= 0`，`customerId/productId` 必填。


### 47. `CustomerQuery` 同时承担查询和转换命令，且不继承 `BaseQuery`

类型：DTO/Query 设计 / 数据权限 / 审计风险

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/CustomerQuery.java:12`
- `dealer-server/src/main/java/com/autodealer/crm/query/CustomerQuery.java:20`
- `dealer-server/src/main/java/com/autodealer/crm/query/CustomerQuery.java:22`
- `dealer-server/src/main/java/com/autodealer/crm/query/CustomerQuery.java:24`
- `dealer-server/src/main/java/com/autodealer/crm/query/CustomerQuery.java:31`
- `dealer-server/src/main/java/com/autodealer/crm/web/CustomerController.java:39`
- `dealer-server/src/main/java/com/autodealer/crm/web/CustomerController.java:63`
- `dealer-server/src/main/resources/mapper/TCustomerMapper.xml:263`
- `dealer-server/src/main/resources/mapper/TCustomerMapper.xml:291`

问题：
`CustomerQuery` 被客户列表查询和线索转客户共用，既有筛选字段 `customerName/productId`，又有转换字段 `clueId/product/description/quantity`，还有客户端可传的 `createBy`。它没有继承 `BaseQuery`，因此也无法被 `DataScopeAspect` 注入统一的数据权限条件。

影响：
- 查询 DTO 和命令 DTO 混用，字段语义不清。
- 客户列表只能通过客户端传入 `createBy` 过滤，无法统一按当前用户做数据权限。
- 前文第 2 条的审计字段伪造问题在 DTO 设计层也有根源。

建议：
- 拆分为 `CustomerListQuery` 和 `ConvertCustomerRequest`。
- 列表查询继承或组合统一的数据权限条件。
- 转换命令不暴露 `createBy`，操作者只来自登录上下文。


### 50. `UserQuery` 暴露账号状态字段且缺少新增/编辑校验

类型：Query 设计 / 安全与账号治理

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:16`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:21`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:31`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:36`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:41`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:46`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:51`
- `dealer-server/src/main/java/com/autodealer/crm/query/UserQuery.java:56`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/UserServiceImpl.java:121`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/UserServiceImpl.java:146`

问题：
`UserQuery` 对登录账号、密码、姓名、手机号、邮箱、账号状态字段都没有 Bean Validation。新增和编辑共用同一个 Query，客户端可以提交 `accountNoExpired/accountNoLocked/accountEnabled` 等账号状态字段。

影响：
- 新增用户时空密码、非法邮箱、重复手机号等问题不能在 DTO 层拦截。
- 账号启禁用、锁定等状态缺少专门命令和权限边界。

建议：
- 拆分 `CreateUserRequest`、`UpdateUserRequest`、`ChangeUserStatusRequest`。
- 新增时密码必填，编辑时密码可选但需满足策略。
- 状态变更走单独接口和权限。


### 51. 备注 Query 缺少父对象和内容校验

类型：Query 设计 / 请求校验

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/ActivityRemarkQuery.java:10`
- `dealer-server/src/main/java/com/autodealer/crm/query/ActivityRemarkQuery.java:12`
- `dealer-server/src/main/java/com/autodealer/crm/query/ClueRemarkQuery.java:8`
- `dealer-server/src/main/java/com/autodealer/crm/query/ClueRemarkQuery.java:10`
- `dealer-server/src/main/java/com/autodealer/crm/query/ClueRemarkQuery.java:12`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ActivityRemarkServiceImpl.java:31`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ClueRemarkServiceImpl.java:31`

问题：
活动备注和线索备注的 Query 没有校验父对象 ID、备注内容、备注方式。service 通过 `BeanUtils.copyProperties` 直接复制到 model，然后入库。

影响：
- 可能出现没有归属对象的备注、空备注、非法备注方式。
- 数据库没有强外键时，这类脏数据会长期存在。

建议：
- `activityId/clueId` 必填，`noteContent` 非空且限制长度。
- `noteWay` 必须是合法字典值。
- 新增备注前确认父对象存在。


### 55. `Result` 与 `R` 两套响应包装并存，`Result` 当前没有实际使用价值

类型：冗余文件 / API 契约重复

位置：
- `dealer-server/src/main/java/com/autodealer/crm/result/Result.java:6`
- `dealer-server/src/main/java/com/autodealer/crm/result/Result.java:11`
- `dealer-server/src/main/java/com/autodealer/crm/result/Result.java:23`
- `dealer-server/src/main/java/com/autodealer/crm/result/R.java:10`
- `dealer-server/src/main/java/com/autodealer/crm/result/R.java:34`
- `dealer-server/src/main/java/com/autodealer/crm/config/handler/GlobalExceptionHandler.java:34`

问题：
项目同时存在 `Result` 和 `R` 两个统一响应类。当前 controller、过滤器、异常处理器主要使用 `R`，未看到 `Result` 在业务链路中被使用。

影响：
- 新增接口时开发者可能选择不同响应类，导致前端契约不一致。
- 两套静态方法和默认文案会让错误处理、文档生成、接口测试变复杂。
- 即使当前未使用，保留重复类也会增加维护成本。

建议：
- 标准化保留 `R` 或重命名成唯一响应类。
- 删除 `Result`，或标记废弃并禁止新增代码使用。
- 全局异常、认证失败、权限失败、controller 返回统一走同一响应类。


### 59. 缺少分层明确的请求 DTO，导致 controller 直接接收 model 或共用 Query

类型：缺失业务契约 / 分层不清

位置：
- `dealer-server/src/main/java/com/autodealer/crm/web/UserController.java:78`
- `dealer-server/src/main/java/com/autodealer/crm/web/UserController.java:92`
- `dealer-server/src/main/java/com/autodealer/crm/web/CustomerController.java:39`
- `dealer-server/src/main/java/com/autodealer/crm/web/CustomerController.java:63`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:57`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:101`
- `dealer-server/src/main/java/com/autodealer/crm/web/TranController.java:155`
- `dealer-server/src/main/java/com/autodealer/crm/web/ProductController.java:32`
- `dealer-server/src/main/java/com/autodealer/crm/web/SystemController.java:29`

问题：
多个业务动作缺少独立 Request DTO。当前常见模式是：
- 新增/编辑用户共用 `UserQuery`。
- 客户列表查询和线索转客户共用 `CustomerQuery`。
- 交易创建/更新共用 `TranCreateRequest`。
- 交易结算使用 `Map<String, Object>`。
- 商品、系统、分类、促销等接口直接接收 model。

影响：
- 创建、更新、状态变更、查询的字段边界混在一起，容易让客户端提交不应控制的字段。
- Bean Validation 很难做到按场景精确校验。
- API 文档和前后端契约不清晰。

建议：
优先补齐以下请求 DTO：
- `CreateUserRequest`
- `UpdateUserRequest`
- `ChangeUserStatusRequest`
- `CustomerListQuery`
- `ConvertCustomerRequest`
- `CreateTranRequest`
- `UpdateTranRequest`
- `SettleTranRequest`
- `RestockRequest`
- `CreateActivityRemarkRequest`
- `CreateClueRemarkRequest`

这些 DTO 不属于“新功能”，而是把已有功能的请求契约补完整。


### 61. 缺少统一业务异常类型，当前大量使用裸 `RuntimeException`

类型：缺失基础设施 / 错误处理

位置：
- `dealer-server/src/main/java/com/autodealer/crm/config/handler/GlobalExceptionHandler.java:40`
- `dealer-server/src/main/java/com/autodealer/crm/config/handler/GlobalExceptionHandler.java:44`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:85`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:173`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/CustomerServiceImpl.java:137`

问题：
业务失败大量直接抛 `RuntimeException`，全局异常处理会把 runtime exception 的 message 返回前端。

影响：
- 业务错误和系统错误混在一起。
- 错误码不可控，前端无法稳定区分“参数错误、权限错误、状态不允许、数据引用中”等场景。
- 异常 message 可能泄露内部细节。

建议：
- 补 `BusinessException` 和业务错误码枚举。
- 全局异常处理区分业务异常、参数校验异常、系统异常、数据库异常。
- service 抛业务异常时使用稳定错误码和安全对外文案。
