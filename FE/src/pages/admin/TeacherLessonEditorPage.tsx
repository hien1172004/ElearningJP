import React, { useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { Button as AntdButton, Tabs } from 'antd'
import { BookOpen, ChevronRight, FileCheck, Languages, Pencil, Plus, Trash2, Type } from 'lucide-react'
import { BackButton, EmptyState, LoadingState } from '@/components/common'
import { confirmDialog } from '@/components/common/ConfirmModal'
import { LessonFormModal } from '@/components/course'
import {
  SECTION_TYPE_LABEL,
  SectionImportButton,
  SectionItemsEditor,
  countSectionItems,
} from '@/components/course/section-editor'
import { QuizPanel } from '@/components/course/quiz-editor'
import { useCourseDetail, useLessonDetail, useSectionMutations } from '@/hooks'
import type { LessonSection, LessonSectionType } from '@/types'

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'

/** Tab trên URL (?tab=) ↔ loại section. */
export type LessonEditorTab = 'vocab' | 'grammar' | 'kanji' | 'quiz'
const TAB_TO_TYPE: Record<Exclude<LessonEditorTab, 'quiz'>, LessonSectionType> = {
  vocab: 'VOCABULARY',
  grammar: 'GRAMMAR',
  kanji: 'KANJI',
}
export const SECTION_TYPE_TO_TAB: Record<LessonSectionType, LessonEditorTab> = {
  VOCABULARY: 'vocab',
  GRAMMAR: 'grammar',
  KANJI: 'kanji',
}
const isTab = (v: string | null): v is LessonEditorTab =>
  v === 'vocab' || v === 'grammar' || v === 'kanji' || v === 'quiz'

/** Đường dẫn trang soạn bài học (dùng ở trang soạn khóa học). */
export const lessonEditorPath = (courseId: number, lessonId: number, tab?: LessonEditorTab) =>
  `/teacher/courses/${courseId}/lessons/${lessonId}${tab ? `?tab=${tab}` : ''}`

// ============================================================================
// Nội dung 1 tab loại (Từ vựng / Ngữ pháp / Chữ Hán)
// ============================================================================

/**
 * Phương án A: giữ nguyên mô hình nhiều section/bài, tab gộp mọi section cùng loại.
 * - Chưa có section → nút tạo section mặc định rồi thêm item.
 * - 1 section → hiển thị thẳng danh sách item (giáo viên không cần biết khái niệm "section").
 * - Nhiều section (dữ liệu cũ / chủ động chia nhóm) → mỗi section là 1 nhóm có tiêu đề.
 */
const SectionTypeTab: React.FC<{
  lessonId: number
  type: LessonSectionType
  allSections: LessonSection[]
}> = ({ lessonId, type, allSections }) => {
  const { createSection, deleteSection } = useSectionMutations(lessonId)
  const label = SECTION_TYPE_LABEL[type]
  const sections = allSections
    .filter((s) => s.sectionType === type)
    .sort((a, b) => a.orderIndex - b.orderIndex)

  const addSection = () =>
    createSection.mutate({
      sectionType: type,
      title: sections.length === 0 ? label : `${label} (nhóm ${sections.length + 1})`,
      orderIndex: allSections.reduce((max, s) => Math.max(max, s.orderIndex), 0) + 1,
    })

  const removeSection = async (s: LessonSection) => {
    const ok = await confirmDialog({
      title: `Xóa nhóm "${s.title}"?`,
      content: `Toàn bộ ${countSectionItems(s)} mục trong nhóm sẽ bị xóa khỏi bài học.`,
      okText: 'Xóa nhóm',
      cancelText: 'Hủy',
      danger: true,
    })
    if (ok) deleteSection.mutate(s.id)
  }

  if (sections.length === 0) {
    return (
      <EmptyState
        title={`Bài học chưa có ${label.toLowerCase()}`}
        description={`Bắt đầu thêm ${label.toLowerCase()} cho bài học này (thêm từng mục hoặc import Excel).`}
        action={
          <AntdButton type="primary" icon={<Plus className="w-4 h-4" />} loading={createSection.isPending} onClick={addSection}>
            Bắt đầu thêm {label.toLowerCase()}
          </AntdButton>
        }
      />
    )
  }

  const multi = sections.length > 1
  return (
    <div className="space-y-4">
      {sections.map((s) => (
        <div key={s.id} className={multi ? 'rounded-lg border border-gray-200 p-3 space-y-3' : 'space-y-3'}>
          <div className="flex flex-wrap items-center justify-between gap-2">
            <p className="text-sm font-semibold text-sumi truncate">
              {multi ? s.title : `Danh sách ${label.toLowerCase()}`}
              <span className="ml-2 text-xs font-normal text-slate-500">{countSectionItems(s)} mục</span>
            </p>
            <div className="flex items-center gap-2">
              <SectionImportButton lessonId={lessonId} section={s} />
              {multi && (
                <AntdButton size="small" danger icon={<Trash2 className="w-3.5 h-3.5" />} onClick={() => removeSection(s)}>
                  Xóa nhóm
                </AntdButton>
              )}
            </div>
          </div>
          <SectionItemsEditor lessonId={lessonId} section={s} />
        </div>
      ))}
      <AntdButton type="dashed" block icon={<Plus className="w-4 h-4" />} loading={createSection.isPending} onClick={addSection}>
        Thêm nhóm {label.toLowerCase()}
      </AntdButton>
    </div>
  )
}

// ============================================================================
// Trang
// ============================================================================

/**
 * Trang soạn 1 bài học (giáo viên): 4 tab cố định Từ vựng · Ngữ pháp · Chữ Hán · Quiz.
 * Mỗi tab là danh sách item + CRUD; bấm 1 dòng → popup xem trước giống giao diện bài học.
 * Tab đang mở lưu trên URL (?tab=).
 */
export const TeacherLessonEditorPage: React.FC = () => {
  const { courseId: rawCourseId, lessonId: rawLessonId } = useParams<{ courseId: string; lessonId: string }>()
  const courseId = Number(rawCourseId)
  const lessonId = Number(rawLessonId)
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const rawTab = searchParams.get('tab')
  const tab: LessonEditorTab = isTab(rawTab) ? rawTab : 'vocab'
  const [editOpen, setEditOpen] = useState(false)

  const { data: course } = useCourseDetail(courseId)
  const { data: lesson, isLoading } = useLessonDetail(lessonId || undefined)
  const coursePath = `/teacher/courses/${courseId}`

  if (isLoading && !lesson) return <LoadingState message="Đang tải bài học..." />
  if (!lesson || (lesson.courseId != null && lesson.courseId !== courseId)) {
    return (
      <EmptyState
        card
        title="Không tìm thấy bài học"
        description="Bài học không thuộc khóa này hoặc đã bị xóa."
        action={<AntdButton onClick={() => navigate(coursePath)}>Về khóa học</AntdButton>}
      />
    )
  }

  const sections = lesson.sections ?? []
  const lessons = course?.lessons ?? []
  const lessonNumber = lessons.findIndex((l) => l.id === lessonId) + 1
  const countOf = (type: LessonSectionType) =>
    sections.filter((s) => s.sectionType === type).reduce((sum, s) => sum + countSectionItems(s), 0)

  const tabLabel = (icon: React.ReactNode, text: string, count?: number) => (
    <span className="flex items-center gap-1.5">
      {icon}
      {text}
      {count != null && (
        <span className="rounded-full bg-slate-100 px-1.5 text-xs font-semibold text-slate-600">{count}</span>
      )}
    </span>
  )

  const typeTab = (key: Exclude<LessonEditorTab, 'quiz'>, icon: React.ReactNode) => {
    const type = TAB_TO_TYPE[key]
    return {
      key,
      label: tabLabel(icon, SECTION_TYPE_LABEL[type], countOf(type)),
      children: <SectionTypeTab lessonId={lessonId} type={type} allSections={sections} />,
    }
  }

  return (
    <div className="max-w-6xl mx-auto px-3 sm:px-4 space-y-4 pb-12">
      <BackButton onClick={() => navigate(coursePath)} />

      {/* Header: breadcrumb khóa học → bài học + sửa bài học */}
      <div className={`${CARD} px-4 py-3 flex flex-wrap items-center justify-between gap-3`}>
        <div className="flex items-center gap-1.5 min-w-0 text-sm">
          <Link to={coursePath} className="truncate font-semibold text-slate-500 hover:text-indigo-600">
            {course?.title ?? lesson.courseTitle ?? 'Khóa học'}
          </Link>
          <ChevronRight className="w-4 h-4 shrink-0 text-slate-300" />
          <h1 className="truncate font-bold text-sumi text-base">
            {lessonNumber > 0 ? `Bài ${lessonNumber}: ` : ''}
            {lesson.title}
          </h1>
        </div>
        <AntdButton size="small" icon={<Pencil className="w-3.5 h-3.5" />} onClick={() => setEditOpen(true)}>
          Sửa bài học
        </AntdButton>
      </div>

      <div className={`${CARD} px-4 pb-4`}>
        <Tabs
          activeKey={tab}
          onChange={(key) => setSearchParams({ tab: key }, { replace: true })}
          destroyOnHidden
          items={[
            typeTab('vocab', <BookOpen className="w-4 h-4" />),
            typeTab('grammar', <Languages className="w-4 h-4" />),
            typeTab('kanji', <Type className="w-4 h-4" />),
            {
              key: 'quiz',
              label: tabLabel(<FileCheck className="w-4 h-4" />, 'Quiz'),
              children: <QuizPanel lessonId={lessonId} lessonTitle={lesson.title} inline />,
            },
          ]}
        />
      </div>

      <LessonFormModal open={editOpen} courseId={courseId} lesson={lesson} onClose={() => setEditOpen(false)} />
    </div>
  )
}
