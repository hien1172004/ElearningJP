import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  handwritingService,
  type HandwritingPracticeLogPayload,
  type HandwritingPracticeLogResult,
  type HandwritingPracticeSummary,
} from '@/services/handwriting.service'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'

/** Tổng hợp điểm luyện viết của tôi cho 1 chữ (chỉ gọi khi đã đăng nhập). */
export function useHandwritingSummary(character?: string, lessonId?: number, options?: { enabled?: boolean }) {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  return useQuery<HandwritingPracticeSummary | null>({
    queryKey: QUERY_KEYS.handwriting.summary(character ?? '', lessonId),
    queryFn: async () => {
      const res = await handwritingService.getSummary(character!, lessonId)
      return res?.data ?? null
    },
    enabled: isAuthenticated && !!character && (options?.enabled ?? true),
    staleTime: 30_000,
    retry: 1,
  })
}

/**
 * Lưu kết quả một lần luyện viết. Khách (chưa đăng nhập) vẫn luyện bình thường, chỉ không lưu.
 * Sau khi lưu: ghi thẳng tổng hợp mới vào cache và invalidate prefix `handwriting`.
 */
export function useSaveHandwritingPractice() {
  const queryClient = useQueryClient()
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  const mutation = useMutation<HandwritingPracticeLogResult | null, unknown, HandwritingPracticeLogPayload>({
    mutationFn: async (payload) => {
      const res = await handwritingService.saveLog(payload)
      return res?.data ?? null
    },
    onSuccess: (data, payload) => {
      if (data?.summary) {
        queryClient.setQueryData(
          QUERY_KEYS.handwriting.summary(payload.character, payload.lessonId),
          data.summary,
        )
      }
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.handwriting.all })
    },
  })

  return { ...mutation, canSave: isAuthenticated }
}
