import { useMutation } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { authService } from '@/services/auth.service'
import { userService } from '@/services/user.service'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  extractRolesFromToken,
  extractUserIdFromToken,
  extractFullNameFromToken,
  extractAvatarUrlFromToken,
} from '@/utils/jwt'
import type { ApiResponse, LoginRequest, LoginResponse, User } from '@/types'

interface UseLoginOptions {
  remember?: boolean
  redirectTo?: string
}

/**
 * Payload login có thể kèm {@code __redirectTo} để override đích điều hướng
 * sau khi đăng nhập thành công (dùng khi login từ modal, nơi redirectTo
 * được truyền theo từng lần gọi thay vì lúc khởi tạo hook).
 */
type LoginPayload = LoginRequest & { __redirectTo?: string }

export function useLogin(options: UseLoginOptions = {}) {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)

  return useMutation({
    mutationFn: async (payload: LoginPayload) => {
      const { __redirectTo, ...rest } = payload
      return authService.login(rest as LoginRequest)
    },
    onSuccess: (res, variables) => {
      const data: LoginResponse | undefined = res?.data
      if (!data?.accessToken) {
        toast.error('Đăng nhập thất bại', {
          description: data?.error || 'Phản hồi không hợp lệ từ máy chủ.',
        })
        return
      }

      const remember = options.remember ?? true

      // Lấy roles + id + fullName + avatarUrl từ JWT
      const scopeRoles = extractRolesFromToken(data.accessToken)
      const userId = extractUserIdFromToken(data.accessToken)
      const tokenFullName = extractFullNameFromToken(data.accessToken)
      const tokenAvatarUrl = extractAvatarUrlFromToken(data.accessToken)

      const user: User = {
        id: userId ?? 0,
        email: variables.email,
        fullName: tokenFullName || '',
        avatarUrl: tokenAvatarUrl,
        role: scopeRoles[0] ?? 'STUDENT',
        roles: scopeRoles,
      }

      setAuth({
        user,
        accessToken: data.accessToken,
        refreshToken: null,
        remember,
      })

      // Nạp profile đầy đủ nhất từ server ngay lúc đăng nhập
      userService
        .getMyProfile()
        .then((profileRes) => {
          if (profileRes?.data) {
            useAuthStore.getState().setUser({
              ...user,
              id: profileRes.data.id || user.id,
              email: profileRes.data.email || user.email,
              fullName: profileRes.data.fullName || user.fullName,
              avatarUrl: profileRes.data.avatarUrl || user.avatarUrl,
              phone: profileRes.data.phoneNumber,
              role: profileRes.data.roles?.[0] ?? user.role,
              roles: profileRes.data.roles ?? user.roles,
            })
          }
        })
        .catch(() => {
          // Bỏ qua nếu lỗi mạng, đã có thông tin trích xuất từ JWT
        })

      toast.success('Đăng nhập thành công', {
        description: 'Chào mừng bạn quay lại với ElearningJP!',
      })

      // Ưu tiên __redirectTo truyền theo lần gọi (từ modal) → fallback option → '/'
      const redirectTo = variables.__redirectTo || options.redirectTo || '/'
      navigate(redirectTo, { replace: true })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const code = error.response?.data?.code
      const msg =
        code === 1006 || code === 1005 || code === 1009
          ? 'Email hoặc mật khẩu không đúng.'
          : (error.response?.data?.message ??
            'Đăng nhập thất bại. Vui lòng thử lại.')
      toast.error('Đăng nhập thất bại', { description: msg })
    },
  })
}
