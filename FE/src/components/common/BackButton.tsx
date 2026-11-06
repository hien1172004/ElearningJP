import React from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { cn } from '@/utils/cn'

interface BackButtonProps {
  /** Trang dự phòng khi không có trang trước trong app (vd: mở thẳng link, F5 ở tab mới) */
  to?: string
  /** Nhãn hiển thị. Mặc định: "Quay lại" */
  label?: string
  /**
   * true (mặc định): quay lại đúng trang trước trong lịch sử của app, nếu không có thì về `to`.
   * false: luôn về `to` (dùng khi trang trước là trang trung gian không nên quay lại, vd: phòng thi).
   */
  useHistory?: boolean
  /** Tự xử lý thay cho điều hướng (vd: thoát khỏi nhóm đang mở trong cùng trang) */
  onClick?: () => void
  className?: string
}

const BASE_CLASS =
  'inline-flex items-center gap-1.5 rounded-lg px-2 py-1 -ml-2 text-sm font-medium text-slate-600 transition hover:bg-slate-100 hover:text-indigo-600'

/** React Router lưu vị trí trong lịch sử ở history.state.idx — idx > 0 nghĩa là còn trang trước trong app */
const hasAppHistory = (): boolean => {
  const state = window.history.state as { idx?: number } | null
  return typeof state?.idx === 'number' && state.idx > 0
}

/** Nút quay lại thống nhất cho mọi trang */
export const BackButton: React.FC<BackButtonProps> = ({
  to = '/',
  label = 'Quay lại',
  useHistory = true,
  onClick,
  className,
}) => {
  const navigate = useNavigate()
  const content = (
    <>
      <ArrowLeft className="h-4 w-4" />
      <span>{label}</span>
    </>
  )

  if (onClick) {
    return (
      <button type="button" onClick={onClick} className={cn(BASE_CLASS, className)}>
        {content}
      </button>
    )
  }

  return (
    <Link
      to={to}
      onClick={(e) => {
        // Giữ Ctrl/Cmd + click mở tab mới theo `to`; click thường thì quay lại lịch sử nếu có
        if (!useHistory || e.ctrlKey || e.metaKey || e.shiftKey || !hasAppHistory()) return
        e.preventDefault()
        navigate(-1)
      }}
      className={cn(BASE_CLASS, className)}
    >
      {content}
    </Link>
  )
}
