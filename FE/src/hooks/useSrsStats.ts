import { useQuery } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { SrsStats } from '@/types'

const EMPTY_STATS: SrsStats = {
  totalItems: 0,
  dueTodayCount: 0,
  newTodayCount: 0,
  newCount: 0,
  learningCount: 0,
  reviewingCount: 0,
  relearningCount: 0,
  masteredCount: 0,
  archivedCount: 0,
}

/**
 * useSrsStats — thống kê SRS cá nhân (tổng / đến hạn / đang học / đang ôn / thành thạo).
 */
export function useSrsStats(options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<SrsStats>({
    queryKey: QUERY_KEYS.srs.stats,
    queryFn: async () => {
      const res = await srsService.getStats()
      return (res?.data ?? EMPTY_STATS) as SrsStats
    },
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 30_000,
    retry: 1,
  })
}
