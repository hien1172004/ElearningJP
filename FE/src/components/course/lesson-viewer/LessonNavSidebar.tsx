import React from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, Circle, PlayCircle, ChevronLeft, ChevronRight } from 'lucide-react'
import { toSlug } from '@/utils'
import type { LessonOutline, ProgressStatus } from '@/types'
import { CheckCircleFilled, PlayCircleFilled } from '@ant-design/icons'

interface LessonNavSidebarProps {
  courseId: number
  courseSlug: string
  lessons: LessonOutline[]
  currentLessonId?: number
  isEnrolled?: boolean
}

function statusIcon(status?: ProgressStatus | null) {
  switch (status) {
    case 'COMPLETED': return <CheckCircleFilled className="w-4 h-4 text-emerald-500 shrink-0" />
    case 'IN_PROGRESS': return <PlayCircleFilled className="w-4 h-4 text-amber-500 shrink-0" />
    default: return <Circle className="w-4 h-4 text-gray-400 shrink-0" />
  }
}

export const LessonNavSidebar: React.FC<LessonNavSidebarProps> = ({
  courseId,
  courseSlug,
  lessons,
  currentLessonId,
  isEnrolled,
}) => {
  const navigate = useNavigate()
  const goTo = (lessonId: number, lessonTitle: string) => {
    const lessonSlug = toSlug(lessonTitle)
    navigate(
      `/learn/${courseSlug}/${lessonSlug}?courseId=${courseId}&lessonId=${lessonId}`,
    )
  }
  const currentIdx = lessons.findIndex((l) => l.id === currentLessonId)
  const prev = currentIdx > 0 ? lessons[currentIdx - 1] : null
  const next = currentIdx < lessons.length - 1 ? lessons[currentIdx + 1] : null

  return (
    <aside className="sticky top-16 w-full bg-white rounded-md border border-gray-200/80 shadow-xs overflow-hidden">
      <div className="p-4 border-b border-gray-100">
        <h3 className="font-bold text-sm text-gray-800">Nội dung bài học</h3>
        <p className="text-xs text-gray-500 mt-0.5">{lessons.length} bài học</p>
      </div>
      <nav className="max-h-[55vh] overflow-y-auto p-2 space-y-1">
        {lessons.map((lesson, idx) => {
          const isActive = lesson.id === currentLessonId
          return (
            <button
              key={lesson.id}
              type="button"
              onClick={() => goTo(lesson.id, lesson.title)}
              disabled={!isEnrolled}
              className={`w-full flex items-center gap-2.5 px-3 py-2 rounded-md text-left transition text-xs ${
                isActive
                  ? 'bg-indigo-50 text-indigo-700 font-semibold ring-1 ring-indigo-200'
                  : 'hover:bg-gray-50 text-gray-700'
              } ${!isEnrolled ? 'opacity-60 cursor-not-allowed' : ''}`}
            >
              {statusIcon(lesson.status)}
              <span className="flex-1 truncate">{lesson.title}</span>
            </button>
          )
        })}
      </nav>
      {isEnrolled && (
        <div className="p-3 border-t border-gray-100 flex items-center justify-between gap-2">
          <button
            type="button"
            onClick={() => prev && goTo(prev.id, prev.title)}
            disabled={!prev}
            className="flex items-center gap-1 px-3 py-1.5 rounded bg-gray-100 hover:bg-indigo-50 text-xs font-semibold disabled:opacity-30 disabled:hover:bg-gray-100 transition"
          >
            <ChevronLeft className="w-4 h-4" />
            <span className="hidden sm:inline">Bài trước</span>
          </button>
          <button
            type="button"
            onClick={() => next && goTo(next.id, next.title)}
            disabled={!next}
            className="flex items-center gap-1 px-3 py-1.5 rounded bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold disabled:opacity-30 disabled:bg-indigo-600 transition"
          >
            <span className="hidden sm:inline">Bài tiếp</span>
            <ChevronRight className="w-4 h-4" />
          </button>
        </div>
      )}
    </aside>
  )
}
