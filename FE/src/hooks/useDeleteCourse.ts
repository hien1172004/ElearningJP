import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { courseService } from '@/services'
import type { ApiResponse } from '@/types'

export function useDeleteCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (id: number) => {
      return courseService.deleteCourse(id)
    },
    onSuccess: () => {
      toast.success('Đã xoá khóa học')
      qc.invalidateQueries({ queryKey: ['courses'] })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể xoá khóa học. Vui lòng thử lại.'
      toast.error('Xoá thất bại', { description: msg })
    },
  })
}
