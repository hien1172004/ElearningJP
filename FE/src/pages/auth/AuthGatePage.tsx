import React, { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useAuthModalStore, useAuthStore } from '@/stores'

/**
 * AuthGatePage — thay thế trang /login và /register cũ bằng MODAL.
 *
 * <p>Trước đây app có 2 trang riêng `/login` và `/register`. Nay toàn bộ luồng
 * đăng nhập / đăng ký diễn ra trong {@code AuthModal}. Trang này chỉ là một
 * "cửa ngõ" để giữ tương thích với các deep-link cũ (ví dụ
 * {@code /login?redirect=/exams} do ProtectedRoute hoặc nút "Đăng nhập" tạo ra):</p>
 *
 * <ol>
 *   <li>Đọc {@code ?redirect=} (nếu có).</li>
 *   <li>Mở AuthModal đúng mode (login/register) với redirectTo tương ứng.</li>
 *   <li>Điều hướng về {@code redirect} (hoặc trang chủ) để modal nằm trên nền app.</li>
 * </ol>
 *
 * <p>Nếu user đã đăng nhập thì bỏ qua modal và đi thẳng tới đích.</p>
 */
interface AuthGatePageProps {
  mode: 'login' | 'register'
}

export const AuthGatePage: React.FC<AuthGatePageProps> = ({ mode }) => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const ran = useRef(false)

  const isAuthenticated = useAuthStore((s) => s.isAuthenticated)
  const openLogin = useAuthModalStore((s) => s.openLogin)
  const openRegister = useAuthModalStore((s) => s.openRegister)

  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const rawRedirect = searchParams.get('redirect')
    // Chỉ chấp nhận path nội bộ bắt đầu bằng "/" để tránh open-redirect
    const redirectTo =
      rawRedirect && /^\/[A-Za-z0-9/_\-?&=.%]*$/.test(rawRedirect)
        ? rawRedirect
        : '/'

    // Đã đăng nhập → đi thẳng tới đích, không mở modal
    if (isAuthenticated) {
      navigate(redirectTo, { replace: true })
      return
    }

    // Mở modal đúng mode với redirectTo, rồi về nền app để modal hiển thị trên đó
    if (mode === 'register') {
      openRegister(redirectTo)
    } else {
      openLogin(redirectTo)
    }
    navigate(redirectTo, { replace: true })
  }, [mode, isAuthenticated, openLogin, openRegister, navigate, searchParams])

  // Không render gì — modal sẽ được mount ở MainLayout
  return null
}
