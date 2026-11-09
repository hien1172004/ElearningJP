import React, { useState } from 'react'
import { Button, Tooltip } from 'antd'
import { BookmarkCheck, BookmarkPlus } from 'lucide-react'
import type { LessonSection, SaveLessonItemRef } from '@/types'
import { AddWordToNotebookModal } from '@/components/notebook/AddWordToNotebookModal'
import { useNotebookSavedIds } from '@/hooks/useNotebooks'
import { SaveLessonToNotebookModal } from './SaveLessonToNotebookModal'

interface SaveLessonToNotebookButtonProps {
  lessonId: number
  lessonTitle?: string
  sections: LessonSection[]
  initialSectionId?: number
  size?: 'small' | 'middle'
}

/** Nút "Lưu vào sổ tay" cấp bài học / nhóm — mở modal chọn mục rồi chọn sổ tay. */
export const SaveLessonToNotebookButton: React.FC<SaveLessonToNotebookButtonProps> = ({
  lessonId,
  lessonTitle,
  sections,
  initialSectionId,
  size = 'middle',
}) => {
  const [open, setOpen] = useState(false)
  const empty = !sections.some(
    (s) => (s.kanjiItems?.length ?? 0) + (s.vocabularyItems?.length ?? 0) + (s.grammarItems?.length ?? 0) > 0,
  )
  return (
    <>
      <Tooltip title={empty ? 'Bài học chưa có nội dung để lưu' : 'Lưu chữ Hán / từ vựng / ngữ pháp của bài vào sổ tay'}>
        <Button
          size={size}
          icon={<BookmarkPlus className="h-4 w-4 text-emerald-600" />}
          disabled={empty}
          onClick={() => setOpen(true)}
        >
          Lưu vào sổ tay
        </Button>
      </Tooltip>
      <SaveLessonToNotebookModal
        open={open}
        onClose={() => setOpen(false)}
        lessonId={lessonId}
        lessonTitle={lessonTitle}
        sections={sections}
        initialSectionId={initialSectionId}
      />
    </>
  )
}

interface SaveItemToNotebookButtonProps {
  lessonId: number
  /** Loại nhóm + id item trong bài */
  type: SaveLessonItemRef['type']
  itemId: number
  /** Id bản ghi gốc trong từ điển — để biết đã lưu chưa */
  sourceId?: number
  /** Tên hiển thị của mục (chữ / từ / mẫu ngữ pháp) */
  label: string
}

/** Nút lưu 1 mục của bài học vào sổ tay (trên thẻ học Chữ Hán / Từ vựng / Ngữ pháp). */
export const SaveItemToNotebookButton: React.FC<SaveItemToNotebookButtonProps> = ({
  lessonId,
  type,
  itemId,
  sourceId,
  label,
}) => {
  const [open, setOpen] = useState(false)
  const { data: savedIds } = useNotebookSavedIds()
  const savedList =
    type === 'KANJI' ? savedIds?.characterIds : type === 'GRAMMAR' ? savedIds?.grammarIds : savedIds?.vocabularyIds
  const isSaved = sourceId != null && (savedList ?? []).includes(sourceId)

  return (
    <>
      <Tooltip title={isSaved ? 'Đã có trong sổ tay của bạn — bấm để lưu thêm vào sổ khác' : 'Lưu vào sổ tay'}>
        <button
          type="button"
          onClick={() => setOpen(true)}
          className={`inline-flex items-center gap-2 px-4 py-2 rounded-md border text-sm font-semibold transition cursor-pointer ${
            isSaved
              ? 'border-emerald-300 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
              : 'border-gray-200 bg-surface text-gray-700 hover:border-emerald-300 hover:text-emerald-700'
          }`}
        >
          {isSaved ? <BookmarkCheck className="w-4 h-4" /> : <BookmarkPlus className="w-4 h-4" />}
          {isSaved ? 'Đã lưu sổ tay' : 'Lưu sổ tay'}
        </button>
      </Tooltip>
      <AddWordToNotebookModal
        open={open}
        wordData={null}
        lessonSave={open ? { lessonId, items: [{ type, itemId }], label: `"${label}"` } : null}
        onClose={() => setOpen(false)}
      />
    </>
  )
}
