import { useQuery } from '@tanstack/react-query'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { CourseDetail } from '@/types'

export function useCourseDetail(id: number | string | undefined) {
  const numericId = typeof id === 'string' ? Number(id) : (id as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<CourseDetail | null>({
    queryKey: enabled
      ? QUERY_KEYS.course.detail(safeId)
      : ['courses', 'detail', 'disabled'],
    queryFn: async () => {
      const res = await courseService.getCourseById(safeId)
      return res.data ?? null
    },
    enabled,
    staleTime: 5 * 60 * 1000,
    retry: (failureCount, error: any) => {
      const status = error?.response?.status
      if (status === 401 || status === 403 || status === 404) {
        return false
      }
      return failureCount < 2
    },
  })
}
