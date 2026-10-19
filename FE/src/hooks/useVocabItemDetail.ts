import { useQuery } from '@tanstack/react-query'
import { vocabItemService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonVocabularyItem } from '@/types'

/**
 * Lấy chi tiết đầy đủ của 1 LessonVocabularyItem từ endpoint
 *   GET /api/v1/lesson-vocabulary-items/vocabulary-items/{id}
 * Endpoint này trả về examples đầy đủ (cả teacher-override + auto-derive),
 * thường được dùng làm fallback khi response tổng từ /lessons/{id} bị thiếu examples.
 */
export function useVocabItemDetail(id: number | string | undefined) {
  const numericId = typeof id === 'string' ? Number(id) : (id as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<LessonVocabularyItem | null>({
    queryKey: enabled
      ? QUERY_KEYS.lesson.vocabItem(safeId)
      : ['lessons', 'vocab-item', 'disabled'],
    queryFn: async () => {
      const res = await vocabItemService.getItemDetail(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 30 * 1000,
    refetchOnMount: true,
    refetchOnWindowFocus: false,
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) return false
      return failureCount < 2
    },
  })
}
