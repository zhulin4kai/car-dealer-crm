# 字典管理模块接口文档

## 概述
字典管理模块用于维护系统基础数据标准和选项配置，如国家地区列表、行业分类、订单状态、客户等级等标准化字段。通过统一数据字典，确保各模块间数据一致性和规范性，减少重复录入错误。支持自定义字段（如添加特定业务标签），适应企业个性化需求。

## 接口规范
- 基础路径: `/api/dict`
- 请求方法: GET, POST, PUT, DELETE
- 认证方式: 所有接口需要认证，传递JWT令牌在请求头中
- 响应格式: JSON

## 通用响应格式
```json
{
  "code": 200,  // 状态码，200表示成功，非200表示失败
  "msg": "success",  // 响应消息
  "data": {}  // 响应数据，可能是对象、数组或null
}
```

## 1. 字典类型管理

### 1.1 获取字典类型列表
- 请求路径: `/api/dict/types`
- 请求方法: GET
- 功能描述: 获取所有字典类型列表，支持分页和条件查询

**请求参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| page | Integer | 否 | 页码，默认值1 |
| size | Integer | 否 | 每页大小，默认值10 |
| typeCode | String | 否 | 字典类型代码，模糊查询 |
| typeName | String | 否 | 字典类型名称，模糊查询 |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 13,
    "list": [
      {
        "id": 1,
        "typeCode": "sex",
        "typeName": "性别",
        "remark": null
      },
      {
        "id": 2,
        "typeCode": "appellation",
        "typeName": "称呼",
        "remark": null
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "pages": 2
  }
}
```

### 1.2 获取字典类型详情
- 请求路径: `/api/dict/types/{id}`
- 请求方法: GET
- 功能描述: 获取指定ID的字典类型详情

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "typeCode": "sex",
    "typeName": "性别",
    "remark": null
  }
}
```

### 1.3 创建字典类型
- 请求路径: `/api/dict/types`
- 请求方法: POST
- 功能描述: 创建新的字典类型

**请求参数**
```json
{
  "typeCode": "education",
  "typeName": "教育程度",
  "remark": "用户的教育水平"
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 14
  }
}
```

### 1.4 更新字典类型
- 请求路径: `/api/dict/types/{id}`
- 请求方法: PUT
- 功能描述: 更新指定ID的字典类型

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**请求参数**
```json
{
  "typeCode": "education",
  "typeName": "教育程度",
  "remark": "用户的最高学历"
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 1.5 删除字典类型
- 请求路径: `/api/dict/types/{id}`
- 请求方法: DELETE
- 功能描述: 删除指定ID的字典类型（若存在关联的字典值，将无法删除）

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 2. 字典值管理

### 2.1 获取字典值列表
- 请求路径: `/api/dict/values`
- 请求方法: GET
- 功能描述: 获取所有字典值列表，支持分页和条件查询

**请求参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| page | Integer | 否 | 页码，默认值1 |
| size | Integer | 否 | 每页大小，默认值10 |
| typeCode | String | 否 | 字典类型代码 |
| typeValue | String | 否 | 字典值，模糊查询 |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "total": 65,
    "list": [
      {
        "id": 51,
        "typeCode": "sex",
        "typeValue": "男",
        "order": 1,
        "remark": null
      },
      {
        "id": 52,
        "typeCode": "sex",
        "typeValue": "女",
        "order": 2,
        "remark": null
      }
    ],
    "pageNum": 1,
    "pageSize": 10,
    "pages": 7
  }
}
```

### 2.2 按类型获取字典值列表
- 请求路径: `/api/dict/values/type/{typeCode}`
- 请求方法: GET
- 功能描述: 获取指定类型代码的字典值列表

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| typeCode | String | 是 | 字典类型代码 |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": [
    {
      "id": 51,
      "typeCode": "sex",
      "typeValue": "男",
      "order": 1,
      "remark": null
    },
    {
      "id": 52,
      "typeCode": "sex",
      "typeValue": "女",
      "order": 2,
      "remark": null
    }
  ]
}
```

### 2.3 获取字典值详情
- 请求路径: `/api/dict/values/{id}`
- 请求方法: GET
- 功能描述: 获取指定ID的字典值详情

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 51,
    "typeCode": "sex",
    "typeValue": "男",
    "order": 1,
    "remark": null
  }
}
```

### 2.4 创建字典值
- 请求路径: `/api/dict/values`
- 请求方法: POST
- 功能描述: 创建新的字典值

**请求参数**
```json
{
  "typeCode": "education",
  "typeValue": "博士",
  "order": 5,
  "remark": "博士学历"
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 66
  }
}
```

### 2.5 更新字典值
- 请求路径: `/api/dict/values/{id}`
- 请求方法: PUT
- 功能描述: 更新指定ID的字典值

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**请求参数**
```json
{
  "typeCode": "education",
  "typeValue": "博士研究生",
  "order": 5,
  "remark": "博士研究生学历"
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.6 删除字典值
- 请求路径: `/api/dict/values/{id}`
- 请求方法: DELETE
- 功能描述: 删除指定ID的字典值（若该字典值被其他表引用，将无法删除）

**路径参数**
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 3. 批量操作

### 3.1 批量删除字典类型
- 请求路径: `/api/dict/types/batch`
- 请求方法: DELETE
- 功能描述: 批量删除多个字典类型

**请求参数**
```json
{
  "ids": [14, 15, 16]
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": 2,
    "failed": 1,
    "failedIds": [16]
  }
}
```

### 3.2 批量删除字典值
- 请求路径: `/api/dict/values/batch`
- 请求方法: DELETE
- 功能描述: 批量删除多个字典值

**请求参数**
```json
{
  "ids": [67, 68, 69]
}
```

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "success": 3,
    "failed": 0,
    "failedIds": []
  }
}
```

## 4. 缓存管理

### 4.1 刷新字典缓存
- 请求路径: `/api/dict/cache/refresh`
- 请求方法: POST
- 功能描述: 刷新字典缓存，确保系统使用最新的字典数据

**响应数据**
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 状态码说明

| 状态码 | 说明 |
| ------ | ---- |
| 200 | 请求成功 |
| 400 | 请求参数错误 |
| 401 | 未认证或认证失败 |
| 403 | 权限不足 |
| 404 | 资源不存在 |
| 409 | 资源冲突 |
| 500 | 服务器内部错误 | 