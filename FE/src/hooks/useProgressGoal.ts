import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AxiosError } from 'axios'
import { toast } from 'sonner'
import { progressService } from '@/services/progress.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, ProgressGoal } from '@/types'

/** Mục tiêu số từ mới mỗi tuần của user hiện tại. */
export function useProgressGoal() {
  return useQuery<ProgressGoal>({
    queryKey: QUERY_KEYS.progress.goal,
    queryFn: async () => {
      const res = await progressService.getGoal()
      return res?.data as ProgressGoal
    },
    retry: 1,
  })
}

/** Lưu mục tiêu, rồi làm mới các khối đang hiển thị mục tiêu. */
export function useUpdateProgressGoal() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: async (weeklyGoalWords: number) => {
      const res = await progressService.updateGoal(weeklyGoalWords)
      return res?.data as ProgressGoal
    },
    onSuccess: (goal) => {
      // Ghi thẳng vào cache: ProgressPage đọc ngay sau khi điều hướng về,
      // nếu còn dữ liệu cũ (configured = false) sẽ bị đẩy lại trang chọn mục tiêu.
      qc.setQueryData(QUERY_KEYS.progress.goal, goal)
      qc.invalidateQueries({ queryKey: QUERY_KEYS.progress.summaryAll })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.progress.calendarAll })
      toast.success('Đã lưu mục tiêu học tập')
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error(
        error.response?.data?.message ?? 'Không thể lưu mục tiêu. Vui lòng thử lại.',
      )
    },
  })
}
