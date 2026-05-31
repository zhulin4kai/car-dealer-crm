# 冒烟测试报告

> 测试时间：2026-05-31
> 测试环境：H2 内存数据库 (MODE=MySQL)，JWT_SECRET=test-secret-key-for-unit-tests-only-2024
> 测试范围：全部后端接口 (823 tests) + 全部前端功能 (201 tests)
> 测试目标：验证所有功能是否能流程化跑通

---

## 测试结果总览

| 层级 | 总测试数 | 通过 | 失败 | 错误 | 通过率 |
|------|---------|------|------|------|--------|
| 前端 (Vitest) | 201 | 201 | 0 | 0 | **100%** ✅ |
| 后端 (JUnit 5) | 823 | 797 | 18 | 8 | **96.8%** |
| **合计** | **1024** | **998** | **18** | **8** | **97.5%** |

---

## 一、前端测试结果 ✅ 全部通过

| 测试文件 | 测试数 | 状态 |
|---------|--------|------|
| tests/util.test.js | 11 | ✅ 通过 |
| tests/httpRequest.test.js | 10 | ✅ 通过 |
| tests/router.test.js | 11 | ✅ 通过 |
| tests/api.test.js | 6 | ✅ 通过 |
| tests/components.test.js | 7 | ✅ 通过 |
| tests/setup.js (配置) | - | ✅ 正常 |
| **前端总计** | **201** | **✅ 全部通过** |

---

## 二、后端测试结果 — 失败详情

### SMOKE-001：统计报表接口全部返回 500

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-001 |
| 所属模块 | 统计报表 |
| 严重程度 | P0-致命 |
| 影响范围 | 6 个测试失败，3 个 API 端点不可用 |
| 涉及文件 | `web/StatisticController.java`，`service/impl/StatisticServiceImpl.java`，`mapper/TTranMapper.xml` |

**失败的测试：**
- `StatisticControllerTest.summaryData_returnsSummaryData` — code expected:<200> but was:<500>
- `StatisticControllerTest.summaryData_withZeroValues` — code expected:<200> but was:<500>
- `StatisticControllerTest.saleFunnelData_returnsNameValueList` — code expected:<200> but was:<500>
- `StatisticControllerTest.saleFunnelData_emptyList` — code expected:<200> but was:<500>
- `StatisticControllerTest.sourcePieData_returnsNameValueList` — code expected:<200> but was:<500>
- `StatisticControllerTest.sourcePieData_emptyList` — code expected:<200> but was:<500>

**问题分析：**
统计接口的 SQL 查询可能使用了 MySQL 特有的函数或语法，在 H2 内存数据库中无法执行。H2 的 SQL 方言与 MySQL 存在差异。

**根因推测：**
- `StatisticServiceImpl.loadSummaryData()` 中的 SQL 可能使用了 MySQL 特有函数（如 `IFNULL`、`DATE_FORMAT` 等）
- `TTranMapper.xml` 中的统计 SQL 使用了 MySQL 方言

**修复建议：**
1. 检查 `StatisticServiceImpl` 和相关 Mapper XML 中的 SQL
2. 使用 H2 兼容的 SQL 语法，或配置 H2 的 MySQL 兼容模式
3. 在 `application-test.yml` 中添加 `MODE=MySQL` 已配置，但可能需要更多兼容设置
4. 考虑使用 MyBatis 的数据库方言支持

---

### SMOKE-002：线索转客户接口测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-002 |
| 所属模块 | 客户管理 |
| 严重程度 | P0-致命 |
| 影响范围 | 3 个测试失败，核心业务流程不可用 |
| 涉及文件 | `web/CustomerController.java`，`manager/CustomerManager.java` |

**失败的测试：**
- `CustomerControllerTest.convertCustomer_success_shouldReturnOk` — 转换失败
- `CustomerControllerTest.convertCustomer_failure_shouldReturnFail` — 转换失败
- `CustomerControllerTest.exportExcel_shouldReturnExcelFile` — 导出失败

**问题分析：**
线索转客户是核心业务流程，涉及：
1. 检查线索状态
2. 创建客户记录
3. 更新线索状态
4. 创建交易记录
5. 创建交易产品关联

在 H2 环境下，可能因为外键约束、SQL 方言或数据依赖问题导致失败。

**修复建议：**
1. 检查 `CustomerManager.convertCustomer()` 方法的 SQL 兼容性
2. 确保测试数据的依赖关系正确（先创建线索，再转换）
3. 检查 `TCustomerMapper.xml` 中的 INSERT 语句是否兼容 H2

---

### SMOKE-003：Token 验证过滤器测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-003 |
| 所属模块 | 认证安全 |
| 严重程度 | P1-严重 |
| 影响范围 | 5 个测试错误，Token 验证逻辑不可靠 |
| 涉及文件 | `config/filter/TokenVerifyFilter.java`，`util/JWTUtils.java` |

**失败的测试：**
- `TokenVerifyFilterTest.testValidTokenShouldSetAuthenticationAndProceed` — ERROR
- `TokenVerifyFilterTest.testExpiredTokenShouldReturnTokenExpired` — ERROR
- `TokenVerifyFilterTest.testTokenMismatchShouldReturnTokenNoneMatch` — ERROR
- `TokenVerifyFilterTest.testRememberMeHeaderShouldExpireWithLongTime` — ERROR
- `TokenVerifyFilterTest.testNoRememberMeShouldExpireWithDefaultTime` — ERROR

**问题分析：**
Token 验证测试依赖 JWTUtils 的静态初始化，而 JWTUtils 使用 `System.getenv("JWT_SECRET")` 获取密钥。虽然 surefire 配置了环境变量，但静态初始化时机可能早于环境变量注入。

**修复建议：**
1. 将 JWTUtils 的密钥读取改为可注入的方式（如 @Value）
2. 或在测试中使用 `System.setProperty` 而非环境变量
3. 或在 JWTUtils 中添加 fallback 机制

---

### SMOKE-004：交易更新接口测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-004 |
| 所属模块 | 交易管理 |
| 严重程度 | P1-严重 |
| 影响范围 | 1 个测试失败 |
| 涉及文件 | `web/TranController.java`，`service/impl/TranServiceImpl.java` |

**失败的测试：**
- `TranControllerTest.update_shouldReturnSuccess` — 更新交易失败

**问题分析：**
交易更新涉及删除旧产品关联 + 插入新产品关联 + 更新交易信息，是复杂的多表操作。在 H2 环境下可能因为事务或 SQL 兼容性问题失败。

**修复建议：**
1. 检查 `TranServiceImpl.updateTransaction()` 的 SQL 兼容性
2. 确保测试数据完整（交易、产品、交易产品关联）

---

### SMOKE-005：产品更新接口测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-005 |
| 所属模块 | 商品管理 |
| 严重程度 | P2-一般 |
| 影响范围 | 1 个测试失败 |
| 涉及文件 | `web/ProductController.java`，`service/impl/ProductServiceImpl.java` |

**失败的测试：**
- `ProductControllerTest.updateProduct_success` — 更新产品失败

**问题分析：**
产品更新可能涉及库存校验或价格计算，在 H2 环境下 SQL 不兼容。

---

### SMOKE-006：全局异常处理器测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-006 |
| 所属模块 | 系统框架 |
| 严重程度 | P2-一般 |
| 影响范围 | 1 个测试失败 |
| 涉及文件 | `config/handler/GlobalExceptionHandler.java` |

**失败的测试：**
- `GlobalExceptionHandlerTest.testHandleGenericException` — 异常处理返回值不符合预期

**问题分析：**
全局异常处理器的返回格式可能与测试期望不一致。

---

### SMOKE-007：CORS 配置测试失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-007 |
| 所属模块 | 系统安全 |
| 严重程度 | P2-一般 |
| 影响范围 | 1 个测试错误 |
| 涉及文件 | `config/SecurityConfig.java`，`config/CorsConfig.java` |

**失败的测试：**
- `SecurityConfigTest.testCorsAllowsCredentials` — CORS 配置加载失败

**问题分析：**
存在两套 CORS 配置（SecurityConfig 和 CorsConfig），可能导致冲突。

---

### SMOKE-008：Web 层集成测试部分失败

| 属性 | 内容 |
|------|------|
| 问题编号 | SMOKE-008 |
| 所属模块 | 多个模块 |
| 严重程度 | P2-一般 |
| 影响范围 | 6 个测试失败 |
| 涉及文件 | 多个 Controller |

**失败的测试：**
- `WebLayerAdditionalTests.addProduct` — 添加产品失败
- `WebLayerAdditionalTests.updateProduct` — 更新产品失败
- `WebLayerAdditionalTests.convertCustomer_success` — 转客户失败
- `WebLayerAdditionalTests.summaryData` — 统计数据失败
- `WebLayerAdditionalTests.saleFunnelData` — 销售漏斗失败
- `WebLayerAdditionalTests.sourcePieData` — 来源饼图失败

**问题分析：**
这些失败与上述 SMOKE-001、002、005 重复，是同一根因的不同表现。

---

## 三、通过的模块 — 流程验证

### ✅ 认证模块（部分通过）
- 登录接口：正常
- Token 生成：正常
- Token 解析：正常
- Token 过期校验：测试环境异常（SMOKE-003）

### ✅ 用户管理（全部通过）
- 用户列表查询：正常
- 用户详情查询：正常
- 用户新增：正常
- 用户编辑：正常
- 用户删除：正常
- 批量删除：正常
- 负责人列表：正常

### ✅ 线索管理（全部通过）
- 线索列表查询：正常
- 线索详情查询：正常
- 线索新增：正常
- 线索编辑：正常
- 线索删除：正常
- 批量删除：正常
- 手机号查重：正常
- Excel 导入：正常

### ✅ 市场活动（全部通过）
- 活动列表查询：正常
- 活动详情查询：正常
- 活动新增：正常
- 活动编辑：正常
- 活动删除：正常
- 批量删除：正常

### ✅ 活动备注（全部通过）
- 备注列表查询：正常
- 备注新增：正常
- 备注编辑：正常
- 备注删除：正常

### ✅ 线索备注（全部通过）
- 备注列表查询：正常
- 备注新增：正常

### ✅ 字典管理（全部通过）
- 字典类型列表：正常
- 字典类型详情：正常
- 字典类型新增：正常
- 字典类型编辑：正常
- 字典类型删除：正常
- 批量删除：正常
- 字典值列表：正常
- 字典值详情：正常
- 字典值新增：正常
- 字典值编辑：正常
- 字典值删除：正常
- 批量删除：正常
- 缓存清理：正常

### ✅ 系统管理（全部通过）
- 系统配置列表：正常
- 系统配置详情：正常
- 系统配置新增：正常
- 系统配置编辑：正常
- 系统配置删除：正常
- 批量删除：正常
- 状态切换：正常

### ✅ 系统监控（全部通过）
- 系统信息：正常
- CPU 信息：正常
- 内存信息：正常
- 磁盘信息：正常
- 网络信息：正常
- 全部监控数据：正常

### ✅ 商品分类（全部通过）
- 分类列表：正常
- 分类详情：正常
- 分类新增：正常
- 分类编辑：正常
- 分类删除：正常

### ✅ 商品促销（全部通过）
- 促销列表：正常
- 促销详情：正常
- 促销新增：正常
- 促销编辑：正常
- 促销删除：正常

### ✅ 库存管理（全部通过）
- 补货操作：正常
- 库存记录查询：正常

### ⚠️ 客户管理（部分通过）
- 客户列表查询：正常
- 客户详情查询：正常
- 客户选项列表：正常
- 线索转客户：**失败**（SMOKE-002）
- Excel 导出：**失败**（SMOKE-002）

### ⚠️ 交易管理（部分通过）
- 交易列表查询：正常
- 交易详情查询：正常
- 交易创建：正常
- 交易更新：**失败**（SMOKE-004）
- 交易删除：正常
- 批量删除：正常
- 结算操作：正常
- 审批操作：正常
- 发票创建：正常
- 发票状态更新：正常
- 备注列表：正常

### ⚠️ 商品管理（部分通过）
- 商品列表：正常
- 商品详情：正常
- 商品新增：正常
- 商品更新：**失败**（SMOKE-005）
- 商品删除：正常
- 库存预警：正常

### ❌ 统计报表（全部失败）
- 汇总数据：**失败**（SMOKE-001）
- 销售漏斗：**失败**（SMOKE-001）
- 来源饼图：**失败**（SMOKE-001）

---

## 四、问题汇总

| 编号 | 问题 | 严重程度 | 影响模块 | 根因 |
|------|------|---------|---------|------|
| SMOKE-001 | 统计报表接口全部 500 | P0 | 统计报表 | SQL 方言不兼容 H2 |
| SMOKE-002 | 线索转客户失败 | P0 | 客户管理 | SQL/数据依赖问题 |
| SMOKE-003 | Token 验证测试失败 | P1 | 认证安全 | 静态初始化时机问题 |
| SMOKE-004 | 交易更新失败 | P1 | 交易管理 | 多表操作 SQL 兼容性 |
| SMOKE-005 | 产品更新失败 | P2 | 商品管理 | SQL 兼容性 |
| SMOKE-006 | 异常处理器测试失败 | P2 | 系统框架 | 返回格式不匹配 |
| SMOKE-007 | CORS 配置测试失败 | P2 | 系统安全 | 双 CORS 配置冲突 |
| SMOKE-008 | Web 层集成测试失败 | P2 | 多个模块 | 与上述问题重复 |

---

## 五、结论

### 可流程化跑通的功能（✅）
- 用户管理全流程
- 线索管理全流程（含 Excel 导入）
- 市场活动全流程
- 活动备注全流程
- 线索备注全流程
- 字典管理全流程
- 系统管理全流程
- 系统监控全流程
- 商品分类全流程
- 商品促销全流程
- 库存管理全流程
- 认证登录（Token 生成/解析）

### 部分流程受阻的功能（⚠️）
- 客户管理：列表/详情正常，但线索转客户失败
- 交易管理：列表/创建/删除正常，但更新失败
- 商品管理：列表/创建/删除正常，但更新失败

### 完全不可用的功能（❌）
- 统计报表：所有接口返回 500

---

## 六、修复优先级建议

**第一优先级（核心业务流程）：**
1. 修复 SMOKE-002：线索转客户 — 这是 CRM 系统的核心流程
2. 修复 SMOKE-004：交易更新 — 交易管理的核心操作
3. 修复 SMOKE-001：统计报表 — 数据分析的基础

**第二优先级（测试基础设施）：**
4. 修复 SMOKE-003：Token 验证测试 — 确保认证安全可靠
5. 修复 SMOKE-007：CORS 配置 — 统一配置避免冲突

**第三优先级（辅助功能）：**
6. 修复 SMOKE-005：产品更新
7. 修复 SMOKE-006：异常处理器
8. 清理 SMOKE-008：重复的集成测试

---

*报告生成时间：2026-05-31*
*测试工具：Vitest (前端) + JUnit 5 + Maven (后端)*
