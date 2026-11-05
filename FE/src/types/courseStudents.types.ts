import type { PageResponse } from './api.types'

export type StudentSort = 'LAST_ACTIVE' | 'PROGRESS' | 'NAME' | 'ENROLLED'
export type LessonProgressStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETED'

/** Khớp `CourseStudentsResponse.StudentProgress` ở BE. */
export interface StudentProgress {
  userId: number
  fullName?: string | null
  email?: string | null
  avatarUrl?: string | null
  enrollmentStatus?: 'ACTIVE' | 'COMPLETED' | null
  enrolledAt?: string | null
  completedLessons: number
  inProgressLessons: number
  totalLessons: number
  /** 0–100 */
  progressPercent: number
  lastActiveAt?: string | null
  avgQuizScore?: number | null
  /** Điểm bài kiểm tra (exam) của khóa: TB điểm quy đổi cao nhất mỗi đề; chỉ có ở danh sách. */
  avgExamScore?: number | null
}

/** Khớp `CourseStudentsResponse` ở BE (GET /teacher/courses/{courseId}/students). */
export interface CourseStudents {
  courseId: number
  courseTitle: string
  totalLessons: number
  summary: {
    totalStudents: number
    finishedStudents: number
    notStartedStudents: number
    activeRecently: number
    recentDays: number
    avgProgressPercent: number
  }
  students: PageResponse<StudentProgress>
}

/** Khớp `CourseStudentsResponse.StudentDetail` ở BE. */
export interface StudentCourseDetail {
  courseId: number
  courseTitle: string
  student: StudentProgress
  lessons: {
    lessonId: number
    title: string
    orderIndex: number
    status: LessonProgressStatus
    startedAt?: string | null
    lastAccessedAt?: string | null
    completedAt?: string | null
    bestScore?: number | null
  }[]
}

export interface CourseStudentsParams {
  keyword?: string
  sort?: StudentSort
  page?: number
  size?: number
}
