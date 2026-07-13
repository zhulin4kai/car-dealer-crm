import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'

import { ApiError } from '@/shared/api/api-error'
import { API_ERROR_CODE, isSessionInvalidCode } from '@/shared/api/error-codes'
import type { ApiEnvelope, DownloadResult } from '@/shared/api/api-types'
import { notifySessionInvalid } from '@/shared/auth/session-invalid-handler'
import { notifyUserManagementGate } from '@/shared/auth/user-management-gate-handler'
import { env } from '@/shared/config/env'
import { readStoredToken } from '@/shared/storage/token-storage'

const axiosClient = axios.create({
  baseURL: env.apiBaseUrl,
  responseType: 'json',
})

axios.defaults.baseURL = env.apiBaseUrl

axiosClient.interceptors.request.use((config) => {
  const storedToken = readStoredToken()
  if (storedToken) {
    config.headers.Authorization = `Bearer ${storedToken.token}`
    if (storedToken.rememberMe) {
      config.headers.rememberMe = true
    }
  }
  return config
})

function shouldInvalidateSession(envelope: ApiEnvelope<unknown>, httpStatus?: number): boolean {
  if (httpStatus === 403) {
    return false
  }
  if (httpStatus === 401) {
    return envelope.code !== API_ERROR_CODE.AUTH_LOGIN_FAILED
  }
  return isSessionInvalidCode(envelope.code)
}

function envelopeToApiError(envelope: ApiEnvelope<unknown>, httpStatus?: number): ApiError {
  const sessionInvalid = shouldInvalidateSession(envelope, httpStatus)
  if (sessionInvalid) {
    void notifySessionInvalid({ code: envelope.code, msg: envelope.msg })
  }
  if (
    envelope.code === API_ERROR_CODE.ADMIN_BOOTSTRAP_REQUIRED ||
    envelope.code === API_ERROR_CODE.RECOVERY_ACCOUNT_BUSINESS_FORBIDDEN
  ) {
    void notifyUserManagementGate({ code: envelope.code })
  }
  return new ApiError(
    envelope.code,
    envelope.msg || '请求失败',
    envelope,
    sessionInvalid,
    httpStatus,
  )
}

axiosClient.interceptors.response.use(
  response => response,
  (error: unknown) => {
    if (axios.isAxiosError(error)) {
      const envelope = error.response?.data as ApiEnvelope<unknown> | undefined
      if (typeof envelope?.code === 'number') {
        return Promise.reject(envelopeToApiError(envelope, error.response?.status))
      }
    }
    return Promise.reject(error)
  },
)

async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await axiosClient.request<ApiEnvelope<T>>(config)
  const envelope = response.data

  if (envelope.code !== 200) {
    throw envelopeToApiError(envelope, response.status)
  }

  return envelope.data
}

function parseFilename(contentDisposition: string | undefined): string {
  if (!contentDisposition) {
    return 'download.bin'
  }
  const rfc5987Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (rfc5987Match?.[1]) {
    try {
      return decodeURIComponent(rfc5987Match[1])
    } catch {
      // fall through to filename=
    }
  }
  const filenameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (filenameMatch?.[1]) {
    try {
      return decodeURIComponent(filenameMatch[1])
    } catch {
      return filenameMatch[1]
    }
  }
  return 'download.bin'
}

async function blobToApiError(blob: Blob, httpStatus?: number): Promise<ApiError | null> {
  const text = await blob.text()
  try {
    const envelope = JSON.parse(text) as ApiEnvelope<unknown>
    if (envelope && typeof envelope.code === 'number') {
      return envelopeToApiError(envelope, httpStatus)
    }
  } catch {
    // not valid JSON
  }
  return null
}

async function download(url: string, config?: AxiosRequestConfig): Promise<DownloadResult> {
  let response: AxiosResponse<Blob>
  try {
    response = await axiosClient.request<Blob>({
      ...config,
      method: 'get',
      url,
      responseType: 'blob',
    })
  } catch (error: unknown) {
    if (axios.isAxiosError(error) && error.response?.data instanceof Blob) {
      const apiError = await blobToApiError(error.response.data, error.response.status)
      if (apiError) {
        throw apiError
      }
    }
    throw error
  }

  const blob = response.data
  const contentType = String(response.headers['content-type'] ?? '')

  if (contentType.includes('application/json')) {
    const apiError = await blobToApiError(blob, response.status)
    if (apiError) {
      throw apiError
    }
    throw new ApiError(500, '下载失败：响应格式异常', null)
  }

  const filename = parseFilename(response.headers['content-disposition'])
  return { blob, filename }
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
  patch<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'patch', url, data })
  },
  delete<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return request<T>({ ...config, method: 'delete', url, data })
  },
  download(url: string, config?: AxiosRequestConfig): Promise<DownloadResult> {
    return download(url, config)
  },
}
