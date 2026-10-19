import { useQuery } from '@tanstack/react-query'
import { quizService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonQuizManageDetail } from '@/types'

/**
 * Hook lấy đề quiz kèm đáp án đúng — chỉ dành cho Admin/Teacher.
 * Endpoint: GET /api/v1/lessons/{id}/quiz/manage
 *
 * <p>Khác với {@link useLessonQuiz} (ẩn đáp án cho học viên), hook này trả
 * thêm {@code correct}, {@code correctOptionId}, {@code correctTextAnswer},
 * {@code correctMatchingAnswers} và {@code explanation} để hiển thị trong
 * trang quản lý câu hỏi.</p>
 *
 * <p>BE sẽ tự check quyền — nếu teacher khác cố truy cập sẽ trả 403.</p>
 */
export function useLessonQuizManage(
  lessonId: number | string | undefined,
  options?: { enabled?: boolean },
) {
  const numericId = typeof lessonId === 'string' ? Number(lessonId) : (lessonId as number | undefined)
  const safeId = numericId ?? 0
  const baseEnabled = numericId != null && !Number.isNaN(numericId) && numericId > 0
  const enabled = options?.enabled !== undefined ? baseEnabled && options.enabled : baseEnabled

  return useQuery<LessonQuizManageDetail | null>({
    queryKey: enabled
      ? QUERY_KEYS.lesson.quizManage(safeId)
      : ['lessons', 'quiz', 'manage', 'disabled'],
    queryFn: async () => {
      const res = await quizService.getLessonQuizManage(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 30 * 1000, // 30s — quản lý cần dữ liệu tươi
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      // 403/404 thường là lỗi phân quyền / không tồn tại — không retry
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}
