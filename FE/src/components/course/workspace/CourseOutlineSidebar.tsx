import React from 'react'
import {
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  ChevronUp,
  ClipboardList,
  FileCheck,
  LayoutDashboard,
  Loader2,
  Pencil,
  Plus,
  Settings,
  Trash2,
  Users,
} from 'lucide-react'
import { cn } from '@/utils/cn'
import type { LessonDetail, LessonOutline, LessonSectionType } from '@/types'
import { countSectionItems } from '@/components/course/section-editor'
import type { WorkspaceSelection } from './workspaceSelection'

const TYPE_BADGE: Record<LessonSectionType, { label: string; className: string }> = {
  KANJI: { label: 'Kanji', className: 'bg-amber-50 text-amber-700 border-amber-200' },
  VOCABULARY: { label: 'Từ vựng', className: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  GRAMMAR: { label: 'Ngữ pháp', className: 'bg-purple-50 text-purple-700 border-purple-200' },
}

interface CourseOutlineSidebarProps {
  lessons: LessonOutline[]
  selection: WorkspaceSelection
  onSelect: (selection: WorkspaceSelection) => void
  /** Chi tiết bài đang mở (để hiện các mục bên trong); null/undefined khi chưa tải xong */
  openLesson?: LessonDetail | null
  openLessonLoading?: boolean
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

/** Mục lục khóa học dạng cây: Tổng quan · Bài học (→ mục + quiz) · Bài kiểm tra · Học viên · Cài đặt. */
export const CourseOutlineSidebar: React.FC<CourseOutlineSidebarProps> = ({
  lessons,
  selection,
  onSelect,
  openLesson,
  openLessonLoading,
  examCount,
  studentCount,
  onAddLesson,
  onEditLesson,
  onDeleteLesson,
  onMoveLesson,
  reorderPending,
}) => {
  const openLessonId = selection.view === 'lesson' ? selection.lessonId : null
  const openSection = selection.view === 'lesson' ? selection.section : undefined
  const sections = [...(openLesson?.sections ?? [])].sort((a, b) => a.orderIndex - b.orderIndex)

  return (
    <nav className="bg-white rounded-xl border border-suna shadow-xs p-2 space-y-1" aria-label="Mục lục khóa học">
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
                    className={cn(navItemClass(isOpen && openSection == null), 'pr-24')}
                    onClick={() => onSelect({ view: 'lesson', lessonId: lesson.id })}
                    title={lesson.title}
                  >
                    {isOpen ? (
                      <ChevronDown className="w-4 h-4 shrink-0 text-slate-400" />
                    ) : (
                      <ChevronRight className="w-4 h-4 shrink-0 text-slate-400" />
                    )}
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

                {/* Các mục của bài đang mở */}
                {isOpen && (
                  <div className="ml-6 mt-0.5 mb-1 border-l border-suna pl-2 space-y-0.5">
                    {openLessonLoading && !openLesson ? (
                      <div className="flex items-center gap-2 px-2 py-1.5 text-xs text-slate-400">
                        <Loader2 className="w-3.5 h-3.5 animate-spin" /> Đang tải…
                      </div>
                    ) : (
                      <>
                        {sections.map((s) => {
                          const count = countSectionItems(s)
                          const badge = TYPE_BADGE[s.sectionType]
                          return (
                            <button
                              key={s.id}
                              type="button"
                              className={cn(navItemClass(openSection === s.id), 'py-1.5')}
                              onClick={() => onSelect({ view: 'lesson', lessonId: lesson.id, section: s.id })}
                              title={s.title}
                            >
                              <span className={cn('shrink-0 rounded border px-1.5 text-[10px] font-bold', badge?.className)}>
                                {badge?.label ?? s.sectionType}
                              </span>
                              <span className="truncate flex-1">{s.title}</span>
                              {count === 0 ? (
                                <AlertTriangle className="w-3.5 h-3.5 shrink-0 text-amber-500" aria-label="Chưa có nội dung" />
                              ) : (
                                <span className="shrink-0 text-xs text-slate-400 tabular-nums">{count}</span>
                              )}
                            </button>
                          )
                        })}
                        <button
                          type="button"
                          className={cn(navItemClass(openSection === 'quiz'), 'py-1.5')}
                          onClick={() => onSelect({ view: 'lesson', lessonId: lesson.id, section: 'quiz' })}
                        >
                          <FileCheck className="w-3.5 h-3.5 shrink-0" />
                          <span className="flex-1">Quiz</span>
                          {openLesson && !openLesson.hasQuiz && (
                            <AlertTriangle className="w-3.5 h-3.5 shrink-0 text-amber-500" aria-label="Chưa có quiz" />
                          )}
                        </button>
                      </>
                    )}
                  </div>
                )}
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
      danger ? 'hover:bg-rose-50 hover:text-rose-600' : 'hover:bg-white hover:text-indigo-600',
    )}
  >
    {children}
  </button>
)
