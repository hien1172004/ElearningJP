import React, { useState } from 'react'
import { Button as AntdButton, Tooltip } from 'antd'
import { CheckCircle2, Lock, Send, Undo2, XCircle } from 'lucide-react'
import { EmptyState, LoadingState } from '@/components/common'
import { confirmDialog } from '@/components/common/ConfirmModal'
import {
  useApproveCourseReview,
  useCourseReviewHistory,
  useRejectCourseReview,
  useSubmitCourseReview,
  useWithdrawCourseReview,
} from '@/hooks'
import type { CourseDetail, CourseReviewRequest, ReviewRequestStatus } from '@/types'
import { cn } from '@/utils/cn'
import { CourseReviewBadge } from './CourseReviewBadge'
import { ReviewNoteModal } from './ReviewNoteModal'

interface CourseReviewPanelProps {
  course: CourseDetail
  /** ADMIN duyệt / từ chối; giáo viên được giao gửi / rút. */
  admin: boolean
  currentUserId?: number
}

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'

const STATUS_LABEL: Record<ReviewRequestStatus, { text: string; tone: string }> = {
  PENDING: { text: 'Đang chờ', tone: 'bg-amber-50 text-amber-700 border-amber-200' },
  APPROVED: { text: 'Đã duyệt', tone: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { text: 'Bị từ chối', tone: 'bg-rose-50 text-rose-700 border-rose-200' },
  WITHDRAWN: { text: 'Đã rút', tone: 'bg-gray-100 text-gray-600 border-gray-200' },
}

const formatTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : '')

/** Duyệt & xuất bản: trạng thái hiện tại, thao tác gửi / rút / duyệt / từ chối và lịch sử các lượt gửi. */
export const CourseReviewPanel: React.FC<CourseReviewPanelProps> = ({ course, admin, currentUserId }) => {
  const courseId = course.id
  const { data: history = [], isLoading } = useCourseReviewHistory(courseId)
  const submit = useSubmitCourseReview(courseId)
  const withdraw = useWithdrawCourseReview(courseId)
  const approve = useApproveCourseReview()
  const reject = useRejectCourseReview()
  const [modal, setModal] = useState<'submit' | 'approve' | 'reject' | null>(null)

  const pendingRequest = history.find((r) => r.status === 'PENDING')
  const isPending = course.reviewStatus === 'PENDING_REVIEW'
  // ADMIN chỉ duyệt: không gửi / rút; rút yêu cầu chỉ người gửi
  const canWithdraw = !admin && !!pendingRequest && pendingRequest.submittedById === currentUserId
  // Giáo viên luôn thấy nút Gửi duyệt; chưa gửi được thì làm mờ kèm lý do (BE cũng chặn các trường hợp này)
  const submitBlockedReason = isPending
    ? 'Khóa học đang chờ quản trị viên duyệt'
    : !course.hasUnpublishedChanges
      ? 'Chưa có thay đổi nào để gửi duyệt'
      : (course.lessons?.length ?? 0) === 0
        ? 'Khóa học cần có ít nhất 1 bài học'
        : null

  const handleWithdraw = async () => {
    const ok = await confirmDialog({
      title: 'Rút yêu cầu duyệt?',
      content: 'Khóa học quay lại trạng thái đang soạn, giáo viên được giao có thể sửa tiếp nội dung.',
      okText: 'Rút yêu cầu',
      cancelText: 'Hủy',
    })
    if (ok) withdraw.mutate()
  }

  return (
    <div className="space-y-4">
      <div className={`${CARD} p-5 space-y-4`}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="font-bold text-sm text-gray-800">Duyệt &amp; xuất bản</h3>
            <p className="text-xs text-gray-500 mt-0.5">
              Giáo viên soạn xong thì gửi duyệt. Chỉ quản trị viên quyết định xuất bản hoặc chuyển nháp.
            </p>
          </div>
          <CourseReviewBadge course={course} />
        </div>

        {course.publishedVersionNo ? (
          <p className="text-xs text-gray-500">
            Học viên đang học <strong>phiên bản {course.publishedVersionNo}</strong>
            {course.publishedVersionAt && <> (duyệt lúc {formatTime(course.publishedVersionAt)})</>}. Thay đổi trong
            bản đang soạn chỉ tới học viên sau khi được duyệt.
          </p>
        ) : (
          !course.published && (
            <p className="text-xs text-gray-500">Chưa có phiên bản nào được duyệt — học viên chưa thấy nội dung khóa.</p>
          )
        )}

        {isPending ? (
          <div className="rounded-lg border border-amber-200 bg-amber-50/60 p-3 text-sm text-amber-800 flex gap-2">
            <Lock className="w-4 h-4 mt-0.5 shrink-0" />
            <div className="space-y-1">
              <p className="font-semibold">Khóa học đang chờ quản trị viên duyệt</p>
              <p className="text-xs">
                Giáo viên tạm khóa chỉnh sửa nội dung để quản trị viên duyệt đúng bản đã gửi.
                {pendingRequest?.submittedByName && ` Người gửi: ${pendingRequest.submittedByName}.`}
              </p>
              {pendingRequest?.note && <p className="text-xs italic">“{pendingRequest.note}”</p>}
            </div>
          </div>
        ) : !course.hasUnpublishedChanges ? (
          <p className="text-sm text-gray-500">
            {course.published
              ? 'Học viên đang học đúng nội dung mới nhất. Sửa bài học sẽ tạo thay đổi cần gửi duyệt lại.'
              : 'Khóa học chưa có nội dung nào để gửi duyệt.'}
          </p>
        ) : (course.lessons?.length ?? 0) === 0 ? (
          <p className="text-sm text-gray-500">Khóa học cần có ít nhất 1 bài học trước khi gửi duyệt.</p>
        ) : (
          <p className="text-sm text-gray-600">
            Bản đang soạn có thay đổi chưa được duyệt
            {course.published ? '. Học viên vẫn học nội dung đã xuất bản cho tới khi được duyệt.' : '.'}
          </p>
        )}

        <div className="flex flex-wrap gap-2">
          {!admin && (
            <Tooltip title={submitBlockedReason}>
              <AntdButton
                type="primary"
                icon={<Send className="w-4 h-4" />}
                loading={submit.isPending}
                disabled={!!submitBlockedReason}
                onClick={() => setModal('submit')}
                className="!rounded-md !font-semibold"
              >
                Gửi duyệt
              </AntdButton>
            </Tooltip>
          )}
          {canWithdraw && (
            <AntdButton
              icon={<Undo2 className="w-4 h-4" />}
              loading={withdraw.isPending}
              onClick={handleWithdraw}
              className="!rounded-md"
            >
              Rút yêu cầu
            </AntdButton>
          )}
          {admin && pendingRequest && (
            <>
              <AntdButton
                type="primary"
                icon={<CheckCircle2 className="w-4 h-4" />}
                loading={approve.isPending}
                onClick={() => setModal('approve')}
                className="!rounded-md !font-semibold"
              >
                Duyệt & xuất bản
              </AntdButton>
              <AntdButton
                danger
                icon={<XCircle className="w-4 h-4" />}
                loading={reject.isPending}
                onClick={() => setModal('reject')}
                className="!rounded-md"
              >
                Từ chối
              </AntdButton>
            </>
          )}
        </div>
      </div>

      <div className={`${CARD} p-5`}>
        <h4 className="font-bold text-sm text-gray-800 mb-3">Lịch sử gửi duyệt</h4>
        {isLoading ? (
          <LoadingState message="Đang tải lịch sử duyệt..." />
        ) : history.length === 0 ? (
          <EmptyState title="Chưa có lượt gửi duyệt nào" />
        ) : (
          <ul className="space-y-3">
            {history.map((r) => (
              <ReviewHistoryItem key={r.id} request={r} />
            ))}
          </ul>
        )}
      </div>

      <ReviewNoteModal
        open={modal === 'submit'}
        title="Gửi khóa học lên duyệt"
        okText="Gửi duyệt"
        placeholder="Ghi chú cho quản trị viên (không bắt buộc), vd: đã soạn xong bài 1–5"
        loading={submit.isPending}
        onClose={() => setModal(null)}
        onSubmit={(note) => submit.mutate(note || undefined, { onSuccess: () => setModal(null) })}
      />
      <ReviewNoteModal
        open={modal === 'approve'}
        title="Duyệt và xuất bản khóa học"
        okText="Duyệt & xuất bản"
        placeholder="Ghi chú gửi giáo viên (không bắt buộc)"
        loading={approve.isPending}
        onClose={() => setModal(null)}
        onSubmit={(note) =>
          pendingRequest &&
          approve.mutate(
            { requestId: pendingRequest.id, courseId, note: note || undefined },
            { onSuccess: () => setModal(null) },
          )
        }
      />
      <ReviewNoteModal
        open={modal === 'reject'}
        title="Từ chối yêu cầu duyệt"
        okText="Từ chối"
        required
        danger
        placeholder="Lý do từ chối để giáo viên sửa tiếp, vd: bài 2 thiếu ví dụ"
        loading={reject.isPending}
        onClose={() => setModal(null)}
        onSubmit={(reason) =>
          pendingRequest &&
          reject.mutate({ requestId: pendingRequest.id, courseId, reason }, { onSuccess: () => setModal(null) })
        }
      />
    </div>
  )
}

const ReviewHistoryItem: React.FC<{ request: CourseReviewRequest }> = ({ request: r }) => {
  const label = STATUS_LABEL[r.status]
  return (
    <li className="rounded-lg border border-gray-100 p-3 space-y-1">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className={cn('px-2 py-0.5 rounded-full text-[11px] font-semibold border', label.tone)}>{label.text}</span>
        <span className="text-xs text-gray-400">{formatTime(r.createdAt)}</span>
      </div>
      <p className="text-xs text-gray-600">
        Gửi bởi <strong>{r.submittedByName ?? 'Tài khoản đã xóa'}</strong>
        {r.note && <> — “{r.note}”</>}
      </p>
      {r.status !== 'PENDING' && (
        <p className="text-xs text-gray-600">
          {r.status === 'WITHDRAWN' ? 'Rút bởi' : 'Xử lý bởi'} <strong>{r.reviewedByName ?? '—'}</strong>
          {r.reviewedAt && <> lúc {formatTime(r.reviewedAt)}</>}
          {r.reviewNote && <> — {r.status === 'REJECTED' ? 'Lý do: ' : ''}“{r.reviewNote}”</>}
        </p>
      )}
    </li>
  )
}
