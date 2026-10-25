import React, { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Circle, ChevronLeft, ChevronRight, Award, FileCheck } from 'lucide-react'
import { toSlug } from '@/utils'
import type { LessonOutline, ProgressStatus } from '@/types'
import type { ExamSummary } from '@/types/exam.types'
import { examService } from '@/services/exam.service'
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
  const [courseExams, setCourseExams] = useState<ExamSummary[]>([])

  useEffect(() => {
    if (!courseId) return
    examService
      .getCourseExams(courseId)
      .then((exams) => setCourseExams(exams || []))
      .catch((err) => console.error('Lỗi tải bài test của khóa học:', err))
  }, [courseId])

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
      <nav className="max-h-[50vh] overflow-y-auto p-2 space-y-1">
        {lessons.map((lesson) => {
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

      {/* Danh sách bài kiểm tra / Mini-test của khóa học */}
      {courseExams.length > 0 && (
        <div className="p-3 border-t border-gray-100 bg-slate-50/60">
          <div className="flex items-center justify-between mb-2">
            <h4 className="font-bold text-xs text-gray-700 flex items-center gap-1.5">
              <Award className="w-3.5 h-3.5 text-indigo-600" />
              Bài kiểm tra khóa học ({courseExams.length})
            </h4>
          </div>
          <div className="space-y-1.5 max-h-[25vh] overflow-y-auto">
            {courseExams.map((exam) => (
              <Link
                key={exam.id}
                to={`/exam/lobby/${exam.id}`}
                className="flex items-center justify-between p-2 rounded-lg bg-white border border-gray-200/80 hover:border-indigo-400 hover:shadow-xs transition text-xs group"
              >
                <div className="flex items-center gap-2 min-w-0">
                  <FileCheck className="w-4 h-4 text-indigo-600 shrink-0 group-hover:scale-110 transition" />
                  <div className="truncate">
                    <p className="font-semibold text-gray-800 truncate group-hover:text-indigo-600 transition">
                      {exam.title}
                    </p>
                    <p className="text-[10px] text-gray-500">
                      {exam.totalQuestions} câu • {exam.totalDurationMinutes} phút
                    </p>
                  </div>
                </div>
                <span className="shrink-0 text-[10px] font-bold px-1.5 py-0.5 rounded bg-indigo-50 text-indigo-600 border border-indigo-100 ml-1">
                  Vào thi
                </span>
              </Link>
            ))}
          </div>
        </div>
      )}

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
