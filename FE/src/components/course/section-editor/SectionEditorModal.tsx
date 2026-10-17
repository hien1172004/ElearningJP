import React, { useEffect, useMemo, useState } from 'react'
import { Modal } from 'antd'
import { BookOpen } from 'lucide-react'
import type { LessonSection } from '@/types'
import { SectionList } from './SectionList'
import { SectionItemsModal } from './SectionItemsModal'
import { QuizPanel } from '@/components/course/quiz-editor'
import { useLessonDetail } from '@/hooks'

interface SectionEditorModalProps {
  open: boolean
  lessonId: number | null
  onClose: () => void
}

/**
 * Modal quản lý toàn bộ nội dung 1 lesson.
 *
 * Bố cục xếp DỌC (1 cột):
 * ┌──────────────────────────────────────┐
 * │ SectionList (Kanji/Vocab/Grammar)   │
 * ├──────────────────────────────────────┤
 * │ QuizPanel (Quiz 1-1 với lesson)     │
 * └──────────────────────────────────────┘
 *
 * - Section có thể chứa items (Kanji/Vocab/Grammar) mở trong SectionItemsModal lồng.
 * - Quiz là quan hệ 1-1 với lesson, không qua section — đặt dưới cùng.
 */
export const SectionEditorModal: React.FC<SectionEditorModalProps> = ({
  open,
  lessonId,
  onClose,
}) => {
  const { data: lesson } = useLessonDetail(lessonId ?? undefined)
  /** Lưu id của section đang mở quản lý items (lưu id, không lưu object, để luôn lấy data mới) */
  const [itemsSectionId, setItemsSectionId] = useState<number | null>(null)

  useEffect(() => {
    // Reset state when modal closes
    if (!open) setItemsSectionId(null)
  }, [open])

  /**
   * Lấy section hiện tại theo id từ `lesson.sections` (data mới nhất sau mỗi refetch).
   * Nếu chưa có trong data (đang refetch) thì fallback về section object cũ.
   * → Đảm bảo items mới thêm/sửa/xoá sẽ hiển thị ngay lập tức trong editor.
   */
  const currentSection = useMemo<LessonSection | null>(() => {
    if (itemsSectionId == null) return null
    return lesson?.sections?.find((s) => s.id === itemsSectionId) ?? null
  }, [itemsSectionId, lesson?.sections])

  return (
    <>
      <Modal
        open={open}
        onCancel={onClose}
        footer={null}
        title={
          <div className="flex items-center gap-2 font-bold text-gray-900">
            <BookOpen className="w-5 h-5 text-indigo-600" />
            <span>Quản lý nội dung - {lesson?.title ?? 'Bài học'}</span>
          </div>
        }
        width={920}
        destroyOnHidden
      >
        {lessonId && (
          <div className="py-2 space-y-3">
            {/* =========== PHẦN TRÊN: Section (Kanji/Vocab/Grammar) =========== */}
            <div className="rounded-md border border-gray-200/80 bg-gray-50/30 p-3">
              <SectionList
                lessonId={lessonId}
                sections={lesson?.sections ?? []}
                onManageItems={(s) => setItemsSectionId(s.id)}
              />
            </div>

            {/* =========== PHẦN DƯỚI: Quiz (1-1 với lesson) =========== */}
            <QuizPanel
              lessonId={lessonId}
              lessonTitle={lesson?.title}
            />
          </div>
        )}
      </Modal>

      <SectionItemsModal
        open={itemsSectionId !== null}
        lessonId={lessonId ?? 0}
        section={currentSection}
        onClose={() => setItemsSectionId(null)}
      />
    </>
  )
}
