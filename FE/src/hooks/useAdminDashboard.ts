import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { adminDashboardService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { AdminDashboardOverview, DashboardRangeDays } from '@/types'

/**
 * Số liệu tổng quan cho dashboard quản trị.
 * staleTime 2 phút: số liệu tổng hợp, không cần realtime; giữ dữ liệu cũ khi đổi khoảng thời gian.
 */
export function useAdminDashboard(days: DashboardRangeDays) {
  return useQuery<AdminDashboardOverview | null>({
    queryKey: QUERY_KEYS.adminDashboard.overview(days),
    queryFn: async () => {
      const res = await adminDashboardService.getOverview(days)
      return res.data ?? null
    },
    staleTime: 2 * 60 * 1000,
    placeholderData: keepPreviousData,
  })
}
