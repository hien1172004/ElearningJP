import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, LessonOutline } from '@/types'
import { toVersionMap } from '@/utils/versions'

export function useReorderLessons(courseId: number) {
  const qc = useQueryClient()
  return useMutation({
    /** Danh sách bài theo thứ tự MỚI (kèm version đang thấy). */
    mutationFn: async (lessons: Pick<LessonOutline, 'id' | 'version'>[]) => {
      return lessonService.reorderLessons(courseId, {
        lessonIds: lessons.map((l) => l.id),
        versions: toVersionMap(lessons),
      })
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
