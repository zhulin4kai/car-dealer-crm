# 补齐活动归因复盘 ROI 任务

## 任务目标

按市场活动 Spec 和活动归因方案，补齐活动状态、来源归因、复盘锁定、转化统计、ROI 口径和活动客户明细权限。

## 前置阅读

- docs/spec/市场活动/00-业务范围与边界.md
- docs/spec/市场活动/01-流程规则与验收规格.md
- docs/plan/市场活动/01-活动归因与ROI落地方案.md
- docs/rule/02-Java后端代码风格.md
- docs/rule/03-Vue-TypeScript前端代码风格.md
- docs/rule/06-业务一致性与事务规范.md
- docs/rule/07-测试编写执行与验收规范.md

## 修改范围

允许修改：

- `dealer-server/src/main/java/com/autodealer/crm/web/ActivityController.java`
- `dealer-server/src/main/java/com/autodealer/crm/web/ActivityRemarkController.java`
- `dealer-server/src/main/java/com/autodealer/crm/service/ActivityService.java`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/ActivityServiceImpl.java`
- `dealer-server/src/main/java/com/autodealer/crm/model/TActivity.java`
- `dealer-server/src/main/java/com/autodealer/crm/mapper/TActivityMapper.java`
- `dealer-server/src/main/resources/mapper/TActivityMapper.xml`
- `dealer-web/src/modules/activity/`
- `dealer-web/src/pages/dashboard/activity/`
- 活动相关测试和文档。

禁止修改：

- 正式财务成本系统口径。
- 线索、客户、商机状态语义。
- 为统计方便反向修改业务事实。
- 让普通销售任意修改活动来源。

## 当前代码落点

- `ActivityController.activityPage`、`addActivity`、`batchDeleteActivities` 是当前活动主要入口。
- `ActivityServiceImpl.batchDeleteActivities` 等方法承载当前活动业务。
- `ActivityRemarkController` 和 `ActivityRemarkServiceImpl` 承载活动备注。
- 前端活动 API、类型和列表逻辑在 `dealer-web/src/modules/activity/`。

## 执行步骤

1. 定义活动状态 code：草稿、计划中、进行中、已结束、已复盘、已取消。
2. 调整活动创建、编辑、结束、复盘和取消命令。
3. 草稿活动禁止作为线索归因来源。
4. 已复盘活动锁定核心成本、时间、负责人和核心结果字段。
5. 补齐活动线索、有效客户、商机、试驾、报价、订单和成交金额统计查询。
6. 活动客户明细和导出加独立权限、数据范围、数量限制和审计。
7. 调整前端活动列表、详情、复盘表单和导出入口。
8. 补充活动状态、来源继承、复盘锁定、导出权限和统计测试。

## 代码逻辑要求

- 活动关闭不自动关闭其产生的线索或商机。
- 活动成本只用于经营分析，不写入财务账。
- ROI 查询只读业务事实，不修正业务状态。
- 活动来源修改必须校验权限并写审计。
- 活动删除前检查关联线索、客户、商机和订单。

## 完成条件

- 新线索可以归因到有效活动。
- 草稿和已取消活动不能被新线索引用。
- 活动产生的线索转客户后，客户仍保留活动来源。
- 已复盘活动普通人员不能修改核心字段。
- 管理者能查看活动转化漏斗和 ROI 摘要。
- 无权限用户不能导出活动客户明细。

## 验证命令

- `cd dealer-server && ./mvnw -Dtest=ActivityServiceImplTest,ActivityRemarkServiceImplTest test`
- `cd dealer-server && ./mvnw -Dtest=StatisticManagerTest,StatisticServiceImplTest test`
- `cd dealer-web && npm run typecheck`
- `cd dealer-web && npm run test -- tests/unit/modules/api-endpoints.test.ts`
- `cd dealer-server && ./mvnw -DskipTests compile`

## 交付说明

完成后说明：

- 活动状态和允许动作。
- ROI 指标来源和限制。
- 复盘锁定字段。
- 活动来源防篡改策略。
- 已执行测试和未覆盖风险。

## 业务场景

- 管理获客活动、来源归因、成本、复盘和线索到成交效果。
- 执行时先确认该任务在业务闭环中的上游对象、下游对象和责任边界。

## 状态事件要求

- 活动状态表达活动生命周期，不代表线索、客户、商机或订单状态。
- 状态推进必须由真实业务事件触发，并保留必要原因、操作者和时间。

## 与其他任务的边界

- 跨模块共享不变量以 `docs/task/核心业务闭环/` 为准。
- 模块内部页面、接口、模型、测试和局部校验由本任务负责。
- 涉及共享文件时遵守 `docs/task/核心业务闭环/00-任务拆分总览.md` 的文件所有权。

## 不得破坏的业务事实

- 不得通过删除活动清理已产生的线索、客户、商机、订单或备注。
- 不得为了让测试通过而降低业务断言或删除历史事实。

## 场景验收

- 完成后能验证活动归因延伸到线索、客户、商机和成交复盘。
- 验收必须能映射到 `docs/task/文档链路治理/验收矩阵.md` 中至少一个端到端场景。
