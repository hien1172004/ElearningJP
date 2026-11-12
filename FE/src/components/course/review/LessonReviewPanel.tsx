import React, { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button as AntdButton, Checkbox, Pagination, Tooltip } from 'antd'
import { Send } from 'lucide-react'
import { EmptyState, LoadingState } from '@/components/common'
import { useLessonReviewHistory, useLessonReviewStates, useSubmitLessonReviews } from '@/hooks'
import type { CourseDetail, LessonReviewRequest, LessonReviewState, ReviewRequestStatus } from '@/types'
import { cn } from '@/utils/cn'
import { formatRelativeTime } from '@/utils/format'
import { LessonReviewActions, LessonReviewStatusTag } from './LessonReviewActions'
import { ReviewNoteModal } from './ReviewNoteModal'
import { SubmittedLessonModal } from './LessonDraftPanel'

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'
const HISTORY_PAGE_SIZE = 5

const STATUS_LABEL: Record<ReviewRequestStatus, { text: string; tone: string }> = {
  PENDING: { text: 'Đang chờ', tone: 'bg-amber-50 text-amber-700 border-amber-200' },
  APPROVED: { text: 'Đã duyệt', tone: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
  REJECTED: { text: 'Bị từ chối', tone: 'bg-rose-50 text-rose-700 border-rose-200' },
  WITHDRAWN: { text: 'Đã rút', tone: 'bg-gray-100 text-gray-600 border-gray-200' },
}

interface LessonReviewPanelProps {
  course: CourseDetail
  admin: boolean
}

/** Bài gửi duyệt được: đang soạn, nháp giáo viên có thay đổi và chưa cũ (V65). */
const submittable = (s: LessonReviewState) =>
  s.reviewStatus === 'EDITING' && s.hasUnpublishedChanges && !s.outdated

/**
 * Duyệt theo bài: trạng thái từng bài, giáo viên chọn nhiều bài để gửi duyệt (mỗi bài kèm draftRevision
 * đang thấy, BE xử lý độc lập), ADMIN duyệt / từ chối / lưu từng bài; kèm lịch sử gửi duyệt theo bài.
 */
export const LessonReviewPanel: React.FC<LessonReviewPanelProps> = ({ course, admin }) => {
  const { data: states = [], isLoading } = useLessonReviewStates(course.id)
  const submit = useSubmitLessonReviews(course.id)
  const [selected, setSelected] = useState<number[]>([])
  const [noteOpen, setNoteOpen] = useState(false)
  // ADMIN: bài mới giáo viên đã gửi duyệt chưa có trong danh sách bài → bấm dòng để xem bản đã gửi
  const [viewing, setViewing] = useState<LessonReviewState | null>(null)

  const coursePending = course.reviewStatus === 'PENDING_REVIEW'
  const candidates = useMemo(() => states.filter(submittable), [states])
  // Trạng thái vừa tải lại (bài đã gửi / hết thay đổi) → bỏ khỏi lựa chọn
  const validSelected = selected.filter((id) => candidates.some((c) => c.lessonId === id))
  const pendingCount = states.filter((s) => s.reviewStatus === 'PENDING_REVIEW').length
  const changedCount = candidates.length

  const toggle = (lessonId: number, checked: boolean) =>
    setSelected((prev) => (checked ? [...prev, lessonId] : prev.filter((id) => id !== lessonId)))

  const handleSubmit = (note: string) => {
    const chosen = candidates.filter((c) => validSelected.includes(c.lessonId))
    submit.mutate(
      {
        // draftRevision = bản giáo viên đang thấy; bài bị sửa sau đó sẽ báo xung đột riêng bài đó
        lessons: chosen.map((c) => ({ lessonId: c.lessonId, draftRevision: c.draftRevision })),
        note: note || undefined,
        titles: Object.fromEntries(chosen.map((c) => [c.lessonId, c.title])),
      },
      {
        onSuccess: () => {
          setNoteOpen(false)
          setSelected([])
        },
      },
    )
  }

  return (
    <div className="space-y-4">
      <div className={`${CARD} p-5 space-y-4`}>
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h3 className="font-bold text-sm text-gray-800">Duyệt theo bài</h3>
            <p className="text-xs text-gray-500 mt-0.5">
              {admin
                ? 'Duyệt / từ chối từng bài giáo viên gửi, hoặc tự lưu bài lên bản học viên đang học (không qua duyệt). '
                : 'Chọn các bài đã soạn xong rồi gửi duyệt. '}
              Bài chờ duyệt tạm khóa chỉnh sửa với mọi người (kể cả quản trị viên).
            </p>
          </div>
          <p className="text-xs text-gray-500">
            <strong className="text-amber-700">{pendingCount}</strong> bài chờ duyệt ·{' '}
            <strong className="text-orange-700">{changedCount}</strong> bài có thay đổi
          </p>
        </div>

        {coursePending && (
          <p className="rounded-lg border border-amber-200 bg-amber-50/60 p-3 text-xs text-amber-800">
            Khóa học đang có yêu cầu duyệt <strong>cả khóa</strong> — xử lý yêu cầu đó (ở mục bên dưới) trước khi duyệt
            theo bài.
          </p>
        )}

        {isLoading ? (
          <LoadingState message="Đang tải trạng thái bài học..." />
        ) : states.length === 0 ? (
          <EmptyState title="Khóa học chưa có bài học nào" />
        ) : (
          <>
            {!admin && (
              <div className="flex flex-wrap items-center justify-between gap-2">
                <Checkbox
                  checked={validSelected.length > 0 && validSelected.length === candidates.length}
                  indeterminate={validSelected.length > 0 && validSelected.length < candidates.length}
                  disabled={candidates.length === 0 || coursePending}
                  onChange={(e) => setSelected(e.target.checked ? candidates.map((c) => c.lessonId) : [])}
                >
                  Chọn mọi bài có thay đổi
                </Checkbox>
                <Tooltip title={validSelected.length === 0 ? 'Chọn ít nhất 1 bài có thay đổi' : undefined}>
                  <AntdButton
                    type="primary"
                    icon={<Send className="w-4 h-4" />}
                    disabled={validSelected.length === 0 || coursePending}
                    loading={submit.isPending}
                    onClick={() => setNoteOpen(true)}
                    className="!rounded-md !font-semibold"
                  >
                    Gửi duyệt {validSelected.length > 0 ? `${validSelected.length} bài` : ''}
                  </AntdButton>
                </Tooltip>
              </div>
            )}
            <ul className="divide-y divide-gray-100 rounded-lg border border-gray-100">
              {states.map((s, index) => (
                // Cả dòng bấm được để mở bài (link phủ dòng qua ::after); checkbox / nút thao tác nằm trên (relative z-10)
                <li
                  key={s.lessonId}
                  className="group relative flex flex-wrap items-center gap-3 px-3 py-2.5 transition-colors first:rounded-t-lg last:rounded-b-lg hover:bg-indigo-50/40"
                >
                  {!admin && (
                    <Checkbox
                      className="relative z-10"
                      aria-label={`Chọn ${s.title}`}
                      checked={validSelected.includes(s.lessonId)}
                      disabled={!submittable(s) || coursePending}
                      onChange={(e) => toggle(s.lessonId, e.target.checked)}
                    />
                  )}
                  <div className="min-w-0 flex-1">
                    {admin && !s.inPublishedVersion && s.pendingRequestId != null ? (
                      // Bài mới chờ duyệt: chỉ vào danh sách bài sau khi duyệt → mở bản giáo viên đã gửi
                      <button
                        type="button"
                        onClick={() => setViewing(s)}
                        className="block w-full truncate text-left text-sm font-semibold text-gray-800 group-hover:text-indigo-600 after:absolute after:inset-0 after:content-['']"
                      >
                        Bài mới: {s.title}
                      </button>
                    ) : (
                      <Link
                        to={`/teacher/courses/${course.id}/lessons/${s.lessonId}`}
                        className="block truncate text-sm font-semibold text-gray-800 group-hover:text-indigo-600 after:absolute after:inset-0 after:content-['']"
                      >
                        Bài {index + 1}: {s.title}
                      </Link>
                    )}
                    <p className="text-xs text-gray-500">
                      {s.reviewStatus === 'PENDING_REVIEW' && s.pendingSubmittedByName
                        ? `Gửi bởi ${s.pendingSubmittedByName} ${formatRelativeTime(s.pendingSubmittedAt)}`
                        : s.draftUpdatedByName
                          ? `Sửa gần nhất: ${s.draftUpdatedByName} ${formatRelativeTime(s.draftUpdatedAt)}`
                          : ' '}
                    </p>
                  </div>
                  <LessonReviewStatusTag state={s} />
                  <div className="relative z-10">
                    <LessonReviewActions course={course} state={s} admin={admin} />
                  </div>
                </li>
              ))}
            </ul>
          </>
        )}
      </div>

      <div className={`${CARD} p-5`}>
        <h4 className="font-bold text-sm text-gray-800 mb-3">Lịch sử gửi duyệt theo bài</h4>
        <LessonReviewHistory courseId={course.id} />
      </div>

      <SubmittedLessonModal
        requestId={viewing?.pendingRequestId}
        title={viewing?.title}
        open={viewing != null}
        onClose={() => setViewing(null)}
      />
      <ReviewNoteModal
        open={noteOpen}
        title={`Gửi ${validSelected.length} bài lên duyệt`}
        okText="Gửi duyệt"
        placeholder="Ghi chú cho quản trị viên, vd: đã soạn xong bài 1–3"
        loading={submit.isPending}
        onClose={() => setNoteOpen(false)}
        onSubmit={handleSubmit}
      />
    </div>
  )
}

/** Lịch sử gửi duyệt theo bài (mới nhất trước), phân trang ở BE. */
const LessonReviewHistory: React.FC<{ courseId: number }> = ({ courseId }) => {
  const [page, setPage] = useState(1)
  const { data, isLoading, isFetching } = useLessonReviewHistory(courseId, page, HISTORY_PAGE_SIZE)
  const items = data?.items ?? []
  const total = data?.totalElements ?? 0
  const lastPage = Math.max(1, data?.totalPages ?? 1)
  // Lịch sử co lại (vd: bài bị xóa) khi đang ở trang cuối → lùi về trang cuối còn dữ liệu
  if (data && page > lastPage) setPage(lastPage)

  if (isLoading) return <LoadingState message="Đang tải lịch sử..." />
  if (total === 0) return <EmptyState title="Chưa có lượt gửi duyệt bài nào" />

  return (
    <div className="space-y-3">
      <ul className={`space-y-2 transition-opacity ${isFetching ? 'opacity-60' : ''}`}>
        {items.map((r) => (
          <HistoryItem key={r.id} request={r} />
        ))}
      </ul>
      {total > HISTORY_PAGE_SIZE && (
        <div className="flex justify-end">
          <Pagination
            size="small"
            current={Math.min(page, lastPage)}
            pageSize={HISTORY_PAGE_SIZE}
            total={total}
            onChange={setPage}
            showSizeChanger={false}
            showTotal={(t) => `${t} lượt gửi`}
          />
        </div>
      )}
    </div>
  )
}

const HistoryItem: React.FC<{ request: LessonReviewRequest }> = ({ request: r }) => {
  const label = STATUS_LABEL[r.status]
  return (
    <li className="rounded-lg border border-gray-100 p-3 space-y-1">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <span className="min-w-0 truncate text-sm font-semibold text-gray-800">{r.lessonTitle}</span>
        <span className="flex items-center gap-1.5">
          {r.majorChange && (
            <span className="px-2 py-0.5 rounded-full text-[11px] font-semibold border bg-pink-50 text-pink-700 border-pink-200">
              Thay đổi lớn
            </span>
          )}
          <span className={cn('px-2 py-0.5 rounded-full text-[11px] font-semibold border', label.tone)}>{label.text}</span>
        </span>
      </div>
      <p className="text-xs text-gray-600">
        Gửi bởi <strong>{r.submittedByName ?? 'Tài khoản đã xóa'}</strong> {formatRelativeTime(r.createdAt)}
        {r.note && <> — “{r.note}”</>}
      </p>
      {r.status !== 'PENDING' && (
        <p className="text-xs text-gray-600">
          {r.status === 'WITHDRAWN' ? 'Rút bởi' : 'Xử lý bởi'} <strong>{r.reviewedByName ?? '—'}</strong>{' '}
          {formatRelativeTime(r.reviewedAt)}
          {r.reviewNote && <> — {r.status === 'REJECTED' ? 'Lý do: ' : ''}“{r.reviewNote}”</>}
        </p>
      )}
    </li>
  )
}
