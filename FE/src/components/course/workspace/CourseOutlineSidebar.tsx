import React from 'react'
import { ClipboardList, LayoutDashboard, Settings, Users } from 'lucide-react'
import { cn } from '@/utils/cn'
import type { WorkspaceSelection } from './workspaceSelection'

interface CourseOutlineSidebarProps {
  lessonCount: number
  selection: WorkspaceSelection
  onSelect: (selection: WorkspaceSelection) => void
  examCount: number
  studentCount: number
}

const navItemClass = (active: boolean) =>
  cn(
    'w-full flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-left transition',
    active ? 'bg-indigo-50 text-indigo-700 font-semibold' : 'text-slate-700 hover:bg-slate-100',
  )

/** Mục lục khóa học: Tổng quan (kèm danh sách bài học phân trang) · Bài kiểm tra · Học viên · Cài đặt. */
export const CourseOutlineSidebar: React.FC<CourseOutlineSidebarProps> = ({
  lessonCount,
  selection,
  onSelect,
  examCount,
  studentCount,
}) => {
  return (
    <nav className="bg-surface rounded-xl border border-suna shadow-xs p-2 space-y-1" aria-label="Mục lục khóa học">
      <button type="button" className={navItemClass(selection.view === 'overview')} onClick={() => onSelect({ view: 'overview' })}>
        <LayoutDashboard className="w-4 h-4 shrink-0" />
        <span className="flex-1">Tổng quan · Bài học</span>
        <span className="text-xs text-slate-400 tabular-nums">{lessonCount}</span>
      </button>

      <div className="space-y-1">
        <button type="button" className={navItemClass(selection.view === 'exams')} onClick={() => onSelect({ view: 'exams' })}>
          <ClipboardList className="w-4 h-4 shrink-0" />
          <span className="flex-1">Bài kiểm tra</span>
          <span className="text-xs text-slate-400 tabular-nums">{examCount}</span>
        </button>
        <button type="button" className={navItemClass(selection.view === 'students')} onClick={() => onSelect({ view: 'students' })}>
          <Users className="w-4 h-4 shrink-0" />
          <span className="flex-1">Học viên</span>
          <span className="text-xs text-slate-400 tabular-nums">{studentCount}</span>
        </button>
        <button type="button" className={navItemClass(selection.view === 'settings')} onClick={() => onSelect({ view: 'settings' })}>
          <Settings className="w-4 h-4 shrink-0" />
          Cài đặt khóa học
        </button>
      </div>
    </nav>
  )
}
