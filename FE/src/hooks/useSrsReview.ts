import { useMutation, useQueryClient } from '@tanstack/react-query'
import type { AxiosError } from 'axios'
import { srsService } from '@/services/srs.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { SrsReviewRequest, SrsReviewResult } from '@/types'

export interface SrsReviewVariables {
  id: number
  payload: SrsReviewRequest
}

/**
 * useSrsReview — nộp kết quả lật thẻ (Again/Hard/Good/Easy).
 *
 * Sau khi ôn xong, invalidate `srs.stats` để thống kê & badge số thẻ đến hạn
 * được làm mới. Hàng đợi ôn tập do trang tự quản lý cục bộ (không cần refetch).
 */
export function useSrsReview() {
  const queryClient = useQueryClient()

  return useMutation<SrsReviewResult, AxiosError, SrsReviewVariables>({
    mutationFn: async ({ id, payload }) => {
      const res = await srsService.reviewCard(id, payload)
      return (res?.data ?? ({} as SrsReviewResult)) as SrsReviewResult
    },
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.srs.stats })
      void queryClient.invalidateQueries({ queryKey: QUERY_KEYS.srs.forecast })
    },
  })
}
