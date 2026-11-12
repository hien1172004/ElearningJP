import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { courseReviewService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, CourseChanges } from '@/types'

const errorMessage = (error: AxiosError<ApiResponse<unknown>>, fallback: string) =>
  error.response?.data?.message ?? fallback

/** Khóa học (danh sách + chi tiết) và dữ liệu thay đổi đều đổi sau khi ADMIN lưu cả khóa. */
const useInvalidateReview = () => {
  const qc = useQueryClient()
  return (courseId?: number) => {
    qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.all })
    qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
    qc.invalidateQueries({ queryKey: ['courses'] })
    if (courseId) qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
  }
}

/** ADMIN "Lưu cả khóa": chốt nháp ADMIN của mọi bài (kèm đề thi gắn khóa) thành phiên bản mới, không qua duyệt. */
export function useSaveCourseVersion(courseId: number) {
  const invalidate = useInvalidateReview()
  return useMutation({
    mutationFn: () => courseReviewService.saveVersion(courseId),
    onSuccess: () => {
      toast.success('Đã lưu phiên bản mới của khóa học')
      invalidate(courseId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Lưu thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      // Lỗi thường do màn hình đang giữ trạng thái cũ (vd: ADMIN vừa chốt / khóa vừa đổi) → làm mới
      invalidate(courseId)
    },
  })
}

/** Thay đổi của bản đang soạn so với bản học viên đang học (ADMIN / giáo viên được giao). */
export function useCourseChanges(courseId: number, enabled = true) {
  return useQuery<CourseChanges | null>({
    queryKey: QUERY_KEYS.courseReview.changes(courseId),
    queryFn: async () => (await courseReviewService.getChanges(courseId)).data ?? null,
    enabled: enabled && courseId > 0,
  })
}
