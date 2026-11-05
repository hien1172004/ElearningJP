/**
 * Theo dõi "đã đi bao nhiêu bài kể từ trang khóa học" trong history state, để nút quay lại
 * ở trang học bài LÙI về đúng trang khóa học đã mở trước đó (giữ nguyên lịch sử phía trước
 * như trang quản lý khóa học), thay vì đẩy thêm một trang khóa học mới vào lịch sử.
 *
 *  - Vào bài từ trang khóa học        → { lessonDepth: 1 }   (dùng `LESSON_FROM_COURSE_STATE`)
 *  - Chuyển sang bài khác trong viewer → lessonDepth + 1      (dùng `nextLessonState`)
 *  - Vào thẳng (thông báo, link…)      → không có state → lessonDepth = 0
 */
export interface LessonNavState {
  lessonDepth?: number
}

export const LESSON_FROM_COURSE_STATE: LessonNavState = { lessonDepth: 1 }

export const getLessonDepth = (state: unknown): number => {
  const depth = (state as LessonNavState | null)?.lessonDepth
  return typeof depth === 'number' && depth > 0 ? depth : 0
}

/** State cho lần chuyển bài tiếp theo: chỉ tăng khi ban đầu vào từ trang khóa học. */
export const nextLessonState = (state: unknown): LessonNavState | undefined => {
  const depth = getLessonDepth(state)
  return depth > 0 ? { lessonDepth: depth + 1 } : undefined
}
