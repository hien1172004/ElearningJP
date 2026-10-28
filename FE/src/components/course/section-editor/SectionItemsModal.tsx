import React, { useMemo } from 'react'
import { Modal, App } from 'antd'
import { BookOpen } from 'lucide-react'
import type { LessonSection } from '@/types'
import { KanjiItemsEditor } from './KanjiItemsEditor'
import { VocabItemsEditor } from './VocabItemsEditor'
import { GrammarItemsEditor } from './GrammarItemsEditor'

interface SectionItemsModalProps {
  open: boolean
  lessonId: number
  section: LessonSection | null
  onClose: () => void
}

const TYPE_LABEL: Record<string, string> = {
  KANJI: 'Chữ Hán',
  VOCABULARY: 'Từ vựng',
  GRAMMAR: 'Ngữ pháp',
}

/**
 * Modal hiển thị editor tương ứng với section.sectionType.
 * Mở editor items (Kanji / Vocab / Grammar) cho 1 section cụ thể.
 */
export const SectionItemsModal: React.FC<SectionItemsModalProps> = ({
  open,
  lessonId,
  section,
  onClose,
}) => {
  const { message } = App.useApp()

  // Cảnh báo nếu section đã xoá mềm (cũng không nên vào)
  const isInvalid = useMemo(() => {
    if (!section) return true
    return !section.id || !section.sectionType
  }, [section])

  if (!section || isInvalid) {
    return (
      <Modal
        open={open}
        onCancel={onClose}
        footer={null}
        title="Không có section"
        destroyOnHidden
      >
        <p className="text-sm text-gray-500">Section không hợp lệ.</p>
      </Modal>
    )
  }

  const editor = (() => {
    switch (section.sectionType) {
      case 'KANJI':
        return (
          <KanjiItemsEditor
            lessonId={lessonId}
            sectionId={section.id}
            items={section.kanjiItems ?? []}
          />
        )
      case 'VOCABULARY':
        return (
          <VocabItemsEditor
            lessonId={lessonId}
            sectionId={section.id}
            items={section.vocabularyItems ?? []}
          />
        )
      case 'GRAMMAR':
        return (
          <GrammarItemsEditor
            lessonId={lessonId}
            sectionId={section.id}
            items={section.grammarItems ?? []}
          />
        )
      default:
        message.error(`Loại section không hỗ trợ: ${section.sectionType}`)
        return null
    }
  })()

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <BookOpen className="w-5 h-5 text-indigo-600" />
          <span>
            Quản lý {TYPE_LABEL[section.sectionType] ?? section.sectionType} —{' '}
            <span className="text-indigo-600">{section.title}</span>
          </span>
        </div>
      }
      width={960}
      destroyOnHidden
    >
      <div className="py-2">{editor}</div>
    </Modal>
  )
}