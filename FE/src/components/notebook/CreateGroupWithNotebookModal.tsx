import React, { useState, useEffect } from 'react'
import { Modal, Input, Button } from 'antd'
import { FolderAddOutlined } from '@ant-design/icons'

interface CreateGroupWithNotebookModalProps {
  open: boolean
  loading?: boolean
  onClose: () => void
  onSubmit: (groupName: string, notebookName: string) => Promise<void>
}

export const CreateGroupWithNotebookModal: React.FC<CreateGroupWithNotebookModalProps> = ({
  open,
  loading = false,
  onClose,
  onSubmit,
}) => {
  const [groupName, setGroupName] = useState('')
  const [notebookName, setNotebookName] = useState('')

  useEffect(() => {
    if (open) {
      setGroupName('')
      setNotebookName('')
    }
  }, [open])

  const handleSubmit = async () => {
    if (!groupName.trim() || !notebookName.trim()) return
    await onSubmit(groupName.trim(), notebookName.trim())
    onClose()
  }

  const isValid = groupName.trim().length > 0 && notebookName.trim().length > 0

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
      <div className="pt-2 pb-1 space-y-4">
        {/* Title chuẩn như ảnh 3 Mazii */}
        <div className="flex items-center gap-2.5 text-base font-bold text-gray-900">
          <FolderAddOutlined className="text-gray-700 text-lg" />
          <span>Tạo mới nhóm sổ tay</span>
        </div>

        {/* Input 1: Tên nhóm sổ tay */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-gray-700">
            Tên nhóm sổ tay
          </label>
          <Input
            placeholder="Nhập tên nhóm sổ tay"
            value={groupName}
            onChange={(e) => setGroupName(e.target.value)}
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
            autoFocus
          />
        </div>

        {/* Input 2: Tên sổ tay */}
        <div className="space-y-1.5">
          <label className="block text-xs font-semibold text-gray-700">
            Tên sổ tay
          </label>
          <Input
            placeholder="Nhập tên sổ tay"
            value={notebookName}
            onChange={(e) => setNotebookName(e.target.value)}
            onPressEnter={handleSubmit}
            className="h-11 rounded-xl border-gray-200 text-sm focus:border-blue-500"
          />
        </div>

        {/* Footer Buttons chuẩn ảnh 3 */}
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
export default CreateGroupWithNotebookModal

