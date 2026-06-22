# 认证配置、Redis 与依赖治理

## 目标

完成 Redis 单一入口、安全序列化、环境配置和 Maven 依赖治理，同时保持已经修复的认证权限行为不退化。

## 当前问题

### Redis 序列化不安全

`DealerCRMApplication` 实现 `CommandLineRunner`，启动后修改 RedisTemplate，并启用 Jackson `DefaultTyping.EVERYTHING`。这扩大反序列化攻击面，也让 Bean 初始化顺序不清晰。

### Redis 封装未完全收口

认证和业务调用已逐步迁移到 `RedisManager`，但 `RedisService/RedisServiceImpl` 文件仍存在。需要确认无调用后删除，避免两套 API 重新被使用。

### 配置和依赖缺少闭环

需要核验 JWT secret、数据库/Redis凭据、CORS、日志级别、上传限制和生产 Profile；测试日志已经出现多个 SLF4J provider，需要检查依赖树和生产包依赖。

## 文件所有权

允许修改：

- `dealer-server/src/main/java/com/autodealer/crm/DealerCRMApplication.java`
- `dealer-server/src/main/java/com/autodealer/crm/config/SecurityConfig.java`
- `dealer-server/src/main/java/com/autodealer/crm/config/CorsConfig.java`
- `dealer-server/src/main/java/com/autodealer/crm/config/JwtSecretValidator.java`
- `dealer-server/src/main/java/com/autodealer/crm/config/security/`
- `dealer-server/src/main/java/com/autodealer/crm/config/filter/`
- 认证成功、失败、退出和拒绝访问 Handler，不含 `GlobalExceptionHandler`
- 新增或现有 Redis/Jackson 配置类
- `dealer-server/src/main/java/com/autodealer/crm/manager/RedisManager.java`
- `dealer-server/src/main/java/com/autodealer/crm/service/RedisService.java`
- `dealer-server/src/main/java/com/autodealer/crm/service/impl/RedisServiceImpl.java`
- `dealer-server/src/main/java/com/autodealer/crm/util/JWTUtils.java`
- `dealer-server/src/main/java/com/autodealer/crm/constant/`
- `dealer-server/src/main/resources/application*.yml`
- `dealer-server/pom.xml`
- 对应认证、Redis、配置和依赖测试
- 前端 `stores/auth.store.ts`、`stores/permission.store.ts`、`shared/storage/`、`router/guards.ts`

禁止修改业务 Controller、业务 Service、Mapper/XML、Schema 和 `GlobalExceptionHandler`。

## 修改方案

### Redis

1. 将 RedisTemplate 序列化器放入显式 `@Configuration` Bean，应用入口只负责启动。
2. 禁用 unrestricted default typing；选择固定 DTO、GenericJackson 安全配置或明确白名单。
3. 保持 String Key；Value/List/Hash 方法必须语义明确，失败行为不得把基础设施故障伪装成缓存未命中。
4. 全项目确认无 `RedisService` 调用后删除接口和实现，并同步测试。
5. Redis Key 统一由 `RedisKeys` 构造；认证会话、字典、交易和负责人 Key 分区明确。
6. 运行参数如会话 TTL、默认缓存 TTL 移入 `@ConfigurationProperties`，源码只保留真正不变的常量。

### 认证与环境

1. 保持 JWT 最小身份声明、Redis 会话校验、数据库账号状态校验和当前用户上下文。
2. JWT secret、数据库密码和 Redis 密码只能来自环境变量或非提交配置；危险默认值必须启动失败。
3. CORS 按 Profile 配置可信 Origin，使用 credentials 时禁止通配 Origin。
4. 核对 CSRF 禁用原因、公开路径、Filter 跳过路径和 SecurityConfig 放行路径一致。
5. 保持登录失败不枚举账号状态，401/403 和稳定错误码不得退化。
6. 配置上传大小、请求大小和生产日志级别；禁止生产默认打开 MyBatis SQL 明细。

### Maven

1. 执行 `dependency:tree` 和 `dependency:analyze`，确认重复 JSON、日志 provider、未使用依赖和错误 scope。
2. 移除导致多个 SLF4J provider 的直接或传递依赖，只保留 Spring Boot 默认日志实现。
3. 对 Spring Boot、MyBatis、PageHelper、EasyExcel、JWT、Jackson、Redis、OSHI 做漏洞检查。
4. 高危漏洞必须升级、替换或记录不可升级原因和缓解措施；禁止无关大版本整体升级。
5. 测试、开发工具依赖使用正确 scope，不进入生产包。

## 测试

- Redis Object、List、Hash、TTL、删除和 SCAN 模式删除测试。
- 恶意或未知类型不能通过 Redis 反序列化构造任意类型。
- 登录、记住我、Token 篡改、Redis 会话缺失、账号禁用、退出和权限拒绝测试。
- CORS 只允许配置 Origin；公开路径在 SecurityConfig 和 Filter 中一致。
- 缺少或使用弱 JWT secret 时应用拒绝启动。
- `dependency:tree` 不再出现多个 SLF4J provider，完整 Maven 测试通过。
- 前端认证恢复、强制退出和权限清理测试通过。

## 验收

- 只有一套 Redis 基础设施入口。
- 不再在应用启动后修改 RedisTemplate，不启用 unrestricted default typing。
- 敏感配置不进入 Git，生产配置没有危险默认值。
- 已修复认证链路和 HTTP 状态行为保持不变。

---

## 实施状态

- **状态**：已完成（P2-1）
- **实际修改文件**：
  - `dealer-server/pom.xml`（为 embedded-redis 添加 slf4j-simple exclusion，消除多 SLF4J provider 警告）
  - `dealer-server/src/main/java/com/autodealer/crm/query/` 下 5 个 BaseQuery 子类（添加 `@EqualsAndHashCode(callSuper = true)`）
- **2026-06-22 更新**：原 `SystemServiceImpl` 已随系统管理模块下线，不再作为本 Plan 的生产文件。
- **已接入的生产入口**：Maven 依赖树清理；BaseQuery 子类 equals/hashCode 正确继承
- **已执行测试命令及结果**：`./mvnw clean test` — 408 tests, 0 failures, 0 errors
- **未完成项和阻塞原因**：无
