import { useBodyScrollLock } from '@/hooks/useBodyScrollLock'
import React, { useEffect, useState } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import {
  Search as SearchLucide,
  ClipboardCheck,
  BookOpen,
  Bookmark,
  Home,
  BarChart3,
  X,
  Award,
  FileText,
  Mic,
  Layers,
  Users,
  PieChart,
  ShieldAlert,
  Megaphone,
  Crown,
  ArrowLeftRight,
} from 'lucide-react'
import { Tooltip } from 'antd'
import { BrandLogo } from '@/components/common/BrandLogo'
import { useAuthStore, useSidebarStore } from '@/stores'
import { isAdmin, isTeacherOrAdmin } from '@/utils/roles'
import { cn } from '@/utils/cn'
import { useSrsStats } from '@/hooks/useSrsStats'

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
  /** Badge hiển thị cạnh nhãn (vd: số thẻ SRS đến hạn). */
  badge?: React.ReactNode
}

const isActiveOrStartsWith = (prefix: string) => (pathname: string) =>
  pathname === prefix || pathname.startsWith(`${prefix}/`)

// Trang soạn khóa học có 2 đường dẫn: /teacher/courses và /admin/courses
const isCourseManageArea = (pathname: string) =>
  isActiveOrStartsWith('/teacher/courses')(pathname) ||
  isActiveOrStartsWith('/admin/courses')(pathname)

/** Menu học viên / khách. */
const studentNav: NavItem[] = [
  { to: '/', label: 'Trang chủ', icon: <Home className="w-5 h-5" /> },
  {
    to: '/dictionary',
    label: 'Tra từ điển',
    icon: <SearchLucide className="w-5 h-5" />,
    match: isActiveOrStartsWith('/dictionary'),
  },
  {
    to: '/notebooks',
    label: 'Sổ tay từ vựng',
    icon: <Bookmark className="w-5 h-5" />,
    match: isActiveOrStartsWith('/notebooks'),
    requiresAuth: true,
  },
  {
    to: '/courses',
    label: 'Khóa học',
    icon: <BookOpen className="w-5 h-5" />,
    match: isActiveOrStartsWith('/courses'),
  },
  {
    to: '/speaking',
    label: 'Luyện hội thoại',
    icon: <Mic className="w-5 h-5" />,
    match: isActiveOrStartsWith('/speaking'),
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
    to: '/srs',
    label: 'Ôn tập SRS',
    icon: <Layers className="w-5 h-5" />,
    match: isActiveOrStartsWith('/srs'),
    requiresAuth: true,
  },
]

// Các mục quản lý dùng chung cho TEACHER & ADMIN (BE tự giới hạn phạm vi dữ liệu theo role).
const sharedStaffNav: NavItem[] = [
  {
    to: '/admin/exams',
    label: 'Đề thi',
    icon: <FileText className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/exams'),
  },
  {
    to: '/admin/scenarios',
    label: 'Kịch bản luyện nói',
    icon: <Mic className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/scenarios'),
  },
  {
    to: '/admin/comments',
    label: 'Kiểm duyệt bình luận',
    icon: <ShieldAlert className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/comments'),
  },
  {
    to: '/admin/broadcasts',
    label: 'Gửi thông báo',
    icon: <Megaphone className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/broadcasts'),
  },
]

/** Menu giáo viên. */
const teacherNav: NavItem[] = [
  {
    to: '/teacher/courses',
    label: 'Khóa học của tôi',
    icon: <BookOpen className="w-5 h-5" />,
    match: isCourseManageArea,
  },
  ...sharedStaffNav,
]

/** Menu quản trị viên. */
const adminNav: NavItem[] = [
  {
    to: '/admin/stats',
    label: 'Thống kê',
    icon: <PieChart className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/stats'),
  },
  {
    to: '/admin/course-reviews',
    label: 'Hàng chờ duyệt',
    icon: <ClipboardCheck className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/course-reviews'),
  },
  {
    to: '/teacher/courses',
    label: 'Khóa học',
    icon: <BookOpen className="w-5 h-5" />,
    match: isCourseManageArea,
  },
  ...sharedStaffNav,
  {
    to: '/admin/users',
    label: 'Người dùng',
    icon: <Users className="w-5 h-5" />,
    match: isActiveOrStartsWith('/admin/users'),
  },
]

// Chiều cao header dùng biến CSS --topbar-h (index.css) — khớp với <TopBar />

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
  onNavigate?: () => void
}

/** Một dòng nav; khi sidebar thu gọn chỉ hiện icon kèm tooltip. */
const NavItemRow: React.FC<NavItemRowProps> = ({ item, collapsed, onNavigate }) => {
  const location = useLocation()

  const isItemActive = item.match
    ? item.match(location.pathname)
    : location.pathname === item.to

  const link = (
    <NavLink
      to={item.to}
      end={item.to === '/'}
      onClick={onNavigate}
      className={cn(
        'flex items-center gap-3 rounded-md text-sm font-semibold transition',
        'px-3 py-2.5',
        collapsed && 'justify-center px-0',
        isItemActive
          ? 'text-indigo-600 bg-indigo-50/80 font-bold'
          : 'text-gray-600 hover:text-indigo-600 hover:bg-gray-50',
      )}
    >
      <span className="shrink-0 relative inline-flex items-center justify-center">
        {item.icon}
        {collapsed && item.badge && (
          <span className="absolute -top-1 -right-1 w-2 h-2 rounded-full bg-indigo-600" />
        )}
      </span>
      {!collapsed && <span className="truncate flex-1">{item.label}</span>}
      {!collapsed && item.badge}
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
  const staff = isTeacherOrAdmin(user)
  // Chỉ học viên mới cần số thẻ SRS đến hạn
  const { data: srsStats } = useSrsStats({ enabled: !staff })
  const dueCount = srsStats?.dueTodayCount ?? 0

  const location = useLocation()
  const isMobile = useIsMobile()

  // Đóng drawer mobile khi chuyển route
  useEffect(() => {
    closeMobile()
  }, [location.pathname, closeMobile])

  // Khoá scroll body khi drawer mobile mở
  useBodyScrollLock(isMobile && mobileOpen)

  // Trên mobile: full-height drawer bắt đầu từ top=0
  // Trên desktop: bắt đầu từ dưới header (top = --topbar-h)
  const sidebarClasses = cn(
    'flex flex-col bg-surface border-r border-gray-200 z-40',
    'transition-[width,transform] duration-300 ease-in-out',
    'fixed left-0',
    isMobile
      ? cn(
          'inset-y-0',
          'w-72',
          mobileOpen ? 'translate-x-0' : '-translate-x-full',
        )
      : cn(
          'top-(--topbar-h) bottom-0', // đúng chiều cao TopBar
          collapsed ? 'w-16' : 'w-60',
          'translate-x-0',
        ),
  )

  // Mỗi role một menu riêng, ưu tiên ADMIN > TEACHER > học viên/khách.
  const baseNav = isAdmin(user) ? adminNav : staff ? teacherNav : studentNav
  const visibleNavItems = baseNav
    .filter((item) => !item.requiresAuth || isAuthenticated)
    .map((item) =>
      // Badge số thẻ SRS đến hạn trên mục "Ôn tập SRS".
      item.to === '/srs' && dueCount > 0
        ? {
            ...item,
            badge: (
              <span className="inline-flex items-center justify-center min-w-5 h-5 px-1.5 rounded-full bg-indigo-600 text-white text-[10px] font-bold tabular-nums">
                {dueCount}
              </span>
            ),
          }
        : item,
    )

  const onNavigate = isMobile ? closeMobile : undefined

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
            <BrandLogo size="md" onClick={closeMobile} />
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
        <nav className="flex-1 overflow-y-auto py-3 px-3 flex flex-col justify-between">
          <div className="space-y-1">
            {visibleNavItems.map((item) => (
              <NavItemRow
                key={item.to}
                item={item}
                collapsed={collapsed}
                onNavigate={onNavigate}
              />
            ))}
          </div>

          <div className="mt-auto pt-4 pb-1">
            {staff ? (
              // Admin/giáo viên vẫn cần vào trang học viên (xem trước khóa học, đề thi...)
              <div className="border-t border-gray-200 pt-3">
                <NavItemRow
                  item={{
                    to: '/',
                    label: 'Xem trang học viên',
                    icon: <ArrowLeftRight className="w-5 h-5" />,
                    match: () => false,
                  }}
                  collapsed={collapsed}
                  onNavigate={onNavigate}
                />
              </div>
            ) : collapsed ? (
              /* ===================== Gói cước PRO (Ở góc dưới Sidebar) ===================== */
              <Tooltip title="Gói cước PRO" placement="right">
                <NavLink
                  to="/pricing"
                  onClick={onNavigate}
                  className={({ isActive }) =>
                    cn(
                      'w-10 h-10 mx-auto rounded-xl flex items-center justify-center transition cursor-pointer',
                      isActive
                        ? 'bg-amber-500/20 text-amber-600'
                        : 'text-amber-600 hover:bg-amber-50',
                    )
                  }
                >
                  <Crown className="w-5 h-5 fill-amber-500 text-amber-500" />
                </NavLink>
              </Tooltip>
            ) : (
              <NavLink
                to="/pricing"
                onClick={onNavigate}
                className={({ isActive }) =>
                  cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-xl transition text-xs font-bold cursor-pointer',
                    isActive
                      ? 'bg-amber-500/15 text-amber-800 border border-amber-500/30'
                      : 'bg-kinari/70 hover:bg-kinari text-sumi border border-suna/70 hover:border-amber-400/50 shadow-2xs',
                  )
                }
              >
                <div className="w-7 h-7 rounded-lg bg-amber-500/20 text-amber-600 flex items-center justify-center shrink-0">
                  <Crown className="w-4 h-4 fill-amber-500 text-amber-500" />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-bold text-sumi text-xs truncate">Gói cước PRO</p>
                  <p className="text-[10px] text-nibi truncate font-normal">Mở khóa toàn bộ quyền lợi</p>
                </div>
              </NavLink>
            )}
          </div>
        </nav>
      </aside>
    </>
  )
}
