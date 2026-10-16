import axios, { AxiosError } from 'axios'
import { storage } from '@/utils/storage'

export const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
})

// Request interceptor: tự động đính kèm JWT token nếu có
axiosClient.interceptors.request.use(
  (config) => {
    const token = storage.getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// Response interceptor: xử lý lỗi tập trung
axiosClient.interceptors.response.use(
  (response) => response.data,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      storage.clearToken()
    }
    return Promise.reject(error)
  }
)
