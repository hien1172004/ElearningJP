import type { User } from '@/types'

/**
 * Tên role chuẩn — khớp với `PredefinedRole` ở BE:
 *   BE/elearningJP/.../shared/constants/PredefinedRole.java
 */
export const ROLE_ADMIN = 'ADMIN'
export const ROLE_TEACHER = 'TEACHER'
export const ROLE_USER = 'USER'

/**
 * Chuẩn hoá tên role về dạng chữ in hoa và bỏ prefix "ROLE_".
 *
 * Lý do: role có thể xuất hiện dưới 2 dạng khác nhau trong hệ thống:
 *  - "ADMIN"     — đọc từ entity `Role.name` (BE `PredefinedRole`)
 *  - "ROLE_ADMIN" — đọc từ authority của Spring Security
 * FE cần so khớp được cả hai nếu không muốn bỏ sót quyền.
 */
export const normalizeRole = (role?: string | null): string => {
  if (!role) return ''
  return role.trim().replace(/^ROLE_/i, '').toUpperCase()
}

/**
 * Lấy toàn bộ role của user, gộp cả `user.roles` (mảng) và `user.role` (chuỗi).
 * Bỏ trùng lặp và trả về dạng đã chuẩn hoá.
 */
export const getUserRoles = (user?: User | null): string[] => {
  if (!user) return []

  const raw: string[] = [
    ...(Array.isArray(user.roles) ? user.roles : []),
    ...(user.role ? [user.role] : []),
  ]

  const normalized = raw.map(normalizeRole).filter(Boolean)
  return Array.from(new Set(normalized))
}

/**
 * User có ít nhất 1 role nằm trong `allowed` không?
 * `allowed` rỗng => true (không giới hạn quyền).
 *
 * Lưu ý: đây chỉ là kiểm tra phía UI để ẩn/hiện menu.
 * BE vẫn là nơi enforce security thực sự.
 */
export const hasRole = (
  user: User | null | undefined,
  allowed: readonly string[],
): boolean => {
  if (!allowed.length) return true

  const wanted = allowed.map(normalizeRole)
  return getUserRoles(user).some((role) => wanted.includes(role))
}

/** User có role ADMIN hay không. */
export const isAdmin = (user?: User | null): boolean =>
  hasRole(user, [ROLE_ADMIN])

/** User có role TEACHER hay không. */
export const isTeacher = (user?: User | null): boolean =>
  hasRole(user, [ROLE_TEACHER])

/** User có role ADMIN hoặc TEACHER hay không (dùng cho menu "Quản lý"). */
export const isTeacherOrAdmin = (user?: User | null): boolean =>
  hasRole(user, [ROLE_ADMIN, ROLE_TEACHER])
