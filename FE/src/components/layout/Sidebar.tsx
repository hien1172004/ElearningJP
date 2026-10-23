import React, { useEffect, useState } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import {
  Search as SearchLucide,
  GraduationCap,
  BookOpen,
  Home,
  BarChart3,
  X,
} from 'lucide-react'
import { Tooltip } from 'antd'
import { useAuthStore, useSidebarStore } from '@/stores'
import { cn } from '@/utils/cn'

// ============================================================================
// Types
// ============================================================================

interface NavItem {
  to: string
  label: string
  icon: React.ReactNode
  match?: (pathname: string) => boolean
  /** Chỉ hiện cho user đã đăng nhập (vd: tiến độ học tập). */
  requiresAuth?: boolean
}

const isActiveOrStartsWith = (prefix: string) => (pathname: string) =>
  pathname === prefix || pathname.startsWith(`${prefix}/`)

const navItems: NavItem[] = [
  { to: '/', label: 'Trang chủ', icon: <Home className="w-5 h-5" /> },
  {
    to: '/dictionary',
    label: 'Tra từ điển',
    icon: <SearchLucide className="w-5 h-5" />,
    match: isActiveOrStartsWith('/dictionary'),
  },
  {
    to: '/courses',
    label: 'Khóa học',
    icon: <BookOpen className="w-5 h-5" />,
    match: isActiveOrStartsWith('/courses'),
  },
  {
    to: '/progress',
    label: 'Tiến độ',
    icon: <BarChart3 className="w-5 h-5" />,
    match: isActiveOrStartsWith('/progress'),
    requiresAuth: true,
  },
  {
    to: '/teacher/courses',
    label: 'Quản lý',
    icon: <GraduationCap className="w-5 h-5" />,
    match: isActiveOrStartsWith('/teacher'),
  },
]

// Chiều cao header (phải khớp với <TopBar />): h-14 = 56px
// (dùng Tailwind class `top-14` để JIT tạo class tương ứng)

// ============================================================================
// Hooks
// ============================================================================

/** Trả về true nếu viewport <= 768px. SSR-safe. */
function useIsMobile(): boolean {
  const [isMobile, setIsMobile] = useState<boolean>(() => {
    if (typeof window === 'undefined') return false
    return window.matchMedia('(max-width: 768px)').matches
  })

  useEffect(() => {
    if (typeof window === 'undefined') return
    const mql = window.matchMedia('(max-width: 768px)')
    const handler = (e: MediaQueryListEvent) => setIsMobile(e.matches)
    mql.addEventListener('change', handler)
    setIsMobile(mql.matches)
    return () => mql.removeEventListener('change', handler)
  }, [])

  return isMobile
}

// ============================================================================
// Sub-components
// ============================================================================

interface NavItemRowProps {
  item: NavItem
  collapsed: boolean
  onNavigate?: () => void
}

const NavItemRow: React.FC<NavItemRowProps> = ({ item, collapsed, onNavigate }) => {
  const location = useLocation()
  const isActive = item.match
    ? item.match(location.pathname)
    : location.pathname === item.to

  const linkClasses = cn(
    'flex items-center gap-3 rounded-md text-sm font-semibold transition',
    'px-3 py-2.5',
    collapsed && 'justify-center px-0',
    isActive
      ? 'text-indigo-600 bg-indigo-50/80 font-bold'
      : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-50',
  )

  const link = (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      onClick={onNavigate}
      className={linkClasses}
    >
      <span className="shrink-0 inline-flex items-center justify-center">
        {item.icon}
      </span>
      {!collapsed && <span className="truncate">{item.label}</span>}
    </NavLink>
  )

  if (collapsed) {
    return (
      <Tooltip title={item.label} placement="right">
        {link}
      </Tooltip>
    )
  }
  return link
}

// ============================================================================
// Main Sidebar component
// ============================================================================

export const Sidebar: React.FC = () => {
  const collapsed = useSidebarStore((s) => s.collapsed)
  const mobileOpen = useSidebarStore((s) => s.mobileOpen)
  const closeMobile = useSidebarStore((s) => s.closeMobile)

  const user = useAuthStore((s) => s.user)
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)

  const location = useLocation()
  const isMobile = useIsMobile()

  const isTeacherOrAdmin =
    user?.role === 'TEACHER' ||
    user?.role === 'ADMIN' ||
    user?.roles?.some((r) => r === 'TEACHER' || r === 'ADMIN')

  // Đóng drawer mobile khi chuyển route
  useEffect(() => {
    closeMobile()
  }, [location.pathname, closeMobile])

  // Khoá scroll body khi drawer mobile mở
  useEffect(() => {
    if (!isMobile) return
    if (mobileOpen) {
      const prev = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      return () => {
        document.body.style.overflow = prev
      }
    }
    return
  }, [mobileOpen, isMobile])

  // Trên mobile: full-height drawer bắt đầu từ top=0
  // Trên desktop: bắt đầu từ dưới header (top-14 = 56px)
  const sidebarClasses = cn(
    'flex flex-col bg-white border-r border-gray-200 z-40',
    'transition-[width,transform] duration-300 ease-in-out',
    'fixed left-0',
    isMobile
      ? cn(
          'inset-y-0',
          'w-72',
          mobileOpen ? 'translate-x-0' : '-translate-x-full',
        )
      : cn(
          'top-14 bottom-0', // đúng chiều cao TopBar (h-14 = 56px)
          collapsed ? 'w-16' : 'w-60',
          'translate-x-0',
        ),
  )

  // Items hiển thị
  const visibleNavItems = navItems.filter((item) => {
    // Chỉ hiện "Quản lý" cho TEACHER/ADMIN
    if (item.to === '/teacher/courses') return isTeacherOrAdmin
    // Chỉ hiện "Tiến độ" khi đã đăng nhập
    if (item.requiresAuth) return isAuthenticated
    return true
  })

  return (
    <>
      {/* Backdrop cho mobile drawer */}
      {isMobile && (
        <div
          onClick={closeMobile}
          className={cn(
            'fixed inset-0 bg-black/40 z-30 md:hidden transition-opacity duration-300',
            mobileOpen ? 'opacity-100' : 'opacity-0 pointer-events-none',
          )}
          aria-hidden="true"
        />
      )}

      <aside className={sidebarClasses} aria-label="Sidebar chính">
        {/* Top của sidebar — chỉ hiện trên mobile (chứa logo + nút đóng) */}
        {isMobile && (
          <div className="relative flex items-center h-14 border-b border-gray-200 shrink-0 px-4">
            <Link
              to="/"
              className="flex items-center gap-2 text-lg font-black text-indigo-600"
              onClick={closeMobile}
            >
              <span className="w-8 h-8 rounded-md bg-indigo-600 text-white flex items-center justify-center font-serif text-base font-bold shadow-xs">
                日
              </span>
              <span>ElearningJP</span>
            </Link>
            <button
              type="button"
              onClick={closeMobile}
              aria-label="Đóng menu"
              className={cn(
                'absolute top-2 right-2 inline-flex items-center justify-center',
                'w-9 h-9 rounded-md text-gray-500 hover:text-rose-600 hover:bg-rose-50 transition',
              )}
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        )}

        {/* ===================== Nav items ===================== */}
        <nav className="flex-1 overflow-y-auto py-3 px-3 space-y-1">
          {visibleNavItems.map((item) => (
            <NavItemRow
              key={item.to}
              item={item}
              collapsed={collapsed}
              onNavigate={isMobile ? closeMobile : undefined}
            />
          ))}
        </nav>

        {/* Spacer đáy (user/auth đã chuyển lên TopBar) */}
        <div className="shrink-0 border-t border-gray-200 p-2" />
      </aside>
    </>
  )
}