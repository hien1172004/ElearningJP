import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ProgressSummary } from '@/types'

export interface UseProgressSummaryParams {
  /** Ngày bắt đầu `yyyy-MM-dd`. undefined → BE mặc định 30 ngày gần nhất. */
  from?: string
  /** Ngày kết thúc `yyyy-MM-dd`. undefined → hôm nay. */
  to?: string
}

/**
 * useProgressSummary — tải tổng hợp tiến độ học tập (Khối 1).
 *
 * ```tsx
 * const { data, isLoading, isError, refetch } = useProgressSummary()
 * ```
 *
 * @param options bật/tắt query (mặc định `true`)
 */
export function useProgressSummary(
  params: UseProgressSummaryParams = {},
  options?: { enabled?: boolean },
) {
  const { from, to } = params

  return useQuery<ProgressSummary>({
    queryKey: QUERY_KEYS.progress.summary({ from, to }),
    queryFn: async () => {
      const res = await progressService.getSummary({ from, to })
      return res?.data as ProgressSummary
    },
    enabled: options?.enabled ?? true,
    // Progress là dữ liệu "real-time-ish": mỗi lần tra từ / heartbeat đều đổi.
    // Không cache ở FE để khi user quay lại trang Progress luôn thấy số mới nhất.
    // BE đã tự lo phần cache phía server (nếu có).
    staleTime: 0,
    refetchOnMount: 'always',
    retry: 1,
  })
}