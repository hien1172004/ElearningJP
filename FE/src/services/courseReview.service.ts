import { axiosClient } from './api/axiosClient'
import type { ApiResponse, CourseChanges } from '@/types'

/** Khóa học: ADMIN lưu cả khóa + thay đổi so với bản học viên — BE: CourseReviewController / CourseChangesController. */
export const courseReviewService = {
  /** ADMIN lưu nháp ADMIN của mọi bài (kèm đề thi gắn khóa) thành phiên bản mới (không qua duyệt, không bật xuất bản). */
  saveVersion: async (courseId: number): Promise<ApiResponse<null>> => {
    return axiosClient.post(`/courses/${courseId}/versions`)
  },

  /** Thay đổi của bản đang soạn so với bản học viên đang học. */
  getChanges: async (courseId: number): Promise<ApiResponse<CourseChanges>> => {
    return axiosClient.get(`/courses/${courseId}/pending-changes`)
  },
}
