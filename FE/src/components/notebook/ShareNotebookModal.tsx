import React, { useState } from 'react'
import { Modal, Button, Switch, Tooltip, message } from 'antd'
import { CopyOutlined, CheckOutlined } from '@ant-design/icons'
import type { Notebook } from '@/types'

interface ShareNotebookModalProps {
  open: boolean
  notebook: Notebook | null
  onClose: () => void
  onToggleSharing?: (enabled: boolean) => Promise<void>
}

export const ShareNotebookModal: React.FC<ShareNotebookModalProps> = ({
  open,
  notebook,
  onClose,
  onToggleSharing,
}) => {
  const [copiedCode, setCopiedCode] = useState(false)
  const [copiedLink, setCopiedLink] = useState(false)
  const [togglingSharing, setTogglingSharing] = useState(false)

  if (!notebook) return null

  const shareCode = notebook.shareCode || '------'
  const shareLink = `${window.location.origin}/notebooks/share/${shareCode}`
  const isEnabled = Boolean(notebook.sharingEnabled || notebook.isPublic)

  const handleCopyCode = async () => {
    if (!shareCode || shareCode === '------') return
    try {
      await navigator.clipboard.writeText(shareCode)
      setCopiedCode(true)
      message.success('Đã sao chép mã chia sẻ!')
      setTimeout(() => setCopiedCode(false), 2000)
    } catch {
      message.error('Không thể sao chép vào bộ nhớ tạm')
    }
  }

  const handleCopyLink = async () => {
    try {
      await navigator.clipboard.writeText(shareLink)
      setCopiedLink(true)
      message.success('Đã sao chép đường link chia sẻ!')
      setTimeout(() => setCopiedLink(false), 2000)
    } catch {
      message.error('Không thể sao chép vào bộ nhớ tạm')
    }
  }

  const handleToggleSharing = async (checked: boolean) => {
    if (!onToggleSharing) return
    try {
      setTogglingSharing(true)
      await onToggleSharing(checked)
      message.success(
        checked
          ? 'Đã bật chia sẻ sổ tay lên cộng đồng!'
          : 'Đã tắt chia sẻ sổ tay (trở về riêng tư)'
      )
    } catch {
      message.error('Có lỗi xảy ra khi cập nhật trạng thái chia sẻ')
    } finally {
      setTogglingSharing(false)
    }
  }

  const letters = shareCode.split('')

  return (
    <Modal
      open={open}
      onCancel={onClose}
      footer={null}
      centered
      width={440}
      className="share-notebook-modal"
      destroyOnClose
    >
      <div className="text-center pt-3 pb-1">
        <h3 className="text-xl font-bold text-gray-800 mb-1">Chia sẻ sổ tay</h3>
        <p className="text-xs text-gray-500 mb-5">Mã chia sẻ của bạn</p>

        {/* Khung hiển thị các ô chữ cách điệu chuẩn như ảnh Mazii */}
        <div className="flex items-center justify-center gap-2 mb-5">
          {letters.map((char, index) => (
            <div
              key={index}
              className={`w-11 h-12 rounded-xl flex items-center justify-center text-xl font-bold transition-all shadow-xs ${
                isEnabled
                  ? 'bg-blue-600 text-white shadow-blue-200'
                  : 'bg-gray-200 text-gray-400'
              }`}
            >
              {char}
            </div>
          ))}
        </div>

        {/* Link chia sẻ kèm icon copy */}
        <div className="flex items-center justify-between bg-gray-50 hover:bg-gray-100/80 border border-gray-200/80 rounded-xl px-3.5 py-2.5 mb-5 transition-colors">
          <span className="text-xs text-gray-600 truncate max-w-[320px] font-mono">
            {shareLink}
          </span>
          <Tooltip title={copiedLink ? 'Đã sao chép' : 'Sao chép link'}>
            <button
              onClick={handleCopyLink}
              disabled={!isEnabled}
              className="text-gray-500 hover:text-blue-600 p-1 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed"
              aria-label="Sao chép link"
            >
              {copiedLink ? (
                <CheckOutlined className="text-emerald-500 text-sm" />
              ) : (
                <CopyOutlined className="text-sm" />
              )}
            </button>
          </Tooltip>
        </div>

        {/* Switch bật tắt chia sẻ chuẩn 100% Mazii */}
        <div className="py-3 px-3.5 bg-gray-50/80 border border-gray-100 rounded-2xl mb-6 text-left space-y-1.5">
          <div className="flex items-center justify-between">
            <span className="text-sm font-semibold text-gray-800">Cho phép chia sẻ</span>
            <Switch
              checked={isEnabled}
              loading={togglingSharing}
              onChange={handleToggleSharing}
              className={isEnabled ? 'bg-blue-600' : ''}
            />
          </div>
          <div className="text-[11px] leading-relaxed">
            {isEnabled ? (
              (notebook.wordCount || 0) >= 20 ? (
                <span className="text-emerald-600 font-medium">
                  ✓ Sổ tay đã đạt {notebook.wordCount} từ và đang hiển thị trên Khám phá cộng đồng.
                </span>
              ) : (
                <span className="text-amber-600">
                  ℹ Sổ tay có {notebook.wordCount || 0}/20 từ. Cần tối thiểu 20 từ để hiển thị trên Khám phá (vẫn chia sẻ qua link và mã bình thường).
                </span>
              )
            ) : (
              <span className="text-gray-500">
                Bật để chia sẻ qua mã, link và tự động xuất hiện trên Khám phá cộng đồng (khi có từ 20 từ trở lên).
              </span>
            )}
          </div>
        </div>

        {/* Hai nút hành động ở chân modal */}
        <div className="grid grid-cols-2 gap-3 pt-1">
          <Button
            size="large"
            onClick={onClose}
            className="rounded-xl border-gray-200 text-gray-600 hover:text-gray-800 font-medium"
          >
            Đóng
          </Button>
          <Button
            type="primary"
            size="large"
            onClick={handleCopyCode}
            disabled={!isEnabled}
            className="rounded-xl bg-blue-600 hover:bg-blue-700 font-medium"
          >
            {copiedCode ? 'Đã chép mã' : 'Sao chép mã'}
          </Button>
        </div>
      </div>
    </Modal>
  )
}
export default ShareNotebookModal
