import React from 'react'
import type { ColumnsType } from 'antd/es/table'
import { kanjiImportService } from '@/services'
import type { ImportRowResult } from '@/types'
import { ImportStepsLayout } from './ImportStepsLayout'
import { countColumn, dictColumn, linkedRowColumn, overrideColumn, textColumn } from './importColumns'

interface Props {
  open: boolean
  onClose: () => void
  lessonId: number
  sectionId: number
}

const ITEM_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('kanji', 'Chữ Hán', 80, true),
  dictColumn('dictHanViet', 'Hán Việt', 110),
  dictColumn('dictMeaning', 'Nghĩa', 200),
  dictColumn('dictOnyomi', 'On', 120),
  dictColumn('dictKunyomi', 'Kun', 140),
  textColumn('note', 'Ghi chú', 180),
  countColumn('relatedCount', 'Từ liên quan', 'tự động'),
]

const RELATED_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('kanji', 'Chữ Hán', 80, true),
  linkedRowColumn('Kanji'),
  textColumn('word', 'Từ vựng', 120, true),
  overrideColumn('hiragana', 'dictHiragana', 'Cách đọc', 130),
  dictColumn('dictMeaning', 'Nghĩa', 220),
  textColumn('note', 'Ghi chú', 160),
]

const RULES: React.ReactNode[] = [
  <>
    Sheet <b>Kanji</b>: mỗi dòng 1 chữ Hán có sẵn trong từ điển. Âm On/Kun, nghĩa, số nét luôn lấy từ từ điển.
  </>,
  <>
    Sheet <b>TuLienQuan</b> (tuỳ chọn): mỗi dòng 1 cặp chữ Hán – từ vựng có trong từ điển. Chữ không khai báo sẽ
    hiển thị từ liên quan tự động.
  </>,
  <>Chữ đã có trong section hoặc không có trong từ điển sẽ bị báo lỗi. Tối đa 500 chữ mỗi file.</>,
]

/** Import chữ Hán vào section KANJI theo mẫu Excel riêng (Kanji + TuLienQuan). */
export const KanjiImportModal: React.FC<Props> = (props) => (
  <ImportStepsLayout
    {...props}
    api={kanjiImportService}
    title="Import chữ Hán từ Excel"
    rules={RULES}
    itemSheet={{ name: 'Kanji', label: 'Chữ Hán', columns: ITEM_COLUMNS }}
    childSheet={{ name: 'TuLienQuan', label: 'Từ liên quan', columns: RELATED_COLUMNS }}
    itemUnit="chữ Hán"
    childUnit="từ liên quan"
  />
)
