import React from 'react'

interface AuthFooterProps {
  children?: React.ReactNode
  note?: string
}

export const AuthFooter: React.FC<AuthFooterProps> = ({ children, note }) => {
  return (
    <div className="text-center pt-6 border-t border-gray-100">
      {children}
      {note && <p className="text-xs text-gray-400 mt-2">{note}</p>}
    </div>
  )
}
