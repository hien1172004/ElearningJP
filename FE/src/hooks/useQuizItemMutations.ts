import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { AxiosError } from 'axios'
import { axiosClient } from '@/services/api/axiosClient'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, QuizQuestionType } from '@/types'

// ============================================================================
// Types (inline — không phụ thuộc file quiz-types đã bị xoá)
// ============================================================================
/** 1 option trong câu hỏi trắc nghiệm hoặc matching. */
interface QuizOptionPayload {
  optionKey: number
  optionText: string
  correct?: boolean
  matchKey?: string
}

// ============================================================================
// MATCHING — khớp với BE (BE lưu dạng {left[], right[]} trong JsonNode)
// ============================================================================
/** Cấu trúc mỗi mục bên trái/phải. */
interface MatchingSide {
  key: string
  label: string
}
/**
 * Cấu trúc matchingData BE đang lưu + FE phải gửi.
 * Lưu ý: BE không lưu "đáp án đúng" — đáp án đúng suy ra từ index
 * (left[i] ↔ right[i]). BE tự sinh options từ cặp (left[i] + " = " + right[i])
 * nhưng FE có thể gửi kèm options để chắc chắn.
 */
interface MatchingData {
  left: MatchingSide[]
  right: MatchingSide[]
}

/** Payload gửi lên BE khi tạo mới 1 câu hỏi quiz. */
export interface LessonQuizItemCreateRequest {
  questionText: string
  questionType?: QuizQuestionType
  explanation?: string
  orderIndex?: number
  /** FILL_IN_THE_BLANK */
  correctTextAnswer?: string
  /** MATCHING — mỗi cặp {leftKey,leftLabel,rightKey,rightLabel} ghép đúng. */
  matchingData?: MatchingData
  /** MCQ hoặc MATCHING (dùng để shuffle nếu cần) */
  options?: QuizOptionPayload[]
}

/**
 * Payload gửi lên BE khi cập nhật 1 câu hỏi quiz.
 * BE (PUT /quiz/questions/{id}) hỗ trợ partial update, nhưng FE hiện tại
 * gửi nguyên payload Create để đơn giản.
 */
export interface LessonQuizItemUpdateRequest {
  questionText?: string
  questionType?: QuizQuestionType
  explanation?: string | null
  orderIndex?: number
  correctTextAnswer?: string | null
  matchingData?: MatchingData | null
  options?: QuizOptionPayload[]
  vocabId?: number | null
  characterId?: number | null
}

/** Câu hỏi trả về từ BE. */
export interface LessonQuizItem {
  id: number
  questionText: string
  orderIndex?: number
  questionType?: QuizQuestionType
  explanation?: string
  correctTextAnswer?: string
  matchingData?: MatchingData
  options?: QuizOptionPayload[]
}

// ============================================================================
// Helpers
// ============================================================================
function getErrorMessage(err: unknown, fallback: string): string {
  return (err as AxiosError<ApiResponse<unknown>>)?.response?.data?.message ?? fallback
}

/**
 * Invalidate toàn bộ cache liên quan đến lesson và quiz.
 * Khi CRUD câu hỏi, cả 2 endpoint đều cần refetch:
 *   - /lessons/{id}/quiz          (học viên, ẩn đáp án)
 *   - /lessons/{id}/quiz/manage   (admin/teacher, có đáp án)
 * Ngoài ra còn refetch /lessons/{id} vì `hasQuiz` / `totalQuestions` thay đổi.
 */
function invalidateLesson(qc: ReturnType<typeof useQueryClient>, lessonId: number) {
  // Invalidate cả 2 cache quiz
  qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.quiz(lessonId) })
  qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.quizManage(lessonId) })
  // Invalidate detail lesson (totalQuestions có thể thay đổi)
  qc.invalidateQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
  // Force refetch detail ngay để UI cập nhật `hasQuiz` mà không cần chờ staleTime
  qc.refetchQueries({ queryKey: QUERY_KEYS.lesson.detail(lessonId) })
}

/**
 * Cập nhật cache cho cả 2 endpoint (/quiz và /quiz/manage) khi CRUD câu hỏi.
 * Vì 2 response có shape gần giống nhau, ta dùng chung 1 helper để tránh
 * cache bị stale sau khi thêm/sửa/xoá.
 */
function applyToAllQuizCaches(
  qc: ReturnType<typeof useQueryClient>,
  lessonId: number,
  updater: (old: any) => any,
) {
  const keys = [
    QUERY_KEYS.lesson.quiz(lessonId),
    QUERY_KEYS.lesson.quizManage(lessonId),
  ]
  for (const key of keys) {
    qc.setQueryData<any>(key, (old: any) => (old ? updater(old) : old))
  }
}

// ============================================================================
// QUIZ ITEM mutations (gọi axios trực tiếp)
// ============================================================================
/**
 * Hook CRUD câu hỏi quiz trong lesson (1-1 với lesson).
 *
 * - createQuizItem: thêm câu hỏi mới (BE tự tạo quiz nếu chưa có).
 *   Endpoint: POST /lessons/{id}/quiz/questions
 * - updateQuizItem: cập nhật câu hỏi (hỗ trợ partial update + di chuyển orderIndex).
 *   Endpoint: PUT /quiz/questions/{id}
 * - deleteQuizItem: xoá mềm câu hỏi.
 *   Endpoint: DELETE /quiz/questions/{id}
 */
export function useQuizItemMutations(lessonId: number) {
  const qc = useQueryClient()
  const invalidate = () => invalidateLesson(qc, lessonId)

  /**
   * Thêm 1 câu hỏi vào quiz.
   */
  const createQuizItem = useMutation({
    mutationFn: async (payload: LessonQuizItemCreateRequest) => {
      // axiosClient interceptor trả về response.data rồi → kết quả là ApiResponse<LessonQuizItem>
      return axiosClient.post<ApiResponse<LessonQuizItem>>(
        `/lessons/${lessonId}/quiz/questions`,
        payload,
      )
    },
    onSuccess: (res: any) => {
      toast.success('Đã thêm câu hỏi')
      // CHỈ invalidate, KHÔNG optimistic applyToAllQuizCaches.
      // Lý do: response từ POST là LessonQuizItem (không có đáp án đúng
      // như correctMatchingAnswers). Nếu merge vào cache /quiz/manage
      // → cache tạm thời thiếu đáp án, teacher review hiển thị sai.
      // Invalidate + refetch sẽ lấy lại data tươi từ BE.
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Thêm câu hỏi thất bại', {
        description: getErrorMessage(e, 'Lỗi không xác định'),
      })
    },
  })

  /**
   * Sửa câu hỏi: gọi PUT /quiz/questions/{id}.
   *
   * BE hỗ trợ partial update: chỉ field nào gửi mới thay đổi, options nếu
   * gửi sẽ THAY THẾ toàn bộ options cũ.
   */
  const updateQuizItem = useMutation<
    LessonQuizItem,
    Error,
    {
      id: number
      orderIndex?: number
      payload: LessonQuizItemCreateRequest
    }
  >({
    mutationFn: async ({ id, orderIndex, payload }) => {
      // Map từ CreateRequest → UpdateRequest.
      // - options: luôn gửi (BE sẽ thay thế).
      // - vocabId/characterId: không có ở CreateRequest nên bỏ qua.
      const updatePayload: LessonQuizItemUpdateRequest = {
        questionText: payload.questionText,
        questionType: payload.questionType,
        explanation: payload.explanation,
        orderIndex: orderIndex ?? payload.orderIndex,
        correctTextAnswer: payload.correctTextAnswer,
        matchingData: payload.matchingData,
        options: payload.options,
      }
      const res: any = await axiosClient.put<ApiResponse<LessonQuizItem>>(
        `/quiz/questions/${id}`,
        updatePayload,
      )
      const updated: LessonQuizItem | undefined = res?.data
      if (!updated?.id) {
        throw new Error('BE không trả về câu hỏi sau khi cập nhật')
      }
      return updated
    },
    onSuccess: (updated) => {
      toast.success('Đã cập nhật câu hỏi')
      // CHỈ invalidate, KHÔNG applyToAllQuizCaches.
      // Lý do: response từ PUT /quiz/questions/{id} là LessonQuizItem
      // (không có correctMatchingAnswers / correctTextAnswer). Nếu merge
      // { ...q, ...updated } sẽ XOÁ các field đáp án đúng khỏi cache
      // /quiz/manage → teacher review hiển thị sai. Invalidate + refetch
      // sẽ lấy lại data tươi từ BE (đã bao gồm đáp án đúng).
      invalidate()
    },
    onError: (e: unknown) => {
      const axErr = e as AxiosError<ApiResponse<unknown>>
      toast.error('Cập nhật câu hỏi thất bại', {
        description: getErrorMessage(axErr, 'Lỗi không xác định'),
      })
    },
  })

  /**
   * Xoá mềm 1 câu hỏi.
   */
  const deleteQuizItem = useMutation({
    mutationFn: async (questionId: number) =>
      axiosClient.delete<ApiResponse<void>>(`/quiz/questions/${questionId}`),
    onSuccess: (_res, id) => {
      toast.success('Đã xoá câu hỏi')
      // Xoá trong cả 2 cache /quiz và /quiz/manage
      applyToAllQuizCaches(qc, lessonId, (old) => {
        const questions = (old.questions ?? []).filter(
          (q: LessonQuizItem) => q.id !== id,
        )
        return {
          ...old,
          questions,
          totalQuestions: questions.length,
        }
      })
      invalidate()
    },
    onError: (e: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Xoá câu hỏi thất bại', {
        description: getErrorMessage(e, 'Lỗi không xác định'),
      })
    },
  })

  return { createQuizItem, updateQuizItem, deleteQuizItem }
}
