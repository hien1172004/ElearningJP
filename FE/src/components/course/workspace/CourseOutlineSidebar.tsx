import React, { useEffect, useRef } from 'react'
import { ClipboardList, GraduationCap, LayoutDashboard, Send, Settings, Users } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { WorkspaceSelection } from './workspaceSelection'

interface CourseOutlineSidebarProps {
  lessonCount: number
  selection: WorkspaceSelection
  onSelect: (selection: WorkspaceSelection) => void
  examCount: number
  studentCount: number
  /** Mục Cài đặt (sửa thông tin, xuất bản, xóa khóa) chỉ dành cho ADMIN. */
  showSettings: boolean
  /** Khóa đang chờ duyệt → chấm nhắc ở tab Duyệt. */
  reviewPending: boolean
}

type View = 'overview' | 'review' | 'exams' | 'students' | 'teachers' | 'settings'

/**
 * Mục lục khóa học dạng thanh tab ngang (trượt ngang khi màn hình hẹp):
 * Tổng quan · Bài học · Duyệt · Bài kiểm tra · Học viên · Giáo viên · Cài đặt.
 * Mở 1 bài học vẫn tính là đang ở "Tổng quan".
 */
export const CourseOutlineSidebar: React.FC<CourseOutlineSidebarProps> = ({
  lessonCount,
  selection,
  onSelect,
  examCount,
  studentCount,
  showSettings,
  reviewPending,
}) => {
  const activeView: View = selection.view === 'lesson' ? 'overview' : (selection.view as View)
  const activeRef = useRef<HTMLButtonElement>(null)

  // Tab đang chọn nằm ngoài vùng nhìn (màn hình hẹp) → trượt tới
  useEffect(() => {
    activeRef.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'nearest' })
  }, [activeView])

  const items: { view: View; label: string; icon: React.ReactNode; badge?: React.ReactNode }[] = [
    { view: 'overview', label: 'Tổng quan · Bài học', icon: <LayoutDashboard className="w-4 h-4" />, badge: lessonCount },
    {
      view: 'review',
      label: 'Duyệt',
      icon: <Send className="w-4 h-4" />,
      badge: reviewPending ? <span className="block w-2 h-2 rounded-full bg-amber-500" aria-label="Đang chờ duyệt" /> : undefined,
    },
    { view: 'exams', label: 'Bài kiểm tra', icon: <ClipboardList className="w-4 h-4" />, badge: examCount },
    { view: 'students', label: 'Học viên', icon: <Users className="w-4 h-4" />, badge: studentCount },
    { view: 'teachers', label: 'Giáo viên', icon: <GraduationCap className="w-4 h-4" /> },
    ...(showSettings
      ? [{ view: 'settings' as const, label: 'Cài đặt khóa học', icon: <Settings className="w-4 h-4" /> }]
      : []),
  ]

  return (
    <nav
      aria-label="Mục lục khóa học"
      className="bg-surface rounded-xl border border-suna shadow-xs px-1.5 overflow-x-auto overscroll-x-contain [scrollbar-width:none] [&::-webkit-scrollbar]:hidden"
    >
      <div role="tablist" className="flex w-max min-w-full gap-1">
        {items.map((item) => {
          const active = activeView === item.view
          return (
            <button
              key={item.view}
              ref={active ? activeRef : undefined}
              type="button"
              role="tab"
              aria-selected={active}
              onClick={() => onSelect({ view: item.view } as WorkspaceSelection)}
              className={cn(
                // Chia đều bề ngang khi đủ chỗ (flex-1); hẹp hơn nội dung thì giữ nguyên và trượt ngang (min-w-max)
                'relative flex-1 min-w-max flex items-center justify-center gap-2 whitespace-nowrap px-3 py-3 text-sm transition',
                active ? 'text-indigo-700 font-semibold' : 'text-slate-600 hover:text-slate-900',
              )}
            >
              <span className="shrink-0">{item.icon}</span>
              {item.label}
              {typeof item.badge === 'number' ? (
                <span
                  className={cn(
                    'rounded-full px-1.5 text-xs tabular-nums',
                    active ? 'bg-indigo-100 text-indigo-700' : 'bg-slate-100 text-slate-500',
                  )}
                >
                  {item.badge}
                </span>
              ) : (
                item.badge
              )}
              {/* Gạch chân tab đang chọn */}
              <span
                className={cn(
                  'absolute inset-x-2 bottom-0 h-0.5 rounded-full transition',
                  active ? 'bg-indigo-600' : 'bg-transparent',
                )}
              />
            </button>
          )
        })}
      </div>
    </nav>
  )
}
