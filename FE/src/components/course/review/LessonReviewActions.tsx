import React, { useState } from 'react'
import { Button as AntdButton, Checkbox, Tag, Tooltip } from 'antd'
import { CheckCircle2, Eye, Send, Undo2, Upload, XCircle } from 'lucide-react'
import { confirmDialog } from '@/components/common/ConfirmModal'
import {
  useApproveLessonReview,
  useCourseEditLock,
  useRejectLessonReview,
  useSaveLessonToLive,
  useSubmitLessonReviews,
  useWithdrawLessonReview,
} from '@/hooks'
import type { CourseDetail, LessonReviewState } from '@/types'
import { formatRelativeTime } from '@/utils/format'
import { ReviewNoteModal } from './ReviewNoteModal'
import { SubmittedLessonModal } from './LessonDraftPanel'

/** Nhãn trạng thái duyệt của 1 bài. */
export const LessonReviewStatusTag: React.FC<{ state?: LessonReviewState | null }> = ({ state }) => {
  if (!state) return null
  // Nháp phía mình dựa trên bản học viên cũ (phía kia vừa đưa bài lên) — V65
  const outdated = state.outdated && (
    <Tooltip title="Bản học viên đang học đã thay đổi sau khi bản nháp này được tạo — cần cập nhật bản mới">
      <Tag color="volcano" className="rounded-md">Nháp đã cũ</Tag>
    </Tooltip>
  )
  return (
    <>
      {outdated}
      <ReviewStateTag state={state} />
    </>
  )
}

const ReviewStateTag: React.FC<{ state: LessonReviewState }> = ({ state }) => {
  if (state.reviewStatus === 'PENDING_REVIEW') {
    return (
      <Tooltip
        title={
          state.pendingSubmittedByName
            ? `Gửi bởi ${state.pendingSubmittedByName} ${formatRelativeTime(state.pendingSubmittedAt)}`
            : undefined
        }
      >
        <Tag color="gold" className="rounded-md">Chờ duyệt</Tag>
      </Tooltip>
    )
  }
  if (!state.inPublishedVersion) return <Tag color="blue" className="rounded-md">Bài mới · chưa duyệt</Tag>
  if (state.hasUnpublishedChanges) return <Tag color="orange" className="rounded-md">Có thay đổi chưa duyệt</Tag>
  return <Tag color="green" className="rounded-md">Bản phát hành</Tag>
}

interface LessonReviewActionsProps {
  course: Pick<CourseDetail, 'id' | 'reviewStatus'>
  state?: LessonReviewState | null
  admin: boolean
  size?: 'small' | 'middle'
}

/**
 * Thao tác duyệt của 1 bài:
 * - giáo viên: Gửi duyệt (kèm draftRevision đang thấy) / Rút yêu cầu (giáo viên được giao bất kỳ);
 * - ADMIN: Duyệt / Từ chối khi bài chờ duyệt; Lưu bài (nháp ADMIN) lên bản học viên khi nháp ADMIN có thay đổi —
 *   cần giữ khóa sửa khóa học.
 * Nháp đã cũ (V65) → cập nhật bản mới trước. Cả khóa đang chờ duyệt theo khóa (luồng cũ) → không gửi / lưu từng bài
 * (BE cũng chặn).
 */
export const LessonReviewActions: React.FC<LessonReviewActionsProps> = ({ course, state, admin, size = 'small' }) => {
  const courseId = course.id
  const submit = useSubmitLessonReviews(courseId)
  const withdraw = useWithdrawLessonReview(courseId)
  const approve = useApproveLessonReview()
  const reject = useRejectLessonReview()
  const save = useSaveLessonToLive(courseId)
  const [modal, setModal] = useState<'submit' | 'approve' | 'reject' | 'content' | null>(null)
  // Lưu bài là thao tác ghi của ADMIN → cần giữ khóa sửa khóa học phía ADMIN (gửi duyệt / rút thì không);
  // chưa giữ khóa thì "Lưu bài" tự lấy khóa → lưu → nhả, chỉ chặn khi admin khác đang sửa
  const { mine: editing, heldByOther, lock } = useCourseEditLock(admin ? courseId : 0)

  if (!state) return null
  const lessonId = state.lessonId
  const pending = state.reviewStatus === 'PENDING_REVIEW'
  const coursePending = course.reviewStatus === 'PENDING_REVIEW'
  const blockedReason = coursePending
    ? 'Khóa học đang chờ duyệt theo cả khóa — xử lý yêu cầu đó trước'
    : state.outdated
      ? 'Bản học viên đang học đã thay đổi — cập nhật bản mới trước'
      : !state.hasUnpublishedChanges
        ? 'Bài không có thay đổi nào so với bản học viên đang học'
        : null
  const saveBlockedReason = blockedReason
    ?? (heldByOther ? `${lock?.userName ?? 'Quản trị viên khác'} đang sửa khóa học — chờ họ bấm "Xong" hoặc tiếp quản trước` : null)
  const decision = state.pendingRequestId
    ? { requestId: state.pendingRequestId, courseId, lessonId }
    : null

  const handleWithdraw = async () => {
    const ok = await confirmDialog({
      title: `Rút yêu cầu duyệt "${state.title}"?`,
      content: 'Bài quay lại đang soạn, giáo viên được giao có thể sửa tiếp.',
      okText: 'Rút yêu cầu',
      cancelText: 'Hủy',
    })
    if (ok) withdraw.mutate(lessonId)
  }

  const handleSave = async () => {
    // Hộp xác nhận dựng 1 lần → ô tick không cần state, chỉ cần ghi lại lựa chọn
    let majorChange = false
    const ok = await confirmDialog({
      title: `Lưu "${state.title}" lên bản học viên đang học?`,
      content: (
        <div className="space-y-2">
          <p>
            {state.draftUpdatedByName
              ? `Nháp của quản trị viên được sửa gần nhất bởi ${state.draftUpdatedByName} ${formatRelativeTime(state.draftUpdatedAt)}. `
              : ''}
            Nháp của quản trị viên sẽ lên bản học viên đang học, không qua duyệt. Phần giáo viên đang soạn không bị đưa
            lên; nếu giáo viên đã sửa bài này, nháp của họ sẽ cần cập nhật bản mới.
          </p>
          <Checkbox onChange={(e) => (majorChange = e.target.checked)}>Thay đổi lớn — báo học viên đã hoàn thành bài (khi khóa đang xuất bản)</Checkbox>
        </div>
      ),
      okText: 'Lưu bài',
      cancelText: 'Hủy',
    })
    if (ok) save.mutate({ lessonId, majorChange, acquireLock: !editing })
  }

  return (
    <div className="flex flex-wrap items-center gap-1.5">
      {!admin && !pending && (
        <Tooltip title={blockedReason}>
          <AntdButton
            size={size}
            type="primary"
            icon={<Send className="w-3.5 h-3.5" />}
            disabled={!!blockedReason}
            loading={submit.isPending}
            onClick={() => setModal('submit')}
          >
            Gửi duyệt
          </AntdButton>
        </Tooltip>
      )}
      {!admin && pending && (
        <AntdButton size={size} icon={<Undo2 className="w-3.5 h-3.5" />} loading={withdraw.isPending} onClick={handleWithdraw}>
          Rút yêu cầu
        </AntdButton>
      )}
      {admin && pending && decision && (
        <>
          {/* ADMIN không đọc nháp đang soạn của giáo viên — xem đúng bản đã gửi */}
          <AntdButton size={size} icon={<Eye className="w-3.5 h-3.5" />} onClick={() => setModal('content')}>
            Xem bản gửi
          </AntdButton>
          <AntdButton
            size={size}
            type="primary"
            icon={<CheckCircle2 className="w-3.5 h-3.5" />}
            loading={approve.isPending}
            onClick={() => setModal('approve')}
          >
            Duyệt
          </AntdButton>
          <AntdButton
            size={size}
            danger
            icon={<XCircle className="w-3.5 h-3.5" />}
            loading={reject.isPending}
            onClick={() => setModal('reject')}
          >
            Từ chối
          </AntdButton>
        </>
      )}
      {admin && !pending && (
        <Tooltip title={saveBlockedReason}>
          <AntdButton
            size={size}
            icon={<Upload className="w-3.5 h-3.5" />}
            disabled={!!saveBlockedReason}
            loading={save.isPending}
            onClick={handleSave}
          >
            Lưu bài
          </AntdButton>
        </Tooltip>
      )}

      {admin && (
        <SubmittedLessonModal
          requestId={state.pendingRequestId}
          title={state.title}
          open={modal === 'content'}
          onClose={() => setModal(null)}
        />
      )}
      <ReviewNoteModal
        open={modal === 'submit'}
        title={`Gửi "${state.title}" lên duyệt`}
        okText="Gửi duyệt"
        placeholder="Ghi chú cho quản trị viên"
        loading={submit.isPending}
        onClose={() => setModal(null)}
        onSubmit={(note) =>
          submit.mutate(
            {
              lessons: [{ lessonId, draftRevision: state.draftRevision }],
              note: note || undefined,
              titles: { [lessonId]: state.title },
            },
            { onSuccess: () => setModal(null) },
          )
        }
      />
      <ReviewNoteModal
        open={modal === 'approve'}
        title={`Duyệt "${state.title}"`}
        okText="Duyệt"
        placeholder="Ghi chú gửi giáo viên"
        checkboxLabel="Thay đổi lớn — báo học viên đã hoàn thành bài (khi khóa đang xuất bản)"
        loading={approve.isPending}
        onClose={() => setModal(null)}
        onSubmit={(note, majorChange) =>
          decision &&
          approve.mutate({ ...decision, note: note || undefined, majorChange }, { onSettled: () => setModal(null) })
        }
      />
      <ReviewNoteModal
        open={modal === 'reject'}
        title={`Từ chối "${state.title}"`}
        okText="Từ chối"
        required
        danger
        placeholder="Lý do từ chối để giáo viên sửa tiếp, vd: thiếu ví dụ ở mục ngữ pháp"
        loading={reject.isPending}
        onClose={() => setModal(null)}
        onSubmit={(reason) => decision && reject.mutate({ ...decision, reason }, { onSettled: () => setModal(null) })}
      />
    </div>
  )
}
