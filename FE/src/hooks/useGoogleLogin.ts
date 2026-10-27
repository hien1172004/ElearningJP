import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services'
import { useAuthStore } from '@/stores'
import { extractRolesFromToken, extractUserIdFromToken, extractEmailFromToken } from '@/utils/jwt'
import type { ApiResponse, LoginResponse, User } from '@/types'

interface UseGoogleLoginOptions {
  redirectTo?: string
}

export function useGoogleLogin(options: UseGoogleLoginOptions = {}) {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation({
    mutationFn: async (idToken: string) => {
      return authService.googleLogin(idToken)
    },
    onSuccess: (res) => {
      const data: LoginResponse | undefined = res?.data
      if (!data?.accessToken) {
        toast.error('Đăng nhập Google thất bại', {
          description: data?.error || 'Phản hồi không hợp lệ từ máy chủ.',
        })
        return
      }

      const scopeRoles = extractRolesFromToken(data.accessToken)
      const userId = extractUserIdFromToken(data.accessToken)
      const email = extractEmailFromToken(data.accessToken)

      const user: User = {
        id: userId ?? 0,
        email,
        fullName: '',
        role: scopeRoles[0] ?? 'STUDENT',
        roles: scopeRoles,
      }

      setAuth({
        user,
        accessToken: data.accessToken,
        refreshToken: null,
        remember: true,
      })

      toast.success('Đăng nhập Google thành công', {
        description: 'Chào mừng bạn đến với ElearningJP!',
      })

      const redirectTo = options.redirectTo || '/'
      navigate(redirectTo, { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ||
        'Đăng nhập Google thất bại. Vui lòng thử lại.'
      toast.error('Đăng nhập Google thất bại', { description: msg })
    },
  })
}
