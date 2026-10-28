import React, { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import {
  Search as SearchLucide,
  GraduationCap,
  BookOpen,
  Home,
  BarChart3,
  X,
  Award,
  ChevronDown,
  ChevronRight,
  FileText,
} from 'lucide-react'
import { Tooltip, Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import { useAuthStore, useSidebarStore } from '@/stores'
import { hasRole } from '@/utils/roles'
import { cn } from '@/utils/cn'

// ============================================================================
// Types
// ============================================================================

/** Một mục con (nằm trong nhóm có thể mở rộng, vd: "Quản lý"). */
interface NavChildItem {
  to: string
  label: string
  icon?: React.ReactNode
  match?: (pathname: string) => boolean
}

interface NavItem {
  to: string
  label: string
  icon: React.ReactNode
  match?: (pathname: string) => boolean
  /** Chỉ hiện cho user đã đăng nhập (vd: tiến độ học tập). */
  requiresAuth?: boolean
  /** Chỉ hiện cho user có ít nhất 1 role trong danh sách (vd: ADMIN/TEACHER). */
  requiresRoles?: readonly string[]
  /** Danh sách mục con — nếu có, mục này trở thành nhóm có thể mở rộng. */
  children?: NavChildItem[]
}

const isActiveOrStartsWith = (prefix: string) => (pathname: string) =>
  pathname === prefix || pathname.startsWith(`${prefix}/`)

const isAdminArea = (pathname: string) =>
  isActiveOrStartsWith('/teacher')(pathname) ||
  isActiveOrStartsWith('/admin')(pathname)

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
    to: '/exams',
    label: 'Thi thử JLPT',
    icon: <Award className="w-5 h-5" />,
    match: isActiveOrStartsWith('/exams'),
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
    match: isAdminArea,
    requiresRoles: ['ADMIN', 'TEACHER'],
    children: [
      {
        to: '/teacher/courses',
        label: 'Khóa học',
        icon: <BookOpen className="w-4 h-4" />,
        match: isActiveOrStartsWith('/teacher/courses'),
      },
      {
        to: '/admin/exams',
        label: 'Đề thi',
        icon: <FileText className="w-4 h-4" />,
        match: isActiveOrStartsWith('/admin'),
      },
    ],
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
    // Đồng bộ lại ngay để khớp với viewport hiện tại (state khởi tạo có thể
    // đã cũ nếu viewport đổi giữa lúc render và lúc effect chạy).
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
  /** Nhóm đang mở rộng (chỉ dùng cho item có children). */
  groupOpen: boolean
  onToggleGroup: () => void
  onNavigate?: () => void
}

/**
 * Một dòng nav. Nếu item có `children` thì:
 *  - Desktop mở rộng -> hiện nút bấm toggle + danh sách mục con thụt lề.
 *  - Desktop thu gọn -> hiện icon, bấm ra menu dropdown chứa các mục con.
 */
const NavItemRow: React.FC<NavItemRowProps> = ({
  item,
  collapsed,
  groupOpen,
  onToggleGroup,
  onNavigate,
}) => {
  const location = useLocation()
  const navigate = useNavigate()

  const hasChildren = !!item.children?.length

  const isItemActive = item.match
    ? item.match(location.pathname)
    : location.pathname === item.to

  const isChildActive = (child: NavChildItem) =>
    child.match ? child.match(location.pathname) : location.pathname === child.to

  // Nhóm được coi là "active" khi bản thân nó hoặc bất kỳ mục con nào active.
  const isGroupActive = isItemActive || (item.children?.some(isChildActive) ?? false)

  // ---------------------- Item KHÔNG có children ----------------------
  if (!hasChildren) {
    const linkClasses = cn(
      'flex items-center gap-3 rounded-md text-sm font-semibold transition',
      'px-3 py-2.5',
      collapsed && 'justify-center px-0',
      isItemActive
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

  // ---------------------- Item CÓ children ----------------------

  // Thu gọn: icon + dropdown menu chứa các mục con.
  if (collapsed) {
    const menuItems: MenuProps['items'] = (item.children ?? []).map((child) => ({
      key: child.to,
      icon: child.icon,
      label: child.label,
    }))

    return (
      <Tooltip title={item.label} placement="right">
        <span className="block w-full">
          <Dropdown
            menu={{
              items: menuItems,
              onClick: ({ key }) => {
                navigate(key)
                onNavigate?.()
              },
            }}
            placement="rightTop"
            trigger={['click']}
          >
            <button
              type="button"
              aria-label={item.label}
              aria-haspopup="menu"
              aria-expanded={groupOpen}
              className={cn(
                'flex items-center justify-center w-full rounded-md transition',
                'px-0 py-2.5',
                isGroupActive
                  ? 'text-indigo-600 bg-indigo-50/80 font-bold'
                  : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-50',
              )}
            >
              <span className="shrink-0 inline-flex items-center justify-center">
                {item.icon}
              </span>
            </button>
          </Dropdown>
        </span>
      </Tooltip>
    )
  }

  // Mở rộng: nút toggle + danh sách mục con.
  const toggleClasses = cn(
    'flex items-center gap-3 rounded-md text-sm font-semibold transition w-full',
    'px-3 py-2.5',
    isGroupActive
      ? 'text-indigo-600 bg-indigo-50/80 font-bold'
      : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-50',
  )

  return (
    <div>
      <button
        type="button"
        onClick={onToggleGroup}
        aria-expanded={groupOpen}
        className={toggleClasses}
      >
        <span className="shrink-0 inline-flex items-center justify-center">
          {item.icon}
        </span>
        <span className="truncate flex-1 text-left">{item.label}</span>
        <span className="shrink-0 inline-flex items-center justify-center">
          {groupOpen ? (
            <ChevronDown className="w-4 h-4" />
          ) : (
            <ChevronRight className="w-4 h-4" />
          )}
        </span>
      </button>

      {groupOpen && (
        <div className="mt-1 ml-4 pl-3 border-l border-gray-200 space-y-0.5">
          {(item.children ?? []).map((child) => {
            const childActive = isChildActive(child)
            return (
              <NavLink
                key={child.to}
                to={child.to}
                onClick={onNavigate}
                className={cn(
                  'flex items-center gap-2.5 rounded-md text-xs font-semibold transition',
                  'px-2.5 py-2',
                  childActive
                    ? 'text-indigo-600 bg-indigo-50/80 font-bold'
                    : 'text-gray-500 hover:text-indigo-600 hover:bg-gray-50',
                )}
              >
                {child.icon && (
                  <span className="shrink-0 inline-flex items-center justify-center">
                    {child.icon}
                  </span>
                )}
                <span className="truncate">{child.label}</span>
              </NavLink>
            )
          })}
        </div>
      )}
    </div>
  )
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

  // Trạng thái mở/đóng các nhóm (key = item.to).
  // `undefined` = chưa người dùng chỉ định -> dùng trạng thái auto (mở khi mục con active).
  const [groupStates, setGroupStates] = useState<Record<string, boolean>>({})

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
    if (item.requiresRoles?.length) return hasRole(user, item.requiresRoles)
    // Chỉ hiện "Tiến độ" khi đã đăng nhập
    if (item.requiresAuth) return isAuthenticated
    return true
  })

  /** Nhóm có đang active (bản thân hoặc mục con) hay không. */
  const isGroupActive = (item: NavItem): boolean => {
    const selfActive = item.match
      ? item.match(location.pathname)
      : location.pathname === item.to
    if (selfActive) return true
    return (
      item.children?.some((child) =>
        child.match ? child.match(location.pathname) : location.pathname === child.to,
      ) ?? false
    )
  }

  const toggleGroup = (key: string) => {
    setGroupStates((prev) => ({ ...prev, [key]: !prev[key] }))
  }

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
          {visibleNavItems.map((item) => {
            const groupOpen = groupStates[item.to] ?? isGroupActive(item)
            return (
              <NavItemRow
                key={item.to}
                item={item}
                collapsed={collapsed}
                groupOpen={groupOpen}
                onToggleGroup={() => toggleGroup(item.to)}
                onNavigate={isMobile ? closeMobile : undefined}
              />
            )
          })}
        </nav>

        {/* Spacer đáy (user/auth đã chuyển lên TopBar) */}
        <div className="shrink-0 border-t border-gray-200 p-2" />
      </aside>
    </>
  )
}
