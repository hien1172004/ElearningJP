import React from 'react'
import { Sparkles } from 'lucide-react'

interface PageHeaderProps {
  badge?: string
  title: React.ReactNode
  subtitle?: React.ReactNode
  actions?: React.ReactNode
  /** Gradient variant. Mặc định: indigo. */
  variant?: 'indigo' | 'amber' | 'rose' | 'emerald'
  /** Hiển thị icon sparkle trong badge. */
  withSparkle?: boolean
  className?: string
}

const borderMap: Record<NonNullable<PageHeaderProps['variant']>, string> = {
  indigo: 'border-indigo-500',
  amber: 'border-amber-500',
  rose: 'border-rose-500',
  emerald: 'border-emerald-500',
}

const bgMap: Record<NonNullable<PageHeaderProps['variant']>, string> = {
  indigo: 'bg-indigo-50/70',
  amber: 'bg-amber-50/70',
  rose: 'bg-rose-50/70',
  emerald: 'bg-emerald-50/70',
}

const titleMap: Record<NonNullable<PageHeaderProps['variant']>, string> = {
  indigo: 'text-indigo-900',
  amber: 'text-amber-900',
  rose: 'text-rose-900',
  emerald: 'text-emerald-900',
}

const badgeBgMap: Record<NonNullable<PageHeaderProps['variant']>, string> = {
  indigo: 'bg-indigo-100 text-indigo-700',
  amber: 'bg-amber-100 text-amber-700',
  rose: 'bg-rose-100 text-rose-700',
  emerald: 'bg-emerald-100 text-emerald-700',
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  badge,
  title,
  subtitle,
  actions,
  variant = 'indigo',
  withSparkle = true,
  className,
}) => {
  return (
    <div
      className={`border-l-4 ${borderMap[variant]} ${bgMap[variant]} rounded shadow-sm ${
        className ?? ''
      }`}
    >
      <div className="px-4 py-2.5">
        {/* Top row: title + actions */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex-1 min-w-0">{title}</div>
          {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
        </div>

        {/* Subtitle */}
        {subtitle && (
          <p className="text-sm text-gray-500 mt-0.5">{subtitle}</p>
        )}
      </div>
    </div>
  )
}
