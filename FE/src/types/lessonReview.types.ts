import type { CourseReviewStatus, ReviewRequestStatus } from './courseReview.types'
import type { LessonSection } from './course.types'

/** Trạng thái duyệt của 1 bài (ADMIN / giáo viên được giao) — BE: LessonReviewStateResponse. */
export interface LessonReviewState {
  lessonId: number
  title: string
  orderIndex?: number | null
  /**
   * PENDING_REVIEW: giáo viên đã gửi duyệt bài — nháp giáo viên chỉ đọc; ADMIN duyệt / từ chối trước khi lưu bài.
   * Trạng thái này giống nhau với cả 2 phía (V65).
   */
  reviewStatus: CourseReviewStatus
  /** Nháp phía người xem (giáo viên / ADMIN — V65) khác bản học viên đang học. */
  hasUnpublishedChanges: boolean
  /** Gửi kèm khi gửi duyệt bài này; lệch (có người vừa sửa) → BE báo xung đột cho bài đó. */
  draftRevision: number
  /** Bài đã có trong bản học viên đang học (false = bài mới, chưa từng duyệt). */
  inPublishedVersion: boolean
  draftUpdatedByName?: string | null
  draftUpdatedAt?: string | null
  pendingRequestId?: number | null
  pendingSubmittedByName?: string | null
  pendingSubmittedAt?: string | null
  /**
   * Nháp phía người xem đã cũ: bản học viên đang học đổi sau khi nháp được tạo (phía kia vừa xuất bản) → phải
   * "Cập nhật bản mới" trước khi gửi duyệt / lưu.
   */
  outdated: boolean
  /** Phía người xem còn nháp cũ chỉ đọc (giữ lại khi cập nhật bản mới) để chép lại phần còn thiếu. */
  hasArchivedDraft: boolean
}

/** Người đang giữ khóa sửa khóa học của phía mình — BE: CourseEditLockService.LockInfo (V65). */
export interface CourseEditLock {
  userId: number
  userName?: string | null
  since?: string | null
}

/**
 * Nội dung 1 bản nháp / bản chụp bài học (dạng {@code LessonSnapshot} ở BE) — dùng để xem nháp cũ chỉ đọc.
 * Chỉ khai báo phần FE hiển thị.
 */
export interface LessonDraftSnapshot {
  detail?: { title?: string | null } | null
  sections?: LessonSection[]
  quiz?: {
    title?: string | null
    questions?: { id?: number | null; questionText?: string | null; questionType?: string | null }[]
  } | null
  speaking?: { scenarioId?: number | null; title?: string | null }[]
}

/** Một lượt gửi 1 bài lên duyệt — BE: LessonReviewRequestResponse. */
export interface LessonReviewRequest {
  id: number
  lessonId: number
  lessonTitle: string
  courseId: number
  courseTitle: string
  /** Các bài gửi cùng lượt có chung mã này. */
  batchId: string
  status: ReviewRequestStatus
  note?: string | null
  submittedById?: number | null
  submittedByName?: string | null
  createdAt?: string | null
  reviewedById?: number | null
  reviewedByName?: string | null
  /** Lý do từ chối / ghi chú khi duyệt. */
  reviewNote?: string | null
  reviewedAt?: string | null
  /** Duyệt kèm "thay đổi lớn" (đã báo học viên đã hoàn thành bài). */
  majorChange?: boolean
}

export interface LessonReviewSubmitPayload {
  lessons: { lessonId: number; draftRevision: number }[]
  note?: string
}

/** Kết quả gửi nhiều bài: mỗi bài xử lý độc lập — BE: LessonReviewSubmitResponse. */
export interface LessonReviewSubmitResult {
  batchId: string
  submittedCount: number
  failedCount: number
  results: {
    lessonId: number
    submitted: boolean
    requestId?: number | null
    /** Mã lỗi BE khi bài đó chưa gửi được (vd 9001: có người vừa sửa bài). */
    code?: number | null
    message?: string | null
  }[]
}

export interface LessonReviewQueueParams {
  status?: ReviewRequestStatus
  /** Tìm theo tên bài, tên khóa học hoặc tên người gửi. */
  keyword?: string
  page?: number
  size?: number
}
