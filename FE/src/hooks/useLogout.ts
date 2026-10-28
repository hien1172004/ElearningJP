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

  return useMutation({
    mutationFn: async () => {
      const token = storage.getAccessToken()
      if (!token) return null
      return authService.logout(token)
    },
    onSuccess: () => {
      clearAuth()
      toast.success('Đã đăng xuất', {
        description: 'Hẹn gặp lại bạn trong lần học tới!',
      })
      navigate('/', { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      // Dù BE lỗi, vẫn logout mềm phía FE.
      clearAuth()
      toast.warning('Đã đăng xuất (cục bộ)', {
        description:
          error.response?.data?.message ??
          'Không thể thông báo tới máy chủ, nhưng bạn đã đăng xuất trên thiết bị này.',
      })
      navigate('/', { replace: true })
    },
  })
}
