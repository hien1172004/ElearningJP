import type { SummaryRangePreset } from '@/utils/progress'

/** Mục tiêu ngày = ceil(tuần / 7), khớp `ProgressGoalService.toDailyGoal` ở BE. */
export function dailyGoalFromWeekly(weeklyGoalWords: number): number {
  return Math.ceil(weeklyGoalWords / 7)
}

export interface GoalPreset {
  label: string
  weeklyGoalWords: number
  /** Class màu của nhãn cấp độ. */
  badgeClass: string
}

export const GOAL_PRESETS: readonly GoalPreset[] = [
  { label: 'Tân binh', weeklyGoalWords: 30, badgeClass: 'bg-amber-50 text-amber-700' },
  { label: 'Chuyên gia', weeklyGoalWords: 40, badgeClass: 'bg-sky-50 text-sky-700' },
  { label: 'Giáo sư', weeklyGoalWords: 50, badgeClass: 'bg-rose-50 text-rose-700' },
]

/** Các mức ở bước "Tùy chỉnh": 60 → 140, bước 10. */
export const CUSTOM_WEEKLY_GOALS: readonly number[] = Array.from(
  { length: 9 },
  (_, i) => 60 + i * 10,
)

/** Mục tiêu tháng = mục tiêu ngày × 30. */
export function monthlyGoalFromDaily(dailyGoalWords: number): number {
  return dailyGoalWords * 30
}

/** Kỳ mục tiêu mà thẻ mascot đang so sánh. */
export type GoalPeriod = 'day' | 'week' | 'month'

export const GOAL_PERIOD_LABEL: Record<GoalPeriod, string> = {
  day: 'Mục tiêu ngày',
  week: 'Mục tiêu tuần',
  month: 'Mục tiêu tháng',
}

/** Lọc ngày/tuần/tháng → mục tiêu tương ứng; toàn thời gian/khác → mục tiêu tuần. */
export function goalPeriodForPreset(preset: SummaryRangePreset): GoalPeriod {
  if (preset === 'day') return 'day'
  if (preset === 'month') return 'month'
  return 'week'
}
