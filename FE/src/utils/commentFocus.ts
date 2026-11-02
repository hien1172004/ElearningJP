import type { CommentLocation } from '@/types'

/**
 * Deep-link tới 1 bình luận: dựng URL từ vị trí BE trả về và đọc lại ở trang đích.
 *
 * Query param dùng chung cho mọi trang có `CommentSection`:
 *  - `comment` — id bình luận cần mở
 *  - `reply`   — id reply cần mở (tuỳ chọn)
 *  - `cpos`    — thứ tự 0-based của bình luận (mới nhất trước) để tính trang
 */
export const COMMENT_FOCUS_PARAM = {
  comment: 'comment',
  reply: 'reply',
  position: 'cpos',
} as const

/** Thời gian giữ highlight sau khi cuộn tới bình luận / reply (ms). */
export const FOCUS_HIGHLIGHT_MS = 3000

/** Query param chọn sẵn mục (từ vựng / ngữ pháp / chữ Hán) ở trang tra cứu. */
export const DICTIONARY_ID_PARAM = 'id'

export interface CommentFocus {
  commentId: number
  replyId?: number
  /** Thứ tự 0-based trong danh sách mới nhất trước. */
  position: number
}

function positiveInt(value: string | null): number | undefined {
  if (!value) return undefined
  const n = Number(value)
  return Number.isInteger(n) && n > 0 ? n : undefined
}

/** Đọc bình luận cần mở từ URL; không có / sai định dạng → `null`. */
export function parseCommentFocus(params: URLSearchParams): CommentFocus | null {
  const commentId = positiveInt(params.get(COMMENT_FOCUS_PARAM.comment))
  if (!commentId) return null
  const position = Number(params.get(COMMENT_FOCUS_PARAM.position))
  return {
    commentId,
    replyId: positiveInt(params.get(COMMENT_FOCUS_PARAM.reply)),
    position: Number.isInteger(position) && position >= 0 ? position : 0,
  }
}

/** Id mục cần chọn sẵn ở trang tra cứu (`?id=`). */
export function parseDictionaryTargetId(params: URLSearchParams): number | undefined {
  return positiveInt(params.get(DICTIONARY_ID_PARAM))
}

const DICTIONARY_TAB: Record<string, string> = {
  VOCABULARY: 'vocab',
  KANJI: 'kanji',
  GRAMMAR: 'grammar',
}

/**
 * Dựng URL mở thẳng tới bình luận.
 *  - LESSON → `/learn/{courseId}/{lessonId}?courseId&lessonId&…` (trang bài tự đổi sang slug)
 *  - Tra cứu → `/dictionary?tab&q=<từ khóa>&id=<targetId>&…`
 * Thiếu dữ liệu để dựng (vd bài học không có courseId) → `null`.
 */
export function buildCommentLocationUrl(location: CommentLocation): string | null {
  const params = new URLSearchParams()
  let path: string

  if (location.targetType === 'LESSON') {
    if (!location.courseId) return null
    path = `/learn/${location.courseId}/${location.targetId}`
    params.set('courseId', String(location.courseId))
    params.set('lessonId', String(location.targetId))
  } else {
    const tab = DICTIONARY_TAB[location.targetType]
    if (!tab || !location.keyword) return null
    path = '/dictionary'
    params.set('tab', tab)
    params.set('q', location.keyword)
    params.set(DICTIONARY_ID_PARAM, String(location.targetId))
  }

  params.set(COMMENT_FOCUS_PARAM.comment, String(location.commentId))
  if (location.replyId) params.set(COMMENT_FOCUS_PARAM.reply, String(location.replyId))
  params.set(COMMENT_FOCUS_PARAM.position, String(location.position))

  return `${path}?${params.toString()}`
}
