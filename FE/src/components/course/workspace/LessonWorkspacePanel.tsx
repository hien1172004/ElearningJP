import React from 'react'
import { Button as AntdButton } from 'antd'
import { ChevronRight, FileCheck, Pencil } from 'lucide-react'
import { EmptyState, LoadingState } from '@/components/common'
import {
  SECTION_TYPE_LABEL,
  SectionItemsEditor,
  SectionList,
} from '@/components/course/section-editor'
import { QuizPanel } from '@/components/course/quiz-editor'
import type { LessonDetail } from '@/types'
import type { WorkspaceSelection } from './workspaceSelection'

interface LessonWorkspacePanelProps {
  lessonId: number
  /** Số thứ tự bài (1-based) để hiển thị "Bài N" */
  lessonNumber: number
  section?: number | 'quiz'
  lesson?: LessonDetail | null
  isLoading?: boolean
  onSelect: (selection: WorkspaceSelection) => void
  onEditLesson: () => void
}

const CARD = 'bg-white rounded-xl border border-suna shadow-xs'

/** Cột phải của không gian soạn khóa học khi đang mở một bài học / mục / quiz. */
export const LessonWorkspacePanel: React.FC<LessonWorkspacePanelProps> = ({
  lessonId,
  lessonNumber,
  section,
  lesson,
  isLoading,
  onSelect,
  onEditLesson,
}) => {
  if (isLoading && !lesson) return <LoadingState message="Đang tải bài học..." />
  if (!lesson) {
    return <EmptyState card title="Không tìm thấy bài học" description="Bài học có thể đã bị xóa." />
  }

  const sections = lesson.sections ?? []
  const currentSection = typeof section === 'number' ? sections.find((s) => s.id === section) : undefined
  const crumb =
    section === 'quiz' ? 'Quiz' : currentSection ? currentSection.title : undefined

  return (
    <div className="space-y-4">
      {/* Breadcrumb + thao tác bài học */}
      <div className={`${CARD} px-4 py-3 flex flex-wrap items-center justify-between gap-3`}>
        <div className="flex items-center gap-1.5 min-w-0 text-sm">
          <button
            type="button"
            onClick={() => onSelect({ view: 'lesson', lessonId })}
            className={`truncate font-semibold ${crumb ? 'text-slate-500 hover:text-indigo-600' : 'text-sumi'}`}
          >
            Bài {lessonNumber}: {lesson.title}
          </button>
          {crumb && (
            <>
              <ChevronRight className="w-4 h-4 shrink-0 text-slate-300" />
              <span className="truncate font-semibold text-sumi">{crumb}</span>
            </>
          )}
        </div>
        <AntdButton size="small" icon={<Pencil className="w-3.5 h-3.5" />} onClick={onEditLesson}>
          Sửa bài học
        </AntdButton>
      </div>

      {section === 'quiz' ? (
        <QuizPanel lessonId={lessonId} lessonTitle={lesson.title} inline />
      ) : typeof section === 'number' ? (
        currentSection ? (
          <div className={`${CARD} p-4 space-y-3`}>
            <div className="flex items-center gap-2">
              <span className="text-xs font-bold uppercase tracking-wider text-indigo-600">
                {SECTION_TYPE_LABEL[currentSection.sectionType] ?? currentSection.sectionType}
              </span>
              <h2 className="text-base font-bold text-sumi">{currentSection.title}</h2>
            </div>
            <SectionItemsEditor lessonId={lessonId} section={currentSection} />
          </div>
        ) : (
          <EmptyState
            card
            title="Không tìm thấy mục này"
            description="Mục có thể đã bị xóa khỏi bài học."
            action={<AntdButton onClick={() => onSelect({ view: 'lesson', lessonId })}>Về bài học</AntdButton>}
          />
        )
      ) : (
        // Tổng quan bài: danh sách mục (thêm / sửa / xóa) + lối vào quiz
        <>
          <div className={`${CARD} p-4`}>
            <SectionList
              lessonId={lessonId}
              sections={sections}
              onManageItems={(s) => onSelect({ view: 'lesson', lessonId, section: s.id })}
            />
          </div>
          <button
            type="button"
            onClick={() => onSelect({ view: 'lesson', lessonId, section: 'quiz' })}
            className={`${CARD} w-full p-4 flex items-center gap-3 text-left hover:border-indigo-300 transition`}
          >
            <FileCheck className="w-5 h-5 text-indigo-600 shrink-0" />
            <div className="flex-1">
              <p className="font-semibold text-sm text-sumi">Câu hỏi cuối bài</p>
              <p className="text-xs text-slate-500">
                {lesson.hasQuiz ? 'Đã có câu hỏi — bấm để chỉnh sửa.' : 'Chưa có câu hỏi — bấm để tạo.'}
              </p>
            </div>
            <ChevronRight className="w-4 h-4 text-slate-400" />
          </button>
        </>
      )}
    </div>
  )
}
