import React from 'react'
import { Outlet, Link } from 'react-router-dom'

export const MainLayout: React.FC = () => {
  return (
    <div className="min-h-screen flex flex-col bg-gray-50 text-gray-900">
      <header className="bg-white border-b border-gray-200 px-6 py-4 flex items-center justify-between">
        <Link to="/" className="text-xl font-bold text-indigo-600">
          ElearningJP
        </Link>
        <nav className="flex items-center gap-4">
          <Link to="/" className="text-sm font-medium hover:text-indigo-600">
            Trang chủ
          </Link>
          <Link
            to="/login"
            className="text-sm font-medium px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
          >
            Đăng nhập
          </Link>
        </nav>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto p-6">
        <Outlet />
      </main>

      <footer className="bg-white border-t border-gray-200 py-6 text-center text-sm text-gray-500">
        © 2026 ElearningJP - Hệ thống học tiếng Nhật trực tuyến
      </footer>
    </div>
  )
}
