# Codex 项目全面审查报告

> 审查时间：2026-05-31  
> 审查范围：`docs/frontend.md`、`docs/backend.md`、`docs/api.md`、`docs/integration.md` 及前后端源码、测试、构建、H2 本地冒烟链路。  
> 抽样深挖链路：线索详情页“转换客户” -> 客户创建 -> 自动创建交易 -> 产品库存扣减。

## 1. 验证结果总览

| 项目 | 命令/方式 | 结果 |
|---|---|---|
| 前端测试 | `npm test` | 201 passed / 201 |
| 后端测试 | `JWT_SECRET=... ./mvnw test` | 823 passed / 823 |
| 前端生产构建 | `npm run build` | 通过，但两个 JS chunk 超过 500 kB，背景图 2.7 MB |
| 后端打包 | `./mvnw -DskipTests package` | 通过，产物约 71 MB |
| 后端覆盖率 | JaCoCo CSV | line 94.4%，method 85.4%，instruction 35.8%，branch 9.0% |
| 本地后端 smoke | `spring-boot:run profile=smoke server.port=0` | H2 启动成功，登录成功 |
| 本地前端服务 | `npm run dev -- --host 127.0.0.1 --port 0` | 入口 HTTP 200 |
| 登出接口实测 | `POST /api/logout` vs `GET /api/logout` | POST 返回业务 500，GET 返回 200 |

说明：后端单测日志中 `CrossLayerConsistencyTest` 明确打印了“前后端路径差异”，但断言仍通过，所以测试通过不能覆盖联调正确性。

## 2. 抽样业务链路审查

本次抽样链路为“线索转换客户并自动创建交易”：

1. `dealer-web/src/view/ClueDetailView.vue:497` 的 `convertCustomerSubmit()` 校验表单后调用 `convertClueToCustomer()`。
2. `dealer-web/src/api/clue.js:57` 向 `POST /api/clue/customer` 发送 `clueId/product/description/nextContactTime`。
3. `dealer-server/src/main/java/com/autodealer/crm/web/CustomerController.java:64` 接收 `CustomerQuery` 并调用 `customerService.convertCustomer()`。
4. `CustomerManager.convertCustomer()` 先将 `t_clue.state` 原子更新为 `-1`，再插入 `t_customer`，最后构造 `TTran` 和 `TTranProduct`。
5. `TranServiceImpl.createTransaction()` 插入交易、插入交易商品、调用 `ProductMapper.updateStock(..., -quantity)` 扣库存。

链路优点：

- `TClueMapper.xml:490` 使用 `WHERE id = #{id} AND state != -1`，能防止同一线索并发重复转换。
- `TranServiceImpl.createTransaction()` 对库存扣减返回行数做检查，库存不足会抛异常并回滚。
- 前端转换后重新拉取线索详情，能刷新“已转客户”状态。

关键问题：

- `CustomerController.convertCustomer()` 没有把当前登录人写入 `CustomerQuery.createBy`，而前端也没有发送 `createBy`。结果是线索 `edit_by`、客户 `create_by`、交易 `create_by` 都可能为空，审计、数据权限和后续统计都受影响。位置：`CustomerController.java:64`、`CustomerManager.java:41-63`、`TClueMapper.xml:492`。
- 如果前端传入的产品 ID 过期或不存在，`CustomerManager.java:69-85` 会创建客户和无产品交易，而不是失败返回。这会让“转换客户”链路生成不可结算交易。
- 该链路依赖后端业务错误返回 `code=500`，但前端响应拦截器把所有 `code >= 500` 都当成 token 失效处理，可能把普通业务失败误导成重新登录。位置：`httpRequest.js:70`。

## 3. 主要问题清单

### P0 - 退出登录前后端方法不一致，真实调用失败

- 前端 `DashboardView.vue:112-114` 使用 `doPost("/api/logout")`。
- 后端 `SecurityConfig.java:76-79` 显式配置 `AntPathRequestMatcher("/api/logout", "GET")`。
- 本地 smoke 实测：`POST /api/logout` 返回 `{"code":500,"msg":"系统繁忙，请稍后重试"}`；`GET /api/logout` 返回 `{"code":200,...}`。
- `docs/frontend.md:233` 写 POST，`docs/backend.md:113` 和 `docs/integration.md:421` 写 GET，文档本身互相矛盾。

建议：统一为一种方法。若保持 Spring Security 当前配置，前端改 `doGet("/api/logout")`，并补一条真实页面或接口集成测试覆盖 `DashboardView`，不要只扫描 `src/api`。

### P0 - 开票流程前端与后端响应协议不一致，可能破坏交易商品

- 后端 `TranController.createInvoice()` 返回 `R<Boolean>`，即 `data` 是布尔值：`TranController.java:231-241`。
- 前端 `TranInvoiceView.vue:404-410` 假设 `res.data.data.id` 是新发票 ID，随后调用 `/api/tran/invoice/{invoiceId}/status`。实际会拿到 `undefined`。
- `markAsIssued()` 中还会在 `updateInvoiceStatus()` 后额外调用 `updateTran({ id, stage: 46 })`：`TranInvoiceView.vue:441-451`。
- 后端 `updateInvoiceStatus("ISSUED")` 已经会把交易状态改为 46：`TranServiceImpl.java:370-388`。额外调用 `updateTran()` 不仅重复，而且 `TranController.update()` 不读取 `stage` 字段，最终会进入 `updateTransactionWithProducts()` 并在 `products == null` 时删除旧交易商品：`TranServiceImpl.java:544-555`。

建议：服务端开票状态流转保持单一事实源。前端创建发票后重新拉取发票列表；标记已开具只调用 `updateInvoiceStatus()`，删除额外 `updateTran()`。如果需要创建后立即返回 ID，后端响应类型改为 `R<TTranInvoice>` 或 `R<Integer>`。

### P1 - 线索转客户审计字段缺失

- `CustomerQuery.createBy` 存在字段定义：`CustomerQuery.java:19-20`。
- `CustomerController.convertCustomer()` 未从 `SecurityContext` 获取当前用户并赋值：`CustomerController.java:64-65`。
- `CustomerManager.convertCustomer()` 使用这个空值写线索编辑人、客户创建人、交易创建人：`CustomerManager.java:41-63`。

建议：Controller 层从 `Authentication` 中取 `TUser`，设置 `customerQuery.setCreateBy(currentUser.getId())`。同时补测试：前端不传 `createBy` 时，转换后的 `t_customer.create_by` 和 `t_tran.create_by` 必须是当前用户。

### P1 - 前端错误拦截把业务失败当 token 失效

- `httpRequest.js:70` 使用 `response.data.code >= 500` 判断 token 验证失败。
- 后端普通失败也常用 `R.FAIL()` 返回 `code=500`，例如登出 POST 实测、转换失败、创建失败等。

建议：只对 `CodeEnum` 中 token 相关码段处理登录跳转，例如 510-520；普通业务 500 应原样交给页面处理。长期建议使用 HTTP 401/403 表达认证授权失败。

### P1 - 集成一致性测试存在“发现问题但仍通过”

- `CrossLayerConsistencyTest` 输出了未匹配路径：`/api/products/stock/restock`、`/api/summary/data`、`/api/login/free` 等，但测试仍通过。
- 登出测试只扫描 `dealer-web/src/api`，而真实调用在 `DashboardView.vue`，所以没有捕获 POST/GET 冲突：`CrossLayerConsistencyTest.java:190-216`。

建议：将“前端调用后端不存在接口”“后端核心接口前端未覆盖”“退出登录方法不一致”从日志升级为断言；扫描范围覆盖 `src/view` 和 API 模块。

### P2 - 文档存在多处过期或互相矛盾

- `docs/integration.md:143-145` 写统计接口“前端未发现 API 调用”，但 `StatisticView.vue:69/79/184` 已调用三条统计接口。
- `docs/frontend.md:663` 和 `docs/api.md:2985` 仍记录 `/api/tran/status/{id}`，当前 `dealer-web/src/api/tran.js` 已无该函数。
- `docs/frontend_issues(fixed).md:466-481` 声称 `/api/productstock/restock` 与后端不匹配，但当前后端同时有 `ProductStockController` 的 `/api/productstock/restock` 和 `ProductController` 的 `/api/products/stock/restock`。
- `README.md:38` 写 JWT 依赖为 `JJWT 0.12.3`，实际 `pom.xml:91-95` 使用 `com.auth0:java-jwt:4.4.0`。
- `docs/smoke_test_report.md` 仍保留后端 18 失败、8 错误的描述；当前实测为 823 全通过。

建议：把文档生成/审查纳入 CI，至少用路由提取脚本比对 `src/api/*.js`、`src/view/*.vue` 和 Controller 路由。

### P2 - 线索/活动备注按钮存在空实现

- `ClueDetailView.vue:539-548` 显示编辑、删除按钮，但只提示“功能待实现”。
- 后端 `ClueRemarkController.java:19-40` 仅实现新增和列表查询，没有编辑/删除接口。
- `ActivityDetailView.vue` 也有“编辑功能待实现”。

建议：要么补齐备注 CRUD，要么隐藏未实现按钮，避免用户误以为功能可用。

### P2 - 调试日志残留较多

示例：

- `DashboardView.vue:106` 打印完整登录用户响应。
- `UserView.vue:289`、`DictTypeView.vue:269`、`DictValueView.vue:313/316` 仍有 `console.log`。
- 交易详情、审批、开票页多处打印交易详情、路由参数和接口响应。

建议：生产构建前移除日志，或基于 `import.meta.env.DEV` 包装；避免泄露用户、权限、交易等敏感信息。

### P2 - 重复端点与概念重复

- 补货存在两个入口：`/api/productstock/restock` 和 `/api/products/stock/restock`，都调用 `productService.restock()`。
- 商品模型存在 `Product` 和 `TProduct` 两套概念，`ProductServiceImpl` 还需要转换为 `TProduct`。这不是立即错误，但已经导致文档、SQL 映射、业务链路里出现重复认知成本。

建议：保留一个规范 REST 入口，另一个标注 deprecated 或删除；明确 `Product` 与 `TProduct` 的边界，最好逐步合并。

### P3 - 多余前端模板文件和构建体积

- `dealer-web/src/components/HelloWorld.vue` 未被业务使用，只保留了 Vite 示例内容。
- `dealer-web/src/assets/vue.svg`、`dealer-web/public/vite.svg` 未被引用；`dealer-web/src/assets/loginBox.svg` 也未被引用。
- `main.js:17-30` 全量注册 Element Plus icons，生产构建出现两个超过 1 MB 的入口 chunk。

建议：删除模板资产；改为按需引入图标，ECharts 和 Element Plus 也可拆分 manual chunks。

## 4. 测试与冒烟覆盖评价

已有覆盖：

- 后端测试数量充足，Controller/Service/Manager/Util 都有覆盖。
- H2 `MODE=MySQL` 可以解决当前没有实际 MySQL 的问题。
- 前端基础单测覆盖 API 封装、路由、工具函数和部分组件。

不足：

- 后端 JaCoCo 行覆盖率高，但 branch coverage 只有 9.0%，说明条件分支、异常分支和状态机路径仍缺测试。
- 前端没有运行覆盖率报告；当前 201 条测试主要验证静态导出/浅层行为，未覆盖真实页面业务流。
- 缺少端到端链路测试：登录、线索转客户、交易结算、审批、开票、登出。
- 缺少“测试必须失败”的联调断言，现有一致性测试把核心差异打印出来但不阻断。

建议补充的优先级测试：

1. 登出：前端方法与后端方法一致，POST/GET 只允许一种。
2. 开票：创建发票返回值、标记已开具、交易状态 43 -> 45 -> 46，且交易商品不被删除。
3. 线索转客户：前端不传 `createBy`，后端仍写入当前用户；产品不存在时应失败并回滚线索状态。
4. 前端页面级测试：`ClueDetailView` 转换客户、`TranInvoiceView` 开票、`DashboardView` 登出。
5. 接口提取比对：从 Controller 和前端 `doGet/doPost/doPut/doDelete` 自动生成差异表，差异不允许静默通过。

## 5. 结论

项目能构建、能跑测试，也能用 H2 在本地启动后端和前端入口；但真实业务面仍存在几个测试没挡住的高风险问题。最先应修复的是：退出登录方法冲突、开票流程响应协议和重复状态更新、线索转客户缺当前用户审计字段、以及前端把普通 500 当 token 失效。文档需要同步更新，否则后续维护会继续被过期问题误导。
