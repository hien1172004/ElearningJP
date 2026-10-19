import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import type { ApiResponse, ForgotPasswordRequest } from '@/types'

export function useForgotPassword() {
  const navigate = useNavigate()

  return useMutation({
    mutationFn: async (payload: ForgotPasswordRequest) => {
      return authService.forgotPassword(payload)
    },
    onSuccess: (res) => {
      toast.success('Đã gửi email đặt lại mật khẩu', {
        description:
          res?.message ??
          'Nếu email tồn tại, link đặt lại mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.',
      })
      setTimeout(() => navigate('/login'), 1200)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Gửi yêu cầu thất bại', {
        description:
          error.response?.data?.message ??
          'Vui lòng kiểm tra lại email và thử lại sau.',
      })
    },
  })
}
