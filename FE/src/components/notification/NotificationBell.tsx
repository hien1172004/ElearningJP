import React, { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Popover, Button } from 'antd'
import { Bell, CheckCheck, Inbox, Loader2 } from 'lucide-react'
import {
  useMarkAllAsRead,
  useMarkAsRead,
  useRecentNotifications,
  useUnreadCount,
} from '@/hooks/useNotifications'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { formatRelativeTime } from '@/utils/format'
import { cn } from '@/utils/cn'
import { NotificationIcon } from './NotificationIcon'
import type { AppNotification } from '@/types'

/** Số thông báo hiển thị trong dropdown — bấm "Xem tất cả" nếu cần nhiều hơn. */
const MAX_DROPDOWN_ITEMS = 10

/**
 * Chuông thông báo trên TopBar.
 *
 * <p>Badge lấy từ store (cập nhật tức thời khi STOMP đẩy message), có
 * `useUnreadCount` làm lưới an toàn (refetch 60s + window focus).</p>
 */
export const NotificationBell: React.FC = () => {
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()

  const storeUnread = useNotificationStore((s) => s.unreadCount)
  const storeItems = useNotificationStore((s) => s.items)
  const hydrated = useNotificationStore((s) => s.hydrated)
  const markReadLocal = useNotificationStore((s) => s.markReadLocal)
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount)

  const { data: unreadCount } = useUnreadCount()
  const { data: recent, isLoading } = useRecentNotifications({ enabled: open })
  const { mutate: markAsRead } = useMarkAsRead()
  const { mutate: markAllAsRead, isPending: isMarkingAll } = useMarkAllAsRead()

  // Đồng bộ store từ REST mỗi khi REST trả số mới (socket rớt / vừa load trang).
  // Chỉ ghi khi giá trị thật sự khác để không tạo vòng lặp render.
  React.useEffect(() => {
    const value = unreadCount?.count
    if (typeof value === 'number' && value !== storeUnread) {
      setUnreadCount(value)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [unreadCount?.count])

  // Store là nguồn chính sau khi đã hydrate (socket đẩy chính xác + mỗi lần đánh dấu
  // đã đọc đều giảm store ngay); REST chỉ dùng lúc chưa có dữ liệu.
  const badgeCount = hydrated ? storeUnread : unreadCount?.count ?? 0

  const items = useMemo<AppNotification[]>(() => {
    // Gộp: item real-time mới nhận + item từ REST, khử trùng theo id.
    const map = new Map<number, AppNotification>()
    storeItems.forEach((n) => map.set(n.id, n))
    ;(recent ?? []).forEach((n) => {
      if (!map.has(n.id)) map.set(n.id, n)
    })
    return Array.from(map.values())
      .sort((a, b) => b.id - a.id)
      .slice(0, MAX_DROPDOWN_ITEMS)
  }, [storeItems, recent])

  const handleItemClick = (notification: AppNotification) => {
    if (!notification.read) {
      markReadLocal(notification.id)
      markAsRead(notification.id)
    }

    const data = notification.dataJson
    if (
      (notification.type === 'COMMUNITY_REPLY' ||
        notification.type === 'COMMUNITY_LIKE' ||
        notification.type === 'COMMUNITY_DISLIKE') &&
      data?.targetType
    ) {
      setOpen(false)
      navigate(resolveTargetUrl(data.targetType))
    }
  }

  const content = (
    <div className="w-[320px] sm:w-[360px] -my-2 -mx-2">
      {/* Header */}
      <div className="flex items-center justify-between px-3 py-2.5 border-b border-gray-100">
        <span className="text-sm font-bold text-gray-900">Thông báo</span>
        {badgeCount > 0 && (
          <Button
            type="text"
            size="small"
            loading={isMarkingAll}
            onClick={() => markAllAsRead()}
            className="!text-xs !font-semibold !text-indigo-600 hover:!text-indigo-700"
          >
            <span className="inline-flex items-center gap-1">
              <CheckCheck className="w-3.5 h-3.5" />
              Đánh dấu tất cả đã đọc
            </span>
          </Button>
        )}
      </div>

      {/* List */}
      <div className="max-h-[380px] overflow-y-auto">
        {isLoading && items.length === 0 ? (
          <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
            <Loader2 className="w-4 h-4 animate-spin" />
            Đang tải…
          </div>
        ) : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-1.5 py-10 text-gray-400">
            <Inbox className="w-7 h-7" />
            <span className="text-sm">Không có thông báo mới</span>
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {items.map((n) => (
              <li key={n.id}>
                <button
                  type="button"
                  onClick={() => handleItemClick(n)}
                  className={cn(
                    'w-full text-left flex items-start gap-2.5 px-3 py-2.5 transition',
                    'hover:bg-gray-50',
                    !n.read && 'bg-indigo-50/60',
                  )}
                >
                  <NotificationIcon type={n.type} />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start gap-1.5">
                      <p
                        className={cn(
                          'text-[13px] leading-snug line-clamp-2',
                          n.read
                            ? 'text-gray-700 font-medium'
                            : 'text-gray-900 font-semibold',
                        )}
                      >
                        {n.title}
                      </p>
                      {!n.read && (
                        <span className="mt-1 w-1.5 h-1.5 rounded-full bg-indigo-500 shrink-0" />
                      )}
                    </div>
                    <p className="mt-0.5 text-xs text-gray-500 line-clamp-2">
                      {n.body}
                    </p>
                    <p className="mt-1 text-[11px] text-gray-400">
                      {formatRelativeTime(n.createdAt)}
                    </p>
                  </div>
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* Footer — chỉ hiện khi còn thông báo chưa đọc để gợi ý hành động */}
      {badgeCount > 0 && (
        <div className="px-3 py-2 border-t border-gray-100 text-center">
          <span className="text-[11px] text-gray-400">
            Bạn còn {badgeCount > 99 ? '99+' : badgeCount} thông báo chưa đọc
          </span>
        </div>
      )}
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      placement="bottomRight"
      open={open}
      onOpenChange={setOpen}
      arrow={false}
      styles={{ content: { padding: 8 } }}
    >
      <button
        type="button"
        aria-label={
          badgeCount > 0 ? `Thông báo (${badgeCount} chưa đọc)` : 'Thông báo'
        }
        title="Thông báo"
        className={cn(
          'relative inline-flex items-center justify-center',
          'w-9 h-9 rounded-md text-gray-600 hover:text-indigo-600 hover:bg-gray-100 transition',
          'focus:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500',
        )}
      >
        <Bell className="w-5 h-5" />
        {badgeCount > 0 && (
          <span
            className={cn(
              'absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1',
              'inline-flex items-center justify-center rounded-full',
              'bg-rose-500 text-white text-[10px] font-bold leading-none',
              'ring-2 ring-white',
            )}
          >
            {badgeCount > 99 ? '99+' : badgeCount}
          </span>
        )}
      </button>
    </Popover>
  )
}

/**
 * Map target của thông báo → URL trên FE.
 *
 * `q` của trang tra cứu là TỪ KHÓA, không phải id, nên không thể deep-link chính xác
 * bằng `targetId`; ta chỉ mở đúng tab để user tự tra tiếp. LESSON cần
 * `courseSlug/lessonSlug` nên đưa về danh sách khóa học.
 */
function resolveTargetUrl(
  targetType: NonNullable<AppNotification['dataJson']>['targetType'],
): string {
  switch (targetType) {
    case 'VOCABULARY':
      return '/dictionary?tab=vocab'
    case 'KANJI':
      return '/dictionary?tab=kanji'
    case 'GRAMMAR':
      return '/dictionary?tab=grammar'
    case 'LESSON':
    default:
      return '/courses'
  }
}
