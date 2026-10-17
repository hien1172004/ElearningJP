import { useQuery } from '@tanstack/react-query'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { JlptLevel, PageResponse, CourseSummary } from '@/types'

interface UseCoursesParams {
  jlptLevel?: JlptLevel
  page?: number
  size?: number
  /** Nếu true -> lấy tất cả cho admin/teacher. */
  admin?: boolean
  [key: string]: unknown
}

export function useCourses(params: UseCoursesParams = {}) {
  return useQuery<PageResponse<CourseSummary>>({
    queryKey: params.admin
      ? QUERY_KEYS.course.list({ ...params, admin: true })
      : QUERY_KEYS.course.list(params),
    queryFn: async () => {
      const res = params.admin
        ? await courseService.getAllCoursesAdmin({
            page: params.page ?? 1,
            size: params.size ?? 10,
          })
        : await courseService.getCourses({
            jlptLevel: params.jlptLevel,
            page: params.page ?? 1,
            size: params.size ?? 12,
          })
      return res.data ?? { page: 1, size: 10, totalElements: 0, totalPages: 0, items: [] }
    },
    staleTime: 5 * 60 * 1000,
  })
}
