import React from 'react'
import { MoreOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons'
import { Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import type { NotebookGroup, NotebookGroupWithNotebooks } from '@/types'

// Icon thư mục xanh dương chuẩn Mazii (Folder tab bo tròn mượt)
export const MaziiFolderIcon: React.FC<{ className?: string }> = ({ className = 'w-7 h-7' }) => (
  <svg viewBox="0 0 24 24" fill="none" className={className}>
    <path
      d="M3 6.5C3 5.39543 3.89543 4.5 5 4.5H9.2C9.73043 4.5 10.2391 4.71071 10.6142 5.08579L11.8 6.27157C12.1751 6.64665 12.6838 6.85736 13.2142 6.85736H19C20.1046 6.85736 21 7.75193 21 8.85736V17.5C21 18.6046 20.1046 19.5 19 19.5H5C3.89543 19.5 3 18.6046 3 17.5V6.5Z"
      fill="#2563EB"
    />
  </svg>
)

interface NotebookGroupCardProps {
  group: NotebookGroup | NotebookGroupWithNotebooks
  notebookCount: number
  onClick: (group: NotebookGroup | NotebookGroupWithNotebooks) => void
  onEdit: (group: NotebookGroup | NotebookGroupWithNotebooks) => void
  onDelete: (group: NotebookGroup | NotebookGroupWithNotebooks) => void
}

export const NotebookGroupCard: React.FC<NotebookGroupCardProps> = ({
  group,
  notebookCount,
  onClick,
  onEdit,
  onDelete,
}) => {
  const menuItems: MenuProps['items'] = [
    {
      key: 'edit',
      label: 'Đổi tên nhóm',
      icon: <EditOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onEdit(group)
      },
    },
    {
      key: 'delete',
      danger: true,
      label: 'Xóa nhóm này',
      icon: <DeleteOutlined />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onDelete(group)
      },
    },
  ]

  const formattedDate = ('createdAt' in group && group.createdAt)
    ? new Date(group.createdAt).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    : ''

  return (
    <div
      onClick={() => onClick(group)}
      className="group relative bg-gray-50 hover:bg-surface border border-gray-100 hover:border-blue-400 hover:shadow-sm rounded-2xl p-4 transition-all duration-200 cursor-pointer flex flex-col justify-between min-h-[92px] select-none"
    >
      {/* Top: Icon thư mục xanh dương + Tên nhóm + Menu 3 chấm */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="shrink-0 group-hover:scale-105 transition-transform">
            <MaziiFolderIcon className="w-7 h-7" />
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-gray-800 text-sm truncate group-hover:text-blue-600 transition-colors">
              {group.name}
            </h3>
          </div>
        </div>

        <div onClick={(e) => e.stopPropagation()}>
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <button
              type="button"
              className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors cursor-pointer"
              aria-label="Thao tác nhóm"
            >
              <MoreOutlined className="text-base" />
            </button>
          </Dropdown>
        </div>
      </div>

      {/* Bottom: Số mục (1 mục, 2 mục...) + Ngày tạo */}
      <div className="mt-4 pt-2.5 border-t border-gray-100 flex items-center justify-between text-xs text-gray-400">
        <span className="font-medium text-gray-600">
          {notebookCount} mục
        </span>
        <span>{formattedDate}</span>
      </div>
    </div>
  )
}
export default NotebookGroupCard
