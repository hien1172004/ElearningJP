import type { JlptLevel } from './dictionary.types'

/** Khớp `AdminDashboardOverviewResponse` ở BE (GET /admin/dashboard/overview). */
export interface AdminDashboardOverview {
  days: number
  /** yyyy-MM-dd, theo múi giờ Asia/Ho_Chi_Minh */
  from: string
  to: string
  users: DashboardUserStats
  activity: DashboardActivityStats
  exams: DashboardExamStats
  /** Mỗi ngày một phần tử (ngày trống = 0), tăng dần theo ngày */
  daily: DashboardDailyPoint[]
}

export interface DashboardUserStats {
  total: number
  newInRange: number
  newPrevious: number
  banned: number
  inactive: number
  googleAccounts: number
  localAccounts: number
}

export interface DashboardActivityStats {
  activeToday: number
  activeInRange: number
  activePrevious: number
  avgDailyActive: number
  studySecondsInRange: number
  studySecondsPrevious: number
  goalMetCount: number
  /** Ước tính từ dữ liệu cache streak; thứ tự 0 → 30+ ngày */
  streakBuckets: DashboardStreakBucket[]
}

export interface DashboardStreakBucket {
  label: string
  users: number
}

export interface DashboardExamStats {
  submittedInRange: number
  submittedPrevious: number
  inProgress: number
  /** 0–1, null nếu chưa có bài có kết quả */
  passRate: number | null
  avgScaledScore: number | null
  /** Luôn đủ 5 cấp N5 → N1 */
  byLevel: DashboardLevelStats[]
}

export interface DashboardLevelStats {
  level: JlptLevel
  submitted: number
  graded: number
  passed: number
  passRate: number | null
  avgScaledScore: number | null
}

export interface DashboardDailyPoint {
  date: string
  activeUsers: number
  newUsers: number
  studySeconds: number
}

/** Các khoảng thời gian trang dashboard cho chọn. */
export type DashboardRangeDays = 7 | 30 | 90
