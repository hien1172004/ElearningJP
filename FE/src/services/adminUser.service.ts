import { axiosClient } from './api/axiosClient'
import type {
  AdminCreateUserRequest,
  AdminUser,
  AdminUserListParams,
  ApiResponse,
  PageResponse,
  UserStatus,
} from '@/types'

/** API quản lý người dùng (chỉ ADMIN) — BE: AdminUserController `/admin/users`. */
export const adminUserService = {
  getUsers: async (
    params: AdminUserListParams,
  ): Promise<ApiResponse<PageResponse<AdminUser>>> => {
    return axiosClient.get('/admin/users', { params })
  },

  getUser: async (id: number): Promise<ApiResponse<AdminUser>> => {
    return axiosClient.get(`/admin/users/${id}`)
  },

  /** Khóa / mở khóa tài khoản. INACTIVE/BANNED sẽ đăng xuất user khỏi mọi thiết bị. */
  updateStatus: async (id: number, status: UserStatus): Promise<ApiResponse<AdminUser>> => {
    return axiosClient.patch(`/admin/users/${id}/status`, { status })
  },

  /** Cấp phát tài khoản giáo viên / quản trị viên (đúng 1 role). */
  createUser: async (payload: AdminCreateUserRequest): Promise<ApiResponse<AdminUser>> => {
    return axiosClient.post('/admin/users', payload)
  },
}
