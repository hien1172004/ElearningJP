import React, { useEffect } from 'react'
import { Modal, Form, Input, Select, App } from 'antd'
import { Save, BookPlus, BookOpen } from 'lucide-react'
import { z } from 'zod'
import type { CourseCreateRequest, CourseDetail, CourseUpdateRequest, JlptLevel } from '@/types'
import { useCreateCourse, useUpdateCourse, useOnConcurrentReload } from '@/hooks'
import { MediaUploadField } from '@/components/common'

const JLPT_LEVELS: JlptLevel[] = ['N5', 'N4', 'N3', 'N2', 'N1']

const baseSchema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề').max(200),
  jlptLevel: z.enum(['N5', 'N4', 'N3', 'N2', 'N1']),
  description: z.string().max(2000).optional().or(z.literal('')),
  thumbnailUrl: z.string().url('URL không hợp lệ').optional().or(z.literal('')),
})

type FormValues = z.infer<typeof baseSchema>

interface CourseFormModalProps {
  open: boolean
  course?: CourseDetail | null
  onClose: () => void
  onSuccess?: (saved: boolean) => void
}

export const CourseFormModal: React.FC<CourseFormModalProps> = ({
  open,
  course,
  onClose,
  onSuccess,
}) => {
  const isEdit = !!course
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()

  const create = useCreateCourse()
  const update = useUpdateCourse(course?.id ?? 0)
  const isPending = create.isPending || update.isPending

  // "Tải bản mới nhất" sau xung đột: form sửa đang giữ version cũ → đóng, mở lại sẽ lấy bản mới nhất
  useOnConcurrentReload(() => {
    if (open && isEdit) onClose()
  })

  useEffect(() => {
    if (open) {
      if (course) {
        form.setFieldsValue({
          title: course.title,
          jlptLevel: course.jlptLevel,
          description: course.description ?? '',
          thumbnailUrl: course.thumbnailUrl ?? '',
        })
      } else {
        form.resetFields()
        form.setFieldsValue({
          jlptLevel: 'N5',
          description: '',
          thumbnailUrl: '',
        })
      }
    }
  }, [open, course, form])

  const onFinish = async () => {
    try {
      const values = (await form.validateFields()) as FormValues
      const parsed = baseSchema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      const v = parsed.data
      if (isEdit && course) {
        const payload: CourseUpdateRequest = {
          title: v.title,
          jlptLevel: v.jlptLevel,
          description: v.description || undefined,
          thumbnailUrl: v.thumbnailUrl || undefined,
          version: course.version,
        }
        await update.mutateAsync(payload)
        onSuccess?.(true)
      } else {
        const payload: CourseCreateRequest = {
          title: v.title,
          jlptLevel: v.jlptLevel,
          description: v.description || undefined,
          thumbnailUrl: v.thumbnailUrl || undefined,
          published: false,
        }
        await create.mutateAsync(payload)
        onSuccess?.(true)
      }
      onClose()
    } catch {
      /* form đã hiển thị lỗi */
    }
  }

  return (
    <Modal
      open={open}
      onCancel={isPending ? undefined : onClose}
      onOk={onFinish}
      title={
        <div className="flex items-center gap-2 font-bold text-gray-900">
          {isEdit ? (
            <BookOpen className="w-5 h-5 text-indigo-600" />
          ) : (
            <BookPlus className="w-5 h-5 text-indigo-600" />
          )}
          <span>{isEdit ? 'Chỉnh sửa khóa học' : 'Tạo khóa học mới'}</span>
        </div>
      }
      okText={isEdit ? 'Lưu thay đổi' : 'Tạo khóa học'}
      cancelText="Huỷ"
      confirmLoading={isPending}
      okButtonProps={{ icon: <Save className="w-4 h-4" /> }}
      maskClosable={!isPending}
      destroyOnHidden
      width={820}
    >
      <Form
        form={form}
        layout="vertical"
        disabled={isPending}
        className="pt-2"
      >
        <Form.Item
          name="title"
          label="Tiêu đề khóa học"
          rules={[{ required: true, message: 'Vui lòng nhập tiêu đề' }]}
        >
          <Input
            placeholder="VD: Tiếng Nhật N5 - Nền tảng cho người mới bắt đầu"
            size="large"
          />
        </Form.Item>


        <Form.Item
          name="jlptLevel"
          label="Cấp độ"
          rules={[{ required: true, message: 'Vui lòng chọn cấp độ' }]}
        >
          <Select size="large" placeholder="Chọn cấp độ">
            {JLPT_LEVELS.map((lvl) => (
              <Select.Option key={lvl} value={lvl}>
                {lvl}
              </Select.Option>
            ))}
          </Select>
        </Form.Item>

        <Form.Item
          name="description"
          label="Mô tả"
          rules={[{ max: 2000, message: 'Mô tả tối đa 2000 ký tự' }]}
        >
          <Input.TextArea
            rows={3}
            placeholder="Mô tả ngắn về khóa học, mục tiêu, đối tượng..."
            showCount
            maxLength={2000}
          />
        </Form.Item>

        <Form.Item
          name="thumbnailUrl"
          label="Ảnh bìa"
          rules={[
            {
              type: 'url',
              message: 'URL không hợp lệ (phải bắt đầu bằng http/https)',
            },
          ]}
        >
          <MediaUploadField accept="image" folder="elearningJP/courses/thumbnails" />
        </Form.Item>

        {/* {isEdit && (
          <Form.Item label="Trạng thái xuất bản">
            <Switch
              checked={course?.published ?? false}
              disabled
              checkedChildren="Đã xuất bản"
              unCheckedChildren="Bản nháp"
            />
            <span className="ml-2 text-xs text-gray-500">
              Dùng nút "Xuất bản / Bản nháp" trong trang chi tiết khoá học.
            </span>
          </Form.Item>
        )} */}
      </Form>
    </Modal>
  )
}
