import { httpClient } from '@/shared/api/http-client'
import type { EntityId } from '@/shared/types/id'
import {
  USER_HISTORY_ACTION,
  type UserHistoryActionOption,
  type UserHistoryBatchSummary,
  type UserHistoryCollection,
  type UserHistoryItem,
  type UserHistoryOperatorSummary,
  type UserHistoryQuery,
  type UserHistoryTargetSummary,
  type UserHistoryValueField,
} from '@/modules/user/model/user-history.types'

type JsonObject = Record<string, unknown>

function objectValue(value: unknown, path: string): JsonObject {
  if (value === null || typeof value !== 'object' || Array.isArray(value)) {
    throw new Error(`用户历史响应格式无效：${path}`)
  }
  return value as JsonObject
}

function arrayValue(value: unknown, path: string): unknown[] {
  if (!Array.isArray(value)) throw new Error(`用户历史响应格式无效：${path}`)
  return value
}

function stringValue(value: unknown, path: string): string {
  if (typeof value !== 'string') throw new Error(`用户历史响应格式无效：${path}`)
  return value
}

function nullableStringValue(value: unknown, path: string): string | null | undefined {
  if (value === undefined || value === null) return value
  return stringValue(value, path)
}

function numberValue(value: unknown, path: string): number {
  if (typeof value !== 'number' || !Number.isInteger(value) || value < 0) {
    throw new Error(`用户历史响应格式无效：${path}`)
  }
  return value
}

function entityIdValue(value: unknown, path: string): EntityId | null | undefined {
  if (value === undefined || value === null) return value
  if (typeof value !== 'string' && typeof value !== 'number') {
    throw new Error(`用户历史响应格式无效：${path}`)
  }
  if (typeof value === 'number' && !Number.isSafeInteger(value)) {
    throw new Error(`用户历史响应格式无效：${path}`)
  }
  return value
}

function normalizeValueField(value: unknown, path: string): UserHistoryValueField {
  const item = objectValue(value, path)
  return {
    code: stringValue(item.code, `${path}.code`),
    label: stringValue(item.label, `${path}.label`),
    valueCode: nullableStringValue(item.valueCode, `${path}.valueCode`),
    valueName: nullableStringValue(item.valueName, `${path}.valueName`),
    displayValue: nullableStringValue(item.displayValue, `${path}.displayValue`),
  }
}

function normalizeTarget(value: unknown, path: string): UserHistoryTargetSummary {
  const item = objectValue(value, path)
  return {
    typeCode: stringValue(item.typeCode, `${path}.typeCode`),
    typeName: stringValue(item.typeName, `${path}.typeName`),
    id: entityIdValue(item.id, `${path}.id`),
    code: nullableStringValue(item.code, `${path}.code`),
    name: nullableStringValue(item.name, `${path}.name`),
  }
}

function normalizeOperator(value: unknown, path: string): UserHistoryOperatorSummary {
  const item = objectValue(value, path)
  return {
    id: entityIdValue(item.id, `${path}.id`),
    name: stringValue(item.name, `${path}.name`),
    employeeNo: nullableStringValue(item.employeeNo, `${path}.employeeNo`),
  }
}

function normalizeBatch(value: unknown, path: string): UserHistoryBatchSummary | null | undefined {
  if (value === undefined || value === null) return value
  const item = objectValue(value, path)
  return {
    batchId: stringValue(item.batchId, `${path}.batchId`),
    totalCount: numberValue(item.totalCount, `${path}.totalCount`),
    successCount: numberValue(item.successCount, `${path}.successCount`),
    failureCount: numberValue(item.failureCount, `${path}.failureCount`),
    targetResultCode: stringValue(item.targetResultCode, `${path}.targetResultCode`),
    targetResultName: stringValue(item.targetResultName, `${path}.targetResultName`),
  }
}

function normalizeItem(value: unknown, index: number): UserHistoryItem {
  const path = `list[${index}]`
  const item = objectValue(value, path)
  return {
    eventId: stringValue(item.eventId, `${path}.eventId`),
    sourceKey: stringValue(item.sourceKey, `${path}.sourceKey`),
    actionCode: stringValue(item.actionCode, `${path}.actionCode`),
    actionName: stringValue(item.actionName, `${path}.actionName`),
    categoryCode: stringValue(item.categoryCode, `${path}.categoryCode`),
    categoryName: stringValue(item.categoryName, `${path}.categoryName`),
    target: normalizeTarget(item.target, `${path}.target`),
    operator: normalizeOperator(item.operator, `${path}.operator`),
    beforeValues: arrayValue(item.beforeValues, `${path}.beforeValues`).map((field, fieldIndex) =>
      normalizeValueField(field, `${path}.beforeValues[${fieldIndex}]`),
    ),
    afterValues: arrayValue(item.afterValues, `${path}.afterValues`).map((field, fieldIndex) =>
      normalizeValueField(field, `${path}.afterValues[${fieldIndex}]`),
    ),
    reason: nullableStringValue(item.reason, `${path}.reason`),
    effectiveFrom: nullableStringValue(item.effectiveFrom, `${path}.effectiveFrom`),
    effectiveTo: nullableStringValue(item.effectiveTo, `${path}.effectiveTo`),
    resultCode: stringValue(item.resultCode, `${path}.resultCode`),
    resultName: stringValue(item.resultName, `${path}.resultName`),
    batchSummary: normalizeBatch(item.batchSummary, `${path}.batchSummary`),
    occurredAt: stringValue(item.occurredAt, `${path}.occurredAt`),
  }
}

function normalizeActionOption(value: unknown, index: number): UserHistoryActionOption {
  const path = `actionOptions[${index}]`
  const item = objectValue(value, path)
  return {
    code: stringValue(item.code, `${path}.code`),
    label: stringValue(item.label, `${path}.label`),
  }
}

export function normalizeUserHistoryCollection(value: unknown): UserHistoryCollection {
  const response = objectValue(value, 'data')
  const unavailableReasons = objectValue(response.unavailableReasons, 'unavailableReasons')
  return {
    list: arrayValue(response.list, 'list').map(normalizeItem),
    total: numberValue(response.total, 'total'),
    pageSize: numberValue(response.pageSize, 'pageSize'),
    pageNum: numberValue(response.pageNum, 'pageNum'),
    pages: numberValue(response.pages, 'pages'),
    size: numberValue(response.size, 'size'),
    actionOptions: arrayValue(response.actionOptions, 'actionOptions').map(normalizeActionOption),
    allowedActions: arrayValue(response.allowedActions, 'allowedActions').filter(
      (action): action is typeof USER_HISTORY_ACTION.VIEW => action === USER_HISTORY_ACTION.VIEW,
    ),
    unavailableReasons: {
      ...(typeof unavailableReasons[USER_HISTORY_ACTION.VIEW] === 'string'
        ? { [USER_HISTORY_ACTION.VIEW]: unavailableReasons[USER_HISTORY_ACTION.VIEW] }
        : {}),
    },
  }
}

export function fetchUserHistory(
  userId: EntityId,
  params: UserHistoryQuery,
  signal?: AbortSignal,
): Promise<UserHistoryCollection> {
  return httpClient
    .get<unknown>(`/api/users/${userId}/history`, { params, signal })
    .then(normalizeUserHistoryCollection)
}
