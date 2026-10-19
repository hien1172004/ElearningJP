import React from 'react'
import { Progress, Tooltip } from 'antd'
import { CheckCircle2 } from 'lucide-react'

interface CourseProgressBarProps {
  percent: number
  completedLessons?: number
  totalLessons?: number
  showText?: boolean
  size?: 'sm' | 'md' | 'lg'
  className?: string
}

const sizeMap = {
  sm: { stroke: 6, fontSize: 'text-xs' },
  md: { stroke: 8, fontSize: 'text-sm' },
  lg: { stroke: 10, fontSize: 'text-base' },
} as const

const antdSizeMap: Record<keyof typeof sizeMap, number> = {
  sm: 6,
  md: 8,
  lg: 10,
}

export const CourseProgressBar: React.FC<CourseProgressBarProps> = ({
  percent,
  completedLessons,
  totalLessons,
  showText = true,
  size = 'md',
  className,
}) => {
  const safe = Math.max(0, Math.min(100, Math.round(percent ?? 0)))
  const isComplete = safe >= 100
  const color = isComplete ? '#10b981' : '#4f46e5'

  return (
    <div className={`flex items-center gap-3 ${className ?? ''}`}>
      <Tooltip
        title={
          totalLessons
            ? `${completedLessons ?? 0}/${totalLessons} bài học`
            : `${safe}%`
        }
      >
        <Progress
          percent={safe}
          strokeColor={color}
          railColor="rgba(0,0,0,0.06)"
          size={antdSizeMap[size]}
          showInfo={false}
          className="!w-32 md:!w-40"
        />
      </Tooltip>
      {showText && (
        <div
          className={`flex items-center gap-1.5 font-bold text-indigo-700 ${sizeMap[size].fontSize}`}
        >
          {isComplete && <CheckCircle2 className="w-4 h-4 text-emerald-500" />}
          <span>{safe}%</span>
          {totalLessons != null && (
            <span className="text-gray-400 font-medium">
              ({completedLessons ?? 0}/{totalLessons})
            </span>
          )}
        </div>
      )}
    </div>
  )
}
