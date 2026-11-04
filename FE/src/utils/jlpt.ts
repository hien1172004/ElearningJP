import type { JlptLevel } from '@/types'

/**
 * Màu cấp độ JLPT theo bảng 和色 (token `--color-jlpt-*` trong index.css).
 * Đi từ nhạt/tươi (N5) đến trầm/đậm (N1) để người học cảm nhận độ khó tăng dần.
 * Chỉ dùng cho nhãn nhỏ (badge, chữ, viền) — không tô nền cả khối.
 */
export const JLPT_LEVEL_TEXT: Record<JlptLevel, string> = {
  N5: 'text-jlpt-n5',
  N4: 'text-jlpt-n4',
  N3: 'text-jlpt-n3',
  N2: 'text-jlpt-n2',
  N1: 'text-jlpt-n1',
}

export const JLPT_LEVEL_BORDER: Record<JlptLevel, string> = {
  N5: 'border-jlpt-n5',
  N4: 'border-jlpt-n4',
  N3: 'border-jlpt-n3',
  N2: 'border-jlpt-n2',
  N1: 'border-jlpt-n1',
}

/** Badge nhỏ: chữ + viền màu cấp độ, nền trắng. */
export const JLPT_LEVEL_BADGE: Record<JlptLevel, string> = {
  N5: 'bg-white text-jlpt-n5 border border-jlpt-n5/40',
  N4: 'bg-white text-jlpt-n4 border border-jlpt-n4/40',
  N3: 'bg-white text-jlpt-n3 border border-jlpt-n3/40',
  N2: 'bg-white text-jlpt-n2 border border-jlpt-n2/40',
  N1: 'bg-white text-jlpt-n1 border border-jlpt-n1/40',
}
