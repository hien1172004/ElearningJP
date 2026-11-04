import { axiosClient } from './api/axiosClient'
import type { AdminDashboardOverview, ApiResponse } from '@/types'

/** API thống kê tổng quan (chỉ ADMIN) — BE: AdminDashboardController `/admin/dashboard`. */
export const adminDashboardService = {
  getOverview: async (days: number): Promise<ApiResponse<AdminDashboardOverview>> => {
    return axiosClient.get('/admin/dashboard/overview', { params: { days } })
  },
}
