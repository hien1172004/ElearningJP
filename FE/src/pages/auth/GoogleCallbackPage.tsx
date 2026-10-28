import React, { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { Spin, Result, Button } from 'antd'
import { useGoogleCallback } from '@/hooks'
import { useAuthModalStore } from '@/stores'

/**
 * Trang callback Google OAuth.
 *
 * Flow:
 *   1. User bấm "Đăng nhập với Google" trong AuthModal
 *   2. FE redirect tới BE /auth/google
 *   3. BE xử lý OAuth với Google → callback về BE /auth/google/callback?token=...
 *   4. BE redirect sang FE /auth/google/callback?token=...
 *   5. Trang này đọc token, lưu vào store, đóng mọi modal, navigate về redirectTo
 *
 * URL params:
 *   - token (required): JWT access token do BE cấp
 *   - redirect (optional): đường dẫn về sau khi đăng nhập
 *   - error (optional): nếu user huỷ hoặc OAuth lỗi
 */
export const GoogleCallbackPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const { handle } = useGoogleCallback()
  const ran = useRef(false)
  const closeAuthModal = useAuthModalStore((s) => s.close)

  useEffect(() => {
    if (ran.current) return
    ran.current = true

    const error = searchParams.get('error')
    if (error) {
      // Không xử lý gì thêm — sẽ render UI lỗi
      return
    }

    const token = searchParams.get('token')
    const result = handle(token)

    if (result.ok) {
      // Đóng modal auth (nếu đang mở ở tab nào đó)
      closeAuthModal()

      // Lấy redirectTo: ưu tiên URL param, fallback sessionStorage
      let redirectTo: string | null = searchParams.get('redirect')
      if (!redirectTo) {
        try {
          redirectTo = sessionStorage.getItem('elearning_google_redirect')
          if (redirectTo) sessionStorage.removeItem('elearning_google_redirect')
        } catch {
          redirectTo = null
        }
      }

      // Validate redirectTo: chỉ chấp nhận path nội bộ bắt đầu bằng "/"
      if (redirectTo && /^\/[A-Za-z0-9/_\-?&=.%]*$/.test(redirectTo)) {
        navigate(redirectTo, { replace: true })
      } else {
        navigate('/', { replace: true })
      }
    }
  }, [searchParams, handle, navigate, closeAuthModal])

  const error = searchParams.get('error')
  const errorDescription = searchParams.get('error_description')

  if (error) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-50 p-4">
        <Result
          status="error"
          title="Đăng nhập Google thất bại"
          subTitle={
            errorDescription ||
            'Đã có lỗi xảy ra trong quá trình đăng nhập. Vui lòng thử lại.'
          }
          extra={[
            <Button
              type="primary"
              key="home"
              onClick={() => navigate('/', { replace: true })}
            >
              Về trang chủ
            </Button>,
            <Button
              key="login"
              onClick={() => {
                navigate('/', { replace: true })
                // Mở lại modal đăng nhập sau khi về home
                setTimeout(() => {
                  useAuthModalStore.getState().openLogin()
                }, 100)
              }}
            >
              Thử lại
            </Button>,
          ]}
        />
      </div>
    )
  }

  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-slate-50 gap-3">
      <Spin size="large" />
      <p className="text-sm text-gray-500">Đang hoàn tất đăng nhập Google…</p>
    </div>
  )
}
