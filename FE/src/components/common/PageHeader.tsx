import React from 'react'
import { Sparkles } from 'lucide-react' 

interface PageHeaderProps {
  badge?: string
  title: React.ReactNode
  subtitle?: React.ReactNode
  actions?: React.ReactNode
  /** Màu chữ của badge. Mặc định: indigo. */
  variant?: 'indigo' | 'amber' | 'rose' | 'emerald'
  /** Hiển thị icon sparkle trong badge. */
  withSparkle?: boolean
  className?: string
}

const badgeTextMap: Record<NonNullable<PageHeaderProps['variant']>, string> = {
  indigo: 'text-indigo-600',
  amber: 'text-amber-700',
  rose: 'text-rose-700',
  emerald: 'text-emerald-700',
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
  // Header phẳng (không banner): không nền màu, không vạch trái — chỉ giữ màu chữ theo variant cho badge
  return (
    <div className={`pb-1 ${className ?? ''}`}>
      {badge && (
        <div className="mb-1 flex items-center gap-1.5">
          <span className={`inline-flex items-center gap-1 text-xs font-semibold ${badgeTextMap[variant]}`}>
            {withSparkle && <Sparkles className="w-3 h-3" />}
            {badge}
          </span>
        </div>
      )}
      {/* Top row: title + actions */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex-1 min-w-0 text-sumi">{title}</div>
        {actions && <div className="flex items-center gap-2 shrink-0">{actions}</div>}
      </div>

      {/* Subtitle */}
      {subtitle && <p className="text-sm text-nibi mt-0.5">{subtitle}</p>}
    </div>
  )
}
