import React from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Lock, ShieldCheck, AlertCircle, ArrowLeft } from 'lucide-react'
import {
  Alert,
  App,
  Button as AntdButton,
  Form,
  Input as AntdInput,
  Spin,
} from 'antd'
import { z } from 'zod'
import { AuthHeader, AuthFooter } from '@/components/auth'
import { useResetPassword, useValidateResetToken } from '@/hooks'

const resetSchema = z
  .object({
    newPassword: z
      .string()
      .min(8, 'Mật khẩu phải có ít nhất 8 ký tự')
      .max(72, 'Mật khẩu quá dài'),
    confirmPassword: z.string().min(1, 'Vui lòng xác nhận mật khẩu'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Mật khẩu xác nhận không khớp',
    path: ['confirmPassword'],
  })

type ResetFormValues = z.infer<typeof resetSchema>

export const ResetPasswordPage: React.FC = () => {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token')
  const { message } = App.useApp()
  const [form] = Form.useForm<ResetFormValues>()

  const {
    data: isTokenValid,
    isLoading: isValidating,
    isError: isTokenInvalid,
  } = useValidateResetToken(token)

  const { mutate, isPending } = useResetPassword()

  const onFinish = async () => {
    if (!token) return
    try {
      const values = (await form.validateFields()) as ResetFormValues
      const parsed = resetSchema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      mutate({ token, ...parsed.data })
    } catch {
      /* form.validateFields đã hiện lỗi */
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-slate-100 via-indigo-50 to-slate-100 p-4">
      <div className="w-full max-w-md bg-white rounded-md shadow-xl border border-gray-200/70 overflow-hidden">
        <AuthHeader
          badge="Đặt lại mật khẩu"
          title="Tạo mật khẩu mới"
          subtitle="Vui lòng chọn một mật khẩu mạnh (tối thiểu 8 ký tự) để bảo vệ tài khoản của bạn."
        />

        <div className="px-6 sm:px-8 py-7">
          {!token ? (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle className="w-5 h-5" />}
              message="Thiếu token đặt lại mật khẩu"
              description={
                <span>
                  Vui lòng mở lại liên kết trong email.{' '}
                  <Link
                    to="/forgot-password"
                    className="font-semibold text-indigo-600 hover:underline"
                  >
                    Yêu cầu gửi lại email
                  </Link>
                </span>
              }
            />
          ) : isValidating ? (
            <div className="flex flex-col items-center text-center space-y-3 py-4">
              <Spin size="large" />
              <p className="text-sm text-gray-500">Đang xác thực token…</p>
            </div>
          ) : !isTokenValid || isTokenInvalid ? (
            <Alert
              type="error"
              showIcon
              icon={<AlertCircle className="w-5 h-5" />}
              message="Token không hợp lệ hoặc đã hết hạn"
              description={
                <span>
                  Vui lòng yêu cầu gửi lại email.{' '}
                  <Link
                    to="/forgot-password"
                    className="font-semibold text-indigo-600 hover:underline"
                  >
                    Yêu cầu email mới
                  </Link>
                </span>
              }
            />
          ) : (
            <Form
              form={form}
              layout="vertical"
              requiredMark={false}
              onFinish={onFinish}
              disabled={isPending}
              initialValues={{ newPassword: '', confirmPassword: '' }}
            >
              <Form.Item
                label="Mật khẩu mới"
                name="newPassword"
                rules={[
                  { required: true, message: 'Vui lòng nhập mật khẩu' },
                  { min: 8, message: 'Mật khẩu phải có ít nhất 8 ký tự' },
                  { max: 72, message: 'Mật khẩu quá dài' },
                ]}
              >
                <AntdInput.Password
                  prefix={<Lock className="w-4 h-4" />}
                  placeholder="Tối thiểu 8 ký tự"
                  autoComplete="new-password"
                  size="large"
                />
              </Form.Item>

              <Form.Item
                label="Xác nhận mật khẩu mới"
                name="confirmPassword"
                dependencies={['newPassword']}
                rules={[
                  { required: true, message: 'Vui lòng xác nhận mật khẩu' },
                  ({ getFieldValue }) => ({
                    validator(_, value) {
                      if (
                        !value ||
                        getFieldValue('newPassword') === value
                      ) {
                        return Promise.resolve()
                      }
                      return Promise.reject(
                        new Error('Mật khẩu xác nhận không khớp'),
                      )
                    },
                  }),
                ]}
              >
                <AntdInput.Password
                  prefix={<Lock className="w-4 h-4" />}
                  placeholder="Nhập lại mật khẩu mới"
                  autoComplete="new-password"
                  size="large"
                />
              </Form.Item>

              <div className="flex items-start gap-2 text-xs text-gray-500 bg-indigo-50/60 border border-indigo-100 rounded-md p-3 mb-4">
                <ShieldCheck className="w-4 h-4 mt-0.5 text-indigo-600 shrink-0" />
                <p>
                  Mật khẩu nên có chữ hoa, chữ thường, số và ký tự đặc biệt để
                  tăng cường bảo mật.
                </p>
              </div>

              <Form.Item className="!mb-0">
                <AntdButton
                  type="primary"
                  size="large"
                  htmlType="submit"
                  block
                  loading={isPending}
                  icon={<ShieldCheck className="w-4 h-4" />}
                  className="!font-semibold"
                >
                  Đặt lại mật khẩu
                </AntdButton>
              </Form.Item>

              <div className="mt-4">
                <AuthFooter>
                  <p className="text-sm text-gray-600 flex items-center justify-center gap-1.5">
                    <ArrowLeft className="w-3.5 h-3.5" />
                    Quay lại{' '}
                    <Link
                      to="/"
                      className="font-semibold text-indigo-600 hover:underline"
                    >
                      trang chủ
                    </Link>
                  </p>
                </AuthFooter>
              </div>
            </Form>
          )}
        </div>
      </div>
    </div>
  )
}
