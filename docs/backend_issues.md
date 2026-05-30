# Car Dealer CRM 后端问题清单

> 审计时间：2026-05-30
> 审计范围：dealer-server 后端全部源代码
> 基于：docs/issues.md、docs/backend.md、docs/api.md 及源码逐文件审计

---

## 问题汇总

| 统计项 | 数量 |
|-------|------|
| P0-致命问题 | 3 |
| P1-严重问题 | 11 |
| P2-一般问题 | 16 |
| P3-轻微问题 | 4 |
| **后端问题总数** | **34** |

---

## 一、业务流程完整性

### ISSUE-001：线索转客户时未校验线索状态的并发问题 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-001 |
| 所属模块 | 线索管理/客户管理 |
| 问题类型 | 流程缺失 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`manager/CustomerManager.java:41-57` |

**问题详情**：
`CustomerManager.convertCustomer()` 方法中，先查询线索状态 `tClue.getState() == -1` 判断是否已转客户，再将线索状态更新为 `-1`。这两个操作之间没有原子性保证，高并发场景下两个请求可能同时通过状态检查，导致同一线索被转换两次，创建重复的客户和交易记录。

**现状**：
当前代码在 `CustomerManager.java:41-43` 先查询状态，`CustomerManager.java:54-57` 再更新状态，查询和更新之间存在时间窗口。虽然方法有 `@Transactional` 注解，但默认隔离级别（MySQL READ_COMMITTED）下，两次 SELECT 读到的是同一份已提交数据，无法防止并发。

```java
// 当前代码（CustomerManager.java:41-43）
TClue tClue = tClueMapper.selectByPrimaryKey(customerQuery.getClueId());
if (tClue.getState() == -1) {
    throw new RuntimeException("该线索已经转过客户，不能再转了.");
}
// ... 中间有插入客户操作 ...
// 当前代码（CustomerManager.java:54-57）
TClue clue = new TClue();
clue.setId(customerQuery.getClueId());
clue.setState(-1);
int update = tClueMapper.updateByPrimaryKeySelective(clue);
```

**修改建议**：
1. 使用乐观锁方式：将线索状态更新改为 `UPDATE t_clue SET state = -1 WHERE id = #{id} AND state != -1`，检查影响行数，若为 0 则说明已被转换。
2. 修改 `TClueMapper.xml`，新增 `updateStateToConverted` SQL：
```sql
UPDATE t_clue SET state = -1, edit_time = NOW(), edit_by = #{editBy}
WHERE id = #{id} AND state != -1
```
3. 在 `CustomerManager.convertCustomer()` 中用该 SQL 替换 `selectByPrimaryKey` + `updateByPrimaryKeySelective` 的两步操作，根据返回的影响行数判断是否转换成功。
4. 在 `TClueMapper.java` 中新增方法 `int updateStateToConverted(@Param("id") Integer id, @Param("editBy") Integer editBy)`。

**验收标准**：
- [ ] 使用 UPDATE ... WHERE 条件方式实现原子性状态检查
- [ ] 并发请求时只有一个能成功转换
- [ ] 失败的请求返回明确的错误提示
- [ ] 添加并发测试用例验证

**代码示例**：
```java
// CustomerManager.java - 修改后的 convertCustomer 方法
@Transactional(rollbackFor = Exception.class)
public Boolean convertCustomer(CustomerQuery customerQuery) {
    // 原子性更新线索状态，防止并发重复转换
    int updateCount = tClueMapper.updateStateToConverted(
        customerQuery.getClueId(), customerQuery.getCreateBy());
    if (updateCount == 0) {
        throw new RuntimeException("该线索已经转过客户，不能再转了.");
    }
    // ... 后续插入客户和创建交易逻辑 ...
}
```

---

### ISSUE-002：交易状态流转缺少非法状态校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-002 |
| 所属模块 | 交易管理 |
| 问题类型 | 流程缺失 |
| 严重程度 | P0-致命 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:110-123, 243-279, 288-316`，`web/TranController.java:151-200` |

**问题详情**：
交易状态流转应严格遵循 `待报价(41) → 待审批(42) → 已审批(43) → 待收款(45) → 已完成(46)`，审批拒绝跳到 `丢失关闭(21)`。但 `settle()`、`approveTran()`、`createTranInvoice()` 等方法都没有校验当前状态是否允许跳转，可直接从任意状态跳到目标状态。

**现状**：
- `TranController.settle()` (行151-200)：直接设置 `tran.setStage(42)`，不检查当前状态是否为 41
- `TranServiceImpl.approveTran()` (行243-279)：直接设置 43 或 21，不检查当前状态是否为 42
- `TranServiceImpl.createTranInvoice()` (行288-316)：直接设置 45，不检查当前状态是否为 43
- `TranServiceImpl.updateTranInvoiceStatus()` (行324-362)：直接设置 46，不检查当前状态是否为 45

**修改建议**：
1. 在每个状态变更方法中，先查询当前交易状态
2. 定义合法状态映射：
   - settle: 当前状态必须为 41
   - approve: 当前状态必须为 42
   - createInvoice: 当前状态必须为 43
   - updateInvoiceStatus(ISSUED): 当前状态必须为 45
3. 在 `TranServiceImpl` 中抽取 `validateStageTransition(Integer tranId, Integer requiredStage)` 公共方法
4. 修改涉及文件：`TranServiceImpl.java`、`TranController.java`

**验收标准**：
- [ ] settle 方法校验当前状态为 41
- [ ] approve 方法校验当前状态为 42
- [ ] createInvoice 方法校验当前状态为 43
- [ ] updateInvoiceStatus 方法校验当前状态为 45
- [ ] 非法状态跳转返回明确错误信息
- [ ] 添加单元测试覆盖所有非法跳转场景

**代码示例**：
```java
// TranServiceImpl.java - 新增状态校验方法
private void validateStageTransition(Integer tranId, Integer requiredStage) {
    TTran tran = tranMapper.selectByPrimaryKey(tranId);
    if (tran == null) {
        throw new RuntimeException("交易记录不存在");
    }
    if (!requiredStage.equals(tran.getStage())) {
        throw new RuntimeException("当前交易状态不允许执行此操作，需要状态: " + requiredStage);
    }
}
```

---

### ISSUE-003：交易删除未校验状态限制 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-003 |
| 所属模块 | 交易管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:396-427`，`web/TranController.java:295-302` |

**问题详情**：
前端限制只有待报价(41)状态的交易可删除，但后端 `deleteTransaction()` 方法没有状态校验。任何状态的交易（包括已审批、已完成）都可被删除，导致数据丢失和审计链断裂。

**现状**：
`TranServiceImpl.deleteTransaction()` 在行396-427只检查交易是否存在，不检查状态：
```java
TTran transaction = tranMapper.selectByPrimaryKey(id);
if (transaction == null) {
    return false;
}
// 直接删除，未校验状态
```

**修改建议**：
1. 在 `deleteTransaction()` 方法开头添加状态校验，只允许待报价(41)状态的交易删除
2. 批量删除 `batchDeleteTransactions()` 也需同样校验
3. 返回明确的错误信息说明为何不能删除

**验收标准**：
- [ ] 只有状态为 41 的交易可被删除
- [ ] 非 41 状态删除返回错误提示
- [ ] 批量删除时跳过非 41 状态或整体失败
- [ ] 单元测试覆盖各状态下的删除尝试

**代码示例**：
```java
// TranServiceImpl.java - deleteTransaction 方法
@Transactional(rollbackFor = Exception.class)
public boolean deleteTransaction(Integer id) {
    TTran transaction = tranMapper.selectByPrimaryKey(id);
    if (transaction == null) {
        return false;
    }
    // 校验只有待报价状态允许删除
    if (transaction.getStage() != 41) {
        throw new RuntimeException("只有待报价状态的交易才能删除");
    }
    // ... 后续删除逻辑 ...
}
```

---

### ISSUE-004：客户删除未级联处理交易记录 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-004 |
| 所属模块 | 客户管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`service/impl/CustomerServiceImpl.java`（缺少删除方法），`mapper/TCustomerMapper.xml` |

**问题详情**：
`CustomerServiceImpl` 没有实现客户删除方法。如果直接通过数据库删除客户，关联的交易记录（`t_tran.customer_id`）将引用不存在的客户，导致查询时数据异常或报错。

**现状**：
`CustomerServiceImpl.java` 中没有 `deleteCustomer()` 方法。`CustomerController` 也没有删除客户接口。数据库中 `t_tran` 表的 `customer_id` 外键引用 `t_customer.id`，无级联处理。

**修改建议**：
1. 在 `CustomerServiceImpl` 中实现删除方法，删除前检查是否有未完成交易
2. 可选方案：
   - 方案A：禁止删除有未完成交易的客户
   - 方案B：软删除（添加 `deleted` 字段标记）
   - 方案C：级联删除所有关联数据（交易、发票、审批等）
3. 在 `CustomerController` 添加删除接口 `DELETE /api/customer/{id}`
4. 建议采用方案A，保护业务数据完整性

**验收标准**：
- [ ] 有未完成交易的客户不可删除
- [ ] 删除客户时返回关联数据提示
- [ ] 单元测试覆盖各场景

---

### ISSUE-005：线索删除未级联处理线索备注 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-005 |
| 所属模块 | 线索管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/ClueServiceImpl.java:106-113` |

**问题详情**：
删除线索时只删除了 `t_clue` 表记录，没有删除 `t_clue_remark` 表的关联备注记录。导致线索删除后，备注记录成为孤儿数据，占用数据库空间且无法清理。

**现状**：
```java
// ClueServiceImpl.java:106-113
@Transactional(rollbackFor = Exception.class)
public int delClueById(Integer id) {
    if (id == null) {
        return 0;
    }
    return tClueMapper.deleteByPrimaryKey(id);  // 只删除了线索，未处理备注
}
```

**修改建议**：
1. 在 `ClueServiceImpl.delClueById()` 中，删除线索前先删除关联备注
2. 注入 `TClueRemarkMapper`，在删除线索前调用 `deleteByClueId(id)`
3. `batchDelClueByIds()` 同样需要处理
4. 或在数据库层面设置外键级联删除

**验收标准**：
- [ ] 删除线索时同时删除关联备注
- [ ] 批量删除时同样处理
- [ ] 使用事务保证原子性

---

### ISSUE-018：审批拒绝后无法重新提交 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-018 |
| 所属模块 | 交易管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:264-266` |

**问题详情**：
审批拒绝后交易状态变为 `丢失关闭(21)`，系统没有提供将状态从 21 回退到 41（待报价）的接口，导致被拒绝的交易永远无法重新进入审批流程。

**现状**：
`TranServiceImpl.approveTran()` 行264-266 设置拒绝状态为 21：
```java
if (!approved) {
    tran.setStage(21); // 丢失关闭
}
```
没有提供将状态 21 改回 41 的方法。

**修改建议**：
1. 新增 `resubmitTransaction(Integer tranId, Integer userId)` 方法
2. 校验当前状态必须为 21（丢失关闭）
3. 将状态改回 41（待报价）
4. 在 `TranController` 新增接口 `PUT /api/tran/resubmit/{id}`
5. 记录重新提交的操作日志

**验收标准**：
- [ ] 审批拒绝后可重新提交
- [ ] 只有状态为 21 的交易可重新提交
- [ ] 重新提交后状态变为 41
- [ ] 记录操作日志

---

### ISSUE-019：客户可被多个销售同时跟进

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-019 |
| 所属模块 | 客户管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/CustomerServiceImpl.java`，`model/TCustomer.java` |

**问题详情**：
`TCustomer` 模型和 `t_customer` 表没有负责人（owner_id）字段，无法实现客户归属管理。多个销售人员可以同时查看和操作同一客户，导致销售冲突。

**现状**：
`CustomerServiceImpl` 中查询客户列表时没有按负责人过滤，所有用户看到的客户数据完全相同。

**修改建议**：
1. `t_customer` 表新增 `owner_id` 字段
2. `TCustomer` 模型新增 `ownerId` 属性
3. 线索转客户时，将线索的 `owner_id` 复制到客户
4. 客户列表查询按 `owner_id` 过滤
5. 支持客户分配和转移功能

**验收标准**：
- [ ] 客户表有 owner_id 字段
- [ ] 客户列表按负责人过滤
- [ ] 支持客户分配功能

---

### ISSUE-020：线索转客户时默认数量为1 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-020 |
| 所属模块 | 客户管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 后端：`manager/CustomerManager.java:78` |

**问题详情**：
`CustomerManager.convertCustomer()` 创建交易产品时，数量硬编码为1。用户在线索转客户时无法选择购买数量。

**现状**：
```java
// CustomerManager.java:78
tranProduct.setQuantity(1); // 默认数量为1
```

**修改建议**：
1. 在 `CustomerQuery` 中新增 `quantity` 字段
2. 使用 `customerQuery.getQuantity()` 替代硬编码的 `1`
3. 前端转换表单增加数量输入框

**验收标准**：
- [ ] 用户可自定义购买数量
- [ ] 默认值为1（向后兼容）
- [ ] 数量校验大于0

---

## 二、数据一致性

### ISSUE-006：库存扣减无下限校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-006 |
| 所属模块 | 商品管理/交易管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P0-致命 |
| 涉及文件 | 后端：`resources/mapper/TProductMapper.xml:102-109`，`service/impl/TranServiceImpl.java:83` |

**问题详情**：
`TProductMapper.xml` 中的 `updateStock` SQL 使用 `stock = stock + #{quantity}`，虽然当 `quantity < 0` 时会检查 `stock >= ABS(#{quantity})`，但 `TranServiceImpl.createTransaction()` 调用时没有检查返回的影响行数，导致即使库存不足也不会抛出异常。

**现状**：
```xml
<!-- TProductMapper.xml:102-109 -->
<update id="updateStock">
    UPDATE t_product
    SET stock = stock + #{quantity}
    WHERE id = #{id}
    <if test="quantity &lt; 0">
        AND stock <![CDATA[ >= ]]> ABS(#{quantity})
    </if>
</update>
```
```java
// TranServiceImpl.java:83 - 未检查返回值
productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
```

**修改建议**：
1. 在 `TranServiceImpl.createTransaction()` 中检查 `productMapper.updateStock()` 的返回值
2. 如果返回值为 0，说明库存不足，抛出异常回滚事务
3. 同样修改 `addTransactionProducts()` 方法
4. 建议在 Service 层添加库存预检查方法

**验收标准**：
- [ ] 库存不足时抛出明确异常
- [ ] 事务正确回滚
- [ ] 并发扣减不会出现超卖
- [ ] 单元测试覆盖库存不足场景

**代码示例**：
```java
// TranServiceImpl.java - createTransaction 中库存扣减
int updateCount = productMapper.updateStock(product.getProductId().longValue(), -product.getQuantity());
if (updateCount == 0) {
    throw new RuntimeException("产品 [" + product.getProductId() + "] 库存不足，无法完成交易");
}
```

---

### ISSUE-007：交易更新时库存恢复和扣减非原子操作

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-007 |
| 所属模块 | 交易管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`web/TranController.java:93-145`，`service/impl/TranServiceImpl.java:93-107` |

**问题详情**：
`TranController.update()` 方法中，先调用 `tranService.updateTransaction(tran)` 更新交易基本信息，再调用 `tranService.deleteTransactionProducts()` 恢复旧库存，最后调用 `tranService.addTransactionProducts()` 扣减新库存。这三个操作不在同一个事务中（Controller 层没有 `@Transactional`），如果中间步骤失败，会导致库存数据不一致。

**现状**：
```java
// TranController.java:122-141 - 三个独立操作，没有事务保护
boolean result = tranService.updateTransaction(tran);
if (result && request.getProducts() != null && !request.getProducts().isEmpty()) {
    tranService.deleteTransactionProducts(request.getId());  // 独立事务
    tranService.addTransactionProducts(request.getId(), products);  // 独立事务
}
```
虽然 `deleteTransactionProducts()` 和 `addTransactionProducts()` 各自有 `@Transactional`，但它们是三个独立事务。

**修改建议**：
1. 在 `TranServiceImpl` 中新增 `updateTransactionWithProducts(TTran tran, List<TTranProduct> products)` 方法
2. 该方法使用单一 `@Transactional` 包裹所有操作
3. 修改 `TranController.update()` 调用新方法
4. 移除 Controller 中的分步调用逻辑

**验收标准**：
- [ ] 更新交易和产品关联在同一个事务中
- [ ] 中间步骤失败时全部回滚
- [ ] 库存数据始终一致

**代码示例**：
```java
// TranServiceImpl.java - 新增统一方法
@Transactional(rollbackFor = Exception.class)
public boolean updateTransactionWithProducts(TTran tran, List<TTranProduct> products) {
    // 1. 更新交易基本信息
    tran.setEditTime(new Date());
    int rows = tranMapper.updateByPrimaryKeySelective(tran);
    if (rows == 0) return false;
    
    // 2. 恢复旧产品库存
    deleteTransactionProducts(tran.getId());
    
    // 3. 扣减新产品库存
    if (products != null && !products.isEmpty()) {
        addTransactionProducts(tran.getId(), products);
    }
    
    clearTransactionCache(tran.getId());
    return true;
}
```

---

### ISSUE-008：交易编号和发票号码存在重复风险 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-008 |
| 所属模块 | 交易管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:368-382` |

**问题详情**：
交易编号和发票号码使用 `日期 + 6位随机数` 生成，没有查重机制。6位随机数只有100万种可能，随着业务量增加，碰撞概率逐渐增大。

**现状**：
```java
// TranServiceImpl.java:368-382
private String generateTranNo() {
    String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
    String randomStr = String.format("%06d", new java.util.Random().nextInt(1000000));
    return "TN" + dateStr + randomStr;
}
private String generateInvoiceNo() {
    String dateStr = new java.text.SimpleDateFormat("yyyyMMdd").format(new Date());
    String randomStr = String.format("%06d", new java.util.Random().nextInt(1000000));
    return "INV" + dateStr + randomStr;
}
```

**修改建议**：
1. 方案A：使用数据库自增序列生成唯一编号
2. 方案B：使用 Snowflake 算法生成分布式唯一ID
3. 方案C：生成后查重，如果重复则重新生成
4. 建议采用方案C，实现简单且兼容现有数据结构
5. 在 `t_tran` 表的 `tran_no` 和 `t_tran_invoice` 表的 `invoice_no` 上添加唯一索引

**验收标准**：
- [ ] 编号生成保证唯一性
- [ ] 数据库有唯一索引约束
- [ ] 高并发场景下不出现重复

---

### ISSUE-009：删除用户未处理关联数据

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-009 |
| 所属模块 | 用户管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`service/impl/UserServiceImpl.java:145-149` |

**问题详情**：
删除用户时，该用户创建的线索、活动、交易等数据的 `create_by`、`owner_id` 外键引用会失效。直接删除用户会导致关联数据查询时报错或显示异常。

**现状**：
```java
// UserServiceImpl.java:145-149
@Transactional(rollbackFor = Exception.class)
public int delUserById(Integer id) {
    return tUserMapper.deleteByPrimaryKey(id);  // 直接物理删除，未处理关联数据
}
```

**修改建议**：
1. 实现软删除：`t_user` 表添加 `deleted` 字段（默认0，删除时设为1）
2. 所有用户查询添加 `deleted = 0` 条件
3. 或删除前检查关联数据：查询该用户的线索、活动、交易数量，有数据则拒绝删除
4. 修改涉及：`TUserMapper.xml`、`UserServiceImpl.java`、`TUser.java`

**验收标准**：
- [ ] 删除用户不影响关联数据的完整性
- [ ] 有关联数据时给出明确提示
- [ ] 使用软删除时查询正确过滤

---

### ISSUE-010：删除字典类型时级联删除范围过广 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-010 |
| 所属模块 | 字典管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/DicServiceImpl.java:156-187`，`resources/mapper/DicMapper.xml` |

**问题详情**：
删除字典类型时，会级联删除关联的交易备注记录（`t_tran_remark.note_way`）。用户删除一个字典类型（如"跟踪方式"），可能导致所有交易的跟踪记录被删除，造成业务数据意外丢失。

**现状**：
```java
// DicServiceImpl.java:168-171
// 3. 先删除关联的备注记录 (t_tran_remark中的note_way引用t_dic_value的id)
if (dicValueIds != null && !dicValueIds.isEmpty()) {
    dicMapper.deleteRemarksByDicValueIds(dicValueIds);
}
```

**修改建议**：
1. 删除字典类型前检查是否有业务数据引用
2. 如果有引用，返回错误提示，不允许删除
3. 修改为：先查询 `t_tran_remark` 中是否有引用这些字典值的记录
4. 有引用时抛出异常："该字典类型下有业务数据引用，无法删除"

**验收标准**：
- [ ] 有业务引用的字典类型不可删除
- [ ] 返回明确的错误提示
- [ ] 不会意外删除业务数据

---

### ISSUE-042：Token 刷新存在竞态条件 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-042 |
| 所属模块 | 认证模块 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`config/filter/TokenVerifyFilter.java:122-130` |

**问题详情**：
`TokenVerifyFilter` 使用线程池异步刷新 token 过期时间。如果多个请求同时到达，可能同时触发多次 Redis `expire` 操作。虽然 Redis 的 `expire` 是幂等的不会造成功能问题，但频繁操作会增加 Redis 负担。

**现状**：
```java
// TokenVerifyFilter.java:122-130
threadPoolTaskExecutor.execute(() -> {
    String rememberMe = request.getHeader("rememberMe");
    if (Boolean.parseBoolean(rememberMe)) {
        redisService.expire(Constants.REDIS_JWT_KEY + tUser.getId(), Constants.EXPIRE_TIME, TimeUnit.SECONDS);
    } else {
        redisService.expire(Constants.REDIS_JWT_KEY + tUser.getId(), Constants.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
    }
});
```

**修改建议**：
1. 记录最后刷新时间，避免频繁刷新（如 5 分钟内不重复刷新）
2. 可在 Redis 中记录一个额外的 key `cdrm:user:token:refresh:{userId}`，设置 5 分钟过期
3. 刷新前先检查该 key 是否存在，存在则跳过
4. 或在内存中使用 `ConcurrentHashMap` 记录最后刷新时间

**验收标准**：
- [ ] 不频繁触发 Redis expire 操作
- [ ] Token 仍然能正常刷新
- [ ] 不影响认证功能

---

### ISSUE-043：字典缓存刷新机制不完善 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-043 |
| 所属模块 | 字典管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`DlykServerApplication.java`，`service/impl/DicServiceImpl.java` |

**问题详情**：
系统存在两套字典缓存：Redis 缓存和 `DlykServerApplication.cacheMap` 内存缓存。字典数据修改后，虽然清除了 Redis 缓存，但 Excel 导入使用的 `cacheMap` 内存缓存不会自动刷新。导致修改字典后，Excel 导入仍使用旧的内存缓存数据。

**现状**：
字典变更时 `DicServiceImpl.clearCache()` 只清除 Redis 缓存，不影响 `DlykServerApplication.cacheMap`。

**修改建议**：
1. 字典变更时同时更新 `DlykServerApplication.cacheMap`
2. 或移除 `cacheMap` 内存缓存，统一使用 Redis 缓存
3. Excel 导入的 Converter 改为从 Redis 读取字典数据
4. 建议采用方案2，减少维护成本

**验收标准**：
- [ ] 字典变更后所有缓存同步更新
- [ ] Excel 导入使用最新字典数据
- [ ] 不出现缓存不一致的情况

---

## 三、输入校验

### ISSUE-012：后端产品接口缺少 @Valid 参数校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-012 |
| 所属模块 | 商品管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/ProductController.java:30-34`，`model/Product.java` |

**问题详情**：
`ProductController` 的新增和更新接口参数没有使用 `@Valid` 注解，后端未对产品数据进行校验。SKU、产品名称、价格等必填字段可提交空值或非法格式。

**现状**：
```java
// ProductController.java:30-34
@PostMapping
public Result<Void> addProduct(@RequestBody Product product) {
    productService.addProduct(product);  // 未校验参数
    return Result.success();
}
```

**修改建议**：
1. 在 `Product` 模型类中添加 JSR-303 校验注解：
   - `@NotBlank` on sku, name
   - `@NotNull` on price
   - `@DecimalMin("0")` on price, stock
2. Controller 方法参数添加 `@Valid` 注解
3. 同样修改 `updateProduct()` 方法

**验收标准**：
- [ ] Product 模型有校验注解
- [ ] Controller 参数有 @Valid 注解
- [ ] 校验失败返回明确错误信息

**代码示例**：
```java
// Product.java - 添加校验注解
public class Product {
    @NotBlank(message = "SKU不能为空")
    private String sku;
    
    @NotBlank(message = "产品名称不能为空")
    private String name;
    
    @NotNull(message = "价格不能为空")
    @DecimalMin(value = "0", message = "价格不能为负数")
    private BigDecimal price;
}

// ProductController.java
@PostMapping
public Result<Void> addProduct(@Valid @RequestBody Product product) {
    productService.addProduct(product);
    return Result.success();
}
```

---

### ISSUE-015：线索编辑时手机号不可编辑但后端未校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-015 |
| 所属模块 | 线索管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/ClueServiceImpl.java:88-104` |

**问题详情**：
前端编辑线索时手机字段不可编辑（disabled），但后端 `updateClue()` 方法直接使用 `BeanUtils.copyProperties` 复制所有属性，没有校验手机号是否被修改。通过 API 直接调用可以修改手机号，绕过前端限制。

**现状**：
```java
// ClueServiceImpl.java:88-104
@Transactional(rollbackFor = Exception.class)
public int updateClue(ClueQuery clueQuery) {
    TClue tClue = new TClue();
    BeanUtils.copyProperties(clueQuery, tClue);  // 未排除 phone 字段
    // ...
    return tClueMapper.updateByPrimaryKeySelective(tClue);
}
```

**修改建议**：
1. 在 `updateClue()` 方法中，更新前先查询原记录的手机号
2. 如果传入的手机号与原记录不同，忽略手机号字段或抛出异常
3. 或在更新前手动设置 `tClue.setPhone(null)`，让 MyBatis 的 `updateByPrimaryKeySelective` 跳过该字段

**验收标准**：
- [ ] 编辑线索时手机号不可被修改
- [ ] API 直接调用也不能修改手机号
- [ ] 尝试修改手机号时返回明确提示

---

### ISSUE-016：发票金额未与交易金额校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-016 |
| 所属模块 | 交易管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:288-316`，`web/TranController.java:236-247` |

**问题详情**：
`TranController.createInvoice()` 接口接收发票金额参数，但 `TranServiceImpl.createTranInvoice()` 没有校验发票金额是否等于交易结算金额。发票金额可以填写任意值，与交易金额不一致。

**现状**：
```java
// TranController.java:236-247
@PostMapping("/invoice")
public R<Boolean> createInvoice(@RequestBody TTranInvoice invoice) {
    invoice.setCreateBy(currentUser.getId());
    invoice.setUpdateBy(currentUser.getId());
    boolean result = tranService.createTranInvoice(invoice);  // 未校验发票金额
    return R.OK(result);
}
```

**修改建议**：
1. 在 `createTranInvoice()` 中，根据 `invoice.getTranId()` 查询交易金额
2. 校验发票金额是否等于交易金额（或允许的误差范围内）
3. 不一致时抛出异常或返回错误提示

**验收标准**：
- [ ] 发票金额必须等于交易结算金额
- [ ] 金额不一致时返回明确错误提示
- [ ] 单元测试覆盖金额校验

---

### ISSUE-017：同一交易可开具多张发票 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-017 |
| 所属模块 | 交易管理 |
| 问题类型 | 业务规则 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:288-316` |

**问题详情**：
`createTranInvoice()` 方法没有检查该交易是否已有发票。允许对同一交易多次开具发票，且每次开票都会将状态改为 45（待收款），导致数据混乱。

**现状**：
```java
// TranServiceImpl.java:288-316
@Transactional(rollbackFor = Exception.class)
public boolean createTranInvoice(TTranInvoice invoice) {
    // 未检查是否已有发票
    invoice.setInvoiceNo(generateInvoiceNo());
    // ...
    tranInvoiceMapper.insertSelective(invoice);
    // ...
}
```

**修改建议**：
1. 在 `createTranInvoice()` 开头查询该交易是否已有发票
2. 查询：`SELECT COUNT(*) FROM t_tran_invoice WHERE tran_id = #{tranId}`
3. 如果已有发票，抛出异常："该交易已开具发票，不可重复开票"
4. 或改为支持多张发票的业务规则（但需明确多次开票逻辑）

**验收标准**：
- [ ] 同一交易只能开具一张发票
- [ ] 重复开票时返回明确错误提示
- [ ] 单元测试覆盖重复开票场景

---

## 四、异常处理

### ISSUE-021：全局异常处理返回内部错误信息 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-021 |
| 所属模块 | 系统框架 |
| 问题类型 | 安全隐患 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`config/handler/GlobalExceptionHandler.java:26-30` |

**问题详情**：
`GlobalExceptionHandler` 的通用异常处理直接返回 `e.getMessage()`，可能暴露数据库表名、SQL 语句、字段名等敏感信息。例如 `Duplicate entry 'xxx' for key 'phone'` 会暴露表结构。

**现状**：
```java
// GlobalExceptionHandler.java:26-30
@ExceptionHandler(value = Exception.class)
public R handException(Exception e) {
    e.printStackTrace();
    return R.FAIL(e.getMessage());  // 直接返回内部异常信息
}
```

**修改建议**：
1. 通用异常返回友好提示："系统繁忙，请稍后重试"
2. 详细错误信息只记录到日志文件，不返回前端
3. 可以维护已知业务异常列表，对已知异常返回特定提示
4. 在开发环境可以通过配置返回详细信息（便于调试）

**验收标准**：
- [ ] 生产环境不返回内部异常信息
- [ ] 详细错误信息记录到日志
- [ ] 用户看到友好的错误提示
- [ ] 开发环境可配置是否返回详细信息

**代码示例**：
```java
// GlobalExceptionHandler.java
@ExceptionHandler(value = Exception.class)
public R handException(Exception e) {
    // 详细信息只记录到日志
    log.error("系统异常: {}", e.getMessage(), e);
    // 返回通用错误信息
    return R.FAIL("系统繁忙，请稍后重试");
}
```

---

## 五、安全隐患

### ISSUE-029：CORS 配置允许所有源 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-029 |
| 所属模块 | 系统安全 |
| 问题类型 | 安全隐患 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`config/CorsConfig.java:18` |

**问题详情**：
`CorsConfig` 使用 `addAllowedOriginPattern("*")` 允许所有源访问，结合 `setAllowCredentials(true)` 存在 CSRF 风险。任何域名都可以发起跨域请求并携带 Cookie。

**现状**：
```java
// CorsConfig.java:18
config.addAllowedOriginPattern("*");  // 允许所有源
config.setAllowCredentials(true);     // 允许携带 Cookie
```

**修改建议**：
1. 将 `addAllowedOriginPattern("*")` 改为具体的前端域名
2. 使用环境变量管理允许的源：`config.addAllowedOriginPattern(corsOrigin)`
3. 在 `application.yml` 中配置：`cors.allowed-origins: http://localhost:5173`
4. 生产环境配置实际的前端域名

**验收标准**：
- [ ] 只允许配置的域名访问
- [ ] 使用环境变量管理域名
- [ ] 生产环境不允许所有源

---

### ISSUE-030：JWT 密钥硬编码在代码中 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-030 |
| 所属模块 | 认证模块 |
| 问题类型 | 安全隐患 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`util/JWTUtils.java:23-25` |

**问题详情**：
JWT 密钥有默认值 `dY8300olWQ3345;1d<3w48`，如果未配置环境变量 `JWT_SECRET` 则使用硬编码密钥。代码泄露即可伪造 JWT。

**现状**：
```java
// JWTUtils.java:23-25
private static final String SECRET = System.getenv("JWT_SECRET") != null
        ? System.getenv("JWT_SECRET")
        : "dY8300olWQ3345;1d<3w48";  // 硬编码默认值
```

**修改建议**：
1. 移除默认值，启动时检查 `JWT_SECRET` 环境变量是否配置
2. 未配置时抛出异常阻止应用启动
3. 密钥长度至少 256 位（32 字符以上）
4. 建议使用密钥管理服务（如 AWS KMS、HashiCorp Vault）

**验收标准**：
- [ ] 无默认密钥值
- [ ] 未配置密钥时启动失败并报错
- [ ] 生产环境使用安全的密钥管理

**代码示例**：
```java
// JWTUtils.java
private static final String SECRET;
static {
    String secret = System.getenv("JWT_SECRET");
    if (secret == null || secret.isEmpty()) {
        throw new IllegalStateException("JWT_SECRET 环境变量未配置，应用无法启动");
    }
    SECRET = secret;
}
```

---

### ISSUE-032：导出 Excel 接口缺少权限控制 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-032 |
| 所属模块 | 客户管理 |
| 问题类型 | 安全隐患 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/CustomerController.java:85-101` |

**问题详情**：
`/api/exportExcel` 接口没有 `@PreAuthorize` 注解，任何登录用户都可以导出客户数据。导出的数据包含客户详细信息，可能存在数据泄露风险。

**现状**：
```java
// CustomerController.java:85-86
@GetMapping(value = "/api/exportExcel")  // 没有 @PreAuthorize 注解
public void exportExcel(HttpServletResponse response, @RequestParam(value = "ids", required = false) String ids) throws IOException {
```

**修改建议**：
1. 添加权限注解：`@PreAuthorize("hasAuthority('customer:export')")`
2. 或使用 `@PreAuthorize("hasAuthority('customer:list')")` 复用现有权限
3. 在权限表中添加对应的权限记录

**验收标准**：
- [ ] 导出接口有权限控制
- [ ] 无权限用户返回 403
- [ ] 权限粒度合理

---

### ISSUE-033：批量删除无数量限制 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-033 |
| 所属模块 | 多个模块 |
| 问题类型 | 安全隐患 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/ClueController.java:78-83`，`web/TranController.java:306-319`，`web/UserController.java:105-110` |

**问题详情**：
批量删除接口没有限制一次删除的数量，恶意用户可以传入大量 ID 一次性删除所有数据。

**现状**：
```java
// ClueController.java:78-83
@PostMapping(value = "/api/clue/batch")
public R batchDelClue(@RequestBody List<Integer> ids) {
    int del = clueService.batchDelClueByIds(ids);  // 未限制 ids 数量
    return del >= 1 ? R.OK() : R.FAIL();
}
```

**修改建议**：
1. 在所有批量操作接口中添加数量限制检查
2. 定义常量 `MAX_BATCH_SIZE = 100`
3. 超过限制时返回错误："单次批量操作最多支持 100 条记录"
4. 修改涉及所有批量删除接口

**验收标准**：
- [ ] 单次批量删除不超过 100 条
- [ ] 超限返回明确错误提示
- [ ] 所有批量接口统一限制

**代码示例**：
```java
// Constants.java
public static final int MAX_BATCH_SIZE = 100;

// ClueController.java
@PostMapping(value = "/api/clue/batch")
public R batchDelClue(@RequestBody List<Integer> ids) {
    if (ids.size() > Constants.MAX_BATCH_SIZE) {
        return R.FAIL("单次批量删除最多支持 " + Constants.MAX_BATCH_SIZE + " 条记录");
    }
    // ...
}
```

---

## 六、API 问题

### ISSUE-037：补货接口路径前后端不一致（存在重复端点）

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-037 |
| 所属模块 | 商品管理 |
| 问题类型 | 流程缺失 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`web/ProductStockController.java:22-26`，`web/ProductController.java:59-63` |

**问题详情**：
前端调用 `/api/productstock/restock`，后端同时存在两个补货端点：
- `ProductStockController`: `POST /api/productstock/restock`
- `ProductController`: `POST /api/products/stock/restock`

存在重复端点，代码维护困难，且可能调用到不同的实现。

**现状**：
```java
// ProductStockController.java:22-26 - 实际被调用的端点
@PostMapping("/restock")  // /api/productstock/restock
public Result<Void> restock(@RequestBody RestockRequest request) { ... }

// ProductController.java:59-63 - 重复端点
@PostMapping("/stock/restock")  // /api/products/stock/restock
public Result<Void> restock(@RequestBody RestockRequest request) { ... }
```

**修改建议**：
1. 移除 `ProductController` 中的重复端点
2. 统一使用 `ProductStockController` 的端点
3. 前端保持调用 `/api/productstock/restock`

**验收标准**：
- [ ] 只存在一个补货端点
- [ ] 前后端路径一致
- [ ] 移除重复代码

---

## 七、额外排查的后端问题

### ISSUE-044：交易更新缺少事务注解 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-044 |
| 所属模块 | 交易管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`service/impl/TranServiceImpl.java:93-107` |

**问题详情**：
`updateTransaction(TTran tTran)` 方法没有 `@Transactional` 注解。该方法只更新交易基本信息，虽然单个操作不需要事务，但被 `TranController.update()` 调用时，与其他操作（删除旧产品、插入新产品）不在同一事务中，导致数据不一致风险。

**现状**：
```java
// TranServiceImpl.java:93-107
@Override  // 缺少 @Transactional
public boolean updateTransaction(TTran tTran) {
    tTran.setEditTime(new Date());
    int rows = tranMapper.updateByPrimaryKeySelective(tTran);
    if (rows > 0) {
        clearTransactionCache(tTran.getId());
        return true;
    }
    return false;
}
```

**修改建议**：
1. 参照 ISSUE-007，将该方法与产品更新合并为一个事务方法
2. 或为该方法添加 `@Transactional(rollbackFor = Exception.class)` 注解
3. 修改 `TranController.update()` 调用新的统一方法

**验收标准**：
- [ ] 更新交易和更新产品在同一事务中
- [ ] 失败时全部回滚

---

### ISSUE-045：TranController.update() 事务不一致 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-045 |
| 所属模块 | 交易管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P1-严重 |
| 涉及文件 | 后端：`web/TranController.java:93-145` |

**问题详情**：
`TranController.update()` 方法在 Controller 层执行多个数据库操作（更新交易、删除旧产品、插入新产品），但 Controller 层没有事务控制。三个独立的 Service 调用各自在自己的事务中执行，中间失败无法回滚前面的操作。

**现状**：
```java
// TranController.java:122-141
boolean result = tranService.updateTransaction(tran);        // 事务1
if (result && ...) {
    tranService.deleteTransactionProducts(request.getId());  // 事务2
    tranService.addTransactionProducts(request.getId(), products); // 事务3
}
```

**修改建议**：
1. 将这三个操作合并到 `TranServiceImpl` 的一个新方法中
2. 该方法使用 `@Transactional(rollbackFor = Exception.class)`
3. Controller 只负责参数转换和调用 Service

**验收标准**：
- [ ] 所有操作在同一事务中
- [ ] 中间失败全部回滚
- [ ] Controller 层不包含业务逻辑

---

### ISSUE-046：Product 缺少 JSR-303 校验注解 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-046 |
| 所属模块 | 商品管理 |
| 问题类型 | 校验缺失 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`model/Product.java` |

**问题详情**：
`Product` 模型类没有任何 JSR-303 校验注解，即使 Controller 添加了 `@Valid` 注解也不会生效。

**现状**：
Product 模型没有 `@NotBlank`、`@NotNull`、`@DecimalMin` 等注解。

**修改建议**：
1. 在 `Product.java` 中添加校验注解：
   - `@NotBlank` on sku, name
   - `@NotNull` on price
   - `@Min(0)` on stock
2. 在 `ProductController` 的 `addProduct()` 和 `updateProduct()` 添加 `@Valid`

**验收标准**：
- [ ] Product 模型有完整的校验注解
- [ ] 校验失败返回明确错误信息

---

### ISSUE-047：CustomerController.convertCustomer 缺少权限控制 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-047 |
| 所属模块 | 客户管理 |
| 问题类型 | 安全隐患 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/CustomerController.java:61-65` |

**问题详情**：
`/api/clue/customer`（线索转客户）接口没有 `@PreAuthorize` 注解，任何登录用户都可以执行线索转客户操作。

**现状**：
```java
// CustomerController.java:61-65
@PostMapping(value = "/api/clue/customer")  // 无权限控制
public R convertCustomer(@RequestBody CustomerQuery customerQuery) {
    Boolean convert = customerService.convertCustomer(customerQuery);
    return convert ? R.OK() : R.FAIL();
}
```

**修改建议**：
1. 添加权限注解：`@PreAuthorize("hasAuthority('customer:transfer')")` 或复用 `clue:edit`
2. 在权限表中添加对应记录

**验收标准**：
- [ ] 接口有权限控制
- [ ] 无权限用户返回 403

---

### ISSUE-048：分页查询缺少用户自定义 page size 支持 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-048 |
| 所属模块 | 多个模块 |
| 问题类型 | 业务规则 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/ClueServiceImpl.java:36-44`，`service/impl/UserServiceImpl.java:90-98`，`web/ClueController.java:24-31`，`web/UserController.java:53-61` |

**问题详情**：
多个分页查询接口（线索、用户）的每页条数由后端常量 `Constants.PAGE_SIZE`（固定10）控制，前端无法传递 `pageSize` 参数调整每页显示数量。

**现状**：
```java
// ClueServiceImpl.java:36-44
public PageInfo<TClue> getClueByPage(Integer current) {
    PageHelper.startPage(current, Constants.PAGE_SIZE);  // 固定10条
    // ...
}
```

**修改建议**：
1. Service 方法增加 `pageSize` 参数
2. Controller 接收前端传递的 `pageSize` 参数
3. 添加参数校验：`pageSize` 范围限制在 1-100
4. 默认值仍为 `Constants.PAGE_SIZE`

**验收标准**：
- [ ] 前端可传递 pageSize 参数
- [ ] pageSize 有合理范围限制
- [ ] 默认值为10

---

### ISSUE-049：getCustomerByExcel 缺少空指针保护 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-049 |
| 所属模块 | 客户管理 |
| 问题类型 | 异常处理 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`service/impl/CustomerServiceImpl.java:48-79` |

**问题详情**：
`getCustomerByExcel()` 方法中直接访问 `tCustomer.getClueDO().getFullName()` 等，如果 `clueDO` 为 `null` 会抛出 `NullPointerException`。

**现状**：
```java
// CustomerServiceImpl.java:60
customerExcel.setFullName(tCustomer.getClueDO().getFullName());  // clueDO可能为null
```

**修改建议**：
1. 添加空指针保护：使用 `Optional` 或先检查是否为 `null`
2. 为 `null` 时设置默认空字符串
3. 同样检查其他 `getXxxDO()` 的返回值

**验收标准**：
- [ ] 不会出现 NullPointerException
- [ ] 关联数据为空时使用默认值

---

### ISSUE-050：Excel 导出接口未处理大量数据场景 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-050 |
| 所属模块 | 客户管理 |
| 问题类型 | 性能问题 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/CustomerController.java:85-101`，`service/impl/CustomerServiceImpl.java:48-79` |

**问题详情**：
当 `ids` 参数为空时，`getCustomerByExcel()` 会导出所有客户数据。如果数据量很大（数万条），会占用大量内存并可能导致 OOM。且查询使用 `selectCustomerByExcel` 关联了多张表，大量数据时 SQL 性能差。

**现状**：
```java
// CustomerController.java:95-96
List<String> idList = StringUtils.hasText(ids) ? Arrays.asList(ids.split(",")) : new ArrayList<>();
List<CustomerExcel> dataList = customerService.getCustomerByExcel(idList);  // 空列表=全量查询
```

**修改建议**：
1. 限制单次导出的最大数量（如 10000 条）
2. 空 `ids` 时导出全部但添加数量限制
3. 大量数据时使用流式写入（EasyExcel 的 `write` 方法天然支持流式）
4. 添加超时保护

**验收标准**：
- [ ] 单次导出不超过 10000 条
- [ ] 不会出现 OOM
- [ ] 大数据量时有进度提示

---

### ISSUE-051：StatisticController 缺少权限控制 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-051 |
| 所属模块 | 统计报表 |
| 问题类型 | 安全隐患 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`web/StatisticController.java` |

**问题详情**：
统计接口 `/api/summary/data`、`/api/saleFunnel/data`、`/api/sourcePie/data` 没有 `@PreAuthorize` 注解，任何登录用户都可以访问统计数据。

**修改建议**：
1. 添加权限注解：`@PreAuthorize("hasAuthority('statistic:view')")`
2. 或根据业务需求设置不同的权限级别

**验收标准**：
- [ ] 统计接口有权限控制
- [ ] 无权限用户返回 403

---

### ISSUE-052：DlykServerApplication.cacheMap 线程安全问题 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-052 |
| 所属模块 | 字典管理 |
| 问题类型 | 数据不一致 |
| 严重程度 | P2-一般 |
| 涉及文件 | 后端：`DlykServerApplication.java` |

**问题详情**：
`DlykServerApplication.cacheMap` 使用普通 `HashMap` 存储字典缓存，在多线程环境下（如 Excel 并发导入）可能出现线程安全问题，导致数据不一致或 `ConcurrentModificationException`。

**修改建议**：
1. 将 `HashMap` 改为 `ConcurrentHashMap`
2. 或使用 `Collections.synchronizedMap()` 包装
3. 建议参照 ISSUE-043，移除 `cacheMap`，统一使用 Redis 缓存

**验收标准**：
- [ ] 缓存操作线程安全
- [ ] 并发导入不会出错

---

### ISSUE-053：Mapper 层无分页参数校验 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-053 |
| 所属模块 | 多个模块 |
| 问题类型 | 校验缺失 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 后端：多个 Mapper XML |

**问题详情**：
多个 Mapper XML 中的分页查询没有对分页参数进行校验。如果传入 `current=0` 或负数，PageHelper 可能产生异常 SQL。

**修改建议**：
1. 在 Service 层统一校验分页参数：`current < 1` 时设为 1，`pageSize < 1` 时设为默认值
2. 或使用 `@Min(1)` 注解在 Controller 参数上

**验收标准**：
- [ ] 分页参数有最小值限制
- [ ] 异常参数使用默认值

---

### ISSUE-054：Redis 缓存 key 未设置过期时间 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-054 |
| 所属模块 | 认证模块 |
| 问题类型 | 数据不一致 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 后端：`service/impl/UserServiceImpl.java:157-174` |

**问题详情**：
`getOwnerList()` 方法将负责人列表缓存到 Redis，使用 `redisManager.setValue()` 但没有设置过期时间。该缓存会永久存在，如果用户数据变更，缓存不会自动失效。

**现状**：
```java
// UserServiceImpl.java:171
redisManager.setValue(Constants.REDIS_OWNER_KEY, t);  // 无过期时间
```

**修改建议**：
1. 添加过期时间参数：`redisManager.setValue(Constants.REDIS_OWNER_KEY, t, 3600)`（1小时）
2. 或在用户数据变更时主动清除该缓存

**验收标准**：
- [ ] 缓存有过期时间
- [ ] 用户变更时缓存失效

---

### ISSUE-055：批次删除字典类型存在 N+1 查询问题 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-055 |
| 所属模块 | 字典管理 |
| 问题类型 | 性能问题 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 后端：`service/impl/DicServiceImpl.java:250-287` |

**问题详情**：
`deleteDicTypesByIds()` 方法中，对每个 typeCode 依次查询关联的字典值 ID，存在 N+1 查询问题。当批量删除大量字典类型时，会产生大量数据库查询。

**现状**：
```java
// DicServiceImpl.java:264-276
for (String typeCode : typeCodes) {
    List<Integer> dicValueIds = dicMapper.selectDicValueIdsByTypeCode(typeCode);  // N次查询
    // ...
}
```

**修改建议**：
1. 使用批量查询替代循环单条查询：`selectDicValueIdsByTypeCodes(List<String> typeCodes)`
2. SQL: `SELECT id FROM t_dic_value WHERE type_code IN (#{typeCodes})`
3. 减少数据库查询次数

**验收标准**：
- [ ] 批量删除只执行少量 SQL
- [ ] 性能不随数量线性下降

---

### ISSUE-056：所有删除操作缺少乐观锁保护 ✅

| 属性 | 内容 |
|------|------|
| 问题编号 | ISSUE-056 |
| 所属模块 | 多个模块 |
| 问题类型 | 数据不一致 |
| 严重程度 | P3-轻微 |
| 涉及文件 | 后端：多个 Service 实现类 |

**问题详情**：
所有删除操作（删除线索、交易、用户等）没有使用乐观锁。在并发场景下，用户 A 查看到数据后准备删除，但用户 B 已经先删除了该数据，此时用户 A 的删除操作虽然不会报错（影响行数为0），但也不会返回明确的失败原因。

**修改建议**：
1. 删除前先查询数据是否存在
2. 使用 `version` 字段实现乐观锁
3. 或在删除后检查影响行数，为 0 时返回"数据不存在或已被删除"

**验收标准**：
- [ ] 删除不存在的数据返回明确提示
- [ ] 并发删除不出现数据不一致

---

## 问题优先级汇总

### P0-致命（必须立即修复）
1. ISSUE-002：交易状态流转缺少非法状态校验
2. ISSUE-006：库存扣减下限校验不完整
3. ISSUE-008：交易编号和发票号码存在重复风险

### P1-严重（尽快修复）
1. ISSUE-001：线索转客户并发问题
2. ISSUE-003：交易删除未校验状态限制
3. ISSUE-004：客户删除未级联处理交易记录
4. ISSUE-007：交易更新时库存操作非原子
5. ISSUE-009：删除用户未处理关联数据
6. ISSUE-021：全局异常处理返回内部错误信息
7. ISSUE-029：CORS 配置允许所有源
8. ISSUE-030：JWT 密钥硬编码
9. ISSUE-037：补货接口存在重复端点
10. ISSUE-044：交易更新缺少事务注解
11. ISSUE-045：TranController.update() 事务不一致

### P2-一般（计划修复）
1. ISSUE-005：线索删除未级联处理备注
2. ISSUE-010：删除字典类型级联删除范围过广
3. ISSUE-012：后端产品接口缺少校验
4. ISSUE-015：线索编辑手机号校验
5. ISSUE-016：发票金额未校验
6. ISSUE-017：同一交易可多次开票
7. ISSUE-018：审批拒绝后无法重新提交
8. ISSUE-019：客户归属机制缺失
9. ISSUE-032：导出接口缺少权限控制
10. ISSUE-033：批量删除无数量限制
11. ISSUE-042：Token 刷新竞态条件
12. ISSUE-043：字典缓存刷新机制不完善
13. ISSUE-046：Product 缺少校验注解
14. ISSUE-047：线索转客户接口缺少权限控制
15. ISSUE-048：分页查询缺少用户自定义 page size
16. ISSUE-049：getCustomerByExcel 空指针保护
17. ISSUE-050：Excel 导出未限制数据量
18. ISSUE-051：StatisticController 缺少权限控制
19. ISSUE-052：cacheMap 线程安全问题
20. ISSUE-054：Redis 缓存未设置过期时间

### P3-轻微（可选修复）
1. ISSUE-020：线索转客户默认数量为1
2. ISSUE-053：Mapper 层无分页参数校验
3. ISSUE-055：批次删除字典类型 N+1 查询
4. ISSUE-056：删除操作缺少乐观锁

---

## 修复建议优先级

1. **第一阶段（1-2天）**：修复所有 P0 问题，确保核心业务流程正确
2. **第二阶段（3-5天）**：修复 P1 问题，处理数据一致性和安全性
3. **第三阶段（1-2周）**：修复 P2 问题，完善业务规则和用户体验
4. **第四阶段（持续优化）**：修复 P3 问题，提升整体质量

---

*审计报告生成时间：2026-05-30*
*审计工具：opencode*
