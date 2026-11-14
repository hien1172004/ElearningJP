import React, { useState } from 'react'
import { Button as AntdButton } from 'antd'
import { RefreshCw } from 'lucide-react'
import { useCourseContentUpdates, type CourseContentAudience } from '@/hooks'

const MESSAGE: Record<CourseContentAudience, { title: string; body: string }> = {
  editor: {
    title: 'Nội dung khóa học vừa được người khác cập nhật',
    body: 'Tải lại để thấy bản mới nhất trước khi sửa tiếp, tránh ghi đè lên thay đổi của người khác.',
  },
  learner: {
    title: 'Khóa học vừa có nội dung mới',
    body: 'Tải lại để học phiên bản mới nhất. Phần đang làm dở (vd: bài kiểm tra chưa nộp) có thể bị mất.',
  },
}

/**
 * Nháp của bài đang mở đã cũ (phía kia vừa đưa bài lên bản học viên, mình đã sửa bài): tải lại không đổi nội dung
 * (trang soạn hiện nháp phía mình) → hướng dẫn bấm "Cập nhật bản mới" trên nội dung bài.
 */
const outdatedMessage = (admin: boolean) => ({
  title: admin ? 'Bài này vừa được duyệt lên bản học viên' : 'Quản trị viên vừa lưu bài này',
  body:
    'Bấm "Cập nhật bản mới" ở trên nội dung bài để lấy nội dung mới — phần bạn đã sửa được giữ ở "Xem nháp cũ" ' +
    'để chép lại.',
})

interface CourseContentUpdateNoticeProps {
  courseId?: number
  audience: CourseContentAudience
  /**
   * Trang soạn bài: nháp phía mình của bài đang mở đã cũ ({@code LessonReviewState.outdated}) → đổi lời nhắn sang
   * "Cập nhật bản mới", nút thành "Đã hiểu".
   */
  draftOutdated?: boolean
  /** Người xem là ADMIN (chọn lời nhắn khi nháp đã cũ). */
  admin?: boolean
}

/**
 * Thông báo nổi (góc dưới) khi nội dung khóa đổi trong lúc đang mở trang; bấm Tải lại để làm mới dữ liệu.
 * Hiệu ứng để nổi bật (định nghĩa ở index.css, tắt khi người dùng giảm chuyển động):
 * trượt lên + nảy, viền phát sáng nhấp nháy, icon xoay chậm, vòng sóng quanh nút Tải lại.
 */
export const CourseContentUpdateNotice: React.FC<CourseContentUpdateNoticeProps> = ({
  courseId,
  audience,
  draftOutdated = false,
  admin = false,
}) => {
  const { stale, reload } = useCourseContentUpdates(courseId, audience)
  const [reloading, setReloading] = useState(false)
  if (!stale) return null

  const outdated = audience === 'editor' && draftOutdated
  const text = outdated ? outdatedMessage(admin) : MESSAGE[audience]
  const handleReload = async () => {
    setReloading(true)
    try {
      await reload()
    } finally {
      setReloading(false)
    }
  }

  return (
    <div
      role="alert"
      aria-live="assertive"
      className="content-notice fixed bottom-4 left-4 right-4 sm:left-auto sm:right-6 sm:max-w-sm z-[1000] overflow-hidden rounded-xl border border-indigo-300 bg-surface"
    >
      {/* Dải màu chủ đạo bên trái để thẻ tách khỏi nền trang */}
      <span aria-hidden className="absolute inset-y-0 left-0 w-1.5 bg-gradient-to-b from-indigo-500 to-purple-600" />
      <div className="p-4 pl-5 space-y-2">
        <p className="text-sm font-bold text-gray-900 flex items-center gap-2">
          <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-indigo-100">
            <RefreshCw className="content-notice-icon w-4 h-4 text-indigo-600" />
          </span>
          {text.title}
        </p>
        <p className="text-xs text-gray-600">{text.body}</p>
        <div className="flex justify-end">
          <span className="relative inline-flex">
            {/* Vòng sóng lan ra quanh nút (ẩn khi đang tải) */}
            {!reloading && (
              <span aria-hidden className="content-notice-wave absolute inset-0 rounded-md bg-indigo-400" />
            )}
            <AntdButton
              type="primary"
              loading={reloading}
              icon={outdated ? undefined : <RefreshCw className="w-4 h-4" />}
              onClick={handleReload}
              className="relative !rounded-md !font-semibold"
            >
              {/* Nháp đã cũ: vẫn làm mới dữ liệu khác (cấu trúc khóa, bài khác), nội dung bài chỉ đổi khi Cập nhật bản mới */}
              {outdated ? 'Đã hiểu' : 'Tải lại'}
            </AntdButton>
          </span>
        </div>
      </div>
    </div>
  )
}
