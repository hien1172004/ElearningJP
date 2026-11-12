import React from 'react'
import { Button as AntdButton, Tooltip } from 'antd'
import { Check, Pencil, UserCheck } from 'lucide-react'
import { confirmDialog } from '@/components/common/ConfirmModal'
import type { useCourseEditSession } from '@/hooks'

interface CourseEditSessionControlsProps {
  /** Kết quả {@code useCourseEditSession(courseId)} của trang (1 phiên mỗi trang). */
  session: ReturnType<typeof useCourseEditSession>
  /** Lý do không bắt đầu sửa được (giáo viên: cả khóa chờ duyệt; ADMIN: còn bài chờ duyệt) — hiện ở tooltip. */
  disabledReason?: string | null
  size?: 'small' | 'middle'
}

/**
 * Khóa sửa khóa học của phía mình (V65): "Sửa khóa học" → giữ khóa (người cùng phía chỉ xem), "Xong" → nhả;
 * người cùng phía đang sửa → "Tiếp quản" (người cũ mất quyền ghi ở lần lưu kế tiếp).
 */
export const CourseEditSessionControls: React.FC<CourseEditSessionControlsProps> = ({ session, disabledReason, size = 'small' }) => {
  const disabled = !!disabledReason
  const { mine, heldByOther, lock, start, stop, isLoading } = session

  const takeover = async () => {
    const ok = await confirmDialog({
      title: `Tiếp quản việc sửa của ${lock?.userName || 'người khác'}?`,
      content: 'Người đang sửa sẽ không lưu được phần chưa lưu của họ và cần bấm "Sửa khóa học" để sửa tiếp.',
      okText: 'Tiếp quản',
      cancelText: 'Hủy',
    })
    if (ok) start.mutate(true)
  }

  if (mine) {
    return (
      <AntdButton size={size} icon={<Check className="w-3.5 h-3.5" />} loading={stop.isPending} onClick={() => stop.mutate()}>
        Xong
      </AntdButton>
    )
  }
  if (heldByOther) {
    return (
      <Tooltip title={disabledReason}>
        <AntdButton
          size={size}
          icon={<UserCheck className="w-3.5 h-3.5" />}
          disabled={disabled}
          loading={start.isPending}
          onClick={takeover}
        >
          Tiếp quản
        </AntdButton>
      </Tooltip>
    )
  }
  return (
    <Tooltip title={disabledReason}>
      <AntdButton
        size={size}
        type="primary"
        icon={<Pencil className="w-3.5 h-3.5" />}
        disabled={disabled || isLoading}
        loading={start.isPending}
        onClick={() => start.mutate(false)}
      >
        Sửa khóa học
      </AntdButton>
    </Tooltip>
  )
}
