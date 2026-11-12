import { useCallback } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { CourseDetail } from '@/types'

export function useCourseDetail(id: number | string | undefined) {
  const numericId = typeof id === 'string' ? Number(id) : (id as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<CourseDetail | null>({
    queryKey: enabled
      ? QUERY_KEYS.course.detail(safeId)
      : ['courses', 'detail', 'disabled'],
    queryFn: async () => {
      const res = await courseService.getCourseById(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) {
        return false
      }
      return failureCount < 2
    },
  })
}

/**
 * Tải lại chi tiết khóa học bỏ qua cache (cập nhật luôn cache của {@link useCourseDetail}).
 * Dùng khi cần cờ trạng thái mới nhất (vd: hasUnpublishedChanges) — sửa section / item / quiz
 * không làm mới course detail.
 */
export function useFetchFreshCourseDetail() {
  const qc = useQueryClient()
  return (id: number) =>
    qc.fetchQuery<CourseDetail | null>({
      queryKey: QUERY_KEYS.course.detail(id),
      queryFn: async () => (await courseService.getCourseById(id)).data ?? null,
      staleTime: 0,
    })
}

/**
 * Sau mỗi thao tác ghi nội dung (section, item, quiz, luyện nói, đề thi gắn khóa...): BE bật cờ
 * {@code hasUnpublishedChanges} của khóa → làm mới course detail (nhãn trạng thái, nút Gửi duyệt / Lưu)
 * và danh sách thay đổi. Hook nội dung chỉ biết lessonId nên làm mới mọi course detail —
 * chỉ query đang hiển thị mới tải lại.
 */
export function invalidateCourseDraftState(qc: ReturnType<typeof useQueryClient>) {
  qc.invalidateQueries({ queryKey: QUERY_KEYS.course.detailAll })
  qc.invalidateQueries({ queryKey: QUERY_KEYS.courseReview.changesAll })
  // Mỗi lần ghi nội dung bài tăng draftRevision của bài → trạng thái duyệt theo bài phải tải lại,
  // nếu không lần gửi duyệt sau sẽ gửi revision cũ và tự xung đột với chính mình
  qc.invalidateQueries({ queryKey: QUERY_KEYS.lessonReview.all })
}

/** Bản hook của {@link invalidateCourseDraftState} cho component gọi service trực tiếp. */
export function useInvalidateCourseDraftState() {
  const qc = useQueryClient()
  return useCallback(() => invalidateCourseDraftState(qc), [qc])
}
