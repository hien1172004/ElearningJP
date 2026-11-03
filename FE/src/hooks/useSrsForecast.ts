import { useQuery } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { SrsForecast } from '@/types'

/**
 * useSrsForecast — dự báo số thẻ SRS cần ôn trong hôm nay và 4 ngày tiếp theo.
 */
export function useSrsForecast(options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<SrsForecast>({
    queryKey: QUERY_KEYS.srs.forecast,
    queryFn: async () => {
      const res = await srsService.getForecast()
      return (res?.data ?? { days: [] }) as SrsForecast
    },
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 60_000,
    retry: 1,
  })
}
