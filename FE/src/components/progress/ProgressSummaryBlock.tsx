import React from 'react'
import { Button, Select, Space, Tooltip } from 'antd'
import { RefreshCw, CalendarRange, Info } from 'lucide-react'
import type { ProgressSummary } from '@/types'
import { StatCard } from './StatCard'
import { Mascot } from './Mascot'
import { cn } from '@/utils/cn'
import {
  formatCompactNumber,
  formatIsoDate,
  formatDuration,
  goalMood,
} from '@/utils/progress'

/**
 * ProgressSummaryBlock — Khối 1: tổng hợp tiến độ.
 *
 * Hiển thị 4 thẻ số liệu + mascot động viên kèm thẻ "Mục tiêu ngày",
 * cùng bộ lọc khoảng thời gian.
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

export type SummaryRangePreset = '7d' | '30d' | '90d' | 'month' | 'year'

interface ProgressSummaryBlockProps {
  summary: ProgressSummary | undefined
  isLoading?: boolean
  isError?: boolean
  onRefetch?: () => void
  /** Khoảng thời gian hiện tại (đã resolve về yyyy-MM-dd). */
  from?: string
  to?: string
  onRangeChange?: (range: {
    preset: SummaryRangePreset
    from?: string
    to?: string
  }) => void
}

// --------------------------------------------------------------------------
// Local date helpers (không dùng dayjs để tránh phụ thuộc ngoài ý muốn)
// --------------------------------------------------------------------------

/** Ngày hôm nay dạng yyyy-MM-dd theo giờ địa phương (không phải UTC). */
function todayIso(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Lùi `n` ngày rồi trả về yyyy-MM-dd. */
function shiftDays(days: number): string {
  const d = new Date()
  d.setDate(d.getDate() + days)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Ngày đầu tháng hiện tại. */
function startOfMonthIso(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}-01`
}

/** Ngày cuối tháng hiện tại. */
function endOfMonthIso(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const last = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate()
  return `${y}-${m}-${String(last).padStart(2, '0')}`
}

/**
 * Năm hiện tại dạng chuỗi 4 chữ số.
 *
 * <p>Tách riêng thành helper (thay vì gọi `new Date()` trực tiếp trong
 * component) để giữ component render "pure" — tránh gọi hàm không tất định
 * (impure) trực tiếp trong quá trình render.</p>
 */
function currentYear(): string {
  return String(new Date().getFullYear())
}

// --------------------------------------------------------------------------
// Loading skeleton
// --------------------------------------------------------------------------
const SkeletonBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4">
    <div className="animate-pulse space-y-3">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      <div className="grid grid-cols-2 gap-3">
        {Array.from({ length: 4 }).map((_, i) => (
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
  <div className="bg-white rounded-md border border-rose-200 shadow-xs p-6">
    <div className="flex flex-col sm:flex-row items-center gap-4 text-center sm:text-left">
      <Mascot mood="sleepy" size={56} />
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
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-6">
    <div className="flex flex-col sm:flex-row items-center gap-5 text-center sm:text-left">
      <Mascot mood="cheer" size={72} showMessage />
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
  from,
  to,
  onRangeChange,
}) => {
  /**
   * Xác định preset hiện đang active để set `value` của Select.
   * So sánh với khoảng ngày chuẩn của từng preset.
   */
  const activePreset: SummaryRangePreset | undefined = React.useMemo(() => {
    if (!from || !to) return undefined
    const presets: Array<[SummaryRangePreset, string, string]> = [
      ['7d', shiftDays(-6), todayIso()],
      ['30d', shiftDays(-29), todayIso()],
      ['90d', shiftDays(-89), todayIso()],
      ['month', startOfMonthIso(), endOfMonthIso()],
      ['year', `${currentYear()}-01-01`, `${currentYear()}-12-31`],
    ]
    return presets.find(([, f, t]) => f === from && t === to)?.[0]
  }, [from, to])

  const handlePreset = (preset: SummaryRangePreset) => {
    if (!onRangeChange) return
    switch (preset) {
      case '7d':
        onRangeChange({
          preset,
          from: shiftDays(-6),
          to: todayIso(),
        })
        break
      case '30d':
        onRangeChange({
          preset,
          from: shiftDays(-29),
          to: todayIso(),
        })
        break
      case '90d':
        onRangeChange({
          preset,
          from: shiftDays(-89),
          to: todayIso(),
        })
        break
      case 'month':
        onRangeChange({
          preset,
          from: startOfMonthIso(),
          to: endOfMonthIso(),
        })
        break
      case 'year':
        onRangeChange({
          preset,
          from: `${new Date().getFullYear()}-01-01`,
          to: `${new Date().getFullYear()}-12-31`,
        })
        break
    }
  }

  if (isLoading && !summary) return <SkeletonBlock />
  if (isError && !summary) return <ErrorBlock onRefetch={onRefetch} />
  if (!summary) return <EmptyBlock />

  const hasActivity = summary.wordsLearned > 0 || summary.studySeconds > 0

  if (!hasActivity) {
    return <EmptyBlock />
  }

  // --- Thẻ "Mục tiêu ngày" ---
  // Dùng `wordsLearnedToday` (tiến độ HÔM NAY) chứ không phải tổng của cả kỳ.
  // BE trả về 0 nếu user chưa có dữ liệu hôm nay (field mới, an toàn với BE cũ).
  const goalWords = summary.goalWords ?? 5
  const wordsToday = summary.wordsLearnedToday ?? 0
  const todayGoalMet = summary.goalMetToday ?? wordsToday >= goalWords

  // Mood của mascot dựa trên tiến độ HÔM NAY (không phải tổng kỳ) — đồng bộ
  // với thẻ "Mục tiêu ngày" ngay bên dưới.
  const mood = goalMood(wordsToday, goalWords)

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
            value={activePreset}
            placeholder="Chọn kỳ"
            onChange={(v) => handlePreset(v as SummaryRangePreset)}
            options={[
              { value: '7d', label: '7 ngày' },
              { value: '30d', label: '30 ngày' },
              { value: '90d', label: '90 ngày' },
              { value: 'month', label: 'Tháng này' },
              { value: 'year', label: 'Năm nay' },
            ]}
            disabled={!onRangeChange}
          />
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
      <div className="grid grid-cols-1 xl:grid-cols-[minmax(0,1fr)_200px] gap-3">
        <div className="grid grid-cols-2 gap-3">
          <StatCard
            icon="words"
            label="Từ đã học"
            value={formatCompactNumber(summary.wordsLearned)}
            hint={`${summary.wordsLearned} lần tra từ điển`}
            deltaPct={summary.wordsLearnedDeltaPct}
            tone="indigo"
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
          <StatCard
            icon="xp"
            label="XP kiếm được"
            value={formatCompactNumber(summary.xpEarned)}
            hint="Tổng trong kỳ"
            tone="rose"
          />
        </div>

        {/* Mascot cạnh danh */}
        <div className="flex flex-col items-center justify-center gap-2 bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5">
          <Mascot mood={mood} size={64} />
          <div className="w-full text-center">
            <p className="text-[11px] font-semibold text-gray-500 uppercase tracking-wide">
              Mục tiêu ngày
            </p>

            {/* Số từ đã tra hôm nay / mục tiêu */}
            <p className="text-xl font-extrabold text-gray-900 mt-0.5 tabular-nums">
              {wordsToday}
              <span className="text-sm font-bold text-gray-400">
                /{goalWords}
              </span>
            </p>

            {/* Thanh tiến độ mục tiêu hôm nay */}
            <div className="h-1.5 w-full bg-gray-100 rounded-full mt-2 overflow-hidden">
              <div
                className={cn(
                  'h-full rounded-full transition-all duration-500',
                  todayGoalMet
                    ? 'bg-emerald-500'
                    : wordsToday > 0
                      ? 'bg-blue-500'
                      : 'bg-gray-300',
                )}
                style={{
                  width: `${Math.min(
                    100,
                    Math.round((wordsToday / Math.max(goalWords, 1)) * 100),
                  )}%`,
                }}
              />
            </div>

            <Tooltip title="1 lần tra từ điển = 1 từ đã học">
              <span className="inline-flex items-center gap-1 text-[10px] text-gray-400 mt-1.5 cursor-help">
                <Info className="w-3 h-3" />
                {todayGoalMet
                  ? 'Đã đạt mục tiêu!'
                  : `Cần thêm ${Math.max(goalWords - wordsToday, 0)} từ`}
              </span>
            </Tooltip>
          </div>
        </div>
      </div>
    </section>
  )
}

export default ProgressSummaryBlock