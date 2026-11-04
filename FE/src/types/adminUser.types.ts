import type { UserStatus } from './auth.types'

/** Khớp `AuthProvider` ở BE. */
export type AuthProvider = 'LOCAL' | 'GOOGLE'

/** Role có thể gán qua trang quản trị (khớp `PredefinedRole`). */
export type AssignableRole = 'USER' | 'TEACHER' | 'ADMIN'

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
