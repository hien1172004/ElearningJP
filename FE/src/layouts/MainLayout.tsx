import React from 'react'
import { Outlet, Link, useLocation } from 'react-router-dom'
import { Search, LogIn } from 'lucide-react'

export const MainLayout: React.FC = () => {
  const location = useLocation()

  return (
    <div className="min-h-screen flex flex-col bg-slate-50 text-gray-900 font-sans">
      <header className="bg-white/90 backdrop-blur-md sticky top-0 z-40 border-b border-gray-200/80 px-6 py-3.5 flex items-center justify-between">
        <div className="flex items-center gap-8">
          <Link to="/" className="text-xl font-black text-indigo-600 flex items-center gap-2">
            <span className="w-8 h-8 rounded-xl bg-indigo-600 text-white flex items-center justify-center font-serif text-lg font-bold shadow-xs">
              日
            </span>
            <span>ElearningJP</span>
          </Link>

          <nav className="hidden md:flex items-center gap-1 text-sm font-semibold text-gray-600">
            <Link
              to="/"
              className={`px-3 py-2 rounded-xl transition ${
                location.pathname === '/'
                  ? 'text-indigo-600 bg-indigo-50/80 font-bold'
                  : 'hover:text-indigo-600 hover:bg-gray-50'
              }`}
            >
              Trang chủ
            </Link>
            <Link
              to="/dictionary"
              className={`px-3 py-2 rounded-xl transition flex items-center gap-1.5 ${
                location.pathname.startsWith('/dictionary')
                  ? 'text-indigo-600 bg-indigo-50/80 font-bold'
                  : 'hover:text-indigo-600 hover:bg-gray-50'
              }`}
            >
              <Search className="w-4 h-4" />
              <span>Tra từ điển</span>
            </Link>
          </nav>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/dictionary"
            className="md:hidden p-2 text-gray-600 hover:text-indigo-600 hover:bg-gray-100 rounded-xl"
            title="Tra từ điển"
          >
            <Search className="w-5 h-5" />
          </Link>
          <Link
            to="/login"
            className="text-sm font-semibold px-4 py-2 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 shadow-xs hover:shadow-sm active:scale-98 transition flex items-center gap-1.5"
          >
            <LogIn className="w-4 h-4" />
            <span>Đăng nhập</span>
          </Link>
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto p-4 md:p-6">
        <Outlet />
      </main>

      <footer className="bg-white border-t border-gray-200 py-6 text-center text-sm text-gray-500">
        © 2026 ElearningJP - Hệ thống học tiếng Nhật trực tuyến thông minh (SRS & Mazii Dictionary)
      </footer>
    </div>
  )
}
