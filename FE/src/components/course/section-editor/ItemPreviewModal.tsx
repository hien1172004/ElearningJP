import React from 'react'
import { Modal } from 'antd'
import { MonitorPlay } from 'lucide-react'
import type { LessonGrammarItem, LessonKanjiItem, LessonVocabularyItem } from '@/types'
import { GrammarStudyView, KanjiStudyView, VocabStudyView } from '../lesson-viewer'

type PreviewProps =
  | { type: 'KANJI'; items: LessonKanjiItem[] }
  | { type: 'VOCABULARY'; items: LessonVocabularyItem[] }
  | { type: 'GRAMMAR'; items: LessonGrammarItem[] }

/**
 * Popup xem trước item đúng như học viên thấy trong bài học (dùng lại các *StudyView).
 * Mở tại item được chọn, có thể chuyển trước/sau trong cùng danh sách.
 */
export const ItemPreviewModal: React.FC<
  PreviewProps & { itemId: number | null; onClose: () => void }
> = ({ itemId, onClose, ...rest }) => {
  const props = rest as PreviewProps
  const initialItemId = itemId ?? undefined

  return (
    <Modal
      open={itemId != null}
      onCancel={onClose}
      footer={null}
      width={960}
      centered
      destroyOnHidden
      title={
        <span className="flex items-center gap-2">
          <MonitorPlay className="w-5 h-5 text-indigo-600" />
          Xem trước trong bài học
        </span>
      }
    >
      <div className="rounded-md border border-gray-200/80 bg-surface p-4 md:p-6 max-h-[75vh] overflow-y-auto">
        {props.type === 'KANJI' && <KanjiStudyView items={props.items} initialItemId={initialItemId} />}
        {props.type === 'VOCABULARY' && <VocabStudyView items={props.items} initialItemId={initialItemId} />}
        {props.type === 'GRAMMAR' && <GrammarStudyView items={props.items} initialItemId={initialItemId} />}
      </div>
    </Modal>
  )
}
