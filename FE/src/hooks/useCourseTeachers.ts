import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import type { AxiosError } from 'axios'
import { courseTeacherService } from '@/services'
import { QUERY_KEYS } from '@/lib/queryKeys'
import type { ApiResponse, CourseTeacher } from '@/types'

/** Giáo viên được giao của 1 khóa (ADMIN hoặc giáo viên được giao khóa đó). */
export function useCourseTeachers(courseId: number) {
  return useQuery<CourseTeacher[]>({
    queryKey: QUERY_KEYS.courseTeachers.list(courseId),
    queryFn: async () => {
      const res = await courseTeacherService.getTeachers(courseId)
      return res.data ?? []
    },
    enabled: courseId > 0,
  })
}

const errorMessage = (error: AxiosError<ApiResponse<unknown>>, fallback: string) =>
  error.response?.data?.message ?? fallback

/** ADMIN giao giáo viên cho khóa. */
export function useAssignCourseTeacher(courseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (teacherId: number) => courseTeacherService.assignTeacher(courseId, teacherId),
    onSuccess: (res) => {
      toast.success('Đã giao giáo viên cho khóa học', { description: res?.data?.fullName ?? res?.data?.email })
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseTeachers.list(courseId) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Giao giáo viên thất bại', {
        description: errorMessage(error, 'Không thể giao giáo viên. Vui lòng thử lại.'),
      })
    },
  })
}

/** ADMIN gỡ giáo viên khỏi khóa. */
export function useUnassignCourseTeacher(courseId: number) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (teacherId: number) => courseTeacherService.unassignTeacher(courseId, teacherId),
    onSuccess: () => {
      toast.success('Đã gỡ giáo viên khỏi khóa học')
      qc.invalidateQueries({ queryKey: QUERY_KEYS.courseTeachers.list(courseId) })
    },
    onError: (error: AxiosError<ApiResponse<unknown>>) => {
      toast.error('Gỡ giáo viên thất bại', {
        description: errorMessage(error, 'Không thể gỡ giáo viên. Vui lòng thử lại.'),
      })
    },
  })
}
