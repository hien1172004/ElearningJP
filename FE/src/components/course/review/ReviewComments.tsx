import React, { useState } from 'react'
import { Link } from 'react-router-dom'
import { Button as AntdButton, Checkbox, Input, Select } from 'antd'
import { CheckCircle2, MessageSquareText, Trash2 } from 'lucide-react'
import { LoadingState } from '@/components/common'
import { useCourseReviewCommentMutations, useCourseReviewComments, useCourseReviewHistory } from '@/hooks'
import type { CourseDetail, CourseReviewComment, LessonSection } from '@/types'
import { cn } from '@/utils/cn'

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'
const MAX_LENGTH = 2000
const formatTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : '')

/** Lượt duyệt đang chờ (nếu có) — ADMIN chỉ ghi nhận xét khi đang chờ. */
const usePendingRequestId = (courseId: number) => {
  const { data: history = [] } = useCourseReviewHistory(courseId)
  return history.find((r) => r.status === 'PENDING')?.id
}

// ============================================================================
// Mục "Nhận xét duyệt" trong tab Duyệt & xuất bản (cả khóa)
// ============================================================================

/** Nhận xét của lượt gửi duyệt gần nhất, nhóm theo bài. ADMIN thêm khi đang chờ; giáo viên đánh dấu đã sửa. */
export const CourseReviewCommentsPanel: React.FC<{ course: CourseDetail; admin: boolean }> = ({ course, admin }) => {
  const courseId = course.id
  const { data: comments = [], isLoading } = useCourseReviewComments(courseId)
  const pendingRequestId = usePendingRequestId(courseId)
  const lessonTitle = new Map((course.lessons ?? []).map((l) => [l.id, l.title]))

  const general = comments.filter((c) => c.lessonId == null)
  const byLesson = new Map<number, CourseReviewComment[]>()
  for (const c of comments) {
    if (c.lessonId == null) continue
    byLesson.set(c.lessonId, [...(byLesson.get(c.lessonId) ?? []), c])
  }
  const open = comments.filter((c) => !c.resolved).length

  return (
    <div className={`${CARD} p-5 space-y-4`}>
      <div>
        <h4 className="font-bold text-sm text-gray-800 flex items-center gap-1.5">
          <MessageSquareText className="w-4 h-4 text-indigo-600" /> Nhận xét duyệt
        </h4>
        <p className="text-xs text-gray-500 mt-0.5">
          {admin
            ? 'Ghi nhận xét cho cả khóa hoặc từng bài khi khóa đang chờ duyệt (mở bài để nhận xét từng mục).'
            : 'Nhận xét của quản trị viên ở lượt duyệt gần nhất. Sửa xong thì đánh dấu đã sửa rồi gửi duyệt lại.'}
          {comments.length > 0 && ` Còn ${open}/${comments.length} nhận xét chưa sửa.`}
        </p>
      </div>

      {admin && pendingRequestId && (
        <CommentForm
          courseId={courseId}
          requestId={pendingRequestId}
          targets={(course.lessons ?? []).map((l) => ({ value: l.id, label: l.title }))}
          targetPlaceholder="Cả khóa (hoặc chọn bài)"
          targetField="lessonId"
        />
      )}

      {isLoading ? (
        <LoadingState message="Đang tải nhận xét..." />
      ) : comments.length === 0 ? (
        <p className="text-sm text-gray-500">Chưa có nhận xét nào.</p>
      ) : (
        <div className="space-y-4">
          {general.length > 0 && (
            <CommentGroup title="Cả khóa" comments={general} courseId={courseId} admin={admin} />
          )}
          {[...byLesson.entries()].map(([lessonId, list]) => (
            <CommentGroup
              key={lessonId}
              title={
                <Link to={`/teacher/courses/${courseId}/lessons/${lessonId}`} className="hover:text-indigo-600">
                  {lessonTitle.get(lessonId) ?? list[0].lessonTitle ?? `Bài #${lessonId}`}
                </Link>
              }
              comments={list}
              courseId={courseId}
              admin={admin}
            />
          ))}
        </div>
      )}
    </div>
  )
}

// ============================================================================
// Khung nhận xét trong trang soạn bài (1 bài)
// ============================================================================

/** Nhận xét của bài đang mở. ADMIN thêm (theo mục) khi đang chờ duyệt; giáo viên đánh dấu đã sửa. */
export const LessonReviewComments: React.FC<{
  courseId: number
  lessonId: number
  sections: LessonSection[]
  admin: boolean
}> = ({ courseId, lessonId, sections, admin }) => {
  const { data: comments = [] } = useCourseReviewComments(courseId)
  const pendingRequestId = usePendingRequestId(courseId)
  const lessonComments = comments.filter((c) => c.lessonId === lessonId)
  const canAdd = admin && !!pendingRequestId

  // Không có gì để xem / làm → không chiếm chỗ trên trang soạn bài
  if (!canAdd && lessonComments.length === 0) return null

  return (
    <div className={`${CARD} p-4 space-y-3`}>
      <h4 className="font-bold text-sm text-gray-800 flex items-center gap-1.5">
        <MessageSquareText className="w-4 h-4 text-indigo-600" /> Nhận xét duyệt của bài này
      </h4>
      {canAdd && pendingRequestId && (
        <CommentForm
          courseId={courseId}
          requestId={pendingRequestId}
          lessonId={lessonId}
          targets={sections.map((s) => ({ value: s.id, label: s.title || `Mục #${s.id}` }))}
          targetPlaceholder="Cả bài (hoặc chọn mục)"
          targetField="sectionId"
        />
      )}
      {lessonComments.length > 0 && (
        <ul className="space-y-2">
          {lessonComments.map((c) => (
            <CommentItem key={c.id} comment={c} courseId={courseId} admin={admin} showSection />
          ))}
        </ul>
      )}
    </div>
  )
}

// ============================================================================
// Phần dùng chung
// ============================================================================

const CommentForm: React.FC<{
  courseId: number
  requestId: number
  lessonId?: number
  targets: { value: number; label: string }[]
  targetPlaceholder: string
  targetField: 'lessonId' | 'sectionId'
}> = ({ courseId, requestId, lessonId, targets, targetPlaceholder, targetField }) => {
  const { add } = useCourseReviewCommentMutations(courseId)
  const [target, setTarget] = useState<number | undefined>()
  const [content, setContent] = useState('')
  const trimmed = content.trim()

  const submit = () => {
    if (!trimmed) return
    const payload =
      targetField === 'lessonId'
        ? { lessonId: target, content: trimmed }
        : { lessonId, sectionId: target, content: trimmed }
    add.mutate({ requestId, payload }, { onSuccess: () => setContent('') })
  }

  return (
    <div className="space-y-2 rounded-lg border border-dashed border-indigo-200 bg-indigo-50/30 p-3">
      <Select
        allowClear
        size="small"
        className="w-full sm:w-80"
        placeholder={targetPlaceholder}
        value={target}
        onChange={setTarget}
        options={targets}
        showSearch
        optionFilterProp="label"
      />
      <Input.TextArea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        maxLength={MAX_LENGTH}
        showCount
        autoSize={{ minRows: 2, maxRows: 6 }}
        placeholder="Vd: Câu 3 phần ngữ pháp thiếu ví dụ; nghĩa của 日 chưa đúng..."
      />
      <div className="flex justify-end">
        <AntdButton size="small" type="primary" disabled={!trimmed} loading={add.isPending} onClick={submit}>
          Thêm nhận xét
        </AntdButton>
      </div>
    </div>
  )
}

const CommentGroup: React.FC<{
  title: React.ReactNode
  comments: CourseReviewComment[]
  courseId: number
  admin: boolean
}> = ({ title, comments, courseId, admin }) => (
  <div className="space-y-2">
    <p className="text-xs font-semibold text-gray-700">{title}</p>
    <ul className="space-y-2">
      {comments.map((c) => (
        <CommentItem key={c.id} comment={c} courseId={courseId} admin={admin} showSection />
      ))}
    </ul>
  </div>
)

const CommentItem: React.FC<{
  comment: CourseReviewComment
  courseId: number
  admin: boolean
  showSection?: boolean
}> = ({ comment: c, courseId, admin, showSection }) => {
  const { remove, resolve } = useCourseReviewCommentMutations(courseId)
  const pending = c.reviewStatus === 'PENDING'
  return (
    <li
      className={cn(
        'rounded-lg border p-3 space-y-1',
        c.resolved ? 'border-emerald-100 bg-emerald-50/40' : 'border-gray-100',
      )}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="min-w-0 space-y-0.5">
          {showSection && c.sectionTitle && <p className="text-[11px] font-semibold text-indigo-600">{c.sectionTitle}</p>}
          {/* Văn bản thuần — không render HTML */}
          <p className={cn('text-sm whitespace-pre-wrap break-words', c.resolved ? 'text-gray-500' : 'text-gray-800')}>
            {c.content}
          </p>
        </div>
        <div className="flex items-center gap-1 shrink-0">
          {!admin && !pending && (
            <Checkbox
              checked={c.resolved}
              disabled={resolve.isPending}
              onChange={(e) => resolve.mutate({ commentId: c.id, resolved: e.target.checked })}
            >
              <span className="text-xs">Đã sửa</span>
            </Checkbox>
          )}
          {admin && pending && (
            <AntdButton
              size="small"
              type="text"
              danger
              icon={<Trash2 className="w-3.5 h-3.5" />}
              loading={remove.isPending}
              onClick={() => remove.mutate(c.id)}
              title="Xóa nhận xét"
            />
          )}
        </div>
      </div>
      <p className="text-[11px] text-gray-400">
        {c.authorName ?? 'Quản trị viên'} · {formatTime(c.createdAt)}
        {c.resolved && (
          <span className="ml-1 inline-flex items-center gap-0.5 text-emerald-600">
            <CheckCircle2 className="w-3 h-3" /> đã sửa{c.resolvedByName ? ` bởi ${c.resolvedByName}` : ''}
          </span>
        )}
      </p>
    </li>
  )
}
