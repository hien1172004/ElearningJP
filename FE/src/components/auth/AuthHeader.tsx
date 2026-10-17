import React from 'react'
import { Link } from 'react-router-dom'

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
        <Link
          to="/"
          className="inline-flex items-center gap-2 text-xl font-bold text-gray-900"
        >
          <span className="w-8 h-8 rounded bg-indigo-600 text-white flex items-center justify-center font-serif text-lg">
            日
          </span>
          <span>ElearningJP</span>
        </Link>

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
