import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse } from '@/types'

export function useDeleteLesson(courseId?: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (lessonId: number) => {
      return lessonService.deleteLesson(lessonId)
    },
    onSuccess: () => {
      toast.success('Đã xoá bài học')
      if (courseId) {
        qc.invalidateQueries({ queryKey: ['courses', courseId, 'lessons'] })
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      } else {
        qc.invalidateQueries({ queryKey: ['courses'] })
        qc.invalidateQueries({ queryKey: ['lessons'] })
      }
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể xoá bài học. Vui lòng thử lại.'
      toast.error('Xoá thất bại', { description: msg })
    },
  })
}
