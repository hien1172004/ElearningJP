import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  ChangePasswordRequest,
  UpdateProfileRequest,
  UserProfile,
} from '@/types'

export const userService = {
  /**
   * Lấy thông tin chi tiết user đang đăng nhập
   */
  getMyProfile: async (): Promise<ApiResponse<UserProfile>> => {
    return axiosClient.get('/users/me')
  },

  /**
   * Cập nhật thông tin profile cá nhân và mục tiêu học tập
   */
  updateMyProfile: async (
    payload: UpdateProfileRequest,
  ): Promise<ApiResponse<UserProfile>> => {
    return axiosClient.put('/users/me', payload)
  },

  /**
   * Đổi ảnh đại diện (upload lên Cloudinary qua backend)
   */
  changeAvatar: async (file: File): Promise<ApiResponse<string>> => {
    const formData = new FormData()
    formData.append('file', file)
    return axiosClient.post('/users/change-avatar', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  /**
   * Đổi mật khẩu
   */
  changePassword: async (
    payload: ChangePasswordRequest,
  ): Promise<ApiResponse<void>> => {
    return axiosClient.put('/users/change-password', payload)
  },
}
