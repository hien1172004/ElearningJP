import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, CourseProgress } from '@/types'

export function useCompleteLesson(lessonId: number, courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async () => {
      return lessonService.completeLesson(lessonId)
    },
    onSuccess: (res) => {
      const data = res?.data as CourseProgress | undefined
      toast.success('Hoàn thành bài học!', {
        description: data
          ? `Tiến độ khóa học: ${data.progressPercent}%`
          : undefined,
      })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
      if (courseId) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
        qc.invalidateQueries({ queryKey: ['courses', courseId, 'lessons'] })
      }
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể hoàn thành bài học. Vui lòng thử lại.'
      toast.error('Hoàn thành thất bại', { description: msg })
    },
  })
}
