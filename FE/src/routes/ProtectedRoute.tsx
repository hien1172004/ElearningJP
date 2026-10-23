import React, { useEffect } from 'react'
import { Outlet, Navigate, useLocation } from 'react-router-dom'
import { useAuthStore, useAuthModalStore } from '@/stores'

interface ProtectedRouteProps {
  /**
   * Nếu truyền roles -> chỉ cho phép user có ít nhất 1 role trùng với danh sách.
   * Nếu không truyền -> bất kỳ user authenticated.
   */
  roles?: string[]
  /**
   * Nếu true -> render children thay vì <Outlet /> (dùng cho route wrapper).
   */
  children?: React.ReactNode
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ roles, children }) => {
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const isInitialized = useAuthStore((s) => s.isInitialized)
  const user = useAuthStore((s) => s.user)
  const location = useLocation()

  // Khi reload, AuthInitializer chưa chạy xong: KHÔNG đá về login ngay
  // vì có thể accessToken còn trong storage và sắp được phục hồi.
  if (!isInitialized) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-10 w-10 border-2 border-indigo-200 border-t-indigo-600" />
      </div>
    )
  }

  if (!isAuthenticated) {
    // Mở modal đăng nhập với redirectTo là trang user đang muốn vào.
    // Mở qua effect để tránh gọi state update trong render.
    // Dùng Navigate tới "/" để user không kẹt ở URL protected.
    return <RedirectWithLoginModal redirectTo={location.pathname + location.search} />
  }

  if (roles && roles.length > 0) {
    const userRoles = user?.roles?.length
      ? user.roles
      : user?.role
        ? [user.role]
        : []
    const ok = userRoles.some((r) => roles.includes(r))
    if (!ok) {
      return <Navigate to="/" replace />
    }
  }

  return children ? <>{children}</> : <Outlet />
}

/**
 * Component phụ: redirect về "/" đồng thời mở modal đăng nhập.
 * Tách riêng để useEffect có thể chạy an toàn.
 */
const RedirectWithLoginModal: React.FC<{ redirectTo: string }> = ({
  redirectTo,
}) => {
  const openLogin = useAuthModalStore((s) => s.openLogin)

  useEffect(() => {
    openLogin(redirectTo)
  }, [openLogin, redirectTo])

  return <Navigate to="/" replace />
}
