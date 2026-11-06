import React from 'react'
import { MoreVertical, Trash2, Edit3, Share2, FolderInput } from 'lucide-react'
import { Dropdown } from 'antd'
import type { MenuProps } from 'antd'
import type { Notebook } from '@/types'

// Icon sổ tay màu vàng chuẩn Mazii (Notepad bo góc có gáy và 3 dòng kẻ trắng)
export const MaziiNotebookIcon: React.FC<{ className?: string }> = ({ className = 'w-7 h-7' }) => (
  <svg viewBox="0 0 24 24" fill="none" className={className}>
    <rect x="4" y="3" width="16" height="18" rx="3" fill="#F59E0B" />
    <rect x="7" y="2" width="10" height="2.5" rx="1" fill="#D97706" />
    <path d="M8 8.5H16M8 12.5H16M8 16.5H13" stroke="white" strokeWidth="1.75" strokeLinecap="round" />
  </svg>
)

interface NotebookCardProps {
  notebook: Notebook
  onClick: (notebook: Notebook) => void
  onEdit: (notebook: Notebook) => void
  onDelete: (notebook: Notebook) => void
  onShare?: (notebook: Notebook) => void
  onMove?: (notebook: Notebook) => void
}

export const NotebookCard: React.FC<NotebookCardProps> = ({
  notebook,
  onClick,
  onEdit,
  onDelete,
  onShare,
  onMove,
}) => {
  const menuItems: MenuProps['items'] = [
    {
      key: 'edit',
      label: 'Chỉnh sửa sổ tay',
      icon: <Edit3 className="w-4 h-4" />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onEdit(notebook)
      },
    },
    {
      key: 'move',
      label: 'Chuyển nhóm...',
      icon: <FolderInput className="w-4 h-4" />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onMove?.(notebook)
      },
    },
    {
      key: 'share',
      label: 'Chia sẻ sổ tay',
      icon: <Share2 className="w-4 h-4" />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onShare?.(notebook)
      },
    },
    {
      type: 'divider',
    },
    {
      key: 'delete',
      danger: true,
      label: 'Xóa sổ tay',
      icon: <Trash2 className="w-4 h-4" />,
      onClick: (e) => {
        e.domEvent.stopPropagation()
        onDelete(notebook)
      },
    },
  ]

  const formattedDate = notebook.createdAt
    ? new Date(notebook.createdAt).toLocaleDateString('vi-VN', {
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
      })
    : ''

  return (
    <div
      onClick={() => onClick(notebook)}
      className="group relative bg-gray-50 hover:bg-surface border border-gray-100 hover:border-amber-300 hover:shadow-sm rounded-2xl p-4 transition-all duration-200 cursor-pointer flex flex-col justify-between min-h-[92px] select-none"
    >
      {/* Top: Icon sổ tay vàng + Tên sổ tay + Menu 3 chấm */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3 min-w-0">
          <div className="shrink-0 group-hover:scale-105 transition-transform">
            <MaziiNotebookIcon className="w-7 h-7" />
          </div>
          <div className="min-w-0">
            <h3 className="font-semibold text-gray-800 text-sm truncate group-hover:text-amber-600 transition-colors">
              {notebook.name}
            </h3>
            <div className="flex items-center gap-1.5 mt-0.5">
              {notebook.jlptLevel && (
                <span className="inline-block px-1.5 py-0.5 text-[10px] font-semibold rounded bg-blue-50 text-blue-600 border border-blue-100">
                  {notebook.jlptLevel}
                </span>
              )}
              {(notebook.sharingEnabled || notebook.isPublic) && (
                <span className="inline-block px-1.5 py-0.5 text-[10px] font-medium rounded bg-emerald-50 text-emerald-600 border border-emerald-100">
                  Đã chia sẻ
                </span>
              )}
            </div>
          </div>
        </div>

        <div onClick={(e) => e.stopPropagation()}>
          <Dropdown menu={{ items: menuItems }} trigger={['click']} placement="bottomRight">
            <button
              type="button"
              className="p-1 rounded-lg text-gray-400 hover:text-gray-600 hover:bg-gray-100 transition-colors cursor-pointer"
              aria-label="Thao tác sổ tay"
            >
              <MoreVertical className="w-4 h-4" />
            </button>
          </Dropdown>
        </div>
      </div>

      {/* Bottom: Số từ + Ngày tạo chuẩn Mazii */}
      <div className="mt-4 pt-2.5 border-t border-gray-100 flex items-center justify-between text-xs text-gray-400">
        <span className="font-medium text-gray-600">
          {notebook.wordCount || 0} từ
        </span>
        <span>{formattedDate}</span>
      </div>
    </div>
  )
}
export default NotebookCard

