import { createContext, useContext } from 'react'
import type { CourseDetail, LessonReviewState } from '@/types'

/**
 * Lý do tạm khóa chỉnh sửa nội dung (BE cũng chặn) — V65: mỗi phía (giáo viên / quản trị viên) sửa nháp riêng và
 * dùng chung 1 khóa sửa khóa học của phía mình:
 * - PENDING_REVIEW: cả khóa đang chờ duyệt (luồng duyệt theo khóa) → giáo viên tạm khóa sửa.
 * - LESSON_PENDING_REVIEW: giáo viên đã gửi duyệt bài này → nháp giáo viên chỉ đọc tới khi ADMIN duyệt / từ chối hoặc
 *   giáo viên rút yêu cầu (nháp ADMIN không bị ảnh hưởng).
 * - REVIEWS_PENDING: (ADMIN) khóa học còn bài chờ duyệt / cả khóa chờ duyệt → duyệt / từ chối hết trước khi sửa
 *   (chặn theo khóa học).
 * - LOCKED_BY_OTHER: người cùng phía đang sửa khóa học → chỉ xem (có thể tiếp quản).
 * - NOT_EDITING: chưa bấm "Sửa khóa học" (phải giữ khóa sửa mới ghi được nội dung).
 *
 * Trang soạn khóa / soạn bài bọc nội dung bằng provider này; các nút thêm / sửa / xóa / sắp xếp / import
 * đọc {@link useCourseEditLocked} để tự làm mờ. Nút chỉ đọc (xem trước, in phiếu, xuất Excel) không bị khóa.
 */
export type EditLockReason =
  | 'PENDING_REVIEW'
  | 'LESSON_PENDING_REVIEW'
  | 'REVIEWS_PENDING'
  | 'LOCKED_BY_OTHER'
  | 'NOT_EDITING'

export const CourseEditLockContext = createContext<EditLockReason | null>(null)

export const useCourseEditLockReason = () => useContext(CourseEditLockContext)

export const useCourseEditLocked = () => useContext(CourseEditLockContext) !== null

const HINTS: Record<EditLockReason, string> = {
  PENDING_REVIEW: 'Khóa học đang chờ quản trị viên duyệt — tạm khóa chỉnh sửa',
  LESSON_PENDING_REVIEW: 'Bài học đã gửi duyệt — tạm khóa chỉnh sửa tới khi được duyệt / từ chối hoặc rút yêu cầu',
  REVIEWS_PENDING: 'Khóa học còn bài chờ duyệt — duyệt / từ chối hết trước khi sửa',
  LOCKED_BY_OTHER: 'Người khác đang sửa khóa học này — bạn chỉ xem được (có thể tiếp quản)',
  NOT_EDITING: 'Bấm "Sửa khóa học" để bắt đầu sửa',
}

/** Tooltip cho nút bị khóa. */
export const editLockHint = (reason: EditLockReason | null) => (reason ? HINTS[reason] : undefined)

export const useCourseEditLockHint = () => editLockHint(useCourseEditLockReason())

/** Trạng thái khóa sửa khóa học của phía người dùng hiện tại ({@code useCourseEditLock}). */
export interface EditSessionState {
  mine: boolean
  heldByOther: boolean
}

/** Số bài chờ duyệt của khóa (ADMIN bị chặn sửa khi > 0). */
export const countPendingReviews = (states: Pick<LessonReviewState, 'reviewStatus'>[] | null | undefined) =>
  (states ?? []).filter((s) => s.reviewStatus === 'PENDING_REVIEW').length

/**
 * Trang soạn khóa, theo thứ tự ưu tiên: khóa chờ duyệt cả khóa (giáo viên) → người cùng phía đang sửa → chưa bấm
 * "Sửa khóa học". {@code session} chưa tải → coi như chưa sửa (BE vẫn chặn).
 */
export const courseEditLockReason = (
  course: Pick<CourseDetail, 'reviewStatus'> | null | undefined,
  admin: boolean,
  session: EditSessionState | null | undefined,
  pendingReviews = 0,
): EditLockReason | null => {
  if (!admin && course?.reviewStatus === 'PENDING_REVIEW') return 'PENDING_REVIEW'
  if (admin && (pendingReviews > 0 || course?.reviewStatus === 'PENDING_REVIEW')) return 'REVIEWS_PENDING'
  if (session?.heldByOther) return 'LOCKED_BY_OTHER'
  if (!session?.mine) return 'NOT_EDITING'
  return null
}

/**
 * Trang soạn 1 bài: như {@link courseEditLockReason}, thêm bài đã gửi duyệt (chỉ khóa nháp giáo viên).
 * {@code lessonState} chưa tải → không xét (BE vẫn chặn).
 */
export const lessonEditLockReason = (
  course: Pick<CourseDetail, 'reviewStatus'> | null | undefined,
  lessonState: Pick<LessonReviewState, 'reviewStatus'> | null | undefined,
  admin: boolean,
  session: EditSessionState | null | undefined,
  pendingReviews = 0,
): EditLockReason | null => {
  if (!admin && course?.reviewStatus === 'PENDING_REVIEW') return 'PENDING_REVIEW'
  if (!admin && lessonState?.reviewStatus === 'PENDING_REVIEW') return 'LESSON_PENDING_REVIEW'
  return courseEditLockReason(course, admin, session, pendingReviews)
}
