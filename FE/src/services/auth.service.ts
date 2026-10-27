import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  ForgotPasswordRequest,
  IntrospectResponse,
  LoginRequest,
  LoginResponse,
  RefreshResponse,
  RegisterRequest,
  RegisterResponse,
  ResetPasswordRequest,
} from '@/types'

export const authService = {
  // ----------------------------------------------------------------
  // LOGIN
  // ----------------------------------------------------------------
  login: async (
    payload: LoginRequest,
  ): Promise<ApiResponse<LoginResponse>> => {
    return axiosClient.post('/auth/login', payload)
  },

  // ----------------------------------------------------------------
  // GOOGLE LOGIN
  // ----------------------------------------------------------------
  googleLogin: async (
    idToken: string,
  ): Promise<ApiResponse<LoginResponse>> => {
    return axiosClient.post('/auth/google', { idToken })
  },

  // ----------------------------------------------------------------
  // REGISTER
  // ----------------------------------------------------------------
  register: async (
    payload: RegisterRequest,
  ): Promise<ApiResponse<RegisterResponse>> => {
    return axiosClient.post('/auth/register', payload)
  },

  // ----------------------------------------------------------------
  // LOGOUT - truyền accessToken (theo quyết định của user).
  // ----------------------------------------------------------------
  logout: async (token: string): Promise<ApiResponse<void>> => {
    return axiosClient.post('/auth/logout', { token })
  },

  // ----------------------------------------------------------------
  // REFRESH - truyền accessToken hiện tại.
  // ----------------------------------------------------------------
  refresh: async (token: string): Promise<ApiResponse<RefreshResponse>> => {
    return axiosClient.post('/auth/refresh', { token })
  },

  // ----------------------------------------------------------------
  // INTROSPECT - kiểm tra token còn hợp lệ không.
  // ----------------------------------------------------------------
  introspect: async (
    token: string,
  ): Promise<ApiResponse<IntrospectResponse>> => {
    return axiosClient.post('/auth/introspect', { token })
  },

  // ----------------------------------------------------------------
  // FORGOT PASSWORD
  // ----------------------------------------------------------------
  forgotPassword: async (
    payload: ForgotPasswordRequest,
  ): Promise<ApiResponse<void>> => {
    return axiosClient.post('/auth/forgot-password', payload)
  },

  // ----------------------------------------------------------------
  // RESET PASSWORD
  // ----------------------------------------------------------------
  resetPassword: async (
    payload: ResetPasswordRequest,
  ): Promise<ApiResponse<void>> => {
    return axiosClient.post('/auth/reset-password', payload)
  },

  // ----------------------------------------------------------------
  // VALIDATE RESET TOKEN
  // ----------------------------------------------------------------
  validateResetToken: async (
    token: string,
  ): Promise<ApiResponse<boolean>> => {
    return axiosClient.get('/auth/reset-password/validate', {
      params: { token },
    })
  },
}
