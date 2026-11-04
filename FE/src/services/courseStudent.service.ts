import { axiosClient } from './api/axiosClient'
import type {
  ApiResponse,
  CourseStudents,
  CourseStudentsParams,
  StudentCourseDetail,
} from '@/types'

/** Theo dõi học viên — BE: CourseStudentController `/teacher/courses/{courseId}/students`. */
export const courseStudentService = {
  getStudents: async (
    courseId: number,
    params: CourseStudentsParams,
  ): Promise<ApiResponse<CourseStudents>> => {
    return axiosClient.get(`/teacher/courses/${courseId}/students`, { params })
  },

  getStudentDetail: async (
    courseId: number,
    userId: number,
  ): Promise<ApiResponse<StudentCourseDetail>> => {
    return axiosClient.get(`/teacher/courses/${courseId}/students/${userId}`)
  },
}
