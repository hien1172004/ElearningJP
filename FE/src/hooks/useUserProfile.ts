import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { userService } from '@/services'
import { useAuthStore } from '@/stores'
import type { ApiResponse, UpdateProfileRequest, UserProfile } from '@/types'

export const USER_PROFILE_QUERY_KEY = ['user-profile']

export function useUserProfile() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<UserProfile>({
    queryKey: USER_PROFILE_QUERY_KEY,
    queryFn: async () => {
      const res = await userService.getMyProfile()
      return res.data!
    },
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  const setUser = useAuthStore((s) => s.setUser)
  const currentUser = useAuthStore((s) => s.user)

  return useMutation({
    mutationFn: async (payload: UpdateProfileRequest) => {
      return userService.updateMyProfile(payload)
    },
    onSuccess: (res) => {
      const updated = res.data
      if (updated && currentUser) {
        setUser({
          ...currentUser,
          fullName: updated.fullName,
          phone: updated.phoneNumber,
        })
      }
      queryClient.invalidateQueries({ queryKey: USER_PROFILE_QUERY_KEY })
      toast.success('Thành công', {
        description: 'Thông tin cá nhân đã được cập nhật.',
      })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ||
        'Cập nhật thông tin thất bại. Vui lòng thử lại.'
      toast.error('Lỗi', { description: msg })
    },
  })
}

export function useChangeAvatar() {
  const queryClient = useQueryClient()
  const setUser = useAuthStore((s) => s.setUser)
  const currentUser = useAuthStore((s) => s.user)

  return useMutation({
    mutationFn: async (file: File) => {
      return userService.changeAvatar(file)
    },
    onSuccess: (res) => {
      const newAvatarUrl = res.data
      if (newAvatarUrl && currentUser) {
        setUser({
          ...currentUser,
          avatarUrl: newAvatarUrl,
        })
      }
      queryClient.invalidateQueries({ queryKey: USER_PROFILE_QUERY_KEY })
      toast.success('Thành công', {
        description: 'Ảnh đại diện đã được cập nhật.',
      })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ||
        'Tải lên ảnh đại diện thất bại. Vui lòng thử lại.'
      toast.error('Lỗi', { description: msg })
    },
  })
}
