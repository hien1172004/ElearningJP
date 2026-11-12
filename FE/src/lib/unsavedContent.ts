/** Một đoạn nội dung người dùng vừa gửi lưu nhưng bị từ chối (xung đột version) — để chép lại. */
export interface UnsavedEntry {
  label: string
  value: string
}

/** Trường kỹ thuật / mã enum: không phải nội dung người dùng gõ, không cần chép lại. */
const SKIP_KEYS = new Set([
  'imageUrl',
  'audioUrl',
  'customAudioUrl',
  'id',
  'ids',
  'itemIds',
  'version',
  'versions',
  'orderIndex',
  'optionKey',
  'matchKey',
  'key',
  'questionType',
  'sectionType',
  'jlptLevel',
  'partOfSpeech',
])

const LABELS: Record<string, string> = {
  title: 'Tiêu đề',
  description: 'Mô tả',
  content: 'Nội dung',
  note: 'Ghi chú',
  questionText: 'Câu hỏi',
  explanation: 'Giải thích',
  optionText: 'Đáp án',
  correctTextAnswer: 'Đáp án đúng',
  label: 'Vế ghép',
  exampleJp: 'Ví dụ (tiếng Nhật)',
  exampleVi: 'Ví dụ (tiếng Việt)',
  meaningVi: 'Nghĩa',
  word: 'Từ vựng',
  reading: 'Cách đọc',
  hiragana: 'Hiragana',
  structure: 'Cấu trúc',
  thumbnailUrl: 'Ảnh bìa',
  examples: 'Ví dụ',
  exampleRomaji: 'Ví dụ (romaji)',
  customMeaning: 'Nghĩa',
  customHiragana: 'Hiragana',
  customRomaji: 'Romaji',
  usageNote: 'Cách dùng',
  mcOptions: 'Đáp án',
  newVocabs: 'Từ mới',
  correctText: 'Đáp án đúng',
  matchingData: 'Ghép cặp',
  left: 'Vế trái',
  right: 'Vế phải',
}

/**
 * Lấy các đoạn chữ người dùng đã nhập từ biến của mutation bị từ chối (vd: {@code { id, payload: {...} }}).
 * Bỏ trường kỹ thuật, chuỗi rỗng; phần tử mảng đánh số "#1, #2…" để biết đoạn nào thuộc dòng nào.
 */
export function extractUnsavedEntries(variables: unknown): UnsavedEntry[] {
  const entries: UnsavedEntry[] = []

  const walk = (value: unknown, label: string | null) => {
    if (typeof value === 'string') {
      if (value.trim() && label) entries.push({ label, value })
      return
    }
    if (Array.isArray(value)) {
      value.forEach((item, i) => walk(item, label ? `${label} #${i + 1}` : `#${i + 1}`))
      return
    }
    if (value && typeof value === 'object') {
      for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
        if (SKIP_KEYS.has(key)) continue
        // "payload" (hook) / "form" (bản tự lưu) chỉ là lớp bọc → không thêm vào nhãn
        if (key === 'payload' || key === 'form') {
          walk(child, label)
          continue
        }
        const name = LABELS[key] ?? key
        walk(child, label ? `${label} › ${name}` : name)
      }
    }
  }

  walk(variables, null)
  return entries
}

const HTML_TAG = /<\/?(p|br|strong|b|u|ruby|rt|span)\b/i

/** Chuỗi do trình soạn thảo tiếng Nhật sinh ra (HTML) hay chữ thường. */
export const isRichText = (value: string) => HTML_TAG.test(value)
