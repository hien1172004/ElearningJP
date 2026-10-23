import React from 'react'
import { Link } from 'react-router-dom'
import { Mail, Send, ArrowLeft } from 'lucide-react'
import { App, Button as AntdButton, Form, Input as AntdInput } from 'antd'
import { z } from 'zod'
import { AuthHeader, AuthFooter } from '@/components/auth'
import { useForgotPassword } from '@/hooks'

const forgotSchema = z.object({
  email: z.string().min(1, 'Vui lòng nhập email').email('Email không hợp lệ'),
})

type ForgotFormValues = z.infer<typeof forgotSchema>

export const ForgotPasswordPage: React.FC = () => {
  const { message } = App.useApp()
  const [form] = Form.useForm<ForgotFormValues>()

  const { mutate, isPending } = useForgotPassword()

  const onFinish = async () => {
    try {
      const values = (await form.validateFields()) as ForgotFormValues
      const parsed = forgotSchema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      mutate(parsed.data)
    } catch {
      /* form.validateFields đã hiện lỗi */
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-indigo-50 to-slate-100 p-4">
      <div className="w-full max-w-md bg-white rounded-md shadow-xl border border-gray-200/70 overflow-hidden">
        <AuthHeader
          badge="Khôi phục mật khẩu"
          title="Quên mật khẩu?"
          subtitle="Nhập email đã đăng ký, chúng tôi sẽ gửi cho bạn một liên kết để đặt lại mật khẩu an toàn."
        />

        <Form
          form={form}
          layout="vertical"
          requiredMark={false}
          onFinish={onFinish}
          disabled={isPending}
          className="px-6 sm:px-8 py-7"
          initialValues={{ email: '' }}
        >
          <Form.Item
            label="Email đã đăng ký"
            name="email"
            rules={[
              { required: true, message: 'Vui lòng nhập email' },
              { type: 'email', message: 'Email không hợp lệ' },
            ]}
          >
            <AntdInput
              prefix={<Mail className="w-4 h-4" />}
              placeholder="your-email@example.com"
              autoComplete="email"
              size="large"
            />
          </Form.Item>

          <Form.Item className="!mb-0">
            <AntdButton
              type="primary"
              size="large"
              htmlType="submit"
              block
              loading={isPending}
              icon={<Send className="w-4 h-4" />}
              className="!font-semibold"
            >
              Gửi link đặt lại
            </AntdButton>
          </Form.Item>

          <AuthFooter>
            <p className="text-sm text-gray-600 flex items-center justify-center gap-1.5">
              <ArrowLeft className="w-3.5 h-3.5" />
              Nhớ mật khẩu rồi?{' '}
              <Link
                to="/"
                className="font-semibold text-indigo-600 hover:text-indigo-700 hover:underline"
              >
                Đăng nhập ngay
              </Link>
            </p>
          </AuthFooter>
        </Form>
      </div>
    </div>
  )
}
