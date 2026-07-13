# 前后端联调分析报告

## 目录
1. [接口对接情况](#1-接口对接情况)
2. [联调问题检查](#2-联调问题检查)
3. [认证流程链路](#3-认证流程链路)
4. [文件上传下载流程](#4-文件上传下载流程)
5. [分页参数传递方式](#5-分页参数传递方式)
6. [错误处理流程](#6-错误处理流程)

---

## 账号邀请、改密与个人中心联调约定

- 激活和密码重置页面只从 URL fragment 读取原始凭证，再提交到 `/api/credentials/*`；服务端响应永不返回凭证、密码或摘要。
- 找回密码对存在和不存在账号返回相同的 `accepted + deliveryStatus` 结构。
- 创建、重新邀请、管理端重置和联系方式验证只有在凭证摘要与 Outbox 原子提交后才返回 HTTP `202`；响应固定为 `accepted=true + deliveryStatus=QUEUED`，仅表示已排队，不表示外部通知已经送达。渠道或派生密钥未配置时在写入前返回失败，不得伪装成排队成功。
- `mustChangePassword=true` 时，服务端只允许登录信息、本人会话查询、退出和首次改密路径，不允许先修改个人资料；前端路由守卫只是体验层。
- `GET /api/login/info` 返回 `protectedRecoveryAccount` 和 `userManagementGateState`。普通账号只有在门禁为 `READY` 时进入业务路由；受保护恢复账号仅在 `UNINITIALIZED` 时进入首个管理员初始化入口，且始终不能进入个人中心或修改自己的权限。业务请求返回 `641/642` 时前端保留登录态、重新读取门禁事实并跳转 `/user-management-gate`，不得按 401 清理会话。
- `/api/profile` 使用独立 `profileVersion` 做并发控制；资料更新成功后前端刷新当前用户 store，但不得把它当成授权版本。
- 管理者修改下属普通资料使用 `/api/users/{id}/profile`，同样携带 `profileVersion`；该入口不能用于调整本人、角色、权限、任职、管理者或账号状态。
- 管理者修改下属登录账号使用 `/api/users/{id}/login-account`，提交 `accountVersion + loginAct + reason`。旧账号进入永久归属历史，不能分配给其他员工；同一原用户可以再次启用自己的退休账号。
- 管理者调整安全期限使用 `/api/users/{id}/security-expiration`，分别提交 `accountExpiresAt` 和 `credentialExpiresAt`。两者是独立期限，空值表示对应期限不限制；成功后目标全部旧会话失效。
- 负责人候选必须提交服务端白名单 `permissionCode + qualificationContext`，响应使用最小 `OwnerCandidate` 投影。

## AI 业务助手联调约定

- 前端通过 `/api/ai/conversations` 创建、查询、重命名和归档 AI Conversation。Conversation 详情响应中的 `turns` 是会话恢复主契约，前端必须用它恢复每轮消息、业务卡片、Proposal、Workflow 和处理过程。
- 前端发送问题时调用 `/api/ai/runs`，优先携带当前 `conversationNo`；未携带时后端按当前用户和业务对象上下文解析默认会话。
- 前端订阅 `/api/ai/runs/{runNo}/events` 获取本次 Run SSE；断线时携带 `afterSequence` 重放后续持久化事件，同一 Run 不会再次启动。刷新恢复整个会话时调用 `/api/ai/conversations/{conversationNo}`。
- Spring Boot 调用 `dealer-ai` 时携带 `conversationNo`、脱敏会话摘要、管理员配置的最近 1 到 8 条活动消息、工具权限交集和运行策略；`dealer-ai` 不保存会话。
- 用户消息编辑调用 `PATCH /api/ai/conversations/{conversationNo}/messages/{messageNo}`，撤回调用对应 `/withdraw`；两者使用 `expectedVersion` 防止并发覆盖。
- 会话归档后默认列表不显示，但 Run trace 和审计链仍可按权限查询。

---

## 1. 接口对接情况

### 1.1 用户、组织与授权模块

| 能力 | HTTP 方法与路径 | 主要契约 | 后端入口 |
|------|-----------------|----------|----------|
| 用户工作台 | `GET /api/users`、`GET /api/users/filter-options`、`GET /api/users/{id}` | 稳定分页、白名单排序、目标级 `allowedActions`；本人详情只读 | `UserController` |
| 邀请创建 | `POST /api/users` | 不接受明文密码；账号、员工、主任职、初始角色、历史和邀请全成功或全回滚 | `UserController` |
| 受管资料与状态 | `PUT /api/users/{id}/profile`、`POST /api/users/{id}/status` | 分别使用资料版本和账号版本；本人、同级、上级、范围外与恢复账号拒绝 | `UserController` |
| 登录标识与安全期限 | `PUT /api/users/{id}/login-account`、`PUT /api/users/{id}/security-expiration` | 登录账号永久归属；账号与凭证期限分离；要求账号版本和原因并撤销旧会话 | `UserController` |
| 本人个人中心 | `GET/PUT /api/profile` | 只允许姓名、联系方式和头像白名单，不得改变账号、任职或授权 | `ProfileController` |
| 凭证生命周期 | `/api/credentials/*`、`POST /api/users/{id}/invitation`、`POST /api/users/{id}/password-reset` | 单次凭证、过期、重签失效、首次改密、密码历史和会话撤销 | `CredentialController` |
| 用户授权 | `GET /api/users/{id}/authorization`、`PUT .../roles`、`PUT .../permissions` | 本人可只读查看来源；任何人不能自改；角色与个人 `INHERIT/GRANT/DENY` 按版本原子保存 | `UserAuthorizationController` |
| 角色与权限目录 | `/api/roles`、`/api/roles/{id}/permissions*`、`GET /api/permissions/tree` | 角色适用组织、影响预览、矩阵版本、委派天花板和不可删除历史 | `RoleAccessController` |
| 组织、岗位与任职 | `/api/organization-units*`、`/api/positions*`、`/api/employees/{id}/organization-*`、`/api/employees/{id}/acting-reporting-relations*` | 组织树、负责人、主任职、兼岗、直属汇报、独立 ACTING 多关系、候选、版本和历史 | `OrganizationController` |
| 会话安全 | `/api/me/sessions*`、`/api/users/{id}/sessions*` | 本人撤销其他会话；管理者仅在范围内撤销下属会话 | `UserSessionController` |
| 历史查询 | `GET /api/users/{id}/history` | 聚合授权历史、操作日志和生命周期事件，稳定分页并安全投影 | `UserHistoryController` |

凭证交付不经过业务 HTTP 响应。生产部署通过 `CREDENTIAL_DELIVERY_WEBHOOK_URL`、`CREDENTIAL_DELIVERY_BEARER_TOKEN` 和独立 `CREDENTIAL_DERIVATION_KEY` 连接受信通知服务，地址必须为 HTTPS；本地人工联调只有在 `CREDENTIAL_DELIVERY_ALLOW_INSECURE_LOOPBACK=true` 时才允许回环 HTTP。创建、重邀和重置只在摘要与 Outbox 原子提交后返回 HTTP 202/`QUEUED`；Worker 提交后使用稳定 messageId 和 `Idempotency-Key` 投递。渠道未配置或无联系方式在入库前拒绝，网络失败进入重试，永久失败撤销凭证并审计。HUMAN 账号只使用员工档案当前联系方式，固定 SYSTEM 恢复账号使用专用账号联系方式。公开凭证流程按主体摘要、联系方式摘要与请求来源执行滑动窗口限流；签发事务以用户行锁和最近签发事实执行用途族冷却。忘记密码在限流、冷却或基础设施不可用时仍返回同形受理响应，不查询或暴露账号事实。
| 人员生命周期 | `/api/users/{id}/lifecycle*` | 调岗、离职预检、待交接、六域确认、完成离职和返聘；Quote/Tran 只做客户派生核验 | `UserLifecycleController` |
| 负责人候选 | `GET /api/owner` | 必须提交白名单权限码和资格场景；只返回最小候选投影 | `UserController` |
| 旧单数写路径 | `/api/user*` 旧创建、更新、状态、角色、密码和交接入口 | deprecated 且始终 fail-close；不得作为兼容写入口继续使用 | `UserController` |

### 1.2 线索管理模块 (clue.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getCurrentClues | GET | /api/clues | params: {page, size} | R\<PageInfo\<TClue\>\> | ClueController.cluePage | 分页查询线索列表 |
| addClue | POST | /api/clue | data: ClueQuery对象 (FormData) | R | ClueController.addClue | 新增线索；手机号按常见分隔符归一化后查重并落库 |
| updateClue | PUT | /api/clue | data: ClueQuery对象 (FormData) | R | ClueController.editClue | 编辑线索 |
| delClueById | DELETE | /api/clue/{id} | 路径参数: id | R | ClueController.delClue | 删除单个线索 |
| batchDeleteCluesByIds | POST | /api/clue/batch | data: List\<Integer\> (JSON数组) | R | ClueController.batchDelClue | 批量删除线索 |
| checkPhoneIsExist | GET | /api/clue/{phone} | 路径参数: phone | R | ClueController.checkPhone | 按归一化手机号检查是否存在 |
| getClueDetail | GET | /api/clue/detail/{id} | 路径参数: id | R\<TClue\> | ClueController.loadClue | 获取线索详情 |
| transferClueOwner | PUT | /api/clue/{id}/owner | 路径参数: id, data: {newOwnerId, reason} (JSON) | R | ClueController.transferOwner | 转派线索负责人并写责任历史 |
| getClueOwnerHistory | GET | /api/clue/{id}/owner-history | 路径参数: id | R\<List\<TClueOwnerHistory\>\> | ClueController.getOwnerHistory | 查询线索责任历史 |
| closeClue | PUT | /api/clue/{id}/close | 路径参数: id, data: {reason} (JSON) | R | ClueController.closeClue | 关闭线索；原因必填并写操作审计 |
| restoreClue | PUT | /api/clue/{id}/restore | 路径参数: id, data: {reason} (JSON) | R | ClueController.restoreClue | 恢复线索；原因必填并校验重复活跃线索 |
| importExcelAPI | POST | /api/importExcel | data: MultipartFile (FormData) | R\<ImportResult\> | ClueController.importExcel | Excel导入线索；逐行归一化手机号并返回重复摘要，存在失败行时 HTTP 422 但合法行可部分成功 |
| addClueRemark | POST | /api/clue/remark | data: {clueId, noteContent, noteWay} (JSON) | R | ClueRemarkController.addActivityRemark | 添加线索备注 |
| getClueRemarkList | GET | /api/clue/remark | params: {page, size, clueId} | R\<PageInfo\<TClueRemark\>\> | ClueRemarkController.clueRemarkPage | 分页查询线索备注 |
| convertClueToCustomer | POST | /api/clue/customer | data: {clueId, product, description, nextContactTime} (JSON) | R | CustomerController.convertCustomer | 线索转换为客户 |

### 1.3 客户管理模块 (customer.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getCustomerList | GET | /api/customers | params: {page, size, ...query} | R\<PageInfo\<TCustomer\>\> | CustomerController.list | 分页查询客户列表 |
| getCustomerOptions | GET | /api/customer/options | 无 | R\<List\<CustomerOption\>\> | CustomerController.options | 获取客户选项(下拉框用) |
| fetchCustomerDetail | GET | /api/customer/{id} | 路径参数: id | R\<CustomerDetailResponse\> | CustomerController.detail | 获取客户主档详情，敏感字段由后端按权限脱敏 |
| transferCustomerOwner | PUT | /api/customer/{id}/owner | data: {newOwnerId, reason} | R | CustomerController.transferOwner | 转移客户负责人并写入归属历史 |
| mergeCustomer | POST | /api/customer/{id}/merge | data: {sourceCustomerId, reason} | R\<CustomerMergeResponse\> | CustomerController.mergeCustomer | 合并重复客户，迁移跟进、交易和报价引用 |
| deleteCustomer | DELETE | /api/customer/{id} | 路径参数: id | R | CustomerController.deleteCustomer | 仅允许删除无业务关系客户；存在引用返回 422 RESOURCE_IN_USE |

### 1.4 市场活动模块 (activity-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getActivityList | GET | /api/activities | params: {page, size, ...activityQuery} | R\<PageInfo\<TActivity\>\> | ActivityController.activityPage | 分页查询活动列表 |
| getActivityById | GET | /api/activity/{id} | 路径参数: id | R\<TActivity\> | ActivityController.loadActivity | 获取活动详情 |
| fetchActivityRoi | GET | /api/activity/{id}/roi | 路径参数: id | R\<ActivityRoiResponse\> | ActivityController.activityRoi | 查询活动 ROI |
| createActivity | POST | /api/activity | data: CreateActivityRequest (JSON) | R | ActivityController.addActivity | 新增草稿活动，负责人和状态由后端生成 |
| updateActivity | PUT | /api/activity | data: UpdateActivityRequest (JSON) | R | ActivityController.editActivity | 编辑未锁定活动核心字段 |
| publishActivity | PUT | /api/activity/{id}/publish | 路径参数: id | R\<TActivity\> | ActivityController.publishActivity | 草稿发布为待开始 |
| startActivity | PUT | /api/activity/{id}/start | 路径参数: id | R\<TActivity\> | ActivityController.startActivity | 待开始活动进入进行中 |
| endActivity | PUT | /api/activity/{id}/end | 路径参数: id | R\<TActivity\> | ActivityController.endActivity | 进行中活动进入已结束 |
| reviewActivity | PUT | /api/activity/{id}/review | data: ReviewActivityRequest (JSON) | R\<TActivity\> | ActivityController.reviewActivity | 已结束活动复盘并锁定实际成本和结果 |
| cancelActivity | PUT | /api/activity/{id}/cancel | data: {reason} (JSON) | R\<TActivity\> | ActivityController.cancelActivity | 取消活动，不影响线索或商机状态 |
| closeActivity | PUT | /api/activity/{id}/close | data: {reason} (JSON) | R\<TActivity\> | ActivityController.closeActivity | 关闭活动，不影响历史归因 |
| exportActivities | GET | /api/activity/export | params: activityQuery | Excel Blob | ActivityController.exportActivities | 按权限和数据范围导出活动 ROI |
| deleteActivity | DELETE | /api/activity/{id} | 路径参数: id | R | ActivityController.deleteActivity | 仅删除无引用草稿活动 |
| batchDeleteActivities | POST | /api/activity/batch | data: List\<Integer\> (JSON数组) | R | ActivityController.batchDeleteActivities | 批量删除无引用草稿活动 |

### 1.5 产品管理模块 (product.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getProductList | GET | /api/products | params: {page, size} | Result\<PageInfo\<Product\>\> | ProductController.getProductList | 分页查询产品列表 |
| getProductDetail | GET | /api/products/{id} | 路径参数: id | Result\<Product\> | ProductController.getProductById | 获取产品详情 |
| createProduct | POST | /api/products | data: CreateProductRequest (JSON, 含初始库存) | Result\<Void\> | ProductController.addProduct | 新增产品 |
| updateProduct | PUT | /api/products/{id} | 路径参数: id, data: UpdateProductRequest (JSON, 不含库存数量) | Result\<Void\> | ProductController.updateProduct | 编辑产品资料 |
| deleteProduct | DELETE | /api/products/{id} | 路径参数: id | Result\<Void\> | ProductController.deleteProduct | 删除产品；存在交易、库存流水、促销、客户或线索引用时返回 422 RESOURCE_IN_USE |
| getStockAlerts | GET | /api/products/stockalerts | params: {page, size, sku, name, category} | Result\<PageInfo\<Product\>\> | ProductController.getStockAlerts | 获取库存预警列表 |
| restockProduct | POST | /api/productstock/restock | data: {productId, quantity, remark} (JSON) | Result\<Void\> | ProductStockController.restock | 产品补货 |
| getStockRecords | GET | /api/productstock/records/{id} | 路径参数: id, params: {page, size} | Result\<PageInfo\<ProductStockRecord\>\> | ProductStockController.getStockRecords | 获取库存变动记录 |
| fetchProductVehicles | GET | /api/productstock/vehicles | params: {productId, status, vin, page, size} | Result\<PageInfo\<ProductVehicle\>\> | ProductStockController.getVehicles | 查询库存车辆实例 |
| inboundProductVehicle | POST | /api/productstock/vehicles | data: CreateProductVehicleRequest (JSON) | Result\<ProductVehicle\> | ProductStockController.inboundVehicle | 登记车辆实例入库并写入库存流水 |
| reserveProductVehicle | POST | /api/productstock/vehicles/{vehicleId}/reserve | 路径参数: vehicleId, data: ReserveProductVehicleRequest (JSON) | Result\<ProductVehicle\> | ProductStockController.reserveVehicle | 占用车辆实例并写入占用流水 |
| releaseProductVehicle | POST | /api/productstock/vehicles/{vehicleId}/release | 路径参数: vehicleId, data: ReleaseProductVehicleRequest (JSON) | Result\<ProductVehicle\> | ProductStockController.releaseVehicle | 释放车辆实例占用并关联原占用流水 |
| getPromotionList | GET | /api/product-promotions | params: {page, size} | Result\<PageInfo\<ProductPromotion\>\> | ProductPromotionController.getPromotionList | 分页查询促销列表 |
| createPromotion | POST | /api/product-promotions | data: ProductPromotionRequest (JSON, 不含 status) | Result\<Void\> | ProductPromotionController.addPromotion | 新增促销，服务端默认 DRAFT |
| updatePromotion | PUT | /api/product-promotions/{id} | 路径参数: id, data: ProductPromotionRequest (JSON, 不含 status) | Result\<Void\> | ProductPromotionController.updatePromotion | 编辑促销规则；终态和生效中规则受后端状态校验限制 |
| publishPromotion | PUT | /api/product-promotions/{id}/publish | 路径参数: id | Result\<ProductPromotion\> | ProductPromotionController.publishPromotion | 草稿发布为待生效或生效中 |
| activatePromotion | PUT | /api/product-promotions/{id}/activate | 路径参数: id | Result\<ProductPromotion\> | ProductPromotionController.activatePromotion | 待生效或暂停促销恢复生效 |
| pausePromotion | PUT | /api/product-promotions/{id}/pause | 路径参数: id, data: {reason} | Result\<ProductPromotion\> | ProductPromotionController.pausePromotion | 暂停促销并记录原因 |
| endPromotion | PUT | /api/product-promotions/{id}/end | 路径参数: id, data: {reason} | Result\<ProductPromotion\> | ProductPromotionController.endPromotion | 结束促销并记录原因 |
| voidPromotion | PUT | /api/product-promotions/{id}/void | 路径参数: id, data: {reason} | Result\<ProductPromotion\> | ProductPromotionController.voidPromotion | 作废促销并记录原因 |
| deletePromotion | DELETE | /api/product-promotions/{id} | 路径参数: id | Result\<Void\> | ProductPromotionController.deletePromotion | 仅允许删除未引用草稿；已被报价、订单或使用流水引用时返回 422 RESOURCE_IN_USE |

产品 `status` 请求值必须使用 `ON_SALE` 或 `OFF_SALE`，前端只把“上架/下架”作为展示 label。
促销 `status` 响应值必须使用稳定 code：`DRAFT`、`PENDING_EFFECTIVE`、`ACTIVE`、`PAUSED`、`ENDED`、`VOIDED`、`EXHAUSTED`；前端只展示中文 label，不提交中文状态。
商品删除失败时前端按 `ApiError.code === 422` 映射“已被业务引用，不能直接删除”，不匹配中文 msg 或数据库外键文案。
分类删除失败时前端同样按 `ApiError.code === 422` 映射“已被商品或历史记录引用，不能直接删除”。
| getCategoryList | GET | /api/product-categories | params: {page, size} | Result\<PageInfo\<ProductCategory\>\> | ProductCategoryController.getCategoryList | 分页查询分类列表 |
| createCategory | POST | /api/product-categories | data: ProductCategory对象 (JSON) | Result\<Void\> | ProductCategoryController.addCategory | 新增分类 |
| updateCategory | PUT | /api/product-categories/{id} | 路径参数: id, data: ProductCategory对象 (JSON) | Result\<Void\> | ProductCategoryController.updateCategory | 编辑分类 |
| deleteCategory | DELETE | /api/product-categories/{id} | 路径参数: id | Result\<Void\> | ProductCategoryController.deleteCategory | 删除分类；存在商品或历史引用时返回 422 RESOURCE_IN_USE |

### 1.6 商机管理模块 (opportunity-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchOpportunityPage | GET | /api/opportunities | params: {page, size, customerId?, ownerId?, stage?, keyword?} | R\<PageInfo\<TOpportunity\>\> | OpportunityController.list | 分页查询商机，按服务端当前用户数据范围过滤 |
| fetchOpportunityDetail | GET | /api/opportunities/{id} | 路径参数: id | R\<TOpportunity\> | OpportunityController.detail | 查询商机详情 |
| createOpportunity | POST | /api/opportunities | data: CreateOpportunityRequest (JSON) | R\<TOpportunity\> | OpportunityController.create | 创建独立商机，不创建交易、订单、收款或发票 |
| updateOpportunity | PUT | /api/opportunities/{id} | 路径参数: id, data: UpdateOpportunityRequest (JSON) | R\<TOpportunity\> | OpportunityController.update | 编辑商机自身字段，不提交客户归属、阶段或履约字段 |
| fetchOpportunityStageHistory | GET | /api/opportunities/{id}/stage-history | 路径参数: id | R\<List\<TOpportunityStageHistory\>\> | OpportunityController.stageHistory | 查询阶段历史 |
| advanceOpportunityStage | PUT | /api/opportunities/{id}/stage | 路径参数: id, data: {expectedStage, targetStage, reason, nextActionTime?} | R\<TOpportunity\> | OpportunityController.advanceStage | 使用稳定阶段 code 和 CAS 推进销售阶段并写历史 |
| markOpportunityWon | PUT | /api/opportunities/{id}/won | 路径参数: id, data: {orderTranId, reason, remark?} | R\<TOpportunity\> | OpportunityController.markWon | 赢单必须关联已成立交易，只记录销售结果 |
| markOpportunityLost | PUT | /api/opportunities/{id}/lost | 路径参数: id, data: {reason, competitor?, remark?} | R\<TOpportunity\> | OpportunityController.markLost | 输单必须填写原因并保留结果事实 |
| shelveOpportunity | PUT | /api/opportunities/{id}/shelve | 路径参数: id, data: {reason, nextActionTime, remark?} | R\<TOpportunity\> | OpportunityController.shelve | 搁置必须填写原因和下一步日期 |
| restoreOpportunity | PUT | /api/opportunities/{id}/restore | 路径参数: id, data: {reason, nextActionTime?, remark?} | R\<TOpportunity\> | OpportunityController.restore | 恢复搁置或输单商机，保留原历史事实 |

商机阶段、报价状态、车辆状态、商品状态等业务状态在接口层统一使用稳定英文编码，前端负责展示中文 label，不匹配中文 msg 或中文状态做业务分支。

### 1.7 试驾管理模块 (test-drive-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchTestDrivePage | GET | /api/test-drives | params: {page, size, customerId?, opportunityId?, vehicleId?, ownerId?, status?, keyword?} | R\<PageInfo\<TTestDrive\>\> | TestDriveController.list | 分页查询试驾记录，按服务端当前用户数据范围过滤 |
| fetchTestDriveDetail | GET | /api/test-drives/{id} | 路径参数: id | R\<TTestDrive\> | TestDriveController.detail | 查询试驾详情 |
| fetchTestDriveHistory | GET | /api/test-drives/{id}/history | 路径参数: id | R\<List\<TTestDriveStatusHistory\>\> | TestDriveController.history | 查询状态历史 |
| createTestDrive | POST | /api/test-drives | data: CreateTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.create | 创建试驾预约，校验客户、商机、车辆和时间段冲突，写入车辆时间占用 |
| rescheduleTestDrive | PUT | /api/test-drives/{id}/reschedule | 路径参数: id, data: RescheduleTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.reschedule | 先校验新时段，再释放原占用并创建新占用 |
| cancelTestDrive | PUT | /api/test-drives/{id}/cancel | 路径参数: id, data: CancelTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.cancel | 取消试驾必须填写原因并释放时间占用 |
| markTestDriveNoShow | PUT | /api/test-drives/{id}/no-show | 路径参数: id, data: CancelTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.noShow | 标记客户爽约，不伪装为完成 |
| checkInTestDrive | PUT | /api/test-drives/{id}/check-in | 路径参数: id, data: CheckInTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.checkIn | 记录到店时间、签到人和客户确认方式 |
| completeTestDrive | PUT | /api/test-drives/{id}/complete | 路径参数: id, data: CompleteTestDriveRequest (JSON) | R\<TTestDrive\> | TestDriveController.complete | 完成试驾必须已签到和安全确认，记录反馈，不自动创建报价或订单 |

试驾状态、商机阶段、报价状态、车辆状态等业务状态在接口层统一使用稳定英文编码，前端负责展示中文 label，不匹配中文 msg 或中文状态做业务分支。

### 1.8 跟进任务模块 (follow-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchFollowTaskPage | GET | /api/follow-tasks | params: {page, size, status?, taskType?, relatedObjectType?, relatedObjectId?, ownerId?, overdueOnly?, keyword?} | R\<PageInfo\<TFollowTask\>\> | FollowTaskController.list | 分页查询跟进任务，按当前用户数据范围过滤并维护逾期状态 |
| createFollowTask | POST | /api/follow-tasks | data: CreateFollowTaskRequest (JSON) | R\<TFollowTask\> | FollowTaskController.create | 创建独立跟进任务，服务端校验关联对象可见性和负责人有效性 |
| fetchFollowTaskDetail | GET | /api/follow-tasks/{id} | 路径参数: id | R\<TFollowTask\> | FollowTaskController.detail | 查询跟进任务详情，按服务端数据范围过滤 |
| startFollowTask | PUT | /api/follow-tasks/{id}/start | 路径参数: id | R\<TFollowTask\> | FollowTaskController.start | 将未终态任务标记为 IN_PROGRESS |
| postponeFollowTask | PUT | /api/follow-tasks/{id}/postpone | 路径参数: id, data: {newPlanTime, reason} | R\<TFollowTask\> | FollowTaskController.postpone | 延期任务并保留原计划时间和原因 |
| cancelFollowTask | PUT | /api/follow-tasks/{id}/cancel | 路径参数: id, data: {reason} | R\<TFollowTask\> | FollowTaskController.cancel | 取消非终态任务并保留取消原因 |
| completeFollowTask | PUT | /api/follow-tasks/{id}/complete | 路径参数: id, data: CompleteFollowTaskRequest (JSON) | R\<TFollowTask\> | FollowTaskController.complete | 完成任务，同事务写沟通记录并回写最近跟进事实 |
| fetchCommunicationRecordPage | GET | /api/communication-records | params: {page, size, relatedObjectType?, relatedObjectId?, followTaskId?, ownerId?, method?, status?} | R\<PageInfo\<TCommunicationRecord\>\> | CommunicationRecordController.list | 分页查询沟通记录，按当前用户数据范围过滤 |
| createCommunicationRecord | POST | /api/communication-records | data: CreateCommunicationRecordRequest (JSON) | R\<TCommunicationRecord\> | CommunicationRecordController.create | 创建沟通记录，可关联同一对象下的跟进任务 |
| correctCommunicationRecord | PUT | /api/communication-records/{id}/correct | 路径参数: id, data: CorrectCommunicationRecordRequest (JSON) | R\<TCommunicationRecord\> | CommunicationRecordController.correct | 原记录置为 CORRECTED 并插入新 ACTIVE 记录 |
| voidCommunicationRecord | PUT | /api/communication-records/{id}/void | 路径参数: id, data: {reason} | R\<TCommunicationRecord\> | CommunicationRecordController.voidRecord | 沟通记录作废为 VOIDED，不物理删除 |

跟进任务状态、任务类型、沟通方式和沟通记录状态均使用稳定英文编码，前端只做中文展示映射，不匹配中文 msg 做业务分支。

### 1.9 报价订单模块 (quote-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchQuotePage | GET | /api/quotes | params: {page, size, quoteNo, customerId, status} | Result\<PageInfo\<Quote\>\> | QuoteController.list | 分页查询报价列表，按客户数据范围过滤 |
| fetchQuoteDetail | GET | /api/quotes/{id} | 路径参数: id | Result\<QuoteDetail\> | QuoteController.detail | 获取报价、当前版本和版本行项 |
| createQuote | POST | /api/quotes | data: CreateQuoteRequest (JSON) | Result\<QuoteDetail\> | QuoteController.create | 创建报价并保存商品快照，不扣减库存；`opportunityId` 可选，提交时必须与客户一致、在数据范围内且未终态 |
| fetchQuoteVersions | GET | /api/quotes/{id}/versions | 路径参数: id | Result\<List\<QuoteVersion\>\> | QuoteController.versions | 获取报价版本列表 |
| createQuoteVersion | POST | /api/quotes/{id}/versions | 路径参数: id, data: CreateQuoteVersionRequest (JSON) | Result\<QuoteDetail\> | QuoteController.createVersion | 创建或覆盖报价版本并重置草稿 |
| updateQuoteStatus | PUT | /api/quotes/{id}/status | 路径参数: id, data: UpdateQuoteStatusRequest (JSON) | Result\<QuoteDetail\> | QuoteController.updateStatus | 使用 expectedStatus 和 targetStatus 稳定编码执行 CAS 状态迁移 |

报价状态、车辆状态、商品状态等业务状态在接口层统一使用稳定英文编码，前端负责展示中文 label，不匹配中文 msg 或中文状态做业务分支。

### 1.10 交易管理模块 (tran.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getTranList | GET | /api/tran/list | params: {page, size, ...query} | R\<PageInfo\<TTran\>\> | TranController.list | 分页查询交易列表 |
| getTranDetail | GET | /api/tran/{id} | 路径参数: id | R\<TTran\> | TranController.detail | 获取交易详情 |
| getTranProducts | GET | /api/tran/products/{id} | 路径参数: id | R\<List\<TTranProduct\>\> | TranController.getTransactionProducts | 获取交易产品历史快照列表；商品主档修改后名称、编码、配置和指导价不随之变化 |
| createTran | POST | /api/transactions | data: CreateTranRequest对象 (JSON) | R\<Integer\> | TranController.create | 创建交易 |
| updateTran | PUT | /api/tran/update | data: TranCreateRequest对象 (JSON) | R\<Boolean\> | TranController.update | 更新交易 |
| fetchSettlementPreview | POST | /api/tran/{id}/settlement-preview | 路径参数: id, data: {promotionId?} | R\<SettlementPreviewResponse\> | TranController.settlementPreview | 服务端结算预览 |
| settleTran | PUT | /api/tran/{id}/settle | 路径参数: id, data: {promotionId?, expectedVersion, pricingFingerprint} | R\<SettlementPreviewResponse\> | TranController.settle | CAS 确认结算 |
| approveTran | PUT | /api/tran/approve/{id} | 路径参数: id, data: {approved, comment} (JSON) | R\<Boolean\> | TranController.approve | 审批交易 |
| getTranApprove | GET | /api/tran/approve/info/{tranId} | 路径参数: tranId | R\<TTranApprove\> | TranController.getApproveInfo | 获取审批信息 |
| createInvoice | POST | /api/tran/invoice | data: CreateTranInvoiceRequest (JSON) | R\<Boolean\> | TranController.createInvoice | 创建待开具发票；支持部分开票和多票；超额返回 409 和 availableAmount |
| getTranInvoiceList | GET | /api/tran/invoice/{tranId} | 路径参数: tranId | R\<List\<TTranInvoice\>\> | TranController.getInvoiceList | 获取交易发票列表；无敏感权限时后端脱敏 |
| updateInvoiceStatus | PUT | /api/tran/invoice/{invoiceId}/status | 路径参数: invoiceId, data: {status, reason?} (JSON) | R\<Boolean\> | TranController.updateInvoiceStatus | 标记已开具、失败或作废；失败/作废必须有原因；ISSUED 后触发交易完成聚合 |
| redReverseInvoice | POST | /api/tran/invoice/{invoiceId}/red-reversal | 路径参数: invoiceId, data: {amount, reason} (JSON) | R\<TTranInvoice\> | TranController.redReverseInvoice | 创建负数红字发票并保留原票；超额返回 409 和 availableAmount |
| reissueInvoice | POST | /api/tran/invoice/{invoiceId}/reissue | 路径参数: invoiceId, data: ReissueInvoiceRequest (JSON) | R\<TTranInvoice\> | TranController.reissueInvoice | 基于作废或红冲事实重开发票；超额返回 409 和 availableAmount |
| recordPayment | POST | /api/tran/payment | data: CreatePaymentRequest (JSON) | R\<TPayment\> | TranController.recordPayment | 登记待确认收款；金额由服务端按剩余应收计算 |
| confirmPayment | PUT | /api/tran/payment/{id}/confirm | 路径参数: id, data: ConfirmPaymentRequest (JSON) | R\<TPayment\> | TranController.confirmPayment | 财务确认或退回待确认收款；确认到账后只触发交易完成聚合，不由收款单独完成交易 |
| fetchTranPayments | GET | /api/tran/payment/{tranId} | 路径参数: tranId | R\<List\<TPayment\>\> | TranController.getPayments | 查询交易收款和退款流水 |
| fetchTranRefundRequests | GET | /api/tran/refund-requests/{tranId} | 路径参数: tranId | R\<List\<TRefundRequest\>\> | TranController.getRefundRequests | 查询交易退款申请 |
| createRefundRequest | POST | /api/tran/payment/{id}/refund-requests | 路径参数: 原收款 id, data: CreateRefundRequest (JSON) | R\<TRefundRequest\> | TranController.createRefundRequest | 基于已确认原收款创建待审批退款申请；支持 PAYMENT、DELIVERY、CANCELLED 交易，超额返回 409 和 availableAmount |
| approveRefundRequest | PUT | /api/tran/refund-requests/{id}/approve | 路径参数: id, data: ApproveRefundRequest (JSON) | R\<TRefundRequest\> | TranController.approveRefundRequest | 审批通过进入待执行，驳回保留原因 |
| executeRefundRequest | POST | /api/tran/refund-requests/{id}/execute | 路径参数: id, data: ExecuteRefundRequest (JSON) | R\<TRefundRequest\> | TranController.executeRefundRequest | 记录退款执行成功或失败；成功新增负数退款流水；不直接取消交易或释放库存 |
| cancelTran | PUT | /api/tran/{id}/cancel | 路径参数: id, data: {reason} (JSON) | R\<Boolean\> | TranController.cancel | 取消交易；存在收款、发票、退款中、已出库或已签收事实时返回冲突；未出库订单占用会写 RELEASE 流水并恢复车辆/商品可售库存，保留历史事实并写入原因 |
| closeTran | PUT | /api/tran/{id}/close | 路径参数: id, data: {reason} (JSON) | R\<Boolean\> | TranController.close | 关闭交易；保留历史事实并写入原因 |
| getTranStatus | GET | /api/tran/status/{id} | 路径参数: id | - | **后端未实现** | 前端调用但后端无对应接口 |

### 1.11 交付管理模块 (delivery-api.ts)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchDeliveryPage | GET | /api/deliveries | params: {page, size, tranId?, customerId?, vehicleId?, responsibleUserId?, status?} | R\<PageInfo\<TDelivery\>\> | DeliveryController.list | 分页查询交付记录，按当前用户交易客户数据范围过滤 |
| createDelivery | POST | /api/deliveries | data: CreateDeliveryRequest (JSON) | R\<TDelivery\> | DeliveryController.create | 创建交付记录和准备清单；交易必须处于 DELIVERY，车辆必须为当前交易 ORDER_RESERVED |
| fetchDeliveryDetail | GET | /api/deliveries/{id} | 路径参数: id | R\<TDelivery\> | DeliveryController.detail | 查询交付详情 |
| fetchDeliveryCheckItems | GET | /api/deliveries/{id}/check-items | 路径参数: id | R\<List\<TDeliveryCheckItem\>\> | DeliveryController.checkItems | 查询交付准备清单 |
| fetchDeliveriesByTranId | GET | /api/deliveries/tran/{tranId} | 路径参数: tranId | R\<List\<TDelivery\>\> | DeliveryController.listByTranId | 查询交易下交付记录 |
| updateDeliveryCheckItem | PUT | /api/deliveries/check-items/{itemId} | 路径参数: itemId, data: {status, remark?} | R\<TDeliveryCheckItem\> | DeliveryController.updateCheckItem | 更新准备项状态，状态使用稳定编码 PENDING/COMPLETED/BLOCKED |
| signDelivery | POST | /api/deliveries/{id}/sign | 路径参数: id, data: SignDeliveryRequest (JSON) | R\<TDelivery\> | DeliveryController.sign | 客户签收并联动库存出库；签收后触发交易完成聚合，不由交付单独完成交易 |
| markDeliveryException | POST | /api/deliveries/{id}/exception | 路径参数: id, data: {exceptionType, reason} | R\<TDelivery\> | DeliveryController.markException | 登记交付异常并保留历史 |
| cancelDelivery | POST | /api/deliveries/{id}/cancel | 路径参数: id, data: {reason} | R\<TDelivery\> | DeliveryController.cancel | 取消未签收交付并保留历史；同一事务内引用原订单占用流水写 RELEASE 流水、恢复车辆和商品可售库存，重复释放不重复加库存 |

### 1.12 字典管理模块 (dict.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getDictTypeList | GET | /api/dict/types | params: {page, size, ...query} | R | DicController.getDicTypes | 分页查询字典类型 |
| getDictTypeDetail | GET | /api/dict/type/get/{id} | 路径参数: id | R | DicController.getDicTypeById | 获取字典类型详情 |
| createDictType | POST | /api/dict/type/create | data: CreateDicTypeRequest (JSON) | R | DicController.addDicType | 新增字典类型，默认启用且非内置 |
| updateDictType | PUT | /api/dict/type/update/{id} | 路径参数: id, data: UpdateDicTypeRequest (JSON) | R | DicController.updateDicType | 编辑字典类型，typeCode 不可改，停用需原因 |
| deleteDictType | DELETE | /api/dict/type/delete/{id} | 路径参数: id | R | DicController.deleteDicType | 删除未被引用且非内置的字典类型；引用返回 422 |
| batchDeleteDictTypes | DELETE | /api/dict/types/batch | data: List\<Integer\> (JSON数组) | R | DicController.batchDeleteDicTypes | 批量删除未被引用且非内置的字典类型 |
| getDictValueList | GET | /api/dict/values | params: {page, size, ...query} | R | DicController.getDicValues | 分页查询字典值 |
| getDictValueDetail | GET | /api/dict/value/get/{id} | 路径参数: id | R | DicController.getDicValueById | 获取字典值详情 |
| createDictValue | POST | /api/dict/value/create | data: CreateDicValueRequest (JSON) | R | DicController.addDicValue | 新增字典值，默认启用且非内置 |
| updateDictValue | PUT | /api/dict/value/update/{id} | 路径参数: id, data: UpdateDicValueRequest (JSON) | R | DicController.updateDicValue | 编辑字典值，typeCode/valueCode 不可改，停用需原因 |
| deleteDictValue | DELETE | /api/dict/value/delete/{id} | 路径参数: id | R | DicController.deleteDicValue | 删除未被引用且非内置的字典值；引用返回 422 |
| batchDeleteDictValues | DELETE | /api/dict/value/batch | data: List\<Integer\> (JSON数组) | R | DicController.batchDeleteDicValues | 批量删除未被引用且非内置的字典值 |
| clearCache | GET | /api/dict/clear | params: {forceRefresh: true} | R | DicController.clearCache | 清除字典缓存 |

### 1.13 审计日志模块

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| fetchLoginLogPage | GET | /api/audit/login-logs | params: {page, size, loginAct?, userName?, result?, reasonCode?, ip?, requestId?, startTime?, endTime?} | R\<PageInfo\<TLoginLog\>\> | AuditLogController.listLoginLogs | 分页查询登录记录，后端按 `audit:login:list` 校验权限 |
| fetchLoginLogDetail | GET | /api/audit/login-logs/{id} | 路径参数: id | R\<TLoginLog\> | AuditLogController.getLoginLog | 查询登录记录详情 |
| exportLoginLogs | GET | /api/audit/login-logs/export | 同登录记录过滤参数 | text/csv | AuditLogController.exportLoginLogs | 导出 UTF-8 CSV，并写 `AUDIT_LOGIN_EXPORT` 操作审计 |
| fetchOperationLogPage | GET | /api/audit/operation-logs | params: {page, size, userName?, actionCode?, moduleName?, objectType?, resourceId?, result?, ip?, requestId?, startTime?, endTime?} | R\<PageInfo\<TOperationLog\>\> | AuditLogController.listOperationLogs | 分页查询操作记录，后端按 `audit:operation:list` 校验权限 |
| fetchOperationLogDetail | GET | /api/audit/operation-logs/{id} | 路径参数: id | R\<TOperationLog\> | AuditLogController.getOperationLog | 查询操作记录详情 |
| exportOperationLogs | GET | /api/audit/operation-logs/export | 同操作记录过滤参数 | text/csv | AuditLogController.exportOperationLogs | 导出 UTF-8 CSV，并写 `AUDIT_OPERATION_EXPORT` 操作审计 |

旧系统配置与系统监控接口已下线，不再提供 `/api/system/*` 和 `/api/monitor/*`。

### 1.14 统计模块

| 后端接口路径 | HTTP方法 | 后端Controller方法 | 响应数据格式 | 前端调用情况 |
|-------------|---------|-------------------|-------------|-------------|
| /api/summary/data | GET | StatisticController.summaryData | R\<SummaryData\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |
| /api/saleFunnel/data | GET | StatisticController.saleFunnelData | R\<List\<NameValue\>\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |
| /api/sourcePie/data | GET | StatisticController.sourcePieData | R\<List\<NameValue\>\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |

统计接口不接收前端范围参数，后端按当前登录用户的数据范围聚合；非管理员统计结果必须能由同范围明细反算。

### 1.15 活动备注模块 (后端提供，前端未定义API文件)

| 后端接口路径 | HTTP方法 | 后端Controller方法 | 响应数据格式 | 前端调用情况 |
|-------------|---------|-------------------|-------------|-------------|
| /api/activity/remark | POST | ActivityRemarkController.addActivityRemark | R | `createActivityRemark` |
| /api/activity/remark | GET | ActivityRemarkController.activityRemarkPage | R\<PageInfo\<TActivityRemark\>\> | `fetchActivityRemarkPage` |
| /api/activity/remark/{id} | GET | ActivityRemarkController.activityRemarkPage | R\<TActivityRemark\> | **前端未发现API调用** |
| /api/activity/remark | PUT | ActivityRemarkController.editActivityRemark | R | **前端未发现API调用** |
| /api/activity/remark/{id} | DELETE | ActivityRemarkController.delActivityRemark | R | `deleteActivityRemark` |

---

## 2. 联调问题检查

### 2.1 路径匹配问题

| 问题类型 | 前端路径 | 后端路径 | 问题描述 | 严重程度 |
|---------|---------|---------|---------|---------|
| **路径不匹配** | /api/tran/status/{id} | 无对应接口 | 前端调用获取交易状态接口，后端未实现 | **高** |

### 2.2 HTTP方法匹配问题

| 前端函数 | 前端HTTP方法 | 后端HTTP方法 | 问题描述 | 严重程度 |
|---------|-------------|-------------|---------|---------|
| batchDeleteCluesByIds | POST | POST | 一致，但批量删除通常使用DELETE | 低 |
| batchDeleteActivities | POST | POST | 一致，但批量删除通常使用DELETE | 低 |
| batchDeleteDictTypes | DELETE | DELETE | 一致 | - |
| batchDeleteDictValues | DELETE | DELETE | 一致 | - |

### 2.3 请求参数格式匹配问题

| 前端函数 | 前端发送格式 | 后端期望格式 | 问题描述 | 严重程度 |
|---------|-------------|-------------|---------|---------|
| createActivity | JSON | JSON (@RequestBody CreateActivityRequest) | 一致 | - |
| updateActivity | JSON | JSON (@RequestBody UpdateActivityRequest) | 一致 | - |
| addClue | FormData | FormData (无@RequestBody) | 一致 | - |
| updateClue | FormData | FormData (无@RequestBody) | 一致 | - |
| createUser | JSON | JSON (@RequestBody CreateUserRequest) | 一致 | - |
| updateUser | JSON | JSON (@RequestBody UpdateUserRequest) | 一致 | - |
| createProduct | JSON | JSON (@RequestBody) | 一致 | - |
| updateProduct | JSON | JSON (@RequestBody) | 一致 | - |
| batchDeleteCluesByIds | JSON数组 | JSON数组 (@RequestBody) | 一致 | - |
| convertClueToCustomer | JSON | JSON (@RequestBody) | 一致 | - |

### 2.4 响应字段名匹配问题

| 模块 | 后端响应类 | 字段名 | 前端读取方式 | 问题描述 | 严重程度 |
|-----|-----------|-------|-------------|---------|---------|
| 用户/线索/活动/字典/交易/系统 | R | code, msg, data | response.data.code/msg/data | 一致 | - |
| 产品/分类/促销/库存 | Result | code, msg, data | response.data.code/msg/data | 一致 | - |
| Token错误 | R | code(510-513), msg | `ApiError.sessionInvalid` | 前端以 HTTP 401 判定会话失效；HTTP 403 只表示权限不足，不清理会话 | - |

### 2.5 前端调用但后端不存在的接口

| 前端函数 | 前端调用路径 | 问题描述 | 严重程度 |
|---------|-------------|---------|---------|
| getTranStatus | GET /api/tran/status/{id} | 后端TranController中无此接口 | **高** |

### 2.6 后端提供但前端未调用的接口

| 后端接口 | 后端Controller | 说明 | 严重程度 |
|---------|---------------|------|---------|
| POST /api/activity/remark | ActivityRemarkController | 添加活动备注 | 中 |
| GET /api/activity/remark | ActivityRemarkController | 查询活动备注列表 | 中 |
| GET /api/activity/remark/{id} | ActivityRemarkController | 获取活动备注详情 | 中 |
| PUT /api/activity/remark | ActivityRemarkController | 编辑活动备注 | 中 |
| DELETE /api/activity/remark/{id} | ActivityRemarkController | 删除活动备注 | 中 |
| GET /api/customers | CustomerController | 客户分页查询(另一个接口) | 低 |
| GET /api/customer/{id} | CustomerController | 获取客户详情 | 低 |
| GET /api/dict/refresh | DicController | 刷新字典数据 | 低 |
| GET /api/login/free | UserController | 免登录接口 | 低 |
| GET /api/exportExcel | CustomerController | 导出Excel | 中 |
| GET /api/product-categories/{id} | ProductCategoryController | 获取分类详情 | 低 |
| GET /api/product-promotions/{id} | ProductPromotionController | 获取促销详情 | 低 |

---

## 3. 认证流程链路

### 3.1 登录流程

```
┌─────────────┐     POST /api/login      ┌─────────────────┐
│   前端登录   │  ──────────────────────►  │  Spring Security │
│   页面       │  {loginAct, loginPwd,    │  框架处理         │
│              │   rememberMe}            │                  │
└─────────────┘                           └─────────────────┘
                                                │
                                                ▼
                                    ┌───────────────────────┐
                                    │ MyAuthenticationSuccess│
                                    │ Handler                │
                                    └───────────────────────┘
                                                │
                            ┌───────────────────┼───────────────────┐
                            ▼                   ▼                   ▼
                    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
                    │ 1.生成JWT    │   │ 2.存入Redis  │   │ 3.设置过期   │
                    │ userId、     │   │ key:         │   │ 记住我:7天   │
                    │ loginAct、   │   │ cdrm:user:   │   │ 否则:4小时   │
                    │ authVersion  │   │ login:{id}   │   │              │
                    └──────────────┘   └──────────────┘   └──────────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │ 返回R对象    │
                    │ {code:200,   │
                    │  msg:"操作   │
                    │  成功",      │
                    │  data:jwt}   │
                    └──────────────┘
```

### 3.2 Token签发与存储

| 步骤 | 操作 | 说明 |
|-----|------|------|
| 1 | 用户提交登录表单 | POST /api/login, 参数: loginAct, loginPwd, rememberMe |
| 2 | Spring Security认证 | 框架自动处理用户名密码验证 |
| 3 | 认证成功处理器 | MyAuthenticationSuccessHandler.onAuthenticationSuccess() |
| 4 | 创建会话事实 | 生成不可猜测 `sessionId`，写入 `t_user_session` 的设备摘要、空闲期限、绝对期限和当前 `authVersion` |
| 5 | 生成JWT | `JWTUtils.createSessionJWT(...)`，只写入 `userId`、`sessionId`、`authVersion`、`iat`、`exp` |
| 6 | 存储到Redis | key: `cdrm:session:{sessionId}`，value 为 JWT HMAC 摘要；同时维护 `cdrm:user:sessions:{userId}` 索引 |
| 7 | 设置期限 | rememberMe=true: 7天绝对期限、24小时空闲期限；否则 4小时绝对期限、30分钟空闲期限 |
| 8 | 写入登录审计 | 数据库与 Redis 成功后记录登录；审计失败时撤销刚创建的会话 |
| 9 | 返回JWT给前端 | 会话事实、Redis 和登录审计全部成功后才返回 R.OK(jwt)；失败不返回 JWT |

### 3.3 请求携带Token

```javascript
// 前端请求拦截器 (httpRequest.js:47-63)
axios.interceptors.request.use((config) => {
    // 1. 优先从sessionStorage获取token
    let token = window.sessionStorage.getItem("dlyk_token");
    
    // 2. 如果sessionStorage没有，从localStorage获取
    if (!token) {
        token = window.localStorage.getItem("dlyk_token");
        if (token) {
            config.headers['rememberMe'] = true;  // 标记为记住我
        }
    }
    
    // 3. 将token放入请求头
    if (token) {
        config.headers['Authorization'] = `Bearer ${token}`;
    }
    
    return config;
});
```

### 3.4 Token校验流程

1. `TokenVerifyFilter` 只从 `Authorization: Bearer <token>` 读取 JWT；缺失、格式错误或签名无效均返回 HTTP 401。
2. 解析 JWT 中的 `userId`、`sessionId` 和 `authVersion`；`cdrm:session:{sessionId}` 中的 HMAC 摘要必须与请求 Token 匹配。
3. 查询 `t_user_session`，校验会话属于该用户、未撤销、未超过空闲/绝对期限且签发版本一致。
4. 根据 `userId` 重新加载数据库用户，检查账号启用和未锁定，并分别以 `account_expires_at`、`password_expires_at` 动态判断账号与凭证是否过期；兼容布尔字段不能覆盖时间事实。
5. 将 JWT `authVersion` 与数据库当前版本比较。数据库是认证版本的权威来源；版本不一致返回 HTTP 401。
6. 缺少 `sessionId` 的旧 JWT 仅在显式兼容截止时间前按旧 Redis 精确键校验；默认禁用，截止后统一返回 HTTP 401。
7. 全部校验通过后按节流窗口更新最后活动时间和 Redis 空闲 TTL，再设置 `SecurityContext`；会话基础设施异常按 HTTP 401 失败关闭。

### 3.5 Token有效期与认证版本

| 场景 | 数据库处理 | Redis处理 | 结果 |
|-----|-----------|-----------|------|
| 登录且 rememberMe=true | 写入独立会话事实和当前 authVersion | 会话摘要空闲 TTL 24 小时，绝对期限 7 天 | 数据库、Redis 和审计都成功后才返回 JWT |
| 登录且 rememberMe=false | 写入独立会话事实和当前 authVersion | 会话摘要空闲 TTL 30 分钟，绝对期限 4 小时 | 数据库、Redis 和审计都成功后才返回 JWT |
| 普通登出 | 只撤销当前 sessionId，不递增 authVersion | 精确删除当前会话 Key 和索引成员 | 其他设备保持登录 |
| 密码、账号、角色、权限或任职安全事实变化 | 递增 authVersion 并撤销全部活动会话事实 | 提交后按已知 sessionId 精确清理 | 数据库提交后旧 Token 全部失效 |
| 旧 JWT 无 sessionId | 仅显式兼容截止时间前允许 | 仍要求旧 Redis 精确键匹配 | 默认禁用，截止后即 HTTP 401 |

### 3.6 Token过期/错误处理

前端 `shared/api/http-client.ts` 以 HTTP 状态区分认证与授权：HTTP 401 将 `ApiError.sessionInvalid` 标记为 `true`，由认证流程处理本地会话失效；HTTP 403 明确标记为非会话失效，只提示当前账号权限不足，不清理 Token。不能再用业务码 `>= 500` 同时覆盖认证失败、权限不足和普通系统错误。

### 3.7 后端Token错误码

| 错误码 | 常量名 | 含义 | 前端处理 |
|-------|-------|------|---------|
| 510 | TOKEN_IS_EMPTY | Token为空 | 按 HTTP 401 处理会话失效 |
| 511 | TOKEN_IS_ERROR | Token格式、签名、账号状态或认证版本无效 | 按 HTTP 401 处理会话失效 |
| 512 | TOKEN_IS_EXPIRED | Redis中Token不存在 | 按 HTTP 401 处理会话失效 |
| 513 | TOKEN_IS_NONE_MATCH | Token与Redis中不一致 | 按 HTTP 401 处理会话失效 |
| 631 | SESSION_NOT_FOUND | 会话不存在或无权访问 | HTTP 404，不泄露其他用户会话 |
| 632 | SESSION_REVOKED | 会话已撤销 | HTTP 409，刷新会话列表 |
| 633 | SESSION_EXPIRED | 会话已过期 | HTTP 410，刷新会话列表 |
| 634 | SESSION_VERSION_CONFLICT | sessionRevision 冲突 | HTTP 409，刷新后重试 |
| 635 | SESSION_CACHE_FAILED | 会话缓存精确清理失败 | HTTP 503；数据库撤销事实保持有效 |

### 3.8 退出登录流程

1. 前端携带当前 Bearer Token 调用 `POST /api/logout`。
2. `MyLogoutSuccessHandler` 从认证详情取得当前 `sessionId`，只撤销这一条数据库会话事实，不递增 `authVersion`。
3. 数据库提交后精确删除该会话 Redis Key 和用户会话索引成员；不扫描其他 Key。
4. 删除失败返回 HTTP 503 和 `SESSION_CACHE_FAILED`；数据库撤销事实仍使当前 Token 失效，其他设备会话不受影响。
5. 前端收到成功响应后清理本地 Token 与权限缓存。

本人通过 `/api/me/sessions` 查看设备，使用 `/api/me/sessions/{sid}/revoke`、`/revoke-others`、`/revoke-all` 撤销会话；管理者通过 `/api/users/{id}/sessions` 及其撤销子路径操作下属，并同时受管理链、组织范围与 `SESSION_VIEW`/`SESSION_REVOKE` 动作约束。

---

## 4. 文件上传下载流程

### 4.1 Excel导入流程 (线索导入)

```
┌─────────────┐     POST /api/importExcel     ┌─────────────────┐
│   前端       │  ────────────────────────────►  │ ClueController  │
│ importExcelAPI│  Content-Type: multipart/     │ .importExcel()  │
│              │  form-data                     │                 │
│              │  body: FormData {file: File}   │                 │
└─────────────┘                                └─────────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ MultipartFile │
                                                │ file          │
                                                └───────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ clueService.  │
                                                │ importExcel(  │
                                                │ file.getInput │
                                                │ Stream(),     │
                                                │ token)        │
                                                └───────────────┘
```

| 项目 | 说明 |
|-----|------|
| 前端函数 | importExcelAPI(file) |
| HTTP方法 | POST |
| 请求路径 | /api/importExcel |
| Content-Type | multipart/form-data |
| 请求参数 | FormData对象，包含file字段 |
| 后端接收 | MultipartFile file |
| 权限要求 | @PreAuthorize("hasAuthority('clue:import')") |
| Token传递 | 通过 `Authorization: Bearer <token>` 请求头 |
| 响应语义 | 全部成功返回 200；存在失败行返回 422，`data.importedCount` 表示已成功导入行数，`data.errors` 表示行级错误 |

### 4.2 Excel导出流程 (客户导出)

```
┌─────────────┐     GET /api/exportExcel      ┌─────────────────┐
│   前端       │  ────────────────────────────►  │ CustomerController│
│              │  params: {ids: "1,2,3"}        │ .exportExcel()  │
│              │  header: Authorization Bearer   │                 │
└─────────────┘                                └─────────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ 设置响应头     │
                                                │ Content-Type: │
                                                │ application/  │
                                                │ octet-stream  │
                                                └───────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ 查询客户数据   │
                                                │ customerService│
                                                │ .getCustomer  │
                                                │ ByExcel()     │
                                                └───────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ EasyExcel写入 │
                                                │ 输出流        │
                                                └───────────────┘
```

| 项目 | 说明 |
|-----|------|
| HTTP方法 | GET |
| 请求路径 | /api/exportExcel |
| 请求参数 | ids(可选，逗号分隔的ID) |
| 认证方式 | 通过 `Authorization: Bearer <token>` 请求头传递 Token |
| 响应类型 | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| 响应头 | Content-disposition: attachment; filename*=UTF-8''<URL编码文件名>.xlsx |
| 权限要求 | @PreAuthorize("hasAuthority('customer:export')") |
| 前端实现 | `httpClient.download()` 以 Blob 接收，解析文件名后 `saveBlob` 触发下载 |

### 4.3 Token在文件操作中的处理

Token 统一通过 `Authorization: Bearer <token>` 请求头传递，包括文件下载。`TokenVerifyFilter` 只从请求头读取 Bearer Token，不再从 URL 参数或裸 Header 获取。前端 `httpClient.download()` 复用 `axiosClient` 的请求拦截器自动注入 Token，不再将 Token 放入 URL。

---

## 5. 分页参数传递方式

### 5.1 前端分页参数传递

| 前端函数 | 参数名 | 传递方式 | 示例 |
|---------|-------|---------|------|
| getUserList | page, size | Query参数 | /api/users?page=1&size=10 |
| getCurrentClues | page, size | Query参数 | /api/clues?page=1&size=10 |
| getActivityList | page, size | Query参数 | /api/activities?page=1&size=10 |
| getCustomerList | page, size | Query参数 | /api/customers?page=1&size=10 |
| getProductList | page, size | Query参数 | /api/products?page=1&size=10 |
| getTranList | page, size | Query参数 | /api/tran/list?page=1&size=10 |
| getDictTypeList | page, size | Query参数 | /api/dict/types?page=1&size=10 |
| getClueRemarkList | page, size | Query参数 | /api/clue/remark?page=1&size=10&clueId=1 |

### 5.2 后端分页参数接收

| 后端Controller | 参数名 | 接收方式 | 默认值 | 分页实现 |
|---------------|-------|---------|-------|---------|
| UserController.userPage | page, size | @RequestParam | 1, 10 | PageHelper |
| ClueController.cluePage | page, size | @RequestParam | 1, 10 | PageHelper |
| ActivityController.activityPage | page, size | @RequestParam | 1, 10 | PageHelper |
| CustomerController.list | page, size | @RequestParam | 1, 10 | PageHelper |
| ProductController.getProductList | page, size | @RequestParam | 1, 10 | PageHelper |
| TranController.list | page, size | @RequestParam | 1, 10 | PageHelper |
| DicController.getDicTypes | page, size | DicQuery对象 | 1, 10 | PageHelper |
| ClueRemarkController.clueRemarkPage | page, size | @RequestParam | 1, 10 | PageHelper |

### 5.3 分页参数不一致问题

| 模块 | 前端参数 | 后端参数 | 问题描述 | 严重程度 |
|-----|---------|---------|---------|---------|
| 用户/线索/活动 | page, size | page, size | 一致 | - |
| 客户/产品/交易 | page, size | page, size | 一致 | - |
| 字典 | page, size | page, size (通过DicQuery) | 一致 | - |

### 5.4 响应分页数据格式

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,          // 当前页码
    "pageSize": 10,        // 每页大小
    "size": 10,            // 当前页记录数
    "total": 100,          // 总记录数
    "pages": 10,           // 总页数
    "list": [...]          // 数据列表
  }
}
```

---

## 6. 错误处理流程

### 6.1 后端错误码定义 (CodeEnum)

| 错误码 | 常量名 | 含义 | 使用场景 |
|-------|-------|------|---------|
| 200 | OK | 操作成功 | 所有成功响应 |
| 500 | FAIL | 操作失败 | 业务逻辑失败 |
| 501 | PARAM_ERROR | 请求参数格式有误 | 参数校验失败 |
| 502 | LOGIN_ERROR | 登录失败 | 用户名密码错误 |
| 503 | UNAUTHORIZED_ERROR | 没有访问权限 | 权限不足 |
| 504 | TOKEN_ERROR | Token无效 | Token验证失败 |
| 505 | TOKEN_EXPIRED | Token已过期 | Token过期 |
| 506 | SYSTEM_ERROR | 系统异常 | 系统内部错误 |
| 510 | TOKEN_IS_EMPTY | Token为空 | 请求未携带Token |
| 511 | TOKEN_IS_ERROR | Token无效 | JWT签名验证失败 |
| 512 | TOKEN_IS_EXPIRED | Token已过期 | Redis中Token不存在 |
| 513 | TOKEN_IS_NONE_MATCH | Token不匹配 | Token与Redis中不一致 |
| 520 | ACCESS_DENIED | 没有访问权限 | Spring Security权限不足 |
| 521 | DATA_ACCESS_EXCEPTION | 数据访问异常 | 数据库访问异常 |

### 6.2 后端响应格式

#### 使用R类的模块 (用户/线索/活动/字典/交易/系统/统计)

```java
// 成功响应
{
    "code": 200,
    "msg": "操作成功",
    "data": { ... }  // 可选
}

// 失败响应
{
    "code": 500,
    "msg": "操作失败",
    "data": null
}

// Token错误响应
{
    "code": 510,  // 或511, 512, 513
    "msg": "token为空",
    "data": null
}
```

#### 使用Result类的模块 (产品/分类/促销/库存)

```java
// 成功响应
{
    "code": 200,
    "msg": "操作成功",
    "data": { ... }  // 可选
}

// 失败响应
{
    "code": 500,
    "msg": "具体错误信息",
    "data": null
}
```

### 6.3 前端错误处理流程

`shared/api/http-client.ts` 先读取 HTTP 状态，再生成统一 `ApiError`：

1. HTTP 401 表示未认证或当前 Token 已失效，通常设置 `sessionInvalid=true`，由认证状态流程清理本地会话并引导重新登录；登录请求自身的凭证失败只显示登录错误，不按“已有会话失效”处理，也不写入任何 Token。
2. HTTP 403 表示当前会话有效但功能权限或数据权限不足，设置 `sessionInvalid=false`，保留 Token 并提示无权限。
3. HTTP 500 表示系统错误，不能据此推断 Token 失效；调用方只显示失败信息并保留当前会话。
4. 其他业务错误按响应中的稳定业务码和消息处理，不使用 `code >= 500` 作为统一认证判断。

### 6.4 前端业务层错误处理

```javascript
// 典型的业务层错误处理模式
apiFunction(params).then(response => {
    if (response.data.code === 200) {
        // 成功处理
        messageTip("操作成功", "success");
    } else {
        // 业务失败提示
        messageTip(response.data.msg, "error");
    }
}).catch(error => {
    // 请求异常处理
    messageTip("请求失败", "error");
});
```

### 6.5 特殊错误处理

| 场景 | 后端返回 | 前端处理 | 用户体验 |
|-----|---------|---------|---------|
| 登录失败 | HTTP 401 和稳定登录错误码 | 不写入 Token、不触发已有会话失效流程，只显示错误消息 | 提示登录失败 |
| 权限不足 | HTTP 403 和 ACCESS_DENIED | 保留会话并提示无权限 | 当前账号可继续访问已有权限功能 |
| Token为空 | HTTP 401、code:510 | 按会话失效处理 | 引导重新登录 |
| Token无效 | HTTP 401、code:511 | 按会话失效处理 | 引导重新登录 |
| Token过期 | HTTP 401、code:512 | 按会话失效处理 | 引导重新登录 |
| Token不匹配 | HTTP 401、code:513 | 按会话失效处理 | 引导重新登录 |
| 业务操作失败 | R.FAIL() 或 R.FAIL("具体消息") | 显示失败消息 | 提示操作失败 |
| 参数校验失败 | R.FAIL(CodeEnum.PARAM_ERROR) | 显示参数错误 | 提示参数有误 |

---

## 附录：历史接口统计汇总

以下数量是旧版联调盘点快照，不作为当前接口契约或验收计数。当前用户管理接口以 `docs/api/openapi.yaml`、对应 Controller 和本文件 1.1 节为准；Task 21 验收必须重新由当前代码生成或统计，不得沿用下表数字。

| 统计项 | 数量 |
|-------|------|
| 前端API函数总数 | 68 |
| 后端Controller接口总数 | 72 |
| 前后端匹配接口数 | 56 |
| 前端调用但后端未实现 | 1 (getTranStatus) |
| 后端提供但前端未调用 | 16 |
| 路径不匹配 | 0 |
| 使用R类响应的模块 | 用户/线索/活动/字典/交易/统计 |
| 使用Result类响应的模块 | 产品/分类/促销/库存 |

---

## 7. 数据库初始化契约

`CarDealerCRM.sql` 是生产空库初始化入口。系统配置与系统监控能力已下线，初始化脚本不再包含 `t_system_info`。

### 7.1 用户管理基础模型升级

已有数据库禁止重新执行完整初始化脚本，也禁止直接执行 `dealer-server/src/main/resources/migration/*.sql`。用户管理迁移的唯一入口是：

```bash
scripts/database/user-management-migrate.sh plan
scripts/database/user-management-migrate.sh status
scripts/database/user-management-migrate.sh apply APPLY
scripts/database/user-management-migrate.sh resume <migration_key> RESUME
scripts/database/user-management-migrate.sh verify
```

完整初始化库使用 `CarDealerCRM.sql` 建库后，不重复执行历史回填，而是逐项核验现有对象并绑定 baseline：

```bash
scripts/database/user-management-migrate.sh baseline BASELINE
scripts/database/user-management-migrate.sh verify
```

执行前必须停止写入流量、完成数据库备份，并设置 `CRM_MIGRATION_DB_HOST`、`CRM_MIGRATION_DB_PORT`、`CRM_MIGRATION_DB_NAME`、`CRM_MIGRATION_DB_USERNAME`、`CRM_MIGRATION_DB_PASSWORD`。可通过 `CRM_MIGRATION_MYSQL_BIN=mysql` 或 `mariadb` 选择兼容客户端。必须记录当前应用版本、`t_user`、`t_user_role`、`t_role_permission` 行数、每个用户的 `id + login_act` 映射和各用户当前有效权限 code 集合。

`dealer-server/src/main/resources/migration/manifest.tsv` 是 Task 03、09、10、11、12、13、15、16、17、18、19、20 的唯一顺序、依赖、脚本 checksum、恢复模式和对象探针清单。执行器遵守以下边界：

- 获取 `car_dealer_crm:user_management_migration` 数据库命名锁后才允许建立或恢复迁移尝试。
- `t_user_management_migration` 记录 `RUNNING/SUCCEEDED/FAILED`、checksum、执行次数、错误摘要和最后完成步骤；`t_user_management_migration_step` 保存可恢复步骤。
- `apply APPLY` 只执行没有账本记录的迁移；`RUNNING/FAILED` 必须使用指定 migration key 的 `resume ... RESUME`，不得删除或伪造账本后重放。
- 每个建表、改表、索引、约束、种子或回填过程都在首个变更前再次校验命名锁、连接、migration key、checksum 和 `RUNNING` 状态；即使直接使用 `mysql --force` 执行 SQL，也不得改变业务 Schema 或回填数据。
- 授权历史和生命周期事件的不可变触发器是过程外方言定义，但只增加审计保护；其余业务对象仍必须在受控过程内完成。
- 只有脚本执行、步骤账本和 manifest 对象探针全部成功，执行器才把迁移标记为 `SUCCEEDED`；对象已存在但账本缺失不能视为成功。
- baseline 逐项执行与 manifest 相同的对象探针并绑定当前 checksum，不执行兼容回填，不得改变核心业务行数。
- Task15 在迁移 context 内新增 `account_expires_at`、`t_login_identifier` 和 `LOGIN_IDENTIFIER_GUARD`，并把所有现有非空 `t_user.login_act` 幂等回填为 ACTIVE 永久归属。账号为空、标识已归属其他用户或用户已有冲突当前标识时必须失败，不能使用 `IGNORE`、覆盖更新或手工删历史继续迁移。

迁移占位组织不是真实门店或团队。管理范围、负责人资格、接收人资格和业务数据范围必须排除 `migration_placeholder=1`；补录真实组织和岗位前不得把占位任职作为授权依据。

故障恢复时先运行 `status`，核对失败 key、checksum 和 `last_completed_step`，修复明确原因后只对该 key 执行 `resume`。禁止从 SQL 文件开头手工重跑、删除字段、清空角色关系、修改 checksum 或伪造成功账本。成功后必须运行 `verify`，并重新核对账号状态、账号/凭证期限、登录标识永久归属、密码摘要、用户角色、角色权限、有效权限集合、孤儿关系、主任职、迁移占位和历史触发器。

H2 只用于 Mapper、约束近似和事务测试，不能证明 MySQL/MariaDB 方言。发布验收必须在受支持的 MySQL 和 MariaDB 实例上分别执行：旧库首次升级、Task 10/12/13/15 故障注入与恢复、登录账号永久不可转让、重复 `apply`、逐脚本 `mysql --force` 防绕过、完整库 baseline，以及授权历史和生命周期事件拒绝 `UPDATE/DELETE`。真实验证入口为 `scripts/database/test-user-management-migrations-real.sh`；该脚本一次验证当前环境变量指定的一种数据库，双库验收必须分别运行并保存厂商、版本和输出证据。

Task15 成功或 baseline 绑定前，至少执行以下只读核对；任一结果不符合预期都不能发布：

```sql
-- 应为 0：每个现有用户都有与当前 login_act 完全匹配的 ACTIVE 归属。
SELECT COUNT(*) AS missing_login_identifier
FROM t_user u
LEFT JOIN t_login_identifier li
  ON li.user_id=u.id AND li.login_act=u.login_act
 AND li.status='ACTIVE' AND li.active_marker=1 AND li.retired_at IS NULL
WHERE u.login_act IS NULL OR li.id IS NULL;

-- 应为空：登录标识不得跨用户重复，每个用户最多一个 ACTIVE 标识。
SELECT login_act, COUNT(DISTINCT user_id) AS owners
FROM t_login_identifier GROUP BY login_act HAVING owners <> 1;
SELECT user_id, COUNT(*) AS active_identifiers
FROM t_login_identifier WHERE active_marker=1
GROUP BY user_id HAVING active_identifiers <> 1;

-- 应返回 1；两个到期列都应存在且允许 NULL。
SELECT COUNT(*) FROM t_authorization_graph_lock WHERE lock_name='LOGIN_IDENTIFIER_GUARD';
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema=DATABASE() AND table_name='t_user'
  AND column_name IN ('account_expires_at','password_expires_at');
```

### 7.2 组织管理联调契约

- 组织树和组织员工列表均由后端按当前操作者组织范围裁剪；前端不得缓存全量 ID 后自行判断范围。
- 组织上级、负责人和直属管理者使用专用候选接口，写接口仍会重新校验候选资格、汇报环和对象范围。
- 组织、岗位状态请求必须携带 `expectedVersion` 和 `reason`；任职更新必须携带员工 `expectedVersion`、完整主任职、完整兼任集合、当前汇报分区和原因。
- 任职与汇报权限按实际差异分别校验。客户端为保持完整表单而回传未变化分区时，不会触发额外权限要求，也不会重写该分区。
- DIRECT 仍是唯一直属管理者；ACTING 通过 `/api/employees/{id}/acting-reporting-relations` 读取和按员工版本整体替换有限期多关系集合，并使用专用 `/manager-candidates` 候选。空集合只结束 ACTING，不得覆盖 DIRECT。
- 用户不能通过组织管理入口读取或调整本人；个人资料入口由后续个人中心契约提供。本人权限始终只能由有权管理其账号的上级或安全管理员调整。
- `403` 表示缺少权限、本人操作、受保护目标或对象超范围；组织环、汇报环、停用阻断和 CAS 冲突返回 `409` 及稳定业务 code。
- ACTING 保存返回 `409` 或员工版本冲突 code 时，前端必须重载关系集合与候选，不能保留旧 `employeeVersion` 继续提交。
- 任职和汇报响应只返回员工号、姓名、组织、岗位、管理者和可操作提示，不返回手机号、邮箱或持久化实体。

### 7.3 角色权限矩阵联调契约

- 权限目录仅允许 GET；客户端不能创建、修改或删除权限 code。
- 角色 code 创建后不可修改，受保护恢复角色不可通过普通目录或矩阵接口削弱，角色不物理删除。
- `GLOBAL` 角色仅由安全管理员维护；`ORGANIZATION` 角色必须提交适用组织，服务端重新校验操作者覆盖全部组织和全部有效成员。
- 矩阵预览和保存都校验 `expectedVersion`、未知/停用权限、操作者实际拥有的权限、可委派标记和授权级别；保存理由必填。
- 预览或保存返回角色版本冲突时，前端必须重载角色详情、权限目录和矩阵，丢弃基于旧版本的本地预览。
- 无差异矩阵请求不删除重插、不递增角色版本、不撤销会话。实际变化在事务内写历史、审计并精确递增受影响用户 `auth_version`；任一行缺失即整体回滚。
- Redis 登录缓存只在提交后清理。响应 `sessionCleanupWarningCount=null` 表示提交后动作尚不能在同步响应中证明；数据库安全版本已经使旧 Token 失效。

### 7.4 用户管理工作台联调契约

- `GET /api/users` 支持关键词、组织、岗位、直属管理者、角色、任职状态、账号状态和锁定状态组合筛选；分页、白名单排序和唯一主键次序全部由后端完成。
- `GET /api/users/filter-options` 的 `roles` 是列表筛选使用的可见角色事实；`assignableRoles` 是创建用户时不超过操作者委派上限的候选。已选择组织时应携带 `organizationUnitId` 重新查询候选，前端不得把两者混用。
- `GET /api/users/{id}` 返回独立的 `profileVersion`、`accountVersion`、`employeeVersion`、`authorizationVersion` 和 `sessionRevision`。`allowedActions` 与 `unavailableReasons` 是目标对象级判断结果，前端不得根据角色名或岗位名推断。
- 批量角色或个人权限命令发生 `409`/授权版本冲突时，前端必须逐个重载所选用户授权详情和列表，重置旧编辑内容；再次提交只能使用新的 `authorizationVersion`。
- 本人管理详情只读并引导到个人中心；本人、同级、上级、跨范围和受保护账号的写操作由后端拒绝。不存在资源返回 `404`，存在但超范围返回 `403`。
- `POST /api/users` 不接受密码、userId、授权状态等额外字段；创建结果不包含密码、Token 或凭证摘要。初始角色、授权历史、凭证和账号员工事实全成功或全回滚。
- 账号状态请求必须携带最新 `accountVersion` 和原因；版本过期返回 `627/409`。人工解锁只清除人工锁定，不清除仍有效的自动锁定。
- 登录账号修改同样携带 `accountVersion` 和原因。服务端持有 `LOGIN_IDENTIFIER_GUARD` 后同时检查 `t_user` 当前账号与 `t_login_identifier` 历史；旧标识只能由原用户重新启用，不能转给其他员工。
- 安全期限修改分别使用 `accountExpiresAt` 与 `credentialExpiresAt`；前端不得根据一个值推导另一个值。设置、清空或改动任一安全期限都会提升认证版本并撤销旧会话，且不能使最后一个有效普通管理员失效。
- 旧 `/api/user` 万能创建/更新、单数状态、角色、密码和交接写路径已停用并 fail-close；只读 `GET /api/user/{id}` 仅作 deprecated 兼容。
- Task 18 由 manifest 在 Task 17 后调度；索引过程在内部二次校验迁移上下文，步骤完成后写入步骤账本，最终由统一对象探针和 checksum 决定是否标记 `SUCCEEDED`。

#### 7.4.1 登录标识与安全期限手工验收

1. 创建普通员工 A，记录账号版本和活动会话；把账号从 `employee_a` 改为 `employee_a_new`，确认旧会话立即返回 401，`t_login_identifier` 同时保留一条 RETIRED 旧标识和一条 ACTIVE 新标识。
2. 创建普通员工 B 时尝试使用 `employee_a`，应返回重复冲突且整笔邀请事务无账号、员工、任职、角色或凭证残留；再由员工 A 改回 `employee_a` 应成功，并把该用户自己的历史标识重新启用。
3. 分别只设置 `accountExpiresAt`、只设置 `credentialExpiresAt`，确认响应和数据库两个时间字段互不覆盖；到期后登录及已有 Token 均失败，清空对应字段后只恢复该维度的期限状态。
4. 对普通管理员设置未来或已到期时间时，确认可用管理员重新计算包含当前时间判断；尝试使最后一个有效普通管理员的账号或凭证到期必须返回 403，且版本、期限、会话和审计均不得部分变化。
5. 并发发起相同登录账号的邀请或改名，只允许一个事务成功；失败请求返回稳定重复/冲突响应，不得泄露数据库唯一约束异常或产生两个 ACTIVE 标识。

### 7.5 用户历史联调契约

- `GET /api/users/{id}/history` 接收 `page`、`size`、`actionCode`、`startTime`、`endTime`；分页从 1 开始，`size` 为 1—100，开始时间晚于结束时间或未知动作返回 `400`。
- 响应固定包含 `list/total/pageSize/pageNum/pages/size`、`actionOptions`、`allowedActions` 和 `unavailableReasons`。前端只消费结构化 `beforeValues/afterValues`，不得读取或猜测操作日志原始 `detail`。
- 后端同时校验 `audit:operation:detail` 和目标用户管理范围；本人、无审计权限、跨组织及受保护目标返回 `403`，目标不存在返回 `404`。
- 同一请求可产生账号、凭证、任职、汇报和角色等多个不同业务子事件；仅真正重复的配套索引可合并，不得按 `requestId` 一刀切吞掉不同语义事件。
- 服务端响应禁止出现密码、哈希、Token、凭证明文、完整手机号、完整邮箱、IP、原始请求/响应或会话标识。前端遮罩只作为第二道防线。

## 8. AI 业务助手联调契约

| 前端 API | 方法 | 路径 | 参数 | 返回 | 后端入口 | 说明 |
|----------|------|------|------|------|----------|------|
| createAiRun | POST | /api/ai/runs | data: CreateAiRunRequest | R\<AiRunResponse\> | AiRunController.create | 创建 AI Run，写入用户消息摘要 |
| fetchAiRun | GET | /api/ai/runs/{runNo} | 路径参数: runNo | R\<AiRunResponse\> | AiRunController.detail | 刷新或断线后恢复 Run 状态 |
| fetchAiRunTrace | GET | /api/ai/runs/{runNo}/trace | 路径参数: runNo | R\<AiRunTraceResponse\> | AiRunController.trace | 恢复 Run、消息、工具调用、Proposal、工作流和执行事件 |
| streamAiRunEvents | GET | /api/ai/runs/{runNo}/events | 路径参数: runNo | text/event-stream | AiRunController.events | 前端只订阅 Spring Boot SSE |
| editAiMessage | PATCH | /api/ai/conversations/{conversationNo}/messages/{messageNo} | 路径参数 + content、expectedVersion | R\<AiRunResponse\> | AiConversationController.editMessage | 创建替代 Run，旧分支保留审计但退出上下文 |
| withdrawAiMessage | POST | /api/ai/conversations/{conversationNo}/messages/{messageNo}/withdraw | 路径参数 + expectedVersion | R\<AiConversationDetailResponse\> | AiConversationController.withdrawMessage | 撤回不回滚已执行业务动作 |
| getAiAssistantPolicy | GET | /api/ai/policy | 无 | R\<AiAssistantPolicyResponse\> | AiAssistantPolicyController.getPolicy | 管理员查看全局工具、安全、联网和上下文策略 |
| updateAiAssistantPolicy | PUT | /api/ai/policy | data: UpdateAiAssistantPolicyRequest | R\<AiAssistantPolicyResponse\> | AiAssistantPolicyController.updatePolicy | 使用 version 乐观锁更新全局策略 |
| confirmAiProposal | POST | /api/ai/proposals/{proposalId}/confirm | 路径参数: proposalId | R\<AiProposalConfirmResponse\> | AiProposalController.confirm | 只执行后端保存参数 |
| rejectAiProposal | POST | /api/ai/proposals/{proposalId}/reject | 路径参数: proposalId | R\<AiProposalConfirmResponse\> | AiProposalController.reject | 拒绝待确认 Proposal |
| executeInternalAiTool | POST | /internal/ai/tools/{toolName}/execute | header: X-Dealer-AI-Tool-Token, data: ExecuteAiToolRequest | R\<AiToolExecutionResponse\> | AiInternalToolController.execute | 仅供 dealer-ai 内部调用 |
| createAiWorkflow | POST | /api/ai/workflows | data: CreateAiWorkflowRequest | R\<AiWorkflowResponse\> | AiWorkflowController.create | 启动受控多步骤工作流 |
| listAiWorkflows | GET | /api/ai/workflows | query: runNo | R\<List\<AiWorkflowResponse\>\> | AiWorkflowController.list | 查询 Run 下工作流 |
| fetchAiWorkflow | GET | /api/ai/workflows/{workflowNo} | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.detail | 查询工作流步骤和状态 |
| pauseAiWorkflow | POST | /api/ai/workflows/{workflowNo}/pause | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.pause | 暂停未终态工作流 |
| resumeAiWorkflow | POST | /api/ai/workflows/{workflowNo}/resume | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.resume | 恢复已暂停工作流 |
| cancelAiWorkflow | POST | /api/ai/workflows/{workflowNo}/cancel | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.cancel | 取消未终态工作流 |
| completeAiWorkflow | POST | /api/ai/workflows/{workflowNo}/complete | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.complete | 标记工作流完成 |
| failAiWorkflow | POST | /api/ai/workflows/{workflowNo}/fail | 路径参数: workflowNo | R\<AiWorkflowResponse\> | AiWorkflowController.fail | 标记工作流失败并记录原因 |
| createAiProactiveSubscription | POST | /api/ai/proactive/subscriptions | data: CreateAiProactiveSubscriptionRequest | R\<AiProactiveSubscriptionResponse\> | AiProactiveController.createSubscription | 创建当前用户主动提醒订阅 |
| listAiProactiveSubscriptions | GET | /api/ai/proactive/subscriptions | 无 | R\<List\<AiProactiveSubscriptionResponse\>\> | AiProactiveController.listSubscriptions | 查询当前用户订阅 |
| pauseAiProactiveSubscription | POST | /api/ai/proactive/subscriptions/{subscriptionNo}/pause | 路径参数: subscriptionNo | R\<AiProactiveSubscriptionResponse\> | AiProactiveController.pauseSubscription | 暂停订阅 |
| resumeAiProactiveSubscription | POST | /api/ai/proactive/subscriptions/{subscriptionNo}/resume | 路径参数: subscriptionNo | R\<AiProactiveSubscriptionResponse\> | AiProactiveController.resumeSubscription | 恢复订阅 |
| cancelAiProactiveSubscription | POST | /api/ai/proactive/subscriptions/{subscriptionNo}/cancel | 路径参数: subscriptionNo | R\<AiProactiveSubscriptionResponse\> | AiProactiveController.cancelSubscription | 取消订阅 |
| listAiProactiveEvents | GET | /api/ai/proactive/events | query: page, size | R\<List\<AiProactiveEventResponse\>\> | AiProactiveController.listEvents | 查询当前用户提醒事件 |
| fetchAiProactiveEvent | GET | /api/ai/proactive/events/{eventNo} | 路径参数: eventNo | R\<AiProactiveEventResponse\> | AiProactiveController.eventDetail | 查询提醒事件详情 |
| generateAiProactiveEvents | POST | /api/ai/proactive/events/generate | 无 | R\<List\<AiProactiveEventResponse\>\> | AiProactiveController.generateEvents | 为当前用户生成到期提醒 |
| listAiProviderConfigs | GET | /api/ai/provider-configs | 无 | R\<List\<AiProviderConfigResponse\>\> | AiProviderConfigController.list | 查询脱敏模型配置列表 |
| createAiProviderConfig | POST | /api/ai/provider-configs | data: CreateAiProviderConfigRequest | R\<AiProviderConfigResponse\> | AiProviderConfigController.create | 新增 Provider 配置并加密保存 API Key |
| updateAiProviderConfig | PUT | /api/ai/provider-configs/{configNo} | 路径参数: configNo, data: UpdateAiProviderConfigRequest | R\<AiProviderConfigResponse\> | AiProviderConfigController.update | 编辑非密钥字段 |
| rotateAiProviderKey | POST | /api/ai/provider-configs/{configNo}/rotate-key | 路径参数: configNo, data: RotateAiProviderKeyRequest | R\<AiProviderConfigResponse\> | AiProviderConfigController.rotateKey | 独立轮换 API Key |
| testAiProviderConfig | POST | /api/ai/provider-configs/{configNo}/test | 路径参数: configNo | R\<AiProviderConfigTestResponse\> | AiProviderConfigController.test | 限 token、限超时测试连接 |
| activateAiProviderConfig | POST | /api/ai/provider-configs/{configNo}/activate | 路径参数: configNo | R\<AiProviderConfigResponse\> | AiProviderConfigController.activate | 启用当前配置并停用其他配置 |
| disableAiProviderConfig | POST | /api/ai/provider-configs/{configNo}/disable | 路径参数: configNo | R\<AiProviderConfigResponse\> | AiProviderConfigController.disable | 停用当前配置 |

AI SSE 已实现事件：`run_started`、`message_delta`、`message_completed`、`tool_call_started`、`tool_call_completed`、`proposal_created`、`workflow_started`、`workflow_step_started`、`workflow_step_completed`、`workflow_waiting_user_confirmation`、`workflow_paused`、`workflow_resumed`、`workflow_cancelled`、`workflow_expired`、`workflow_failed`、`workflow_completed`、`error`、`run_completed`。

`dealer-web` 不调用独立 AI 服务；`dealer-ai` 不连接业务数据库、Redis 会话或 Mapper。`dealer-ai` 只通过内部 Tool API 请求 Spring Boot，最终权限、数据范围、参数校验、Proposal 保存和业务写入均由 Spring Boot 控制。

Compose 演示环境使用服务名 `ai` 和容器名 `car-dealer-crm-ai` 托管编排服务。`dealer-server` 的目标地址固定为容器内网 `http://ai:8091`，浏览器和宿主机不直接访问该端口。启动脚本等待 `/ready` 通过；超时、退出或 unhealthy 时输出 `ai`、`server` 最近日志并以非零状态结束。Spring Boot 的本地 AI Provider 主密钥目录挂载到 `server-ai-secret` 命名卷，旧容器密钥只允许在目标卷为空时迁移。

ToolCall 成功结果必须通过 Spring Boot 保存脱敏 `displayPayload`，Run trace 和 Conversation turns 都必须返回该字段，保证刷新和切换会话后业务卡片不丢失。

`dealer-ai` 生成的工具参数和 Proposal 参数必须使用 Spring Boot Java enum、后端 DTO 校验和 OpenAPI enum 已支持的值。ToolCall 成功和失败都由 Spring Boot 写入 trace，`fetchAiRunTrace` 刷新恢复时必须能返回 toolCalls、proposals、approvals、workflows 和 executionEvents，前端不得只依赖当前 SSE 连接展示结果。主动提醒生成在 Spring Boot 内完成，复用现有只读业务口径，不调用 `dealer-ai`。

Provider API Key 明文只允许出现在新增和轮换请求体中。Spring Boot 加密保存并只向前端返回 `maskedApiKey`；`providerRuntimeConfig` 禁止进入前端响应、SSE、trace 或日志。`prod` 等非本地环境必须显式配置 `AI_PROVIDER_KEY_ENCRYPTION_SECRET`；本地、开发、测试和 smoke 环境未配置时自动使用 `~/.car-dealer-crm/ai-provider-key.secret`。

服务间认证变量关系：

- `DEALER_AI_ENV`：Spring Boot 与 `dealer-ai` 共用的运行环境名；Python 继续兼容旧 `DEALER_AI_ENVIRONMENT`，Compose 统一使用前者。
- `DEALER_AI_BASE_URL`：Spring Boot 调用 `dealer-ai` 的内部地址，Compose 固定为 `http://ai:8091`。
- `DEALER_AI_INTERNAL_TOKEN`：Spring Boot 调用 `dealer-ai` 的 token，`dealer-ai` 用同名变量校验。
- `DEALER_AI_TOOL_TOKEN`：Spring Boot 内部 Tool API 校验 `X-Dealer-AI-Tool-Token` 的 token。
- `DEALER_AI_SPRING_TOOL_BASE_URL`：`dealer-ai` 调用 Spring Boot Tool API 的内部地址，Compose 固定为 `http://server:8089/internal/ai`。
- `DEALER_AI_SPRING_TOOL_TOKEN`：`dealer-ai` 调用 Spring Boot 内部 Tool API 时发送的 token，必须与 `DEALER_AI_TOOL_TOKEN` 一致。
- 本地 `local/dev/test/smoke` 环境默认使用 `dev-internal-token`，非本地环境必须显式配置上述 token，不允许使用默认值启动。

---

*文档生成时间: 2026-05-30*
*分析工具: opencode*
