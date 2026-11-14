import { useMutation, useQueryClient } from '@tanstack/react-query'
import { progressService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonProgressUpdateRequest } from '@/types'

/** Lưu tiến độ trong bài (số mục đã xem / % cả bài) lên server. */
export function useUpdateLessonProgress(lessonId: number, courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: LessonProgressUpdateRequest) =>
      progressService.updateSectionProgress(lessonId, payload),
    onSuccess: (_res, payload) => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
      // % bài trong mục lục khóa học (sidebar + trang khóa học)
      if (courseId && payload.lessonPercent != null) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      }
    },
    onError: (error: any) => {
      // Không chặn việc học; lần xem kế tiếp sẽ gửi lại giá trị lớn hơn.
      console.warn('[useUpdateLessonProgress]', error?.response?.data?.message)
    },
  })
}
