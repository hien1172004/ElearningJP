import React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Mail, Lock, User, Phone, BookOpen, Brain, Shield } from 'lucide-react'
import { App, Button as AntdButton, Form, Input as AntdInput } from 'antd'
import { z } from 'zod'
import { AuthFooter } from '@/components/auth'
import { useRegister } from '@/hooks'

// Regex số điện thoại Việt Nam phổ biến (10-11 số, bắt đầu bằng 0)
const phoneRegex = /^(0|\+84)(3|5|7|8|9|1[2|6|8|9])\d{8}$/

const registerSchema = z
  .object({
    fullName: z
      .string()
      .min(1, 'Vui lòng nhập họ tên')
      .min(2, 'Họ tên phải có ít nhất 2 ký tự')
      .max(100, 'Họ tên quá dài'),
    email: z
      .string()
      .min(1, 'Vui lòng nhập email')
      .email('Email không hợp lệ'),
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

// Feature highlights - rút gọn
const features = [
  { icon: BookOpen, title: '5,000+ từ vựng', desc: 'Từ N5 đến N1' },
  { icon: Brain, title: 'Học với SRS', desc: 'Nhớ lâu hơn 10x' },
  { icon: Shield, title: 'Bảo mật', desc: 'Dữ liệu được bảo vệ' },
]

export const RegisterPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const redirectTo = searchParams.get('redirect') || '/'
  const { message } = App.useApp()
  const [form] = Form.useForm<RegisterFormValues>()

  const { mutate, isPending } = useRegister()

  const onFinish = async () => {
    try {
      const values = (await form.validateFields()) as RegisterFormValues
      const parsed = registerSchema.safeParse(values)
      if (!parsed.success) {
        const first = parsed.error.issues[0]
        message.error(first.message)
        return
      }
      mutate(parsed.data, {
        onSuccess: () => {
          if (redirectTo !== '/') {
            // người dùng từ một trang protected -> vẫn đi về đó
          }
        },
      })
    } catch {
      /* form.validateFields đã hiện lỗi */
    }
  }

  return (
    <div className="min-h-screen flex">
      {/* Left Panel - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-purple-700 via-indigo-800 to-slate-900 p-12 xl:p-16 flex-col justify-between text-white">
        <div>
          {/* Logo */}
          <Link to="/" className="inline-flex items-center gap-3">
            <span className="w-12 h-12 rounded-md bg-white/10 backdrop-blur flex items-center justify-center font-serif text-2xl font-black">
              日
            </span>
            <span className="text-2xl font-bold">ElearningJP</span>
          </Link>

          {/* Headline */}
          <div className="mt-10">
            <h1 className="text-3xl xl:text-4xl font-bold leading-tight">
              Bắt đầu
              <span className="block text-purple-300">hành trình</span>
              ngay hôm nay
            </h1>
            <p className="text-lg text-purple-200/80 mt-4 max-w-md">
              Đăng ký miễn phí để tiếp cận hệ thống học tiếng Nhật thông minh.
            </p>
          </div>

          {/* Features */}
          <div className="mt-10 grid gap-4">
            {features.map((feature, index) => (
              <div key={index} className="flex items-center gap-4 p-4 rounded-md bg-white/5">
                <div className="w-10 h-10 rounded bg-purple-500/30 flex items-center justify-center">
                  <feature.icon className="w-5 h-5 text-purple-300" />
                </div>
                <div>
                  <div className="font-semibold">{feature.title}</div>
                  <div className="text-sm text-purple-200/70">{feature.desc}</div>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer note */}
        {/* <div className="flex items-center gap-2 text-sm text-purple-200/60">
          <Shield className="w-4 h-4" />
          <span>Đăng ký an toàn • Dữ liệu của bạn luôn được bảo vệ</span>
        </div> */}
      </div>

      {/* Right Panel - Register Form */}
      <div className="flex-1 flex items-center justify-center p-6 sm:p-10 bg-gray-50 overflow-y-auto">
        <div className="w-full max-w-md py-8">
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
              <h2 className="text-xl font-semibold text-gray-900">Tạo tài khoản</h2>
              <p className="text-sm text-gray-500 mt-1">Bắt đầu hành trình N5 → N1</p>
            </div>

            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              onFinish={onFinish}
              disabled={isPending}
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
                <AntdInput
                  prefix={<User className="w-4 h-4 text-gray-400" />}
                  placeholder="Họ và tên"
                  autoComplete="name"
                  size="large"
                  className="!rounded"
                />
              </Form.Item>

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
                name="phone"
                rules={[
                  { required: true, message: 'Vui lòng nhập số điện thoại' },
                  {
                    pattern: phoneRegex,
                    message: 'Số điện thoại không hợp lệ (VD: 0912345678)',
                  },
                ]}
              >
                <AntdInput
                  prefix={<Phone className="w-4 h-4 text-gray-400" />}
                  placeholder="Số điện thoại"
                  autoComplete="tel"
                  size="large"
                  className="!rounded"
                />
              </Form.Item>

              <Form.Item
                name="password"
                extra={<span className="text-xs text-gray-400">Tối thiểu 8 ký tự</span>}
                rules={[
                  { required: true, message: 'Vui lòng nhập mật khẩu' },
                  { min: 8, message: 'Mật khẩu phải có ít nhất 8 ký tự' },
                ]}
              >
                <AntdInput.Password
                  prefix={<Lock className="w-4 h-4 text-gray-400" />}
                  placeholder="Mật khẩu"
                  autoComplete="new-password"
                  size="large"
                  className="!rounded"
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
                      return Promise.reject(new Error('Mật khẩu xác nhận không khớp'))
                    },
                  }),
                ]}
              >
                <AntdInput.Password
                  prefix={<Lock className="w-4 h-4 text-gray-400" />}
                  placeholder="Xác nhận mật khẩu"
                  autoComplete="new-password"
                  size="large"
                  className="!rounded"
                />
              </Form.Item>

              <Form.Item className="!mb-0">
                <AntdButton
                  type="primary"
                  size="large"
                  htmlType="submit"
                  block
                  loading={isPending}
                  className="!rounded !font-medium"
                >
                  Tạo tài khoản
                </AntdButton>
              </Form.Item>
            </Form>

            <AuthFooter>
              <p className="text-sm text-gray-600">
                Đã có tài khoản?{' '}
                <Link
                  to={
                    redirectTo !== '/'
                      ? `/login?redirect=${encodeURIComponent(redirectTo)}`
                      : '/login'
                  }
                  className="text-indigo-600 hover:text-indigo-700 font-medium"
                >
                  Đăng nhập
                </Link>
              </p>
            </AuthFooter>
          </div>
        </div>
      </div>
    </div>
  )
}
