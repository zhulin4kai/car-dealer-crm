import { httpClient } from '@/shared/api/http-client'
import type { PageResult } from '@/shared/api/api-types'
import type { SelectOption } from '@/shared/types/common'
import type { Customer, CustomerQuery } from '@/modules/customer/model/customer.types'

export function fetchCustomerPage(params: CustomerQuery): Promise<PageResult<Customer>> {
  return httpClient.get<PageResult<Customer>>('/api/customer/list', { params })
}

export function fetchCustomerOptions(): Promise<SelectOption[]> {
  return httpClient.get<SelectOption[]>('/api/customer/options')
}

export const getCustomerList = fetchCustomerPage
export const getCustomerOptions = fetchCustomerOptions
