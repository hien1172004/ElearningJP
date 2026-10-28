import React, { useMemo } from 'react'
import { Button, Tooltip } from 'antd'
import { CalendarDays, ChevronLeft, ChevronRight } from 'lucide-react'
import { useProgressCalendar } from '@/hooks/useProgressCalendar'
import { cn } from '@/utils/cn'
import {
  buildCalendarGrid,
  formatMonthLabel,
  weekdayLabels,
  type DayState,
} from '@/utils/calendar'
import { currentMonthInTz, shiftMonth } from '@/utils/week'

/**
 * StudyCalendarBlock — Khối 2: lịch học + chuỗi streak.
 *
 * Quy tắc màu ô ngày (đúng theo thiết kế):
 * - **xanh dương** — ngày học **đạt mục tiêu** (`newWords >= goalWords`)
 * - **xanh lá**   — ngày có học nhưng **chưa đạt mục tiêu**
 * - **không màu** — ngày không học (và ngày chưa tới → làm mờ)
 *
 * <p>Chính sách STRICT: một ngày chỉ được coi là "có học" khi học được ít nhất
 * 1 TỪ MỚI. Chỉ tra lại từ đã học thì ngày đó không được tô màu, không đạt
 * mục tiêu và không giữ streak.</p>
 *
 * ```tsx
 * <StudyCalendarBlock />
 * ```
 */

/** Màu nền + chữ cho từng trạng thái ngày. */
const STATE_STYLE: Record<DayState, string> = {
  // Đạt mục tiêu — xanh dương (nổi bật nhất, giống "perfect day" trong ảnh).
  goal: 'bg-blue-600 text-white border-blue-600 hover:bg-blue-700',
  // Có học nhưng chưa đạt — xanh lá.
  active: 'bg-emerald-500 text-white border-emerald-500 hover:bg-emerald-600',
  // Không học — không tô màu (nền trắng, viền xám nhạt).
  empty: 'bg-white text-gray-500 border-gray-200',
  // Ngày chưa tới — làm mờ, không tô màu.
  future: 'bg-gray-50 text-gray-300 border-gray-100',
}

/** Màu viền riêng cho hôm nay (để nổi lên nền đã tô màu). */
const TODAY_RING = 'ring-2 ring-offset-1 ring-orange-400'

// --------------------------------------------------------------------------
// Loading / Error
// --------------------------------------------------------------------------
const SkeletonBlock: React.FC = () => (
  <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-4">
    <div className="animate-pulse space-y-3">
      <div className="h-5 w-44 bg-gray-200 rounded" />
      <div className="grid grid-cols-7 gap-1">
        {Array.from({ length: 35 }).map((_, i) => (
          <div key={i} className="h-8 bg-gray-100 rounded" />
        ))}
      </div>
    </div>
  </div>
)

const ErrorBlock: React.FC<{ onRetry?: () => void; message?: string }> = ({
  onRetry,
  message,
}) => (
  <div className="bg-white rounded-md border border-rose-200 shadow-xs p-4">
    <div className="flex flex-col sm:flex-row items-center gap-3 text-center sm:text-left">
      <span className="w-10 h-10 rounded-md bg-rose-50 text-rose-500 flex items-center justify-center shrink-0">
        <CalendarDays className="w-5 h-5" />
      </span>
      <div className="flex-1">
        <p className="font-bold text-gray-900 text-sm">Không tải được lịch học</p>
        <p className="text-xs text-gray-500 mt-0.5">
          {message ?? 'Có lỗi xảy ra. Vui lòng thử lại sau.'}
        </p>
      </div>
      {onRetry && (
        <Button size="small" onClick={onRetry}>Thử lại</Button>
      )}
    </div>
  </div>
)

// --------------------------------------------------------------------------
// Main block
// --------------------------------------------------------------------------
export const StudyCalendarBlock: React.FC = () => {
  // Tháng đang xem — mặc định tháng hiện tại (theo múi giờ nghiệp vụ).
  const [month, setMonth] = React.useState<string>(() => currentMonthInTz())

  const { data, isLoading, isError, error, refetch } = useProgressCalendar(month)

  const thisMonth = useMemo(() => currentMonthInTz(), [])
  const viewingCurrentMonth = month === thisMonth

  /** Dựng lưới + tra cứu số lần tra để hiện tooltip. */
  const { cells, lookupsByDate, newWordsByDate } = useMemo(() => {
    const lookups: Record<string, number> = {}
    const newWords: Record<string, number> = {}
    const goals = new Set<string>()

    for (const day of data?.days ?? []) {
      if (day.lookups > 0) lookups[day.date] = day.lookups
      if (day.newWords > 0) newWords[day.date] = day.newWords
      if (day.goalMet) goals.add(day.date)
    }

    const grid = data
      ? buildCalendarGrid({
          monthStart: data.monthStart,
          monthEnd: data.monthEnd,
          today: data.today,
          newWordsByDate: newWords,
          goalDates: goals,
        })
      : []

    return { cells: grid, lookupsByDate: lookups, newWordsByDate: newWords }
  }, [data])

  if (isLoading && !data) return <SkeletonBlock />
  if (isError && !data) {
    return <ErrorBlock message={error?.message} onRetry={() => void refetch()} />
  }

  const weekdays = weekdayLabels()

  return (
    <section className="space-y-3" aria-label="Lịch học và chuỗi ngày liên tụp">
      {/* ---------- Header: tiêu đề + điều hướng tháng ---------- */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-2">
        <div>
          <h2 className="text-base font-extrabold text-gray-900 flex items-center gap-2">
            <CalendarDays className="w-4 h-4 text-indigo-600" />
            Lịch học &amp; chuỗi ngày
          </h2>
          {/* <p className="text-xs text-gray-500 mt-0.5">
            {formatMonthLabel(month)}
            {data && data.activeDays > 0 && (
              <span className="text-gray-400">
                {' '}
                · {data.activeDays} ngày có học · {formatCompactNumber(data.totalLookups)} từ
              </span>
            )}
          </p> */}
        </div>

        <div className="flex items-center gap-2">
          {/* Chuyển tháng */}
          <div className="flex items-center gap-1 bg-white rounded-md border border-gray-200/80 shadow-xs p-0.5">
            <Tooltip title="Tháng trước">
              <Button
                type="text"
                size="small"
                onClick={() => setMonth((m) => shiftMonth(m, -1))}
                icon={<ChevronLeft className="w-3.5 h-3.5" />}
                aria-label="Tháng trước"
              />
            </Tooltip>
            <span className="text-[11px] font-semibold text-gray-700 px-1 tabular-nums min-w-[92px] text-center">
              {formatMonthLabel(month)}
            </span>
            <Tooltip title="Tháng sau">
              <Button
                type="text"
                size="small"
                onClick={() => setMonth((m) => shiftMonth(m, 1))}
                // Không cho xem tương lai quá xa — chỉ chặn vượt qua tháng hiện tại.
                disabled={viewingCurrentMonth}
                icon={<ChevronRight className="w-3.5 h-3.5" />}
                aria-label="Tháng sau"
              />
            </Tooltip>
          </div>

          {viewingCurrentMonth && (
            <Button size="small" onClick={() => setMonth(thisMonth)}>
              Tháng này
            </Button>
          )}
        </div>
      </div>

      <div className="space-y-3">
        {/* ---------- Lưới lịch ---------- */}
        <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5">
          {/* Header thứ */}
          <div className="grid grid-cols-7 gap-1 mb-1.5">
            {weekdays.map((label) => (
              <div
                key={label}
                className="text-center text-[10px] font-bold text-gray-400 uppercase tracking-wide py-0.5"
              >
                {label}
              </div>
            ))}
          </div>

          {/* Các ô ngày */}
          <div className="grid grid-cols-7 gap-1">
            {cells.map((cell, idx) => {
              if (cell.dayNumber === null) {
                // Ô trống đệm để lưới thẳng hàng theo thứ 2.
                return <div key={`blank-${idx}`} aria-hidden="true" />
              }

              const lookups = lookupsByDate[cell.date]
              const newWords = newWordsByDate[cell.date] ?? 0
              const hasActivity = cell.state === 'goal' || cell.state === 'active'

              const tip = hasActivity
                ? `${cell.dayNumber}/${cell.date.slice(5, 7)} — ${newWords} từ mới · ${lookups ?? 0} lần tra${
                    cell.state === 'goal' ? ' · đạt mục tiêu' : ' · chưa đạt mục tiêu'
                  }`
                : cell.isFuture
                  ? `${cell.dayNumber}/${cell.date.slice(5, 7)} — chưa tới`
                  : `${cell.dayNumber}/${cell.date.slice(5, 7)} — không học`

              return (
                <Tooltip key={cell.date} title={tip}>
                  <div
                    className={cn(
                      'h-9 rounded border flex flex-col items-center justify-center',
                      'text-[11px] font-bold tabular-nums transition-colors cursor-default',
                      // Ô tương lai dùng style riêng (làm mờ) — không phải "không học".
                      cell.isFuture ? STATE_STYLE.future : STATE_STYLE[cell.state],
                      cell.isToday && TODAY_RING,
                    )}
                    aria-label={tip}
                  >
                    <span>{cell.dayNumber}</span>
                    {/* Chấm nhỏ báo đã học, giúp màu không chỉ dựa vào sắc độ. */}
                    {hasActivity && (
                      <span
                        className="w-0.5 h-0.5 rounded-full bg-white/80 mt-0.5"
                        aria-hidden="true"
                      />
                    )}
                  </div>
                </Tooltip>
              )
            })}
          </div>

          {/* Chú thích màu */}
          <div className="flex flex-wrap items-center gap-x-3 gap-y-1.5 mt-3 pt-2.5 border-t border-gray-100">
            <LegendItem color="bg-blue-600" label="Đạt mục tiêu" />
            <LegendItem color="bg-emerald-500" label="Có học, chưa đạt" />
            <LegendItem color="bg-white border border-gray-300" label="Không học" />
            <span className="text-[10px] text-gray-400 ml-auto">
              Mục tiêu {data?.goalWords ?? 5} từ MỚI/ngày
            </span>
          </div>
        </div>

        {/* ---------- Card streak ---------- */}
        {/* <div className="grid grid-cols-2 gap-3">
          <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5 text-center">
            <span className="inline-flex w-10 h-10 rounded-full bg-amber-50 text-amber-500 items-center justify-center">
              <Flame className="w-5 h-5" />
            </span>
            <p className="text-2xl font-extrabold text-gray-900 mt-1.5 tabular-nums">
              {data?.currentStreak ?? 0}
            </p>
            <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wide">
              ngày liên tiếp
            </p>
            {data && (
              <p className="text-[10px] text-gray-400 mt-0.5">
                Kỷ lục {data.longestStreak} ngày
              </p>
            )}
          </div>

          <div className="bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5 text-center">
            <span className="inline-flex w-10 h-10 rounded-full bg-blue-50 text-blue-500 items-center justify-center">
              <Trophy className="w-5 h-5" />
            </span>
            <p className="text-2xl font-extrabold text-gray-900 mt-1.5 tabular-nums">
              {data?.goalMetDays ?? 0}
            </p>
            <p className="text-[10px] font-semibold text-gray-500 uppercase tracking-wide">
              ngày đạt mục tiêu
            </p>
            <p className="text-[10px] text-gray-400 mt-0.5">
              trong {formatMonthShort(month).toLowerCase()}
            </p>
          </div>

          {data && data.days.length > 0 && (
            <p className="text-[10px] text-gray-400 text-center col-span-2">
              Tổng{' '}
              {formatDuration(
                data.days.reduce((sum, d) => sum + d.studySeconds, 0),
              )}{' '}
              học trong tháng
            </p>
          )}
        </div> */}
      </div>
    </section>
  )
}

// --------------------------------------------------------------------------
// Chú thích
// --------------------------------------------------------------------------
const LegendItem: React.FC<{ color: string; label: string }> = ({
  color,
  label,
}) => (
  <span className="inline-flex items-center gap-1.5 text-[11px] text-gray-500">
    <span className={cn('w-3 h-3 rounded-sm', color)} aria-hidden="true" />
    {label}
  </span>
)

export default StudyCalendarBlock