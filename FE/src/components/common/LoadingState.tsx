import React from 'react'
import { Loader2 } from 'lucide-react'

interface LoadingStateProps {
  message?: string
  className?: string
  size?: 'sm' | 'md' | 'lg'
  fullHeight?: boolean
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  message = 'Đang tải...',
  className,
  size = 'md',
  fullHeight = false,
}) => {
  const sizeMap = {
    sm: 'w-5 h-5',
    md: 'w-8 h-8',
    lg: 'w-12 h-12',
  } as const
  return (
    <div
      className={`flex flex-col items-center justify-center gap-3 ${
        fullHeight ? 'min-h-[60vh]' : 'py-12'
      } ${className ?? ''}`}
    >
      <Loader2 className={`${sizeMap[size]} text-indigo-600 animate-spin`} />
      {message && (
        <p className="text-sm font-medium text-gray-500 animate-pulse">
          {message}
        </p>
      )}
    </div>
  )
}
