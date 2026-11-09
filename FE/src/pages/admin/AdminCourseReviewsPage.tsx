import React, { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button as AntdButton, Segmented, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { CheckCircle2, ExternalLink, XCircle } from 'lucide-react'
import { EmptyState, PageHeader } from '@/components/common'
import { ReviewNoteModal } from '@/components/course'
import { useApproveCourseReview, useCourseReviewQueue, useRejectCourseReview } from '@/hooks'
import { formatRelativeTime } from '@/utils/format'
import type { CourseReviewRequest, ReviewRequestStatus } from '@/types'

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

/** Hàng chờ duyệt khóa học (ADMIN): duyệt & xuất bản hoặc từ chối kèm lý do. */
export const AdminCourseReviewsPage: React.FC = () => {
  const navigate = useNavigate()
  const [status, setStatus] = useState<ReviewRequestStatus>('PENDING')
  const [page, setPage] = useState(1)
  const [target, setTarget] = useState<{ request: CourseReviewRequest; action: 'approve' | 'reject' } | null>(null)

  const { data, isLoading, isError } = useCourseReviewQueue({ status, page, size: PAGE_SIZE })
  const approve = useApproveCourseReview()
  const reject = useRejectCourseReview()
  const items = data?.items ?? []

  const columns: ColumnsType<CourseReviewRequest> = [
    {
      title: 'Khóa học',
      key: 'course',
      render: (_, r) => <span className="font-semibold text-sm text-gray-900">{r.courseTitle}</span>,
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
          {r.status !== 'PENDING' && r.reviewNote && (
            <p className="text-xs text-gray-500 line-clamp-2" title={r.reviewNote}>{r.reviewNote}</p>
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
          <AntdButton
            size="small"
            icon={<ExternalLink className="w-3.5 h-3.5" />}
            onClick={() => navigate(`/teacher/courses/${r.courseId}?view=review`)}
          >
            Xem khóa
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

  const close = () => setTarget(null)

  return (
    <div className="space-y-6 pb-12">
      <PageHeader
        className="max-w-7xl mx-auto px-3 sm:px-4"
        title="Hàng chờ duyệt"
        subtitle="Giáo viên gửi khóa học lên duyệt; chỉ quản trị viên quyết định xuất bản hoặc chuyển nháp."
      />
      <div className="max-w-7xl mx-auto px-3 sm:px-4 space-y-4">
        <Segmented<ReviewRequestStatus>
          options={STATUS_OPTIONS}
          value={status}
          onChange={(v) => {
            setStatus(v)
            setPage(1)
          }}
        />
        {isError ? (
          <EmptyState card title="Không tải được hàng chờ duyệt" description="Vui lòng thử lại sau." />
        ) : !isLoading && items.length === 0 ? (
          <EmptyState
            card
            title={status === 'PENDING' ? 'Không có khóa học nào chờ duyệt' : 'Chưa có yêu cầu nào'}
            description={status === 'PENDING' ? 'Bạn sẽ nhận thông báo khi giáo viên gửi duyệt.' : undefined}
          />
        ) : (
          <Table<CourseReviewRequest>
            rowKey="id"
            loading={isLoading}
            dataSource={items}
            columns={columns}
            pagination={{
              current: page,
              pageSize: PAGE_SIZE,
              total: data?.totalElements ?? 0,
              showSizeChanger: false,
              onChange: setPage,
            }}
          />
        )}
      </div>

      <ReviewNoteModal
        open={target?.action === 'approve'}
        title={`Duyệt và xuất bản "${target?.request.courseTitle ?? ''}"?`}
        okText="Duyệt & xuất bản"
        placeholder="Ghi chú gửi giáo viên (không bắt buộc)"
        loading={approve.isPending}
        onClose={close}
        onSubmit={(note) =>
          target &&
          approve.mutate(
            { requestId: target.request.id, courseId: target.request.courseId, note: note || undefined },
            { onSuccess: close },
          )
        }
      />
      <ReviewNoteModal
        open={target?.action === 'reject'}
        title={`Từ chối "${target?.request.courseTitle ?? ''}"`}
        okText="Từ chối"
        required
        danger
        placeholder="Lý do từ chối để giáo viên sửa tiếp"
        loading={reject.isPending}
        onClose={close}
        onSubmit={(reason) =>
          target &&
          reject.mutate(
            { requestId: target.request.id, courseId: target.request.courseId, reason },
            { onSuccess: close },
          )
        }
      />
    </div>
  )
}
