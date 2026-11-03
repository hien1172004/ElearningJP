import React from 'react'
import { CalendarDays } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useSrsForecast } from '@/hooks/useSrsForecast'

const formatLabel = (date: string, today: boolean): string => {
  if (today) return 'Hôm nay'
  const d = new Date(date + 'T00:00:00')
  if (Number.isNaN(d.getTime())) return date

  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)

  if (
    d.getFullYear() === tomorrow.getFullYear() &&
    d.getMonth() === tomorrow.getMonth() &&
    d.getDate() === tomorrow.getDate()
  ) {
    return 'Mai'
  }
  return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}`
}

export interface SrsForecastBarProps {
  className?: string
}

/**
 * SrsForecastBar — dự báo số thẻ cần ôn trong hôm nay và 4 ngày tiếp theo.
 */
export const SrsForecastBar: React.FC<SrsForecastBarProps> = ({ className }) => {
  const { data, isLoading } = useSrsForecast()
  const days = data?.days ?? []

  if (isLoading) {
    return (
      <div className={cn('flex items-center gap-2 animate-pulse py-1', className)}>
        <div className="h-6 w-16 bg-slate-100 rounded-full" />
        <div className="h-6 w-16 bg-slate-100 rounded-full" />
        <div className="h-6 w-16 bg-slate-100 rounded-full" />
      </div>
    )
  }

  if (days.length === 0) return null

  return (
    <div className={cn('flex items-center gap-2 flex-wrap', className)}>
      <span className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500">
        <CalendarDays className="w-3.5 h-3.5" />
        Ôn tập:
      </span>
      {days.map((d) => (
        <div
          key={d.date}
          title={d.date}
          className={cn(
            'flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-semibold border transition-colors',
            d.today
              ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs'
              : 'bg-white text-slate-600 border-slate-200 hover:border-slate-300',
          )}
        >
          <span>{formatLabel(d.date, d.today)}</span>
          <span
            className={cn(
              'tabular-nums font-bold',
              d.today ? 'text-white' : 'text-indigo-600',
            )}
          >
            {d.count}
          </span>
        </div>
      ))}
    </div>
  )
}
