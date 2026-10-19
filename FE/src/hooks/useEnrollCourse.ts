import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { useNavigate } from 'react-router-dom'
import { AxiosError } from 'axios'
import { enrollmentService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse } from '@/types'

export function useEnrollCourse() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  return useMutation({
    mutationFn: async (courseId: number) => {
      return enrollmentService.enroll(courseId)
    },
    onSuccess: (_res, courseId) => {
      toast.success('Đăng ký khóa học thành công!', {
        description: 'Bạn có thể bắt đầu học ngay bây giờ.',
      })
      qc.invalidateQueries({ queryKey: ['enrollments'] })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
      // Điều hướng về trang chi tiết khoá học để user bấm "Vào học".
      navigate(`/courses/${courseId}`)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const code = error.response?.data?.code
      let msg = error.response?.data?.message ?? 'Không thể đăng ký khóa học.'
      if (code === 2001) msg = 'Bạn đã đăng ký khóa học này rồi.'
      else if (code === 2002) msg = 'Khóa học chưa được xuất bản.'
      else if (code === 2003) msg = 'Khóa học đã được lưu trữ.'
      toast.error('Đăng ký thất bại', { description: msg })
    },
  })
}
