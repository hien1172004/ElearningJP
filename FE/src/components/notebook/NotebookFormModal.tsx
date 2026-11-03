import React from 'react'
import { Modal, Form, Input, Select, Switch } from 'antd'
import type { CreateNotebookRequest, Notebook } from '@/types'

interface NotebookFormModalProps {
  open: boolean
  initialData?: Notebook | null
  groups?: { label: string; value: number }[]
  onClose: () => void
  onSubmit: (values: CreateNotebookRequest) => Promise<void>
  loading?: boolean
}

const JLPT_LEVELS = [
  { label: 'Không xác định', value: '' },
  { label: 'N5 (Nhập môn)', value: 'N5' },
  { label: 'N4 (Sơ cấp)', value: 'N4' },
  { label: 'N3 (Trung cấp)', value: 'N3' },
  { label: 'N2 (Trung-Cao cấp)', value: 'N2' },
  { label: 'N1 (Cao cấp)', value: 'N1' },
]

export const NotebookFormModal: React.FC<NotebookFormModalProps> = ({
  open,
  initialData,
  groups = [],
  onClose,
  onSubmit,
  loading = false,
}) => {
  const [form] = Form.useForm()

  React.useEffect(() => {
    if (open) {
      if (initialData) {
        form.setFieldsValue({
          name: initialData.name,
          description: initialData.description || '',
          groupId: initialData.groupId || null,
          jlptLevel: initialData.jlptLevel || '',
          isPublic: initialData.isPublic ?? false,
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          groupId: null,
          jlptLevel: '',
          isPublic: false,
        })
      }
    }
  }, [open, initialData, form])

  const handleFinish = async (values: any) => {
    await onSubmit({
      ...values,
      jlptLevel: values.jlptLevel ? values.jlptLevel : null,
    })
    form.resetFields()
  }

  return (
    <Modal
      title={initialData ? 'Chỉnh sửa sổ tay' : 'Tạo sổ tay mới'}
      open={open}
      onCancel={onClose}
      onOk={() => form.submit()}
      confirmLoading={loading}
      okText={initialData ? 'Lưu thay đổi' : 'Tạo sổ tay'}
      cancelText="Hủy"
      centered
    >
      <Form form={form} layout="vertical" onFinish={handleFinish} className="mt-4">
        <Form.Item
          name="name"
          label="Tên sổ tay"
          rules={[
            { required: true, message: 'Vui lòng nhập tên sổ tay' },
            { max: 100, message: 'Tên không được vượt quá 100 ký tự' },
          ]}
        >
          <Input placeholder="Ví dụ: Từ vựng N2 Mimikara, Từ vựng bài 1..." />
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả"
          rules={[{ max: 500, message: 'Mô tả không được vượt quá 500 ký tự' }]}
        >
          <Input.TextArea
            rows={3}
            placeholder="Mô tả chi tiết về sổ tay (tùy chọn)..."
          />
        </Form.Item>

        <Form.Item name="groupId" label="Nhóm sổ tay">
          <Select
            allowClear
            placeholder="Chọn nhóm (để trống nếu chưa phân nhóm)"
            options={groups}
          />
        </Form.Item>

        <Form.Item name="jlptLevel" label="Cấp độ JLPT">
          <Select options={JLPT_LEVELS} placeholder="Chọn cấp độ JLPT" />
        </Form.Item>

        <Form.Item
          name="isPublic"
          valuePropName="checked"
          label="Cho phép chia sẻ"
          extra="Bật để chia sẻ qua mã, link và hiển thị trên Khám phá sổ tay cộng đồng"
        >
          <Switch checkedChildren="Bật" unCheckedChildren="Tắt" />
        </Form.Item>
      </Form>
    </Modal>
  )
}

