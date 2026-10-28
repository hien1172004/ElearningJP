import { useMutation, useQueryClient } from '@tanstack/react-query'
import { progressService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'

export function useStartLesson(lessonId: number, courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      return progressService.startLesson(lessonId)
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
      if (courseId) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      }
    },
    onError: (error: any) => {
      // Không cần toast lỗi - lỗi thường do chưa enroll, page sẽ xử lý.
      console.warn('[useStartLesson]', error?.response?.data?.message)
    },
  })
}
