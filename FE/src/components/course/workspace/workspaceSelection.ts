/**
 * Mục đang mở trong không gian soạn khóa học, lưu trên URL để F5 / chia sẻ link /
 * nút quay lại của trình duyệt đều đúng chỗ:
 *   ?view=overview | exams | settings
 *   ?lesson=5                → tổng quan bài 5 (danh sách mục)
 *   ?lesson=5&section=12     → editor của mục (section) 12
 *   ?lesson=5&section=quiz   → quiz của bài 5
 */
export type WorkspaceSelection =
  | { view: 'overview' }
  | { view: 'exams' }
  | { view: 'settings' }
  | { view: 'lesson'; lessonId: number; section?: number | 'quiz' }

const toPositiveInt = (raw: string | null): number | null => {
  const n = Number(raw)
  return Number.isInteger(n) && n > 0 ? n : null
}

export const parseSelection = (params: URLSearchParams): WorkspaceSelection => {
  const lessonId = toPositiveInt(params.get('lesson'))
  if (lessonId) {
    const rawSection = params.get('section')
    const section = rawSection === 'quiz' ? 'quiz' : (toPositiveInt(rawSection) ?? undefined)
    return { view: 'lesson', lessonId, section }
  }
  const view = params.get('view')
  if (view === 'exams' || view === 'settings') return { view }
  return { view: 'overview' }
}

export const toSearchParams = (sel: WorkspaceSelection): URLSearchParams => {
  const params = new URLSearchParams()
  if (sel.view === 'lesson') {
    params.set('lesson', String(sel.lessonId))
    if (sel.section != null) params.set('section', String(sel.section))
  } else if (sel.view !== 'overview') {
    params.set('view', sel.view)
  }
  return params
}
