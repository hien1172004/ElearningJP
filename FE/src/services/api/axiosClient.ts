import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { storage } from '@/utils/storage'
import { useAuthStore } from '@/stores/useAuthStore'
import { refreshAccessToken } from '@/utils/tokenRefresh'
import { emitAuthExpired } from '@/utils/authEvents'

/**
 * Tạo axiosClient với:
 *  - Request interceptor: tự động đính kèm Authorization Bearer nếu có token.
 *  - Response interceptor: xử lý 401 -> tự cấp lại token (single-flight) rồi gửi lại request.
 */
export const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
})

// ============================================================================
// Request interceptor
// ============================================================================
axiosClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = storage.getAccessToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// ============================================================================
// Response interceptor: auto refresh on 401
// ============================================================================

const AUTH_ENDPOINTS = [
  '/auth/login',
  '/auth/register',
  '/auth/logout',
  '/auth/refresh',
  '/auth/introspect',
  '/auth/forgot-password',
  '/auth/reset-password',
]

function isAuthEndpoint(url?: string): boolean {
  if (!url) return false
  return AUTH_ENDPOINTS.some((p) => url.includes(p))
}

/** Xoá phiên + mở modal đăng nhập, giữ lại trang hiện tại để quay về sau khi đăng nhập. */
function expireSession(reason: 'expired' | 'refresh_failed') {
  useAuthStore.getState().clearAuth()
  if (typeof window !== 'undefined') {
    const currentPath = window.location.pathname + window.location.search
    emitAuthExpired({
      redirectTo: currentPath === '/login' ? '/' : currentPath,
      reason,
    })
  }
}

axiosClient.interceptors.response.use(
  (response) => response.data,
  async (error: AxiosError) => {
    const originalRequest = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined

    const status = error.response?.status

    // Không refresh cho endpoint auth hoặc khi không có config
    if (!originalRequest || isAuthEndpoint(originalRequest.url)) {
      return Promise.reject(error)
    }

    if (status !== 401 || originalRequest._retry) {
      return Promise.reject(error)
    }

    // Request gửi kèm token nhưng phiên cục bộ đã bị xoá (vd vừa đăng xuất, request còn đang bay)
    // → 401 là đương nhiên: không xoá phiên lần nữa, không mở modal "hết phiên".
    if (originalRequest.headers?.Authorization && !storage.getAccessToken()) {
      return Promise.reject(error)
    }

    // Token đã được tab/request khác refresh trong lúc request này đang bay → chỉ cần gửi lại.
    const sentToken = String(originalRequest.headers?.Authorization ?? '').replace(/^Bearer\s+/i, '')
    const latestToken = storage.getAccessToken()
    originalRequest._retry = true
    if (latestToken && sentToken && latestToken !== sentToken) {
      originalRequest.headers.Authorization = `Bearer ${latestToken}`
      return axiosClient(originalRequest)
    }

    // Thử cấp lại token (single-flight, áp dụng cho mọi phiên — không phụ thuộc "Remember me")
    try {
      const newToken = await refreshAccessToken()
      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`
      }
      return axiosClient(originalRequest)
    } catch (refreshError) {
      // Hết cửa sổ refresh / token bị thu hồi → buộc đăng nhập lại
      if (storage.getAccessToken()) expireSession('refresh_failed')
      return Promise.reject(refreshError)
    }
  },
)

// Export lại type AxiosRequestConfig để các service khác import cho tiện.
export type { AxiosRequestConfig }
