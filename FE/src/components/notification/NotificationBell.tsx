import React, { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Popover, Button } from 'antd'
import { Bell, CheckCheck, Inbox, Loader2 } from 'lucide-react'
import {
  NOTIFICATION_FEED_PAGE_SIZE,
  useMarkAllAsRead,
  useNotificationFeed,
  useUnreadCount,
} from '@/hooks/useNotifications'
import { useOpenNotification } from '@/hooks/useOpenNotification'
import { useNotificationStore } from '@/stores/useNotificationStore'
import { cn } from '@/utils/cn'
import { NotificationItem } from './NotificationItem'
import type { AppNotification } from '@/types'

/**
 * Chuông thông báo trên TopBar.
 *
 * <p>Danh sách kiểu Facebook: mở ra thấy {@link NOTIFICATION_FEED_PAGE_SIZE} thông báo mới
 * nhất (cả đã đọc lẫn chưa đọc). Nút "Xem thông báo trước" tải thêm 1 trang; từ đó kéo tới
 * cuối danh sách sẽ tự tải tiếp từng trang.</p>
 *
 * <p>Badge lấy từ store (cập nhật tức thời khi STOMP đẩy message), có
 * `useUnreadCount` làm lưới an toàn (refetch 60s + window focus).</p>
 */
export const NotificationBell: React.FC = () => {
  const [open, setOpen] = useState(false)
  // false: chỉ hiện trang đầu + nút "Xem thông báo trước"; true: cuộn tới cuối là tải thêm.
  const [expanded, setExpanded] = useState(false)
  const openNotification = useOpenNotification()

  const listRef = useRef<HTMLDivElement>(null)
  const sentinelRef = useRef<HTMLDivElement>(null)

  const storeUnread = useNotificationStore((s) => s.unreadCount)
  const storeItems = useNotificationStore((s) => s.items)
  const hydrated = useNotificationStore((s) => s.hydrated)
  const setUnreadCount = useNotificationStore((s) => s.setUnreadCount)

  const { data: unreadCount } = useUnreadCount()
  const {
    data: feed,
    isLoading,
    isError,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useNotificationFeed({ enabled: open })
  const { mutate: markAllAsRead, isPending: isMarkingAll } = useMarkAllAsRead()

  // Đồng bộ store từ REST mỗi khi REST trả số mới (socket rớt / vừa load trang).
  // Chỉ ghi khi giá trị thật sự khác để không tạo vòng lặp render.
  useEffect(() => {
    const value = unreadCount?.count
    if (typeof value === 'number' && value !== storeUnread) {
      setUnreadCount(value)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [unreadCount?.count])

  // Store là nguồn chính sau khi đã hydrate (socket đẩy chính xác + mỗi lần đánh dấu
  // đã đọc đều giảm store ngay); REST chỉ dùng lúc chưa có dữ liệu.
  const badgeCount = hydrated ? storeUnread : unreadCount?.count ?? 0

  // Gộp: thông báo real-time vừa nhận + các trang đã tải, khử trùng theo id
  // (thông báo mới đến khi đang cuộn làm trang sau lặp lại mục cuối của trang trước).
  const allItems = useMemo<AppNotification[]>(() => {
    const map = new Map<number, AppNotification>()
    const add = (n: AppNotification) => {
      const existing = map.get(n.id)
      map.set(n.id, existing ? { ...existing, read: existing.read || n.read } : n)
    }
    storeItems.forEach(add)
    feed?.pages.forEach((page) => page.items.forEach(add))
    return Array.from(map.values()).sort((a, b) => b.id - a.id)
  }, [storeItems, feed])

  const items = expanded ? allItems : allItems.slice(0, NOTIFICATION_FEED_PAGE_SIZE)
  const hasHiddenItems = allItems.length > items.length
  const canShowOlder = !expanded && (hasHiddenItems || !!hasNextPage)

  // Cuộn tới cuối danh sách → tải trang kế tiếp (chỉ sau khi đã bấm "Xem thông báo trước").
  useEffect(() => {
    if (!open || !expanded || !hasNextPage) return
    const root = listRef.current
    const sentinel = sentinelRef.current
    if (!root || !sentinel) return

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries[0]?.isIntersecting && !isFetchingNextPage) {
          void fetchNextPage()
        }
      },
      { root, rootMargin: '80px' },
    )
    observer.observe(sentinel)
    return () => observer.disconnect()
  }, [open, expanded, hasNextPage, isFetchingNextPage, fetchNextPage])

  const handleOpenChange = (next: boolean) => {
    setOpen(next)
    if (!next) {
      // Mở lại thì quay về trạng thái ban đầu: 10 thông báo mới nhất + nút xem thêm.
      setExpanded(false)
      listRef.current?.scrollTo({ top: 0 })
    }
  }

  const handleShowOlder = () => {
    setExpanded(true)
    // Đã có sẵn mục ẩn (real-time đẩy vào) thì hiện chúng trước; hết thì tải trang kế.
    if (!hasHiddenItems && hasNextPage && !isFetchingNextPage) {
      void fetchNextPage()
    }
  }

  // Bấm mục nào cũng đóng popover (kể cả loại không có trang đích) cho cách phản hồi nhất quán
  const handleItemClick = (notification: AppNotification) => {
    handleOpenChange(false)
    void openNotification(notification)
  }

  // Rộng 100vw - 2×12px lề - 24px khung popover → vừa khít màn hẹp; desktop tối đa 360px
  const content = (
    <div className="w-[calc(100vw-48px)] max-w-[360px] -my-2 -mx-2">
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
      <div ref={listRef} className="max-h-[420px] overflow-y-auto">
        {isLoading && allItems.length === 0 ? (
          <div className="flex items-center justify-center gap-2 py-8 text-sm text-gray-500">
            <Loader2 className="w-4 h-4 animate-spin" />
            Đang tải…
          </div>
        ) : allItems.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-1.5 py-10 text-gray-400">
            <Inbox className="w-7 h-7" />
            <span className="text-sm">
              {isError ? 'Không tải được thông báo' : 'Chưa có thông báo nào'}
            </span>
          </div>
        ) : (
          <>
            <ul className="divide-y divide-gray-50">
              {items.map((n) => (
                <li key={n.id}>
                  <NotificationItem notification={n} onClick={handleItemClick} />
                </li>
              ))}
            </ul>

            {canShowOlder && (
              <div className="px-3 py-2 border-t border-gray-100">
                <Button
                  block
                  onClick={handleShowOlder}
                  loading={isFetchingNextPage}
                  className="!text-[13px] !font-semibold !text-gray-700"
                >
                  Xem thông báo trước
                </Button>
              </div>
            )}

            {expanded && (
              <div ref={sentinelRef} className="py-3 text-center text-[11px] text-gray-400">
                {isFetchingNextPage ? (
                  <span className="inline-flex items-center gap-1.5">
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    Đang tải thêm…
                  </span>
                ) : hasNextPage ? (
                  ' '
                ) : (
                  'Bạn đã xem hết thông báo'
                )}
              </div>
            )}
          </>
        )}
      </div>

      {/* Footer — số chưa đọc + lối vào trang đầy đủ */}
      <div className="flex items-center justify-between gap-2 px-3 py-2 border-t border-gray-100">
        <span className="text-[11px] text-gray-400">
          {badgeCount > 0 ? `Bạn còn ${badgeCount > 99 ? '99+' : badgeCount} thông báo chưa đọc` : ''}
        </span>
        <Link
          to="/notifications"
          onClick={() => handleOpenChange(false)}
          className="text-xs font-semibold text-indigo-600 hover:text-indigo-700"
        >
          Xem tất cả
        </Link>
      </div>
    </div>
  )

  return (
    <Popover
      content={content}
      trigger="click"
      placement="bottomRight"
      open={open}
      onOpenChange={handleOpenChange}
      arrow={false}
      rootClassName="notification-popover"
      styles={{ content: { padding: 8 } }}
      // TopBar là `fixed`: render popup vào body thì khi cuộn trang popup trôi theo trang rồi
      // mới được căn lại (nhảy ra rồi quay về). Gắn vào ngay trong header để đứng yên cùng chuông.
      getPopupContainer={(trigger) => trigger.parentElement ?? document.body}
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
