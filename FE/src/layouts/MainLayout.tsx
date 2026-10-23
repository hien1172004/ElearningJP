import React from 'react'
import { Outlet } from 'react-router-dom'
import { Sidebar, TopBar } from '@/components/layout'
import { useSidebarStore } from '@/stores'
import { AuthModal } from '@/components/auth'
import { cn } from '@/utils/cn'

/**
 * MainLayout:
 *  - <TopBar /> ở root, `fixed top-0 inset-x-0` -> rộng full màn hình.
 *  - <Sidebar /> fixed left, `top-14` -> bắt đầu dưới TopBar, không che header.
 *  - <main /> có `md:ml-60|md:ml-16` chừa chỗ cho Sidebar, và `pt-14` chừa chỗ cho TopBar.
 */
export const MainLayout: React.FC = () => {
  const collapsed = useSidebarStore((s) => s.collapsed)

  return (
    <div className="min-h-screen bg-slate-50 text-gray-900 font-sans">
      {/* TopBar — full-width, fixed top */}
      <TopBar />

      {/* Sidebar — fixed left, dưới TopBar */}
      <Sidebar />

      {/* Main content — được đẩy sang phải theo sidebar width, xuống dưới TopBar */}
      <div
        className={cn(
          'flex flex-col min-w-0 min-h-screen',
          'pt-14', // chừa chỗ cho TopBar (h-14 = 56px)
          'transition-[margin] duration-300 ease-in-out',
          // Trên desktop: đẩy nội dung sang phải theo width sidebar
          collapsed ? 'md:ml-16' : 'md:ml-60',
        )}
      >
        <main className="flex-1 w-full max-w-[1650px] mx-auto px-3 sm:px-4 lg:px-5 py-5">
          <Outlet />
        </main>

        <footer className="bg-white border-t border-gray-200">
          <div className="max-w-[1920px] mx-auto px-3 sm:px-4 lg:px-5 py-6 text-center text-sm text-gray-500">
            © 2026 ElearningJP - Hệ thống học tiếng Nhật trực tuyến thông minh
            (SRS &amp; Mazii Dictionary)
          </div>
        </footer>
      </div>

      {/* Mount AuthModal 1 lần ở layout root để mở từ bất kỳ đâu */}
      <AuthModal />
    </div>
  )
}
