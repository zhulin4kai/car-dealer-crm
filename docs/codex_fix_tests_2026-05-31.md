# 修复测试覆盖指南

生成时间：2026-05-31  
审查范围：`dealer-server/src/test/java`、`dealer-web/tests`、`dealer-server/src/test/resources`、`dealer-web/src/api`、`dealer-web/src/view`、`dealer-web/src/router`、`dealer-web/src/http`，以及 `docs/frontend.md`、`docs/backend.md`、`docs/api.md`、`docs/integration.md` 中描述的业务契约。

## 目标

当前测试最大的问题不是数量少，而是大量测试只证明“代码形状存在”，没有证明系统功能真的正确。修复后的目标应该是：

1. 测试失败时，优先代表真实业务行为、接口契约、权限、数据状态或前后端集成出现问题。
2. 测试不再靠 getter/setter、源码字符串、mock 返回值、`not.toThrow()`、路由存在性制造覆盖率。
3. 后端关键链路使用 H2 测试库跑真实 Controller -> Security -> Service -> Mapper -> SQL。
4. 前端关键链路挂载真实 View，通过用户操作触发 API、路由、状态和 DOM 变化。
5. 不下载真实 MySQL 或 Redis。数据库使用项目已有 H2 测试资源；Redis 使用 test profile 下的 fake/in-memory bean 或只在少量缓存契约测试里 mock。

## 当前数量结论

测试总量：

| 层 | 测试文件 | 测试用例 |
|---|---:|---:|
| 后端 | 58 | 823 |
| 前端 | 5 | 201 |
| 合计 | 63 | 1024 |

保守口径下，明确假覆盖或近假覆盖为 297 条，占 29.0%：

| 层 | 假/近假测试 |
|---|---:|
| 后端 | 212 |
| 前端 | 85 |
| 合计 | 297 |

宽口径下，如果把“只测试 mock service 的 Controller 壳”和“只测试 API wrapper 拼 URL”的测试也视为低价值覆盖，低价值测试约 667 条，占 65.1%。

## 判定标准

下面类型应该删除、合并或重写：

1. 纯 getter/setter/default constructor/builder 测试。
2. 纯常量值测试。
3. 只验证组件、方法、hook、setup、路由定义是否存在的测试。
4. 读取源码文本后 `toContain()` 某个字符串的测试。
5. 已经发现不一致，但只 `System.out.println()` 或写注释，不让测试失败。
6. `expect(() => fn()).not.toThrow()`，但不验证副作用、DOM、请求、返回值。
7. 用 mock service 预设返回值，再断言 Controller 把这个 mock 返回值包成 JSON，但完全绕过权限、service、mapper、事务、SQL。
8. 用 axios mock 断言前端 API wrapper 拼了某个 path，但不校验后端是否真的存在对应 method/path/request body/response body。

不是所有 mock 测试都要删。保留 mock 的前提是它验证了明确分支、异常路径、边界条件或外部依赖契约；否则它只能算壳层测试。

## 必须处理的假测试清单

### 后端：明确假/近假覆盖

| 测试文件 | 数量 | 问题 | 应该改成 |
|---|---:|---|---|
| `dealer-server/src/test/java/com/bjpowernode/query/QueryTest.java` | 42 | 主要测试 Query 对象 getter/setter/default value。不能证明查询参数真的影响 SQL、分页或权限过滤。 | 删除大部分。把必要默认值放到 Controller/Service/Mapper 集成测试里验证，例如 `/api/users?current=2`、`/api/activitys?ownerId=...` 是否真的影响 H2 查询结果。 |
| `dealer-server/src/test/java/com/bjpowernode/model/ModelTest.java` | 71 | 大量模型字段读写。少数 `TUser.getAuthorities()` 有行为价值。 | 删除纯 POJO 测试；保留并强化 `TUser` 权限转换测试。模型字段正确性改由 Mapper 集成测试证明，例如插入/查询 `t_user`、`t_clue`、`t_tran` 后断言字段映射。 |
| `dealer-server/src/test/java/com/bjpowernode/dto/DTOTest.java` | 20 | DTO getter/setter，不证明系统监控数据采集正确。 | 删除。改测 `SystemMonitorServiceImpl` 返回的 `SystemMonitorDTO` 是否有真实 CPU/JVM/Memory/Disk 结构，百分比在合法范围内，格式化字段不为空。 |
| `dealer-server/src/test/java/com/bjpowernode/constant/ConstantsTest.java` | 22 | 常量值锁死，和业务行为无关。 | 删除。常量应通过登录、鉴权、错误码响应间接验证，例如 token 为空时返回约定 code/msg。 |
| `dealer-server/src/test/java/com/bjpowernode/result/ResultTest.java` | 47 | 响应包装对象构造测试过多，覆盖率虚高。 | 只保留少量工厂方法契约测试，例如 `R.OK`、`R.FAIL(CodeEnum)`。其余通过真实 Controller 响应验证。 |
| `dealer-server/src/test/java/com/bjpowernode/integration/CrossLayerConsistencyTest.java` | 6 | 静态扫描不完整；有 mismatch 只打印不失败；`testResponseWrapperConsistency` 用 `assertFalse(controllersUsingResult.isEmpty())` 反而要求存在不一致。 | 重写为真正失败的契约测试。发现前端 path/method/backend response wrapper 不一致必须 fail。不要用注释记录 known issue 后放行。 |
| `dealer-server/src/test/java/com/bjpowernode/config/SecurityConfigTest.java` | 2 | 只测 bean 或配置形状，不能证明真实认证、鉴权、登出链路。 | 改成 `MockMvc` + filters enabled：未登录访问 `/api/users` 被拒绝；登录成功返回 token；无权限访问返回 503/403 契约；`/api/logout` 的 HTTP method 与前端/API 文档一致。 |
| `dealer-server/src/test/java/com/bjpowernode/DlykServerApplicationTests.java` | 1 | `contextLoads` 只能证明 Spring 上下文没炸。 | 可保留为 smoke，但不能计入功能覆盖。新增完整 H2 smoke：登录、获取用户信息、查询列表、创建或删除一条测试数据。 |
| `dealer-server/src/test/java/com/bjpowernode/model/TUserTest.java` | 1 | 如果只是补充模型方法，价值有限且和 `ModelTest` 重叠。 | 合并到专门的 `TUserSecurityTest`，只验证 `UserDetails` 行为和权限集合。 |

### 前端：明确假/近假覆盖

| 测试文件 | 数量 | 问题 | 应该改成 |
|---|---:|---|---|
| `dealer-web/tests/components.test.js` | 35 | 多数只断言组件有 `setup`、`methods`、`mounted`、某个文件存在或源码包含字符串。 | 按业务页面拆成 View 测试：挂载 `LoginView.vue`、`DashboardView.vue`、`UserView.vue`、`ClueView.vue` 等，模拟输入、点击、接口返回、DOM 更新和路由跳转。 |
| `dealer-web/tests/router.test.js` | 29 | 只检查路由数组里有某个 path，不能证明导航守卫、权限、token、重定向正确。 | 改成真实导航测试：无 token 访问 dashboard 回登录页；有 token 可进入；未知路由重定向；菜单权限缺失时不显示入口或被拒绝。 |
| `dealer-web/tests/httpRequest.test.js` | 约 11/24 | 存在纯数学判断 `code >= 500`、只检查 interceptor 函数存在、读源码字符串。 | 直接捕获 axios 注册的 request/response interceptor 并调用：session/local token 是否写入 Authorization；local token 是否加 rememberMe；后端返回 token error 时是否调用 confirm、removeToken、redirect。 |
| `dealer-web/tests/util.test.js` | 约 10/25 | 多个 `not.toThrow()` 和 `typeof string`，不验证真实副作用。 | mock `ElMessage`、`ElMessageBox`、storage、history，验证参数、返回 Promise、清理 token/permission 的副作用。 |

### 前端 API wrapper：低价值覆盖，不建议直接删除

`dealer-web/tests/api.test.js` 有 88 条，主要验证 `dealer-web/src/api/*.js` 调用了某个 method/path。它不是完全无用，因为能约束前端请求形状；但它不能证明后端真的支持这些接口，也不能证明请求体、权限、响应结构和页面使用方式正确。

修复方向：

1. 保留少量 wrapper 单元测试，只测动态 path、DELETE body、FormData 等容易写错的地方。
2. 新增前后端契约测试，以后端 Controller 注解为准生成或校验 method/path。
3. 对照文件：
   - `dealer-web/src/api/activity.js`
   - `dealer-web/src/api/clue.js`
   - `dealer-web/src/api/customer.js`
   - `dealer-web/src/api/dict.js`
   - `dealer-web/src/api/product.js`
   - `dealer-web/src/api/system.js`
   - `dealer-web/src/api/tran.js`
   - `dealer-web/src/api/user.js`
   - `dealer-server/src/main/java/com/bjpowernode/web/*Controller.java`
4. 契约测试必须校验 method、path、path variable、query/body、response code/msg/data shape。只校验 URL 字符串不够。

## Controller mock 测试改造

下面这些后端 web 测试合计约 282 条，当前主要价值是“路由能进 Controller，返回 JSON 壳正确”。它们不能证明真实业务正确，因为大量使用 `@MockBean service`，并且很多地方 `@AutoConfigureMockMvc(addFilters = false)` 绕过了鉴权。

涉及文件：

| 文件 | 用例数 | 改造建议 |
|---|---:|---|
| `ActivityControllerTest.java` | 11 | 用 H2 跑真实活动 CRUD、分页、权限。 |
| `ActivityRemarkControllerTest.java` | 9 | 用 H2 跑活动备注新增、列表、软删除。 |
| `ClueControllerTest.java` | 15 | 用 H2 跑线索创建、详情、查重、导入边界、转客户。 |
| `ClueRemarkControllerTest.java` | 6 | 用 H2 跑备注新增和查询。 |
| `CustomerControllerTest.java` | 12 | 用 H2 跑客户列表、详情、跟进字段。 |
| `DicControllerTest.java` | 28 | 用 H2 跑字典类型/字典值 CRUD 和缓存清理契约。 |
| `ProductCategoryControllerTest.java` | 6 | 用 H2 跑分类 CRUD。 |
| `ProductControllerTest.java` | 9 | 用 H2 跑产品 CRUD、库存字段、分类关联。 |
| `ProductPromotionControllerTest.java` | 6 | 用 H2 跑促销 CRUD、时间范围。 |
| `ProductStockControllerTest.java` | 6 | 用 H2 跑库存流水和库存变更。 |
| `StatisticControllerTest.java` | 6 | 用 H2 固定数据验证统计聚合。 |
| `SystemControllerTest.java` | 8 | 用 H2 跑系统配置读写，特别是 `isopen` 字段契约（不可写成 `isOpen`）。 |
| `SystemMonitorControllerTest.java` | 7 | 用真实 service 或受控 fake 验证监控结构。 |
| `TranControllerTest.java` | 30 | 用 H2 跑交易创建、产品扣库存、审批、发票。 |
| `UserControllerTest.java` | 16 | 用 H2 + Security 跑登录、权限、用户 CRUD。 |
| `WebLayerAdditionalTests.java` | 107 | 不要再扩展这个大杂烩。拆回各 Controller 或端到端业务链路测试。 |

改造原则：

1. 真正的功能测试不要 mock 对应业务 service。
2. 允许 mock Redis 或外部不可控系统，但数据库、Mapper、事务、权限应尽量真实。
3. `addFilters = false` 只能用于极少量 Controller 参数绑定测试；鉴权相关测试必须启用 filters。
4. 每个业务模块至少有一条成功路径、一条权限失败路径、一条数据不存在路径、一条参数错误路径。
5. 删除“大杂烩”测试文件，避免一个文件承载多个业务域，导致失败定位困难。

## 后端真实测试环境

项目已经有 H2 测试资源，不需要下载数据库：

| 文件 | 用途 |
|---|---|
| `dealer-server/src/test/resources/application-test.yml` | H2 datasource、MyBatis、PageHelper 测试配置 |
| `dealer-server/src/test/resources/schema-test.sql` | H2 测试表结构 |
| `dealer-server/src/test/resources/data.sql` | 测试种子数据 |
| `dealer-server/src/main/resources/schema-test.sql` | smoke profile 可复用结构 |
| `dealer-server/src/main/resources/data.sql` | smoke profile 可复用数据 |

建议新增一个基类：

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendIntegrationTestBase {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    protected String loginAsAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/login")
                .param("loginAct", "admin")
                .param("loginPwd", "123456"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }
}
```

如果当前登录响应结构不是 `$.data.token`，不要让测试迁就错误猜测；应先按 `MyAuthenticationSuccessHandler` 的真实响应修正测试读取路径，再把这个响应结构写进 `docs/api.md`。

Redis 处理方式：

1. 不下载 Redis。
2. 缓存无关测试：在 test profile 中用 `@MockBean RedisManager` 或 `@TestConfiguration` 提供 no-op/fake Redis。
3. 缓存相关测试：使用内存 Map fake，断言 key、ttl、deletePattern 等行为。不要只 `verify(redisManager)` 后就认为业务正确，还要验证 H2 中真实数据状态。

## 前端真实测试环境

前端测试应从“结构断言”改成“用户行为断言”。

建议工具：

1. `@vue/test-utils` 挂载真实 View。
2. `vitest` mock `vue-router`、Element Plus 消息组件、axios 边界。
3. 对 HTTP 层优先使用 axios adapter/mock 或 MSW 风格拦截，而不是读源码字符串。

必须覆盖的前端文件：

| 文件 | 应测行为 |
|---|---|
| `dealer-web/src/http/httpRequest.js` | baseURL、Authorization、rememberMe、token 失效处理、错误 Promise。 |
| `dealer-web/src/util/util.js` | token/permission storage、消息、确认框、history。 |
| `dealer-web/src/router/router.js` | token 门禁、未知路由、dashboard 子路由、权限菜单。 |
| `dealer-web/src/view/LoginView.vue` | 输入账号密码、记住我、成功跳转、失败提示。 |
| `dealer-web/src/view/DashboardView.vue` | 加载登录用户、菜单、退出登录、刷新路由。 |
| `dealer-web/src/view/UserView.vue` | 列表、分页、新增、编辑、删除、权限按钮。 |
| `dealer-web/src/view/ClueView.vue` 和 `ClueDetailView.vue` | 线索创建、详情、备注、转客户。 |
| `dealer-web/src/view/TranView.vue`、`TranDetailView.vue`、`TranApproveView.vue`、`TranInvoiceView.vue` | 交易列表、审批、开票。 |
| `dealer-web/src/view/ProductView.vue`、`ProductCategoryView.vue`、`ProductPromotionView.vue`、`ProductStockAlertView.vue` | 产品、分类、促销、库存告警。 |
| `dealer-web/src/view/SystemView.vue` | 系统配置字段，重点校验 `isopen` 与后端字段契约（不可写成 `isOpen`）。 |

示例：`httpRequest.js` 的响应拦截器测试不应该这样写：

```js
expect(content).toContain('messageConfirm')
expect(content).toContain('setTimeout')
```

应该这样写：

```js
const onFulfilled = axios.interceptors.response.use.mock.calls[0][0]
await expect(onFulfilled({ data: { code: 510, msg: 'token为空' } }))
  .rejects.toThrow('token为空')
expect(messageConfirm).toHaveBeenCalledWith('token为空，是否重新去登录？')
```

## 推荐优先修复的一条完整业务链路

先选“登录 -> 用户管理”作为第一条真实链路，因为它横跨 Security、JWT、权限、Controller、Service、Mapper、H2、前端 router、httpRequest、UserView。

### 后端链路

涉及生产文件：

| 环节 | 文件 |
|---|---|
| Security | `dealer-server/src/main/java/com/bjpowernode/config/SecurityConfig.java` |
| Token filter | `dealer-server/src/main/java/com/bjpowernode/config/filter/TokenVerifyFilter.java` |
| 登录成功/失败 | `dealer-server/src/main/java/com/bjpowernode/config/handler/MyAuthenticationSuccessHandler.java`、`MyAuthenticationFailureHandler.java` |
| 用户接口 | `dealer-server/src/main/java/com/bjpowernode/web/UserController.java` |
| 用户业务 | `dealer-server/src/main/java/com/bjpowernode/service/impl/UserServiceImpl.java` |
| 用户 SQL | `dealer-server/src/main/resources/mapper/TUserMapper.xml` 或对应 mapper XML |
| 测试数据 | `dealer-server/src/test/resources/schema-test.sql`、`data.sql` |

必须新增或重写的测试：

1. 未登录访问 `/api/users`：应被拒绝，不能返回业务数据。
2. 使用 `admin/123456` 登录：返回成功 code、token、用户基本信息或约定结构。
3. 携带 token 访问 `/api/login/info`：返回当前用户，不应该是 500。
4. 携带 token 访问 `/api/users?current=1`：返回 H2 种子用户列表，且分页字段正确。
5. 新增用户：POST `/api/user` 后，再 GET 详情或列表能查到。
6. 修改用户：PUT `/api/user` 后，H2 查询结果变化。
7. 删除用户：DELETE `/api/user/{id}` 后，详情或列表不可再查到。
8. 批量删除：DELETE `/api/user` body 为数组 `[id1,id2]`，必须和前端 `batchDeleteUsers(ids)` 保持一致。
9. 权限不足：用一个缺少 `user:add` 或 `user:delete` 的用户访问对应接口，应返回权限错误契约。
10. 登出：当前实现 `SecurityConfig` 已显式配置 `AntPathRequestMatcher("/api/logout", "GET")`，测试、前端 `DashboardView.vue`、`docs/api.md` 必须统一。如果业务决定改 POST，则先改实现和前端，再改测试。

### 前端链路

涉及生产文件：

| 环节 | 文件 |
|---|---|
| 请求封装 | `dealer-web/src/http/httpRequest.js` |
| token 和权限 | `dealer-web/src/util/util.js` |
| 路由 | `dealer-web/src/router/router.js` |
| 登录页 | `dealer-web/src/view/LoginView.vue` |
| 主框架 | `dealer-web/src/view/DashboardView.vue` |
| 用户页 | `dealer-web/src/view/UserView.vue` |
| 用户 API | `dealer-web/src/api/user.js` |

必须新增或重写的测试：

1. 登录页输入 `admin/123456`，mock `/api/login` 成功，断言 token 保存、跳转 `/dashboard`。
2. 登录失败，断言错误提示出现，且不跳转。
3. 没有 token 时访问 `/dashboard/user`，断言被导航回 `/`。
4. 有 token 但 `/api/login/info` 返回 token 错误，断言 `removeToken`、提示、重定向。
5. 打开 UserView，mock `/api/users` 返回两条数据，断言表格渲染姓名、分页。
6. 点击新增，填写表单，mock POST `/api/user` 成功，断言重新加载列表。
7. 点击删除，确认后 mock DELETE `/api/user/{id}`，断言列表刷新。
8. 批量删除时断言请求体是数组，不是 `{ ids: [...] }`。

## CrossLayerConsistencyTest 重写要求

当前这个测试最危险，因为它看起来像集成测试，但实际上有多处放行。

必须修：

1. 所有前端使用的 API path/method，如果后端不存在，测试必须 fail。
2. 所有后端公开 Controller path/method，如果 docs/api.md 标记为前端使用但前端没有调用，测试必须 fail。
3. 不允许 `System.out.println()` 后继续通过。
4. `testResponseWrapperConsistency` 应改为：

```java
assertTrue(controllersUsingResult.isEmpty(),
    "Controllers must use R.java consistently, but found: " + controllersUsingResult);
```

5. `testFieldNameConsistency` 应验证真实序列化字段，而不是只检查源码含有 `isopen`（绝不可写成 `isOpen`）。建议通过 `SystemController` 的真实 GET/PUT 响应验证字段名。
6. `testLogoutHttpMethod` 不要写“Spring Security default POST”。当前实现是 GET，测试应验证 docs、前端、后端三者一致；如果目标契约是 POST，则先改 `SecurityConfig` 和前端。
7. path 提取不能只读 `dealer-web/src/api`，还要扫描 View 中直接写死的请求或路由跳转；更好是集中禁止 View 直接写 API path。

## 应该删除或降权的覆盖率

这些测试即使保留，也不应该作为业务覆盖率的核心：

1. POJO getter/setter。
2. 常量值。
3. 响应包装对象全量构造。
4. 组件 method/setup 存在性。
5. 路由数组长度和 path 列表。
6. API wrapper 全量 path 快照。

如果保留，建议放入单独标签或目录，例如：

```text
src/test/java/.../contract-shape
dealer-web/tests/contract-shape
```

CI 指标上应区分：

1. `unit`: 小函数和真实分支。
2. `integration`: H2 + MockMvc + real service/mapper。
3. `contract`: 前后端 API 契约。
4. `e2e-smoke`: 后端 H2 smoke + 前端 dev server + 浏览器链路。
5. `shape`: 低价值结构测试，不计入核心质量门禁。

## 建议的重构顺序

第一阶段：修正最误导的测试。

1. 重写 `CrossLayerConsistencyTest`，让真实不一致失败。
2. 把 `SecurityConfigTest` 改成真实登录、鉴权、登出测试。
3. 删除或降权 `QueryTest`、`DTOTest`、`ConstantsTest`、大部分 `ModelTest`、大部分 `ResultTest`。
4. 把 `components.test.js` 和 `router.test.js` 从结构测试改成行为测试。

第二阶段：补第一条完整业务链路。

1. 后端新增 `UserFlowIntegrationTest`。
2. 前端新增 `LoginView.behavior.test.js`、`UserView.behavior.test.js`、`router.guard.test.js`。
3. 让测试覆盖登录、token、权限、用户列表、新增、编辑、删除、批量删除。

第三阶段：扩展到核心业务。

1. 线索：`ClueController`、`ClueServiceImpl`、`ClueView`、`ClueDetailView`。
2. 客户：`CustomerController`、`CustomerServiceImpl`、`CustomerView`。
3. 交易：`TranController`、`TranServiceImpl`、`TranView`、审批、开票、库存扣减。
4. 产品：`ProductController`、分类、促销、库存流水。
5. 字典和系统配置：重点验证前后端字段名和缓存清理契约。

第四阶段：建立冒烟测试。

1. 后端以 `test` 或 `smoke` profile 启动 H2。
2. 前端以 Vite 启动，指向后端随机端口或固定测试端口。
3. 浏览器执行最小链路：登录 -> 进入 dashboard -> 用户列表 -> 新增测试用户 -> 删除测试用户 -> 登出。
4. 冒烟测试失败时保留响应 body、浏览器 console、network、截图。

## 验收标准

修复完成后，测试质量应满足：

1. 删除或降权至少 297 条假/近假覆盖。
2. 后端至少 5 条真实 H2 业务链路测试：登录/用户、线索、客户、交易、产品或字典。
3. 前端至少 5 个真实行为测试：登录、路由守卫、用户列表、用户变更、token 失效。
4. Cross-layer 契约测试发现不一致必须失败。
5. Controller 测试中至少核心链路不再使用 `@MockBean` mock 对应 service。
6. 鉴权测试不得使用 `addFilters = false`。
7. 所有测试失败信息能定位到真实业务、接口契约或数据状态，而不是“某个方法不存在”“源码没包含某个字符串”。

## 需要同步更新的文档

测试修复时必须对照并更新：

1. `docs/api.md`：接口 method、path、请求体、响应结构、错误码。
2. `docs/backend.md`：认证、鉴权、Redis/H2 测试策略、Controller/Service 责任边界。
3. `docs/frontend.md`：路由守卫、token 存储、页面行为、API wrapper 约定。
4. `docs/integration.md`：前后端字段名、登出 method、批量删除 body、系统配置字段、错误码契约。

文档也在审查范围内。只要文档与实现或测试不一致，契约测试应该失败，不能把文档当作绝对正确来源。
