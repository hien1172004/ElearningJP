import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import { useAuthStore } from '@/stores/useAuthStore'
import type {
  ApiResponse,
  RegisterRequest,
  RegisterResponse,
  User,
} from '@/types'

/**
 * Payload register có thể kèm {@code __redirectTo} để override đích điều hướng
 * sau khi đăng ký thành công (dùng khi register từ modal).
 */
type RegisterPayload = RegisterRequest & { __redirectTo?: string }

export function useRegister() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation({
    mutationFn: async (payload: RegisterPayload) => {
      const { __redirectTo, ...rest } = payload
      return authService.register(rest as RegisterRequest)
    },
    onSuccess: (res, variables) => {
      const data: RegisterResponse | undefined = res?.data
      const token = data?.token

      if (!token) {
        toast.error('Đăng ký thất bại', {
          description: data?.error || 'Không nhận được token từ máy chủ.',
        })
        return
      }

      const user: User = {
        id: 0,
        email: data.userInfoResponse?.email ?? variables.email,
        fullName: data.userInfoResponse?.fullName ?? variables.fullName,
        phone: variables.phone,
        role: 'STUDENT',
        status: data.userInfoResponse?.status,
      }

      setAuth({
        user,
        accessToken: token,
        refreshToken: null,
        remember: true,
      })

      toast.success('Đăng ký thành công', {
        description: 'Tài khoản của bạn đã sẵn sàng. Chúc bạn học vui!',
      })

      // Ưu tiên __redirectTo (từ modal) → fallback '/'
      const redirectTo = variables.__redirectTo || '/'
      navigate(redirectTo, { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const code = error.response?.data?.code
      let msg = error.response?.data?.message ?? 'Đăng ký thất bại.'

      if (code === 1001) msg = 'Tài khoản đã tồn tại.'
      else if (code === 1007) msg = 'Email đã được sử dụng.'
      else if (code === 1008) msg = 'Email không hợp lệ.'

      toast.error('Đăng ký thất bại', { description: msg })
    },
  })
}
