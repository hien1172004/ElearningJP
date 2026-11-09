import React, { useState } from 'react'
import { formatNotebookCounts } from './itemTypeMeta'
import { Modal, Input, Button, message, Alert } from 'antd'
import { DownloadOutlined, KeyOutlined, CheckCircleOutlined } from '@ant-design/icons'
import { notebookService } from '@/services/notebook.service'
import type { Notebook } from '@/types'

interface ImportShareCodeModalProps {
  open: boolean
  onClose: () => void
  onSuccess: (clonedNotebook: Notebook) => void
}

export const ImportShareCodeModal: React.FC<ImportShareCodeModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [shareCode, setShareCode] = useState('')
  const [loading, setLoading] = useState(false)
  const [previewNotebook, setPreviewNotebook] = useState<Notebook | null>(null)
  const [searching, setSearching] = useState(false)

  const handleReset = () => {
    setShareCode('')
    setPreviewNotebook(null)
    setLoading(false)
    setSearching(false)
  }

  const handleClose = () => {
    handleReset()
    onClose()
  }

  const handleCheckCode = async () => {
    const code = shareCode.trim()
    if (!code) {
      message.warning('Vui lòng nhập mã chia sẻ 6 ký tự!')
      return
    }

    try {
      setSearching(true)
      const res = await notebookService.getNotebookByShareCode(code)
      if (res.data) {
        setPreviewNotebook(res.data)
      }
    } catch {
      message.error('Không tìm thấy sổ tay hoặc tính năng chia sẻ đã bị tắt!')
      setPreviewNotebook(null)
    } finally {
      setSearching(false)
    }
  }

  const handleClone = async () => {
    const code = shareCode.trim()
    if (!code) return

    try {
      setLoading(true)
      const res = await notebookService.cloneNotebookByShareCode(code)
      if (res.data) {
        message.success(`Đã nhân bản thành công sổ tay "${res.data.name}"!`)
        onSuccess(res.data)
        handleClose()
      }
    } catch {
      message.error('Có lỗi xảy ra khi nhân bản sổ tay!')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal
      title={
        <div className="flex items-center gap-2">
          <KeyOutlined className="text-blue-600 text-lg" />
          <span>Nhập sổ tay bằng mã chia sẻ</span>
        </div>
      }
      open={open}
      onCancel={handleClose}
      destroyOnClose
      width={460}
      footer={null}
    >
      <div className="space-y-4 my-2">
        <p className="text-xs text-gray-500">
          Nhập mã chia sẻ 6 ký tự được người khác cung cấp để tải toàn bộ từ vựng vào kho sổ tay cá nhân của bạn.
        </p>

        <div className="flex gap-2">
          <Input
            placeholder="Ví dụ: CtCLaC"
            value={shareCode}
            onChange={(e) => setShareCode(e.target.value.trim())}
            onPressEnter={handleCheckCode}
            maxLength={10}
            className="text-center font-mono font-bold tracking-widest text-lg"
            autoFocus
          />
          <Button
            type="primary"
            onClick={handleCheckCode}
            loading={searching}
            className="bg-blue-600 hover:bg-blue-700"
          >
            Kiểm tra
          </Button>
        </div>

        {previewNotebook && (
          <div className="p-4 bg-blue-50/60 rounded-2xl border border-blue-100 space-y-3">
            <div className="flex items-center gap-2 text-emerald-600 font-semibold text-sm">
              <CheckCircleOutlined /> Tìm thấy sổ tay hợp lệ!
            </div>
            <div className="space-y-1">
              <div className="text-base font-bold text-gray-900">
                {previewNotebook.name}
              </div>
              <div className="text-xs text-gray-500">
                Tác giả: <span className="font-medium text-gray-700">{previewNotebook.authorName || 'Ẩn danh'}</span> • {formatNotebookCounts(previewNotebook)}
              </div>
              {previewNotebook.description && (
                <div className="text-xs text-gray-600 italic">
                  "{previewNotebook.description}"
                </div>
              )}
            </div>

            <Alert
              type="info"
              showIcon
              message={
                <span className="text-[11px] text-gray-600">
                  Khi nhân bản, toàn bộ từ vựng sẽ được sao chép sang sổ tay mới của bạn. Tiến độ ghi nhớ SRS sẽ bắt đầu mới từ đầu.
                </span>
              }
              className="py-1"
            />

            <Button
              type="primary"
              icon={<DownloadOutlined />}
              onClick={handleClone}
              loading={loading}
              block
              size="large"
              className="bg-blue-600 hover:bg-blue-700 font-semibold rounded-xl"
            >
              Nhân bản vào kho của tôi
            </Button>
          </div>
        )}
      </div>
    </Modal>
  )
}
export default ImportShareCodeModal

