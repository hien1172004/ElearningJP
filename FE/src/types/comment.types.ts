/**
 * Types cho module bình luận (Comment).
 *
 * Đối chiếu với BE:
 *  - CommentResponse        → CommentItem
 *  - CommentReplyResponse   → CommentReplyItem
 *  - CommentListResponse    → CommentListResponse
 *  - CommentReplyListResponse → CommentReplyListResponse
 */

/** Loại đối tượng mà bình luận được gắn vào. */
export type CommentTargetType = 'LESSON' | 'VOCABULARY' | 'KANJI' | 'GRAMMAR'

/** Thông tin tối giản của user — BE cố tình không trả email / role. */
export interface CommentUserInfo {
  id: number
  fullName?: string
  avatarUrl?: string
}

/**
 * Trạng thái reaction sau khi toggle like / dislike.
 *
 * Đối chiếu với BE: `CommentReactionResponse`.
 *
 * FE dùng object này làm **source of truth** trong `onSuccess` để cập nhật
 * tag like/dislike ngay sau khi server xác nhận — không cần reload trang.
 */
export interface CommentReactionState {
  liked: boolean
  disliked: boolean
  likeCount: number
  dislikeCount: number
}

export interface CommentItem {
  id: number
  targetType: CommentTargetType
  targetId: number
  user?: CommentUserInfo | null
  content: string
  status?: string
  likeCount: number
  dislikeCount: number
  replyCount: number
  /** Cờ: user hiện tại đã like / dislike comment này. */
  liked: boolean
  disliked: boolean
  /** Cờ: user hiện tại là chủ sở hữu (dùng để hiện nút sửa / xoá). */
  owner: boolean
  createdAt: string
  updatedAt: string
}

export interface CommentReplyItem {
  id: number
  commentId: number
  /** Reply lồng nhau — client tự dựng cây từ trường này. */
  parentReplyId?: number | null
  user?: CommentUserInfo | null
  content: string
  status?: string
  likeCount: number
  dislikeCount: number
  liked: boolean
  disliked: boolean
  owner: boolean
  createdAt: string
  updatedAt: string
}

export interface CommentListResponse {
  items: CommentItem[]
  total: number
  page: number
  size: number
  totalPages: number
}

export interface CommentReplyListResponse {
  items: CommentReplyItem[]
  total: number
  page: number
  size: number
  totalPages: number
}
