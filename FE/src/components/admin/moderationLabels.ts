import type { CommentTargetType } from '@/types'

/** Tên loại nội dung được bình luận (trang kiểm duyệt). */
export const TARGET_TYPE_LABELS: Record<CommentTargetType, string> = {
  LESSON: 'Bài học',
  VOCABULARY: 'Từ vựng',
  KANJI: 'Kanji',
  GRAMMAR: 'Ngữ pháp',
}
