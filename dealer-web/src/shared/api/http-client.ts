import axios, { type AxiosRequestConfig } from 'axios'

import { ApiError } from '@/shared/api/api-error'
import type { ApiEnvelope } from '@/shared/api/api-types'
import { env } from '@/shared/config/env'
import { clearPermissionCodes } from '@/shared/storage/permission-storage'
import { clearStoredToken, readStoredToken } from '@/shared/storage/token-storage'
import { messageConfirm, messageTip } from '@/shared/utils/feedback'

const axiosClient = axios.create({
  baseURL: env.apiBaseUrl,
  responseType: 'json',
})

axios.defaults.baseURL = env.apiBaseUrl

axiosClient.interceptors.request.use((config) => {
  const storedToken = readStoredToken()
  if (storedToken) {
    config.headers.Authorization = storedToken.token
    if (storedToken.rememberMe) {
      config.headers.rememberMe = true
    }
  }
  return config
})

axiosClient.interceptors.response.use(
  (response) => {
    const envelope = response.data as ApiEnvelope<unknown>

    if (typeof envelope?.code === 'number' && envelope.code >= 500) {
      messageConfirm(`${envelope.msg}，是否重新去登录？`)
        .then(() => {
          clearStoredToken()
          clearPermissionCodes()
          window.location.href = '/'
        })
        .catch(() => {
          messageTip('登录已过期，即将跳转到登录页', 'warning')
          window.setTimeout(() => {
            clearStoredToken()
            clearPermissionCodes()
            window.location.href = '/'
          }, 1500)
        })

      throw new ApiError(envelope.code, envelope.msg, envelope)
    }

    return response
  },
  (error: unknown) => Promise.reject(error),
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await axiosClient.request<ApiEnvelope<T>>(config)
  const envelope = response.data

  if (envelope.code !== 200) {
    throw new ApiError(envelope.code, envelope.msg || '请求失败', envelope)
  }

  return envelope.data
}

export const httpClient = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'get', url })
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'post', url, data })
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'put', url, data })
  },
  delete<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'delete', url, data })
  },
  blob(url: string, config?: AxiosRequestConfig): Promise<Blob> {
    return axiosClient
      .request<Blob>({ ...config, method: 'get', url, responseType: 'blob' })
      .then((response) => response.data)
  },
}

export function doGet<T>(url: string, params?: unknown): Promise<T> {
  return httpClient.get<T>(url, { params })
}

export function doPost<T>(url: string, data?: unknown): Promise<T> {
  return httpClient.post<T>(url, data)
}

export function doPut<T>(url: string, data?: unknown): Promise<T> {
  return httpClient.put<T>(url, data)
}

export function doDelete<T>(url: string, data?: unknown): Promise<T> {
  return httpClient.delete<T>(url, data)
}
