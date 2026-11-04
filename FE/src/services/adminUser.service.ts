import { axiosClient } from './api/axiosClient'
import type {
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

  /** Gán lại role (USER luôn được BE giữ lại). */
  updateRoles: async (id: number, roles: string[]): Promise<ApiResponse<AdminUser>> => {
    return axiosClient.put(`/admin/users/${id}/roles`, { roles })
  },
}
