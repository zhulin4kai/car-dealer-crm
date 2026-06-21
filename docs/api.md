# 汽车经销商 CRM 系统 API 文档

## 基础信息

### Base URL
```
http://localhost:8089
```

### 认证方式
系统使用 **Bearer Token (JWT)** 进行认证。登录成功后获取 token，后续请求需要在 Header 中携带：
```
Authorization: Bearer <token>
```

### 统一响应格式

#### R 格式（主要格式）
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

#### Result 格式（商品相关接口）
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

### 分页参数约定

| 参数名 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| current | Integer | 1 | 当前页码 |
| pageSize | Integer | 10 | 每页条数 |
| page | Integer | 1 | 当前页码（商品相关） |
| size | Integer | 10 | 每页条数（商品相关） |

### 分页响应结构
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "list": []
  }
}
```

### 错误码对照表

所有接口错误均通过 `GlobalExceptionHandler` 统一处理，返回结构化 `R.FAIL` 响应，code 值来源于 `CodeEnum`。

| code | CodeEnum 名称 | 说明 |
|------|--------------|------|
| 200 | OK | 操作成功 |
| 401 | — | 未认证 / Token 失效（Spring Security 拦截） |
| 403 | — | 无权限访问（Spring Security 拦截） |
| 404 | NOT_FOUND | 资源不存在 |
| 409 | DUPLICATE | 数据已存在（唯一约束冲突） |
| 422 | RESOURCE_IN_USE | 资源被引用，无法操作 |
| 500 | FAIL | 操作失败（通用业务错误） |
| 501 | PARAM_ERROR | 请求参数格式有误（校验失败或请求体格式错误） |
| 506 | SYSTEM_ERROR | 系统异常（未预期的运行时错误） |
| 507 | TRAN_NO_PRODUCTS | 交易没有产品信息 |
| 510 | TOKEN_IS_EMPTY | Token 为空 |
| 511 | TOKEN_IS_ERROR | Token 无效 |
| 512 | TOKEN_IS_EXPIRED | Token 已过期 |
| 513 | TOKEN_IS_NONE_MATCH | Token 不匹配 |
| 520 | ACCESS_DENIED | 没有访问权限 |
| 521 | DATA_ACCESS_EXCEPTION | 数据访问异常 |
| 550 | OPERATION_FAILED | 业务操作失败 |

> **P1-3 说明**：所有 Service 层业务异常统一抛出 `BusinessException(CodeEnum, msg)`，由 `GlobalExceptionHandler` 捕获并返回 `R.FAIL(code, msg)` 格式响应。不再使用原始 `RuntimeException`。

---

## 1. 认证模块

### 1.1 获取登录用户信息
- **URL**: `/api/login/info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取当前登录用户的详细信息

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "loginAct": "admin",
    "name": "管理员",
    "phone": "13800138000",
    "email": "admin@example.com",
    "accountNoExpired": 1,
    "credentialsNoExpired": 1,
    "accountNoLocked": 1,
    "accountEnabled": 1,
    "createTime": "2024-01-01 00:00:00",
    "lastLoginTime": "2024-01-15 10:30:00",
    "roleList": ["admin"],
    "permissionList": ["user:list", "user:add", "clue:list"],
    "menuPermissionList": []
  }
}
```

### 1.2 免登录验证
- **URL**: `/api/login/free`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 验证用户登录状态是否有效

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 1.3 获取负责人列表
- **URL**: `/api/owner`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取所有可选的负责人列表（用于下拉选择）

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "张三",
      "phone": "13800138000"
    },
    {
      "id": 2,
      "name": "李四",
      "phone": "13900139000"
    }
  ]
}
```

---

## 2. 用户管理

### 2.1 获取用户列表
- **URL**: `/api/users`
- **Method**: GET
- **权限**: 需要权限 `user:list`
- **描述**: 分页查询用户列表

**请求参数**（对应 `UserListQuery` DTO）:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |
| loginAct | String | 否 | 登录账号（模糊查询） |
| name | String | 否 | 用户姓名（模糊查询） |
| phone | String | 否 | 手机号（模糊查询） |
| email | String | 否 | 邮箱（模糊查询） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 50,
    "list": [
      {
        "id": 1,
        "loginAct": "admin",
        "name": "管理员",
        "phone": "13800138000",
        "email": "admin@example.com",
        "accountEnabled": 1,
        "createTime": "2024-01-01 00:00:00"
      }
    ]
  }
}
```

### 2.2 获取用户详情
- **URL**: `/api/user/{id}`
- **Method**: GET
- **权限**: 需要权限 `user:view`
- **描述**: 获取指定用户的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "loginAct": "admin",
    "name": "管理员",
    "phone": "13800138000",
    "email": "admin@example.com",
    "accountNoExpired": 1,
    "credentialsNoExpired": 1,
    "accountNoLocked": 1,
    "accountEnabled": 1,
    "createTime": "2024-01-01 00:00:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1,
    "lastLoginTime": "2024-01-15 10:30:00",
    "roleList": ["admin"],
    "permissionList": ["user:list"]
  }
}
```

### 2.3 新增用户
- **URL**: `/api/user`
- **Method**: POST
- **权限**: 需要权限 `user:add`
- **描述**: 创建新用户

**请求体**（对应 `CreateUserRequest` DTO）:
```json
{
  "loginAct": "user001",
  "loginPwd": "123456",
  "name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| loginAct | String | 是 | 登录账号（1-32位） |
| loginPwd | String | 是 | 登录密码（6-16位） |
| name | String | 是 | 用户姓名（1-32位） |
| phone | String | 是 | 手机号 |
| email | String | 是 | 邮箱 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 10,
    "loginAct": "user001",
    "name": "张三",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "accountNoExpired": 1,
    "credentialsNoExpired": 1,
    "accountNoLocked": 1,
    "accountEnabled": 1,
    "createTime": "2024-01-15 10:30:00"
  }
}
```

### 2.4 编辑用户
- **URL**: `/api/user`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 更新用户信息

**请求体**（对应 `UpdateUserRequest` DTO）:
```json
{
  "id": 1,
  "loginAct": "user001",
  "name": "张三（已修改）",
  "phone": "13800138000",
  "email": "zhangsan@example.com"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |
| loginAct | String | 是 | 登录账号（1-32位） |
| name | String | 是 | 用户姓名（1-32位） |
| phone | String | 是 | 手机号 |
| email | String | 是 | 邮箱 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "loginAct": "user001",
    "name": "张三（已修改）",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "accountNoExpired": 1,
    "credentialsNoExpired": 1,
    "accountNoLocked": 1,
    "accountEnabled": 1,
    "createTime": "2024-01-01 00:00:00",
    "editTime": "2024-01-15 11:00:00"
  }
}
```

### 2.5 禁用用户
- **URL**: `/api/user/{id}/disable`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 禁用指定用户（将 accountEnabled 设为 0）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.6 启用用户
- **URL**: `/api/user/{id}/enable`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 启用指定用户（将 accountEnabled 设为 1）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.7 锁定用户
- **URL**: `/api/user/{id}/lock`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 锁定指定用户（将 accountNoLocked 设为 0）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.8 解锁用户
- **URL**: `/api/user/{id}/unlock`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 解锁指定用户（将 accountNoLocked 设为 1）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.9 批量禁用用户
- **URL**: `/api/users/batch-disable`
- **Method**: PUT
- **权限**: 需要权限 `user:delete`
- **描述**: 批量禁用指定用户（替代原 DELETE 删除操作，改为逻辑禁用）

**请求体**（对应 `BatchDisableUsersRequest` DTO）:
```json
{
  "ids": [1, 2, 3]
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | List\<Integer\> | 是 | 用户ID列表，不能为空 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

> **P1-5 变更说明**：原 `DELETE /api/user/{id}` 和 `DELETE /api/user`（批量）已移除。用户不再物理删除，改为通过禁用/启用管理生命周期。

### 2.10 分配用户角色
- **URL**: `/api/user/{id}/roles`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 为指定用户分配角色

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**请求体**（对应 `AssignUserRolesRequest` DTO）:
```json
{
  "roleIds": [1, 2, 3]
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| roleIds | List\<Integer\> | 否 | 角色ID列表（空列表表示清除所有角色） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.11 修改用户密码
- **URL**: `/api/user/{id}/password`
- **Method**: PUT
- **权限**: 需要权限 `user:edit`
- **描述**: 修改指定用户的登录密码

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 用户ID |

**请求体**（对应 `ChangePasswordRequest` DTO）:
```json
{
  "newPassword": "newpass123"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| newPassword | String | 是 | 新密码（6-16位） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 3. 线索管理

### 3.1 获取线索列表
- **URL**: `/api/clues`
- **Method**: GET
- **权限**: 需要权限 `clue:list`
- **描述**: 分页查询线索列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 100,
    "list": [
      {
        "id": 1,
        "ownerId": 1,
        "activityId": 1,
        "fullName": "张三",
        "appellation": 1,
        "phone": "13800138000",
        "weixin": "zhangsan",
        "email": "zhangsan@example.com",
        "state": 1,
        "source": 1,
        "description": "有购车意向",
        "createTime": "2024-01-15 10:30:00",
        "ownerDO": {
          "id": 1,
          "name": "李四"
        },
        "activityDO": {
          "id": 1,
          "name": "春节促销"
        }
      }
    ]
  }
}
```

### 3.2 获取线索详情
- **URL**: `/api/clue/detail/{id}`
- **Method**: GET
- **权限**: 需要权限 `clue:view`
- **描述**: 获取指定线索的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 线索ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "ownerId": 1,
    "activityId": 1,
    "fullName": "张三",
    "appellation": 1,
    "phone": "13800138000",
    "weixin": "zhangsan",
    "qq": "12345678",
    "email": "zhangsan@example.com",
    "age": 30,
    "job": "工程师",
    "yearIncome": 200000,
    "address": "北京市朝阳区",
    "needLoan": 1,
    "intentionState": 1,
    "intentionProduct": 1,
    "state": 1,
    "source": 1,
    "description": "有购车意向",
    "nextContactTime": "2024-01-20 10:00:00",
    "createTime": "2024-01-15 10:30:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1
  }
}
```

### 3.3 新增线索
- **URL**: `/api/clue`
- **Method**: POST
- **权限**: 需要权限 `clue:add`
- **描述**: 创建新线索

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ownerId | Integer | 否 | 线索所属人ID |
| activityId | Integer | 否 | 活动ID |
| fullName | String | 是 | 姓名 |
| appellation | Integer | 否 | 称呼（字典值） |
| phone | String | 是 | 手机号 |
| weixin | String | 否 | 微信号 |
| qq | String | 否 | QQ号 |
| email | String | 否 | 邮箱 |
| age | Integer | 否 | 年龄 |
| job | String | 否 | 职业 |
| yearIncome | BigDecimal | 否 | 年收入 |
| address | String | 否 | 地址 |
| needLoan | Integer | 否 | 是否需要贷款（0不需要 1需要） |
| intentionState | Integer | 否 | 意向状态（字典值） |
| intentionProduct | Integer | 否 | 意向产品（产品ID） |
| state | Integer | 否 | 线索状态（字典值） |
| source | Integer | 否 | 线索来源（字典值） |
| description | String | 否 | 线索描述 |
| nextContactTime | String | 否 | 下次联系时间（yyyy-MM-dd HH:mm:ss） |

**请求示例**:
```json
fullName=张三&phone=13800138000&activityId=1&appellation=1&source=1
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 3.4 编辑线索
- **URL**: `/api/clue`
- **Method**: PUT
- **权限**: 需要权限 `clue:edit`
- **描述**: 更新线索信息

**请求参数**: 同新增线索，需额外传递 `id`

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 3.5 删除线索
- **URL**: `/api/clue/{id}`
- **Method**: DELETE
- **权限**: 需要权限 `clue:delete`
- **描述**: 删除指定线索

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 线索ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 3.6 批量删除线索
- **URL**: `/api/clue/batch`
- **Method**: POST
- **权限**: 需要权限 `clue:delete`
- **描述**: 批量删除线索

**请求体**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 3.7 导入线索 Excel
- **URL**: `/api/importExcel`
- **Method**: POST
- **权限**: 需要权限 `clue:import`
- **描述**: 通过 Excel 文件批量导入线索

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| file | MultipartFile | 是 | Excel文件 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 3.8 手机号查重
- **URL**: `/api/clue/{phone}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 检查手机号是否已存在

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| phone | String | 是 | 手机号 |

**响应示例**:
```json
// 手机号可用
{
  "code": 200,
  "msg": "success",
  "data": null
}

// 手机号已存在
{
  "code": 500,
  "msg": "手机号已存在",
  "data": null
}
```

---

## 4. 线索备注

### 4.1 获取线索备注列表
- **URL**: `/api/clue/remark`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询指定线索的备注列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |
| clueId | Integer | 是 | 线索ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "clueId": 1,
        "noteWay": 1,
        "noteContent": "电话联系客户，客户有购车意向",
        "createTime": "2024-01-15 10:30:00",
        "createBy": 1,
        "createByDO": {
          "id": 1,
          "name": "李四"
        },
        "noteWayDO": {
          "id": 1,
          "typeValue": "电话"
        }
      }
    ]
  }
}
```

### 4.2 新增线索备注
- **URL**: `/api/clue/remark`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 为指定线索添加备注

**请求体**:
```json
{
  "clueId": 1,
  "noteContent": "电话联系客户，客户有购车意向",
  "noteWay": 1
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| clueId | Integer | 是 | 线索ID |
| noteContent | String | 是 | 备注内容 |
| noteWay | Integer | 否 | 跟踪方式（字典值） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 5. 客户管理

### 5.1 获取客户列表（新）
- **URL**: `/api/customer/list`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询客户列表（包含线索名称）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |
| customerName | String | 否 | 客户名称（模糊查询） |
| productId | Integer | 否 | 产品ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 50,
    "list": [
      {
        "id": 1,
        "clueId": 1,
        "product": 1,
        "description": "意向客户",
        "nextContactTime": "2024-01-20 10:00:00",
        "createTime": "2024-01-15 10:30:00",
        "clueDO": {
          "id": 1,
          "fullName": "张三",
          "phone": "13800138000"
        }
      }
    ]
  }
}
```

### 5.2 获取客户选项
- **URL**: `/api/customer/options`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取客户下拉选项列表

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "name": "张三",
      "phone": "13800138000"
    }
  ]
}
```

### 5.3 获取客户详情
- **URL**: `/api/customer/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定客户的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 客户ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "clueId": 1,
    "product": 1,
    "description": "意向客户",
    "nextContactTime": "2024-01-20 10:00:00",
    "createTime": "2024-01-15 10:30:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1,
    "clueDO": {
      "id": 1,
      "fullName": "张三",
      "phone": "13800138000",
      "weixin": "zhangsan"
    },
    "ownerDO": {
      "id": 1,
      "name": "李四"
    }
  }
}
```

### 5.4 线索转客户
- **URL**: `/api/clue/customer`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 将线索转换为客户

**请求体**:
```json
{
  "clueId": 1,
  "product": 1,
  "description": "意向客户",
  "nextContactTime": "2024-01-20 10:00:00"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| clueId | Integer | 是 | 线索ID |
| product | Integer | 否 | 选购产品ID |
| description | String | 否 | 客户描述 |
| nextContactTime | String | 否 | 下次联系时间 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 5.5 获取客户列表（旧）
- **URL**: `/api/customers`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询客户列表（兼容旧接口）

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |

### 5.6 导出客户 Excel
- **URL**: `/api/exportExcel`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 导出客户数据为 Excel 文件

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ids | String | 否 | 客户ID列表，逗号分隔（为空则导出全部） |

**响应**: 直接返回 Excel 文件流

---

## 6. 交易管理

### 6.1 获取交易列表
- **URL**: `/api/tran/list`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询交易列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |
| tranNo | String | 否 | 交易编号 |
| customerId | Integer | 否 | 客户ID |
| customerName | String | 否 | 客户名称 |
| stage | Integer | 否 | 交易阶段 |
| minMoney | BigDecimal | 否 | 最小金额 |
| maxMoney | BigDecimal | 否 | 最大金额 |
| createBy | Integer | 否 | 创建人ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 50,
    "list": [
      {
        "id": 1,
        "tranNo": "20240115001",
        "customerId": 1,
        "customerName": "张三",
        "money": 150000,
        "stage": 41,
        "description": "购买SUV",
        "createTime": "2024-01-15 10:30:00",
        "createBy": 1
      }
    ]
  }
}
```

**交易阶段说明**:
| stage | 说明 |
|-------|------|
| 41 | 待报价 |
| 42 | 待审批 |
| 43 | 已通过 |
| 44 | 已拒绝 |
| 45 | 已成交 |
| 46 | 已取消 |

### 6.2 获取交易详情
- **URL**: `/api/tran/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定交易的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "tranNo": "20240115001",
    "customerId": 1,
    "money": 150000,
    "expectedDate": "2024-02-15 00:00:00",
    "stage": 41,
    "description": "购买SUV",
    "nextContactTime": "2024-01-20 10:00:00",
    "createTime": "2024-01-15 10:30:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1
  }
}
```

### 6.3 创建交易
- **URL**: `/api/tran/create`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新交易

**请求体**:
```json
{
  "customerId": 1,
  "amount": 150000,
  "description": "购买SUV",
  "expectedDeliveryDate": "2024-02-15 00:00:00",
  "products": [
    {
      "productId": 1,
      "quantity": 1,
      "price": 150000
    }
  ]
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| customerId | Integer | 是 | 客户ID |
| amount | BigDecimal | 否 | 交易金额（可自动计算） |
| description | String | 否 | 交易描述 |
| expectedDeliveryDate | String | 否 | 预计交付日期（yyyy-MM-dd HH:mm:ss） |
| products | Array | 是 | 产品列表 |
| products[].productId | Integer | 是 | 产品ID |
| products[].quantity | Integer | 是 | 数量 |
| products[].price | BigDecimal | 是 | 单价 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": 1
}
```

### 6.4 更新交易
- **URL**: `/api/tran/update`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新交易信息

**请求体**:
```json
{
  "id": 1,
  "customerId": 1,
  "amount": 150000,
  "description": "购买SUV（已更新）",
  "expectedDeliveryDate": "2024-02-20 00:00:00",
  "products": [
    {
      "productId": 1,
      "quantity": 1,
      "price": 150000
    }
  ]
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |
| 其他参数 | - | - | 同创建交易 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

### 6.5 结算交易
- **URL**: `/api/tran/settle/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 结算交易，计算总金额并更新状态为待审批

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |

**请求体**（可选）:
```json
{
  "amount": 150000
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| amount | BigDecimal | 否 | 结算金额（不传则自动计算） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

### 6.6 审批交易
- **URL**: `/api/tran/approve/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 审批交易（通过或拒绝）

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |

**请求体**:
```json
{
  "approved": true,
  "comment": "审批通过"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| approved | Boolean | 是 | 审批结果（true通过 false拒绝） |
| comment | String | 是 | 审批意见 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

### 6.7 获取交易审批信息
- **URL**: `/api/tran/approve/info/{tranId}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取交易的审批记录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tranId | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "tranId": 1,
    "approved": true,
    "comment": "审批通过",
    "createBy": 1,
    "createTime": "2024-01-15 10:30:00"
  }
}
```

### 6.8 创建发票
- **URL**: `/api/tran/invoice`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 为交易创建发票

**请求体**:
```json
{
  "tranId": 1,
  "invoiceNo": "INV-2024-001",
  "amount": 150000,
  "status": "待开票",
  "remark": "增值税专用发票"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tranId | Integer | 是 | 交易ID |
| invoiceNo | String | 否 | 发票编号 |
| amount | BigDecimal | 是 | 发票金额 |
| status | String | 否 | 发票状态 |
| remark | String | 否 | 备注 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

### 6.9 获取交易发票列表
- **URL**: `/api/tran/invoice/{tranId}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定交易的发票列表

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tranId | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "tranId": 1,
      "invoiceNo": "INV-2024-001",
      "amount": 150000,
      "status": "已开票",
      "createTime": "2024-01-15 10:30:00"
    }
  ]
}
```

### 6.10 更新发票状态
- **URL**: `/api/tran/invoice/{invoiceId}/status`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新发票状态

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| invoiceId | Integer | 是 | 发票ID |

**请求体**:
```json
{
  "status": "已开票"
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": true
}
```

### 6.11 获取交易备注
- **URL**: `/api/tran/remarks/{tranId}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定交易的备注列表

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| tranId | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "tranId": 1,
      "noteContent": "客户已确认购买",
      "createTime": "2024-01-15 10:30:00",
      "createBy": 1
    }
  ]
}
```

### 6.12 获取交易产品详情
- **URL**: `/api/tran/products/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定交易的产品列表

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "tranId": 1,
      "productId": 1,
      "productName": "SUV 2024款",
      "quantity": 1,
      "price": 150000,
      "createTime": "2024-01-15 10:30:00",
      "createBy": 1
    }
  ]
}
```

### 6.13 删除交易
- **URL**: `/api/tran/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定交易

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 交易ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "删除成功",
  "data": "删除成功"
}
```

### 6.14 批量删除交易
- **URL**: `/api/tran/batch-delete`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 批量删除交易

**请求体**:
```json
{
  "ids": [1, 2, 3]
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "批量删除成功",
  "data": "批量删除成功"
}
```

---

## 7. 市场活动

### 7.1 获取活动列表
- **URL**: `/api/activitys`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询市场活动列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |
| name | String | 否 | 活动名称（模糊查询） |
| ownerId | Integer | 否 | 活动所属人ID |
| startTime | String | 否 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间（yyyy-MM-dd HH:mm:ss） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 20,
    "list": [
      {
        "id": 1,
        "ownerId": 1,
        "name": "春节促销活动",
        "startTime": "2024-01-01 00:00:00",
        "endTime": "2024-02-01 00:00:00",
        "cost": 50000,
        "description": "春节期间购车优惠",
        "createTime": "2024-01-01 00:00:00",
        "createBy": 1,
        "ownerDO": {
          "id": 1,
          "name": "李四"
        }
      }
    ]
  }
}
```

### 7.2 获取活动详情
- **URL**: `/api/activity/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定活动的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 活动ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "ownerId": 1,
    "name": "春节促销活动",
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-02-01 00:00:00",
    "cost": 50000,
    "description": "春节期间购车优惠",
    "createTime": "2024-01-01 00:00:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1
  }
}
```

### 7.3 新增活动
- **URL**: `/api/activity`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新市场活动

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| ownerId | Integer | 否 | 活动所属人ID |
| name | String | 是 | 活动名称 |
| startTime | String | 否 | 开始时间（yyyy-MM-dd HH:mm:ss） |
| endTime | String | 否 | 结束时间（yyyy-MM-dd HH:mm:ss） |
| cost | BigDecimal | 否 | 活动预算 |
| description | String | 否 | 活动描述 |

**请求示例**:
```json
name=春节促销活动&startTime=2024-01-01 00:00:00&endTime=2024-02-01 00:00:00&cost=50000
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 7.4 编辑活动
- **URL**: `/api/activity`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新市场活动信息

**请求参数**: 同新增活动，需额外传递 `id`

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 7.5 删除活动
- **URL**: `/api/activity/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定市场活动

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 活动ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 7.6 批量删除活动
- **URL**: `/api/activity/batch`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 批量删除市场活动

**请求体**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 8. 活动备注

### 8.1 获取活动备注列表
- **URL**: `/api/activity/remark`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询指定活动的备注列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| current | Integer | 否 | 当前页码，默认1 |
| activityId | Integer | 是 | 活动ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "activityId": 1,
        "noteContent": "活动筹备中",
        "createTime": "2024-01-15 10:30:00",
        "createBy": 1,
        "createByDO": {
          "id": 1,
          "name": "李四"
        }
      }
    ]
  }
}
```

### 8.2 获取活动备注详情
- **URL**: `/api/activity/remark/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定活动备注的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 备注ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "activityId": 1,
    "noteContent": "活动筹备中",
    "createTime": "2024-01-15 10:30:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1
  }
}
```

### 8.3 新增活动备注
- **URL**: `/api/activity/remark`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 为指定活动添加备注

**请求体**:
```json
{
  "activityId": 1,
  "noteContent": "活动筹备中，预计下周开始宣传"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| activityId | Integer | 是 | 活动ID |
| noteContent | String | 是 | 备注内容 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 8.4 编辑活动备注
- **URL**: `/api/activity/remark`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新活动备注

**请求体**:
```json
{
  "id": 1,
  "activityId": 1,
  "noteContent": "活动筹备已完成"
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 8.5 删除活动备注
- **URL**: `/api/activity/remark/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定活动备注

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 备注ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 9. 商品管理

### 9.1 获取商品列表
- **URL**: `/api/products`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询商品列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 50,
    "list": [
      {
        "id": 1,
        "sku": "SKU001",
        "name": "SUV 2024款",
        "category": "SUV",
        "specification": "2.0T 自动挡",
        "price": 150000,
        "stock": 10,
        "minStock": 5,
        "status": "上架",
        "createTime": "2024-01-01 00:00:00",
        "updateTime": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

### 9.2 获取商品详情
- **URL**: `/api/products/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定商品的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "sku": "SKU001",
    "name": "SUV 2024款",
    "category": "SUV",
    "specification": "2.0T 自动挡",
    "price": 150000,
    "stock": 10,
    "minStock": 5,
    "status": "上架",
    "createTime": "2024-01-01 00:00:00",
    "updateTime": "2024-01-15 10:30:00"
  }
}
```

### 9.3 新增商品
- **URL**: `/api/products`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新商品

**请求体**:
```json
{
  "sku": "SKU002",
  "name": "轿车 2024款",
  "category": "轿车",
  "specification": "1.5T 自动挡",
  "price": 120000,
  "stock": 20,
  "minStock": 5,
  "status": "上架"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| sku | String | 是 | 库存单位 |
| name | String | 是 | 商品名称 |
| category | String | 否 | 商品类别 |
| specification | String | 否 | 商品规格 |
| price | BigDecimal | 是 | 商品价格 |
| stock | Integer | 否 | 库存数量 |
| minStock | Integer | 否 | 最低库存警戒值 |
| status | String | 否 | 商品状态 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 9.4 编辑商品
- **URL**: `/api/products/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新商品信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID |

**请求体**: 同新增商品

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 9.5 删除商品
- **URL**: `/api/products/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定商品

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 商品ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 9.6 获取库存预警列表
- **URL**: `/api/products/stockalerts`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取库存低于警戒值的商品列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |
| sku | String | 否 | SKU编码 |
| name | String | 否 | 商品名称 |
| category | String | 否 | 商品类别 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "sku": "SKU001",
        "name": "SUV 2024款",
        "stock": 3,
        "minStock": 5,
        "status": "库存不足"
      }
    ]
  }
}
```

---

## 10. 商品分类

### 10.1 获取分类列表
- **URL**: `/api/product-categories`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询商品分类列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "name": "SUV",
        "code": "SUV",
        "description": "运动型多用途汽车",
        "sort": 1,
        "status": "启用",
        "createTime": "2024-01-01 00:00:00",
        "updateTime": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

### 10.2 获取分类详情
- **URL**: `/api/product-categories/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定分类的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 分类ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "name": "SUV",
    "code": "SUV",
    "description": "运动型多用途汽车",
    "sort": 1,
    "status": "启用",
    "createTime": "2024-01-01 00:00:00",
    "updateTime": "2024-01-15 10:30:00"
  }
}
```

### 10.3 新增分类
- **URL**: `/api/product-categories`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新商品分类

**请求体**:
```json
{
  "name": "轿车",
  "code": "SEDAN",
  "description": "轿车类型",
  "sort": 2,
  "status": "启用"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 分类名称 |
| code | String | 是 | 分类编码 |
| description | String | 否 | 分类描述 |
| sort | Integer | 否 | 排序号 |
| status | String | 否 | 状态 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 10.4 编辑分类
- **URL**: `/api/product-categories/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新商品分类信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 分类ID |

**请求体**: 同新增分类

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 10.5 删除分类
- **URL**: `/api/product-categories/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定商品分类

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 分类ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

## 11. 商品促销

### 11.1 获取促销列表
- **URL**: `/api/product-promotions`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询促销活动列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "name": "春节特惠",
        "type": "折扣",
        "discount": 0.85,
        "startTime": "2024-01-01 00:00:00",
        "endTime": "2024-02-01 00:00:00",
        "status": "进行中",
        "createTime": "2024-01-01 00:00:00",
        "updateTime": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

### 11.2 获取促销详情
- **URL**: `/api/product-promotions/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定促销活动的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 促销ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "id": 1,
    "name": "春节特惠",
    "type": "折扣",
    "discount": 0.85,
    "startTime": "2024-01-01 00:00:00",
    "endTime": "2024-02-01 00:00:00",
    "status": "进行中",
    "createTime": "2024-01-01 00:00:00",
    "updateTime": "2024-01-15 10:30:00"
  }
}
```

### 11.3 新增促销
- **URL**: `/api/product-promotions`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新促销活动

**请求体**:
```json
{
  "name": "元宵节特惠",
  "type": "满减",
  "discount": 5000,
  "startTime": "2024-02-10 00:00:00",
  "endTime": "2024-02-15 00:00:00",
  "status": "未开始"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| name | String | 是 | 促销名称 |
| type | String | 否 | 促销类型（折扣/满减等） |
| discount | BigDecimal | 否 | 折扣值 |
| startTime | String | 否 | 开始时间 |
| endTime | String | 否 | 结束时间 |
| status | String | 否 | 状态 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 11.4 编辑促销
- **URL**: `/api/product-promotions/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新促销活动信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 促销ID |

**请求体**: 同新增促销

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 11.5 删除促销
- **URL**: `/api/product-promotions/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定促销活动

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Long | 是 | 促销ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

---

## 12. 库存管理

### 12.1 补货
- **URL**: `/api/productstock/restock`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 为商品补充库存

**请求体**:
```json
{
  "productId": 1,
  "quantity": 10,
  "remark": "常规补货"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productId | Long | 是 | 商品ID |
| quantity | Integer | 是 | 补货数量 |
| remark | String | 否 | 备注 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": null
}
```

### 12.2 获取库存变动记录
- **URL**: `/api/productstock/records/{productId}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定商品的库存变动记录

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| productId | Long | 是 | 商品ID |

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "productId": 1,
        "quantity": 10,
        "type": "入库",
        "remark": "常规补货",
        "createTime": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

---

## 13. 字典管理

### 13.1 获取字典类型列表
- **URL**: `/api/dict/types`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询字典类型列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |
| typeName | String | 否 | 类型名称（模糊查询） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 10,
    "list": [
      {
        "id": 1,
        "typeCode": "appellation",
        "typeName": "称呼",
        "remark": "客户称呼"
      }
    ]
  }
}
```

### 13.2 获取字典类型详情
- **URL**: `/api/dict/type/get/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定字典类型的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典类型ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "typeCode": "appellation",
    "typeName": "称呼",
    "remark": "客户称呼"
  }
}
```

### 13.3 新增字典类型
- **URL**: `/api/dict/type/create`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新字典类型

**请求体**:
```json
{
  "typeCode": "source",
  "typeName": "来源",
  "remark": "线索来源"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| typeCode | String | 是 | 类型代码 |
| typeName | String | 是 | 类型名称 |
| remark | String | 否 | 备注 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.4 编辑字典类型
- **URL**: `/api/dict/type/update/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新字典类型信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典类型ID |

**请求体**: 同新增字典类型

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.5 删除字典类型
- **URL**: `/api/dict/type/delete/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定字典类型

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典类型ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.6 批量删除字典类型
- **URL**: `/api/dict/types/batch`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 批量删除字典类型

**请求体**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.7 获取字典值列表
- **URL**: `/api/dict/values`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 分页查询字典值列表

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| page | Integer | 否 | 当前页码，默认1 |
| size | Integer | 否 | 每页条数，默认10 |
| typeCode | String | 否 | 字典类型代码 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "pageNum": 1,
    "pageSize": 10,
    "total": 5,
    "list": [
      {
        "id": 1,
        "typeCode": "appellation",
        "typeValue": "先生",
        "order": 1,
        "remark": null
      }
    ]
  }
}
```

### 13.8 获取字典值详情
- **URL**: `/api/dict/value/get/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定字典值的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典值ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "typeCode": "appellation",
    "typeValue": "先生",
    "order": 1,
    "remark": null
  }
}
```

### 13.9 新增字典值
- **URL**: `/api/dict/value/create`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新字典值

**请求体**:
```json
{
  "typeCode": "appellation",
  "typeValue": "女士",
  "order": 2,
  "remark": null
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| typeCode | String | 是 | 字典类型代码 |
| typeValue | String | 是 | 字典值 |
| order | Integer | 否 | 排序号 |
| remark | String | 否 | 备注 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.10 编辑字典值
- **URL**: `/api/dict/value/update/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新字典值信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典值ID |

**请求体**: 同新增字典值

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.11 删除字典值
- **URL**: `/api/dict/value/delete/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定字典值

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 字典值ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.12 批量删除字典值
- **URL**: `/api/dict/value/batch`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 批量删除字典值

**请求体**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.13 清除字典缓存
- **URL**: `/api/dict/clear`
- **Method**: GET
- **权限**: 需要权限 `admin`
- **描述**: 清除字典缓存并可选重新加载

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| forceRefresh | Boolean | 否 | 是否强制刷新缓存 |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 13.14 刷新字典数据
- **URL**: `/api/dict/refresh`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 刷新字典数据缓存

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| type | String | 否 | 刷新类型（type/value/不传刷新全部） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 14. 系统管理

### 14.1 获取系统配置列表
- **URL**: `/api/system/list`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取所有系统配置列表

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "systemCode": "CRM",
      "name": "汽车经销商CRM系统",
      "site": "http://localhost:8089",
      "logo": "/logo.png",
      "title": "汽车经销商CRM",
      "description": "汽车经销商客户关系管理系统",
      "keywords": "CRM,汽车,经销商",
      "tel": "400-123-4567",
      "weixin": "car-dealer-crm",
      "email": "support@example.com",
      "address": "北京市朝阳区",
      "version": "1.0.0",
      "isopen": "1"
    }
  ]
}
```

### 14.2 获取系统配置详情
- **URL**: `/api/system/{id}`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取指定系统配置的详细信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 系统配置ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "systemCode": "CRM",
    "name": "汽车经销商CRM系统",
    "site": "http://localhost:8089",
    "logo": "/logo.png",
    "title": "汽车经销商CRM",
    "description": "汽车经销商客户关系管理系统",
    "keywords": "CRM,汽车,经销商",
    "shortcuticon": "/favicon.ico",
    "tel": "400-123-4567",
    "weixin": "car-dealer-crm",
    "email": "support@example.com",
    "address": "北京市朝阳区",
    "version": "1.0.0",
    "closeMsg": "系统维护中",
    "isopen": "1",
    "createTime": "2024-01-01 00:00:00",
    "createBy": 1,
    "editTime": "2024-01-15 10:30:00",
    "editBy": 1
  }
}
```

### 14.3 创建系统配置
- **URL**: `/api/system/create`
- **Method**: POST
- **权限**: 需要登录
- **描述**: 创建新系统配置

**请求体**:
```json
{
  "systemCode": "CRM2",
  "name": "另一个CRM系统",
  "site": "http://localhost:8090",
  "title": "另一个CRM",
  "isopen": "1"
}
```

**请求参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| systemCode | String | 是 | 系统代码 |
| name | String | 是 | 系统名称 |
| site | String | 否 | 站点地址 |
| logo | String | 否 | Logo路径 |
| title | String | 否 | 系统标题 |
| description | String | 否 | 系统描述 |
| keywords | String | 否 | 关键词 |
| shortcuticon | String | 否 | 快捷图标 |
| tel | String | 否 | 联系电话 |
| weixin | String | 否 | 微信号 |
| email | String | 否 | 邮箱 |
| address | String | 否 | 地址 |
| version | String | 否 | 版本号 |
| closeMsg | String | 否 | 关闭提示信息 |
| isopen | String | 否 | 是否开启（1开启 0关闭） |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 14.4 更新系统配置
- **URL**: `/api/system/{id}`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 更新系统配置信息

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 系统配置ID |

**请求体**: 同创建系统配置

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 14.5 删除系统配置
- **URL**: `/api/system/{id}`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 删除指定系统配置

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 系统配置ID |

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 14.6 批量删除系统配置
- **URL**: `/api/system/batch`
- **Method**: DELETE
- **权限**: 需要登录
- **描述**: 批量删除系统配置

**请求体**:
```json
[1, 2, 3]
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 14.7 切换系统状态
- **URL**: `/api/system/{id}/status`
- **Method**: PUT
- **权限**: 需要登录
- **描述**: 切换系统的开启/关闭状态

**路径参数**:
| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | Integer | 是 | 系统配置ID |

**请求体**:
```json
{
  "isopen": "0"
}
```

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

---

## 15. 系统监控

### 15.1 获取系统信息
- **URL**: `/api/monitor/system-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取系统基本信息（操作系统、主机名等）

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "osName": "Mac OS X",
    "osVersion": "14.0",
    "osArch": "aarch64",
    "hostName": "MacBook-Pro",
    "userName": "admin",
    "systemBootTime": "2024-01-01 00:00:00",
    "serverIp": "192.168.1.100"
  }
}
```

### 15.2 获取 CPU 信息
- **URL**: `/api/monitor/cpu-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取 CPU 使用情况

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "cpuName": "Apple M1",
    "cpuCores": 8,
    "cpuUsage": 25.5,
    "sysUsage": 10.2,
    "userUsage": 15.3,
    "idle": 74.5
  }
}
```

### 15.3 获取内存信息
- **URL**: `/api/monitor/memory-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取内存使用情况

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalMemory": 17179869184,
    "usedMemory": 8589934592,
    "freeMemory": 8589934592,
    "memoryUsage": 50.0,
    "totalMemoryFormatted": "16.0 GB",
    "usedMemoryFormatted": "8.0 GB",
    "freeMemoryFormatted": "8.0 GB"
  }
}
```

### 15.4 获取磁盘信息
- **URL**: `/api/monitor/disk-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取磁盘使用情况

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "totalDisk": 500107862016,
    "usedDisk": 250053931008,
    "freeDisk": 250053931008,
    "diskUsage": 50.0,
    "totalDiskFormatted": "465.7 GB",
    "usedDiskFormatted": "232.9 GB",
    "freeDiskFormatted": "232.9 GB"
  }
}
```

### 15.5 获取网络信息
- **URL**: `/api/monitor/network-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取网络接口信息

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "networkInterfaces": [
      {
        "name": "en0",
        "ip": "192.168.1.100",
        "mac": "AA:BB:CC:DD:EE:FF",
        "status": "UP"
      }
    ]
  }
}
```

### 15.6 获取 JVM 信息
- **URL**: `/api/monitor/jvm-info`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取 JVM 运行时信息

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "javaVersion": "17.0.2",
    "javaHome": "/Library/Java/JavaVirtualMachines/jdk-17.jdk",
    "jvmName": "Java HotSpot(TM) 64-Bit Server VM",
    "jvmVersion": "17.0.2+8-86",
    "totalMemory": 268435456,
    "maxMemory": 4294967296,
    "freeMemory": 134217728,
    "usedMemory": 134217728,
    "startTime": "2024-01-15 10:00:00",
    "runTime": "2h 30m",
    "inputArgs": ["-Xms256m", "-Xmx4g"]
  }
}
```

### 15.7 获取完整监控数据
- **URL**: `/api/monitor/all`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 一次性获取所有监控数据

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "systemInfo": {},
    "cpuInfo": {},
    "memoryInfo": {},
    "diskInfo": {},
    "networkInfo": {},
    "jvmInfo": {}
  }
}
```

---

## 16. 统计报表

### 16.1 获取汇总数据
- **URL**: `/api/summary/data`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取系统汇总统计数据

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "effectiveActivityCount": 10,
    "totalActivityCount": 20,
    "totalClueCount": 500,
    "totalCustomerCount": 200,
    "successTranAmount": 5000000,
    "totalTranAmount": 10000000
  }
}
```

**字段说明**:
| 字段 | 类型 | 说明 |
|------|------|------|
| effectiveActivityCount | Integer | 有效的市场活动数 |
| totalActivityCount | Integer | 总市场活动数 |
| totalClueCount | Integer | 线索总数 |
| totalCustomerCount | Integer | 客户总数 |
| successTranAmount | BigDecimal | 成功交易额 |
| totalTranAmount | BigDecimal | 总交易额 |

### 16.2 获取销售漏斗数据
- **URL**: `/api/saleFunnel/data`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取销售漏斗统计数据（用于 ECharts 漏斗图）

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "value": 100,
      "name": "线索"
    },
    {
      "value": 80,
      "name": "客户"
    },
    {
      "value": 60,
      "name": "交易"
    },
    {
      "value": 20,
      "name": "成交"
    }
  ]
}
```

### 16.3 获取来源饼图数据
- **URL**: `/api/sourcePie/data`
- **Method**: GET
- **权限**: 需要登录
- **描述**: 获取线索来源分布数据（用于 ECharts 饼图）

**响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "value": 1048,
      "name": "搜索引擎"
    },
    {
      "value": 735,
      "name": "直接访问"
    },
    {
      "value": 580,
      "name": "邮件营销"
    },
    {
      "value": 484,
      "name": "联盟广告"
    },
    {
      "value": 300,
      "name": "视频广告"
    }
  ]
}
```

---

## 附录

### A. 前后端不一致说明

| 接口 | 说明 |
|------|------|
| `/api/customer/list` | 新版接口，使用 `page`/`size` 分页参数 |
| `/api/customers` | 旧版接口，使用 `current` 分页参数 |
| `/api/products/stock/{productId}` | 前端调用 `/api/productstock/records/{productId}` |
| `/api/dict/types/batch` | 前端使用 DELETE 方法，后端也使用 DELETE |
| `/api/tran/status/{id}` | 前端定义但后端未实现 |

### B. 权限标识符列表

| 权限标识符 | 说明 |
|------------|------|
| admin | 管理员角色 |
| user:list | 用户列表查看 |
| user:view | 用户详情查看 |
| user:add | 用户新增 |
| user:edit | 用户编辑 |
| user:delete | 用户批量禁用（原删除权限，现用于 `PUT /api/users/batch-disable`） |
| clue:list | 线索列表查看 |
| clue:view | 线索详情查看 |
| clue:add | 线索新增 |
| clue:edit | 线索编辑 |
| clue:delete | 线索删除 |
| clue:import | 线索导入 |

### C. 交易阶段说明

| 阶段代码 | 说明 |
|----------|------|
| 41 | 待报价 |
| 42 | 待审批 |
| 43 | 已通过 |
| 44 | 已拒绝 |
| 45 | 已成交 |
| 46 | 已取消 |

### D. 数据库表对应关系

| 表名 | 说明 |
|------|------|
| t_user | 用户表 |
| t_activity | 市场活动表 |
| t_activity_remark | 活动备注表 |
| t_clue | 线索表 |
| t_clue_remark | 线索备注表 |
| t_customer | 客户表 |
| t_tran | 交易表 |
| t_tran_product | 交易产品关联表 |
| t_tran_approve | 交易审批表 |
| t_tran_invoice | 交易发票表 |
| t_tran_remark | 交易备注表 |
| t_dic_type | 字典类型表 |
| t_dic_value | 字典值表 |
| t_system | 系统配置表 |
| product | 商品表 |
| product_category | 商品分类表 |
| product_promotion | 商品促销表 |
| product_stock_record | 库存变动记录表 |

### E. P1 批次变更说明

#### P1-2：请求体 DTO 规范化

所有 `@RequestBody` 接口方法已改为使用专用 DTO 类，不再直接接收实体类型或裸集合。涉及 6 个模块共 12 个 DTO：

| 模块 | DTO |
|------|-----|
| 用户 | `CreateUserRequest`, `UpdateUserRequest`, `BatchDisableUsersRequest`, `AssignUserRolesRequest`, `ChangePasswordRequest` |
| 字典 | `CreateDicTypeRequest`, `UpdateDicTypeRequest`, `CreateDicValueRequest`, `UpdateDicValueRequest` |
| 系统 | `CreateSystemRequest`, `UpdateSystemRequest`, `ToggleSystemStatusRequest` |
| 商品 | `CreateProductRequest`, `UpdateProductRequest` |
| 分类 | `CreateProductCategoryRequest`, `UpdateProductCategoryRequest` |
| 促销 | `CreateProductPromotionRequest`, `UpdateProductPromotionRequest` |

> 所有 DTO 均使用 `jakarta.validation` 注解进行参数校验，校验失败返回 code 501 (`PARAM_ERROR`)。

#### P1-3：统一异常处理

- 所有 Service 方法统一抛出 `BusinessException(CodeEnum, msg)`，替代原有的 `RuntimeException`。
- `GlobalExceptionHandler` 捕获所有异常类型并返回结构化 `R.FAIL` 响应。
- 新增业务错误码：`DUPLICATE(409)`, `NOT_FOUND(404)`, `RESOURCE_IN_USE(422)`, `OPERATION_FAILED(550)`。

#### P1-4：审计日志集成

7 个 Service 实现类共增加 29 个审计调用点，覆盖核心业务操作的创建、更新、删除（禁用）等变更场景。审计日志记录操作人、操作类型和关键业务标识。

#### P1-5：用户模块端点变更

| 变更类型 | 端点 | 说明 |
|----------|------|------|
| 已移除 | `DELETE /api/user/{id}` | 单个用户物理删除 |
| 已移除 | `DELETE /api/user` | 批量物理删除（裸 `List<Integer>` 请求体） |
| 新增 | `PUT /api/users/batch-disable` | 批量逻辑禁用，使用 `BatchDisableUsersRequest` DTO |
| 新增 | `PUT /api/user/{id}/roles` | 分配用户角色 |
| 新增 | `PUT /api/user/{id}/password` | 修改用户密码 |
| 已有 | `PUT /api/user/{id}/disable` | 禁用单个用户 |
| 已有 | `PUT /api/user/{id}/enable` | 启用单个用户 |
| 已有 | `PUT /api/user/{id}/lock` | 锁定单个用户 |
| 已有 | `PUT /api/user/{id}/unlock` | 解锁单个用户 |
