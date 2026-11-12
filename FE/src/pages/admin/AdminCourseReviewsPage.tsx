import React from 'react'
import { LessonReviewQueue } from '@/components/course'

/**
 * Hàng chờ duyệt (ADMIN): duyệt theo bài là luồng duyệt duy nhất — xem bản giáo viên đã gửi, duyệt (bài lên bản học
 * viên đang học) hoặc từ chối kèm lý do. Luồng gửi / duyệt cả khóa đã bỏ.
 */
export const AdminCourseReviewsPage: React.FC = () => (
  <div className="space-y-6 pb-12">
    <div className="max-w-7xl mx-auto px-3 sm:px-4">
      <LessonReviewQueue />
    </div>
  </div>
)
