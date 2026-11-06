import React from 'react'
import { Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { grammarImportService } from '@/services'
import type { ImportRowResult } from '@/types'
import { ImportStepsLayout } from './ImportStepsLayout'
import { countColumn, dictColumn, linkedRowColumn, overrideColumn, textColumn } from './importColumns'

interface Props {
  open: boolean
  onClose: () => void
  lessonId: number
  sectionId: number
}

const levelColumn = {
  title: 'Cấp',
  key: 'jlpt_level',
  width: 70,
  render: (_: unknown, row: ImportRowResult) =>
    row.data.jlpt_level ? <Tag color="geekblue">{String(row.data.jlpt_level).toUpperCase()}</Tag> : null,
}

const ITEM_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('title', 'Ngữ pháp', 150, true),
  levelColumn,
  dictColumn('dictTitle', 'Khớp từ điển', 150),
  overrideColumn('custom_meaning', 'dictMeaning', 'Nghĩa', 220),
  overrideColumn('custom_structure', 'dictStructure', 'Cấu trúc', 200),
  textColumn('note', 'Ghi chú', 160),
  countColumn('exampleCount', 'Câu ví dụ', 'lấy từ điển'),
]

const EXAMPLE_COLUMNS: ColumnsType<ImportRowResult> = [
  textColumn('title', 'Ngữ pháp', 150, true),
  levelColumn,
  linkedRowColumn('NguPhap'),
  textColumn('example_jp', 'Câu tiếng Nhật', 260, true),
  textColumn('example_vi', 'Bản dịch', 240),
]

const RULES: React.ReactNode[] = [
  <>
    Sheet <b>NguPhap</b>: mỗi dòng 1 điểm ngữ pháp có sẵn trong từ điển, tra theo <b>title</b> + <b>jlpt_level</b>. Chọn tên ở dropdown trong file mẫu, hoặc gõ tay: 〜/～/~ và ngoặc ()/（） được coi là như nhau.
  </>,
  <>
    Sheet <b>ViDu</b> (tuỳ chọn): câu ví dụ nối theo title + jlpt_level. Điểm ngữ pháp không có câu ví dụ sẽ tự lấy
    từ từ điển.
  </>,
  <>
    Cột tuỳ chỉnh để trống = lấy dữ liệu từ điển, nhập <b>-</b> = để trống (không hiển thị) trường đó trong bài.
  </>,
  <>Điểm ngữ pháp đã có trong section hoặc không có trong từ điển sẽ bị báo lỗi. Tối đa 300 dòng mỗi file.</>,
]

/** Import ngữ pháp vào section GRAMMAR theo mẫu Excel riêng (NguPhap + ViDu). */
export const GrammarImportModal: React.FC<Props> = (props) => (
  <ImportStepsLayout
    {...props}
    api={grammarImportService}
    title="Import ngữ pháp từ Excel"
    rules={RULES}
    itemSheet={{ name: 'NguPhap', label: 'Ngữ pháp', columns: ITEM_COLUMNS }}
    childSheet={{ name: 'ViDu', label: 'Câu ví dụ', columns: EXAMPLE_COLUMNS }}
    itemUnit="ngữ pháp"
    childUnit="câu ví dụ"
  />
)
