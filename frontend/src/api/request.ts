import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
import { ApiResult, ResultCode } from './types'

/**
 * axios 实例（PRD 6.2.2 / 6.2.3）。
 * - baseURL 取 VITE_API_BASE_URL
 * - 请求拦截：携带 JWT（Authorization: Bearer <token>）
 * - 响应拦截：剥离出 ApiResult；code !== 200 抛错；401 清登录态并跳登录页（带 redirect 回跳）
 *
 * 用法（业务侧）：
 *   const res = await request.get<UserVO, ApiResult<UserVO>>('/api/auth/me')
 *   const data = res.data
 */
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('recall_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

service.interceptors.response.use(
  (response) => {
    const res = response.data as ApiResult
    // 非 Result 结构（如文件流）直接返回原始 response
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return response
    }
    if (res.code !== ResultCode.SUCCESS) {
      if (res.code === ResultCode.UNAUTHORIZED) {
        handleUnauthorized()
      }
      const errMsg = res.message || '请求失败'
      const event = new CustomEvent('app-toast', { detail: { text: errMsg, type: 'error' } })
      window.dispatchEvent(event)
      return Promise.reject(new Error(errMsg))
    }
    // 剥离出 ApiResult，业务侧用第二泛型接收
    return res as unknown as typeof response
  },
  (error: AxiosError<ApiResult>) => {
    if (error.response?.status === 401) {
      handleUnauthorized()
    }
    const msg = error.response?.data?.message || error.message || '网络异常'
    const event = new CustomEvent('app-toast', { detail: { text: msg, type: 'error' } })
    window.dispatchEvent(event)
    return Promise.reject(new Error(msg))
  },
)

function handleUnauthorized() {
  localStorage.removeItem('recall_token')
  localStorage.removeItem('recall_user')
  const { pathname, search } = window.location
  if (pathname !== '/login') {
    const redirect = encodeURIComponent(pathname + search)
    window.location.href = `/login?redirect=${redirect}`
  }
}

export default service
