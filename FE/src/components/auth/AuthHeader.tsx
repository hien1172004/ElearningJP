import React from 'react'
import { BrandLogo } from '@/components/common/BrandLogo'

interface AuthHeaderProps {
  title: string
  subtitle?: string
  badge?: string
}

export const AuthHeader: React.FC<AuthHeaderProps> = ({
  title,
  subtitle,
  badge,
}) => {
  return (
    <div className="px-8 py-8 border-b border-gray-100">
      <div className="max-w-sm mx-auto space-y-4 text-center">
        <div>
          <BrandLogo size="lg" />
        </div>

        {badge && (
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-gray-100 text-gray-600 text-xs font-medium">
            {badge}
          </div>
        )}

        <h1 className="text-2xl font-bold text-gray-900">{title}</h1>
        {subtitle && (
          <p className="text-sm text-gray-500 leading-relaxed">{subtitle}</p>
        )}
      </div>
    </div>
  )
}
