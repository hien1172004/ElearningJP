import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { TopLookup } from '@/types'

export interface UseTopLookupsParams {
  /**
   * Ngày bắt đầu `yyyy-MM-dd`.
   *
   * Bỏ trống (cùng `to`) → BE xếp hạng **toàn bộ lịch sử**.
   */
  from?: string
  /** Ngày kết thúc `yyyy-MM-dd`. undefined → hôm nay. */
  to?: string
  /** Số dòng tối đa. undefined → 30. */
  limit?: number
}

/**
 * useTopLookups — bảng "Từ đã tra nhiều nhất" (Khối 4).
 *
 * ```tsx
 * const { data, isLoading } = useTopLookups({ limit: 30 })
 * ```
 *
 * @param params khoảng thời gian (tùy chọn) + số dòng
 * @param options bật/tắt query (mặc định `true`)
 */
export function useTopLookups(
  params: UseTopLookupsParams = {},
  options?: { enabled?: boolean },
) {
  const { from, to, limit } = params

  return useQuery<TopLookup[]>({
    queryKey: QUERY_KEYS.progress.topLookups({ from, to, limit }),
    queryFn: async () => {
      const res = await progressService.getTopLookups({ from, to, limit })
      return (res?.data ?? []) as TopLookup[]
    },
    enabled: options?.enabled ?? true,
    staleTime: 60_000,
    retry: 1,
  })
}