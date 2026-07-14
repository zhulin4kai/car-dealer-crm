# Mapper 扫描与递归资源布局任务

## 权威依据

- Plan：`docs/plan/后端架构/01-后端模块化目录重构方案.md`
- Rule：`docs/rule/01-开发流程与架构边界.md`
- Rule：`docs/rule/02-Java后端代码风格.md`
- Rule：`docs/rule/04-数据库与MyBatis规范.md`
- Rule：`docs/rule/07-测试编写执行与验收规范.md`

## 目标

- Mapper 接口迁入任意业务模块后仍能被确定发现，不维护不断增长的扫描包列表。
- 所有 Mapper 接口显式标注 `@Mapper`，根包扫描只注册带注解接口，禁止误注册 Service。
- Mapper XML 支持 `mapper/<领域组>/<子域>/` 递归布局，同时保持当前根目录 XML 可用。
- 使用 Context 与 Mapper XML 测试证明 Bean 和 statement 没有丢失或重复。

## 允许修改范围

- `DealerCRMApplication.java` 的 Mapper 扫描配置。
- 全部现有 Mapper 接口的 `@Mapper` 注解。
- main、smoke、test Profile 的 `mybatis.mapper-locations`。
- Mapper 扫描、XML 发现和 Spring Context 直接测试。
- 本任务实施证据和必要的后端技术文档。

## 禁止修改范围

- 不移动业务 Mapper、Model 或 XML；实际移动由各领域批次负责。
- 不修改 SQL、namespace、statement、resultMap、Schema 和类型别名语义。
- 不修改 Service、事务、数据范围和业务状态。
- 不新增 Maven 依赖。

## 执行步骤

1. 补齐未标注 Mapper 的 `org.apache.ibatis.annotations.Mapper` import 和 `@Mapper`。
2. 将 `@MapperScan` 调整为扫描 `com.autodealer.crm`，并通过 `annotationClass` 限定为 `@Mapper` 接口。
3. 将 main、smoke、test 的 Mapper XML 路径调整为 `classpath*:mapper/**/*.xml`。
4. 增加架构测试，证明所有 `*Mapper.java` 接口均带 `@Mapper`。
5. 运行现有 Mapper XML 完整性、Spring Context、经营分析集成和完整后端测试。

## 验证命令

```bash
cd dealer-server && ./mvnw -Dtest=BackendModuleBoundaryTest,MapperXmlCompletenessTest,DealerCRMApplicationTests test
cd dealer-server && ./mvnw -DskipTests compile
while IFS= read -r file; do rg -q '^@Mapper$' "$file" || echo "$file"; done < <(rg --files src/main/java | rg '/[^/]*Mapper\.java$')
git diff --check
```

## 完成条件

- 77 个现有 Mapper 接口全部显式标注 `@Mapper`。
- 根包扫描不会把普通 Service 接口注册为 Mapper。
- 根目录和未来子目录中的 XML 均能由递归模式发现。
- Context、Mapper XML 完整性和完整后端测试通过。
- 差异不包含 SQL、Schema、API 或业务行为变化。

## 实施证据（2026-07-14）

- 77 个 `*Mapper.java` 均显式标注 `@Mapper`，缺失数为 0，且不存在连续重复 `@Mapper`。
- `DealerCRMApplication` 使用根包扫描，并以 `annotationClass = Mapper.class` 限定注册对象。
- main、smoke、test 三套配置均使用 `classpath*:mapper/**/*.xml`。
- `MapperXmlCompletenessTest` 递归发现 75 个 Mapper XML，校验 namespace 可加载、接口带 `@Mapper` 且 namespace 不重复。
- 定向验证 `BackendModuleBoundaryTest,MapperXmlCompletenessTest,DealerCRMApplicationTests,AiMapperContextContractTest,StatisticControllerH2IntegrationTest`：10 个测试通过，0 失败、0 错误、0 跳过。
- `CrossLayerConsistencyTest` 改为递归扫描后端 `*Controller.java`，5 个测试通过，0 失败、0 错误、0 跳过。
- 后端全量 `./mvnw test`：1039 个测试，0 失败、0 错误、4 跳过，构建成功。
- `git diff --check` 通过；未改动 SQL、Schema、Mapper statement 或前后端 API 契约。
