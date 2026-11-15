import React from 'react'
import { formatRelativeTime } from '@/utils/format'
import { cn } from '@/utils/cn'
import { NotificationIcon } from './NotificationIcon'
import type { AppNotification } from '@/types'

interface NotificationItemProps {
  notification: AppNotification
  onClick: (n: AppNotification) => void
  /** Nút phụ bên phải (vd nút xoá trên trang /notifications). */
  extra?: React.ReactNode
}

/** Một dòng thông báo — dùng chung cho chuông TopBar và trang /notifications để hiển thị giống nhau. */
export const NotificationItem: React.FC<NotificationItemProps> = ({ notification: n, onClick, extra }) => (
  <div className={cn('flex items-start transition hover:bg-gray-50', !n.read && 'bg-indigo-50/60')}>
    <button
      type="button"
      onClick={() => onClick(n)}
      className="flex-1 min-w-0 text-left flex items-start gap-2.5 px-3 py-2.5"
    >
      <NotificationIcon type={n.type} />
      <div className="min-w-0 flex-1">
        <div className="flex items-start gap-1.5">
          <p
            className={cn(
              'text-[13px] leading-snug line-clamp-2',
              n.read ? 'text-gray-700 font-medium' : 'text-gray-900 font-semibold',
            )}
          >
            {n.title}
          </p>
          {!n.read && <span className="mt-1 w-1.5 h-1.5 rounded-full bg-indigo-500 shrink-0" aria-label="Chưa đọc" />}
        </div>
        <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">{n.body}</p>
        <p className="mt-1 text-[11px] text-gray-400">{formatRelativeTime(n.createdAt)}</p>
      </div>
    </button>
    {extra && <div className="pr-2 pt-2 shrink-0">{extra}</div>}
  </div>
)
