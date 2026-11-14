import React from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, Circle, PlayCircle, ChevronRight, Mic } from 'lucide-react'
import { ProgressCircle } from '@/components/common'
import { toSlug } from '@/utils'
import { LESSON_FROM_COURSE_STATE } from '@/utils/lessonNav'
import type { LessonOutline, ProgressStatus } from '@/types'

interface LessonOutlineListProps {
  lessons: LessonOutline[]
  courseId: number
  courseSlug: string
  isEnrolled?: boolean | null
  /** Hiện vòng tròn % thay cho icon trạng thái (chỉ học viên đã đăng ký). */
  showProgress?: boolean
  onSelect?: (lesson: LessonOutline) => void
}

function statusIcon(status?: ProgressStatus | null) {
  switch (status) {
    case 'COMPLETED':
      return <CheckCircle2 className="w-5 h-5 text-emerald-500" />
    case 'IN_PROGRESS':
      return <PlayCircle className="w-5 h-5 text-amber-500" />
    case 'NOT_STARTED':
    default:
      return <Circle className="w-5 h-5 text-gray-400" />
  }
}

function statusLabel(status?: ProgressStatus | null) {
  switch (status) {
    case 'COMPLETED':
      return 'Đã hoàn thành'
    case 'IN_PROGRESS':
      return 'Đang học'
    default:
      return 'Chưa học'
  }
}

export const LessonOutlineList: React.FC<LessonOutlineListProps> = ({
  lessons,
  courseId,
  courseSlug,
  isEnrolled,
  showProgress,
  onSelect,
}) => {
  const navigate = useNavigate()
  if (!lessons || lessons.length === 0) {
    return (
      <div className="text-center py-10 text-sm text-gray-500">
        Khoá học này chưa có bài học nào.
      </div>
    )
  }

  return (
    <ol className="space-y-2">
      {lessons.map((lesson, idx) => {
        const href = `/learn/${courseSlug}/${toSlug(lesson.title)}?courseId=${courseId}&lessonId=${lesson.id}`
        return (
          <li key={lesson.id}>
            <div
              onClick={() => {
                if (onSelect) onSelect(lesson)
                else if (isEnrolled) navigate(href, { state: LESSON_FROM_COURSE_STATE })
              }}
              className={`flex items-center gap-3 p-3.5 rounded-md border border-gray-200/80 hover:border-indigo-300 hover:bg-indigo-50/30 transition cursor-pointer ${
                isEnrolled ? '' : 'opacity-80'
              }`}
            >
              <div className="shrink-0 w-9 h-9 rounded-md bg-gray-50 text-gray-500 font-bold text-sm flex items-center justify-center border border-gray-200/80">
                {String(idx + 1).padStart(2, '0')}
              </div>
              {showProgress ? <ProgressCircle percent={lesson.progressPercent ?? 0} size={40} /> : statusIcon(lesson.status)}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h4 className="font-semibold text-sm text-gray-900 truncate">
                    {lesson.title}
                  </h4>
                  {lesson.hasSpeaking && (
                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 text-[10px] font-bold border border-purple-200">
                      <Mic className="w-3 h-3" /> Hội thoại AI
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-500 mt-0.5">
                  {statusLabel(lesson.status)}
                  {lesson.bestScore != null && (
                    <>
                      {' • '}
                      <span className="font-semibold text-indigo-600">
                        Điểm cao nhất: {lesson.bestScore}
                      </span>
                    </>
                  )}
                </p>
              </div>
              <ChevronRight className="w-4 h-4 text-gray-400 shrink-0" />
            </div>
          </li>
        )
      })}
    </ol>
  )
}
