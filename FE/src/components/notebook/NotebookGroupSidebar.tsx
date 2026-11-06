import React, { useState } from 'react'
import { Folder, FolderPlus, MoreHorizontal, Edit2, Trash2 } from 'lucide-react'
import { Dropdown, Modal, Form, Input } from 'antd'
import type { NotebookGroup, CreateNotebookGroupRequest } from '@/types'

interface NotebookGroupSidebarProps {
  groups: NotebookGroup[]
  selectedGroupId: number | null
  ungroupedCount: number
  totalCount: number
  onSelectGroup: (groupId: number | null) => void
  onCreateGroup: (data: CreateNotebookGroupRequest) => Promise<void>
  onUpdateGroup: (id: number, data: CreateNotebookGroupRequest) => Promise<void>
  onDeleteGroup: (id: number) => Promise<void>
}

export const NotebookGroupSidebar: React.FC<NotebookGroupSidebarProps> = ({
  groups,
  selectedGroupId,
  ungroupedCount,
  totalCount,
  onSelectGroup,
  onCreateGroup,
  onUpdateGroup,
  onDeleteGroup,
}) => {
  const [modalOpen, setModalOpen] = useState(false)
  const [editingGroup, setEditingGroup] = useState<NotebookGroup | null>(null)
  const [form] = Form.useForm()

  const handleOpenCreate = () => {
    setEditingGroup(null)
    form.resetFields()
    setModalOpen(true)
  }

  const handleOpenEdit = (group: NotebookGroup) => {
    setEditingGroup(group)
    form.setFieldsValue({ name: group.name })
    setModalOpen(true)
  }

  const handleModalSubmit = async (values: { name: string }) => {
    if (editingGroup) {
      await onUpdateGroup(editingGroup.id, values)
    } else {
      await onCreateGroup(values)
    }
    setModalOpen(false)
    form.resetFields()
  }

  return (
    <aside className="w-64 bg-surface border border-gray-100 rounded-2xl p-4 flex flex-col h-full shadow-xs">
      <div className="flex items-center justify-between pb-3 mb-2 border-b border-gray-100">
        <h2 className="font-semibold text-gray-700 text-sm">Nhóm sổ tay</h2>
        <button
          type="button"
          onClick={handleOpenCreate}
          className="flex items-center gap-1 text-xs text-blue-600 hover:text-blue-700 font-medium px-2 py-1 rounded-lg hover:bg-blue-50 transition-colors"
        >
          <FolderPlus className="w-3.5 h-3.5" />
          <span>Thêm nhóm</span>
        </button>
      </div>

      <div className="flex-1 overflow-y-auto space-y-1 pr-1">
        {/* Tất cả */}
        <button
          type="button"
          onClick={() => onSelectGroup(null)}
          className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-sm font-medium transition-colors ${
            selectedGroupId === null
              ? 'bg-blue-50 text-blue-600 font-semibold'
              : 'text-gray-600 hover:bg-gray-50'
          }`}
        >
          <div className="flex items-center gap-2.5 truncate">
            <Folder className={`w-4 h-4 ${selectedGroupId === null ? 'fill-blue-400/20' : ''}`} />
            <span className="truncate">Tất cả sổ tay</span>
          </div>
          <span className="text-xs text-gray-400 bg-surface px-2 py-0.5 rounded-full border border-gray-100">
            {totalCount}
          </span>
        </button>

        {/* Chưa phân nhóm */}
        <button
          type="button"
          onClick={() => onSelectGroup(-1)}
          className={`w-full flex items-center justify-between px-3 py-2 rounded-xl text-sm font-medium transition-colors ${
            selectedGroupId === -1
              ? 'bg-blue-50 text-blue-600 font-semibold'
              : 'text-gray-600 hover:bg-gray-50'
          }`}
        >
          <div className="flex items-center gap-2.5 truncate">
            <Folder className="w-4 h-4 text-gray-400" />
            <span className="truncate">Chưa phân nhóm</span>
          </div>
          <span className="text-xs text-gray-400 bg-surface px-2 py-0.5 rounded-full border border-gray-100">
            {ungroupedCount}
          </span>
        </button>

        {/* Danh sách nhóm cá nhân */}
        {groups.map((group) => {
          const isSelected = selectedGroupId === group.id
          return (
            <div
              key={group.id}
              onClick={() => onSelectGroup(group.id)}
              className={`group flex items-center justify-between px-3 py-2 rounded-xl text-sm font-medium transition-colors cursor-pointer ${
                isSelected
                  ? 'bg-blue-50 text-blue-600 font-semibold'
                  : 'text-gray-600 hover:bg-gray-50'
              }`}
            >
              <div className="flex items-center gap-2.5 min-w-0">
                <Folder className={`w-4 h-4 shrink-0 ${isSelected ? 'fill-blue-400/20' : ''}`} />
                <span className="truncate">{group.name}</span>
              </div>

              <div className="flex items-center gap-1" onClick={(e) => e.stopPropagation()}>
                <span className="text-xs text-gray-400 bg-surface px-2 py-0.5 rounded-full border border-gray-100">
                  {group.notebookCount || 0}
                </span>

                <Dropdown
                  menu={{
                    items: [
                      {
                        key: 'edit',
                        label: 'Đổi tên nhóm',
                        icon: <Edit2 className="w-3.5 h-3.5" />,
                        onClick: () => handleOpenEdit(group),
                      },
                      {
                        key: 'delete',
                        danger: true,
                        label: 'Xóa nhóm',
                        icon: <Trash2 className="w-3.5 h-3.5" />,
                        onClick: () => onDeleteGroup(group.id),
                      },
                    ],
                  }}
                  trigger={['click']}
                  placement="bottomRight"
                >
                  <button
                    type="button"
                    className="p-1 rounded-md opacity-0 group-hover:opacity-100 text-gray-400 hover:text-gray-600 hover:bg-gray-200/50 transition-opacity"
                  >
                    <MoreHorizontal className="w-3.5 h-3.5" />
                  </button>
                </Dropdown>
              </div>
            </div>
          )
        })}
      </div>

      {/* Modal tạo/sửa nhóm */}
      <Modal
        title={editingGroup ? 'Đổi tên nhóm' : 'Tạo nhóm sổ tay mới'}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        onOk={() => form.submit()}
        okText={editingGroup ? 'Lưu' : 'Tạo nhóm'}
        cancelText="Hủy"
        centered
      >
        <Form form={form} layout="vertical" onFinish={handleModalSubmit} className="mt-4">
          <Form.Item
            name="name"
            label="Tên nhóm"
            rules={[
              { required: true, message: 'Vui lòng nhập tên nhóm' },
              { max: 100, message: 'Tên không quá 100 ký tự' },
            ]}
          >
            <Input placeholder="Ví dụ: Luyện thi JLPT N2, Từ vựng chuyên ngành..." />
          </Form.Item>
        </Form>
      </Modal>
    </aside>
  )
}

