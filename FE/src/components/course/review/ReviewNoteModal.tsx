import React, { useState } from 'react'
import { Input, Modal } from 'antd'

interface ReviewNoteModalProps {
  open: boolean
  title: string
  okText: string
  /** Bắt buộc nhập (vd: lý do từ chối). */
  required?: boolean
  danger?: boolean
  placeholder?: string
  loading?: boolean
  onSubmit: (text: string) => void
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
  onSubmit,
  onClose,
}) => {
  const [text, setText] = useState('')
  const [touched, setTouched] = useState(false)
  const missing = required && !text.trim()

  const close = () => {
    setText('')
    setTouched(false)
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
        onSubmit(text.trim())
        setText('')
        setTouched(false)
      }}
    >
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
    </Modal>
  )
}
