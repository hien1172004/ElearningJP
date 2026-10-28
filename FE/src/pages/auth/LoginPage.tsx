import React, { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Mail, Lock, BookOpen, Brain, BarChart2 } from 'lucide-react'
import { App, Button as AntdButton, Checkbox, Form, Input as AntdInput, Spin } from 'antd'
import { z } from 'zod'
import { AuthFooter } from '@/components/auth'
import { useLogin, useGoogleLogin } from '@/hooks'

declare global {
  interface Window {
    google?: {
      accounts: {
        id: {
          initialize: (config: {
            client_id: string
            callback: (response: { credential: string }) => void
          }) => void
          renderButton: (
            parent: HTMLElement,
            options: {
              theme?: 'outline' | 'filled_blue' | 'filled_black'
              size?: 'large' | 'medium' | 'small'
              text?: 'signin_with' | 'signup_with' | 'continue_with' | 'signin'
              shape?: 'rectangular' | 'pill' | 'circle' | 'square'
              width?: number
            },
          ) => void
          prompt?: () => void
        }
      }
    }
  }
}

const loginSchema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
  password: z
    .string()
    .min(1, 'Vui lòng nhập mật khẩu')
    .min(5, 'Mật khẩu phải có ít nhất 5 ký tự'),
})

type LoginFormValues = z.infer<typeof loginSchema>

// Feature highlights - rút gọn
const features = [
  { icon: BookOpen, title: 'Từ vựng SRS', desc: 'Học thông minh với Spaced Repetition' },
  { icon: Brain, title: 'Kanji N5→N1', desc: 'Hệ thống Kanji toàn diện' },
  { icon: BarChart2, title: 'Theo dõi tiến độ', desc: 'Thống kê chi tiết' },
]

export const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const [remember, setRemember] = useState(true)
  const redirectTo = searchParams.get('redirect') || '/'
  const { message } = App.useApp()
  const [form] = Form.useForm<LoginFormValues>()

  const googleBtnRef = useRef<HTMLDivElement>(null)

  const { mutate, isPending } = useLogin({ remember, redirectTo })
  const { mutate: googleLogin, isPending: isGooglePending } = useGoogleLogin({ redirectTo })

  useEffect(() => {
    if (searchParams.get('expired') === '1') {
      message.warning({
        content: 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại để tiếp tục.',
        duration: 4,
      })
    }
  }, [searchParams, message])

  // Khởi tạo Google Identity Services
  useEffect(() => {
    const clientId =
      import.meta.env.VITE_GOOGLE_CLIENT_ID ||
      '142602742944-vskbcvgq85k9kgl1469k0f5k2d5h73m0.apps.googleusercontent.com'

    const initGoogle = () => {
      if (window.google && googleBtnRef.current) {
        try {
          window.google.accounts.id.initialize({
            client_id: clientId,
            callback: (res: { credential: string }) => {
              if (res.credential) {
                googleLogin(res.credential)
              }
            },
          })

          googleBtnRef.current.innerHTML = ''
          window.google.accounts.id.renderButton(googleBtnRef.current, {
            theme: 'outline',
            size: 'large',
            text: 'signin_with',
            shape: 'rectangular',
            width: 350,
          })
        } catch (e) {
          console.error('Google gsi init error:', e)
        }
      }
    }

    if (!document.getElementById('google-gsi-script')) {
      const script = document.createElement('script')
      script.id = 'google-gsi-script'
      script.src = 'https://accounts.google.com/gsi/client'
      script.async = true
      script.defer = true
      script.onload = initGoogle
      document.body.appendChild(script)
    } else {
      initGoogle()
    }
  }, [googleLogin])

  const onFinish = async () => {
    try {
      const values = (await form.validateFields()) as LoginFormValues
      const parsed = loginSchema.safeParse(values)
      if (!parsed.success) {
        const first = parsed.error.issues[0]
        message.error(first.message)
        return
      }
      mutate(parsed.data)
    } catch {
      /* form.validateFields đã hiện lỗi */
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left Panel - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-indigo-700 via-indigo-800 to-slate-900 p-12 xl:p-16 flex-col justify-between text-white">
        <div>
          {/* Logo */}
          <Link to="/" className="inline-flex items-center gap-3">
            <span className="w-12 h-12 rounded-md text-white bg-white/10 backdrop-blur flex items-center justify-center font-serif text-2xl font-black">
              日
            </span>
            <span className="text-2xl font-bold text-white">ElearningJP</span>
          </Link>

          {/* Headline */}
          <div className="mt-10">
            <p className="text-4xl xl:text-5xl font-bold leading-tight mb-4">
              Chinh phục
              <span className="block text-indigo-300">Tiếng Nhật</span>
            </p>
            <p className="text-lg text-indigo-200/80 max-w-md">
              Học từ vựng, Kanji và luyện thi JLPT với công nghệ SRS thông minh.
            </p>
          </div>

          {/* Features */}
          <div className="mt-10 grid gap-4">
            {features.map((feature, index) => (
              <div key={index} className="flex items-center gap-4 p-4 rounded-md bg-white/5">
                <div className="w-10 h-10 rounded bg-indigo-500/30 flex items-center justify-center">
                  <feature.icon className="w-5 h-5 text-indigo-300" />
                </div>
                <div>
                  <div className="font-semibold">{feature.title}</div>
                  <div className="text-sm text-indigo-200/70">{feature.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right Panel - Login Form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10 bg-gray-50">
        <div className="w-full max-w-md">
          {/* Mobile Logo */}
          <div className="lg:hidden text-center mb-8">
            <Link to="/" className="inline-flex items-center gap-2">
              <span className="w-10 h-10 rounded-md bg-indigo-600 text-white flex items-center justify-center font-serif text-xl font-bold">
                日
              </span>
              <span className="text-2xl font-bold text-gray-900">ElearningJP</span>
            </Link>
          </div>

          <div className="bg-white rounded-md shadow-sm border border-gray-200 p-8">
            <div className="text-center mb-6">
              <h2 className="text-xl font-semibold text-gray-900">Đăng nhập tài khoản</h2>
              <p className="text-sm text-gray-500 mt-1">Chào mừng bạn quay lại</p>
            </div>

            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              onFinish={onFinish}
              disabled={isPending || isGooglePending}
              initialValues={{ email: '', password: '' }}
            >
              <Form.Item
                name="email"
                rules={[
                  { required: true, message: 'Vui lòng nhập email' },
                  { type: 'email', message: 'Email không hợp lệ' },
                ]}
              >
                <AntdInput
                  prefix={<Mail className="w-4 h-4 text-gray-400" />}
                  placeholder="Email"
                  autoComplete="email"
                  size="large"
                  className="!rounded"
                />
              </Form.Item>

              <Form.Item
                name="password"
                rules={[
                  { required: true, message: 'Vui lòng nhập mật khẩu' },
                  { min: 5, message: 'Mật khẩu phải có ít nhất 5 ký tự' },
                ]}
              >
                <AntdInput.Password
                  prefix={<Lock className="w-4 h-4 text-gray-400" />}
                  placeholder="Mật khẩu"
                  autoComplete="current-password"
                  size="large"
                  className="!rounded"
                />
              </Form.Item>

              <div className="flex items-center justify-between mb-6">
                <Checkbox
                  checked={remember}
                  onChange={(e) => setRemember(e.target.checked)}
                >
                  <span className="text-sm text-gray-600">Ghi nhớ tôi</span>
                </Checkbox>
                <Link
                  to="/forgot-password"
                  className="text-sm text-indigo-600 hover:text-indigo-700"
                >
                  Quên mật khẩu?
                </Link>
              </div>

              <Form.Item className="!mb-0">
                <AntdButton
                  type="primary"
                  size="large"
                  htmlType="submit"
                  block
                  loading={isPending}
                  className="!rounded !font-medium bg-indigo-600 hover:bg-indigo-700"
                >
                  Đăng nhập
                </AntdButton>
              </Form.Item>
            </Form>

            {/* Google Sign-in Divider */}
            <div className="relative my-6">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200" />
              </div>
              <div className="relative flex justify-center text-xs uppercase">
                <span className="bg-white px-3 text-gray-500 font-medium">Hoặc</span>
              </div>
            </div>

            {/* Google Button */}
            <div className="flex justify-center w-full min-h-[44px]">
              {isGooglePending ? (
                <div className="flex items-center gap-2 text-indigo-600 font-medium py-2">
                  <Spin size="small" />
                  <span className="text-sm">Đang xác thực Google...</span>
                </div>
              ) : (
                <div ref={googleBtnRef} className="w-full flex justify-center" />
              )}
            </div>

            <AuthFooter>
              <p className="text-sm text-gray-600">
                Chưa có tài khoản?{' '}
                <Link
                  to="/register"
                  className="text-indigo-600 hover:text-indigo-700 font-medium"
                  onClick={(e) => {
                    if (redirectTo !== '/') {
                      e.preventDefault()
                      navigate(`/register?redirect=${encodeURIComponent(redirectTo)}`)
                    }
                  }}
                >
                  Đăng ký ngay
                </Link>
              </p>
            </AuthFooter>
          </div>
        </div>
      </div>
    </div>
  )
}
