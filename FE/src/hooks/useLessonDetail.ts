import { useQuery } from '@tanstack/react-query'
import { lessonService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { LessonDetail } from '@/types'

export function useLessonDetail(id: number | string | undefined) {
  const numericId = typeof id === 'string' ? Number(id) : (id as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<LessonDetail | null>({
    queryKey: enabled
      ? QUERY_KEYS.lesson.detail(safeId)
      : ['lessons', 'detail', 'disabled'],
    queryFn: async () => {
      const res = await lessonService.getLessonDetail(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 60 * 1000,
    // Không dùng retry mặc định (3 lần) cho các request authenticated
    // vì nếu token hết hạn, retry sẽ fail liên tục.
    retry: (failureCount, error: any) => {
      // Chỉ retry nếu không phải lỗi auth/permission
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) {
        return false
      }
      return failureCount < 2
    },
  })
}
