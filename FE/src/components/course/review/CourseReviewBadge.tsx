import React from 'react'
import type { CourseSummary } from '@/types'

const BADGE = 'px-2.5 py-1 rounded-md text-xs font-bold border whitespace-nowrap'

/**
 * Trạng thái khóa học theo 2 chiều: hiển thị với học viên (xuất bản / nháp)
 * và bản đang soạn (chờ duyệt / có thay đổi chưa duyệt).
 */
export const CourseReviewBadge: React.FC<{
  course: Pick<CourseSummary, 'published' | 'reviewStatus' | 'hasUnpublishedChanges'>
}> = ({ course }) => (
  <span className="inline-flex flex-wrap items-center gap-1.5">
    {course.published ? (
      <span className={`${BADGE} bg-emerald-50 text-emerald-700 border-emerald-200`}>Đã xuất bản</span>
    ) : (
      <span className={`${BADGE} bg-gray-100 text-gray-600 border-gray-200`}>Bản nháp</span>
    )}
    {course.reviewStatus === 'PENDING_REVIEW' ? (
      <span className={`${BADGE} bg-amber-50 text-amber-700 border-amber-200`}>Chờ duyệt</span>
    ) : course.hasUnpublishedChanges ? (
      <span className={`${BADGE} bg-sky-50 text-sky-700 border-sky-200`}>Có thay đổi chưa duyệt</span>
    ) : null}
  </span>
)
