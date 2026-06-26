import { httpClient } from '@/shared/api/http-client'
import type { DownloadResult, PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { SelectOption } from '@/shared/types/common'
import type {
  Customer,
  CustomerDetail,
  CustomerMergeResponse,
  CustomerQuery,
  MergeCustomerRequest,
  TransferCustomerOwnerRequest,
} from '@/modules/customer/model/customer.types'

export function fetchCustomerPage(params: CustomerQuery): Promise<PageResult<Customer>> {
  return httpClient.get<PageResult<Customer>>('/api/customer/list', { params })
}

export function fetchCustomerDetail(id: EntityId, signal?: AbortSignal): Promise<CustomerDetail> {
  return httpClient.get<CustomerDetail>(`/api/customer/${id}`, { signal })
}

export function fetchCustomerOptions(): Promise<SelectOption[]> {
  return httpClient.get<SelectOption[]>('/api/customer/options')
}

export function exportCustomers(ids?: EntityId[]): Promise<DownloadResult> {
  const params = ids && ids.length > 0 ? { ids: ids.map(String).join(',') } : undefined
  return httpClient.download('/api/exportExcel', { params })
}

export function transferCustomerOwner(
  id: EntityId,
  data: TransferCustomerOwnerRequest,
): Promise<unknown> {
  return httpClient.put(`/api/customer/${id}/owner`, data)
}

export function mergeCustomer(
  targetCustomerId: EntityId,
  data: MergeCustomerRequest,
): Promise<CustomerMergeResponse> {
  return httpClient.post<CustomerMergeResponse>(`/api/customer/${targetCustomerId}/merge`, data)
}

export function deleteCustomer(id: EntityId): Promise<unknown> {
  return httpClient.delete(`/api/customer/${id}`)
}

export const getCustomerList = fetchCustomerPage
export const getCustomerOptions = fetchCustomerOptions
