import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { notify } from '@/utils/notify'
import type { ApiResponse } from './types'

let isRedirectingToLogin = false

export const requestClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request Interceptor
requestClient.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('csms_token')
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    // traceId for logging
    if (config.method?.toUpperCase() === 'GET' && config.headers) {
      config.headers['X-Request-Trace'] = Math.random().toString(36).substring(2, 10)
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response Interceptor
requestClient.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    const res = response.data
    // Spring Boot returns standard envelope {code, message, data}
    if (res.code !== undefined && res.code !== 0 && res.code !== 200) {
      notify(res.message || `操作失败 (错误码 ${res.code})`, 'error')
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return response
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      const msg = error.response.data?.message || '服务器响应异常'

      if (status === 401) {
        localStorage.removeItem('csms_token')
        localStorage.removeItem('csms_user')

        // 已经在登录页就不再跳转，否则会把当前 URL（含它自己的 redirect 参数）
        // 再次塞进新的 redirect，形成层层嵌套的重定向死循环。
        const onLoginPage = window.location.pathname === '/login'
        if (onLoginPage) {
          isRedirectingToLogin = false
          notify(msg, 'error')
          return Promise.reject(error)
        }

        if (!isRedirectingToLogin) {
          isRedirectingToLogin = true
          notify('登录已失效，请重新登录', 'warning')

          // 只取路径，剥掉可能已存在的 redirect 参数，避免递归嵌套
          const { pathname, search } = window.location
          const params = new URLSearchParams(search)
          params.delete('redirect')
          const rest = params.toString()
          const target = pathname + (rest ? `?${rest}` : '')

          setTimeout(() => {
            window.location.href = `/login?redirect=${encodeURIComponent(target)}`
          }, 300)
        }
      } else if (status === 403) {
        notify(window.location.pathname === '/login' ? msg : '权限不足，当前角色无法访问该资源', 'error')
      } else if (status >= 500) {
        notify(`系统繁忙: ${msg}`, 'error')
      } else {
        notify(msg, 'error')
      }
    } else {
      notify('网络连接超时或断开，请检查网络与后端服务', 'error')
    }
    return Promise.reject(error)
  }
)

export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await requestClient.request<ApiResponse<T>>(config)
  if (response.data && response.data.data !== undefined) {
    return response.data.data
  }
  return response.data as unknown as T
}

export default requestClient
