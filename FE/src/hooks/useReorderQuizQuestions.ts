import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { axiosClient } from '@/services/api/axiosClient'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse } from '@/types'
import type { LessonQuizItem } from './useQuizItemMutations'

/**
 * Hook reorder hàng loạt câu hỏi trong quiz của 1 lesson (drag-drop / nút ↑↓).
 *
 * <p><b>BE contract:</b></p>
 * <ul>
 *   <li>Endpoint: {@code POST /lessons/{id}/quiz/questions/reorder}</li>
 *   <li>Body: {@code { itemIds: number[] }} (theo thứ tự MỚI)</li>
 *   <li>Yêu cầu: danh sách phải chứa <b>đúng tất cả</b> active question hiện tại
 *       (không thiếu, không thừa, không trùng). Nếu sai → 400.</li>
 *   <li>Quyền: ADMIN hoặc TEACHER (chỉ trong course do mình tạo).</li>
 * </ul>
 *
 * <p><b>Client contract:</b></p>
 * <ul>
 *   <li>Chỉ truyền active question IDs (đã loại bỏ câu đã soft-delete).</li>
 *   <li>Sắp xếp theo thứ tự MỚI mong muốn; vị trí 0 = orderIndex 1.</li>
 * </ul>
 */
export function useReorderQuizQuestions(lessonId: number) {
  const qc = useQueryClient()
  return useMutation<
    ApiResponse<LessonQuizItem[]>,
    AxiosError<ApiResponse<unknown>>,
    number[]
  >({
    mutationFn: async (questionIds: number[]) => {
      const res = await axiosClient.post<ApiResponse<LessonQuizItem[]>>(
        `/lessons/${lessonId}/quiz/questions/reorder`,
        { itemIds: questionIds },
      )
      return res.data
    },
    onSuccess: () => {
      toast.success('Đã cập nhật thứ tự câu hỏi')
      // Invalidate cả 2 endpoint quiz (học viên + teacher) + lesson detail
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.quiz(lessonId) })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.quizManage(lessonId) })
      qc.refetchQueries({ queryKey: QUERY_KEYS.lesson.quiz(lessonId) })
      qc.refetchQueries({ queryKey: QUERY_KEYS.lesson.quizManage(lessonId) })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      const msg =
        error.response?.data?.message ??
        'Không thể sắp xếp lại. Vui lòng thử lại.'
      toast.error('Sắp xếp thất bại', { description: msg })
    },
  })
}
