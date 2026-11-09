/** Trạng thái bản đang soạn của khóa — BE: CourseReviewStatus. */
export type CourseReviewStatus = 'EDITING' | 'PENDING_REVIEW'

/** Trạng thái 1 lượt gửi duyệt — BE: ReviewRequestStatus. */
export type ReviewRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'

/** Một lượt gửi duyệt khóa học — BE: CourseReviewRequestResponse. */
export interface CourseReviewRequest {
  id: number
  courseId: number
  courseTitle: string
  status: ReviewRequestStatus
  /** Ghi chú của người gửi. */
  note?: string | null
  submittedById?: number | null
  submittedByName?: string | null
  createdAt?: string | null
  /** ADMIN duyệt / từ chối, hoặc người rút yêu cầu. */
  reviewedById?: number | null
  reviewedByName?: string | null
  /** Lý do từ chối / ghi chú khi duyệt. */
  reviewNote?: string | null
  reviewedAt?: string | null
}

export interface CourseReviewQueueParams {
  status?: ReviewRequestStatus
  page?: number
  size?: number
}

/** Nhận xét duyệt của ADMIN (cả khóa / 1 bài / 1 mục) — BE: CourseReviewCommentResponse. */
export interface CourseReviewComment {
  id: number
  reviewRequestId: number
  /** Trạng thái lượt duyệt chứa nhận xét — PENDING: ADMIN còn thêm / xóa được. */
  reviewStatus: ReviewRequestStatus
  /** null = nhận xét chung cho cả khóa. */
  lessonId?: number | null
  lessonTitle?: string | null
  sectionId?: number | null
  sectionTitle?: string | null
  content: string
  authorId?: number | null
  authorName?: string | null
  resolved: boolean
  resolvedByName?: string | null
  resolvedAt?: string | null
  createdAt?: string | null
}

export interface CourseReviewCommentPayload {
  lessonId?: number
  sectionId?: number
  content: string
}

/** ADDED: chưa có ở bản đang chạy · REMOVED: sẽ bị gỡ · MODIFIED: có sửa · UNCHANGED. */
export type ChangeStatus = 'ADDED' | 'REMOVED' | 'MODIFIED' | 'UNCHANGED'

export interface ItemsChange {
  added: number
  removed: number
  modified: number
  reordered: boolean
  metaChanged: boolean
}

export interface SectionChange {
  sectionId: number
  title?: string | null
  sectionType: 'VOCABULARY' | 'KANJI' | 'GRAMMAR'
  status: ChangeStatus
  items?: ItemsChange | null
}

export interface LessonChange {
  lessonId: number
  title?: string | null
  orderIndex?: number | null
  publishedOrderIndex?: number | null
  status: ChangeStatus
  infoChanged: boolean
  moved: boolean
  sections: SectionChange[]
  quiz?: ItemsChange | null
  speakingChanged: boolean
}

export interface ExamChange {
  examId: number
  title?: string | null
  status: ChangeStatus
}

/** Thay đổi của bản đang soạn so với bản học viên đang học — BE: CourseChangesResponse. */
export interface CourseChanges {
  /** null = khóa chưa có bản được duyệt (mọi bài đều mới). */
  publishedVersionNo?: number | null
  publishedAt?: string | null
  hasChanges: boolean
  lessons: LessonChange[]
  exams: ExamChange[]
}
