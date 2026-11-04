/** Khớp enum `BroadcastAudience` ở BE. */
export type BroadcastAudience = 'ALL_USERS' | 'ROLE' | 'COURSE_STUDENTS'

/** Khớp `BroadcastRequest` ở BE (POST /admin/broadcasts). */
export interface BroadcastPayload {
  title: string
  body: string
  audience: BroadcastAudience
  role?: string
  courseId?: number
  /** Đường dẫn trong ứng dụng, bắt đầu bằng "/" */
  link?: string
}

/** Khớp `BroadcastResponse` ở BE (lịch sử gửi). */
export interface Broadcast {
  id: number
  title: string
  body: string
  audience: BroadcastAudience
  role?: string | null
  courseId?: number | null
  courseTitle?: string | null
  link?: string | null
  recipientCount: number
  senderId?: number | null
  senderName?: string | null
  createdAt: string
}

export interface RecipientCountParams {
  audience: BroadcastAudience
  role?: string
  courseId?: number
}
