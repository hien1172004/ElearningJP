import React from 'react'
import { Inbox } from 'lucide-react'

interface EmptyStateProps {
  icon?: React.ReactNode
  title: string
  description?: string
  action?: React.ReactNode
  className?: string
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  icon,
  title,
  description,
  action,
  className,
}) => {
  return (
    <div
      className={`flex flex-col items-center justify-center py-16 px-4 text-center ${
        className ?? ''
      }`}
    >
      <div className="w-16 h-16 rounded-md bg-gray-100 text-gray-400 flex items-center justify-center mb-4">
        {icon ?? <Inbox className="w-8 h-8" />}
      </div>
      <h3 className="text-base font-bold text-gray-800">{title}</h3>
      {description && (
        <p className="text-sm text-gray-500 mt-1.5 max-w-md">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
