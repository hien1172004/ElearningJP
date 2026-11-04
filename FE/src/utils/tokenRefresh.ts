import { storage } from '@/utils/storage'
import { decodeJwt } from '@/utils/jwt'
import { useAuthStore } from '@/stores/useAuthStore'
import { authService } from '@/services/auth.service'

/**
 * Cấp lại access token — dùng chung cho interceptor 401, AuthInitializer (reload trang)
 * và bộ hẹn giờ refresh chủ động.
 *
 * Single-flight: nhiều nơi gọi cùng lúc chỉ phát sinh 1 request `/auth/refresh`
 * (BE blacklist token cũ sau khi refresh → gọi song song bằng token cũ sẽ thất bại).
 *
 * BE cho phép refresh token đã hết hạn trong cửa sổ `jwt.refreshable-duration`
 * (mặc định 24h kể từ lúc phát hành), không phụ thuộc cờ "Remember me".
 */
let inflight: Promise<string> | null = null

export function refreshAccessToken(): Promise<string> {
  if (inflight) return inflight

  const current = storage.getAccessToken()
  if (!current) return Promise.reject(new Error('No access token'))

  inflight = authService
    .refresh(current)
    .then((res) => {
      const newToken = res?.data?.token
      if (!newToken) throw new Error('No token in refresh response')
      useAuthStore.getState().updateAccessToken(newToken)
      return newToken
    })
    .finally(() => {
      inflight = null
    })

  return inflight
}

/** Số giây còn lại trước khi token hết hạn (âm nếu đã hết hạn, null nếu không đọc được). */
export function secondsUntilExpiry(token: string | null): number | null {
  if (!token) return null
  const exp = decodeJwt(token)?.exp
  if (typeof exp !== 'number') return null
  return exp - Math.floor(Date.now() / 1000)
}
