import type { UserStatus } from './auth.types'

/** Khớp `AuthProvider` ở BE. */
export type AuthProvider = 'LOCAL' | 'GOOGLE'

/** Role có thể gán qua trang quản trị (khớp `PredefinedRole`). */
export type AssignableRole = 'USER' | 'TEACHER' | 'ADMIN'

/** Role được cấp khi admin tạo tài khoản (mỗi tài khoản chỉ có 1 role). */
export type ProvisionRole = 'TEACHER' | 'ADMIN'

/** Khớp `AdminCreateUserRequest` ở BE (POST /admin/users). */
export interface AdminCreateUserRequest {
  email: string
  fullName: string
  password: string
  role: ProvisionRole
}

/** Khớp `AdminUserResponse` ở BE. */
export interface AdminUser {
  id: number
  email: string
  fullName: string
  avatarUrl?: string | null
  authProvider: AuthProvider
  status: UserStatus
  roles: string[]
  emailVerifiedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface AdminUserListParams {
  keyword?: string
  role?: AssignableRole
  status?: UserStatus
  page?: number
  size?: number
}
