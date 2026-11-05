import React from 'react'
import { Modal } from 'antd'
import { BookOpen } from 'lucide-react'
import type { LessonSection } from '@/types'
import { SectionItemsEditor } from './SectionItemsEditor'
import { SECTION_TYPE_LABEL } from './sectionMeta'

interface SectionItemsModalProps {
  open: boolean
  lessonId: number
  section: LessonSection | null
  onClose: () => void
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
  if (!section || !section.id || !section.sectionType) {
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

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          <BookOpen className="w-5 h-5 text-indigo-600" />
          <span>
            Quản lý {SECTION_TYPE_LABEL[section.sectionType] ?? section.sectionType} —{' '}
            <span className="text-indigo-600">{section.title}</span>
          </span>
        </div>
      }
      width={960}
      destroyOnHidden
    >
      <div className="py-2">
        <SectionItemsEditor lessonId={lessonId} section={section} />
      </div>
    </Modal>
  )
}
