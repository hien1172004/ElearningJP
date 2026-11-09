import React from 'react'
import { Lock } from 'lucide-react'
import { useCourseEditLockReason } from './editLockContext'

/** Thanh báo khóa sửa (chỉ hiện khi đang bị khóa). */
export const CourseEditLockBanner: React.FC = () => {
  const reason = useCourseEditLockReason()
  // if (reason === 'ADMIN_READ_ONLY') {
  //   return (
  //     <div className="rounded-lg border border-indigo-200 bg-indigo-50/70 px-3 py-2 text-sm text-indigo-800 flex items-start gap-2">
  //       <Eye className="w-4 h-4 mt-0.5 shrink-0" />
  //       <p>
  //         Quản trị viên chỉ xem và duyệt nội dung; giáo viên được giao soạn bài. Ghi nhận xét cho giáo viên ở mục{' '}
  //         <strong>Nhận xét duyệt</strong> khi khóa đang chờ duyệt.
  //       </p>
  //     </div>
  //   )
  // }
  if (reason === 'PENDING_REVIEW') {
    return (
      <div className="rounded-lg border border-amber-200 bg-amber-50/70 px-3 py-2 text-sm text-amber-800 flex items-start gap-2">
        <Lock className="w-4 h-4 mt-0.5 shrink-0" />
        <p>
          Khóa học đang chờ quản trị viên duyệt nên tạm khóa chỉnh sửa. Bạn vẫn xem được nội dung; muốn sửa tiếp hãy
          rút yêu cầu ở mục <strong>Duyệt &amp; xuất bản</strong>.
        </p>
      </div>
    )
  }
  return null
}
