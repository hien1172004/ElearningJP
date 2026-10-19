import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, LessonUpdateRequest, LessonDetail } from '@/types'

export function useUpdateLesson(lessonId: number, courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload: LessonUpdateRequest) => {
      return lessonService.updateLesson(lessonId, payload)
    },
    onSuccess: (res) => {
      const data = res?.data as LessonDetail | undefined
      toast.success('Đã cập nhật bài học', { description: data?.title })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
      if (courseId) {
        qc.invalidateQueries({ queryKey: ['courses', courseId, 'lessons'] })
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      }
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể cập nhật bài học. Vui lòng thử lại.'
      toast.error('Cập nhật thất bại', { description: msg })
    },
  })
}
