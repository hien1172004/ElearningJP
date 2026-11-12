/** Trạng thái bản đang soạn của khóa — BE: CourseReviewStatus. */
export type CourseReviewStatus = 'EDITING' | 'PENDING_REVIEW'

/** Trạng thái 1 lượt gửi duyệt — BE: ReviewRequestStatus. */
export type ReviewRequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'WITHDRAWN'

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
