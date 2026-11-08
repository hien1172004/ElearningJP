import type { ColumnType } from 'antd/es/table'
import { Tag } from 'antd'
import type { ImportRowResult } from '@/types'

/** Ký hiệu "xoá giá trị" trong cột tuỳ chỉnh của mẫu Excel (khớp BE). */
const CLEAR_MARKER = '-'

const str = (v: unknown) => (v === null || v === undefined || v === '' ? null : String(v))

const EMPTY = <span className="text-gray-300">—</span>

/** Cột hiển thị nguyên giá trị đã đọc từ file. */
export const textColumn = (key: string, title: string, width = 140, jp = false): ColumnType<ImportRowResult> => ({
  title,
  key,
  width,
  render: (_, row) => {
    const v = str(row.data[key])
    return v ? <span className={jp ? 'text-base' : ''}>{v}</span> : EMPTY
  },
})

/**
 * Cột tuỳ chỉnh có giá trị từ điển dự phòng:
 * ô trống → hiện giá trị từ điển (chữ xám), '-' → để trống (không hiển thị trong bài), còn lại → giá trị giáo viên nhập.
 */
export const overrideColumn = (
  key: string,
  dictKey: string,
  title: string,
  width = 200,
): ColumnType<ImportRowResult> => ({
  title,
  key,
  width,
  render: (_, row) => {
    const v = str(row.data[key])
    if (v === CLEAR_MARKER) return <Tag>không hiển thị</Tag>
    if (v) return <span className="text-gray-900">{v}</span>
    const dict = str(row.data[dictKey])
    return dict ? (
      <span className="text-gray-400" title="Lấy từ từ điển">
        {dict} <span className="text-[10px] uppercase">(từ điển)</span>
      </span>
    ) : (
      EMPTY
    )
  },
})

/** Nguồn của giá trị Hiragana/Romaji do BE chọn (khớp ImportVocabularyLookup). */
const SOURCE_TAG: Record<string, { color: string; label: string; hint: string }> = {
  FILE: { color: 'blue', label: 'file', hint: 'Lấy từ file Excel' },
  DICT: { color: 'default', label: 'từ điển', hint: 'Lấy từ từ điển' },
  AUTO: { color: 'orange', label: 'tự sinh', hint: 'Hệ thống tự sinh (Kuromoji) — nên kiểm tra lại' },
}

/**
 * Cột giá trị BE đã chọn cuối cùng (file → từ điển → tự sinh) kèm nhãn nguồn.
 * Giá trị rỗng ('-' trong file) hiển thị "không hiển thị".
 */
export const resolvedColumn = (
  key: string,
  sourceKey: string,
  title: string,
  width = 160,
  jp = false,
): ColumnType<ImportRowResult> => ({
  title,
  key,
  width,
  render: (_, row) => {
    const source = SOURCE_TAG[String(row.data[sourceKey] ?? '')]
    if (!source) return EMPTY
    const raw = row.data[key]
    const v = str(raw)
    return (
      <span className="inline-flex flex-wrap items-center gap-1" title={source.hint}>
        {v ? <span className={jp ? 'text-base' : ''}>{v}</span> : raw === '' ? <Tag>không hiển thị</Tag> : EMPTY}
        <Tag color={source.color} className="!m-0 !text-[10px] !leading-4">
          {source.label}
        </Tag>
      </span>
    )
  },
})

/** Cột giá trị tra từ điển (không có trong file). */
export const dictColumn = (key: string, title: string, width = 200): ColumnType<ImportRowResult> => ({
  title,
  key,
  width,
  render: (_, row) => {
    const v = row.data[key]
    const text = Array.isArray(v) ? v.join('、') : str(v)
    return text ? <span className="text-gray-500">{text}</span> : EMPTY
  },
})

/** Cột đếm số dòng con (câu ví dụ / từ liên quan) BE đã gắn vào item. */
export const countColumn = (key: string, title: string, fallbackText?: string): ColumnType<ImportRowResult> => ({
  title,
  key,
  width: 120,
  render: (_, row) => {
    const n = row.data[key]
    if (typeof n !== 'number') return EMPTY
    if (n === 0 && fallbackText) return <span className="text-xs text-gray-400">{fallbackText}</span>
    return <Tag color="blue">{n}</Tag>
  },
})

/** Cột "thuộc dòng" ở sheet phụ: dòng tương ứng trong sheet chính. */
export const linkedRowColumn = (sheetLabel: string): ColumnType<ImportRowResult> => ({
  title: `Dòng ở ${sheetLabel}`,
  key: 'linkedRow',
  width: 110,
  render: (_, row) => (typeof row.data.linkedRow === 'number' ? `#${row.data.linkedRow}` : EMPTY),
})
