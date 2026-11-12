import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { isConcurrentModification } from '@/lib/queryClient'
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
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all }) // trạng thái duyệt theo bài (draftRevision)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      if (isConcurrentModification(error)) return // modal "Tải bản mới nhất" đã xử lý
      const msg =
        error.response?.data?.message ??
        'Không thể sắp xếp lại. Vui lòng thử lại.'
      toast.error('Sắp xếp thất bại', { description: msg })
    },
  })
}
