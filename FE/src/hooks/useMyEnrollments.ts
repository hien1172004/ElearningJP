import { useQuery } from '@tanstack/react-query'
import { enrollmentService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import { useAuthStore } from '@/stores/useAuthStore'
import type { Enrollment } from '@/types'

export function useMyEnrollments(activeOnly = false) {
  // Khách chưa đăng nhập không có lượt đăng ký — không gọi API để tránh 401
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  return useQuery<Enrollment[]>({
    queryKey: QUERY_KEYS.enrollment.mine(activeOnly),
    queryFn: async () => {
      const res = activeOnly
        ? await enrollmentService.getMyActiveEnrollments()
        : await enrollmentService.getMyEnrollments()
      return (res.data as Enrollment[] | undefined) ?? []
    },
    enabled: isAuthenticated,
    staleTime: 60 * 1000,
  })
}
