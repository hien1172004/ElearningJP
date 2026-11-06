import React from 'react'
import {
  ChevronDown,
  ChevronUp,
  ClipboardList,
  LayoutDashboard,
  Pencil,
  Plus,
  Settings,
  Trash2,
  Users,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import type { LessonOutline } from '@/types'
import type { WorkspaceSelection } from './workspaceSelection'

interface CourseOutlineSidebarProps {
  lessons: LessonOutline[]
  selection: WorkspaceSelection
  onSelect: (selection: WorkspaceSelection) => void
  examCount: number
  studentCount: number
  onAddLesson: () => void
  onEditLesson: (lesson: LessonOutline) => void
  onDeleteLesson: (lesson: LessonOutline) => void
  onMoveLesson: (lessonId: number, direction: 'up' | 'down') => void
  reorderPending?: boolean
}

const navItemClass = (active: boolean) =>
  cn(
    'w-full flex items-center gap-2 rounded-lg px-2.5 py-2 text-sm text-left transition',
    active ? 'bg-indigo-50 text-indigo-700 font-semibold' : 'text-slate-700 hover:bg-slate-100',
  )

/** Mục lục khóa học dạng cây: Tổng quan · Bài học (mở trang soạn bài) · Bài kiểm tra · Học viên · Cài đặt. */
export const CourseOutlineSidebar: React.FC<CourseOutlineSidebarProps> = ({
  lessons,
  selection,
  onSelect,
  examCount,
  studentCount,
  onAddLesson,
  onEditLesson,
  onDeleteLesson,
  onMoveLesson,
  reorderPending,
}) => {
  const openLessonId = selection.view === 'lesson' ? selection.lessonId : null

  return (
    <nav className="bg-surface rounded-xl border border-suna shadow-xs p-2 space-y-1" aria-label="Mục lục khóa học">
      <button type="button" className={navItemClass(selection.view === 'overview')} onClick={() => onSelect({ view: 'overview' })}>
        <LayoutDashboard className="w-4 h-4 shrink-0" />
        Tổng quan
      </button>

      {/* ===== Bài học ===== */}
      <div className="pt-2">
        <div className="flex items-center justify-between px-2.5 pb-1">
          <span className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
            Bài học ({lessons.length})
          </span>
          <button
            type="button"
            onClick={onAddLesson}
            className="inline-flex items-center gap-1 rounded-md px-1.5 py-0.5 text-xs font-semibold text-indigo-600 hover:bg-indigo-50"
          >
            <Plus className="w-3.5 h-3.5" /> Bài
          </button>
        </div>

        {lessons.length === 0 && (
          <p className="px-2.5 py-3 text-xs text-slate-400">Chưa có bài học. Bấm “+ Bài” để tạo.</p>
        )}

        <ol className="space-y-0.5">
          {lessons.map((lesson, idx) => {
            const isOpen = lesson.id === openLessonId
            return (
              <li key={lesson.id}>
                <div className="group relative">
                  <button
                    type="button"
                    className={cn(navItemClass(isOpen), 'pr-24')}
                    onClick={() => onSelect({ view: 'lesson', lessonId: lesson.id })}
                    title={lesson.title}
                  >
                    <span className="shrink-0 text-xs font-bold text-slate-400 tabular-nums">
                      {String(idx + 1).padStart(2, '0')}
                    </span>
                    <span className="truncate">{lesson.title}</span>
                  </button>
                  {/* Thao tác nhanh — hiện khi rê chuột */}
                  <div className="absolute right-1 top-1/2 -translate-y-1/2 hidden items-center gap-0.5 group-hover:flex group-focus-within:flex">
                    <IconAction label="Lên trên" disabled={idx === 0 || reorderPending} onClick={() => onMoveLesson(lesson.id, 'up')}>
                      <ChevronUp className="w-3.5 h-3.5" />
                    </IconAction>
                    <IconAction
                      label="Xuống dưới"
                      disabled={idx === lessons.length - 1 || reorderPending}
                      onClick={() => onMoveLesson(lesson.id, 'down')}
                    >
                      <ChevronDown className="w-3.5 h-3.5" />
                    </IconAction>
                    <IconAction label="Sửa bài học" onClick={() => onEditLesson(lesson)}>
                      <Pencil className="w-3.5 h-3.5" />
                    </IconAction>
                    <IconAction label="Xóa bài học" danger onClick={() => onDeleteLesson(lesson)}>
                      <Trash2 className="w-3.5 h-3.5" />
                    </IconAction>
                  </div>
                </div>

              </li>
            )
          })}
        </ol>
      </div>

      <div className="border-t border-suna pt-1 mt-2 space-y-1">
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

const IconAction: React.FC<{
  label: string
  onClick: () => void
  disabled?: boolean
  danger?: boolean
  children: React.ReactNode
}> = ({ label, onClick, disabled, danger, children }) => (
  <button
    type="button"
    title={label}
    aria-label={label}
    disabled={disabled}
    onClick={onClick}
    className={cn(
      'rounded p-1 text-slate-400 transition disabled:opacity-30 disabled:cursor-not-allowed',
      danger ? 'hover:bg-rose-50 hover:text-rose-600' : 'hover:bg-surface hover:text-indigo-600',
    )}
  >
    {children}
  </button>
)
