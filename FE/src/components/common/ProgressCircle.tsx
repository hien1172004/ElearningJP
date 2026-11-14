import React from 'react'
import { Progress } from 'antd'

interface ProgressCircleProps {
  percent: number
  /** Đường kính (px). */
  size?: number
  className?: string
}

/** Vòng tròn tiến độ nhỏ, hiển thị % ở giữa (xanh lá khi đạt 100%). Màu giống CourseProgressBar. */
export const ProgressCircle: React.FC<ProgressCircleProps> = ({ percent, size = 36, className }) => {
  const safe = Math.max(0, Math.min(100, Math.round(percent ?? 0)))
  return (
    <Progress
      type="circle"
      size={size}
      percent={safe}
      strokeWidth={9}
      strokeColor={safe >= 100 ? '#10b981' : '#2e5c8a'}
      // Cỡ chữ theo đường kính để "100%" vẫn nằm gọn trong vòng nhỏ
      format={(p) => (
        <span className="font-bold text-gray-700" style={{ fontSize: Math.max(8, Math.round(size * 0.26)) }}>
          {p}%
        </span>
      )}
      aria-label={`Tiến độ ${safe}%`}
      className={`shrink-0 ${className ?? ''}`}
    />
  )
}
