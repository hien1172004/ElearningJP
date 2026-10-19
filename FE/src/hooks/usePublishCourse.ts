import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, CourseSummary } from '@/types'

export function usePublishCourse(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (publish: boolean) => {
      return courseService.publishCourse(id, publish)
    },
    onSuccess: (res, publish) => {
      const data = res?.data as CourseSummary | undefined
      toast.success(publish ? 'Đã xuất bản khóa học' : 'Đã chuyển về bản nháp', {
        description: data?.title,
      })
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(id) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể thay đổi trạng thái. Vui lòng thử lại.'
      toast.error('Cập nhật trạng thái thất bại', { description: msg })
    },
  })
}
