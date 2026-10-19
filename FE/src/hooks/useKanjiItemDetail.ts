import { useQuery } from '@tanstack/react-query'
import { kanjiItemService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonKanjiItem } from '@/types'

/**
 * Lấy chi tiết đầy đủ của 1 LessonKanjiItem từ endpoint
 *   GET /api/v1/lesson-kanji-items/kanji-items/{id}
 * Endpoint này trả về relatedVocabulary đầy đủ (cả teacher-override + auto-derive),
 * thường được dùng làm fallback khi response tổng từ /lessons/{id} bị thiếu relatedVocabulary.
 */
export function useKanjiItemDetail(id: number | string | undefined) {
  const numericId = typeof id === 'string' ? Number(id) : (id as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<LessonKanjiItem | null>({
    queryKey: enabled
      ? QUERY_KEYS.lesson.kanjiItem(safeId)
      : ['lessons', 'kanji-item', 'disabled'],
    queryFn: async () => {
      const res = await kanjiItemService.getItemDetail(safeId)
      return res.data ?? null
    },
    enabled,
    // Ngắn vì mỗi lần mở modal sau mutation muốn thấy data tươi.
    // setQueryData trong useSectionMutations sẽ tự cập nhật cache
    // nên khi re-mount không cần refetch lại nếu cache còn fresh.
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
