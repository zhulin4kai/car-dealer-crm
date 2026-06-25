# 前后端联调分析报告

## 目录
1. [接口对接情况](#1-接口对接情况)
2. [联调问题检查](#2-联调问题检查)
3. [认证流程链路](#3-认证流程链路)
4. [文件上传下载流程](#4-文件上传下载流程)
5. [分页参数传递方式](#5-分页参数传递方式)
6. [错误处理流程](#6-错误处理流程)

---

## 1. 接口对接情况

### 1.1 用户管理模块 (user.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getUserList | GET | /api/users | params: {current: number} | R\<PageInfo\<TUser\>\> | UserController.userPage | 分页查询用户列表，默认每页10条 |
| getUserDetail | GET | /api/user/{id} | 路径参数: id | R\<TUser\> | UserController.userDetail | 根据ID获取用户详情 |
| createUser | POST | /api/user | data: UserQuery对象 (FormData) | R | UserController.addUser | 新增用户，需要Authorization头 |
| updateUser | PUT | /api/user | data: UserQuery对象 (FormData) | R | UserController.editUser | 编辑用户，需要Authorization头 |
| deleteUser | DELETE | /api/user/{id} | 路径参数: id | R | UserController.delUser | 删除单个用户 |
| batchDeleteUsers | DELETE | /api/user | data: List\<Integer\> (JSON数组) | R | UserController.batchDelUser | 批量删除用户 |
| getOwnerList | GET | /api/owner | 无 | R\<List\<TUser\>\> | UserController.owner | 获取负责人列表(不分页) |
| getLoginInfo | GET | /api/login/info | 无 | R\<TUser\> | UserController.loginInfo | 获取当前登录用户信息 |

### 1.2 线索管理模块 (clue.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getCurrentClues | GET | /api/clues | params: {current: number} | R\<PageInfo\<TClue\>\> | ClueController.cluePage | 分页查询线索列表 |
| addClue | POST | /api/clue | data: ClueQuery对象 (FormData) | R | ClueController.addClue | 新增线索 |
| updateClue | PUT | /api/clue | data: ClueQuery对象 (FormData) | R | ClueController.editClue | 编辑线索 |
| delClueById | DELETE | /api/clue/{id} | 路径参数: id | R | ClueController.delClue | 删除单个线索 |
| batchDeleteCluesByIds | POST | /api/clue/batch | data: List\<Integer\> (JSON数组) | R | ClueController.batchDelClue | 批量删除线索 |
| checkPhoneIsExist | GET | /api/clue/{phone} | 路径参数: phone | R | ClueController.checkPhone | 检查手机号是否存在 |
| getClueDetail | GET | /api/clue/detail/{id} | 路径参数: id | R\<TClue\> | ClueController.loadClue | 获取线索详情 |
| importExcelAPI | POST | /api/importExcel | data: MultipartFile (FormData) | R | ClueController.importExcel | Excel导入线索 |
| addClueRemark | POST | /api/clue/remark | data: {clueId, noteContent, noteWay} (JSON) | R | ClueRemarkController.addActivityRemark | 添加线索备注 |
| getClueRemarkList | GET | /api/clue/remark | params: {current, clueId} | R\<PageInfo\<TClueRemark\>\> | ClueRemarkController.clueRemarkPage | 分页查询线索备注 |
| convertClueToCustomer | POST | /api/clue/customer | data: {clueId, product, description, nextContactTime} (JSON) | R | CustomerController.convertCustomer | 线索转换为客户 |

### 1.3 客户管理模块 (customer.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getCustomerList | GET | /api/customer/list | params: {page, size, ...query} | R\<PageInfo\<TCustomer\>\> | CustomerController.list | 分页查询客户列表 |
| getCustomerOptions | GET | /api/customer/options | 无 | R\<List\<CustomerOption\>\> | CustomerController.options | 获取客户选项(下拉框用) |

### 1.4 市场活动模块 (activity.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getActivityList | GET | /api/activitys | params: {current, ...activityQuery} | R\<PageInfo\<TActivity\>\> | ActivityController.activityPage | 分页查询活动列表 |
| getActivityById | GET | /api/activity/{id} | 路径参数: id | R\<TActivity\> | ActivityController.loadActivity | 获取活动详情 |
| createActivity | POST | /api/activity | data: ActivityQuery对象 (FormData) | R | ActivityController.addActivity | 新增活动 |
| updateActivity | PUT | /api/activity | data: ActivityQuery对象 (FormData) | R | ActivityController.editActivity | 编辑活动 |
| deleteActivity | DELETE | /api/activity/{id} | 路径参数: id | R | ActivityController.deleteActivity | 删除单个活动 |
| batchDeleteActivities | POST | /api/activity/batch | data: List\<Integer\> (JSON数组) | R | ActivityController.batchDeleteActivities | 批量删除活动 |

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
| getPromotionList | GET | /api/product-promotions | params: {page, size} | Result\<PageInfo\<ProductPromotion\>\> | ProductPromotionController.getPromotionList | 分页查询促销列表 |
| createPromotion | POST | /api/product-promotions | data: ProductPromotion对象 (JSON) | Result\<Void\> | ProductPromotionController.addPromotion | 新增促销 |

产品 `status` 请求值必须使用 `ON_SALE` 或 `OFF_SALE`，前端只把“上架/下架”作为展示 label。
商品删除失败时前端按 `ApiError.code === 422` 映射“已被业务引用，不能直接删除”，不匹配中文 msg 或数据库外键文案。
分类删除失败时前端同样按 `ApiError.code === 422` 映射“已被商品或历史记录引用，不能直接删除”。
| updatePromotion | PUT | /api/product-promotions/{id} | 路径参数: id, data: ProductPromotion对象 (JSON) | Result\<Void\> | ProductPromotionController.updatePromotion | 编辑促销 |
| deletePromotion | DELETE | /api/product-promotions/{id} | 路径参数: id | Result\<Void\> | ProductPromotionController.deletePromotion | 删除促销 |
| getCategoryList | GET | /api/product-categories | params: {page, size} | Result\<PageInfo\<ProductCategory\>\> | ProductCategoryController.getCategoryList | 分页查询分类列表 |
| createCategory | POST | /api/product-categories | data: ProductCategory对象 (JSON) | Result\<Void\> | ProductCategoryController.addCategory | 新增分类 |
| updateCategory | PUT | /api/product-categories/{id} | 路径参数: id, data: ProductCategory对象 (JSON) | Result\<Void\> | ProductCategoryController.updateCategory | 编辑分类 |
| deleteCategory | DELETE | /api/product-categories/{id} | 路径参数: id | Result\<Void\> | ProductCategoryController.deleteCategory | 删除分类；存在商品或历史引用时返回 422 RESOURCE_IN_USE |

### 1.6 交易管理模块 (tran.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getTranList | GET | /api/tran/list | params: {page, size, ...query} | R\<PageInfo\<TTran\>\> | TranController.list | 分页查询交易列表 |
| getTranDetail | GET | /api/tran/{id} | 路径参数: id | R\<TTran\> | TranController.detail | 获取交易详情 |
| getTranProducts | GET | /api/tran/products/{id} | 路径参数: id | R\<List\<TTranProduct\>\> | TranController.getTransactionProducts | 获取交易产品历史快照列表；商品主档修改后名称、编码、配置和指导价不随之变化 |
| createTran | POST | /api/tran/create | data: TranCreateRequest对象 (JSON) | R\<Integer\> | TranController.create | 创建交易 |
| updateTran | PUT | /api/tran/update | data: TranCreateRequest对象 (JSON) | R\<Boolean\> | TranController.update | 更新交易 |
| fetchSettlementPreview | POST | /api/tran/{id}/settlement-preview | 路径参数: id, data: {promotionId?} | R\<SettlementPreviewResponse\> | TranController.settlementPreview | 服务端结算预览 |
| settleTran | PUT | /api/tran/{id}/settle | 路径参数: id, data: {promotionId?, expectedVersion, pricingFingerprint} | R\<SettlementPreviewResponse\> | TranController.settle | CAS 确认结算 |
| approveTran | PUT | /api/tran/approve/{id} | 路径参数: id, data: {approved, comment} (JSON) | R\<Boolean\> | TranController.approve | 审批交易 |
| getTranApprove | GET | /api/tran/approve/info/{tranId} | 路径参数: tranId | R\<TTranApprove\> | TranController.getApproveInfo | 获取审批信息 |
| createInvoice | POST | /api/tran/invoice | data: TTranInvoice对象 (JSON) | R\<Boolean\> | TranController.createInvoice | 创建发票 |
| getTranInvoiceList | GET | /api/tran/invoice/{tranId} | 路径参数: tranId | R\<List\<TTranInvoice\>\> | TranController.getInvoiceList | 获取交易发票列表 |
| updateInvoiceStatus | PUT | /api/tran/invoice/{invoiceId}/status | 路径参数: invoiceId, data: {status} (JSON) | R\<Boolean\> | TranController.updateInvoiceStatus | 更新发票状态 |
| deleteTran | DELETE | /api/tran/{id} | 路径参数: id | R\<String\> | TranController.delete | 删除交易 |
| batchDeleteTran | POST | /api/tran/batch-delete | data: {ids: List\<Integer\>} (JSON) | R\<String\> | TranController.batchDelete | 批量删除交易 |
| getTranStatus | GET | /api/tran/status/{id} | 路径参数: id | - | **后端未实现** | 前端调用但后端无对应接口 |

### 1.7 字典管理模块 (dict.js)

| 前端函数名 | HTTP方法 | 请求路径 | 请求参数格式 | 响应数据格式 | 后端Controller方法 | 处理逻辑概要 |
|-----------|---------|---------|-------------|-------------|-------------------|-------------|
| getDictTypeList | GET | /api/dict/types | params: {page, size, ...query} | R | DicController.getDicTypes | 分页查询字典类型 |
| getDictTypeDetail | GET | /api/dict/type/get/{id} | 路径参数: id | R | DicController.getDicTypeById | 获取字典类型详情 |
| createDictType | POST | /api/dict/type/create | data: TDicType对象 (JSON) | R | DicController.addDicType | 新增字典类型 |
| updateDictType | PUT | /api/dict/type/update/{id} | 路径参数: id, data: TDicType对象 (JSON) | R | DicController.updateDicType | 编辑字典类型 |
| deleteDictType | DELETE | /api/dict/type/delete/{id} | 路径参数: id | R | DicController.deleteDicType | 删除字典类型 |
| batchDeleteDictTypes | DELETE | /api/dict/types/batch | data: List\<Integer\> (JSON数组) | R | DicController.batchDeleteDicTypes | 批量删除字典类型 |
| getDictValueList | GET | /api/dict/values | params: {page, size, ...query} | R | DicController.getDicValues | 分页查询字典值 |
| getDictValueDetail | GET | /api/dict/value/get/{id} | 路径参数: id | R | DicController.getDicValueById | 获取字典值详情 |
| createDictValue | POST | /api/dict/value/create | data: TDicValue对象 (JSON) | R | DicController.addDicValue | 新增字典值 |
| updateDictValue | PUT | /api/dict/value/update/{id} | 路径参数: id, data: TDicValue对象 (JSON) | R | DicController.updateDicValue | 编辑字典值 |
| deleteDictValue | DELETE | /api/dict/value/delete/{id} | 路径参数: id | R | DicController.deleteDicValue | 删除字典值 |
| batchDeleteDictValues | DELETE | /api/dict/value/batch | data: List\<Integer\> (JSON数组) | R | DicController.batchDeleteDicValues | 批量删除字典值 |
| clearCache | GET | /api/dict/clear | params: {forceRefresh: true} | R | DicController.clearCache | 清除字典缓存 |

### 1.8 审计日志模块

审计日志已确认为后续正式模块，业务规格见 `docs/spec/审计日志/`。接口尚未实现，本文不声明可调用的审计日志 API。

旧系统配置与系统监控接口已下线，不再提供 `/api/system/*` 和 `/api/monitor/*`。

### 1.9 统计模块

| 后端接口路径 | HTTP方法 | 后端Controller方法 | 响应数据格式 | 前端调用情况 |
|-------------|---------|-------------------|-------------|-------------|
| /api/summary/data | GET | StatisticController.summaryData | R\<SummaryData\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |
| /api/saleFunnel/data | GET | StatisticController.saleFunnelData | R\<List\<NameValue\>\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |
| /api/sourcePie/data | GET | StatisticController.sourcePieData | R\<List\<NameValue\>\> | `dealer-web/src/modules/statistic/api/statistic-api.ts` |

统计接口不接收前端范围参数，后端按当前登录用户的数据范围聚合；非管理员统计结果必须能由同范围明细反算。

### 1.10 活动备注模块 (后端提供，前端未定义API文件)

| 后端接口路径 | HTTP方法 | 后端Controller方法 | 响应数据格式 | 前端调用情况 |
|-------------|---------|-------------------|-------------|-------------|
| /api/activity/remark | POST | ActivityRemarkController.addActivityRemark | R | **前端未发现API调用** |
| /api/activity/remark | GET | ActivityRemarkController.activityRemarkPage | R\<PageInfo\<TActivityRemark\>\> | **前端未发现API调用** |
| /api/activity/remark/{id} | GET | ActivityRemarkController.activityRemarkPage | R\<TActivityRemark\> | **前端未发现API调用** |
| /api/activity/remark | PUT | ActivityRemarkController.editActivityRemark | R | **前端未发现API调用** |
| /api/activity/remark/{id} | DELETE | ActivityRemarkController.delActivityRemark | R | **前端未发现API调用** |

---

## 2. 联调问题检查

### 2.1 路径匹配问题

| 问题类型 | 前端路径 | 后端路径 | 问题描述 | 严重程度 |
|---------|---------|---------|---------|---------|
| **路径不匹配** | /api/tran/status/{id} | 无对应接口 | 前端调用获取交易状态接口，后端未实现 | **高** |
| 路径命名不一致 | /api/activitys | /api/activitys | 前后端一致，但命名不规范(应为activities) | 低 |

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
| createActivity | FormData | FormData (无@RequestBody) | 一致 | - |
| updateActivity | FormData | FormData (无@RequestBody) | 一致 | - |
| addClue | FormData | FormData (无@RequestBody) | 一致 | - |
| updateClue | FormData | FormData (无@RequestBody) | 一致 | - |
| createUser | FormData | FormData (无@RequestBody) | 一致 | - |
| updateUser | FormData | FormData (无@RequestBody) | 一致 | - |
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
                    │  作为负载)   │   │ cdrm:user:   │   │ 否则:30分钟  │
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
| 6 | 设置过期时间 | rememberMe=true: 7天, rememberMe=false: 30分钟 |
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
        config.headers['Authorization'] = token;
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
| 普通模式 | rememberMe=false | redisService.expire() | 30分钟 |

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

Redis 删除成功后返回退出成功；删除失败或抛出异常时返回 HTTP 500 和 SYSTEM_ERROR，不得让前端误认为会话已经失效。

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
| Token传递 | 通过Authorization请求头 |

### 4.2 Excel导出流程 (客户导出)

```
┌─────────────┐     GET /api/exportExcel      ┌─────────────────┐
│   前端       │  ────────────────────────────►  │ CustomerController│
│              │  params: {ids: "1,2,3"}        │ .exportExcel()  │
│              │  params: {Authorization: jwt}   │                 │
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
| 认证方式 | 通过 Authorization 请求头传递 Token |
| 响应类型 | application/vnd.openxmlformats-officedocument.spreadsheetml.sheet |
| 响应头 | Content-disposition: attachment; filename*=UTF-8''<URL编码文件名>.xlsx |
| 权限要求 | @PreAuthorize("hasAuthority('customer:export')") |
| 前端实现 | `httpClient.download()` 以 Blob 接收，解析文件名后 `saveBlob` 触发下载 |

### 4.3 Token在文件操作中的处理

Token 统一通过 `Authorization` 请求头传递，包括文件下载。`TokenVerifyFilter` 只从请求头读取 Token，不再从 URL 参数获取。前端 `httpClient.download()` 复用 `axiosClient` 的请求拦截器自动注入 Token，不再将 Token 放入 URL。

---

## 5. 分页参数传递方式

### 5.1 前端分页参数传递

| 前端函数 | 参数名 | 传递方式 | 示例 |
|---------|-------|---------|------|
| getUserList | current | Query参数 | /api/users?current=1 |
| getCurrentClues | current | Query参数 | /api/clues?current=1 |
| getActivityList | current | Query参数 | /api/activitys?current=1 |
| getCustomerList | page, size | Query参数 | /api/customer/list?page=1&size=10 |
| getProductList | page, size | Query参数 | /api/products?page=1&size=10 |
| getTranList | page, size | Query参数 | /api/tran/list?page=1&size=10 |
| getDictTypeList | page, size | Query参数 | /api/dict/types?page=1&size=10 |
| getClueRemarkList | current | Query参数 | /api/clue/remark?current=1&clueId=1 |

### 5.2 后端分页参数接收

| 后端Controller | 参数名 | 接收方式 | 默认值 | 分页实现 |
|---------------|-------|---------|-------|---------|
| UserController.userPage | current | @RequestParam | 1 | PageHelper |
| ClueController.cluePage | current | @RequestParam | 1 | PageHelper |
| ActivityController.activityPage | current | @RequestParam | 1 | PageHelper |
| CustomerController.list | page, size | @RequestParam | 1, 10 | PageHelper |
| ProductController.getProductList | page, size | @RequestParam | 1, 10 | PageHelper |
| TranController.list | page, size | @RequestParam | 1, 10 | PageHelper |
| DicController.getDicTypes | page, size | DicQuery对象 | 1, 10 | PageHelper |
| ClueRemarkController.clueRemarkPage | current | @RequestParam | 1 | PageHelper |

### 5.3 分页参数不一致问题

| 模块 | 前端参数 | 后端参数 | 问题描述 | 严重程度 |
|-----|---------|---------|---------|---------|
| 用户/线索/活动 | current | current | 一致 | - |
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

---

*文档生成时间: 2026-05-30*
*分析工具: opencode*
