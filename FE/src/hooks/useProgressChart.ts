import { useQuery } from '@tanstack/react-query'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ChartGranularity, ProgressChartResponse } from '@/types'

export interface UseProgressChartParams {
  /** Ngày bắt đầu `yyyy-MM-dd`. undefined → BE tự default 30 ngày gần nhất. */
  from?: string
  /** Ngày kết thúc `yyyy-MM-dd`. undefined → hôm nay. */
  to?: string
  /**
   * Đơn vị bucket: `day` | `week` | `month`.
   *
   * <p>BE tự gộp bucket — FE chỉ cần truyền để BE gộp đúng và để format
   * nhãn trục X khớp với dữ liệu.</p>
   */
  granularity?: ChartGranularity
}

/**
 * useProgressChart — dữ liệu biểu đồ tiến độ (Khối 3).
 *
 * <p>Trả về 2 series: số từ học mỗi bucket (cột) và số từ học tích lũy
 * (đường, cộng dồn từ 0 trong kỳ).</p>
 *
 * ```tsx
 * const { data, isLoading } = useProgressChart({ from, to, granularity: 'week' })
 * ```
 *
 * @param params khoảng thời gian + đơn vị bucket
 * @param options bật/tắt query (mặc định `true`)
 */
export function useProgressChart(
  params: UseProgressChartParams = {},
  options?: { enabled?: boolean },
) {
  const { from, to, granularity } = params

  return useQuery<ProgressChartResponse>({
    queryKey: QUERY_KEYS.progress.chart({ from, to, granularity }),
    queryFn: async () => {
      const res = await progressService.getChart({ from, to, granularity })
      return (res?.data ?? {
        from: '',
        to: '',
        granularity: 'day' as const,
        totalWords: 0,
        points: [],
      }) as ProgressChartResponse
    },
    enabled: options?.enabled ?? true,
    // Biểu đồ đổi liên tục khi user học → luôn refetch khi mount lại trang
    // (giống hành vi của useProgressSummary ở Khối 1).
    staleTime: 0,
    refetchOnMount: 'always',
    retry: 1,
  })
}
