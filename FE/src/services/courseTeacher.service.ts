import { axiosClient } from './api/axiosClient'
import type { ApiResponse, CourseTeacher } from '@/types'

/** Phân công giáo viên — BE: CourseTeacherController `/courses/{courseId}/teachers`. */
export const courseTeacherService = {
  getTeachers: async (courseId: number): Promise<ApiResponse<CourseTeacher[]>> => {
    return axiosClient.get(`/courses/${courseId}/teachers`)
  },

  assignTeacher: async (courseId: number, teacherId: number): Promise<ApiResponse<CourseTeacher>> => {
    return axiosClient.post(`/courses/${courseId}/teachers`, { teacherId })
  },

  unassignTeacher: async (courseId: number, teacherId: number): Promise<ApiResponse<void>> => {
    return axiosClient.delete(`/courses/${courseId}/teachers/${teacherId}`)
  },
}
