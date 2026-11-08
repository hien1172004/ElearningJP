import React, { useState } from 'react'
import { Button, Tooltip } from 'antd'
import { Printer } from 'lucide-react'
import type { PracticeSheetType } from '@/services'
import type { LessonSection } from '@/types'
import { PracticeSheetModal } from './PracticeSheetModal'

interface PracticeSheetButtonProps {
  lessonId: number
  lessonTitle?: string
  type: PracticeSheetType
  /** Tất cả nhóm (section) của bài — modal hiển thị mọi nhóm cùng loại để chọn. */
  sections: LessonSection[]
  /** Nhóm chọn sẵn khi mở modal. */
  initialSectionId?: number
  size?: 'small' | 'middle'
  /** Ẩn chữ, chỉ hiện icon. */
  iconOnly?: boolean
}

/** Nút "In phiếu luyện viết" + modal chọn mục cần in (mục Chữ Hán / Từ vựng). */
export const PracticeSheetButton: React.FC<PracticeSheetButtonProps> = ({
  lessonId,
  lessonTitle,
  type,
  sections,
  initialSectionId,
  size = 'small',
  iconOnly = false,
}) => {
  const [open, setOpen] = useState(false)
  const empty = !sections.some((s) =>
    s.sectionType === type && (type === 'KANJI' ? s.kanjiItems?.length : s.vocabularyItems?.length),
  )

  return (
    <>
      <Tooltip title={empty ? 'Chưa có dữ liệu để in' : 'In phiếu luyện viết (PDF ô chữ Mễ + thứ tự nét)'}>
        <Button
          size={size}
          icon={<Printer className="h-4 w-4 text-indigo-600" />}
          disabled={empty}
          onClick={() => setOpen(true)}
        >
          {!iconOnly && 'In phiếu luyện viết'}
        </Button>
      </Tooltip>
      <PracticeSheetModal
        open={open}
        onClose={() => setOpen(false)}
        lessonId={lessonId}
        lessonTitle={lessonTitle}
        type={type}
        sections={sections}
        initialSectionId={initialSectionId}
      />
    </>
  )
}
