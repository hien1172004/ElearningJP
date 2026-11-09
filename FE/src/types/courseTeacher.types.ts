/** Giáo viên được giao khóa học — BE: CourseTeacherResponse. */
export interface CourseTeacher {
  teacherId: number
  fullName?: string | null
  email: string
  avatarUrl?: string | null
  /** Thời điểm được giao (ISO). */
  assignedAt?: string | null
}
