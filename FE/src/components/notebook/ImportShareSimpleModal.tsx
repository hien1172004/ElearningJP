import React, { useState, useEffect } from 'react'
import { Modal, Input, Button, message } from 'antd'
import { PlusCircleOutlined } from '@ant-design/icons'

interface ImportShareSimpleModalProps {
  open: boolean
  loading?: boolean
  onClose: () => void
  onConfirm: (shareCode: string) => Promise<void>
}

export const ImportShareSimpleModal: React.FC<ImportShareSimpleModalProps> = ({
  open,
  loading = false,
  onClose,
  onConfirm,
}) => {
  const [code, setCode] = useState('')

  useEffect(() => {
    if (open) {
      setCode('')
    }
  }, [open])

  const handleConfirm = async () => {
    const trimmed = code.trim()
    if (!trimmed) {
      message.warning('Vui lòng nhập mã sổ tay!')
      return
    }
    await onConfirm(trimmed)
    onClose()
  }

  const isValid = code.trim().length > 0

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
        {/* Title chuẩn như ảnh 4 Mazii */}
        <div className="flex items-center gap-2.5 text-base font-bold text-gray-900">
          <PlusCircleOutlined className="text-gray-700 text-lg" />
          <span>Nhập mã sổ tay</span>
        </div>

        {/* Input: Mã sổ tay */}
        <div className="space-y-2">
          <label className="block text-xs font-semibold text-gray-700">
            Mã sổ tay
          </label>
          <Input
            placeholder="Mã sổ tay"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            onPressEnter={handleConfirm}
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500 font-mono"
            autoFocus
          />
        </div>

        {/* Footer Buttons chuẩn ảnh 4 */}
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
            onClick={handleConfirm}
            className={`h-11 rounded-xl font-medium transition-all ${
              isValid
                ? 'bg-blue-600 hover:bg-blue-700 text-white'
                : 'bg-gray-200 text-gray-400 border-none cursor-not-allowed'
            }`}
          >
            Xác nhận
          </Button>
        </div>
      </div>
    </Modal>
  )
}
export default ImportShareSimpleModal

