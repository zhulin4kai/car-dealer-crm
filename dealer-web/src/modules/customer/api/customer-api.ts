import { httpClient } from '@/shared/api/http-client'
import type { DownloadResult, PageResult } from '@/shared/api/api-types'
import type { EntityId } from '@/shared/types/id'
import type { SelectOption } from '@/shared/types/common'
import type { Customer, CustomerDetail, CustomerQuery } from '@/modules/customer/model/customer.types'

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

export const getCustomerList = fetchCustomerPage
export const getCustomerOptions = fetchCustomerOptions
