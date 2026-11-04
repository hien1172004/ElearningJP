import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { courseStudentService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { CourseStudents, CourseStudentsParams, StudentCourseDetail } from '@/types'

/** Học viên + tiến độ của 1 khóa (tắt khi chưa chọn khóa). */
export function useCourseStudents(courseId: number | null, params: CourseStudentsParams) {
  return useQuery<CourseStudents | null>({
    queryKey: courseId
      ? QUERY_KEYS.courseStudents.list(courseId, { ...params })
      : QUERY_KEYS.courseStudents.disabled,
    queryFn: async () => {
      if (!courseId) return null
      const res = await courseStudentService.getStudents(courseId, params)
      return res.data ?? null
    },
    enabled: !!courseId,
    placeholderData: keepPreviousData,
  })
}

/** Tiến độ từng bài của 1 học viên (tắt khi chưa chọn học viên). */
export function useStudentCourseDetail(courseId: number | null, userId: number | null) {
  return useQuery<StudentCourseDetail | null>({
    queryKey:
      courseId && userId
        ? QUERY_KEYS.courseStudents.detail(courseId, userId)
        : QUERY_KEYS.courseStudents.disabled,
    queryFn: async () => {
      if (!courseId || !userId) return null
      const res = await courseStudentService.getStudentDetail(courseId, userId)
      return res.data ?? null
    },
    enabled: !!courseId && !!userId,
  })
}
