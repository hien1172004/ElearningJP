import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, CourseUpdateRequest, CourseSummary } from '@/types'

export function useUpdateCourse(id: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload: CourseUpdateRequest) => {
      return courseService.updateCourse(id, payload)
    },
    onSuccess: (res) => {
      const data = res?.data as CourseSummary | undefined
      toast.success('Cập nhật khóa học thành công', {
        description: data?.title,
      })
      qc.invalidateQueries({ queryKey: ['courses'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(id) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể cập nhật khóa học. Vui lòng thử lại.'
      toast.error('Cập nhật thất bại', { description: msg })
    },
  })
}
