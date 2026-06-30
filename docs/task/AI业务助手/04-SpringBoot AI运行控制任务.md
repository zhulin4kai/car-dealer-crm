# SpringBoot AI 运行控制任务

## 任务目标

调整 Spring Boot AI Run、SSE、Run trace 和 `DealerAiClient`，让每次 Run 使用管理员启用的 Provider 配置，并把 `providerRuntimeConfig` 安全传给 `dealer-ai`。

## 前置阅读

- docs/plan/AI业务助手/00-AI助手整体架构与边界方案.md
- docs/plan/AI业务助手/01-模型供应商配置与密钥治理方案.md
- docs/plan/AI业务助手/05-运行追踪审计与数据保留方案.md

## 允许修改范围

- dealer-server/src/main/java/com/autodealer/crm/ai/dto/
- dealer-server/src/main/java/com/autodealer/crm/ai/service/DealerAiClient.java
- dealer-server/src/main/java/com/autodealer/crm/ai/service/impl/AiConversationServiceImpl.java
- dealer-server/src/main/java/com/autodealer/crm/ai/service/impl/AiTraceServiceImpl.java
- dealer-server/src/test/java/com/autodealer/crm/ai/
- docs/api/openapi.yaml
- docs/backend.md
- docs/integration.md

## 禁止修改范围

- 不得把 `providerRuntimeConfig` 写入 Run trace、SSE 或前端响应。
- 不得把 API Key 写日志。
- 不得改变普通业务 API。

## 执行步骤

1. Run 启动前读取当前启用 Provider 配置。
2. 没有启用配置时拒绝启动并返回稳定错误。
3. 解密 API Key 后构造 `providerRuntimeConfig`。
4. 扩展 `DealerAiRunRequest`，只在服务间请求中携带 runtime config。
5. `DealerAiClient` 设置连接和读取超时。
6. SSE 过滤 runtime config 和敏感字段。
7. Run trace 不保存 runtime config。
8. 补齐成功、失败、无配置、Provider 错误和密钥脱敏测试。

## 完成条件

- Spring Boot 是 Provider 配置真源。
- `dealer-ai` 收到运行时配置。
- 前端、SSE、trace、日志均不出现 API Key。
- Run 失败有稳定错误码。

## 验证命令

- `cd dealer-server && ./mvnw -DskipTests compile`
- `cd dealer-server && ./mvnw test`
- `rg -n "providerRuntimeConfig|maskedApiKey|AI_PROVIDER_CONFIG" dealer-server/src docs`
- `rg -n "apiKey|encryptedApiKey|providerRuntimeConfig" dealer-server/src/main/java/com/autodealer/crm/ai/service/impl/AiTraceServiceImpl.java dealer-server/src/main/java/com/autodealer/crm/ai/dto/AiRunTraceResponse.java`

## 交付说明

完成后说明：

- Provider runtime config 如何进入 `dealer-ai`。
- 哪些位置明确不会保存密钥。
- 无启用配置时如何处理。
