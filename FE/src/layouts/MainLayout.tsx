import React from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Sidebar, TopBar } from '@/components/layout'
import { AuthModal } from '@/components/auth'
import { useSidebarStore } from '@/stores'
import { useNotificationSocket, useWebPush } from '@/hooks'
import { cn } from '@/utils/cn'

/**
 * MainLayout:
 *  - <TopBar /> fixed top, cao `--topbar-h` (56px, index.css) -> rộng full màn hình.
 *  - <Sidebar /> fixed left, top = `--topbar-h` -> bắt đầu dưới TopBar, không che header.
 *  - <main /> có `md:ml-60|md:ml-16` chừa chỗ cho Sidebar, và padding-top `--topbar-h` chừa chỗ cho TopBar.
 *  - Phần tử sticky trong trang dùng utility `sticky-below-topbar` / `sticky-panel` (index.css).
 */
export const MainLayout: React.FC = () => {
  const collapsed = useSidebarStore((s) => s.collapsed)
  const location = useLocation()

  // Mount 1 lần ở cấp layout: mở kết nối STOMP để nhận thông báo real-time.
  // Hook tự bỏ qua khi chưa đăng nhập và deactivate khi logout.
  useNotificationSocket()

  // Tự động đăng ký Web Push khi đã đăng nhập (guard bằng isAuthenticated bên trong).
  useWebPush()

  // Không hiển thị footer trên trang ôn tập sổ tay (Flashcard, Quizz, Speaking)
  const hideFooter =
    location.pathname.includes('/notebooks/') && location.pathname.includes('/review')

  return (
    <div className="min-h-screen bg-kinari text-sumi font-sans">
      {/* TopBar - full-width, fixed top */}
      <TopBar />

      {/* Sidebar - fixed left, dưới TopBar */}
      <Sidebar />

      {/* Main content - đẩy sang phải theo sidebar width, xuống dưới TopBar */}
      <div
        className={cn(
          'flex flex-col min-w-0 min-h-screen',
          'pt-(--topbar-h)', // chừa chỗ cho TopBar
          'transition-[margin] duration-300 ease-in-out',
          // Trên desktop: đẩy nội dung sang phải theo width sidebar
          collapsed ? 'md:ml-16' : 'md:ml-60',
        )}
      >
        <main
          className={cn(
            'flex-1 w-full',
            hideFooter
              ? 'max-w-[1650px] mx-auto px-3 sm:px-4 lg:px-5 py-2'
              : 'max-w-[1650px] mx-auto px-3 sm:px-4 lg:px-5 py-5',
          )}
        >
          <Outlet />
        </main>

        {!hideFooter && (
          <footer className="bg-surface border-t border-gray-200">
            <div className="max-w-[1920px] mx-auto px-3 sm:px-4 lg:px-5 py-6 text-center text-sm text-gray-500">
              © 2026 ElearningJP - Hệ thống học tiếng Nhật trực tuyến thông minh (SRS &amp; Mazii Dictionary)
            </div>
          </footer>
        )}
      </div>

      {/* Mount AuthModal 1 lần ở layout root để mở từ bất kỳ đâu */}
      <AuthModal />
    </div>
  )
}
