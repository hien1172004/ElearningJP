import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, LessonCreateRequest, LessonDetail } from '@/types'

export function useCreateLesson(courseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload: LessonCreateRequest) => {
      return lessonService.createLesson(courseId, payload)
    },
    onSuccess: (res) => {
      const data = res?.data as LessonDetail | undefined
      toast.success('Đã thêm bài học', {
        description: data?.title,
      })
      qc.invalidateQueries({ queryKey: ['courses', courseId, 'lessons'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể thêm bài học. Vui lòng thử lại.'
      toast.error('Thêm bài học thất bại', { description: msg })
    },
  })
}
