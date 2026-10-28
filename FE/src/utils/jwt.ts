/**
 * JWT helpers — chỉ dùng để ĐỌC claims phía FE cho mục đích UI (role check, hiển thị).
 * KHÔNG verify chữ ký ở đây. BE vẫn là nơi enforce security thực sự.
 *
 * Cấu trúc payload BE build (xem AuthService.generateToken):
 *   {
 *     sub: "<email>",
 *     iss: "E-Learning JP Service",
 *     iat, exp, jti,
 *     scope: "ROLE_USER ROLE_TEACHER ...",   // chuỗi space-separated, có prefix "ROLE_"
 *     userId: <number>
 *   }
 */

export interface JwtPayload {
  sub?: string
  userId?: number
  email?: string
  iss?: string
  iat?: number
  exp?: number
  jti?: string
  scope?: string
  [key: string]: unknown
}

/**
 * Decode phần payload (giữa) của JWT mà KHÔNG verify chữ ký.
 * Hỗ trợ base64url chuẩn JWT (chuyển `-_` → `+/` và thêm padding `=`).
 */
export function decodeJwt(token: string | null | undefined): JwtPayload | null {
  if (!token || typeof token !== 'string') return null
  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64 + '='.repeat((4 - (base64.length % 4)) % 4)
    return JSON.parse(atob(padded)) as JwtPayload
  } catch {
    return null
  }
}

/**
 * Trích danh sách roles từ claim "scope".
 * BE build dạng: "ROLE_USER ROLE_TEACHER ROLE_ADMIN"
 * Hàm này strip prefix "ROLE_" → trả ["USER", "TEACHER", "ADMIN"].
 * Trả mảng rỗng nếu token không hợp lệ hoặc không có claim scope.
 */
export function extractRolesFromToken(token: string | null | undefined): string[] {
  if (!token) return []
  const payload = decodeJwt(token)
  const scope = payload?.scope
  if (typeof scope !== 'string' || !scope.trim()) return []
  return scope
    .split(' ')
    .filter(Boolean)
    .map((s) => s.replace(/^ROLE_/, ''))
}

/**
 * Trích userId từ claim "userId" (number). Nếu không có, fallback parse "sub".
 * Trả null nếu không suy ra được id hợp lệ.
 */
export function extractUserIdFromToken(token: string | null | undefined): number | null {
  if (!token) return null
  const payload = decodeJwt(token)
  if (typeof payload?.userId === 'number' && Number.isFinite(payload.userId)) {
    return payload.userId
  }
  if (typeof payload?.sub === 'string' && payload.sub.trim()) {
    const parsed = Number(payload.sub)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

/**
 * Trích email từ claim "sub" (BE set subject = user.getEmail()).
 * Trả '' nếu không đọc được.
 */
export function extractEmailFromToken(token: string | null | undefined): string {
  if (!token) return ''
  const payload = decodeJwt(token)
  return typeof payload?.sub === 'string' ? payload.sub : ''
}

/**
 * Kiểm tra token có chứa ít nhất 1 role nằm trong `allowed` không.
 * Hữu ích cho client-side guard. BE vẫn là nguồn sự thật cuối cùng.
 */
export function hasAnyRole(
  token: string | null | undefined,
  allowed: ReadonlyArray<string>,
): boolean {
  if (!allowed.length) return true
  const roles = extractRolesFromToken(token)
  return roles.some((r) => allowed.includes(r))
}
