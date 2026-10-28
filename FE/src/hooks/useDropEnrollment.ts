import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { enrollmentService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse } from '@/types'

export function useDropEnrollment() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (params: { enrollmentId?: number; courseId?: number }) => {
      if (params.enrollmentId) {
        return enrollmentService.drop(params.enrollmentId)
      }
      if (params.courseId) {
        return enrollmentService.dropByCourseId(params.courseId)
      }
      throw new Error('Cần truyền enrollmentId hoặc courseId')
    },
    onSuccess: (_, vars) => {
      toast.success('Đã huỷ đăng ký khóa học')
      qc.invalidateQueries({ queryKey: ['enrollments'] })
      if (vars.courseId) {
        qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(vars.courseId) })
      }
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ?? 'Không thể huỷ đăng ký.'
      toast.error('Huỷ đăng ký thất bại', { description: msg })
    },
  })
}
