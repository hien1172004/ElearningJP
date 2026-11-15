import React, { useState } from 'react'
import { Button, Pagination, Segmented, Select } from 'antd'
import { CheckCheck, Inbox, Loader2, Trash2 } from 'lucide-react'
import {
  useDeleteNotification,
  useMarkAllAsRead,
  useNotifications,
} from '@/hooks/useNotifications'
import { useOpenNotification } from '@/hooks/useOpenNotification'
import { NotificationItem } from '@/components/notification/NotificationItem'
import { confirmDialog } from '@/components/common/ConfirmModal'
import type { NotificationType } from '@/types'

const PAGE_SIZE = 20

/** Nhóm loại cho bộ lọc — mỗi nhóm gửi danh sách loại lên BE (`types`). */
const GROUPS: { value: string; label: string; types: NotificationType[] }[] = [
  { value: 'all', label: 'Tất cả loại', types: [] },
  { value: 'study', label: 'Học tập', types: ['SRS_DUE', 'STREAK_WARNING', 'COURSE_STATUS_CHANGED', 'LESSON_UPDATED'] },
  { value: 'exam', label: 'Thi thử', types: ['EXAM_RESULT', 'EXAM_UPDATED'] },
  { value: 'community', label: 'Cộng đồng', types: ['COMMUNITY_REPLY', 'COMMUNITY_LIKE', 'COMMENT_MODERATED'] },
  { value: 'announcement', label: 'Thông báo chung', types: ['ANNOUNCEMENT'] },
  {
    value: 'teaching',
    label: 'Giảng dạy & duyệt bài',
    types: [
      'COURSE_TEACHER_ASSIGNMENT',
      'COURSE_REVIEW',
      'LESSON_SUBMITTED',
      'LESSON_WITHDRAWN',
      'LESSON_APPROVED',
      'LESSON_REJECTED',
      'DRAFT_STALE',
      'LESSON_DRAFT_DISCARDED',
    ],
  },
]

/** Trang xem toàn bộ thông báo: lọc chưa đọc / theo nhóm, xoá từng thông báo. */
export const NotificationsPage: React.FC = () => {
  const [page, setPage] = useState(1)
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [group, setGroup] = useState('all')
  const types = GROUPS.find((g) => g.value === group)?.types ?? []

  const { data, isLoading, isError, isFetching } = useNotifications(page, PAGE_SIZE, { unreadOnly, types })
  const { mutate: markAllAsRead, isPending: isMarkingAll } = useMarkAllAsRead()
  const { mutate: deleteNotification } = useDeleteNotification()
  const openNotification = useOpenNotification()

  const items = data?.items ?? []

  const handleDelete = async (id: number) => {
    const ok = await confirmDialog({ title: 'Xoá thông báo này?', okText: 'Xoá', danger: true })
    if (ok) deleteNotification(id)
  }

  return (
    <div className="max-w-3xl mx-auto space-y-4 pb-12">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-xl font-bold text-sumi">Thông báo</h1>
        <Button
          icon={<CheckCheck className="w-4 h-4" />}
          loading={isMarkingAll}
          disabled={!data?.unreadCount}
          onClick={() => markAllAsRead()}
        >
          Đánh dấu tất cả đã đọc
        </Button>
      </div>

      <div className="flex flex-wrap items-center gap-3">
        <Segmented
          value={unreadOnly ? 'unread' : 'all'}
          options={[
            { label: 'Tất cả', value: 'all' },
            { label: `Chưa đọc${data?.unreadCount ? ` (${data.unreadCount})` : ''}`, value: 'unread' },
          ]}
          onChange={(v) => {
            setUnreadOnly(v === 'unread')
            setPage(1)
          }}
        />
        <Select
          value={group}
          options={GROUPS.map(({ value, label }) => ({ value, label }))}
          onChange={(v) => {
            setGroup(v)
            setPage(1)
          }}
          className="min-w-[200px]"
          aria-label="Lọc theo nhóm thông báo"
        />
        {isFetching && !isLoading && <Loader2 className="w-4 h-4 animate-spin text-gray-400" />}
      </div>

      <div className="bg-surface rounded-xl border border-suna shadow-xs overflow-hidden">
        {isLoading ? (
          <div className="flex items-center justify-center gap-2 py-12 text-sm text-gray-500">
            <Loader2 className="w-4 h-4 animate-spin" />
            Đang tải…
          </div>
        ) : items.length === 0 ? (
          <div className="flex flex-col items-center justify-center gap-1.5 py-14 text-gray-400">
            <Inbox className="w-8 h-8" />
            <span className="text-sm">{isError ? 'Không tải được thông báo' : 'Không có thông báo nào'}</span>
          </div>
        ) : (
          <ul className="divide-y divide-gray-50">
            {items.map((n) => (
              <li key={n.id}>
                <NotificationItem
                  notification={n}
                  onClick={(item) => void openNotification(item)}
                  extra={
                    <button
                      type="button"
                      onClick={() => void handleDelete(n.id)}
                      className="p-1.5 rounded-md text-gray-400 hover:text-rose-600 hover:bg-rose-50 transition"
                      aria-label="Xoá thông báo"
                      title="Xoá thông báo"
                    >
                      <Trash2 className="w-4 h-4" />
                    </button>
                  }
                />
              </li>
            ))}
          </ul>
        )}
      </div>

      {(data?.total ?? 0) > PAGE_SIZE && (
        <div className="flex justify-center">
          <Pagination
            current={page}
            pageSize={PAGE_SIZE}
            total={data?.total ?? 0}
            onChange={setPage}
            showSizeChanger={false}
          />
        </div>
      )}
    </div>
  )
}

export default NotificationsPage
