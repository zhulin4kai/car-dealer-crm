---
description: Java Spring Boot 后端的分层类型、命名、方法、注释、常量与格式规范。
globs: "dealer-server/src/**/*.java"
---

# Java 后端代码风格

## 通用命名

- 类、接口、枚举使用 `PascalCase`；方法、变量、参数使用 `lowerCamelCase`。
- 常量使用 `UPPER_SNAKE_CASE`；包名全小写；集合变量使用复数名。
- ID 必须带业务对象名，如 `userId`、`tranId`，禁止只写 `id` 后跨层传递。
- 布尔值使用 `is`、`has`、`can`、`should` 前缀，禁止含义模糊的 `flag`、`statusFlag`。
- 查询方法使用 `get/find/list/count/exists`，命令方法使用 `create/update/delete/approve/refund/cancel`。
- 禁止使用 `handleData`、`processInfo`、`doBusiness`、`execute` 等无法表达业务动作的名称。
- 一个 `.java` 文件只定义一个顶级类型，文件名必须与顶级类型一致。

## 依赖与类结构

- 新代码使用构造器注入和 `final` 字段，禁止新增字段式 `@Autowired` 或 `@Resource`。
- 字段顺序统一为：静态常量、依赖字段、状态字段；方法顺序统一为：构造器、public 方法、private 方法。
- Spring Bean 禁止保存请求级可变状态；共享集合必须说明线程安全策略。
- 依赖只能指向同层稳定接口或下一层，禁止 Controller 直接依赖 Mapper、RedisManager。
- 禁止通配符 import、未使用 import、注释掉的旧代码和仅为缩短名称创建的别名类型。

## Controller 类与方法

- 类命名为 `XxxController`，放在 `web` 包并使用 `@RestController`。
- Controller 只负责 HTTP 参数绑定、Bean Validation、权限入口、调用 Service 和组装统一响应 `R`。
- Controller 禁止编写业务状态迁移、事务、库存或金额计算，禁止直接调用 Mapper 或 RedisManager。
- 写接口必须使用用途明确的 Request DTO，禁止直接接收持久化 Model 承担新增或修改请求。
- 请求体使用 `@Valid @RequestBody`；路径、查询参数必须显式声明名称和是否必填。
- 权限使用 `@PreAuthorize` 声明，前端是否隐藏按钮不能替代后端权限校验。
- 方法按 HTTP 动作命名为 `listXxx/getXxx/createXxx/updateXxx/deleteXxx/exportXxx`，禁止 `save` 同时表示新增和修改。
- Controller 禁止捕获通用 `Exception` 后返回失败文案；异常统一交给全局异常处理器。
- Controller 禁止信任客户端提交的 `createBy`、`editBy`、`approveBy` 和数据权限字段。

## Service 接口与实现

- 接口命名为 `XxxService`，实现类命名为 `XxxServiceImpl` 并放在 `service.impl`，禁止 `IXxxService`。
- Service public 方法必须对应完整业务用例，名称必须表达业务动作，禁止只转发 Mapper。
- Service 负责业务校验、权限与数据范围、状态迁移、事务、并发控制及写入结果判断。
- Service 禁止依赖 `HttpServletRequest`、`HttpServletResponse`、HTTP 状态码或统一响应类 `R`。
- 当前操作者统一从 `CurrentUserProvider` 获取，禁止把客户端 userId 当作可信操作人。
- 多表写入或“校验后写入”必须由 public Service/Manager 方法建立事务边界。
- Mapper 写操作必须检查影响行数；`0` 行必须区分不存在、非法状态或并发冲突。
- 返回值必须表达结果语义；跨层返回禁止使用无结构的 `Map<String, Object>`。
- 私有校验方法使用 `requireXxx` 或 `validateXxx`，构造方法使用 `buildXxx`，缓存清理使用 `clearXxxCache`。
- 查询方法不得产生隐藏写入；命令方法不得通过名称伪装成查询。

## Manager 类与方法

- Manager 仅用于跨领域流程编排或基础设施封装，命名为 `XxxManager`。
- 单领域业务规则保留在对应 Service，禁止为每个 Service 创建一层同名透传 Manager。
- Manager public 方法必须表达完整编排目标，并明确事务、失败和补偿边界。
- 基础设施 Manager 不得内置某个业务领域的状态判断或错误文案。
- Redis Key 由 `RedisKeys` 构造，Manager 接收完整 Key，不得在内部猜测业务 Key。

## Mapper 接口与方法

- Mapper 命名为 `XxxMapper`，保持接口形式；SQL 由 Mapper XML 或明确注解承载。
- 方法使用 `select/insert/update/delete/count/exists` 表达持久化动作，禁止沿用 Service 的业务动作名。
- 多个简单参数必须使用 `@Param` 明确 SQL 参数名；复杂查询使用专用 Query 对象。
- 写方法返回影响行数，批量写方法必须明确空集合和最大数量行为。
- Mapper 禁止抛业务文案、修改业务状态机、访问当前用户或调用其他 Mapper。
- Mapper 返回 Model、标量或专用投影对象，禁止返回含义不明的 `Map`。

## Model 持久化对象

- 持久化对象放在 `model` 包；现有 `TDomain` 命名保持一致，未经整体迁移不得混用另一套命名。
- 字段使用 Java `lowerCamelCase` 对应数据库 `snake_case`，主外键类型必须与 Schema 一致。
- Model 只表达持久化数据和必要关联，不承担请求校验、权限判断或 Controller 响应结构。
- Model 禁止直接作为新增、修改接口的请求体；敏感字段禁止进入通用响应。
- 金额使用 `BigDecimal`，状态使用领域枚举或稳定 code，禁止使用枚举 ordinal 持久化。
- `equals`、`hashCode`、`toString` 不得意外包含密码、Token、大型关联或循环引用。

## DTO、Query 与 Response

- 类型名必须体现方向和用途，如 `CreateTranRequest`、`TranQuery`、`TranDetailResponse`。
- 禁止使用 `DataDTO`、`CommonDTO`、`ResultInfo` 等无法说明场景的名称。
- Request 只包含客户端可提交字段，并使用 Bean Validation 表达格式和必填约束。
- Query 只承载筛选、排序和分页条件，不得携带操作者或服务端审计字段。
- Response 只暴露接口契约需要的数据，禁止继承 Model 或原样返回带敏感字段的对象。
- DTO 转换必须集中在明确的转换方法或转换器中，禁止在多个 Controller 重复复制字段。
- 可空字段必须有明确业务含义；集合返回空集合，禁止用 `null` 同时表达“无数据”和“未加载”。

## 枚举、常量与配置

- 业务状态、支付方式等有限值使用领域枚举；枚举必须保存稳定 code，禁止依赖 ordinal。
- 状态解析必须提供统一方法，未知 code 必须显式失败，禁止静默选择默认状态。
- 常量按最小共享范围定义，仅当前类使用的常量必须留在当前类。
- 跨类常量按业务领域或基础设施职责拆分，禁止建立无明确职责的全局 `Constants` 类。
- Redis Key、错误码和安全路径分别由 `RedisKeys`、`CodeEnum`、`SecurityPaths` 统一定义。
- 过期时间、批量上限等需要按环境调整的参数应进入配置类；秘密信息只能来自安全配置或环境变量。
- 空字符串、布尔值和简单循环边界不需要为消除字面量而提取为公共常量。
- 配置类只负责属性绑定和 Bean 装配，禁止承载业务流程或访问数据库。

## 安全组件、过滤器与处理器

- Filter 只处理请求级认证上下文，不承载业务权限和业务数据查询流程。
- 当前用户解析统一收口到 `CurrentUserProvider`，业务代码禁止自行解析 JWT。
- 认证成功、失败、退出和拒绝访问处理器必须使用统一响应与错误码。
- 安全失败默认关闭访问，禁止捕获异常后继续放行请求。
- Aspect 只处理稳定的横切关注点，禁止隐藏核心业务写入、状态迁移或权限判断。

## 异常处理

- 业务异常必须携带稳定 `CodeEnum`，禁止依赖中文异常文案驱动前端行为。
- Service 抛业务异常，不返回 `R.FAIL()`；Controller 不翻译同一异常。
- 全局异常处理器负责业务异常到 HTTP 状态和 `R` 的统一映射。
- 禁止吞异常、空 `catch`、`printStackTrace()` 或捕获后返回成功。
- 记录异常时必须传递异常对象；禁止只记录 `e.getMessage()` 后丢失堆栈。
- 同一异常只在责任边界记录一次，具体日志要求遵守 `12-日志与审计规范.md`。

## 工具类

- 无状态工具类使用 `final` 类和 private 构造器，方法保持纯函数或明确副作用。
- 需要 Spring 依赖、配置、缓存或数据库的能力不得伪装成静态工具类，应建为 Component 或 Manager。
- 工具方法禁止捕获所有异常后返回 `null`、空字符串或 `false` 隐藏失败。
- JSON、时间、加密等基础能力必须复用统一工具或框架配置，禁止业务类自行创建全局配置实例。

## 方法与控制流

- 一个方法只完成一个主要动作；参数超过 4 个时应使用用途明确的参数对象。
- 方法超过约 40 行或嵌套超过 3 层时应拆分，但不得拆成无业务语义的碎片方法。
- 优先使用提前返回处理非法输入，禁止多层 `if/else` 掩盖主流程。
- 输入参数在入口完成校验，方法内部不得反复猜测 `null` 的业务含义。
- 禁止通过修改入参制造隐藏副作用；必须修改时在方法名或 JavaDoc 中说明。
- 时间、当前用户和随机标识等外部上下文应由统一组件提供，便于测试和审计。

## JavaDoc 与注释

- 标识符使用英文；注释和 JavaDoc 使用中文。
- public 业务类必须说明职责和边界。
- Service public 方法必须说明前置条件、状态迁移、副作用、事务、幂等性和异常。
- `@param`、`@return`、`@throws` 必须描述业务含义，禁止复述类型或方法名。
- Controller 的简单 CRUD 映射、getter、setter 和显然的构造器不写无意义 JavaDoc。
- 注释解释原因、约束和非显然取舍，不逐行翻译代码。
- TODO 使用 `TODO(issue-编号): 内容`，必须有跟踪编号；禁止保留无归属 TODO 和注释掉的旧代码。

## 格式

- UTF-8、LF、4 空格缩进、禁止 Tab，文件末尾保留换行。
- 一行不超过 120 字符；超长参数、链式调用和日志参数按语义换行。
- 同一模块保持注解顺序、空行、括号和链式调用风格一致。
- 自动格式化只能作用于任务涉及的文件，禁止制造全项目无关格式差异。
