# 数据模型与SQL契约

来源：[后端模型与服务审查索引](../2026-06-16-后端模型与服务审查.md)

修复目标：修复数据库 schema、字典、状态、约束、删除策略和统计口径，让业务修复有持久层保护。

## 建议修复顺序

1. 3. 数据库没有外键，模型关联只能靠业务代码维持

2. 4. 字典 ID 与业务数据不一致，且字段注释与实际存储冲突

3. 5. 产品状态值不统一，导致上架车辆无法被业务查询到

4. 9. 生产 SQL、测试 schema、初始化数据结构不一致

5. 10. `TProduct` 模型与 SQL 约束不匹配

6. 11. 产品分页同时使用 PageHelper 和手写 offset，分页语义混乱

7. 12. 线索手机号唯一性只靠先查再插，不具备并发安全

8. 13. 线索更新禁止修改手机号的逻辑与用户意图相反

9. 14. 活动删除是物理删除，没有处理线索关联

10. 15. 产品删除没有检查交易产品、线索意向、客户选购引用

11. 17. `BaseQuery` 默认分页值在 Builder 下失效

12. 39. 字典删除会删除交易备注，且引用检查范围不足

13. 41. 统计漏斗中的“交易数”实际统计的是有交易客户数

14. 48. `DicQuery` 同时存在两套分页字段，且缺少默认值

15. 49. `DicQuery` 的值查询条件用 OR 拼接，可能扩大查询范围


## 问题详情

### 3. 数据库没有外键，模型关联只能靠业务代码维持

类型：基础模型 / SQL 结构

位置：
- `dealer-server/src/main/resources/CarDealerCRM.sql:104`
- `dealer-server/src/main/resources/CarDealerCRM.sql:222`
- `dealer-server/src/main/resources/CarDealerCRM.sql:933`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1039`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1094`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1129`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1325`

问题：
SQL 里为 `owner_id`、`clue_id`、`customer_id`、`tran_id`、`product_id`、`create_by` 等列建了索引，但没有 `FOREIGN KEY`。例如交易可引用不存在的客户，交易产品可引用不存在的产品，客户可引用不存在的线索。

影响：
- 删除活动、线索、客户、产品、交易时容易留下孤儿数据。
- mapper 的 `LEFT JOIN` 会返回空关联对象，业务层可能继续处理错误数据。
- 财务和库存数据没有数据库级完整性保护。

建议：
- 先梳理删除策略：核心主数据建议限制删除，备注/历史可级联或逻辑删除。
- 对 `t_customer.clue_id`、`t_tran.customer_id`、`t_tran_product.tran_id/product_id`、`t_payment.tran_id`、`t_tran_invoice.tran_id`、`t_tran_approve.tran_id` 等补充外键。
- 若暂时不能加外键，也应在 service 中补全存在性校验，并在 SQL 中补唯一约束和非空约束。


### 4. 字典 ID 与业务数据不一致，且字段注释与实际存储冲突

类型：基础模型 / SQL 数据 / 业务语义

位置：
- `dealer-server/src/main/resources/CarDealerCRM.sql:119`
- `dealer-server/src/main/resources/CarDealerCRM.sql:150`
- `dealer-server/src/main/resources/CarDealerCRM.sql:399`
- `dealer-server/src/main/resources/CarDealerCRM.sql:445`
- `dealer-server/src/main/resources/CarDealerCRM.sql:469`
- `dealer-server/src/main/resources/data.sql:236`
- `dealer-server/src/main/resources/data.sql:240`

问题：
`t_clue.need_loan` 注释写的是 `0/1`，但数据中使用的是字典 ID `49/50`。同类问题还出现在 `appellation`、`intention_state`、`state`、`source`。同时 `CarDealerCRM.sql` 中 `stage` 字典 ID 从 12、37、42 等开始，而 `data.sql` 中 stage 是 22、23、26；线索数据里使用的字典值 ID 也与测试数据不一致。

影响：
- 同一字段到底是布尔、字典 ID、枚举值不清晰。
- 初始数据、测试数据、代码常量之间不可互换。
- 迁移或重建环境后，页面展示可能错乱。

建议：
- 字段注释改成“引用 `t_dic_value.id`，type_code=xxx”，不要再写 `0/1`。
- 字典数据使用稳定 code，而不是在业务逻辑中依赖自增 ID。
- 生产 SQL 与测试 SQL 保持同一组字典 code/value 契约。


### 5. 产品状态值不统一，导致上架车辆无法被业务查询到

类型：模型 / SQL 数据 / 业务逻辑

位置：
- `dealer-server/src/main/resources/CarDealerCRM.sql:1139`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1147`
- `dealer-server/src/main/resources/mapper/TProductMapper.xml:121`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ProductServiceImpl.java:125`

问题：
生产 SQL 初始化的产品状态为中文 `上架`，但 mapper 和 service 判断的是英文 code `on_sale`。`selectAllOnSale` 只查询 `p.status = 'on_sale'`，因此 SQL 初始化出来的产品不会出现在“在售产品”列表中。

影响：
- 线索意向产品下拉、客户转换、交易选品可能拿不到可售车辆。
- `ProductSimpleDTO.state` 会把中文 `上架` 映射成非在售。

建议：
- 产品状态统一为 code：`on_sale/off_sale`，展示层再翻译为中文。
- SQL 初始化数据、枚举/常量、前端选项全部使用同一套状态 code。
- 对 `status` 加非空和可选值约束。


### 9. 生产 SQL、测试 schema、初始化数据结构不一致

类型：SQL 结构 / 环境一致性

位置：
- `dealer-server/src/main/resources/schema-test.sql:218`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1069`
- `dealer-server/src/main/resources/schema-test.sql:269`
- `dealer-server/src/main/resources/schema-test.sql:346`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1325`

问题：
`schema-test.sql` 包含 `t_tran_production`，但 Java model/mapper/service 中没有对应模型和业务接口。`CarDealerCRM.sql` 也有该表，但当前交易生产状态只在 `TranQuery.productionStatus` 中出现，mapper 查询未使用。多个新增表在生产 SQL 和测试数据里字段/约束不完全同步。

影响：
- 测试环境和生产环境行为不一致。
- 未使用表会误导后续开发，业务状态可能重复建模。

建议：
- 明确每张表的 owner 和业务用途。
- 未接入业务的表应从主 schema 移出到待设计草案，或补齐 model/mapper/service。
- 建议使用迁移工具维护 schema，避免多个 SQL 文件长期漂移。


### 10. `TProduct` 模型与 SQL 约束不匹配

类型：模型 / SQL 约束

位置：
- `dealer-server/src/main/java/com/autodealer/crm/model/TProduct.java:21`
- `dealer-server/src/main/java/com/autodealer/crm/model/TProduct.java:31`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1132`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1136`
- `dealer-server/src/main/resources/CarDealerCRM.sql:1142`

问题：
`TProduct.sku` 有 `@NotBlank`，`price` 有 `@NotNull`，但 SQL 中 `sku` 和 `price` 都允许 `NULL`，且没有 `sku` 唯一约束。Java 校验只在 Controller 使用 `@Valid` 时生效，数据库仍可写入无效产品。

影响：
- 重复 SKU 会破坏 `getProductBySku` 的语义。
- 空价格产品在交易结算时可能导致 NPE 或错误金额。

建议：
- `t_product.sku`、`price`、`stock`、`status` 补充非空约束。
- 对 `sku` 添加唯一约束。
- service 层也做兜底校验，避免绕过 controller 写入坏数据。


### 11. 产品分页同时使用 PageHelper 和手写 offset，分页语义混乱

类型：业务逻辑 / 编写风格

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ProductServiceImpl.java:31`
- `dealer-server/src/main/resources/mapper/TProductMapper.xml:24`

问题：
`getProductList` 调用 `PageHelper.startPage(pageNum, pageSize)`，又向 `selectList((pageNum - 1) * pageSize, pageSize)` 传 offset/limit。但 mapper 的 `selectList` 没有使用 offset/limit 参数。

影响：
- 代码含义混乱，容易误以为 mapper 手动分页。
- 后续若 mapper 加上 `LIMIT`，会出现双重分页。

建议：
- 保留 PageHelper 时，mapper 不传 offset/limit。
- 若改手写分页，则移除 PageHelper 并显式编写 `LIMIT/OFFSET` 和 count。


### 12. 线索手机号唯一性只靠先查再插，不具备并发安全

类型：数据一致性 / 并发

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ClueServiceImpl.java:77`
- `dealer-server/src/main/resources/mapper/TClueMapper.xml:128`
- `dealer-server/src/main/resources/CarDealerCRM.sql:111`

问题：
保存线索时先 `selectByCount(phone)`，再插入。数据库没有对 `t_clue.phone` 加唯一约束，并发请求可以同时通过检查并插入重复手机号。

影响：
- 线索去重失效。
- 客户转换、跟进记录归属可能混乱。

建议：
- 对 `phone` 增加唯一约束，或按业务定义增加组合唯一键。
- service 捕获唯一键冲突并返回明确业务错误。


### 13. 线索更新禁止修改手机号的逻辑与用户意图相反

类型：业务逻辑 / 可维护性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ClueServiceImpl.java:118`

问题：
注释写“如果传入手机号与原记录不同，忽略手机号字段”，实际会静默丢弃用户提交的新手机号。若业务要求手机号不可改，应明确报错；若允许改，应校验唯一后更新。

影响：
- 用户以为更新成功，但手机号没有变化。
- 前后端状态可能不一致。

建议：
- 明确手机号更新规则。
- 不允许修改时返回失败；允许修改时做唯一性检查和审计记录。


### 14. 活动删除是物理删除，没有处理线索关联

类型：业务逻辑 / 数据完整性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ActivityServiceImpl.java:82`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ActivityServiceImpl.java:91`
- `dealer-server/src/main/resources/mapper/TActivityMapper.xml:113`
- `dealer-server/src/main/resources/mapper/TActivityMapper.xml:246`

问题：
活动可被直接物理删除，但线索表中 `activity_id` 仍会引用被删除活动。数据库没有外键限制，service 也没有检查是否被线索使用。

影响：
- 线索详情中的活动关联为空。
- 活动统计和线索来源分析会失真。

建议：
- 被线索引用的活动禁止删除，或只允许逻辑删除。
- 删除前检查 `t_clue.activity_id` 引用数量。


### 15. 产品删除没有检查交易产品、线索意向、客户选购引用

类型：业务逻辑 / 数据完整性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ProductServiceImpl.java:63`
- `dealer-server/src/main/resources/mapper/TProductMapper.xml:72`
- `dealer-server/src/main/resources/mapper/TTranProductMapper.xml:25`

问题：
`deleteProduct` 直接删除产品，没有检查 `t_tran_product.product_id`、`t_clue.intention_product`、`t_customer.product`。

影响：
- 历史交易产品名称、客户选购产品、线索意向产品关联断裂。
- 库存记录仍保留 product_id，但产品不存在。

建议：
- 已被交易引用的产品禁止物理删除，只允许下架。
- 业务删除应改为状态变更。


### 17. `BaseQuery` 默认分页值在 Builder 下失效

类型：语法/编译警告 / 稳健性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/BaseQuery.java:8`
- `dealer-server/src/main/java/com/autodealer/crm/query/BaseQuery.java:18`
- `dealer-server/src/main/java/com/autodealer/crm/query/BaseQuery.java:19`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ClueServiceImpl.java:55`

问题：
编译警告显示 Lombok `@Builder` 会忽略字段初始化表达式。`BaseQuery.builder().build()` 得到的 `current/pageSize` 会是 `null`，不是 `1/10`。

影响：
- 使用 builder 构造查询对象时分页默认值不可靠。
- 当前 `ClueServiceImpl` 使用 builder，但 PageHelper 参数由方法入参控制，暂未直接触发；后续扩展容易踩坑。

建议：
- 使用 `@Builder.Default`。
- 或移除 `BaseQuery` 上的 `@Builder`，改为显式构造。

## 低优先级问题


### 39. 字典删除会删除交易备注，且引用检查范围不足

类型：数据完整性 / 删除策略

位置：
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:169`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:205`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:275`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:303`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:187`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:225`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:285`

问题：
删除单个字典值和批量删除字典值时，会先删除 `t_tran_remark` 中关联备注。删除字典类型时只检查交易备注引用，未检查线索、客户、用户、交易等表中使用的字典字段。

影响：
- 删除字典值会直接丢失业务历史备注。
- 字典被删除后，其他业务表可能留下无法展示含义的数字 ID。

建议：
- 字典值被业务引用时应禁止删除，或采用禁用状态。
- 引用检查应覆盖所有使用该字典类型的业务字段。
- 禁止为了删除字典而删除业务记录。


### 41. 统计漏斗中的“交易数”实际统计的是有交易客户数

类型：业务逻辑 / 统计口径

位置：
- `dealer-server/src/main/java/com/autodealer/crm/manager/StatisticManager.java:75`
- `dealer-server/src/main/java/com/autodealer/crm/manager/StatisticManager.java:76`
- `dealer-server/src/main/resources/mapper/TTranMapper.xml:106`
- `dealer-server/src/main/resources/mapper/TTranMapper.xml:113`

问题：
`StatisticManager` 把 `selectByTotalTranCount` 作为“交易”数量，把 `selectBySuccessTranCount` 作为“成交”数量。但 mapper SQL 是 `SELECT DISTINCT customer_id FROM t_tran` 后再 count，实际统计的是“产生交易的客户数”和“成交客户数”。

影响：
- 销售漏斗数据口径错误。
- 若一个客户有多笔交易，统计值会偏低。

建议：
- 明确漏斗口径：交易单数、客户数、机会数只能选一种。
- 方法名、SQL 和前端展示文案保持一致。


### 48. `DicQuery` 同时存在两套分页字段，且缺少默认值

类型：Query 设计 / 分页健壮性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/BaseQuery.java:18`
- `dealer-server/src/main/java/com/autodealer/crm/query/BaseQuery.java:19`
- `dealer-server/src/main/java/com/autodealer/crm/query/DicQuery.java:19`
- `dealer-server/src/main/java/com/autodealer/crm/query/DicQuery.java:20`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:36`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java:44`

问题：
`DicQuery` 已继承 `BaseQuery.current/pageSize`，但自身又定义 `page/size`。`DicServiceImpl` 使用 `query.getPage()` 和 `query.getSize()` 调 PageHelper，controller 没有默认赋值。

影响：
- 未传 `page/size` 时可能触发空指针或分页异常。
- 同一个查询对象里出现 `current/pageSize` 和 `page/size` 两套概念，接口契约不清楚。

建议：
- 统一所有分页字段命名。
- 删除重复字段，或让 controller 明确默认值。
- BaseQuery builder 默认值问题按前文第 17 条一并修复。


### 49. `DicQuery` 的值查询条件用 OR 拼接，可能扩大查询范围

类型：查询逻辑 / 业务准确性

位置：
- `dealer-server/src/main/java/com/autodealer/crm/query/DicQuery.java:10`
- `dealer-server/src/main/java/com/autodealer/crm/query/DicQuery.java:13`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:47`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:49`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:52`
- `dealer-server/src/main/resources/mapper/DicMapper.xml:55`

问题：
字典值查询同时传 `typeCode` 和 `typeValue` 时，SQL 使用 `dv.type_code = ? OR dv.type_value LIKE ?`。通常字典值列表期望是在指定类型内按值搜索，应为 AND。

影响：
- 查询某个字典类型下的值时，会把其它类型中名称匹配的值也查出来。
- 前端维护字典值时容易误操作跨类型数据。

建议：
- 若语义是类型内搜索，改为 `AND`。
- 若确实需要全局搜索，应拆出专门接口并在命名上说明。
