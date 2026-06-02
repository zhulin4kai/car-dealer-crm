# dealer-server 后端业务实现 Review

审查时间: 2026-06-02  
审查范围: `dealer-server/src/main/java/com/autodealer/crm`、`dealer-server/src/main/resources/mapper`、关键后端文档中与实际代码冲突的部分。  
验证基线: `./mvnw -q -DskipTests compile` 通过；`./mvnw -q test` 通过。  
说明: 本报告关注真实业务实现和长期维护风险，不以测试通过作为业务正确的结论。

## 总结

后端目前最大的问题不是单点语法错误，而是同一业务概念被多套类、缓存工具和状态流转入口重复表达。交易、商品、字典、Redis、数据权限这几条链路已经出现实际功能风险: 批量删除交易绕过阶段校验，客户转换可创建无商品交易，字典缓存清理不完整，Redis 列表缓存会重复堆积且没有过期时间。

建议先处理 P0/P1，再统一模型/DTO/缓存/状态枚举。否则后续新增功能会继续在 `Product/TProduct`、`RedisManager/RedisService`、实体入参/Query 入参之间分叉。

## 问题清单

| ID | 等级 | 模块 | 问题 |
| --- | --- | --- | --- |
| B01 | P0 | 交易 | 批量删除交易绕过单条删除的阶段校验 |
| B02 | P0 | 客户转换 | 产品不存在时仍然创建交易，生成无商品交易 |
| B04 | P1 | 字典缓存 | `clearCache(pattern)` 忽略入参，且漏删 `dic:values:*` |
| B05 | P1 | Redis | `RedisManager` 和 `RedisService` 职责/命名冲突，同名方法语义不同 |
| B06 | P1 | Redis | 负责人缓存使用 List 存储，无过期、重复写入会累积重复数据 |
| B07 | P1 | 交易状态 | 多个交易操作缺少当前状态校验，状态机只局部存在 |
| B08 | P1 | 交易库存 | 交易商品增删和库存扣减/恢复重复实现，部分路径不检查库存恢复结果 |
| B09 | P1 | 数据权限 | 线索/交易查询的数据权限不完整，SQL 仍使用 `${filterSQL}` 拼接 |
| B10 | P1 | DTO | Controller 大量直接暴露实体或 `Map`，数据流转边界不一致 |
| B11 | P1 | 商品 | 同一产品概念存在 `Product/TProduct`、`ProductMapper/TProductMapper` 两套模型 |
| B12 | P1 | 字典删除 | 单删和批删业务约束不一致，删除字典值会直接删除业务备注 |
| B13 | P1 | 交易编号 | 交易号/发票号使用日期加随机数，无唯一性兜底 |
| B14 | P2 | 系统配置 | 系统缓存代码半禁用，注入 Redis 但清理方法为空 |
| B15 | P2 | 交易接口 | 发票相关方法重复，旧接口和新接口同时存在 |
| B16 | P2 | 商品库存 | 入库接口缺少数量、商品存在性和状态校验 |
| B17 | P2 | 删除逻辑 | 产品、活动、系统配置多处硬删除缺少引用检查 |
| B18 | P2 | 分页 | 部分 Mapper 接口声明 offset/limit，但 XML 不使用，依赖 PageHelper，接口语义误导 |
| B19 | P2 | Excel 导入 | Excel 转换器依赖启动类全局缓存，缓存未加载时返回 `-1` 静默失败 |
| B20 | P2 | 结果封装 | `R` 与 `Result` 重复，`Result` 在生产代码未使用 |
| B22 | P2 | 时间类型 | 老 CRM 模型使用 `Date`，商品/系统模型使用 `LocalDateTime`，序列化策略被迫绕开 |
| B23 | P3 | 命名/拼写 | Redis key 仍使用 `cdrm`，并存在 `production/products` 易混命名 |
| B24 | P3 | 代码格式 | 多处格式异常，影响审查和后续维护 |

## 详细问题

### B01 P0 交易批量删除绕过阶段校验

位置:

- `TranServiceImpl.deleteTransaction`: `dealer-server/src/main/java/com/autodealer/crm/service/impl/TranServiceImpl.java:445`
- `TranServiceImpl.batchDeleteTransactions`: `dealer-server/src/main/java/com/autodealer/crm/service/impl/TranServiceImpl.java:482`

单条删除先读取交易，并要求阶段为 `TranStage.QUOTATION`，否则抛出“只有待报价状态的交易才能删除”。批量删除没有读取交易主表，也没有判断阶段，直接恢复库存、删除商品关联、删除备注，最后 `deleteByIds` 删除主记录。

业务影响:

- 已审批、待收款、已完成、已拒绝等交易可以通过批量删除接口被删除。
- 库存会被恢复，造成历史成交被删除同时库存回滚。
- 单删和批删业务规则不一致，前端只要换接口就能绕过规则。

建议:

- 批量删除复用单条删除的校验逻辑，或先批量查询交易并要求全部处于可删除状态。
- 校验应在任何库存恢复/备注删除之前完成。
- 对不满足条件的 ID 返回明确错误，不允许部分成功。

### B02 P0 客户转换可创建无商品交易

位置: `dealer-server/src/main/java/com/autodealer/crm/manager/CustomerManager.java:64`

`convertCustomer` 在客户插入成功后，如果 `customerQuery.getProduct()` 不为空但 `productMapper.selectById` 返回 null，只是不加入 `products` 列表，随后仍调用 `tranService.createTransaction(tTran, products)`。

业务影响:

- 可以创建没有任何商品的交易。
- 后续 `/api/tran/settle/{id}` 会因为没有商品而无法结算。
- 线索已经被转为客户，客户已经落库，业务上形成半成品数据。

建议:

- 转换请求中如果包含产品 ID，必须校验产品存在、在售、库存足够。
- 产品不存在或数量非法时直接抛业务异常，整笔转换事务回滚。
- 若业务允许“先建空交易”，也必须显式用 DTO 字段表达，而不是由产品不存在隐式触发。

### B04 P1 字典缓存清理不完整

位置:

- 写入 `dic:values:type:{typeId}`: `DicServiceImpl.java:219`
- `clearCache(String pattern)`: `DicServiceImpl.java:239`
- 文档 `docs/backend.md` 缓存说明和实际实现也不完全一致。

`clearCache(pattern)` 接收 pattern，但实现完全忽略入参，只删除固定的 `dic:type:*`、`dic:value:*`、`dic:list:*`。实际读取路径会写入 `dic:values:type:{typeId}`，该 key 不在固定删除范围内。

业务影响:

- 字典值更新、删除后，按类型查询的缓存可能继续返回旧数据。
- 调用方传入 `dic:values:*` 或 `dic:*` 看似能清理，实际没有效果。
- 文档和代码都容易误导后续维护者。

建议:

- `clearCache(pattern)` 必须按入参删除，或移除入参并改成 `clearAllDictionaryCache`。
- 缓存 key 统一命名: `dic:type:id:{id}`、`dic:type:code:{code}`、`dic:value:id:{id}`、`dic:value:list:type:{typeId}`。
- 为每个写入 key 建立对应失效规则，并用单元测试覆盖 set/delete 对称性。

### B05 P1 Redis 抽象职责冲突

位置:

- `RedisManager`: `dealer-server/src/main/java/com/autodealer/crm/manager/RedisManager.java:12`
- `RedisService`: `dealer-server/src/main/java/com/autodealer/crm/service/RedisService.java:5`
- `RedisServiceImpl`: `dealer-server/src/main/java/com/autodealer/crm/service/impl/RedisServiceImpl.java:10`

项目同时存在 `RedisManager` 和 `RedisService` 两个 Redis 包装类。更危险的是两者都有 `getValue/setValue` 风格方法，但语义不同:

- `RedisService.getValue` 使用 `opsForValue`。
- `RedisManager.getValue` 使用 `opsForList().range`。
- `RedisManager.get/set` 又是 `opsForValue`。

业务影响:

- 登录 token 用 `RedisService` 写入，用 `RedisManager.get` 校验，当前碰巧同为 value 结构。
- 负责人列表用 `RedisManager.getValue/setValue` 写成 list 结构。
- 名字接近但数据结构不同，后续调用很容易把 string/list/hash 混用，造成 Redis 类型错误或缓存读不到。

建议:

- 保留一个 Redis facade，例如 `CacheService`。
- 方法名必须体现数据结构: `getObject/setObject`、`getList/replaceList`、`deleteByPattern`。
- 业务层不要直接依赖 `RedisTemplate`。

### B06 P1 负责人缓存会重复堆积且无过期

位置: `dealer-server/src/main/java/com/autodealer/crm/service/impl/UserServiceImpl.java:157`

`getOwnerList` 使用 `redisManager.getValue(Constants.REDIS_OWNER_KEY)` 从 list 读取，缓存未命中后用 `leftPushAll` 写入。写入前没有删除旧 list，也没有设置过期时间。

业务影响:

- 只要缓存被误判为空或手动重复写入，同一个负责人列表会被追加进同一个 Redis list。
- 用户被禁用、删除、角色变更后没有失效规则。
- 长期运行后负责人列表可能重复、过期、和数据库不一致。

建议:

- 改为 value 存储整个列表并设置 TTL，或 list 写入前先 delete 再 push 并 expire。
- 用户新增、更新、删除、角色变更时清理 `REDIS_OWNER_KEY`。

### B07 P1 交易状态机只局部存在

位置:

- 结算接口: `TranController.java:146`
- 审批: `TranServiceImpl.java:250`
- 开票: `TranServiceImpl.java:297`
- 发票状态更新: `TranServiceImpl.java:355`

交易阶段已抽成 `TranStage` 枚举，但状态机仍只局部存在。审批、开票、发票开具、重新提交等少数方法调用 `validateStageTransition`；结算直接把交易改成 `TranStage.PENDING`，没有校验当前必须为 `TranStage.QUOTATION`。`updateTransaction`、`updateTransactionStage` 也没有状态流转约束。

业务影响:

- 已完成交易可以再次被结算改成待审批。
- 任何调用 `updateTransactionStage` 的内部代码都可以跳过状态机。
- 发票状态只对 `ISSUED` 做特殊逻辑，其他任意字符串状态都能写入。

建议:

- 基于 `TranStage` 枚举建立状态流转表。
- 所有改状态入口必须走同一个 `transition(tranId, targetStage, action)`。
- Controller 不直接设置阶段，只提交动作: settle、approve、reject、invoice、issue、resubmit。

### B08 P1 交易库存逻辑重复且结果检查不完整

位置:

- 扣库存重复: `TranServiceImpl.java:75`、`TranServiceImpl.java:230`
- 恢复库存重复: `TranServiceImpl.java:212`、`TranServiceImpl.java:457`、`TranServiceImpl.java:491`
- 更新交易商品: `TranServiceImpl.java:542`

创建交易、追加商品都先插入 `TTranProduct`，再扣库存。删除商品、删除交易、批量删除交易、更新商品都各自恢复库存。恢复库存时没有检查 `productMapper.updateStock` 返回值。

业务影响:

- 产品不存在、库存行异常、并发更新失败时，恢复库存路径可能静默失败。
- 多处重复代码很难保证规则一致。
- `updateTransactionWithProducts` 会先更新交易，再删除旧商品并恢复库存；如果产品列表为空或缺失，可能把旧商品全部清掉。

建议:

- 抽出 `TranProductStockService` 或私有统一方法: validateProducts、deductStock、restoreStock、replaceProducts。
- 扣减前先校验产品存在、在售、数量大于 0、价格来源可信。
- 所有库存 SQL 更新结果必须检查。

### B09 P1 数据权限不完整且使用 SQL 字符串拼接

位置:

- `DataScopeAspect`: `dealer-server/src/main/java/com/autodealer/crm/aspect/DataScopeAspect.java:32`
- `${filterSQL}`: `TActivityMapper.xml:76`、`TUserMapper.xml:68`、`TTranMapper.xml:90`
- 线索列表: `TClueMapper.xml:104`

数据权限通过 AOP 写入 `BaseQuery.filterSQL`，Mapper 用 `${filterSQL}` 拼接 SQL。当前 userId 做了数字校验，但整体机制仍是字符串 SQL 拼接。更重要的是，`TClueMapper.selectClueByPage` 没有 `@DataScope`，SQL 也没有 filter 条件；`TTranMapper.selectByQuery` 有 `filterSQL` 条件位，但 Mapper 接口没有 `@DataScope`。

业务影响:

- 非管理员可能看到全部线索或全部交易。
- 权限过滤依赖注解是否手动加对，缺少统一保障。
- `${}` 拼接方式一旦 filterSQL 来源扩大，会变成注入风险。

建议:

- 用结构化字段传递权限条件，例如 `ownerId`/`createBy`，XML 使用 `#{}`。
- 明确每个列表接口的数据权限策略，线索、客户、交易必须补齐。
- 给普通用户访问列表的 H2 集成测试加断言: 只能看到本人数据。

### B10 P1 DTO/实体流转不一致

位置示例:

- `CustomerController` 返回 `PageInfo<TCustomer>`、`TCustomer`: `CustomerController.java:38`
- `DicController` 直接接收 `TDicType/TDicValue`: `DicController.java:41`
- `TranController` 返回 `TTran/TTranApprove/TTranInvoice/TTranProduct`，并接收 `TTranInvoice`: `TranController.java:43`
- `ProductController` 直接接收/返回 `Product`: `ProductController.java:20`
- `SystemController` 直接接收 `TSystem`: `SystemController.java:29`

项目只有一个真正 DTO 目录下的 `SystemMonitorDTO`。多数业务接口直接暴露数据库实体，少数使用 `Query`，交易创建请求放在 `model` 包下的 `TranCreateRequest`，审批/发票状态又使用 `Map`。

业务影响:

- API 字段被数据库模型牵着走，后续改表容易破坏接口。
- 入参校验分散，很多接口没有 Bean Validation。
- 实体包含关联查询字段或内部字段，容易过度暴露。

建议:

- 按模块建立 request/response DTO。
- Controller 只接收 DTO，不接收 Entity/Map。
- Service 内部使用 Command 或领域对象，Mapper 层才接触数据库实体。

### B11 P1 产品模型和 Mapper 双轨并存

位置:

- `Product`: `dealer-server/src/main/java/com/autodealer/crm/model/Product.java`
- `TProduct`: `dealer-server/src/main/java/com/autodealer/crm/model/TProduct.java`
- `ProductMapper`: `dealer-server/src/main/java/com/autodealer/crm/mapper/ProductMapper.java`
- `TProductMapper`: `dealer-server/src/main/java/com/autodealer/crm/mapper/TProductMapper.java`

同一张 `t_product` 同时有新模型 `Product` 和老模型 `TProduct`。`ProductServiceImpl.getAllOnSaleProduct` 从 `Product` 查询后手动转换成 `TProduct`，只填充部分字段。

业务影响:

- 同一产品状态在 `Product.status` 中是 `on_sale`，在 `TProduct.state` 中是 `0/1`。
- 时间类型也不同: `LocalDateTime` vs `Date`。
- 产品字段演进时需要维护两套模型，容易漏同步。

建议:

- 确定唯一产品实体或明确 `TProduct` 只是兼容 DTO。
- 如果保留兼容层，命名为 `LegacyProductView` 或 `ProductOptionDTO`，不要和数据库实体同级。
- 删除不用的 `TProductMapper` 或把所有调用迁移到统一 Mapper。

### B12 P1 字典删除会破坏业务备注，且单删/批删规则不一致

位置:

- 单删字典类型引用检查: `DicServiceImpl.java:169`
- 单删字典值直接删备注: `DicServiceImpl.java:205`
- 批删字典类型直接删备注和值: `DicServiceImpl.java:272`
- 批删字典值直接删备注: `DicServiceImpl.java:303`

单删字典类型会检查是否有业务备注引用，存在引用则拒绝删除。但批删字典类型不做同样检查，直接删除关联备注和值。单删/批删字典值也会先删除备注记录，再删除字典值。

业务影响:

- 批量接口可以绕过单条接口的业务保护。
- 删除基础字典会连带删除交易备注，业务历史被破坏。
- 审计数据不可恢复。

建议:

- 字典被业务引用时只允许禁用，不允许物理删除。
- 单删和批删共用同一套引用检查。
- 业务备注保留原始文本，不因字典删除而删除历史记录。

### B13 P1 交易号和发票号有并发碰撞风险

位置: `dealer-server/src/main/java/com/autodealer/crm/service/impl/TranServiceImpl.java:401`

`generateTranNo` 和 `generateInvoiceNo` 使用日期加 6 位随机数。没有看到数据库唯一键冲突处理或重试逻辑。

业务影响:

- 高并发或批量导入时可能生成重复交易号/发票号。
- 如果数据库没有唯一约束，会产生业务重复号；如果有唯一约束，会抛数据库异常而不是业务重试。

建议:

- 数据库加唯一索引。
- 使用序列、雪花 ID、数据库号段或带重试的唯一号生成器。

### B14 P2 系统配置缓存半禁用

位置: `dealer-server/src/main/java/com/autodealer/crm/service/impl/SystemServiceImpl.java:20`

`SystemServiceImpl` 注入了 `RedisManager`，常量类也定义了系统缓存 key，但读取方法注释说明“暂时禁用 Redis 缓存”，`clearCache` 是空方法。

业务影响:

- 代码表达和真实行为不一致。
- 后续维护者可能以为系统配置有缓存失效机制。
- 根因是 `LocalDateTime` Redis 序列化问题没有被真正解决。

建议:

- 要么彻底删除系统 Redis 缓存相关代码和常量。
- 要么把 Redis 序列化配置放入正式 `@Configuration`，解决 Java Time 模块序列化后恢复缓存。

### B15 P2 发票相关接口重复

位置:

- `TranService.createInvoice/updateInvoiceStatus/getTransactionInvoices`
- `TranService.createTranInvoice/updateTranInvoiceStatus/getTranInvoices`

服务接口中存在两套发票方法。旧方法只创建/更新发票，不驱动交易状态；新方法带状态校验和阶段流转。两套方法同时存在，容易被错误调用。

业务影响:

- 调用旧方法可以绕过开票前状态校验。
- 发票状态和交易阶段可能不一致。

建议:

- 删除或废弃旧方法，只保留带状态机的发票用例。
- 如果保留查询方法，统一命名为 `listInvoicesByTranId`。

### B16 P2 商品入库缺少校验

位置: `dealer-server/src/main/java/com/autodealer/crm/service/impl/ProductServiceImpl.java:88`

`restock` 直接把传入 quantity 加到库存并写入流水，没有校验商品是否存在、quantity 是否大于 0、商品是否允许入库。

业务影响:

- 传负数可以通过入库接口变相出库。
- 不存在的产品可能只更新 0 行，但仍写库存流水。
- 库存流水和真实库存不一致。

建议:

- 入库前查询产品并校验存在。
- quantity 必须大于 0。
- 更新库存返回 0 时抛业务异常，不写流水。

### B17 P2 多处硬删除缺少引用检查

位置示例:

- 产品删除: `ProductServiceImpl.java:62`
- 活动删除: `ActivityServiceImpl.java:89`
- 系统配置删除: `SystemServiceImpl.java:48`
- Mapper 中大量 `DELETE FROM ...`

商品、活动、系统配置等管理接口多处直接硬删除。产品已经被交易引用时，删除产品会让交易商品详情和历史报表出现断链。活动被线索引用时也有类似风险。

建议:

- 对可被业务引用的数据使用状态停用或软删除。
- 删除前统一检查引用关系。
- 历史业务表保留快照字段，例如交易商品名、成交单价。

### B18 P2 分页接口参数误导

位置:

- `ProductServiceImpl.getProductList`: `ProductServiceImpl.java:31`
- `ProductCategoryServiceImpl.getCategoryList`: `ProductCategoryServiceImpl.java:22`
- `ProductPromotionServiceImpl.getPromotionList`: `ProductPromotionServiceImpl.java:22`
- `ProductStockRecordServiceImpl.getStockRecordsByProductId`: `ProductStockRecordServiceImpl.java:20`

这些服务先调用 `PageHelper.startPage`，又向 Mapper 传 `offset/limit`。但对应 XML select 并未使用 offset/limit，实际分页完全依赖 PageHelper。

业务影响:

- Mapper 接口语义不真实，维护者可能误以为 XML 里做了物理分页。
- 未来如果 XML 补 `LIMIT`，会和 PageHelper 形成双重分页。

建议:

- 使用 PageHelper 时 Mapper 不声明 offset/limit。
- 如果决定手写分页，则移除 PageHelper。

### B19 P2 Excel 转换依赖启动类全局缓存

位置:

- `DealerCRMApplication.cacheMap`: `DealerCRMApplication.java:22`
- `AppellationConverter`: `AppellationConverter.java:36`
- `IntentionProductConverter`: `IntentionProductConverter.java:36`

Excel 转换器直接读取 `DealerCRMApplication.cacheMap`。如果缓存未预热，转换器返回 `-1`，导入流程可能把无效字典 ID 写入业务表。

业务影响:

- 导入结果依赖进程内缓存状态，不依赖数据库真实数据。
- 缓存清理后未重建时，导入会静默失败或写入非法 ID。

建议:

- 转换器依赖专门的字典查询服务，未命中时查数据库。
- 匹配不到字典值时抛出明确导入错误，而不是返回 `-1`。
- 启动类只负责启动，不持有业务缓存。

### B20 P2 结果封装类重复

位置:

- `R`: `dealer-server/src/main/java/com/autodealer/crm/result/R.java`
- `Result`: `dealer-server/src/main/java/com/autodealer/crm/result/Result.java`

生产代码使用 `R`，`Result` 没有生产调用。测试注释中仍提到 `Result.success`，说明历史命名残留还在影响认知。

建议:

- 删除未使用的 `Result`，或迁移全部接口到一个统一响应类。
- 保留兼容方法时也要有明确废弃计划。

### B22 P2 时间类型和 Redis 序列化不统一

位置:

- `Product` 使用 `LocalDateTime`: `Product.java:41`
- `TTran` 使用 `Date`: `TTran.java:62`
- `TSystem` 使用 `LocalDateTime`
- `SystemServiceImpl` 因 `LocalDateTime` 序列化问题禁用缓存: `SystemServiceImpl.java:23`

业务影响:

- API 时间格式不一致。
- Redis 序列化配置被放在 `CommandLineRunner` 中，且没有注册 JavaTimeModule，导致系统缓存被绕开。

建议:

- 统一 API 层时间格式。
- Redis ObjectMapper 正式配置 Java Time 支持。
- 数据库实体和响应 DTO 分离，避免内部时间类型直接暴露。

### B23 P3 命名残留和 key 拼写问题

位置: `dealer-server/src/main/java/com/autodealer/crm/constant/Constants.java`

Redis key 仍使用 `cdrm` 前缀，和当前 `autodealer.crm` 命名不一致。交易缓存同时有 `CACHE_KEY_TRAN_PRODUCTS` 和 `CACHE_KEY_TRAN_PRODUCTION`，后者只在清理时出现，没有看到对应读取/写入。

建议:

- 统一 Redis key 前缀，例如 `autodealer:crm:`。
- 删除没有业务落点的 `production` key，或补齐生产模块缓存读写。

### B24 P3 代码格式异常影响维护

位置示例:

- `SystemServiceImpl.java:20`: `private RedisManager redisManager;    @Override`
- `SystemServiceImpl.java:25`: `}    @Override`
- `TSystemMapper.xml:19`: 两个 result 写在一行

建议:

- 对后端启用统一 formatter。
- 将格式化作为 CI 检查，但不要和业务重构混在一个提交里。

## 推荐修复顺序

1. 交易核心一致性: 修 B01、B07、B08、B13、B15。
2. 客户转换和商品库存: 修 B02、B11、B16、B17。
3. Redis/缓存: 修 B04、B05、B06、B14、B19、B22、B23。
4. API 边界和权限: 修 B09、B10。
5. 代码清理: 修 B18、B20、B24。

## 建议补充的业务验收用例

1. 批量删除包含非 `TranStage.QUOTATION` 的交易时，整批失败，库存和备注不变化。
2. 客户转换传入不存在产品 ID 时，线索状态、客户表、交易表都不应变更。
3. 更新字典值后，`dic:values:type:{typeId}` 缓存必须失效。
4. 同一负责人列表缓存重复刷新后，Redis 中不出现重复负责人，且有 TTL。
5. 普通用户访问线索/交易列表，只能看到本人权限范围数据。
6. 入库数量为负数、产品不存在时，库存和流水都不应变化。
7. 发票状态传非法字符串时必须失败，不能落库。
8. 交易完成后再次结算必须失败。
9. 创建交易请求缺少 products 或产品数量小于等于 0 时必须失败。

## 本次实际执行的检查

- 初始化并使用 `codegraph` 辅助关系分析。
- 使用 `rg/find` 全局扫描 Controller、Service、Mapper、Redis key、DTO/Entity 暴露、状态码、硬删除、数据权限。
- 使用只读脚本统计 Controller 实体暴露、潜在未使用注入、重复命名。
- 执行 `./mvnw -q -DskipTests compile`，结果通过。
- 执行 `./mvnw -q test`，结果通过；但测试通过没有覆盖上述业务缺陷。
