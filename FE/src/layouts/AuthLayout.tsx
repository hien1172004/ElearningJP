import React from 'react'
import { Outlet, Link } from 'react-router-dom'

export const AuthLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100 p-4">
      <div className="w-full max-w-md bg-white rounded-2xl shadow-sm border border-gray-200 p-8">
        <div className="text-center mb-6">
          <Link to="/" className="text-2xl font-bold text-indigo-600">
            ElearningJP
          </Link>
          <p className="text-sm text-gray-500 mt-1">Học và luyện thi tiếng Nhật mỗi ngày</p>
        </div>
        <Outlet />
      </div>
    </div>
  )
}
