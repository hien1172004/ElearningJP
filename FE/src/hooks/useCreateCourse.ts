import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { courseService } from '@/services'
import type {
  ApiResponse,
  CourseCreateRequest,
  CourseSummary,
} from '@/types'

export function useCreateCourse() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (payload: CourseCreateRequest) => {
      return courseService.createCourse(payload)
    },
    onSuccess: (res) => {
      const data = res?.data as CourseSummary | undefined
      toast.success('Tạo khóa học thành công', {
        description: data?.title ? `Khóa học "${data.title}" đã được tạo.` : undefined,
      })
      qc.invalidateQueries({ queryKey: ['courses'] })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể tạo khóa học. Vui lòng thử lại.'
      toast.error('Tạo khóa học thất bại', { description: msg })
    },
  })
}
