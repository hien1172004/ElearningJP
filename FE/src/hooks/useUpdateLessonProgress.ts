import { useMutation, useQueryClient } from '@tanstack/react-query'
import { progressService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonDetail, LessonProgressUpdateRequest } from '@/types'

/** Lưu tiến độ trong bài (số mục đã xem / % cả bài) lên server. */
export function useUpdateLessonProgress(lessonId: number, courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (payload: LessonProgressUpdateRequest) =>
      progressService.updateSectionProgress(lessonId, payload),
    // Mạng chập chờn / lỗi server (không có response hoặc 5xx) → tự gửi lại tối đa 2 lần;
    // lỗi 4xx (chưa đăng ký, dữ liệu sai) gửi lại cũng vô ích nên bỏ qua.
    retry: (failureCount, error: any) => failureCount < 2 && (error?.response?.status ?? 500) >= 500,
    onSuccess: (res, payload) => {
      // Server trả sẵn sectionProgress mới → cập nhật cache thay vì tải lại cả bài
      // (GET lesson detail còn ghi last_accessed_at vào DB mỗi lần gọi)
      const sectionProgress = res?.data?.sectionProgress
      if (sectionProgress) {
        qc.setQueryData<LessonDetail | null>(QUERY_KEYS.lesson.detail(lessonId), (old) =>
          old ? { ...old, sectionProgress } : old,
        )
      }
      // % bài trong mục lục khóa học (sidebar + trang khóa học) chỉ đổi khi có gửi % cả bài
      if (courseId && payload.lessonPercent != null) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      }
    },
    onError: (error: any) => {
      // Không chặn việc học; trang học tự gửi lại ở lần render sau.
      console.warn('[useUpdateLessonProgress]', error?.response?.data?.message)
    },
  })
}
