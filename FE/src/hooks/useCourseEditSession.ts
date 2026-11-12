import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { lessonReviewService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { ApiResponse, CourseEditLock, LessonDraftSnapshot } from '@/types'

/** Khớp CourseEditLockService.TTL (5 phút) ở BE — gia hạn đều đặn trước khi hết. */
const HEARTBEAT_MS = 60_000
/**
 * Rời trang soạn → nhả khóa sau một nhịp ngắn: chuyển giữa trang khóa ↔ trang bài (cùng khóa học) hoặc StrictMode
 * gỡ / gắn lại effect ngay → trang mới hủy lệnh nhả, giữ nguyên phiên sửa.
 */
const RELEASE_DELAY_MS = 800
const pendingRelease = new Map<number, number>()

const errorMessage = (error: unknown, fallback: string) =>
  (error as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? fallback

/**
 * Ai đang giữ khóa sửa khóa học của phía mình (V65: giáo viên dùng chung 1 khóa, ADMIN dùng chung 1 khóa; hai phía
 * không chặn nhau). Làm mới mỗi 30 giây: khóa hết hạn (đóng tab, mất mạng) không có tín hiệu realtime.
 */
export function useCourseEditLock(courseId: number) {
  const userId = useAuthStore((s) => s.user?.id)
  const query = useQuery<CourseEditLock | null>({
    queryKey: QUERY_KEYS.courseEditLock.detail(courseId),
    queryFn: async () => (await lessonReviewService.getEditLock(courseId)).data ?? null,
    enabled: courseId > 0,
    refetchInterval: 30_000,
    staleTime: 10_000,
  })
  const lock = query.data ?? null
  const mine = lock != null && lock.userId === userId
  const heldByOther = lock != null && lock.userId !== userId
  return { ...query, lock, mine, heldByOther }
}

/**
 * Phiên sửa khóa học của người dùng hiện tại — dùng 1 lần mỗi trang soạn (nút "Sửa khóa học"):
 * - {@code start(takeover)}: bắt đầu sửa / tiếp quản; {@code stop()}: "Xong" (phần đã ghi vẫn ở nháp của phía mình).
 * - Đang giữ khóa: gia hạn mỗi 60 giây; mất khóa (hết hạn / bị tiếp quản) → báo và tải lại trạng thái.
 * - Rời trang soạn khi đang giữ khóa → tự nhả (BE bỏ qua nếu không còn là người giữ).
 */
export function useCourseEditSession(courseId: number) {
  const qc = useQueryClient()
  const lockQuery = useCourseEditLock(courseId)
  const { mine } = lockQuery
  const invalidate = () => {
    void qc.invalidateQueries({ queryKey: QUERY_KEYS.courseEditLock.detail(courseId) })
    void qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
  }

  const start = useMutation({
    mutationFn: (takeover: boolean) => lessonReviewService.acquireEditLock(courseId, takeover),
    onSuccess: (res, takeover) => {
      qc.setQueryData(QUERY_KEYS.courseEditLock.detail(courseId), res.data ?? null)
      toast.success(takeover ? 'Đã tiếp quản việc sửa khóa học' : 'Đã bắt đầu sửa khóa học', {
        description: 'Người cùng phía tạm thời chỉ xem được khóa học. Bấm "Xong" khi sửa xong.',
      })
      invalidate()
    },
    onError: (error) => {
      toast.error('Chưa bắt đầu sửa được', { description: errorMessage(error, 'Vui lòng thử lại.') })
      invalidate()
    },
  })

  const stop = useMutation({
    mutationFn: () => lessonReviewService.releaseEditLock(courseId),
    onSuccess: () => {
      qc.setQueryData(QUERY_KEYS.courseEditLock.detail(courseId), null)
      toast.success('Đã kết thúc sửa khóa học', {
        description: 'Phần đã sửa vẫn ở bản nháp, chưa lên bản học viên đang học.',
      })
      invalidate()
    },
    onError: (error) => toast.error('Kết thúc sửa thất bại', { description: errorMessage(error, 'Vui lòng thử lại.') }),
  })

  // Gia hạn khi đang giữ khóa
  useEffect(() => {
    if (!mine || courseId <= 0) return
    const timer = window.setInterval(() => {
      lessonReviewService.heartbeatEditLock(courseId).catch((error) => {
        toast.error('Phiên sửa khóa học đã kết thúc', {
          description: errorMessage(error, 'Phiên sửa đã hết hạn hoặc bị người khác tiếp quản.'),
        })
        void qc.invalidateQueries({ queryKey: QUERY_KEYS.courseEditLock.detail(courseId) })
      })
    }, HEARTBEAT_MS)
    return () => window.clearInterval(timer)
  }, [mine, courseId, qc])

  // Rời trang soạn khi đang giữ khóa → nhả
  useEffect(() => {
    if (!mine || courseId <= 0) return
    const scheduled = pendingRelease.get(courseId)
    if (scheduled) {
      window.clearTimeout(scheduled)
      pendingRelease.delete(courseId)
    }
    return () => {
      pendingRelease.set(
        courseId,
        window.setTimeout(() => {
          pendingRelease.delete(courseId)
          lessonReviewService.releaseEditLock(courseId).catch(() => undefined) // khóa tự hết hạn nếu lỗi
          qc.setQueryData(QUERY_KEYS.courseEditLock.detail(courseId), null)
        }, RELEASE_DELAY_MS),
      )
    }
  }, [mine, courseId, qc])

  return { ...lockQuery, start, stop }
}

/**
 * Nháp của phía mình (V65): "Bỏ nháp" (dựng lại từ bản học viên đang học) và "Cập nhật bản mới" (nháp hiện tại thành
 * nháp cũ chỉ đọc, nháp mới từ bản học viên). Cả hai cần giữ khóa sửa khóa học; nội dung bài đổi → làm mới mọi query.
 */
export function useLessonDraftActions(lessonId: number) {
  const qc = useQueryClient()
  const onError = (title: string) => (error: unknown) => {
    toast.error(title, { description: errorMessage(error, 'Vui lòng thử lại.') })
    void qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
  }

  const discard = useMutation({
    mutationFn: () => lessonReviewService.discardDraft(lessonId),
    onSuccess: () => {
      toast.success('Đã bỏ nháp', { description: 'Bài đã quay về đúng bản học viên đang học.' })
      void qc.invalidateQueries()
    },
    onError: onError('Chưa bỏ được bản nháp'),
  })

  const rebase = useMutation({
    mutationFn: () => lessonReviewService.rebaseDraft(lessonId),
    onSuccess: () => {
      toast.success('Đã cập nhật bản mới', {
        description: 'Nháp mới lấy từ bản học viên đang học. Nháp cũ được giữ để bạn chép lại phần còn thiếu.',
      })
      void qc.invalidateQueries()
    },
    onError: onError('Chưa cập nhật được bản mới'),
  })

  return { discard, rebase }
}

/** Bản giáo viên đã gửi của 1 yêu cầu duyệt — ADMIN xem để duyệt (chỉ tải khi mở xem). */
export function useLessonReviewRequestContent(requestId: number | null | undefined, enabled: boolean) {
  return useQuery<LessonDraftSnapshot | null>({
    queryKey: QUERY_KEYS.lessonReview.content(requestId ?? 0),
    queryFn: async () => (await lessonReviewService.getRequestContent(requestId as number)).data ?? null,
    enabled: enabled && !!requestId,
    staleTime: 5 * 60_000, // bản chụp không đổi
  })
}

/** Nháp cũ chỉ đọc của phía mình (chỉ tải khi mở xem). */
export function useArchivedLessonDraft(lessonId: number, enabled: boolean) {
  return useQuery<LessonDraftSnapshot | null>({
    queryKey: QUERY_KEYS.lessonReview.archived(lessonId),
    queryFn: async () => (await lessonReviewService.getArchivedDraft(lessonId)).data ?? null,
    enabled: enabled && lessonId > 0,
  })
}
