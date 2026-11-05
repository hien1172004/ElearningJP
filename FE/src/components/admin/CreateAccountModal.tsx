import React from 'react'
import { Form, Input, Modal, Radio } from 'antd'
import { z } from 'zod'
import { useCreateUser } from '@/hooks'
import type { ProvisionRole } from '@/types'

// Khớp validation ở BE (AdminCreateUserRequest)
const schema = z
  .object({
    fullName: z.string().trim().min(1, 'Vui lòng nhập họ tên').max(100, 'Họ tên tối đa 100 ký tự'),
    email: z.string().trim().min(1, 'Vui lòng nhập email').email('Email không hợp lệ').max(255),
    password: z.string().min(8, 'Mật khẩu tối thiểu 8 ký tự').max(100, 'Mật khẩu tối đa 100 ký tự'),
    confirmPassword: z.string(),
    role: z.enum(['TEACHER', 'ADMIN'], { message: 'Vui lòng chọn vai trò' }),
  })
  .refine((v) => v.password === v.confirmPassword, {
    message: 'Mật khẩu nhập lại không khớp',
    path: ['confirmPassword'],
  })

type FormValues = z.input<typeof schema>

const ROLE_OPTIONS: { value: ProvisionRole; label: string }[] = [
  { value: 'TEACHER', label: 'Giáo viên' },
  { value: 'ADMIN', label: 'Quản trị viên' },
]

interface CreateAccountModalProps {
  open: boolean
  onClose: () => void
}

/** Admin cấp phát tài khoản đăng nhập bằng email cho giáo viên / quản trị viên. */
export const CreateAccountModal: React.FC<CreateAccountModalProps> = ({ open, onClose }) => {
  const [form] = Form.useForm<FormValues>()
  const createUser = useCreateUser()

  const handleSubmit = async () => {
    const values = await form.validateFields().catch(() => null)
    if (!values) return
    const parsed = schema.safeParse(values)
    if (!parsed.success) {
      // Đẩy lỗi zod về đúng ô nhập
      form.setFields(
        parsed.error.issues.map((issue) => ({
          name: issue.path[0] as keyof FormValues,
          errors: [issue.message],
        })),
      )
      return
    }
    const { fullName, email, password, role } = parsed.data
    createUser.mutate({ fullName, email, password, role }, {
      onSuccess: () => {
        form.resetFields()
        onClose()
      },
    })
  }

  return (
    <Modal
      open={open}
      title="Tạo tài khoản"
      okText="Tạo tài khoản"
      cancelText="Huỷ"
      centered
      confirmLoading={createUser.isPending}
      onOk={handleSubmit}
      onCancel={onClose}
      destroyOnHidden
    >
      <Form<FormValues>
        form={form}
        layout="vertical"
        requiredMark={false}
        initialValues={{ role: 'TEACHER' }}
        disabled={createUser.isPending}
        className="pt-2"
      >
        <Form.Item name="role" label="Vai trò">
          <Radio.Group options={ROLE_OPTIONS} optionType="button" buttonStyle="solid" />
        </Form.Item>
        <Form.Item name="fullName" label="Họ tên">
          <Input maxLength={100} placeholder="VD: Nguyễn Văn A" />
        </Form.Item>
        <Form.Item name="email" label="Email đăng nhập">
          <Input maxLength={255} placeholder="giaovien@example.com" autoComplete="off" />
        </Form.Item>
        <Form.Item name="password" label="Mật khẩu">
          <Input.Password maxLength={100} placeholder="Tối thiểu 8 ký tự" autoComplete="new-password" />
        </Form.Item>
        <Form.Item name="confirmPassword" label="Nhập lại mật khẩu">
          <Input.Password maxLength={100} autoComplete="new-password" />
        </Form.Item>
        <p className="text-xs text-slate-500">
          Tài khoản được kích hoạt ngay. Hãy gửi email và mật khẩu cho người dùng, khuyến nghị họ đổi mật
          khẩu sau lần đăng nhập đầu tiên.
        </p>
      </Form>
    </Modal>
  )
}
