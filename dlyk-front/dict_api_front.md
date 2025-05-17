# 字典管理前端 API 文档

## 概述
本文档详细描述了字典管理模块的前端 API 接口，包括字典类型管理和字典值管理的所有接口。该文档基于前端实现，用于前后端开发人员之间的沟通。

## 基础信息
- 所有接口的基础路径: `/api/dict`
- 请求方法: GET, POST, PUT, DELETE
- 所有请求头需要包含认证信息(JWT Token)

## 响应格式
所有接口返回统一的 JSON 格式:
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
- 功能描述: 获取字典类型列表，支持分页和条件查询

**请求参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| page | Integer | 否 | 页码，默认值1 |
| size | Integer | 否 | 每页大小，默认值10 |
| typeCode | String | 否 | 字典类型代码，模糊查询 |
| typeName | String | 否 | 字典类型名称，模糊查询 |

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**成功响应示例**:
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

**请求体**:
```json
{
  "typeCode": "education",
  "typeName": "教育程度",
  "remark": "用户的教育水平"
}
```

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**请求体**:
```json
{
  "typeCode": "education",
  "typeName": "教育程度",
  "remark": "用户的最高学历"
}
```

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典类型ID |

**成功响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 1.6 批量删除字典类型
- 请求路径: `/api/dict/types/batch`
- 请求方法: DELETE
- 功能描述: 批量删除多个字典类型

**请求参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| ids | Array | 是 | 字典类型ID数组 |

**成功响应示例**:
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

## 2. 字典值管理

### 2.1 获取字典值列表
- 请求路径: `/api/dict/values`
- 请求方法: GET
- 功能描述: 获取字典值列表，支持分页和条件查询

**请求参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| page | Integer | 否 | 页码，默认值1 |
| size | Integer | 否 | 每页大小，默认值10 |
| typeCode | String | 否 | 字典类型代码 |
| typeValue | String | 否 | 字典值，模糊查询 |

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| typeCode | String | 是 | 字典类型代码 |

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**成功响应示例**:
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

**请求体**:
```json
{
  "typeCode": "education",
  "typeValue": "博士",
  "order": 5,
  "remark": "博士学历"
}
```

**成功响应示例**:
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

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**请求体**:
```json
{
  "typeCode": "education",
  "typeValue": "博士研究生",
  "order": 5,
  "remark": "博士研究生学历"
}
```

**成功响应示例**:
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
- 功能描述: 删除指定ID的字典值

**路径参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| id | Integer | 是 | 字典值ID |

**成功响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

### 2.7 批量删除字典值
- 请求路径: `/api/dict/values/batch`
- 请求方法: DELETE
- 功能描述: 批量删除多个字典值

**请求参数**:
| 参数名 | 类型 | 是否必须 | 描述 |
| ------ | ---- | -------- | ---- |
| ids | Array | 是 | 字典值ID数组 |

**成功响应示例**:
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

## 3. 缓存管理

### 3.1 刷新字典缓存
- 请求路径: `/api/dict/cache/refresh`
- 请求方法: POST
- 功能描述: 刷新字典缓存，确保系统使用最新的字典数据

**成功响应示例**:
```json
{
  "code": 200,
  "msg": "success",
  "data": null
}
```

## 前端页面与路由

### 1. 字典类型管理页面
- 路由路径: `/dashboard/dict/type`
- 组件: `DictTypeView.vue`
- 功能: 提供字典类型的查询、新增、编辑、删除和批量删除功能

### 2. 字典值管理页面
- 路由路径: `/dashboard/dict/value`
- 组件: `DictValueView.vue`
- 功能: 提供字典值的查询、新增、编辑、删除、批量删除和缓存刷新功能

## 注意事项

1. 字典类型和字典值的路由采用了REST风格的命名规范，前端路由与后端API保持一致性

2. 在字典值管理中，初始加载需要先获取所有字典类型数据，用于下拉选择

3. 字典类型被删除前，应先检查是否有关联的字典值存在，若存在则应提示用户或阻止删除

4. 批量删除接口需要传递ID数组，后端会返回成功与失败的数量和失败的ID列表 