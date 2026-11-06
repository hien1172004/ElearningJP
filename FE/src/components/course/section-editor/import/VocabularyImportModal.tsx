import React from 'react'
import { Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { vocabularyImportService } from '@/services'
import type { ImportRowResult } from '@/types'
import { ImportStepsLayout } from './ImportStepsLayout'
import { countColumn, linkedRowColumn, overrideColumn, textColumn } from './importColumns'

interface Props {
  open: boolean
  onClose: () => void
  lessonId: number
  sectionId: number
}

const ITEM_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('word', 'Từ vựng', 110, true),
  overrideColumn('hiragana', 'dictHiragana', 'Cách đọc', 120),
  overrideColumn('meaning_vi', 'dictMeaning', 'Nghĩa', 220),
  textColumn('romaji', 'Romaji (riêng)', 120),
  textColumn('image_url', 'Ảnh', 140),
  textColumn('note', 'Ghi chú', 160),
  countColumn('exampleCount', 'Câu ví dụ', 'lấy từ điển'),
]

const EXAMPLE_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('word', 'Từ vựng', 110, true),
  linkedRowColumn('TuVung'),
  textColumn('example_jp', 'Câu tiếng Nhật', 240, true),
  textColumn('example_vi', 'Bản dịch', 220),
  textColumn('example_romaji', 'Romaji', 160),
  {
    title: 'Câu chính',
    key: 'primary',
    width: 90,
    render: (_, row) => (row.data.primary ? <Tag color="purple">chính</Tag> : null),
  },
]

const RULES: React.ReactNode[] = [
  <>
    Sheet <b>TuVung</b>: mỗi dòng 1 từ có sẵn trong từ điển, tra theo <b>word</b> (+ <b>hiragana</b> nếu chữ có
    nhiều cách đọc).
  </>,
  <>
    Sheet <b>ViDu</b> (tuỳ chọn): câu ví dụ nối với từ theo word + hiragana. Từ không có câu ví dụ sẽ tự lấy ví dụ
    từ từ điển.
  </>,
  <>
    Cột tuỳ chỉnh để trống = lấy dữ liệu từ điển, nhập <b>-</b> = để trống (không hiển thị) trường đó trong bài.
  </>,
  <>Từ đã có trong section hoặc không có trong từ điển sẽ bị báo lỗi. Tối đa 500 từ mỗi file.</>,
]

/** Import từ vựng vào section VOCABULARY theo mẫu Excel riêng (TuVung + ViDu). */
export const VocabularyImportModal: React.FC<Props> = (props) => (
  <ImportStepsLayout
    {...props}
    api={vocabularyImportService}
    title="Import từ vựng từ Excel"
    rules={RULES}
    itemSheet={{ name: 'TuVung', label: 'Từ vựng', columns: ITEM_COLUMNS }}
    childSheet={{ name: 'ViDu', label: 'Câu ví dụ', columns: EXAMPLE_COLUMNS }}
    itemUnit="từ vựng"
    childUnit="câu ví dụ"
  />
)
