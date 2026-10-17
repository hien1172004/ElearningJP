import { useQuery } from '@tanstack/react-query'
import { enrollmentService } from '@/services'
import type { Enrollment } from '@/types'

export function useMyEnrollments(activeOnly = false) {
  return useQuery<Enrollment[]>({
    queryKey: ['enrollments', 'me', activeOnly ? 'active' : 'all'],
    queryFn: async () => {
      const res = activeOnly
        ? await enrollmentService.getMyActiveEnrollments()
        : await enrollmentService.getMyEnrollments()
      return (res.data as Enrollment[] | undefined) ?? []
    },
    staleTime: 60 * 1000,
  })
}
