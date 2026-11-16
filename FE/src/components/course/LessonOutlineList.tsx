import React from 'react'
import { useNavigate } from 'react-router-dom'
import { CheckCircle2, Circle, PlayCircle, ChevronRight, Mic, Lock } from 'lucide-react'
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
  isLocked?: boolean
  onLockedClick?: () => void
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
  isLocked,
  onLockedClick,
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
                if (isLocked && onLockedClick) {
                  onLockedClick()
                } else if (onSelect) {
                  onSelect(lesson)
                } else if (isEnrolled) {
                  navigate(href, { state: LESSON_FROM_COURSE_STATE })
                }
              }}
              className={`flex items-center gap-3 p-3.5 rounded-xl border transition cursor-pointer ${
                isLocked
                  ? 'border-suna/70 bg-surface hover:border-amber-400 hover:bg-amber-50/20 opacity-90'
                  : isEnrolled
                  ? 'border-suna/90 bg-surface hover:border-ai/50 hover:bg-kamenozoki/20'
                  : 'border-suna/60 bg-surface opacity-80'
              }`}
            >
              <div className="shrink-0 w-9 h-9 rounded-xl bg-kinari text-nibi font-bold text-sm flex items-center justify-center border border-suna">
                {String(idx + 1).padStart(2, '0')}
              </div>
              {isLocked ? (
                <div className="w-8 h-8 rounded-lg bg-amber-50 dark:bg-amber-950/50 flex items-center justify-center text-amber-500 shrink-0">
                  <Lock className="w-4 h-4" />
                </div>
              ) : showProgress ? (
                <ProgressCircle percent={lesson.progressPercent ?? 0} size={40} />
              ) : (
                statusIcon(lesson.status)
              )}
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 flex-wrap">
                  <h4 className="font-semibold text-sm text-sumi truncate">
                    {lesson.title}
                  </h4>
                  {lesson.hasSpeaking && (
                    <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-purple-50 text-purple-700 text-[10px] font-bold border border-purple-200">
                      <Mic className="w-3 h-3" /> Hội thoại AI
                    </span>
                  )}
                </div>
                <p className="text-xs text-nibi mt-0.5">
                  {statusLabel(lesson.status)}
                  {lesson.bestScore != null && (
                    <>
                      {' • '}
                      <span className="font-semibold text-ai">
                        Điểm cao nhất: {lesson.bestScore}
                      </span>
                    </>
                  )}
                </p>
              </div>
              {isLocked ? (
                <Lock className="w-4 h-4 text-amber-500 shrink-0" />
              ) : (
                <ChevronRight className="w-4 h-4 text-nibi shrink-0" />
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}
