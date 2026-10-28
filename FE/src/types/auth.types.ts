export type UserStatus = 'ACTIVE' | 'INACTIVE' | 'BANNED' | 'PENDING'

export interface User {
  id: number
  email: string
  fullName: string
  avatarUrl?: string
  role: string
  roles?: string[]
  status?: UserStatus
  phone?: string
}

export interface UserInfo {
  fullName: string
  email: string
  status: UserStatus
}

// ============================================================================
// LOGIN
// ============================================================================
export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  error?: string
  success: boolean
}

// ============================================================================
// REGISTER
// ============================================================================
export interface RegisterRequest {
  fullName: string
  email: string
  phone: string
  password: string
  confirmPassword: string
}

export interface RegisterResponse {
  token: string
  userInfoResponse: UserInfo
  error?: string
  success: boolean
}

// ============================================================================
// REFRESH
// ============================================================================
export interface RefreshRequest {
  token: string
}

export interface RefreshResponse {
  token: string
}

// ============================================================================
// INTROSPECT
// ============================================================================
export interface IntrospectRequest {
  token: string
}

export interface IntrospectResponse {
  valid: boolean
}

// ============================================================================
// LOGOUT
// ============================================================================
export interface LogoutRequest {
  token: string
}

// ============================================================================
// FORGOT / RESET PASSWORD
// ============================================================================
export interface ForgotPasswordRequest {
  email: string
}

export interface ResetPasswordRequest {
  token: string
  newPassword: string
  confirmPassword: string
}
