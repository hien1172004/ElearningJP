import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { lessonReviewService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { CONCURRENT_MODIFICATION_CODE, isConcurrentModification } from '@/lib/queryClient'
import { useConcurrentEditStore } from '@/stores/useConcurrentEditStore'
import type {
  ApiResponse,
  LessonReviewQueueParams,
  LessonReviewRequest,
  LessonReviewState,
  LessonReviewSubmitPayload,
  PageResponse,
} from '@/types'

const errorMessage = (error: AxiosError<ApiResponse<unknown>>, fallback: string) =>
  error.response?.data?.message ?? fallback

/** Sau mỗi thao tác duyệt theo bài: trạng thái / lịch sử / hàng chờ + khóa học, thay đổi chờ duyệt, bài học. */
const useInvalidateLessonReview = () => {
  const qc = useQueryClient()
  return (courseId?: number, lessonId?: number) => {
    qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
    qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.changesAll })
    if (courseId) qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detail(courseId) })
    if (lessonId) qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
  }
}

/** Trạng thái duyệt từng bài của khóa (ADMIN / giáo viên được giao). */
export function useLessonReviewStates(courseId: number, enabled = true, refetchInterval?: number) {
  return useQuery<LessonReviewState[]>({
    queryKey: QUERY_KEYS.lessonReview.states(courseId),
    queryFn: async () => (await lessonReviewService.getStates(courseId)).data ?? [],
    enabled: enabled && courseId > 0,
    refetchInterval,
  })
}

/**
 * Trạng thái duyệt của 1 bài (lấy từ danh sách trạng thái của khóa). Trang soạn bài làm mới mỗi 30 giây:
 * phiên sửa của ADMIN hết hạn (đóng tab, mất mạng) không có tín hiệu realtime.
 */
export function useLessonReviewState(courseId: number, lessonId: number, enabled = true) {
  const query = useLessonReviewStates(courseId, enabled && lessonId > 0, 30_000)
  return { ...query, data: query.data?.find((s) => s.lessonId === lessonId) }
}

/** Lịch sử gửi duyệt theo bài của khóa, mới nhất trước — phân trang ở BE (page bắt đầu từ 1). */
export function useLessonReviewHistory(courseId: number, page: number, size: number) {
  return useQuery<PageResponse<LessonReviewRequest> | null>({
    queryKey: QUERY_KEYS.lessonReview.history(courseId, page),
    queryFn: async () => (await lessonReviewService.getHistory(courseId, page, size)).data ?? null,
    enabled: courseId > 0,
    // Đổi trang: giữ trang cũ tới khi trang mới về để danh sách không nháy trống
    placeholderData: keepPreviousData,
  })
}

/** Hàng chờ duyệt theo bài của ADMIN. */
export function useLessonReviewQueue(params: LessonReviewQueueParams) {
  return useQuery<PageResponse<LessonReviewRequest> | null>({
    queryKey: QUERY_KEYS.lessonReview.queue({ ...params }),
    queryFn: async () => (await lessonReviewService.getQueue(params)).data ?? null,
    placeholderData: keepPreviousData,
  })
}

/**
 * Giáo viên gửi một hoặc nhiều bài (kèm draftRevision đang thấy). BE xử lý từng bài độc lập:
 * bài bị người khác sửa sau khi mở (9001) → modal "Tải bản mới nhất" (tải xong phải bấm gửi lại — không tự gửi);
 * lý do khác hiện theo từng bài.
 */
export function useSubmitLessonReviews(courseId: number) {
  const invalidate = useInvalidateLessonReview()
  return useMutation({
    mutationFn: async (payload: LessonReviewSubmitPayload & { titles: Record<number, string> }) => {
      const res = await lessonReviewService.submit(courseId, payload)
      return { result: res.data, titles: payload.titles }
    },
    onSuccess: ({ result, titles }) => {
      invalidate(courseId)
      if (!result) return
      if (result.submittedCount > 0) {
        toast.success(`Đã gửi ${result.submittedCount} bài lên duyệt`, {
          description: 'Bài đã gửi tạm khóa chỉnh sửa (cả quản trị viên) cho tới khi có quyết định hoặc được rút.',
        })
      }
      const failed = result.results.filter((r) => !r.submitted)
      const conflicted = failed.filter((r) => r.code === CONCURRENT_MODIFICATION_CODE)
      const others = failed.filter((r) => r.code !== CONCURRENT_MODIFICATION_CODE)
      if (others.length > 0) {
        toast.error(`${others.length} bài chưa gửi được`, {
          description: others.map((r) => `${titles[r.lessonId] ?? `Bài #${r.lessonId}`}: ${r.message}`).join('\n'),
          duration: 8000,
        })
      }
      if (conflicted.length > 0) {
        // Như mọi xung đột khác: modal 1 nút "Tải bản mới nhất", không hiện lỗi 409
        useConcurrentEditStore.getState().report([])
        toast.info(`${conflicted.length} bài vừa được người khác sửa`, {
          description: `${conflicted.map((r) => titles[r.lessonId] ?? `Bài #${r.lessonId}`).join(', ')} — tải bản mới nhất rồi bấm gửi duyệt lại.`,
          duration: 8000,
        })
      }
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      if (isConcurrentModification(error)) return // modal "Tải bản mới nhất" đã xử lý
      toast.error('Gửi duyệt thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate(courseId)
    },
  })
}

export function useWithdrawLessonReview(courseId: number) {
  const invalidate = useInvalidateLessonReview()
  return useMutation({
    mutationFn: (lessonId: number) => lessonReviewService.withdraw(lessonId),
    onSuccess: (_res, lessonId) => {
      toast.success('Đã rút yêu cầu duyệt bài', { description: 'Bạn có thể tiếp tục chỉnh sửa bài học.' })
      invalidate(courseId, lessonId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>, lessonId) => {
      if (isConcurrentModification(error)) return
      // Thường do quản trị viên vừa quyết định — message BE nói rõ ai, lúc nào
      toast.error('Rút yêu cầu thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate(courseId, lessonId)
    },
  })
}

type DecisionVars = { requestId: number; courseId: number; lessonId: number }

export function useApproveLessonReview() {
  const invalidate = useInvalidateLessonReview()
  return useMutation({
    mutationFn: ({ requestId, note, majorChange }: DecisionVars & { note?: string; majorChange?: boolean }) =>
      lessonReviewService.approve(requestId, note, majorChange),
    onSuccess: (_res, vars) => {
      toast.success('Đã duyệt bài học', { description: 'Bài đã lên bản học viên đang học (nếu khóa đang xuất bản).' })
      invalidate(vars.courseId, vars.lessonId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>, vars) => {
      if (isConcurrentModification(error)) return
      // Hai ADMIN cùng duyệt: người sau nhận kết quả của người trước trong message
      toast.error('Duyệt thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate(vars.courseId, vars.lessonId)
    },
  })
}

export function useRejectLessonReview() {
  const invalidate = useInvalidateLessonReview()
  return useMutation({
    mutationFn: ({ requestId, reason }: DecisionVars & { reason: string }) =>
      lessonReviewService.reject(requestId, reason),
    onSuccess: (_res, vars) => {
      toast.success('Đã từ chối bài học', { description: 'Bài quay về đang soạn kèm lý do cho giáo viên.' })
      invalidate(vars.courseId, vars.lessonId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>, vars) => {
      if (isConcurrentModification(error)) return
      toast.error('Từ chối thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate(vars.courseId, vars.lessonId)
    },
  })
}

/** ADMIN "Lưu" 1 bài: đưa bản nháp lên bản học viên đang học, không qua duyệt. */
export function useSaveLessonToLive(courseId: number) {
  const invalidate = useInvalidateLessonReview()
  return useMutation({
    mutationFn: ({ lessonId, majorChange }: { lessonId: number; majorChange?: boolean }) =>
      lessonReviewService.saveToLive(lessonId, majorChange),
    onSuccess: (_res, { lessonId }) => {
      toast.success('Đã lưu bài lên bản học viên đang học')
      invalidate(courseId, lessonId)
    },
    onError: (error: AxiosError<ApiResponse<unknown>>, { lessonId }) => {
      if (isConcurrentModification(error)) return
      toast.error('Lưu bài thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate(courseId, lessonId)
    },
  })
}
