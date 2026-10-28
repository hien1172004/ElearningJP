import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LeaderboardResponse, MyRank } from '@/types'

/**
 * useLeaderboard — dữ liệu bảng xếp hạng tuần (Khối 5).
 *
 * <p>Gồm 3 hook:</p>
 * <ul>
 *   <li>{@link useLeaderboard} — bảng top 5 của 1 tuần</li>
 *   <li>{@link useMyRank} — hạng cá nhân của user hiện tại</li>
 *   <li>{@link useRefreshLeaderboard} — mutation nút "Reload"</li>
 * </ul>
 */

/**
 * Bảng xếp hạng của 1 tuần.
 *
 * @param week tuần ISO `YYYY-'W'ww`. `undefined` → tuần hiện tại (BE tự resolve).
 */
export function useLeaderboard(week?: string, options?: { enabled?: boolean }) {
  return useQuery<LeaderboardResponse>({
    queryKey: QUERY_KEYS.progress.leaderboard(week),
    queryFn: async () => {
      const res = await progressService.getLeaderboard(week)
      return res?.data as LeaderboardResponse
    },
    enabled: options?.enabled ?? true,
    // Bảng xếp hạng là dữ liệu gần như read-only trong 1 tuần → cache ngắn.
    // 30s đủ để chuyển qua lại giữa các tab mà không gọi lại BE.
    staleTime: 30_000,
    retry: 1,
  })
}

/**
 * Hạng cá nhân của user hiện tại trong tuần.
 *
 * <p>Tách khỏi {@link useLeaderboard} vì dữ liệu này thuộc về **1 user** — không
 * phụ thuộc user có nằm trong top 5 hay không.</p>
 */
export function useMyRank(week?: string, options?: { enabled?: boolean }) {
  return useQuery<MyRank>({
    queryKey: QUERY_KEYS.progress.myRank(week),
    queryFn: async () => {
      const res = await progressService.getMyRank(week)
      return res?.data as MyRank
    },
    enabled: options?.enabled ?? true,
    staleTime: 30_000,
    retry: 1,
  })
}

/**
 * Mutation nút "Reload" — bù cho trường hợp Redis ZSET bị lỡ.
 *
 * <p><b>Không sửa điểm giảm</b> — BE dùng `ZADD GT` (chỉ nâng điểm). Muốn sửa
 * điểm giảm → endpoint `refresh/hard` (chỉ ADMIN).</p>
 *
 * <p><b>Cooldown 30s ở BE</b> → gọi lại trong 30s sẽ nhận HTTP 429. Component
 * nên kiểm tra {@link AxiosError.response?.status === 429} để hiện thông báo
 * phù hợp thay vì lỗi chung.</p>
 */
export function useRefreshLeaderboard() {
  const queryClient = useQueryClient()

  return useMutation<
    LeaderboardResponse,
    AxiosError,
    { week?: string }
  >({
    mutationFn: async ({ week }) => {
      const res = await progressService.refreshLeaderboard(week)
      return res?.data as LeaderboardResponse
    },
    onSuccess: (_data, { week }) => {
      // Invalidate cả bảng lẫn hạng cá nhân — cả hai đều có thể đổi sau refresh.
      void queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.progress.leaderboard(week),
      })
      void queryClient.invalidateQueries({
        queryKey: QUERY_KEYS.progress.myRank(week),
      })
    },
  })
}
