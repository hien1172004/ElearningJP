import React, { useEffect } from 'react'
import { Modal, Form, Input, InputNumber, App } from 'antd'
import { Save, Plus, Pencil } from 'lucide-react'
import { z } from 'zod'
import type { LessonCreateRequest, LessonDetail, LessonUpdateRequest } from '@/types'
import { useCreateLesson, useUpdateLesson } from '@/hooks'

const schema = z.object({
  title: z.string().min(1, 'Vui lòng nhập tiêu đề').max(200),
  orderIndex: z.number().int().min(1, 'Thứ tự phải >= 1'),
})

type FormValues = z.infer<typeof schema>

interface LessonFormModalProps {
  open: boolean
  courseId: number
  lesson?: LessonDetail | null
  /** orderIndex gợi ý khi tạo mới (= lessons.length + 1). */
  suggestedOrder?: number
  onClose: () => void
  onSuccess?: (lesson: LessonDetail) => void
}

export const LessonFormModal: React.FC<LessonFormModalProps> = ({
  open,
  courseId,
  lesson,
  suggestedOrder = 1,
  onClose,
  onSuccess,
}) => {
  const isEdit = !!lesson
  const { message } = App.useApp()
  const [form] = Form.useForm<FormValues>()

  const create = useCreateLesson(courseId)
  const update = useUpdateLesson(lesson?.id ?? 0, courseId)
  const isPending = create.isPending || update.isPending

  useEffect(() => {
    if (open) {
      if (lesson) {
        form.setFieldsValue({
          title: lesson.title,
          orderIndex: lesson.orderIndex,
        })
      } else {
        form.resetFields()
        form.setFieldsValue({ orderIndex: suggestedOrder })
      }
    }
  }, [open, lesson, suggestedOrder, form])

  const onFinish = async () => {
    try {
      const values = (await form.validateFields()) as FormValues
      const parsed = schema.safeParse(values)
      if (!parsed.success) {
        message.error(parsed.error.issues[0].message)
        return
      }
      if (isEdit && lesson) {
        const payload: LessonUpdateRequest = parsed.data
        const res = await update.mutateAsync(payload)
        if (res?.data) onSuccess?.(res.data as LessonDetail)
      } else {
        const payload: LessonCreateRequest = parsed.data
        const res = await create.mutateAsync(payload)
        if (res?.data) onSuccess?.(res.data as LessonDetail)
      }
      onClose()
    } catch {
      /* ignore */
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
            <Pencil className="w-5 h-5 text-indigo-600" />
          ) : (
            <Plus className="w-5 h-5 text-indigo-600" />
          )}
          <span>{isEdit ? 'Chỉnh sửa bài học' : 'Thêm bài học mới'}</span>
        </div>
      }
      okText={isEdit ? 'Lưu thay đổi' : 'Thêm bài học'}
      cancelText="Huỷ"
      confirmLoading={isPending}
      okButtonProps={{ icon: <Save className="w-4 h-4" /> }}
      maskClosable={!isPending}
      destroyOnHidden
      width={720}
    >
      <Form form={form} layout="vertical" requiredMark={false} className="pt-2">
        <Form.Item
          name="title"
          label="Tiêu đề bài học"
          rules={[
            { required: true, message: 'Vui lòng nhập tiêu đề' },
            { max: 200, message: 'Tối đa 200 ký tự' },
          ]}
        >
          <Input
            placeholder="VD: Bài 1 - Chào hỏi cơ bản (あいさつ)"
            size="large"
            showCount
            maxLength={200}
          />
        </Form.Item>

        <Form.Item
          name="orderIndex"
          label="Thứ tự hiển thị"
          // extra={
          //   <span className="text-xs text-gray-500">
          //     Số càng nhỏ càng hiển thị trước. Phải là duy nhất trong khoá học.
          //   </span>
          // }
          rules={[
            { required: true, message: 'Vui lòng nhập thứ tự' },
            { type: 'number', min: 1, message: 'Thứ tự phải >= 1' },
          ]}
        >
          <InputNumber size="large" min={1} step={1} className="!w-full" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
