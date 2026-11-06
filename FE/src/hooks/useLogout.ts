import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/stores/useAuthStore'
import { storage } from '@/utils/storage'
import type { ApiResponse } from '@/types'

export function useLogout() {
  const navigate = useNavigate()
  const clearAuth = useAuthStore((s) => s.clearAuth)

  /**
   * Thứ tự quan trọng:
   *  1. Về trang chủ trước — rời trang cần đăng nhập (dừng timer / query của trang đó).
   *  2. Xoá phiên cục bộ ngay — request sau không còn gắn token cũ.
   *  3. Mới báo BE thu hồi token (endpoint public, token đi trong body).
   *
   * Nếu gọi BE trước: token bị thu hồi khi đang còn ở trang cần đăng nhập, request song song
   * của trang dính 401 → interceptor xoá phiên → `ProtectedRoute` đá sang `/login?redirect=…`.
   */
  return useMutation({
    mutationFn: async () => {
      const token = storage.getAccessToken()
      try {
        await navigate('/', { replace: true })
      } finally {
        clearAuth({ loggedOut: true })
      }
      if (!token) return null
      return authService.logout(token)
    },
    onSuccess: () => {
      toast.success('Đã đăng xuất', {
        description: 'Hẹn gặp lại bạn trong lần học tới!',
      })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      // Phiên cục bộ đã xoá ở mutationFn — chỉ báo BE không nhận được.
      toast.warning('Đã đăng xuất (cục bộ)', {
        description:
          error.response?.data?.message ??
          'Không thể thông báo tới máy chủ, nhưng bạn đã đăng xuất trên thiết bị này.',
      })
    },
  })
}
