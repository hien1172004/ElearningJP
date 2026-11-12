import React from 'react'
import type { CourseSummary } from '@/types'

const BADGE = 'px-2.5 py-1 rounded-md text-xs font-bold border whitespace-nowrap'

type BadgeCourse = Pick<CourseSummary, 'published' | 'reviewStatus' | 'pendingLessonReviews'>

/**
 * Trạng thái khóa học: hiển thị với học viên (xuất bản / nháp), "Chờ duyệt" khi giáo viên đã gửi duyệt cả khóa và
 * "Có n bản đang chờ duyệt" khi có bài gửi duyệt theo bài đang chờ.
 * Không hiện "đang soạn" / "người sửa gần nhất": từ V65 mỗi phía sửa nháp riêng, cờ cấp khóa dùng chung sẽ để lộ
 * việc phía bên kia đang sửa — trạng thái từng bài xem ở mục Duyệt.
 */
export const CourseReviewBadge: React.FC<{ course: BadgeCourse }> = ({ course }) => (
  <span className="inline-flex flex-wrap items-center gap-1.5">
    {course.published ? (
      <span className={`${BADGE} bg-emerald-50 text-emerald-700 border-emerald-200`}>Đã xuất bản</span>
    ) : (
      <span className={`${BADGE} bg-gray-100 text-gray-600 border-gray-200`}>Bản nháp</span>
    )}
    {course.reviewStatus === 'PENDING_REVIEW' && (
      <span className={`${BADGE} bg-amber-50 text-amber-700 border-amber-200`}>Chờ duyệt</span>
    )}
    {!!course.pendingLessonReviews && course.pendingLessonReviews > 0 && (
      <span className={`${BADGE} bg-amber-50 text-amber-700 border-amber-200`}>
        Có {course.pendingLessonReviews} bản đang chờ duyệt
      </span>
    )}
  </span>
)
