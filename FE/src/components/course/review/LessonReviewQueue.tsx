import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button as AntdButton, Select, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckCircle2, ExternalLink, Eye, XCircle } from 'lucide-react'
import { EmptyState, FilterBar, SearchInput } from '@/components/common'
import { useApproveLessonReview, useLessonReviewQueue, useRejectLessonReview } from '@/hooks'
import { formatRelativeTime } from '@/utils/format'
import type { LessonReviewRequest, ReviewRequestStatus } from '@/types'
import { ReviewNoteModal } from './ReviewNoteModal'
import { SubmittedLessonModal } from './LessonDraftPanel'

const PAGE_SIZE = 10

const STATUS_OPTIONS: { label: string; value: ReviewRequestStatus }[] = [
  { label: 'Đang chờ', value: 'PENDING' },
  { label: 'Đã duyệt', value: 'APPROVED' },
  { label: 'Bị từ chối', value: 'REJECTED' },
  { label: 'Đã rút', value: 'WITHDRAWN' },
]

const STATUS_TAG: Record<ReviewRequestStatus, { text: string; color: string }> = {
  PENDING: { text: 'Đang chờ', color: 'gold' },
  APPROVED: { text: 'Đã duyệt', color: 'green' },
  REJECTED: { text: 'Bị từ chối', color: 'red' },
  WITHDRAWN: { text: 'Đã rút', color: 'default' },
}

/**
 * Hàng chờ duyệt theo bài (ADMIN): mỗi dòng 1 bài. Duyệt → bài lên bản học viên đang học (các bài khác giữ nguyên);
 * từ chối bắt buộc lý do. Hai ADMIN cùng xử lý: người sau nhận kết quả của người trước.
 */
export const LessonReviewQueue: React.FC = () => {
  const navigate = useNavigate()
  const [status, setStatus] = useState<ReviewRequestStatus>('PENDING')
  const [page, setPage] = useState(1)
  const [keywordInput, setKeywordInput] = useState('')
  const [keyword, setKeyword] = useState('')
  const [target, setTarget] = useState<{ request: LessonReviewRequest; action: 'approve' | 'reject' | 'content' } | null>(null)

  const { data, isLoading, isFetching, isError } = useLessonReviewQueue({
    status,
    keyword: keyword || undefined,
    page,
    size: PAGE_SIZE,
  })
  const approve = useApproveLessonReview()
  const reject = useRejectLessonReview()
  const items = data?.items ?? []
  const close = () => setTarget(null)
  const vars = (r: LessonReviewRequest) => ({ requestId: r.id, courseId: r.courseId, lessonId: r.lessonId })

  const columns: ColumnsType<LessonReviewRequest> = [
    {
      title: 'Bài học',
      key: 'lesson',
      render: (_, r) => (
        <div className="min-w-0">
          <p className="font-semibold text-sm text-gray-900">{r.lessonTitle}</p>
          <p className="text-xs text-gray-500">{r.courseTitle}</p>
        </div>
      ),
    },
    {
      title: 'Người gửi',
      key: 'submitter',
      width: 220,
      render: (_, r) => (
        <div className="text-sm">
          <p className="text-gray-800">{r.submittedByName ?? 'Tài khoản đã xóa'}</p>
          {r.note && <p className="text-xs text-gray-500 line-clamp-2" title={r.note}>“{r.note}”</p>}
        </div>
      ),
    },
    {
      title: 'Gửi lúc',
      key: 'createdAt',
      width: 130,
      render: (_, r) => <span className="text-xs text-gray-500">{formatRelativeTime(r.createdAt)}</span>,
    },
    {
      title: 'Trạng thái',
      key: 'status',
      width: 200,
      render: (_, r) => (
        <div className="space-y-1">
          <Tag className="rounded-md" color={STATUS_TAG[r.status].color}>{STATUS_TAG[r.status].text}</Tag>
          {r.majorChange && <Tag className="rounded-md" color="magenta">Thay đổi lớn</Tag>}
          {r.status !== 'PENDING' && (r.reviewedByName || r.reviewNote) && (
            <p className="text-xs text-gray-500 line-clamp-2" title={r.reviewNote ?? undefined}>
              {r.reviewedByName}
              {r.reviewNote && <> — {r.reviewNote}</>}
            </p>
          )}
        </div>
      ),
    },
    {
      title: 'Hành động',
      key: 'actions',
      width: 260,
      render: (_, r) => (
        <div className="flex flex-wrap items-center gap-1.5">
          {/* Bản giáo viên đã gửi (ADMIN không đọc nháp đang soạn của giáo viên) */}
          <AntdButton size="small" icon={<Eye className="w-3.5 h-3.5" />} onClick={() => setTarget({ request: r, action: 'content' })}>
            Xem bản gửi
          </AntdButton>
          <AntdButton
            size="small"
            icon={<ExternalLink className="w-3.5 h-3.5" />}
            onClick={() => navigate(`/teacher/courses/${r.courseId}?view=review`)}
          >
            Mở khóa
          </AntdButton>
          {r.status === 'PENDING' && (
            <>
              <AntdButton
                size="small"
                type="primary"
                icon={<CheckCircle2 className="w-3.5 h-3.5" />}
                onClick={() => setTarget({ request: r, action: 'approve' })}
              >
                Duyệt
              </AntdButton>
              <AntdButton
                size="small"
                danger
                icon={<XCircle className="w-3.5 h-3.5" />}
                onClick={() => setTarget({ request: r, action: 'reject' })}
              >
                Từ chối
              </AntdButton>
            </>
          )}
        </div>
      ),
    },
  ]

  return (
    <div className="space-y-4">
      <FilterBar>
        <SearchInput
          placeholder="Tìm theo tên bài, khóa học hoặc người gửi"
          value={keywordInput}
          onChange={setKeywordInput}
          onSearch={(v) => {
            setKeyword(v)
            setPage(1)
          }}
          className="md:flex-1 md:min-w-60"
        />
        {/* Luôn lọc theo 1 trạng thái (BE mặc định PENDING) → không cho xóa chọn */}
        <Select<ReviewRequestStatus>
          value={status}
          onChange={(v) => {
            setStatus(v)
            setPage(1)
          }}
          options={STATUS_OPTIONS}
          className="md:w-44"
          aria-label="Lọc theo trạng thái"
        />
      </FilterBar>
      {isError ? (
        <EmptyState card title="Không tải được hàng chờ duyệt" description="Vui lòng thử lại sau." />
      ) : !isLoading && items.length === 0 ? (
        <EmptyState
          card
          title={
            keyword
              ? 'Không có yêu cầu phù hợp'
              : status === 'PENDING'
                ? 'Không có bài học nào chờ duyệt'
                : 'Chưa có yêu cầu nào'
          }
          description={
            keyword
              ? 'Thử đổi từ khóa tìm kiếm.'
              : status === 'PENDING'
                ? 'Bạn sẽ nhận thông báo khi giáo viên gửi bài lên duyệt.'
                : undefined
          }
        />
      ) : (
        <Table<LessonReviewRequest>
          rowKey="id"
          loading={isLoading || isFetching}
          dataSource={items}
          columns={columns}
          scroll={{ x: 900 }}
          pagination={{
            current: page,
            pageSize: PAGE_SIZE,
            total: data?.totalElements ?? 0,
            showSizeChanger: false,
            showTotal: (total) => `${total} yêu cầu`,
            onChange: setPage,
          }}
        />
      )}

      <SubmittedLessonModal
        requestId={target?.request.id}
        title={target?.request.lessonTitle}
        open={target?.action === 'content'}
        onClose={close}
      />
      <ReviewNoteModal
        open={target?.action === 'approve'}
        title={`Duyệt "${target?.request.lessonTitle ?? ''}"?`}
        okText="Duyệt"
        placeholder="Ghi chú gửi giáo viên"
        checkboxLabel="Thay đổi lớn — báo học viên đã hoàn thành bài (khi khóa đang xuất bản)"
        loading={approve.isPending}
        onClose={close}
        onSubmit={(note, majorChange) =>
          target &&
          approve.mutate({ ...vars(target.request), note: note || undefined, majorChange }, { onSettled: close })
        }
      />
      <ReviewNoteModal
        open={target?.action === 'reject'}
        title={`Từ chối "${target?.request.lessonTitle ?? ''}"`}
        okText="Từ chối"
        required
        danger
        placeholder="Lý do từ chối để giáo viên sửa tiếp"
        loading={reject.isPending}
        onClose={close}
        onSubmit={(reason) => target && reject.mutate({ ...vars(target.request), reason }, { onSettled: close })}
      />
    </div>
  )
}
