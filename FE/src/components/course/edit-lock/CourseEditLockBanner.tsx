import React from 'react'
import { Lock } from 'lucide-react'
import type { CourseEditLock } from '@/types'
import { formatRelativeTime } from '@/utils/format'
import { useCourseEditLockReason } from './editLockContext'

interface CourseEditLockBannerProps {
  /** Người cùng phía đang giữ khóa sửa khóa học (lý do LOCKED_BY_OTHER). */
  lock?: CourseEditLock | null
  /** Số bài chờ duyệt (lý do REVIEWS_PENDING). */
  pendingReviews?: number
}

/** Thanh báo khóa sửa (chỉ hiện khi đang bị khóa). */
export const CourseEditLockBanner: React.FC<CourseEditLockBannerProps> = ({ lock, pendingReviews = 0 }) => {
  const reason = useCourseEditLockReason()
  if (!reason) return null
  const tone =
    reason === 'NOT_EDITING'
      ? 'border-sky-200 bg-sky-50/70 text-sky-800'
      : 'border-amber-200 bg-amber-50/70 text-amber-800'
  return (
    <div role="status" className={`rounded-lg border px-3 py-2 text-sm flex items-start gap-2 ${tone}`}>
      <Lock className="w-4 h-4 mt-0.5 shrink-0" />
      {reason === 'PENDING_REVIEW' && (
        <p>
          Khóa học đang chờ quản trị viên duyệt nên tạm khóa chỉnh sửa. Bạn vẫn xem được nội dung; muốn sửa tiếp hãy
          rút yêu cầu ở mục <strong>Duyệt</strong>.
        </p>
      )}
      {reason === 'LESSON_PENDING_REVIEW' && (
        <p>
          Bài học đã gửi duyệt nên tạm khóa chỉnh sửa. Bạn vẫn xem được nội dung; quản trị viên duyệt / từ chối, hoặc
          giáo viên được giao rút yêu cầu để sửa tiếp.
        </p>
      )}
      {reason === 'REVIEWS_PENDING' && (
        <p>
          <strong>
            Khóa học còn {pendingReviews > 0 ? `${pendingReviews} bài chờ duyệt` : 'yêu cầu duyệt cả khóa'}
          </strong>
          . Hãy duyệt / từ chối hết ở mục <strong>Duyệt</strong> trước khi sửa — để không sửa chồng lên bản giáo viên
          đang chờ.
        </p>
      )}
      {reason === 'LOCKED_BY_OTHER' && (
        <p>
          <strong>{lock?.userName || 'Người khác'} đang sửa khóa học này</strong>
          {lock?.since && <> (từ {formatRelativeTime(lock.since)})</>}. Bạn vẫn xem được; bấm{' '}
          <strong>Tiếp quản</strong> nếu cần sửa ngay.
        </p>
      )}
      {reason === 'NOT_EDITING' && (
        <p>
          Bạn đang xem. Bấm <strong>Sửa khóa học</strong> để bắt đầu sửa — trong lúc bạn sửa, người cùng phía chỉ xem
          được. Bấm <strong>Xong</strong> khi sửa xong.
        </p>
      )}
    </div>
  )
}
