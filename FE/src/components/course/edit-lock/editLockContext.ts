import { createContext, useContext } from 'react'
import type { CourseDetail } from '@/types'

/**
 * Lý do tạm khóa chỉnh sửa nội dung khóa học (BE cũng chặn):
 * - ADMIN_READ_ONLY: quản trị viên chỉ duyệt, không soạn (giáo viên được giao soạn).
 * - PENDING_REVIEW: khóa đang chờ duyệt → giáo viên tạm khóa sửa tới khi ADMIN quyết định / rút yêu cầu.
 *
 * Trang soạn khóa / soạn bài bọc nội dung bằng provider này; các nút thêm / sửa / xóa / sắp xếp / import
 * đọc {@link useCourseEditLocked} để tự làm mờ. Nút chỉ đọc (xem trước, in phiếu, xuất Excel) không bị khóa.
 */
export type EditLockReason = 'ADMIN_READ_ONLY' | 'PENDING_REVIEW'

export const CourseEditLockContext = createContext<EditLockReason | null>(null)

export const useCourseEditLockReason = () => useContext(CourseEditLockContext)

export const useCourseEditLocked = () => useContext(CourseEditLockContext) !== null

const HINTS: Record<EditLockReason, string> = {
  ADMIN_READ_ONLY: 'Quản trị viên chỉ duyệt nội dung — giáo viên được giao soạn',
  PENDING_REVIEW: 'Khóa học đang chờ quản trị viên duyệt — tạm khóa chỉnh sửa',
}

/** Tooltip cho nút bị khóa. */
export const editLockHint = (reason: EditLockReason | null) => (reason ? HINTS[reason] : undefined)

export const useCourseEditLockHint = () => editLockHint(useCourseEditLockReason())

/** ADMIN luôn chỉ xem; giáo viên bị khóa khi khóa đang chờ duyệt. */
export const courseEditLockReason = (
  course: Pick<CourseDetail, 'reviewStatus'> | null | undefined,
  admin: boolean,
): EditLockReason | null => {
  if (admin) return 'ADMIN_READ_ONLY'
  return course?.reviewStatus === 'PENDING_REVIEW' ? 'PENDING_REVIEW' : null
}
