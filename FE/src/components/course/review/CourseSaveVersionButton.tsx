import React, { useState } from 'react'
import { Button as AntdButton, Modal, Tooltip } from 'antd'
import type { ButtonProps } from 'antd'
import { AlertTriangle, Save } from 'lucide-react'
import { toast } from 'sonner'
import { useCourseChanges, useFetchFreshCourseDetail, useSaveCourseVersion } from '@/hooks'
import type { CourseDetail } from '@/types'
import { CourseChangesView } from './CourseChangesView'

interface CourseSaveVersionButtonProps {
  course: Pick<CourseDetail, 'id' | 'reviewStatus'>
  size?: ButtonProps['size']
}

/**
 * ADMIN "Lưu": chốt bản đang soạn thành phiên bản mới, không cần duyệt (không bật xuất bản).
 * Ẩn khi khóa đang chờ duyệt — lúc đó ADMIN duyệt / từ chối yêu cầu (duyệt cũng chốt phần ADMIN đã sửa).
 *
 * Sửa section / item / quiz không làm mới course detail (staleTime 5 phút) nên cờ
 * {@code hasUnpublishedChanges} trong cache có thể cũ → bấm Lưu thì tải lại khóa rồi mới kiểm tra.
 *
 * Lưu cả khóa chốt nháp của quản trị viên (V65) — phần giáo viên đang soạn không bị đưa lên; trước khi lưu hiện danh
 * sách thay đổi sẽ được chốt. Còn bài chờ duyệt thì BE chặn (duyệt / từ chối trước).
 */
export const CourseSaveVersionButton: React.FC<CourseSaveVersionButtonProps> = ({ course, size }) => {
  const fetchFreshCourse = useFetchFreshCourseDetail()
  // Chỉ tải khi mở hộp xác nhận (refetch thủ công bên dưới)
  const changes = useCourseChanges(course.id, false)
  const saveVersion = useSaveCourseVersion(course.id)
  const [checking, setChecking] = useState(false)
  const [confirm, setConfirm] = useState<{ published: boolean } | null>(null)

  if (course.reviewStatus === 'PENDING_REVIEW') return null

  const handleClick = async () => {
    setChecking(true)
    let fresh: CourseDetail | null
    try {
      // Danh sách thay đổi tải song song để mở hộp xác nhận là thấy ngay
      const [freshCourse] = await Promise.all([fetchFreshCourse(course.id), changes.refetch()])
      fresh = freshCourse
    } catch {
      toast.error('Không tải được trạng thái khóa học', { description: 'Vui lòng thử lại.' })
      return
    } finally {
      setChecking(false)
    }

    if (!fresh) return
    if (fresh.reviewStatus === 'PENDING_REVIEW') {
      toast.info('Khóa học đang chờ duyệt', { description: 'Hãy duyệt hoặc từ chối yêu cầu ở mục Duyệt.' })
      return
    }
    if (!fresh.hasUnpublishedChanges) {
      toast.info('Chưa có thay đổi nào để lưu')
      return
    }
    if ((fresh.lessons?.length ?? 0) === 0) {
      toast.info('Khóa học cần có ít nhất 1 bài học trước khi lưu')
      return
    }
    setConfirm({ published: !!fresh.published })
  }

  return (
    <>
      <Tooltip title="Chốt phần đã sửa thành phiên bản mới (không cần duyệt)">
        <AntdButton
          type="primary"
          size={size}
          icon={<Save className="w-4 h-4" />}
          loading={checking || saveVersion.isPending}
          onClick={handleClick}
          className="!rounded-md !font-semibold"
        >
          Lưu
        </AntdButton>
      </Tooltip>

      <Modal
        open={confirm !== null}
        title={<span className="font-bold text-slate-800">Lưu phiên bản mới?</span>}
        okText="Lưu"
        cancelText="Hủy"
        width={640}
        centered
        confirmLoading={saveVersion.isPending}
        onCancel={() => setConfirm(null)}
        onOk={() => saveVersion.mutate(undefined, { onSuccess: () => setConfirm(null) })}
      >
        <div className="space-y-3">
          <p className="text-sm text-slate-600">
            {confirm?.published
              ? 'Các thay đổi dưới đây được chốt thành phiên bản mới, học viên thấy ngay.'
              : 'Các thay đổi dưới đây được chốt thành phiên bản mới. Khóa học chưa xuất bản nên học viên chưa thấy.'}
          </p>
          <p className="rounded-lg border border-amber-200 bg-amber-50/70 px-3 py-2 text-xs text-amber-800 flex gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0" />
            Lưu cả khóa chốt nháp của quản trị viên cho mọi bài (kèm đề thi gắn khóa), không qua duyệt. Phần giáo viên
            đang soạn không bị đưa lên; bài giáo viên đã sửa sẽ cần cập nhật bản mới.
          </p>
          <div className="max-h-[50vh] overflow-y-auto">
            <CourseChangesView courseId={course.id} title="Thay đổi sẽ được chốt" />
          </div>
        </div>
      </Modal>
    </>
  )
}
