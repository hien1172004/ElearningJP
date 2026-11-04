import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ShieldAlert } from 'lucide-react'
import { Button as AntdButton, Input, Modal, Pagination, Segmented, Select, Tabs } from 'antd'
import { toast } from 'sonner'
import { PageHeader, LoadingState, EmptyState, SearchInput, confirmDialog } from '@/components/common'
import { ModerationItemCard, TARGET_TYPE_LABELS } from '@/components/admin'
import {
  useModerationAction,
  useModerationBrowse,
  useModerationQueue,
} from '@/hooks'
import { extractErrorMessage, useResolveCommentLink } from '@/hooks/useComments'
import { useAuthStore } from '@/stores'
import { isAdmin } from '@/utils/roles'
import type {
  CommentTargetType,
  ModerationAction,
  ModerationItem,
  ModerationKind,
  ModerationReportStatus,
  PageResponse,
} from '@/types'

const PAGE_SIZE = 10
const MAX_NOTE_LENGTH = 300

type TabKey = 'open' | 'handled' | 'browse'

/** Danh sách thẻ + phân trang + 3 trạng thái tải / rỗng / lỗi. */
const ItemList: React.FC<{
  data?: PageResponse<ModerationItem>
  isLoading: boolean
  isError: boolean
  onRetry: () => void
  page: number
  onPageChange: (page: number) => void
  emptyTitle: string
  emptyDescription: string
  showDismiss?: boolean
  busyKey: string | null
  onAction: (item: ModerationItem, action: ModerationAction) => void
  onOpen: (item: ModerationItem) => void
}> = ({
  data,
  isLoading,
  isError,
  onRetry,
  page,
  onPageChange,
  emptyTitle,
  emptyDescription,
  showDismiss,
  busyKey,
  onAction,
  onOpen,
}) => {
    if (isLoading) return <LoadingState message="Đang tải..." />
    if (isError) {
      return (
        <EmptyState
          icon={<ShieldAlert className="w-10 h-10 text-rose-400" />}
          title="Không tải được dữ liệu"
          description="Kiểm tra kết nối hoặc quyền truy cập rồi thử lại."
          action={<AntdButton onClick={onRetry}>Thử lại</AntdButton>}
        />
      )
    }
    const items = data?.items ?? data?.content ?? []
    if (items.length === 0) {
      return (
        <EmptyState
          icon={<ShieldAlert className="w-10 h-10 text-slate-300" />}
          title={emptyTitle}
          description={emptyDescription}
        />
      )
    }
    return (
      <div className="space-y-3">
        {items.map((item) => (
          <ModerationItemCard
            key={`${item.kind}-${item.id}`}
            item={item}
            showDismiss={showDismiss}
            busy={busyKey === `${item.kind}-${item.id}`}
            onAction={onAction}
            onOpen={onOpen}
          />
        ))}
        {(data?.totalPages ?? 0) > 1 && (
          <div className="flex justify-center pt-2">
            <Pagination
              current={page}
              pageSize={PAGE_SIZE}
              total={data?.totalElements ?? 0}
              onChange={onPageChange}
              showSizeChanger={false}
            />
          </div>
        )}
      </div>
    )
  }

export const AdminModerationPage: React.FC = () => {
  const navigate = useNavigate()
  const admin = isAdmin(useAuthStore((s) => s.user))
  const resolveCommentLink = useResolveCommentLink()

  const [tab, setTab] = useState<TabKey>('open')
  const [openPage, setOpenPage] = useState(1)
  const [handledStatus, setHandledStatus] = useState<Exclude<ModerationReportStatus, 'OPEN'>>('RESOLVED')
  const [handledPage, setHandledPage] = useState(1)

  const [kind, setKind] = useState<ModerationKind>('COMMENT')
  const [browseStatus, setBrowseStatus] = useState<'VISIBLE' | 'HIDDEN' | undefined>()
  const [targetType, setTargetType] = useState<CommentTargetType | undefined>()
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [browsePage, setBrowsePage] = useState(1)

  // Hộp thoại nhập ghi chú khi ẩn / xóa
  const [pending, setPending] = useState<{ item: ModerationItem; action: 'hide' | 'delete' } | null>(null)
  const [note, setNote] = useState('')

  const openQueue = useModerationQueue({ status: 'OPEN', page: openPage, size: PAGE_SIZE })
  const handledQueue = useModerationQueue({ status: handledStatus, page: handledPage, size: PAGE_SIZE })
  const browse = useModerationBrowse({
    kind,
    status: browseStatus,
    targetType,
    keyword: keyword || undefined,
    page: browsePage,
    size: PAGE_SIZE,
  })
  const moderate = useModerationAction()
  const busyKey =
    moderate.isPending && moderate.variables ? `${moderate.variables.kind}-${moderate.variables.id}` : null

  const runAction = (item: ModerationItem, action: ModerationAction, actionNote?: string) =>
    moderate.mutate(
      { kind: item.kind, id: item.id, action, note: actionNote },
      { onSuccess: () => setPending(null) },
    )

  const handleAction = async (item: ModerationItem, action: ModerationAction) => {
    if (action === 'hide' || action === 'delete') {
      setNote('')
      setPending({ item, action })
      return
    }
    if (action === 'dismiss') {
      const ok = await confirmDialog({
        title: 'Đánh dấu không vi phạm?',
        content: 'Các báo cáo đang mở của nội dung này sẽ được đóng, nội dung vẫn hiển thị bình thường.',
        okText: 'Không vi phạm',
      })
      if (!ok) return
    }
    runAction(item, action)
  }

  const handleOpen = async (item: ModerationItem) => {
    try {
      const link = await resolveCommentLink(item.commentId, item.kind === 'REPLY' ? item.id : null)
      if (link) navigate(link)
      else toast.error('Không mở được vị trí bình luận.')
    } catch (err) {
      toast.error(extractErrorMessage(err, 'Bình luận không còn hiển thị.'))
    }
  }

  const listProps = { busyKey, onAction: handleAction, onOpen: handleOpen }
  const openCount = openQueue.data?.totalElements ?? 0

  return (
    <div className="max-w-5xl mx-auto px-4 py-6 space-y-5">
      <PageHeader
        // badge="Quản trị"
        title="Kiểm duyệt bình luận"
        subtitle={
          admin
            ? 'Xử lý báo cáo vi phạm và kiểm duyệt mọi bình luận trong hệ thống.'
            : 'Xử lý báo cáo vi phạm và kiểm duyệt bình luận trong các bài học thuộc khóa của bạn.'
        }
        variant="amber"
      />

      <Tabs
        activeKey={tab}
        onChange={(k) => setTab(k as TabKey)}
        items={[
          {
            key: 'open',
            label: `Chờ xử lý${openCount > 0 ? ` (${openCount})` : ''}`,
            children: (
              <ItemList
                {...listProps}
                data={openQueue.data}
                isLoading={openQueue.isLoading}
                isError={openQueue.isError}
                onRetry={() => void openQueue.refetch()}
                page={openPage}
                onPageChange={setOpenPage}
                showDismiss
                emptyTitle="Không có báo cáo nào đang chờ"
                emptyDescription="Khi người dùng báo cáo bình luận vi phạm, chúng sẽ xuất hiện ở đây."
              />
            ),
          },
          {
            key: 'handled',
            label: 'Đã xử lý',
            children: (
              <div className="space-y-4">
                <Segmented
                  value={handledStatus}
                  onChange={(v) => {
                    setHandledStatus(v as Exclude<ModerationReportStatus, 'OPEN'>)
                    setHandledPage(1)
                  }}
                  options={[
                    { label: 'Đã ẩn / xóa', value: 'RESOLVED' },
                    { label: 'Không vi phạm', value: 'REJECTED' },
                  ]}
                />
                <ItemList
                  {...listProps}
                  data={handledQueue.data}
                  isLoading={handledQueue.isLoading}
                  isError={handledQueue.isError}
                  onRetry={() => void handledQueue.refetch()}
                  page={handledPage}
                  onPageChange={setHandledPage}
                  emptyTitle="Chưa có báo cáo nào đã xử lý"
                  emptyDescription="Lịch sử các báo cáo đã xử lý sẽ hiện ở đây."
                />
              </div>
            ),
          },
          {
            key: 'browse',
            label: 'Tra cứu bình luận',
            children: (
              <div className="space-y-4">
                <div className="flex flex-col md:flex-row md:flex-wrap gap-3">
                  <Segmented
                    value={kind}
                    onChange={(v) => {
                      setKind(v as ModerationKind)
                      setBrowsePage(1)
                    }}
                    options={[
                      { label: 'Bình luận', value: 'COMMENT' },
                      { label: 'Trả lời', value: 'REPLY' },
                    ]}
                  />
                  <SearchInput
                    placeholder="Nội dung, tên hoặc email người viết"
                    value={keywordInput}
                    onChange={setKeywordInput}
                    onSearch={(v) => {
                      setKeyword(v)
                      setBrowsePage(1)
                    }}
                    className="md:max-w-xs"
                  />
                  <Select
                    allowClear
                    placeholder="Mọi trạng thái"
                    value={browseStatus}
                    onChange={(v?: 'VISIBLE' | 'HIDDEN') => {
                      setBrowseStatus(v)
                      setBrowsePage(1)
                    }}
                    options={[
                      { value: 'VISIBLE', label: 'Đang hiển thị' },
                      { value: 'HIDDEN', label: 'Đã ẩn' },
                    ]}
                    className="md:w-44"
                  />
                  {/* Giáo viên chỉ kiểm duyệt bình luận bài học → không cần lọc loại nội dung */}
                  {admin && (
                    <Select
                      allowClear
                      placeholder="Mọi loại nội dung"
                      value={targetType}
                      onChange={(v?: CommentTargetType) => {
                        setTargetType(v)
                        setBrowsePage(1)
                      }}
                      options={(Object.keys(TARGET_TYPE_LABELS) as CommentTargetType[]).map((t) => ({
                        value: t,
                        label: TARGET_TYPE_LABELS[t],
                      }))}
                      className="md:w-44"
                    />
                  )}
                </div>
                <ItemList
                  {...listProps}
                  data={browse.data}
                  isLoading={browse.isLoading}
                  isError={browse.isError}
                  onRetry={() => void browse.refetch()}
                  page={browsePage}
                  onPageChange={setBrowsePage}
                  emptyTitle="Không có bình luận phù hợp"
                  emptyDescription="Thử đổi từ khóa hoặc bộ lọc."
                />
              </div>
            ),
          },
        ]}
      />

      <Modal
        open={!!pending}
        title={pending?.action === 'delete' ? 'Xóa nội dung này?' : 'Ẩn nội dung này?'}
        okText={pending?.action === 'delete' ? 'Xóa' : 'Ẩn'}
        cancelText="Huỷ"
        okButtonProps={{ danger: pending?.action === 'delete' }}
        confirmLoading={moderate.isPending}
        onOk={() => pending && runAction(pending.item, pending.action, note.trim() || undefined)}
        onCancel={() => setPending(null)}
        centered
        destroyOnHidden
      >
        <p className="text-sm text-slate-600 mb-3">
          {pending?.action === 'delete'
            ? 'Nội dung sẽ bị xóa (kèm các trả lời bên dưới nếu là trả lời). Người viết sẽ nhận được thông báo.'
            : 'Nội dung sẽ bị gỡ khỏi trang công khai, có thể khôi phục lại sau. Người viết sẽ nhận được thông báo.'}
        </p>
        <Input.TextArea
          value={note}
          onChange={(e) => setNote(e.target.value)}
          placeholder="Lý do gửi kèm thông báo cho người viết (không bắt buộc)"
          maxLength={MAX_NOTE_LENGTH}
          showCount
          autoSize={{ minRows: 2, maxRows: 4 }}
        />
      </Modal>
    </div>
  )
}
