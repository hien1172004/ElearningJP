import React from 'react'
import { CalendarDays, Check } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useSrsForecast } from '@/hooks/useSrsForecast'
import type { SrsForecastDay } from '@/types'

const WEEKDAY_LABELS = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN']

const parseDate = (date: string): Date | null => {
  const d = new Date(date + 'T00:00:00')
  return Number.isNaN(d.getTime()) ? null : d
}

const dayTitle = (d: SrsForecastDay): string => {
  const parts = [d.date]
  if (!d.past) parts.push(`${d.count} thẻ cần ôn`)
  if (d.past || d.today) parts.push(`đã ôn ${d.reviewedCount} thẻ`)
  return parts.join(' · ')
}

export interface SrsForecastBarProps {
  className?: string
}

/**
 * SrsForecastBar — lịch ôn tập tuần hiện tại (Thứ 2 → Chủ nhật).
 *  - Hôm nay: được đánh dấu nổi bật, hiện số thẻ cần ôn (gồm quá hạn + thẻ mới).
 *  - Ngày tới: số thẻ sẽ đến hạn.
 *  - Ngày đã qua: số thẻ đã ôn (dấu ✓).
 */
export const SrsForecastBar: React.FC<SrsForecastBarProps> = ({ className }) => {
  const { data, isLoading } = useSrsForecast()
  const days = data?.days ?? []

  if (isLoading) {
    return (
      <div className={cn('grid grid-cols-7 gap-2 animate-pulse', className)}>
        {WEEKDAY_LABELS.map((l) => (
          <div key={l} className="h-16 bg-slate-100 rounded-xl" />
        ))}
      </div>
    )
  }

  if (days.length === 0) return null

  return (
    <div className={className}>
      <div className="flex items-center gap-1.5 mb-2 text-xs font-semibold text-slate-500">
        <CalendarDays className="w-3.5 h-3.5" />
        Lịch ôn tập tuần này
      </div>
      <div className="grid grid-cols-7 gap-2">
        {days.map((d, i) => {
          const date = parseDate(d.date)
          return (
            <div
              key={d.date}
              title={dayTitle(d)}
              aria-current={d.today ? 'date' : undefined}
              className={cn(
                'flex flex-col items-center justify-center gap-0.5 py-2 rounded-xl border text-xs transition-colors',
                d.today
                  ? 'bg-indigo-600 text-white border-indigo-600 shadow-sm'
                  : d.past
                    ? 'bg-slate-50 text-slate-400 border-slate-100'
                    : 'bg-surface text-slate-600 border-slate-200',
              )}
            >
              <span className={cn('font-semibold', d.today ? 'text-white/80' : '')}>
                {WEEKDAY_LABELS[i] ?? ''}
              </span>
              <span className="text-base font-bold tabular-nums leading-none">
                {date ? date.getDate() : '—'}
              </span>
              {d.past ? (
                <span className="inline-flex items-center gap-0.5 text-[11px] font-semibold text-emerald-600 tabular-nums">
                  {d.reviewedCount > 0 ? (
                    <>
                      <Check className="w-3 h-3" />
                      {d.reviewedCount}
                    </>
                  ) : (
                    <span className="text-slate-300">–</span>
                  )}
                </span>
              ) : (
                <span
                  className={cn(
                    'text-[11px] font-bold tabular-nums',
                    d.today ? 'text-white' : d.count > 0 ? 'text-indigo-600' : 'text-slate-300',
                  )}
                >
                  {d.count} thẻ
                </span>
              )}
            </div>
          )
        })}
      </div>
    </div>
  )
}
