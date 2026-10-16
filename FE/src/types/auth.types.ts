export interface User {
  id: number
  email: string
  fullName: string
  avatarUrl?: string
  role: string
}

export interface LoginRequest {
  email: string
  passwordHash: string
}

export interface LoginResponse {
  token: string
  authenticated: boolean
  user?: User
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

export interface RegisterResponse {
  id: number
  email: string
  fullName: string
}
