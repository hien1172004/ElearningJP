import { useCallback, useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { SrsSavedIds } from '@/types'

export type SrsItemTargetType = 'VOCABULARY' | 'KANJI' | 'GRAMMAR'

/**
 * useSrsSavedIds — nạp tập hợp ID các từ vựng, chữ Hán, ngữ pháp mà học viên đã lưu vào SRS.
 * Giúp các nút "Lưu SRS" trên toàn bộ từ điển đồng bộ trạng thái chính xác, không bị reset khi chuyển trang.
 */
export function useSrsSavedIds() {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  const { data, isLoading, refetch } = useQuery<SrsSavedIds>({
    queryKey: QUERY_KEYS.srs.savedIds,
    queryFn: async () => {
      const res = await srsService.getSavedIds()
      return res?.data ?? { vocabIds: [], characterIds: [], grammarIds: [] }
    },
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  })

  const vocabSet = useMemo(() => new Set(data?.vocabIds ?? []), [data?.vocabIds])
  const characterSet = useMemo(() => new Set(data?.characterIds ?? []), [data?.characterIds])
  const grammarSet = useMemo(() => new Set(data?.grammarIds ?? []), [data?.grammarIds])

  const isSaved = useCallback(
    (type: SrsItemTargetType, id: number | undefined | null): boolean => {
      if (id == null) return false
      if (type === 'VOCABULARY') return vocabSet.has(id)
      if (type === 'KANJI') return characterSet.has(id)
      if (type === 'GRAMMAR') return grammarSet.has(id)
      return false
    },
    [vocabSet, characterSet, grammarSet],
  )

  return { isSaved, isLoading, refetch }
}