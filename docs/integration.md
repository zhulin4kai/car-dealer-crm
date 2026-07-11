# 前后端联调分析报告

## 目录
1. [接口对接情况](#1-接口对接情况)
2. [联调问题检查](#2-联调问题检查)
3. [认证流程链路](#3-认证流程链路)
4. [文件上传下载流程](#4-文件上传下载流程)
5. [分页参数传递方式](#5-分页参数传递方式)
6. [错误处理流程](#6-错误处理流程)

---

## AI 业务助手联调约定

- 前端通过 `/api/ai/conversations` 创建、查询、重命名和归档 AI Conversation。Conversation 详情响应中的 `turns` 是会话恢复主契约，前端必须用它恢复每轮消息、业务卡片、Proposal、Workflow 和处理过程。
- 前端发送问题时调用 `/api/ai/runs`，优先携带当前 `conversationNo`；未携带时后端按当前用户和业务对象上下文解析默认会话。
- 前端订阅 `/api/ai/runs/{runNo}/events` 获取本次 Run SSE；断线时携带 `afterSequence` 重放后续持久化事件，同一 Run 不会再次启动。刷新恢复整个会话时调用 `/api/ai/conversations/{conversationNo}`。
- Spring Boot 调用 `dealer-ai` 时携带 `conversationNo`、脱敏会话摘要、管理员配置的最近 1 到 8 条活动消息、工具权限交集和运行策略；`dealer-ai` 不保存会话。
- 用户消息编辑调用 `PATCH /api/ai/conversations/{conversationNo}/messages/{messageNo}`，撤回调用对应 `/withdraw`；两者使用 `expectedVersion` 防止并发覆盖。
- 会话归档后默认列表不显示，但 Run trace 和审计链仍可按权限查询。

---

## 1. 接口对接情况

### 1.1 用户管理模块 (user.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getUserList | GET | /api/users | params: {page, size, ...query} | R\<PageInfo\<TUser\>\> | UserController.userPage | 分页查询用户列表，默认每页10条 |
| getUserDetail | GET | /api/user/{id} | 路径参数: id | R\<TUser\> | UserController.userDetail | 根据ID获取用户详情 |
| createUser | POST | /api/user | data: CreateUserRequest (JSON) | R\<UserDetailResponse\> | UserController.createUser | 新增用户，需要 Bearer Token |
| updateUser | PUT | /api/user | data: UpdateUserRequest (JSON) | R\<UserDetailResponse\> | UserController.updateUser | 编辑用户，需要 Bearer Token |
| disableUser | PUT | /api/user/{id}/disable | 路径参数: id | R | UserController.disableUser | 禁用账号并撤销 Redis 会话 |
| enableUser | PUT | /api/user/{id}/enable | 路径参数: id | R | UserController.enableUser | 启用账号并刷新负责人缓存 |
| batchDisableUsers | PUT | /api/users/batch-disable | data: {ids: List\<Integer\>} (JSON) | R | UserController.batchDisableUsers | 批量禁用账号 |
| handoverUserResponsibilities | PUT | /api/user/{id}/handover | 路径参数: id, data: {targetUserId, reason} (JSON) | R\<HandoverUserResponsibilitiesResponse\> | UserController.handoverResponsibilities | 交接当前负责的活动、线索和客户并写审计 |
| getOwnerList | GET | /api/owner | 无 | R\<List\<TUser\>\> | UserController.owner | 获取负责人列表(不分页) |
| getLoginInfo | GET | /api/login/info | 无 | R\<TUser\> | UserController.loginInfo | 获取当前登录用户信息 |

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
| Token错误 | R | code(510-520), msg | response.data.code >= 500 | 前端通过code>=500判断token错误，与后端CodeEnum一致 | - |

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
                    │ (用户信息    │   │ key:         │   │ 记住我:7天   │
                    │  作为负载)   │   │ cdrm:user:   │   │ 否则:4小时   │
                    │              │   │ login:{id}   │   │              │
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
| 4 | 生成JWT | JWTUtils.createJWT(userJSON), 负载为用户信息JSON |
| 5 | 存储到Redis | key: `cdrm:user:login:{userId}`, value: jwt |
| 6 | 设置过期时间 | rememberMe=true: 7天, rememberMe=false: 4小时 |
| 7 | 返回JWT给前端 | 仅 Redis 写入成功后返回 R.OK(jwt) |
| 8 | Redis 写入失败 | 返回 HTTP 500 和 SYSTEM_ERROR，不返回 JWT |

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

```
┌─────────────┐     请求(带Authorization头)     ┌─────────────────┐
│   前端请求   │  ──────────────────────────────►  │ TokenVerifyFilter│
└─────────────┘                                  └─────────────────┘
                                                        │
                                                        ▼
                                                ┌───────────────┐
                                                │ 是否是登录请求?│
                                                └───────────────┘
                                                   │         │
                                                  是         否
                                                   │         │
                                                   ▼         ▼
                                            ┌──────────┐ ┌──────────────┐
                                            │ 直接放行 │ │ 获取Token    │
                                            └──────────┘ └──────────────┘
                                                              │
                                                              ▼
                                                      ┌───────────────┐
                                                      │ Token是否为空? │
                                                      └───────────────┘
                                                         │         │
                                                        是         否
                                                         │         │
                                                         ▼         ▼
                                                  ┌──────────┐ ┌──────────────┐
                                                  │返回510   │ │ JWT签名验证  │
                                                  │token为空 │ └──────────────┘
                                                  └──────────┘        │
                                                                      ▼
                                                              ┌───────────────┐
                                                              │ 签名是否有效? │
                                                              └───────────────┘
                                                                 │         │
                                                                否         是
                                                                 │         │
                                                                 ▼         ▼
                                                          ┌──────────┐ ┌──────────────┐
                                                          │返回511   │ │ 从Redis获取  │
                                                          │token无效 │ │ 存储的token  │
                                                          └──────────┘ └──────────────┘
                                                                            │
                                                                            ▼
                                                                    ┌───────────────┐
                                                                    │ Redis有token? │
                                                                    └───────────────┘
                                                                       │         │
                                                                      否         是
                                                                       │         │
                                                                       ▼         ▼
                                                                ┌──────────┐ ┌──────────────┐
                                                                │返回512   │ │ Token匹配?   │
                                                                │token过期 │ └──────────────┘
                                                                └──────────┘    │         │
                                                                               否         是
                                                                                │         │
                                                                                ▼         ▼
                                                                         ┌──────────┐ ┌──────────────┐
                                                                         │返回513   │ │ 设置Security │
                                                                         │token不匹配│ │ 上下文认证   │
                                                                         └──────────┘ └──────────────┘
                                                                                          │
                                                                                          ▼
                                                                                  ┌──────────────┐
                                                                                  │ 异步刷新Token│
                                                                                  │ 过期时间     │
                                                                                  └──────────────┘
                                                                                          │
                                                                                          ▼
                                                                                  ┌──────────────┐
                                                                                  │ 继续执行     │
                                                                                  │ Filter链     │
                                                                                  └──────────────┘
```

### 3.5 Token刷新机制

| 场景 | 触发条件 | 刷新逻辑 | 新过期时间 |
|-----|---------|---------|-----------|
| 每次请求 | Token校验通过后 | 异步执行(threadPoolTaskExecutor) | - |
| 记住我模式 | rememberMe=true | redisService.expire() | 7天 |
| 普通模式 | rememberMe=false | redisService.expire() | 4小时 |

### 3.6 Token过期/错误处理

```javascript
// 前端响应拦截器 (httpRequest.js:67-87)
axios.interceptors.response.use((response) => {
    // code >= 500 表示token验证未通过
    if (response.data.code >= 500) {
        // 提示用户并询问是否重新登录
        messageConfirm(response.data.msg + "，是否重新去登录？").then(() => {
            // 删除本地token
            removeToken();
            // 跳转到登录页
            window.location.href = "/";
        }).catch(() => {
            messageTip("取消去登录", "warning");
        });
        return Promise.reject(new Error(response.data.msg));
    }
    return response;
});
```

### 3.7 后端Token错误码

| 错误码 | 常量名 | 含义 | 前端处理 |
|-------|-------|------|---------|
| 510 | TOKEN_IS_EMPTY | Token为空 | 提示并跳转登录 |
| 511 | TOKEN_IS_ERROR | Token无效(签名错误) | 提示并跳转登录 |
| 512 | TOKEN_IS_EXPIRED | Token已过期(Redis中不存在) | 提示并跳转登录 |
| 513 | TOKEN_IS_NONE_MATCH | Token不匹配(与Redis中不一致) | 提示并跳转登录 |

### 3.8 退出登录流程

```
┌─────────────┐     POST /api/logout     ┌─────────────────┐
│   前端退出   │  ──────────────────────►  │ Spring Security │
│              │                           │ 框架处理         │
└─────────────┘                           └─────────────────┘
                                                │
                                                ▼
                                    ┌───────────────────────┐
                                    │ MyLogoutSuccessHandler │
                                    └───────────────────────┘
                                                │
                            ┌───────────────────┼───────────────────┐
                            ▼                   ▼                   ▼
                    ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
                    │ 1.获取用户   │   │ 2.删除Redis  │   │ 3.返回结果   │
                    │ 信息         │   │ 中的Token    │   │ 成功或失败   │
                    └──────────────┘   └──────────────┘   └──────────────┘
```

Redis 删除成功后返回退出成功，前端随后清理本地 token 与权限缓存；删除失败或抛出异常时返回 HTTP 500 和 SYSTEM_ERROR，前端保留本地会话并提示失败，不得误认为会话已经失效。

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

```
┌─────────────┐     axios响应      ┌─────────────────┐
│   后端响应   │  ────────────────►  │ 响应拦截器       │
└─────────────┘                    └─────────────────┘
                                          │
                                          ▼
                                  ┌───────────────┐
                                  │ code >= 500?  │
                                  └───────────────┘
                                     │         │
                                    是         否
                                     │         │
                                     ▼         ▼
                              ┌──────────┐ ┌──────────────┐
                              │ Token错误│ │ 正常响应      │
                              │ 处理     │ │ 返回给调用方  │
                              └──────────┘ └──────────────┘
                                     │
                                     ▼
                              ┌──────────────┐
                              │ 弹出确认框   │
                              │ "xxx，是否   │
                              │  重新去登录？"│
                              └──────────────┘
                                     │
                        ┌────────────┴────────────┐
                        ▼                         ▼
                ┌──────────────┐          ┌──────────────┐
                │ 点击"确定"   │          │ 点击"取消"   │
                │ 删除Token    │          │ 提示"取消    │
                │ 跳转登录页   │          │  去登录"     │
                └──────────────┘          └──────────────┘
```

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
| 登录失败 | R.FAIL(exception.getMessage()) | 显示错误消息 | 提示具体失败原因 |
| 权限不足 | MyAccessDeniedHandler | 返回403 | 提示无权限 |
| Token为空 | code:510 | 弹窗询问是否登录 | 可选择重新登录 |
| Token无效 | code:511 | 弹窗询问是否登录 | 可选择重新登录 |
| Token过期 | code:512 | 弹窗询问是否登录 | 可选择重新登录 |
| Token不匹配 | code:513 | 弹窗询问是否登录 | 可选择重新登录 |
| 业务操作失败 | R.FAIL() 或 R.FAIL("具体消息") | 显示失败消息 | 提示操作失败 |
| 参数校验失败 | R.FAIL(CodeEnum.PARAM_ERROR) | 显示参数错误 | 提示参数有误 |

---

## 附录：接口统计汇总

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
