import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse } from '@/types'

export function useReorderLessons(courseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (lessonIds: number[]) => {
      return lessonService.reorderLessons(courseId, { lessonIds })
    },
    onSuccess: () => {
      toast.success('Đã cập nhật thứ tự bài học')
      qc.invalidateQueries({ queryKey: ['courses', courseId, 'lessons'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể sắp xếp lại. Vui lòng thử lại.'
      toast.error('Sắp xếp thất bại', { description: msg })
    },
  })
}
