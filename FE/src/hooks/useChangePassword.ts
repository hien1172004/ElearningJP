import { useMutation } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { userService } from '@/services'
import type { ApiResponse, ChangePasswordRequest } from '@/types'

export function useChangePassword() {
  return useMutation({
    mutationFn: async (payload: ChangePasswordRequest) => {
      return userService.changePassword(payload)
    },
    onSuccess: () => {
      toast.success('Thành công', {
        description: 'Đổi mật khẩu thành công.',
      })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ||
        'Đổi mật khẩu thất bại. Vui lòng kiểm tra lại thông tin.'
      toast.error('Lỗi', { description: msg })
    },
  })
}
