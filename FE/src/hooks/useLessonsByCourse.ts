import { useQuery } from '@tanstack/react-query'
import { lessonService } from '@/services'
import type { LessonSummary } from '@/types'

export function useLessonsByCourse(courseId: number | string | undefined) {
  const numericId = typeof courseId === 'string' ? Number(courseId) : (courseId as number | undefined)
  const safeId = numericId ?? 0
  const enabled = numericId != null && !Number.isNaN(numericId) && numericId > 0

  return useQuery<LessonSummary[]>({
    queryKey: ['courses', safeId, 'lessons'],
    queryFn: async () => {
      const res = await lessonService.getLessonsByCourse(safeId)
      return (res.data as LessonSummary[] | undefined) ?? []
    },
    enabled,
    staleTime: 60 * 1000,
  })
}
