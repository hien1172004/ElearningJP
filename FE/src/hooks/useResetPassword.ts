import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import type { ApiResponse, ResetPasswordRequest } from '@/types'

export function useResetPassword() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async (payload: ResetPasswordRequest) => {
      return authService.resetPassword(payload)
    },
    onSuccess: (res) => {
      toast.success('Đặt lại mật khẩu thành công', {
        description:
          res?.message ??
          'Mật khẩu của bạn đã được cập nhật. Vui lòng đăng nhập lại.',
      })
      // Quay về trang chủ — user mở modal đăng nhập từ header.
      setTimeout(() => navigate('/'), 1200)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const code = error.response?.data?.code
      let msg =
        error.response?.data?.message ??
        'Không thể đặt lại mật khẩu. Vui lòng thử lại.'

      if (code === 1014) msg = 'Token đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.'
      else if (code === 1012) msg = 'Mật khẩu xác nhận không khớp.'
      else if (code === 1015) msg = 'Mật khẩu mới phải khác mật khẩu cũ.'

      toast.error('Đặt lại mật khẩu thất bại', { description: msg })
    },
  })
}
