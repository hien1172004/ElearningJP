import React, { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Modal, Tabs, App as AntdApp, Divider, Form, Input, Button, Checkbox } from 'antd'
import { Mail, Lock, User, Phone } from 'lucide-react'
import { z } from 'zod'
import { useAuthModalStore } from '@/stores'
import { useGoogleLogin, useLogin, useRegister } from '@/hooks'
import { GoogleButton } from './GoogleButton'
import { AUTH_EXPIRED_EVENT, type AuthExpiredDetail } from '@/utils/authEvents'

// ============================================================================
// Schema validation (giữ nguyên như các page login/register cũ)
// ============================================================================

const loginSchema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z
    .string()
    .min(1, 'Vui lòng nhập mật khẩu')
    .min(5, 'Mật khẩu phải có ít nhất 5 ký tự'),
})
type LoginFormValues = z.infer<typeof loginSchema>

const phoneRegex = /^(0|\+84)(3|5|7|8|9|1[2|6|8|9])\d{8}$/

const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(1, 'Vui lòng nhập họ tên')
      .min(2, 'Họ tên phải có ít nhất 2 ký tự')
      .max(100, 'Họ tên quá dài'),
    email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
    phone: z
      .string()
      .min(1, 'Vui lòng nhập số điện thoại')
      .regex(phoneRegex, 'Số điện thoại không hợp lệ (VD: 0912345678)'),
    password: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .max(72, 'Mật khẩu quá dài'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })
type RegisterFormValues = z.infer<typeof registerSchema>

// ============================================================================
// Component
// ============================================================================

export const AuthModal: React.FC = () => {
  const navigate = useNavigate()
  const { message } = AntdApp.useApp()

  const { open, mode, redirectTo, close, setMode } = useAuthModalStore()
  const { startGoogleLogin, isLoading: isGoogleLoading } = useGoogleLogin()

  // Form instances
  const [loginForm] = Form.useForm<LoginFormValues>()
  const [registerForm] = Form.useForm<RegisterFormValues>()
  const [remember, setRemember] = React.useState(true)

  // Lưu redirectTo vào sessionStorage để hook Google đọc lại khi cần
  useEffect(() => {
    if (open && redirectTo) {
      try {
        sessionStorage.setItem('elearning_google_redirect', redirectTo)
      } catch {
        /* ignore */
      }
    }
  }, [open, redirectTo])

  // Lắng nghe event từ axiosClient khi token hết hạn / refresh fail
  useEffect(() => {
    const handler = (e: Event) => {
      const detail = (e as CustomEvent<AuthExpiredDetail>).detail
      // Mở modal đăng nhập với redirect về trang hiện tại
      useAuthModalStore.getState().openLogin(detail.redirectTo)
    }
    window.addEventListener(AUTH_EXPIRED_EVENT, handler)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handler)
  }, [])

  // Reset forms khi đóng modal
  useEffect(() => {
    if (!open) {
      loginForm.resetFields()
      registerForm.resetFields()
    }
  }, [open, loginForm, registerForm])

  const { mutate: loginMutate, isPending: isLoginPending } = useLogin({
    remember,
  })
  const { mutate: registerMutate, isPending: isRegisterPending } = useRegister()

  // ------------------------------------------------------------------
  // Submit handlers
  // ------------------------------------------------------------------
  const onLogin = async () => {
    try {
      const values = (await loginForm.validateFields()) as LoginFormValues
      const parsed = loginSchema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      // Truyền redirectTo qua __redirectTo để override default trong hook.
      loginMutate(
        { ...parsed.data, __redirectTo: redirectTo ?? '/' } as LoginFormValues & {
          __redirectTo?: string
        },
        {
          onSuccess: () => {
            // Sau khi login thành công, useLogin đã navigate tới redirectTo.
            close()
          },
        },
      )
    } catch {
      /* validateFields đã hiện lỗi */
    }
  }

  const onRegister = async () => {
    try {
      const values = (await registerForm.validateFields()) as RegisterFormValues
      const parsed = registerSchema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      registerMutate(
        { ...parsed.data, __redirectTo: redirectTo ?? '/' } as RegisterFormValues & {
          __redirectTo?: string
        },
        {
          onSuccess: () => {
            // Sau khi đăng ký, useRegister đã navigate về '/'
            close()
          },
        },
      )
    } catch {
      /* validateFields đã hiện lỗi */
    }
  }

  const handleGoogleLogin = () => {
    // Lưu redirectTo để sau callback quay lại đúng chỗ
    try {
      if (redirectTo) {
        sessionStorage.setItem('elearning_google_redirect', redirectTo)
      }
    } catch {
      /* ignore */
    }
    startGoogleLogin()
  }

  const handleForgotPassword = () => {
    close()
    navigate('/forgot-password')
  }

  // ------------------------------------------------------------------
  // Render
  // ------------------------------------------------------------------
  const isPending = isLoginPending || isRegisterPending

  return (
    <Modal
      open={open}
      onCancel={close}
      footer={null}
      width={460}
      centered
      destroyOnHidden
      maskClosable={!isPending}
      closable={!isPending}
      className="auth-modal"
      styles={{
        body: { padding: 0 },
      }}
    >
      <div className="flex flex-col bg-white max-h-[90vh]">
        {/* ============================================================
            HEADER — Logo + Title
           ============================================================ */}
        <div className="px-6 sm:px-8 pt-7 pb-5 text-center border-b border-gray-100">
          <div className="inline-flex items-center gap-2 mb-4">
            <span className="w-9 h-9 rounded-md bg-indigo-600 text-white flex items-center justify-center font-serif text-lg font-bold">
              日
            </span>
            <span className="text-lg font-bold text-gray-900">ElearningJP</span>
          </div>
          <h2 className="text-base font-semibold text-gray-900">
            {mode === 'login' ? 'Đăng nhập tài khoản' : 'Tạo tài khoản mới'}
          </h2>
          <p className="text-xs text-gray-500 mt-1">
            {mode === 'login'
              ? 'Chào mừng bạn quay lại'
              : 'Bắt đầu hành trình N5 → N1'}
          </p>
        </div>

        {/* ============================================================
            TAB SWITCH
           ============================================================ */}
        <div className="px-6 sm:px-8 pt-4">
          <Tabs
            activeKey={mode}
            onChange={(key) => setMode(key as 'login' | 'register')}
            centered
            items={[
              {
                key: 'login',
                label: (
                  <span className="px-3 font-semibold text-sm">Đăng nhập</span>
                ),
              },
              {
                key: 'register',
                label: (
                  <span className="px-3 font-semibold text-sm">Đăng ký</span>
                ),
              },
            ]}
          />
        </div>

        <div className="flex-1 overflow-y-auto px-6 sm:px-8 pb-6">
            {/* ===== Google button (hiển thị ở cả 2 tab) ===== */}
            <GoogleButton
              onClick={handleGoogleLogin}
              loading={isGoogleLoading}
              label={
                mode === 'login'
                  ? 'Đăng nhập với Google'
                  : 'Đăng ký với Google'
              }
            />

            <Divider plain className="!my-4 !text-xs text-gray-400">
              hoặc {mode === 'login' ? 'đăng nhập' : 'tạo tài khoản'} bằng email
            </Divider>

            {/* ============= LOGIN FORM ============= */}
            {mode === 'login' && (
              <Form
                form={loginForm}
                layout="vertical"
                requiredMark={false}
                onFinish={onLogin}
                disabled={isLoginPending}
                initialValues={{ email: '', password: '' }}
              >
                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: 'Vui lòng nhập email' },
                    { type: 'email', message: 'Email không hợp lệ' },
                  ]}
                >
                  <Input
                    prefix={<Mail className="w-4 h-4 text-gray-400" />}
                    placeholder="Email"
                    autoComplete="email"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  rules={[
                    { required: true, message: 'Vui lòng nhập mật khẩu' },
                    { min: 5, message: 'Mật khẩu phải có ít nhất 5 ký tự' },
                  ]}
                >
                  <Input.Password
                    prefix={<Lock className="w-4 h-4 text-gray-400" />}
                    placeholder="Mật khẩu"
                    autoComplete="current-password"
                    size="large"
                  />
                </Form.Item>

                <div className="flex items-center justify-between mb-5 -mt-1">
                  <Checkbox
                    checked={remember}
                    onChange={(e) => setRemember(e.target.checked)}
                  >
                    <span className="text-sm text-gray-600">Nhớ mật khẩu</span>
                  </Checkbox>
                  <button
                    type="button"
                    onClick={handleForgotPassword}
                    className="text-sm text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    Quên mật khẩu?
                  </button>
                </div>

                <Form.Item className="!mb-0">
                  <Button
                    type="primary"
                    size="large"
                    htmlType="submit"
                    block
                    loading={isLoginPending}
                  >
                    Đăng nhập
                  </Button>
                </Form.Item>

                <p className="text-center text-sm text-gray-600 mt-4">
                  Chưa có tài khoản?{' '}
                  <button
                    type="button"
                    onClick={() => setMode('register')}
                    className="text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    Đăng ký ngay
                  </button>
                </p>
              </Form>
            )}

            {/* ============= REGISTER FORM ============= */}
            {mode === 'register' && (
              <Form
                form={registerForm}
                layout="vertical"
                requiredMark={false}
                onFinish={onRegister}
                disabled={isRegisterPending}
                initialValues={{
                  fullName: '',
                  email: '',
                  phone: '',
                  password: '',
                  confirmPassword: '',
                }}
              >
                <Form.Item
                  name="fullName"
                  rules={[
                    { required: true, message: 'Vui lòng nhập họ tên' },
                    { min: 2, message: 'Họ tên phải có ít nhất 2 ký tự' },
                  ]}
                >
                  <Input
                    prefix={<User className="w-4 h-4 text-gray-400" />}
                    placeholder="Họ và tên"
                    autoComplete="name"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="email"
                  rules={[
                    { required: true, message: 'Vui lòng nhập email' },
                    { type: 'email', message: 'Email không hợp lệ' },
                  ]}
                >
                  <Input
                    prefix={<Mail className="w-4 h-4 text-gray-400" />}
                    placeholder="Email"
                    autoComplete="email"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="phone"
                  rules={[
                    { required: true, message: 'Vui lòng nhập số điện thoại' },
                    {
                      pattern: phoneRegex,
                      message: 'Số điện thoại không hợp lệ (VD: 0912345678)',
                    },
                  ]}
                >
                  <Input
                    prefix={<Phone className="w-4 h-4 text-gray-400" />}
                    placeholder="Số điện thoại"
                    autoComplete="tel"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="password"
                  extra={
                    <span className="text-xs text-gray-400">
                      Tối thiểu 8 ký tự
                    </span>
                  }
                  rules={[
                    { required: true, message: 'Vui lòng nhập mật khẩu' },
                    { min: 8, message: 'Mật khẩu phải có ít nhất 8 ký tự' },
                  ]}
                >
                  <Input.Password
                    prefix={<Lock className="w-4 h-4 text-gray-400" />}
                    placeholder="Mật khẩu"
                    autoComplete="new-password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item
                  name="confirmPassword"
                  dependencies={['password']}
                  rules={[
                    { required: true, message: 'Vui lòng xác nhận mật khẩu' },
                    ({ getFieldValue }) => ({
                      validator(_, value) {
                        if (!value || getFieldValue('password') === value) {
                          return Promise.resolve()
                        }
                        return Promise.reject(
                          new Error('Mật khẩu xác nhận không khớp'),
                        )
                      },
                    }),
                  ]}
                >
                  <Input.Password
                    prefix={<Lock className="w-4 h-4 text-gray-400" />}
                    placeholder="Xác nhận mật khẩu"
                    autoComplete="new-password"
                    size="large"
                  />
                </Form.Item>

                <Form.Item className="!mb-0">
                  <Button
                    type="primary"
                    size="large"
                    htmlType="submit"
                    block
                    loading={isRegisterPending}
                  >
                    Tạo tài khoản
                  </Button>
                </Form.Item>

                <p className="text-center text-sm text-gray-600 mt-4">
                  Đã có tài khoản?{' '}
                  <button
                    type="button"
                    onClick={() => setMode('login')}
                    className="text-indigo-600 hover:text-indigo-700 font-medium"
                  >
                    Đăng nhập
                  </button>
                </p>
              </Form>
            )}
          </div>
      </div>
    </Modal>
  )
}
