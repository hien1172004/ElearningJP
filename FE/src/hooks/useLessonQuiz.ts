import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { quizService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type {
  LessonQuizDetail,
  QuizSubmitRequest,
  QuizSubmitResponse,
} from '@/types'

export function useLessonQuiz(lessonId: number | string | undefined) {
  const numericId = typeof lessonId === 'string' ? Number(lessonId) : (lessonId as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<LessonQuizDetail | null>({
    queryKey: enabled
      ? QUERY_KEYS.lesson.quiz(safeId)
      : ['lessons', 'quiz', 'disabled'],
    queryFn: async () => {
      const res = await quizService.getLessonQuiz(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 60 * 1000,
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}

export function useSubmitQuiz(lessonId: number) {
  const qc = useQueryClient()
  return useMutation<QuizSubmitResponse, unknown, QuizSubmitRequest>({
    mutationFn: async (payload) => {
      const res = await quizService.submitQuiz(lessonId, payload)
      return (res.data as QuizSubmitResponse) ?? ({} as QuizSubmitResponse)
    },
    onSuccess: () => {
      // Invalidate related caches so progress UI refreshes
      qc.invalidateQueries({ queryKey: ['lessons', 'detail', lessonId] })
      qc.invalidateQueries({ queryKey: ['progress'] })
    },
  })
}
