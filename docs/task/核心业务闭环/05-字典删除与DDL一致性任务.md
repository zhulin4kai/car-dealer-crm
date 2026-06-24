# 字典删除与 DDL 一致性任务

## 任务目标

修复字典删除破坏业务历史、生产和测试建表规则不一致、交易删除留下孤儿历史等问题，建立唯一数据库结构来源和引用保护规则。

## 前置阅读

- docs/spec/数据字典/01-流程规则与验收规格.md
- docs/spec/交易履约/01-流程规则与验收规格.md
- docs/spec/线索管理/01-流程规则与验收规格.md
- docs/spec/客户管理/01-流程规则与验收规格.md
- docs/spec/车辆商品/01-流程规则与验收规格.md
- docs/plan/核心业务闭环/01-核心业务不变量治理方案.md
- docs/plan/数据字典/01-字典编码与启停治理方案.md
- docs/rule/06-业务一致性与事务规范.md
- docs/rule/10-文档与Spec治理规范.md

## 修改范围

允许修改：

- `dealer-server/src/main/java/com/autodealer/crm/web/DicController.java`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/DicServiceImpl.java`
- `dealer-server/src/main/java/com/autodealer/crm/mapper/DicMapper.java`
- `dealer-server/src/main/resources/mapper/DicMapper.xml`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/TranServiceImpl.java`
- `dealer-server/src/main/java/com/autodealer/crm/mapper/TTranMapper.java`
- `dealer-server/src/main/java/com/autodealer/crm/mapper/TTranHistoryMapper.java`
- `dealer-server/src/main/resources/mapper/TTranMapper.xml`
- `dealer-server/src/main/resources/mapper/TTranHistoryMapper.xml`
- `dealer-server/src/main/resources/CarDealerCRM.sql`
- `dealer-server/src/main/resources/schema-test.sql`
- 数据迁移脚本目录。
- 与字典、交易删除、DDL 一致性相关的测试文件。

禁止修改：

- 不通过删除 `t_tran_remark`、线索备注、客户备注或活动备注来删除字典。
- 不允许修改系统内置字典类型编码来改变业务含义。
- 不继续维护两份含义不同的建表脚本。
- 不让测试 schema 拥有生产 schema 没有的关键约束。

## 当前代码落点

- `DicServiceImpl.deleteDicValue()` 删除字典值前会调用 `DicMapper.deleteRemarksByDicValueId()`，需要移除这种破坏历史的行为。
- `DicServiceImpl.deleteDicTypesByIds()` 和 `deleteDicValuesByIds()` 批量路径同样需要停止删除业务备注。
- `DicMapper.xml` 中 `deleteRemarksByDicTypeId`、`deleteRemarksByDicValueId`、`deleteRemarksByDicTypeIds`、`deleteRemarksByDicValueIds` 需要从删除流程移除或废弃。
- `TTranMapper.deleteByPrimaryKey()` 和批量删除需要处理 `t_tran_history` 的引用保护或禁止删除策略。
- `dealer-server/src/main/resources/schema-test.sql` 已有部分唯一约束和外键。
- `dealer-server/src/main/resources/CarDealerCRM.sql` 与测试 schema 在核心约束上不一致。

## 执行步骤

1. 字典值引用检查：
   - 为字典值建立统一引用检查函数。
   - 检查交易备注、线索称呼、线索状态、线索来源、贷款意向、意向状态、跟进记录、客户跟进、活动跟进等所有使用字典值的字段。
   - 被引用字典值返回不可删除错误。
   - 未被引用的草稿字典值允许删除。
2. 字典类型删除保护：
   - 系统内置字典类型禁止修改 `typeCode`。
   - 字典类型下存在被引用字典值时禁止删除整个类型。
   - 字典类型删除不能级联删除业务备注。
   - 若业务需要不可见，使用停用状态。
3. 删除危险 SQL：
   - 从 `DicServiceImpl` 删除流程移除 `deleteRemarksByDicValueId()`、`deleteRemarksByDicTypeId()`、`deleteRemarksByDicValueIds()`、`deleteRemarksByDicTypeIds()` 调用。
   - Mapper 中危险 delete 方法若无调用，删除方法声明和 XML 语句；若暂时保留，标记为禁用并保证无业务调用。
4. 交易删除和历史保护：
   - 已产生审批、发票、支付、商品行、库存流水或交易历史的交易不得普通物理删除。
   - 删除未使用草稿交易时必须同时处理所有子表，包含 `t_tran_history`。
   - 更推荐将业务删除改为取消或关闭状态，保留交易历史。
   - 批量删除必须逐条检查并返回不可删除原因。
5. 确定唯一 DDL 来源：
   - 明确生产 DDL、测试 schema 和本地初始化由同一迁移源生成。
   - 不再手工维护两份不同约束含义的 SQL。
   - 在文档或脚本中说明生成流程。
6. 统一核心约束：
   - 线索手机号唯一约束。
   - 客户到线索外键。
   - 交易到客户外键。
   - 交易历史到交易外键。
   - 交易商品到交易和商品外键。
   - 发票、审批、支付到交易外键。
   - 客户意向商品到商品外键。
   - 与删除策略冲突的外键必须选择 `RESTRICT` 或业务软删除，不使用静默级联删除历史。
7. DDL 一致性检查：
   - 增加测试或脚本比较生产 DDL 与测试 schema 的核心约束清单。
   - CI 或本地验证命令能发现缺失唯一约束、缺失外键和删除策略不一致。
8. 前端字典删除提示：
   - `dealer-web/src/pages/dashboard/dict/type.vue` 和 `dict/value.vue` 删除失败时展示不可删除原因。
   - 字典停用作为推荐操作，不鼓励删除已使用字典。
9. 补充测试：
   - 被交易备注引用的字典值不能删除，备注不被删除。
   - 被线索来源引用的字典值不能删除。
   - 系统内置字典类型编码不能修改。
   - 有历史记录的交易不能普通删除。
   - 生产和测试 DDL 核心约束清单一致。

## 代码逻辑要求

- 字典删除必须先查引用再删除。
- 删除失败必须说明业务原因。
- 批量删除不能部分静默成功；若采用部分成功，响应必须返回成功和失败明细。
- DDL 迁移脚本必须可重复审查，不依赖口头约定。
- 所有外键和唯一约束变化要考虑已有脏数据迁移。

## 完成条件

- 字典删除不会删除任何业务备注或历史。
- 已被业务引用的字典值不能物理删除。
- 交易历史不会因交易删除产生孤儿记录。
- 生产、测试和本地数据库核心约束一致。
- DDL 一致性有自动验证。

## 验证命令

- `cd dealer-server && ./mvnw -Dtest=DicServiceImplTest test`
- `cd dealer-server && ./mvnw -Dtest=TranServiceImplTest test`
- `cd dealer-server && ./mvnw -Dtest=DatabaseSchemaConsistencyTest test`
- `cd dealer-server && ./mvnw -Dtest=DicControllerH2IntegrationTest test`
- `cd dealer-server && ./mvnw -DskipTests compile`
- `cd dealer-web && npm run typecheck`

## 交付说明

完成后说明：

- 字典值引用检查覆盖哪些表和字段。
- 字典类型编码保护规则。
- 交易删除或关闭策略。
- 生产和测试 DDL 的唯一来源。
- DDL 一致性验证方式。

## 业务场景

- 跨模块修复权限、商品促销契约、交易资金库存、历史保护、DDL 和统计口径。
- 执行时先确认该任务在业务闭环中的上游对象、下游对象和责任边界。

## 状态事件要求

- 核心任务负责共享不变量和跨模块状态事件，不替代模块内部状态实现。
- 状态推进必须由真实业务事件触发，并保留必要原因、操作者和时间。

## 与其他任务的边界

- 跨模块共享不变量以 `docs/task/核心业务闭环/` 为准。
- 模块内部页面、接口、模型、测试和局部校验由本任务负责。
- 涉及共享文件时遵守 `docs/task/核心业务闭环/00-任务拆分总览.md` 的文件所有权。

## 不得破坏的业务事实

- 不得和模块任务抢同一文件；共享文件以任务拆分总览 owner 为准。
- 不得为了让测试通过而降低业务断言或删除历史事实。

## 场景验收

- 完成后能验证端到端场景从线索到经营分析可追踪。
- 验收必须能映射到 `docs/task/文档链路治理/验收矩阵.md` 中至少一个端到端场景。
