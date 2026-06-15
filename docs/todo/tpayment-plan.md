# TPayment 支付模型方案（半成品，讨论中）

> **状态：未完成，方案讨论中**
>
> 本文档记录了 TPayment 支付模型的讨论过程、已实现部分和待讨论事项。

---

## 一、背景

交易模块（TTran）存在 `PAYMENT` 阶段，但该阶段名存实亡——创建发票时进入，开具发票时退出，从未记录任何实际收款。种子数据中已有"定金""分期""尾款"等业务描述，但代码未实现。

## 二、已实现部分

### 2.1 枚举常量

| 枚举 | 文件 | 值 |
|------|------|-----|
| `PaymentMethod` | `enums/PaymentMethod.java` | CASH, BANK_TRANSFER, WECHAT, ALIPAY, CHECK, OTHER |
| `PaymentType` | `enums/PaymentType.java` | DEPOSIT(定金), INSTALLMENT(分期款), FULL(全款), BALANCE(尾款), REFUND(退款) |
| `PaymentStatus` | `enums/PaymentStatus.java` | PENDING, COMPLETED, FAILED, REFUNDED |

### 2.2 数据模型

**TPayment** (`model/TPayment.java`):
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键 |
| tranId | Integer | 关联交易 |
| paymentNo | String | 支付流水号 |
| amount | BigDecimal | 支付金额 |
| paymentMethod | String | 支付方式 |
| paymentType | String | 支付类型 |
| paymentStatus | String | 支付状态 |
| paymentTime | Date | 到账时间 |
| transactionRef | String | 第三方交易号 |
| remark | String | 备注 |

**数据库表 `t_payment`**：已在 `schema-test.sql` 中定义，`CarManager.sql` 待补充。

### 2.3 Mapper

- `TPaymentMapper.java` + `TPaymentMapper.xml`：CRUD + selectByTranId + deleteByTranId
- `TTranHistoryMapper.java` + `TTranHistoryMapper.xml`：阶段变更审计日志

### 2.4 Service 层

`TranServiceImpl` 新增方法：

- **`recordPayment(TPayment)`**：记录收款，检查 `SUM(已收) >= money` 时自动完成交易
- **`refundPayment(paymentId, userId)`**：退款 = 交易取消，恢复库存，回退阶段
- **`getTransactionPayments(tranId)`**：查询收款记录
- **`writeHistory()`**：每次阶段变更写入 `t_tran_history`

### 2.5 Controller

`TranController` 新增端点：

| 方法 | 端点 | 权限 | 说明 |
|------|------|------|------|
| POST | `/api/tran/payment` | `tran:edit` | 记录收款 |
| GET | `/api/tran/payment/{tranId}` | `tran:view` | 查询收款记录 |
| POST | `/api/tran/payment/{id}/refund` | `tran:edit` | 退款 |

### 2.6 前端

- `tran.types.ts`：新增 `TPayment` 类型
- `tran-api.ts`：新增 `recordPayment`、`fetchTranPayments`、`refundPayment`
- `tran/[id].vue`：新增收款记录卡片 + 收款对话框 + 退款按钮
- `tran/invoice/[id].vue`：解耦"创建即开具"

---

## 三、待讨论事项

### 3.1 分期计划（TODO）

当前实现为**自由金额的多次收款**。后续需支持：

- 预设分期计划：期数、每期金额、每期到期日
- 自动提醒：到期未付的催收提醒
- 逾期处理：逾期罚息、阶段变更

### 3.2 支付网关集成（TODO）

当前为手动确认到账（`payment_status = COMPLETED`）。后续需支持：

- 微信支付 / 支付宝回调自动确认
- 支付回调签名验证
- 支付状态同步

### 3.3 应收账款报表（TODO）

当前仅 dashboard 汇总 `successTranAmount` / `totalTranAmount`。后续需支持：

- 按客户统计待收金额
- 按时间段统计收款趋势
- 应收账款账龄分析

### 3.4 交易生命周期确认

当前修改后的生命周期：

```
QUOTATION → PENDING → APPROVED → PAYMENT ←→ COMPLETED
                ↑                      ↑
                |                SUM(已收) >= money 时进入
                +--- resubmit    (原子 CAS UPDATE)
```

需确认：
- 发票开具是否应该独立于收款？（当前已解耦）
- 退款后库存恢复策略是否正确？
- 是否需要"部分退款"（退部分金额，交易继续）？

### 3.5 统计模块联动

`StatisticManager` 需新增聚合查询：
- `SUM(amount) FROM t_payment WHERE payment_status='COMPLETED'` — 实收金额
- `tran.money - SUM(payment.amount)` — 应收账款

### 3.6 Dashboard 前端

`dashboard/index.vue` 需新增"实收/应收"指标卡片，待后端统计接口完善后接入。

---

## 四、已修复的相关 Bug

交易模块在 TPayment 开发前修复了 30 个 Bug：

- **Critical 7**：@PreAuthorize 缺失、阶段校验绕过、级联删除遗漏、SQL 注入、UNIQUE 约束缺失等
- **High 9**：TOCTOU 竞态条件（approve/invoice/status 改为原子 CAS）、金额修改限制、发票 null 校验等
- **Medium 8**：ID 碰撞、TooManyResultsException、batch 限制、发票作废处理等
- **Low 6**：NPE 防护、正则修正、方法命名等

详见 commit `8059e66`。

---

## 五、相关文件清单

```
新增:
  enums/PaymentMethod.java
  enums/PaymentType.java
  enums/PaymentStatus.java
  model/TPayment.java
  model/TTranHistory.java
  mapper/TPaymentMapper.java
  mapper/TPaymentMapper.xml
  mapper/TTranHistoryMapper.java
  mapper/TTranHistoryMapper.xml

修改:
  service/TranService.java          (新增 recordPayment/refundPayment/getTransactionPayments)
  service/impl/TranServiceImpl.java  (支付逻辑 + 历史记录)
  web/TranController.java           (支付 API 端点)
  config/SecurityConfig.java        (CORS + 权限)
  CarManager.sql                    (新权限 + UNIQUE 约束)
  schema-test.sql                   (t_payment 表)

前端:
  modules/tran/model/tran.types.ts
  modules/tran/api/tran-api.ts
  pages/dashboard/tran/[id].vue
  pages/dashboard/tran/invoice/[id].vue
```
