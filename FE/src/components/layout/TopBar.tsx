import React from 'react'
import { Link } from 'react-router-dom'
import {
  Menu,
  LogIn,
  LogOut,
  User as UserIcon,
  UserPlus,
  PanelLeftClose,
  PanelLeftOpen,
} from 'lucide-react'
import {
  useAuthStore,
  useAuthModalStore,
  useSidebarStore,
} from '@/stores'
import { useLogout } from '@/hooks'
import { cn } from '@/utils/cn'

/**
 * Header ngang, dùng cho cả desktop và mobile.
 *
 * - Logo (bên trái) -> /
 * - Nút toggle / hamburger để mở sidebar (bên trái, cạnh logo)
 * - Bên phải: tài khoản + đăng xuất (hoặc đăng nhập / đăng ký)
 *
 * Phần điều hướng chính (Trang chủ, Tra từ điển, Khóa học, Quản lý)
 * đã được chuyển vào Sidebar — Header không chứa nav.
 *
 * Sidebar bắt đầu từ dưới header này (top: h-14 = 56px).
 */
export const TopBar: React.FC = () => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const user = useAuthStore((s) => s.user)
  const { mutate: logout } = useLogout()
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const openRegister = useAuthModalStore((s) => s.openRegister)

  const collapsed = useSidebarStore((s) => s.collapsed)
  const toggle = useSidebarStore((s) => s.toggle)
  const setMobileOpen = useSidebarStore((s) => s.setMobileOpen)

  return (
    <header className="fixed top-0 inset-x-0 z-50 bg-white/90 backdrop-blur-md border-b border-gray-200/80 h-14">
      <div className="h-14 flex items-center justify-between gap-2 pl-0 pr-3 sm:pr-4 lg:pr-5">
        {/* Trái: hamburger/toggle + logo */}
        <div className="flex items-center gap-2 sm:gap-3 shrink-0">
          {/* Hamburger — chỉ hiện trên mobile (mở drawer overlay) */}
          <button
            type="button"
            onClick={() => setMobileOpen(true)}
            aria-label="Mở menu"
            className="md:hidden inline-flex items-center justify-center w-9 h-9 rounded-md text-gray-700 hover:bg-gray-100 transition shrink-0"
          >
            <Menu className="w-5 h-5" />
          </button>

          {/* Toggle sidebar — chỉ hiện trên desktop.
              Inline marginLeft để icon của button thẳng hàng với icon nav bên dưới. */}
          <TooltipToggle collapsed={collapsed} onToggle={toggle} />

          <Link
            to="/"
            className="text-base sm:text-lg md:text-xl font-black text-indigo-600 flex items-center gap-2 shrink-0 ml-1"
          >
            <span className="w-8 h-8 rounded-md bg-indigo-600 text-white flex items-center justify-center font-serif text-base font-bold shadow-xs shrink-0">
              日
            </span>
            <span className="hidden sm:inline">ElearningJP</span>
          </Link>
        </div>

        {/* Phải: tài khoản + đăng xuất (hoặc đăng nhập / đăng ký) */}
        <div className="flex items-center gap-2 shrink-0">
          {isAuthenticated ? (
            <>
              <Link
                to="/profile"
                className="inline-flex items-center gap-2 text-sm font-semibold px-3 py-2 rounded-md text-gray-700 hover:bg-gray-50 max-w-[160px] truncate"
                title="Tài khoản"
              >
                <UserIcon className="w-4 h-4 shrink-0" />
                <span className="truncate">
                  {user?.fullName || user?.email || 'Tài khoản'}
                </span>
              </Link>
              <button
                type="button"
                onClick={() => logout()}
                className={cn(
                  'inline-flex items-center gap-1.5',
                  'text-sm font-semibold px-3 py-2',
                  'bg-rose-50 text-rose-700 rounded-md hover:bg-rose-100 transition',
                )}
                title="Đăng xuất"
              >
                <LogOut className="w-4 h-4" />
                <span className="hidden sm:inline">Đăng xuất</span>
              </button>
            </>
          ) : (
            <>
              <button
                type="button"
                onClick={() => openLogin()}
                className={cn(
                  'inline-flex items-center gap-1.5',
                  'text-sm font-semibold px-3 py-2',
                  'text-indigo-700 bg-indigo-50/60 rounded-md hover:bg-indigo-100 transition',
                )}
              >
                <LogIn className="w-4 h-4" />
                <span className="hidden sm:inline">Đăng nhập</span>
              </button>
              <button
                type="button"
                onClick={() => openRegister()}
                className={cn(
                  'inline-flex items-center gap-1.5',
                  'text-sm font-semibold px-4 py-2',
                  'bg-indigo-600 text-white rounded-md hover:bg-indigo-700 shadow-xs',
                  'active:scale-[0.98] transition',
                )}
              >
                <UserPlus className="w-4 h-4" />
                <span>Đăng ký</span>
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}

/**
 * Nút toggle sidebar (chỉ hiển thị trên md+).
 *
 * - Khi sidebar đang collapsed: hiện PanelLeftOpen (mở rộng)
 * - Khi sidebar đang expanded: hiện PanelLeftClose (thu lại)
 */
const TooltipToggle: React.FC<{
  collapsed: boolean
  onToggle: () => void
}> = ({ collapsed, onToggle }) => {
  // Căn thẳng hàng icon trong nút toggle với icon trong các nav-row của sidebar:
  // - Sidebar collapsed: width = 64px (w-16), icon w-5 (20px) ở giữa -> tâm icon = 32px.
  //   Button w-9 (36px) có tâm = 18px -> cần margin-left = 14px để tâm trùng.
  // - Sidebar expanded: width = 240px (w-60), icon cách mép trái sidebar = 20px
  //   (px-2 của nav + px-3 của row). Button w-9 có tâm = 18px -> cần margin-left
  //   = 20 - 18 = 2px để tâm icon trùng tâm icon nav.
  const marginLeft = 14

  return (
    <button
      type="button"
      onClick={onToggle}
      aria-label={collapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
      title={collapsed ? 'Mở rộng sidebar' : 'Thu gọn sidebar'}
      style={{ marginLeft }}
      className={cn(
        'hidden md:inline-flex items-center justify-center',
        'w-9 h-9 rounded-md text-gray-600 hover:text-indigo-600 hover:bg-gray-100 transition shrink-0',
        'focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500',
      )}
    >
      {collapsed ? (
        <PanelLeftOpen className="w-5 h-5" />
      ) : (
        <PanelLeftClose className="w-5 h-5" />
      )}
    </button>
  )
}