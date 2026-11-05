import type { CommentTargetType, ReportReason } from './comment.types'

export type ModerationKind = 'COMMENT' | 'REPLY'
export type ModerationContentStatus = 'VISIBLE' | 'HIDDEN' | 'DELETED'
export type ModerationReportStatus = 'OPEN' | 'RESOLVED' | 'REJECTED'

/** Khớp `ModerationItemResponse` ở BE (/admin/moderation/**). */
export interface ModerationItem {
  kind: ModerationKind
  id: number
  /** Bình luận gốc (với REPLY là bình luận chứa trả lời) */
  commentId: number
  content: string
  status: ModerationContentStatus
  author?: {
    id: number
    fullName?: string | null
    email?: string | null
    avatarUrl?: string | null
  } | null
  targetType: CommentTargetType
  targetId: number
  targetLabel?: string | null
  courseId?: number | null
  courseTitle?: string | null
  createdAt: string
  reportCount: number
  latestReportAt?: string | null
  reasons: { reason: ReportReason; count: number }[]
  recentDetails: string[]
}

export interface ModerationQueueParams {
  status?: ModerationReportStatus
  page?: number
  size?: number
}

export interface ModerationBrowseParams {
  /** Bỏ trống = gộp cả bình luận gốc và trả lời */
  kind?: ModerationKind
  status?: Exclude<ModerationContentStatus, 'DELETED'>
  targetType?: CommentTargetType
  keyword?: string
  page?: number
  size?: number
}

export type ModerationAction = 'hide' | 'restore' | 'delete' | 'dismiss'
