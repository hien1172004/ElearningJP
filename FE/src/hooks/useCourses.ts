import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { courseService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { JlptLevel, PageResponse, CourseSummary } from '@/types'

interface UseCoursesParams {
  jlptLevel?: JlptLevel
  page?: number
  size?: number
  /** Nếu true -> lấy tất cả cho admin/teacher. */
  admin?: boolean
  /** (admin) Chỉ khóa có thay đổi chưa duyệt. */
  pendingChanges?: boolean
  /** (admin) Tìm theo tên khóa học. */
  keyword?: string
  /** (admin) true = đã xuất bản, false = bản nháp. */
  published?: boolean
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
            pendingChanges: params.pendingChanges || undefined,
            keyword: params.keyword || undefined,
            jlptLevel: params.jlptLevel,
            published: params.published,
          })
        : await courseService.getCourses({
            jlptLevel: params.jlptLevel,
            page: params.page ?? 1,
            size: params.size ?? 12,
          })
      return res.data ?? { page: 1, size: 10, totalElements: 0, totalPages: 0, items: [] }
    },
    staleTime: 5 * 60 * 1000,
    // Trang quản lý: giữ dữ liệu cũ khi đổi trang / bộ lọc để bảng không nháy trống
    placeholderData: params.admin ? keepPreviousData : undefined,
  })
}
