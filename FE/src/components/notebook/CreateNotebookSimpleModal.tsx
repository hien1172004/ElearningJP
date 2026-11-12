import React, { useState, useEffect } from 'react'
import { Modal, Input, Button } from 'antd'
import { FileTextOutlined } from '@ant-design/icons'
import { RequiredMark } from '@/components/common'

interface CreateNotebookSimpleModalProps {
  open: boolean
  loading?: boolean
  onClose: () => void
  onSubmit: (name: string) => Promise<void>
  /** Tên gợi ý điền sẵn khi mở (vd tên bài học) */
  defaultName?: string
}

export const CreateNotebookSimpleModal: React.FC<CreateNotebookSimpleModalProps> = ({
  open,
  loading = false,
  onClose,
  onSubmit,
  defaultName,
}) => {
  const [name, setName] = useState('')

  useEffect(() => {
    if (open) {
      setName(defaultName ?? '')
    }
  }, [open, defaultName])

  const handleSubmit = async () => {
    if (!name.trim()) return
    await onSubmit(name.trim())
    onClose()
  }

  const isValid = name.trim().length > 0

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      centered
      width={460}
      destroyOnClose
      className="mazii-modal"
    >
      <div className="pt-2 pb-1 space-y-5">
        {/* Title chuẩn như ảnh Mazii */}
        <div className="flex items-center gap-2.5 text-base font-bold text-gray-900">
          <FileTextOutlined className="text-gray-700 text-lg" />
          <span>Tạo sổ tay</span>
        </div>

        {/* Input Tên sổ tay */}
        <div className="space-y-2">
          <label className="block text-xs font-semibold text-gray-700">
            Tên sổ tay<RequiredMark />
          </label>
          <Input
            placeholder="Nhập tên sổ tay"
            value={name}
            onChange={(e) => setName(e.target.value)}
            onPressEnter={handleSubmit}
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
            autoFocus
          />
        </div>

        {/* Footer Buttons chuẩn ảnh 2 */}
        <div className="grid grid-cols-2 gap-3 pt-2">
          <Button
            size="large"
            onClick={onClose}
            className="h-11 rounded-xl bg-gray-100 hover:bg-gray-200 border-none text-gray-700 font-medium"
          >
            Hủy
          </Button>
          <Button
            type="primary"
            size="large"
            disabled={!isValid}
            loading={loading}
            onClick={handleSubmit}
            className={`h-11 rounded-xl font-medium transition-all ${
              isValid
                ? 'bg-blue-600 hover:bg-blue-700 text-white'
                : 'bg-gray-200 text-gray-400 border-none cursor-not-allowed'
            }`}
          >
            Thêm mới
          </Button>
        </div>
      </div>
    </Modal>
  )
}
export default CreateNotebookSimpleModal

