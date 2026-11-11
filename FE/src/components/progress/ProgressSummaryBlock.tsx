import React from 'react'
import { Button, DatePicker, Select, Space, Tooltip } from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import { Link } from 'react-router-dom'
import { RefreshCw, CalendarRange, Info, CircleAlert, Sparkles } from 'lucide-react'
import type { ProgressSummary } from '@/types'
import { StatCard } from './StatCard'
import { MascotProgress } from './MascotProgress'
import { cn } from '@/utils/cn'
import {
  formatCompactNumber,
  formatIsoDate,
  formatDuration,
  MAX_CUSTOM_RANGE_DAYS,
  resolvePresetRange,
  type ProgressRange,
  type SummaryRangePreset,
} from '@/utils/progress'
import { todayIso } from '@/utils/week'
import {
  GOAL_PERIOD_LABEL,
  goalPeriodForPreset,
  monthlyGoalFromDaily,
} from '@/utils/progressGoal'

/**
 * ProgressSummaryBlock — Khối 1: tổng hợp tiến độ.
 *
 * Hiển thị 6 thẻ số liệu + mascot động viên kèm thẻ mục tiêu theo kỳ lọc
 * (ngày / tuần / tháng; toàn thời gian và khác dùng mục tiêu tuần),
 * cùng bộ lọc khoảng thời gian.
 *
 * <p><b>Hai khái niệm được tách bạch:</b></p>
 * <ul>
 *   <li><b>Từ đã học</b> — số mục MỚI học lần đầu trong kỳ.</li>
 *   <li><b>Số lần tra từ</b> — tổng lượt tra (kể cả tra lại từ đã học).</li>
 * </ul>
 *
 * <p><b>Không có thẻ "Đạt mục tiêu" riêng</b> — số ngày đạt mục tiêu đã hiện ở
 * lịch (Khối 2) và ở thẻ "Mục tiêu ngày" bên cạnh mascot. Giữ cả hai sẽ
 * trùng lặp thông tin.</p>
 *
 * ```tsx
 * <ProgressSummaryBlock
 *   summary={data}
 *   isLoading={isLoading}
 *   isError={isError}
 *   onRefetch={refetch}
 * />
 * ```
 */

/**
 * Re-export từ `utils/progress` để có 1 nguồn duy nhất cho cả Khối 1 và Khối 3.
 */
export type { SummaryRangePreset } from '@/utils/progress'

const { RangePicker } = DatePicker

const RANGE_OPTIONS: Array<{ value: SummaryRangePreset; label: string }> = [
  { value: 'day', label: 'Ngày' },
  { value: 'week', label: 'Tuần' },
  { value: 'month', label: 'Tháng' },
  { value: 'all', label: 'Toàn thời gian' },
  { value: 'custom', label: 'Khác' },
]

/** Không chọn ngày tương lai, và không vượt quá độ dài tối đa BE cho phép. */
function isCustomDateDisabled(current: Dayjs, info: { from?: Dayjs }): boolean {
  if (current.isAfter(dayjs(todayIso()), 'day')) return true
  if (!info.from) return false
  return Math.abs(current.diff(info.from, 'day')) >= MAX_CUSTOM_RANGE_DAYS
}

interface ProgressSummaryBlockProps {
  summary: ProgressSummary | undefined
  isLoading?: boolean
  isError?: boolean
  onRefetch?: () => void
  /** Kỳ đang chọn. */
  preset: SummaryRangePreset
  from?: string
  to?: string
  onRangeChange?: (range: ProgressRange) => void
  /** Mục tiêu từ/tuần đã chọn; null → suy ra từ mục tiêu ngày × 7. */
  weeklyGoalWords?: number | null
  /**
   * Số từ mới của tuần này — chỉ cần khi kỳ là "Toàn thời gian"/"Khác"
   * (lúc đó mascot so với mục tiêu tuần, không theo số liệu của kỳ).
   */
  currentWeekWordsLearned?: number
}

// --------------------------------------------------------------------------
// Loading skeleton
// --------------------------------------------------------------------------
const SkeletonBlock: React.FC = () => (
  <div className="rounded-md p-4">
    <div className="animate-pulse space-y-3">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      <div className="grid grid-cols-2 gap-3">
        {Array.from({ length: 6 }).map((_, i) => (
          <div key={i} className="h-20 bg-gray-100 rounded-md" />
        ))}
      </div>
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Error state
// --------------------------------------------------------------------------
const ErrorBlock: React.FC<{ onRefetch?: () => void; message?: string }> = ({
  onRefetch,
  message,
}) => (
  <div className="rounded-md bg-rose-50/60 p-6">
    <div className="flex flex-col sm:flex-row items-center gap-4 text-center sm:text-left">
      <span className="flex items-center justify-center w-14 h-14 rounded-full bg-rose-50 text-rose-500 shrink-0">
        <CircleAlert className="w-7 h-7" aria-hidden="true" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900">Không tải được tiến độ</p>
        <p className="text-sm text-gray-500 mt-0.5">
          {message ?? 'Có lỗi xảy ra. Vui lòng thử lại sau.'}
        </p>
      </div>
      {onRefetch && (
        <Button icon={<RefreshCw className="w-4 h-4" />} onClick={onRefetch}>
          Thử lại
        </Button>
      )}
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Empty state — user chưa có hoạt động nào
// --------------------------------------------------------------------------
const EmptyBlock: React.FC<{ onGoStudy?: () => void }> = ({ onGoStudy }) => (
  <div className="rounded-md p-6">
    <div className="flex flex-col sm:flex-row items-center gap-5 text-center sm:text-left">
      <span className="flex items-center justify-center w-[72px] h-[72px] rounded-full bg-gradient-to-br from-indigo-100 to-sky-100 text-indigo-500 shrink-0">
        <Sparkles className="w-9 h-9" aria-hidden="true" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900 text-lg">Bắt đầu ghi nhận tiến độ!</p>
        <p className="text-sm text-gray-500 mt-1 max-w-md">
          Mỗi lần bạn tra một từ trong từ điển hoặc học một bài, hệ thống sẽ
          ghi nhận lại. Hãy bắt đầu bằng việc tra vài từ vựng nhé!
        </p>
        {onGoStudy && (
          <Button type="primary" className="!rounded-md !font-semibold mt-4" onClick={onGoStudy}>
            Bắt đầu học ngay
          </Button>
        )}
      </div>
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Main block
// --------------------------------------------------------------------------
export const ProgressSummaryBlock: React.FC<ProgressSummaryBlockProps> = ({
  summary,
  isLoading = false,
  isError = false,
  onRefetch,
  preset,
  from,
  to,
  onRangeChange,
  weeklyGoalWords = null,
  currentWeekWordsLearned,
}) => {
  // "Khác" chỉ mở ô chọn ngày; chưa chọn xong thì số liệu vẫn giữ theo kỳ cũ.
  const [customOpen, setCustomOpen] = React.useState(preset === 'custom')

  const handlePreset = (next: SummaryRangePreset) => {
    if (next === 'custom') {
      setCustomOpen(true)
      return
    }
    setCustomOpen(false)
    onRangeChange?.(resolvePresetRange(next))
  }

  const handleCustomRange = (dates: [Dayjs | null, Dayjs | null] | null) => {
    if (!dates?.[0] || !dates[1]) return
    onRangeChange?.({
      preset: 'custom',
      from: dates[0].format('YYYY-MM-DD'),
      to: dates[1].format('YYYY-MM-DD'),
    })
  }

  if (isLoading && !summary) return <SkeletonBlock />
  if (isError && !summary) return <ErrorBlock onRefetch={onRefetch} />
  if (!summary) return <EmptyBlock />

  // --- Thẻ mục tiêu cạnh mascot: theo kỳ đang lọc ---
  // Ngày  → từ mới HÔM NAY / mục tiêu ngày.
  // Tuần  → từ mới của kỳ (tuần này) / mục tiêu tuần.
  // Tháng → từ mới của kỳ (tháng này) / mục tiêu ngày × 30.
  // Toàn thời gian / Khác → từ mới tuần này / mục tiêu tuần.
  const dailyGoal = summary.goalWords
  const goalPeriod = goalPeriodForPreset(preset)
  let learnedWords: number
  let goalWords: number
  if (goalPeriod === 'day') {
    // BE trả về 0 nếu user chưa có dữ liệu hôm nay (field mới, an toàn với BE cũ).
    learnedWords = summary.wordsLearnedToday ?? 0
    goalWords = dailyGoal
  } else if (goalPeriod === 'month') {
    learnedWords = summary.wordsLearned
    goalWords = monthlyGoalFromDaily(dailyGoal)
  } else {
    learnedWords = preset === 'week' ? summary.wordsLearned : (currentWeekWordsLearned ?? 0)
    goalWords = weeklyGoalWords ?? dailyGoal * 7
  }
  const goalMet =
    goalPeriod === 'day'
      ? (summary.goalMetToday ?? learnedWords >= goalWords)
      : learnedWords >= goalWords

  return (
    <section className="space-y-3" aria-label="Tổng hợp tiến độ học tập">
      {/* ---------- Header + bộ lọc kỳ ---------- */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
            <CalendarRange className="w-4 h-4 text-indigo-600" />
            Tổng hợp tiến độ
          </h2>
          <p className="text-xs text-gray-500 mt-0.5">
            {formatIsoDate(summary.from)} — {formatIsoDate(summary.to)}
            {summary.activeDays > 0 && (
              <span className="ml-1.5 text-gray-400">
                ({summary.activeDays} ngày có hoạt động)
              </span>
            )}
          </p>
        </div>

        <Space wrap size={8}>
          <Select
            size="small"
            className="min-w-[120px]"
            value={customOpen ? 'custom' : preset}
            onChange={handlePreset}
            options={RANGE_OPTIONS}
            disabled={!onRangeChange}
          />
          {customOpen && (
            <RangePicker
              size="small"
              allowClear={false}
              format="DD/MM/YYYY"
              placeholder={['Từ ngày', 'Đến ngày']}
              value={preset === 'custom' && from && to ? [dayjs(from), dayjs(to)] : null}
              disabledDate={isCustomDateDisabled}
              onChange={handleCustomRange}
            />
          )}
          {onRefetch && (
            <Tooltip title="Làm mới dữ liệu">
              <Button
                size="small"
                icon={<RefreshCw className="w-3.5 h-3.5" />}
                onClick={onRefetch}
                loading={isLoading}
                aria-label="Làm mới dữ liệu tiến độ"
              />
            </Tooltip>
          )}
        </Space>
      </div>

      {/* ---------- Mascot + thẻ số liệu ---------- */}
      {/* Thẻ số liệu giới hạn rộng tối đa 440px; phần còn lại dành cho mascot */}
      <div className="grid grid-cols-1 md:grid-cols-[minmax(0,440px)_minmax(0,1fr)] gap-3">
        <div className="grid grid-cols-2 gap-2.5 content-start">
          <StatCard
            icon="words"
            label="Từ đã học"
            value={formatCompactNumber(summary.wordsLearned)}
            hint="Từ mới học lần đầu trong kỳ"
            deltaPct={summary.wordsLearnedDeltaPct}
            tone="indigo"
          />
          <StatCard
            icon="search"
            label="Số lần tra từ"
            value={formatCompactNumber(summary.totalLookups)}
            hint="Tổng lượt tra trong kỳ"
            tone="cyan"
          />
          <StatCard
            icon="time"
            label="Thời gian học"
            value={formatDuration(summary.studySeconds)}
            hint="Tổng trong kỳ"
            deltaPct={summary.studySecondsDeltaPct}
            tone="sky"
          />
          <StatCard
            icon="streak"
            label="Chuỗi ngày"
            value={`${summary.currentStreak} ngày`}
            hint={`Kỷ lục: ${summary.longestStreak} ngày`}
            tone="amber"
          />
          {/* <StatCard
            icon="xp"
            label="XP kiếm được"
            value={formatCompactNumber(summary.xpEarned)}
            hint="Tổng trong kỳ"
            tone="rose"
          />
          <StatCard
            icon="days"
            label="Ngày có học"
            value={`${summary.activeDays} ngày`}
            hint="Trong kỳ đã chọn"
            tone="emerald"
          /> */}
        </div>

        {/* Mascot cạnh danh */}
        <div className="flex flex-col items-center justify-center gap-2 bg-kinari/80 border border-suna/70 rounded-xl p-4">
          <MascotProgress learned={learnedWords} goal={goalWords} size={128} />
          <div className="w-full max-w-[240px] text-center">
            <p className="text-[11px] font-semibold text-nibi uppercase tracking-wide">
              {GOAL_PERIOD_LABEL[goalPeriod]}
            </p>

            {/* Số từ MỚI học trong kỳ mục tiêu / mục tiêu */}
            <p className="text-xl font-extrabold text-sumi mt-0.5 tabular-nums">
              {learnedWords}
              <span className="text-sm font-bold text-nibi">
                /{goalWords}
              </span>
            </p>

            {/* Thanh tiến độ mục tiêu */}
            <div className="h-1.5 w-full bg-slate-100 rounded-full mt-2 overflow-hidden">
              <div
                className={cn(
                  'h-full rounded-full transition-all duration-500',
                  goalMet
                    ? 'bg-emerald-500'
                    : learnedWords > 0
                      ? 'bg-ai'
                      : 'bg-gray-300',
                )}
                style={{
                  width: `${Math.min(
                    100,
                    Math.round((learnedWords / Math.max(goalWords, 1)) * 100),
                  )}%`,
                }}
              />
            </div>

            <Tooltip title="Từ đã học = số từ MỚI tra lần đầu. Tra lại từ cũ chỉ tăng số lần tra từ.">
              <span className="inline-flex items-center gap-1 text-[10px] text-nibi mt-1.5 cursor-help">
                <Info className="w-3 h-3 text-ai" />
                {goalMet
                  ? 'Đã đạt mục tiêu!'
                  : `Cần thêm ${Math.max(goalWords - learnedWords, 0)} từ`}
              </span>
            </Tooltip>

            <Link
              to="/progress/goal"
              className="block text-[11px] font-semibold text-ai hover:text-ai/80 hover:underline mt-1.5 transition-colors"
            >
              Đổi mục tiêu
            </Link>

            {/* Tổng lượt tra hôm nay (kể cả tra lại từ đã học) */}
            {/* <p className="text-[10px] text-gray-400 mt-1">
              Lượt tra hôm nay:{' '}
              <span className="font-semibold text-gray-600 tabular-nums">
                {lookupsToday}
              </span>
            </p> */}
          </div>
        </div>
      </div>
    </section>
  )
}

export default ProgressSummaryBlock