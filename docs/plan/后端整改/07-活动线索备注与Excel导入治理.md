# 活动、线索、备注与 Excel 导入治理

## 目标

修复活动删除、线索手机号、备注契约和 Excel 导入脏数据问题，建立可解释、可验证且不部分写入的导入流程。

## 当前问题

### 线索与活动

- 线索手机号只做“先查再插”，并发时仍可重复。
- 更新手机号时当前实现会静默忽略新值，用户看到成功但数据未变化。
- 活动物理删除前只检查数据权限，没有检查线索引用。

### 备注

`ActivityRemarkQuery`、`ClueRemarkQuery` 缺少父对象 ID、内容长度和备注方式校验，Service 使用 BeanUtils 后直接写入。

### Excel 导入

- 多个 converter 依赖 `DealerCRMApplication.cacheMap`，匹配失败返回 `-1`。
- Listener 不校验 `-1`，会写入不存在的字典或商品 ID。
- Controller 没有可靠检查空文件、大小、格式和行数；空批次仍可能调用批量 SQL。
- 接口只返回成功或异常，没有行号、字段和错误原因。

## 文件所有权

允许修改：

- Activity、ActivityRemark、Clue、ClueRemark 相关 Controller、Service、Model、Query、DTO、Mapper 和 XML
- `config/converter/` 下线索 Excel 使用的 converter
- `config/listener/UploadDataListener.java`
- 线索 Excel 输入/输出类型和新 ImportResult DTO
- 对应后端测试
- `dealer-web/src/modules/activity/`、`modules/clue/`、相关页面和测试

禁止修改应用入口、Redis 基础类、Schema、公共异常/错误码和其他业务模块。

## 修改方案

### 活动和线索生命周期

1. 活动删除前查询线索引用；被引用活动禁止物理删除，优先增加停用语义或返回资源被引用错误。
2. 线索创建依赖数据库手机号唯一约束，并把唯一冲突转换为稳定业务错误。
3. 明确手机号可修改：本次允许修改，但必须验证格式、唯一性、数据权限并记录审计；禁止静默忽略。
4. 删除线索前检查客户转换、备注和业务引用，已转客户线索不得物理删除。
5. Activity/Clue Mapper 写操作检查影响行数，列表、详情、备注均应用数据权限。

### 备注契约

1. 新增 `CreateActivityRemarkRequest`、`CreateClueRemarkRequest` 和必要更新 DTO。
2. 父对象 ID 必填，内容非空且限制长度，备注方式必须是合法字典值。
3. 写入前确认父对象存在且当前用户可访问。
4. Query 只用于筛选，显式声明 Lombok equals/hashCode 父类策略。

### 导入流程

1. 不再让 converter 读取静态全局缓存；导入开始时由 Service 一次性构造字典和商品名称映射上下文。
2. 解析 DTO 保留原始文本；转换和业务校验在可测试的导入校验器中完成。
3. 不使用 `-1` 哨兵值。无法匹配时记录行号、列名、原值和安全错误原因。
4. Controller 校验文件非空、扩展名、Content-Type、大小；解析器限制最大行数和单元格长度。
5. 防止 Excel 公式注入；导入文本以 `= + - @` 开头时按规则拒绝或转义。
6. Listener 空批次直接返回，批次写入检查影响行数。
7. 采用“先完整解析和校验，再单事务写入”的全成功策略；任一行错误时 importedCount 为 0。
8. 返回 `ImportResult`：总行数、有效行数、失败行数、导入数和 `ImportRowError[]`。
9. 同一文件内和数据库中手机号重复都必须在写入前报告；并发唯一冲突仍由数据库兜底。

### 权限与审计

- 活动、线索列表/详情/备注/导入/删除分别核对权限码和数据权限。
- 审计活动删除/停用、线索手机号变更、导入批次和备注写入；不记录完整 Excel 行和手机号全值。

## 测试

- 活动被线索引用时删除失败且活动、线索不变。
- 线索手机号合法修改成功，重复手机号和并发唯一冲突失败。
- 父对象不存在、无权限、空备注、超长备注和非法备注方式失败。
- 空文件、错误格式、超限文件、空工作表、未知字典、未知商品、重复手机号和公式内容测试。
- 任一导入行错误时数据库无新增；全部合法时批次完整写入。
- ImportResult 行号、字段和数量准确，不包含敏感数据。
- 前端导入结果、错误列表、loading 和重复提交测试。

## 验收

- 导入流程不再依赖 `DealerCRMApplication.cacheMap`，不再产生 `-1` 外键。
- 活动和线索删除不会留下孤儿或破坏转换历史。
- 备注写入具备 DTO 校验、父对象校验和数据权限。
- 导入失败可定位到具体行和字段，并保证事务一致性。

---

## 实施状态

- **状态**：进行中（Excel 导入校验、转换、全量写入与错误响应链已完成）
- **实际修改文件**：`ClueController`、`ClueServiceImpl`、`ClueImportValidator`、导入 DTO、`TClueMapper` 及相关测试
- **已接入的生产入口**：`POST /api/importExcel`，包含文件边界校验、逐行校验、数据库重复检查和事务批量写入
- **已执行测试命令及结果**：`ClueImportValidatorTest` 20 tests、`ClueServiceImplTest` 15 tests 全部通过；`./mvnw clean test` — 408 tests, 0 failures, 0 errors
- **未完成项和阻塞原因**：活动/线索完整生命周期和删除保护仍按本 Plan 后续范围推进
