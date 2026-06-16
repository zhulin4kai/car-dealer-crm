# 后端模型与服务规范

适用范围：
- `dealer-server/src/main/java/com/autodealer/crm/model/`
- `dealer-server/src/main/java/com/autodealer/crm/query/`
- `dealer-server/src/main/java/com/autodealer/crm/service/`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/`
- `dealer-server/src/main/java/com/autodealer/crm/web/`
- `dealer-server/src/main/java/com/autodealer/crm/config/`
- `dealer-server/src/main/java/com/autodealer/crm/aspect/`
- `dealer-server/src/main/java/com/autodealer/crm/manager/`
- `dealer-server/src/main/resources/*.sql`
- `dealer-server/src/main/resources/mapper/*.xml`

目标：
- 让 SQL、模型、mapper、service、controller、认证授权和缓存的业务契约一致。
- 降低状态漂移、孤儿数据、审计不可信、库存/财务重复处理的风险。
- 规范文档与注释，不追求机械堆注释。

## SQL 规范

1. 主 schema 必须是领域模型的权威契约。
2. 每张表必须明确：
   - 主键
   - 必填字段
   - 唯一约束
   - 外键或等效业务约束
   - 状态字段可选值
   - 是否允许物理删除
3. 字典字段必须在注释中标明引用关系：
   - 示例：`引用 t_dic_value.id，type_code=needLoan`
   - 禁止同一字段注释写 `0/1`，实际却存字典 ID。
4. 状态字段优先使用稳定 code：
   - 示例：`on_sale/off_sale`
   - 示例：`QUOTATION/PENDING/APPROVED/PAYMENT/COMPLETED/LOST`
5. 业务数据不得依赖自增 ID 表达语义；可用 ID 做外键，但逻辑判断应使用 code。
6. 生产 schema、测试 schema、初始化数据必须保持同一字段契约。
7. 核心业务表不建议物理删除：
   - 交易、支付、发票、库存记录、审批记录、历史记录应保留审计链。
   - 可删除数据也必须先定义关联处理策略。

## Model 规范

1. model 类需要类级 JavaDoc，至少包含：
   - 中文业务名
   - 对应表名
   - 关键业务边界
2. 数据库字段需要字段级注释，说明字段含义。
3. 非数据库字段必须标明：
   - `非数据库字段`
   - 来源表或计算来源
4. 同一领域内 ID 类型应统一。
   - 已有老表以 `Integer` 为主，产品新表以 `Long` 为主时，需要在 mapper/service 边界显式转换。
   - 禁止隐式假设所有 ID 都可安全互转。
5. 时间类型应按模块统一。
   - 若使用 `LocalDateTime`，需确认 MyBatis、Jackson、Redis 序列化配置完整。
   - 若使用 `Date`，需明确时区和格式化策略。
6. 校验注解与 SQL 约束必须一致。
   - `@NotBlank` 对应数据库 `NOT NULL` 和必要的唯一约束。
   - `@Min(0)` 对应数据库非负约束或 service 层兜底。

## Query/DTO 规范

1. Query/DTO 分为三类，不混用：
   - 查询条件 DTO
   - 创建/更新命令 DTO
   - 响应 DTO
2. 客户端不得提交审计字段作为最终可信值：
   - `createBy`
   - `editBy`
   - `approveBy`
   - `paymentBy`
3. 审计字段必须由 Controller 或 Service 从登录上下文获取。
4. 继承 `BaseQuery` 的类必须显式声明 Lombok 父类策略：
   - `@EqualsAndHashCode(callSuper = true)`
   - 或 `@EqualsAndHashCode(callSuper = false)`
5. `BaseQuery` 默认分页值若使用 builder，必须使用 `@Builder.Default`。
6. SQL 片段类字段必须禁止客户端传入。
   - `filterSQL` 只能由服务端白名单逻辑生成。

## Mapper 规范

1. mapper XML 不直接拼接不可信输入。
   - 优先使用 `#{}`。
   - `${}` 只能用于服务端白名单生成的列名、排序或权限片段。
2. 关联查询必须为每个关联 ID 使用稳定别名。
3. 分页策略只能选一种：
   - PageHelper
   - 手写 `LIMIT/OFFSET`
4. 状态更新需要使用 CAS 时，SQL 必须带当前状态条件。
5. 删除 SQL 应尽量带业务状态条件，避免 service 层遗漏。

## Service 规范

Service 接口是业务契约层。公共方法 JavaDoc 必须说明：
- 方法用途
- 关键参数含义
- 返回值含义
- 可能抛出的业务异常
- 是否开启事务
- 会修改哪些表
- 状态迁移规则
- 是否要求幂等

示例：

```java
/**
 * 审批待审批交易。
 *
 * 前置条件：交易必须处于 PENDING。
 * 状态迁移：通过时 PENDING -> APPROVED，拒绝时 PENDING -> LOST。
 * 副作用：写入 t_tran_approve 和 t_tran_history。
 * 幂等性：同一交易只允许存在一条有效审批记录。
 *
 * @param tranId 交易ID
 * @param approved 是否通过
 * @param comment 审批意见
 * @param approveBy 审批人ID，必须来自登录上下文
 * @return 审批成功返回 true
 */
boolean approveTran(Integer tranId, Boolean approved, String comment, Integer approveBy);
```

不要求给所有私有小函数机械添加 JavaDoc。私有函数只有在包含复杂业务规则、状态机、并发控制或非显然算法时才需要注释。

## 事务与幂等规范

1. 一个业务动作修改多张表时必须加事务。
2. 涉及库存、支付、审批、状态迁移时必须考虑并发。
3. 可重复点击的接口必须有幂等设计：
   - 创建交易
   - 审批交易
   - 创建发票
   - 记录收款
   - 退款
   - 批量删除
4. 库存变更必须有记录表，且记录应包含：
   - 产品 ID
   - 数量变化
   - 业务来源类型
   - 来源业务 ID
   - 操作人
   - 创建时间
5. 财务记录不得物理删除，退款应追加反向流水。

## 状态机规范

交易状态：
- `QUOTATION`：待报价，可编辑产品和金额，可删除。
- `PENDING`：待审批，不可编辑金额和产品。
- `APPROVED`：已审批，可创建发票。
- `PAYMENT`：待收款，可记录收款。
- `COMPLETED`：已完成，不可删除。
- `LOST`：丢失关闭，可按明确规则重新提交。

状态迁移必须通过 service 方法完成，不允许 controller 或 mapper 被随意调用改状态。

每次状态迁移必须写入历史：
- 交易 ID
- 新状态 code
- 金额
- 预计日期
- 操作人
- 操作时间

## Controller 规范

1. Controller 只做：
   - 参数接收
   - 登录用户提取
   - 基础格式校验
   - 调用 service
2. Controller 不直接写业务状态。
3. 当前用户必须从 `SecurityContext` 或统一认证工具获取。
4. 需要权限的接口必须有 `@PreAuthorize`。
5. 错误信息应由业务异常统一处理，不建议到处 `try/catch` 返回不一致格式。
6. 写接口必须使用 Request DTO + `@Valid`，不直接接收数据库 model。
7. Controller 不信任客户端传入的审计字段、状态字段和金额类最终值。

## 认证授权规范

1. Security 白名单和自定义认证过滤器必须使用同一套 matcher 规则。
2. 所有公开接口、登录接口、验证码接口、CORS `OPTIONS` 请求必须显式跳过 token 校验。
3. JWT 只保存最小身份信息：
   - 用户 ID
   - 登录账号
   - 权限版本或会话 ID
   - 必要的过期信息
4. JWT 不保存密码哈希、手机号、邮箱等非鉴权必需信息。
5. token 生命周期只能有一个权威来源：
   - 若使用 JWT 过期时间，则 Redis 不能表达更长的登录有效期。
   - 若使用 Redis session，则 JWT 中应保存 session id 或短期凭证。
6. 退出登录使用非 GET 方法，并按幂等接口处理。
7. 登录失败对外返回统一文案，具体原因写入服务端日志。
8. 后端权限必须覆盖所有敏感动作：
   - 查询
   - 详情
   - 新增
   - 编辑
   - 删除
   - 导入
   - 导出
   - 刷新缓存
   - 系统监控

## 导入导出规范

1. 导入接口必须校验文件存在、大小、格式和解析错误。
2. 导入数据必须先校验再入库：
   - 必填字段
   - 唯一字段
   - 字典值合法性
   - 外键存在性
   - 金额和数量范围
3. 导入失败应返回行号和字段级错误，不允许用 `-1` 等哨兵值入库。
4. 批量入库前必须判断列表非空。
5. 导出必须限制最大数量，未传 ID 或筛选条件时不得默认导出全量。
6. 导出不得通过 URL 参数传递长期 token。

## 缓存规范

1. 缓存 key 必须集中定义，避免同一业务出现多套命名。
2. 缓存清理方法的名称、入参和实际行为必须一致。
3. 禁止在生产路径使用 Redis `KEYS` 做模式删除，应使用 `SCAN` 或版本化 key。
4. 本地静态缓存不能作为跨实例一致性的权威数据源。
5. RedisTemplate 序列化器必须用 Bean 显式配置，避免运行期修改。
6. 不启用不受控的 Jackson default typing。

## 操作审计规范

1. 关键业务动作必须写操作日志：
   - 用户新增/禁用/授权
   - 字典新增/修改/删除
   - 商品、分类、促销、库存调整
   - 线索导入/转换/删除
   - 交易创建/审批/开票/收款/退款/删除
   - 系统配置修改
2. 操作日志至少记录：
   - 操作人 ID 和名称
   - 模块
   - 操作类型
   - 业务记录 ID
   - 变更摘要
   - IP
   - 操作时间
3. 审计日志不得因为删除业务数据而被级联删除。

## 删除策略

默认策略：
- 核心业务数据：逻辑删除或状态关闭。
- 审计数据：不删除。
- 字典、产品、客户、活动：若已被引用，禁止物理删除。

允许物理删除前必须检查：
- 是否被其他表引用。
- 是否会影响统计。
- 是否会破坏审计链。
- 是否需要恢复库存或撤销衍生记录。

## 编写风格

1. 使用 `@Resource` 或构造器注入，避免同一模块混用多种注入方式。
2. 注释解释业务意图，不重复代码字面含义。
3. 避免行尾长注释，优先字段 JavaDoc。
4. 常量集中定义，不在 service 中散落状态字符串。
5. Mapper 方法名应准确：
   - 避免拼写错误，如 `selecOngoingActivity`。
   - 废弃方法必须有迁移目标和删除计划。
