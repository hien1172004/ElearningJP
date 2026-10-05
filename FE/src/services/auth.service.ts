import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  RegisterRequest,
  RegisterResponse,
} from '@/types'

export const authService = {
  login: async (payload: LoginRequest): Promise<ApiResponse<LoginResponse>> => {
    return axiosClient.post('/auth/login', payload)
  },

  register: async (payload: RegisterRequest): Promise<ApiResponse<RegisterResponse>> => {
    return axiosClient.post('/auth/register', payload)
  },

  logout: async (token: string): Promise<ApiResponse<void>> => {
    return axiosClient.post('/auth/logout', { token })
  },
}
