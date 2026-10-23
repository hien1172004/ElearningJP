import React from 'react'
import {
  BookOpen,
  Clock,
  Flame,
  Target,
  TrendingDown,
  TrendingUp,
  Minus,
  Sparkles,
} from 'lucide-react'
import { cn } from '@/utils/cn'

/**
 * StatCard — 1 thẻ số liệu cho Khối 1.
 *
 * Hiển thị: icon + label + giá trị + delta % (nếu có).
 */

export type StatIcon = 'words' | 'time' | 'streak' | 'goal' | 'xp' | 'days'

const ICON_MAP: Record<StatIcon, React.ReactNode> = {
  words: <BookOpen className="w-4 h-4" />,
  time: <Clock className="w-4 h-4" />,
  streak: <Flame className="w-4 h-4" />,
  goal: <Target className="w-4 h-4" />,
  xp: <Sparkles className="w-4 h-4" />,
  days: <BookOpen className="w-4 h-4" />,
}

const TONE_MAP = {
  indigo: 'bg-indigo-50 text-indigo-600',
  emerald: 'bg-emerald-50 text-emerald-600',
  amber: 'bg-amber-50 text-amber-600',
  rose: 'bg-rose-50 text-rose-600',
  sky: 'bg-sky-50 text-sky-600',
} as const

export interface StatCardProps {
  icon: StatIcon
  label: string
  value: React.ReactNode
  /** Phụ (vd: "mục tiêu 5 từ/ngày"). */
  hint?: string
  /**
   * % thay đổi so với kỳ trước.
   * `null` = kỳ trước bằng 0 → không hiển thị delta.
   */
  deltaPct?: number | null
  tone?: keyof typeof TONE_MAP
  className?: string
}

export const StatCard: React.FC<StatCardProps> = ({
  icon,
  label,
  value,
  hint,
  deltaPct,
  tone = 'indigo',
  className,
}) => {
  // Delta: null → ẩn; 0 → hiện "0%" màu xám; >0 tăng; <0 giảm
  const showDelta = deltaPct !== null && deltaPct !== undefined
  const deltaValue = showDelta ? (deltaPct as number) : 0
  const DeltaIcon = deltaValue > 0 ? TrendingUp : deltaValue < 0 ? TrendingDown : Minus
  const deltaColor =
    deltaValue > 0
      ? 'text-emerald-600 bg-emerald-50'
      : deltaValue < 0
        ? 'text-rose-600 bg-rose-50'
        : 'text-gray-500 bg-gray-100'

  return (
    <div
      className={cn(
        'bg-white rounded-md border border-gray-200/80 shadow-xs p-3.5',
        'flex flex-col gap-1.5',
        className,
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <span
          className={cn(
            'inline-flex items-center justify-center w-8 h-8 rounded-md shrink-0',
            TONE_MAP[tone],
          )}
        >
          {ICON_MAP[icon]}
        </span>
        {showDelta && (
          <span
            className={cn(
              'inline-flex items-center gap-1 px-1.5 py-0.5 rounded-md text-[11px] font-bold shrink-0',
              deltaColor,
            )}
            title="So với kỳ trước"
          >
            <DeltaIcon className="w-3 h-3" />
            {Math.abs(deltaValue)}%
          </span>
        )}
      </div>

      <div>
        <p className="text-[11px] font-semibold text-gray-500 uppercase tracking-wide">
          {label}
        </p>
        <p className="text-xl font-extrabold text-gray-900 mt-0.5 tabular-nums">
          {value}
        </p>
        {hint && <p className="text-[11px] text-gray-500 mt-0.5">{hint}</p>}
      </div>
    </div>
  )
}

export default StatCard