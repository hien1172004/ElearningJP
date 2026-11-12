import React, { useState } from 'react'
import { Checkbox, Input, Modal } from 'antd'
import { RequiredMark } from '@/components/common'

interface ReviewNoteModalProps {
  open: boolean
  title: string
  okText: string
  /** Bắt buộc nhập (vd: lý do từ chối). */
  required?: boolean
  danger?: boolean
  placeholder?: string
  loading?: boolean
  /** Có thì hiện thêm 1 ô tick (vd: "Thay đổi lớn"); giá trị trả qua tham số thứ 2 của onSubmit. */
  checkboxLabel?: string
  onSubmit: (text: string, checked: boolean) => void
  onClose: () => void
}

const MAX = 1000

/** Hộp thoại nhập ghi chú khi gửi duyệt / duyệt, hoặc lý do từ chối (bắt buộc). */
export const ReviewNoteModal: React.FC<ReviewNoteModalProps> = ({
  open,
  title,
  okText,
  required,
  danger,
  placeholder,
  loading,
  checkboxLabel,
  onSubmit,
  onClose,
}) => {
  const [text, setText] = useState('')
  const [checked, setChecked] = useState(false)
  const [touched, setTouched] = useState(false)
  const missing = required && !text.trim()

  const close = () => {
    setText('')
    setTouched(false)
    setChecked(false)
    onClose()
  }

  return (
    <Modal
      open={open}
      title={title}
      okText={okText}
      cancelText="Hủy"
      centered
      destroyOnHidden
      okButtonProps={{ danger, loading }}
      onCancel={close}
      onOk={() => {
        setTouched(true)
        if (missing) return
        onSubmit(text.trim(), checked)
        setText('')
        setTouched(false)
        setChecked(false)
      }}
    >
      {required && (
        <p className="mb-1 text-xs font-semibold text-gray-700">
          Lý do
          <RequiredMark />
        </p>
      )}
      <Input.TextArea
        value={text}
        onChange={(e) => setText(e.target.value)}
        onBlur={() => setTouched(true)}
        maxLength={MAX}
        showCount
        autoSize={{ minRows: 3, maxRows: 8 }}
        placeholder={placeholder}
        status={touched && missing ? 'error' : undefined}
        className='mb-2'
      />
      {touched && missing && <p className="text-xs text-rose-600 mt-1">Vui lòng nhập lý do.</p>}
      {checkboxLabel && (
        <Checkbox checked={checked} onChange={(e) => setChecked(e.target.checked)} className="mt-1">
          {checkboxLabel}
        </Checkbox>
      )}
    </Modal>
  )
}
