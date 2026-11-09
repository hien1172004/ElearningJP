/**
 * Types cho module thông báo (Notification).
 *
 * Đối chiếu với BE:
 *  - NotificationResponse     → AppNotification
 *  - NotificationListResponse → NotificationListResponse
 *  - UnreadCountResponse      → { count }
 *
 * Payload STOMP trên `/user/queue/notifications` có đúng shape của `AppNotification`
 * nên FE dùng chung một type cho cả REST và WebSocket.
 */

export type NotificationType =
  | 'SRS_DUE'
  | 'STREAK_WARNING'
  | 'TASK_REMINDER'
  | 'EXAM_RESULT'
  | 'BADGE_UNLOCKED'
  | 'COMMUNITY_REPLY'
  | 'COMMUNITY_LIKE'
  | 'COMMUNITY_DISLIKE'
  /** Bình luận / trả lời bị người kiểm duyệt ẩn hoặc xóa */
  | 'COMMENT_MODERATED'
  /** Thông báo hàng loạt do ADMIN / TEACHER gửi */
  | 'ANNOUNCEMENT'
  /** Khóa học đã đăng ký được chuyển giữa bản nháp và xuất bản */
  | 'COURSE_STATUS_CHANGED'
  /** Giáo viên được ADMIN giao hoặc gỡ khỏi một khóa học */
  | 'COURSE_TEACHER_ASSIGNMENT'
  /** Luồng duyệt khóa học: gửi / rút / duyệt / từ chối / xuất bản / chuyển nháp (dataJson.action) */
  | 'COURSE_REVIEW'

/** Dữ liệu đính kèm — dùng để điều hướng khi click (vd reply bình luận). */
export interface NotificationData {
  commentId?: number
  replyId?: number
  targetType?: 'LESSON' | 'VOCABULARY' | 'KANJI' | 'GRAMMAR'
  targetId?: number
  /** ANNOUNCEMENT / COURSE_STATUS_CHANGED: khóa học liên quan / đường dẫn trong ứng dụng */
  courseId?: number
  link?: string
  [key: string]: unknown
}

export interface AppNotification {
  id: number
  title: string
  body: string
  type: NotificationType
  /** BE trả field `dataJson` (khớp cột data_json) — có thể null. */
  dataJson?: NotificationData | null
  read: boolean
  createdAt: string
}

export interface NotificationListResponse {
  items: AppNotification[]
  total: number
  page: number
  size: number
  totalPages: number
  unreadCount: number
}

export interface UnreadCountResponse {
  count: number
}
