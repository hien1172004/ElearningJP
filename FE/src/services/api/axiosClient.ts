import axios, {
  AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig,
} from 'axios'
import { storage } from '@/utils/storage'
import { useAuthStore } from '@/stores/useAuthStore'
import { authService } from '@/services/auth.service'
import { emitAuthExpired } from '@/utils/authEvents'

/**
 * Tạo axiosClient với:
 *  - Request interceptor: tự động đính kèm Authorization Bearer nếu có token.
 *  - Response interceptor: xử lý 401 -> auto refresh token (nếu Remember me) với mutex + queue.
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

// Mutex + queue cho việc refresh token.
let isRefreshing = false
let failedQueue: Array<{
  resolve: (value: unknown) => void
  reject: (reason?: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach(({ resolve, reject }) => {
    if (error) {
      reject(error)
    } else {
      resolve(token)
    }
  })
  failedQueue = []
}

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

    const remember = storage.getRemember()

    // Nếu user không bật Remember me -> logout mềm.
    if (!remember) {
      useAuthStore.getState().clearAuth()
      if (typeof window !== 'undefined') {
        const currentPath = window.location.pathname + window.location.search
        emitAuthExpired({
          redirectTo: currentPath === '/login' ? '/' : currentPath,
          reason: 'expired',
        })
      }
      return Promise.reject(error)
    }

    // Có Remember me -> thử refresh 1 lần duy nhất.
    originalRequest._retry = true

    if (isRefreshing) {
      // Đang refresh -> push vào queue chờ
      return new Promise((resolve, reject) => {
        failedQueue.push({ resolve, reject })
      })
        .then((token) => {
          if (typeof token === 'string' && originalRequest.headers) {
            originalRequest.headers.Authorization = `Bearer ${token}`
          }
          return axiosClient(originalRequest)
        })
        .catch((err) => Promise.reject(err))
    }

    isRefreshing = true
    const currentToken = storage.getAccessToken()

    if (!currentToken) {
      isRefreshing = false
      useAuthStore.getState().clearAuth()
      return Promise.reject(error)
    }

    try {
      const res = await authService.refresh(currentToken)
      const newToken = res?.data?.token
      if (!newToken) throw new Error('No token in refresh response')

      useAuthStore.getState().updateAccessToken(newToken)
      processQueue(null, newToken)

      if (originalRequest.headers) {
        originalRequest.headers.Authorization = `Bearer ${newToken}`
      }
      return axiosClient(originalRequest)
    } catch (refreshError) {
      processQueue(refreshError, null)
      useAuthStore.getState().clearAuth()
      if (typeof window !== 'undefined') {
        const currentPath = window.location.pathname + window.location.search
        emitAuthExpired({
          redirectTo: currentPath === '/login' ? '/' : currentPath,
          reason: 'refresh_failed',
        })
      }
      return Promise.reject(refreshError)
    } finally {
      isRefreshing = false
    }
  },
)

// Export lại type AxiosRequestConfig để các service khác import cho tiện.
export type { AxiosRequestConfig }
