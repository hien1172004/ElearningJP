import { useQuery } from '@tanstack/react-query'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { SrsDueItem } from '@/types'

/**
 * useSrsDue — hàng đợi thẻ SRS đến hạn ôn tập.
 *
 * `staleTime: 0` + `refetchOnMount: 'always'` vì hàng đợi thay đổi liên tục
 * (mỗi lần ôn xong 1 thẻ là thẻ khác trượt lên thế chỗ).
 */
export function useSrsDue(limit = 20, options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<SrsDueItem[]>({
    queryKey: QUERY_KEYS.srs.due({ limit }),
    queryFn: async () => {
      const res = await srsService.getDueCards(limit)
      return (res?.data ?? []) as SrsDueItem[]
    },
    enabled: isAuthenticated && (options?.enabled ?? true),
    staleTime: 0,
    refetchOnMount: 'always',
    retry: 1,
  })
}
