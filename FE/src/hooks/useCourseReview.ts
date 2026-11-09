import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { courseReviewService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  ApiResponse,
  CourseChanges,
  CourseReviewComment,
  CourseReviewCommentPayload,
  CourseReviewQueueParams,
  CourseReviewRequest,
  PageResponse,
} from '@/types'

const errorMessage = (error: AxiosError<ApiResponse<unknown>>, fallback: string) =>
  error.response?.data?.message ?? fallback

/** Khóa học (danh sách + chi tiết) và dữ liệu duyệt đều đổi sau mỗi thao tác duyệt. */
const useInvalidateReview = () => {
  const qc = useQueryClient()
  return (courseId?: number) => {
    qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.all })
    qc.invalidateQueries({ queryKey: ['courses'] })
    if (courseId) qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
  }
}

/** Lịch sử gửi duyệt của 1 khóa (ADMIN hoặc giáo viên được giao). */
export function useCourseReviewHistory(courseId: number) {
  return useQuery<CourseReviewRequest[]>({
    queryKey: QUERY_KEYS.courseReview.history(courseId),
    queryFn: async () => (await courseReviewService.getHistory(courseId)).data ?? [],
    enabled: courseId > 0,
  })
}

/** Hàng chờ duyệt của ADMIN. */
export function useCourseReviewQueue(params: CourseReviewQueueParams) {
  return useQuery<PageResponse<CourseReviewRequest> | null>({
    queryKey: QUERY_KEYS.courseReview.queue({ ...params }),
    queryFn: async () => (await courseReviewService.getQueue(params)).data ?? null,
    placeholderData: keepPreviousData,
  })
}

export function useSubmitCourseReview(courseId: number) {
  const invalidate = useInvalidateReview()
  return useMutation({
    mutationFn: (note?: string) => courseReviewService.submit(courseId, note),
    onSuccess: () => {
      toast.success('Đã gửi khóa học lên duyệt', {
        description: 'Nội dung tạm khóa chỉnh sửa cho tới khi quản trị viên quyết định.',
      })
      invalidate(courseId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Gửi duyệt thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
    },
  })
}

export function useWithdrawCourseReview(courseId: number) {
  const invalidate = useInvalidateReview()
  return useMutation({
    mutationFn: () => courseReviewService.withdraw(courseId),
    onSuccess: () => {
      toast.success('Đã rút yêu cầu duyệt', { description: 'Bạn có thể tiếp tục chỉnh sửa nội dung.' })
      invalidate(courseId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Rút yêu cầu thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
    },
  })
}

export function useApproveCourseReview() {
  const invalidate = useInvalidateReview()
  return useMutation({
    mutationFn: ({ requestId, note }: { requestId: number; courseId: number; note?: string }) =>
      courseReviewService.approve(requestId, note),
    onSuccess: (_res, vars) => {
      toast.success('Đã duyệt và xuất bản khóa học')
      invalidate(vars.courseId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Duyệt thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
    },
  })
}

export function useRejectCourseReview() {
  const invalidate = useInvalidateReview()
  return useMutation({
    mutationFn: ({ requestId, reason }: { requestId: number; courseId: number; reason: string }) =>
      courseReviewService.reject(requestId, reason),
    onSuccess: (_res, vars) => {
      toast.success('Đã từ chối yêu cầu duyệt')
      invalidate(vars.courseId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Từ chối thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
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

/** Nhận xét của lượt gửi duyệt gần nhất. */
export function useCourseReviewComments(courseId: number, enabled = true) {
  return useQuery<CourseReviewComment[]>({
    queryKey: QUERY_KEYS.courseReview.comments(courseId),
    queryFn: async () => (await courseReviewService.getLatestComments(courseId)).data ?? [],
    enabled: enabled && courseId > 0,
  })
}

/** ADMIN thêm / xóa nhận xét, giáo viên đánh dấu đã sửa. */
export function useCourseReviewCommentMutations(courseId: number) {
  const qc = useQueryClient()
  const invalidate = () => qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.comments(courseId) })
  const onError = (fallback: string) => (error: AxiosError<ApiResponse<unknown>>) =>
    toast.error(fallback, { description: errorMessage(error, 'Vui lòng thử lại.') })

  const add = useMutation({
    mutationFn: ({ requestId, payload }: { requestId: number; payload: CourseReviewCommentPayload }) =>
      courseReviewService.addComment(requestId, payload),
    onSuccess: () => {
      toast.success('Đã thêm nhận xét')
      invalidate()
    },
    onError: onError('Thêm nhận xét thất bại'),
  })
  const remove = useMutation({
    mutationFn: (commentId: number) => courseReviewService.deleteComment(commentId),
    onSuccess: invalidate,
    onError: onError('Xóa nhận xét thất bại'),
  })
  const resolve = useMutation({
    mutationFn: ({ commentId, resolved }: { commentId: number; resolved: boolean }) =>
      courseReviewService.resolveComment(commentId, resolved),
    onSuccess: invalidate,
    onError: onError('Cập nhật nhận xét thất bại'),
  })
  return { add, remove, resolve }
}
