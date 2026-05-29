import { doGet } from '../http/httpRequest'

/**
 * 获取客户列表（包含线索名称）
 */
export const getCustomerList = (params) => {
  return doGet('/api/customer/list', params)
}

/**
 * 获取所有客户选项（用于下拉选择）
 */
export const getCustomerOptions = () => {
  return doGet('/api/customer/options')
} 