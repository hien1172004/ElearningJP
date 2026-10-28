import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ProgressCalendar } from '@/types'

/**
 * useProgressCalendar — lịch học + streak theo tháng (Khối 2).
 *
 * ```tsx
 * const { data, isLoading } = useProgressCalendar('2026-10')
 * ```
 *
 * @param month tháng `yyyy-MM`. `undefined` → tháng hiện tại (BE tự resolve).
 * @param options bật/tắt query (mặc định `true`)
 */
export function useProgressCalendar(
  month?: string,
  options?: { enabled?: boolean },
) {
  return useQuery<ProgressCalendar>({
    queryKey: QUERY_KEYS.progress.calendar(month),
    queryFn: async () => {
      const res = await progressService.getCalendar(month)
      return res?.data as ProgressCalendar
    },
    enabled: options?.enabled ?? true,
    // Lịch tháng gần như không đổi trong suốt 1 ngày → cache dài hơn khối summary.
    // Người dùng vẫn thấy dữ liệu mới sau khi học vì trang Progress đã bật
    // `refetchOnMount: 'always'` ở tầng trên (invalidate khi rời trang khác).
    staleTime: 5 * 60_000,
    retry: 1,
  })
}