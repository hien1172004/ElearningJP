import React from 'react'
import { Link } from 'react-router-dom'
import { Button as AntdButton, Tag } from 'antd'
import { GitCompare, RefreshCw } from 'lucide-react'
import { EmptyState, LoadingState } from '@/components/common'
import { useCourseChanges } from '@/hooks'
import type { ChangeStatus, ItemsChange, LessonChange } from '@/types'
import { cn } from '@/utils/cn'

const CARD = 'bg-surface rounded-xl border border-suna shadow-xs'

const STATUS: Record<ChangeStatus, { text: string; color: string }> = {
  ADDED: { text: 'Mới', color: 'green' },
  REMOVED: { text: 'Sẽ gỡ', color: 'red' },
  MODIFIED: { text: 'Có sửa', color: 'gold' },
  UNCHANGED: { text: 'Không đổi', color: 'default' },
}

const SECTION_LABEL = { VOCABULARY: 'Từ vựng', KANJI: 'Chữ Hán', GRAMMAR: 'Ngữ pháp' } as const

/** "+2 · −1 · sửa 3 · đổi thứ tự" */
const describe = (c?: ItemsChange | null) => {
  if (!c) return ''
  const parts: string[] = []
  if (c.added) parts.push(`+${c.added}`)
  if (c.removed) parts.push(`−${c.removed}`)
  if (c.modified) parts.push(`sửa ${c.modified}`)
  if (c.reordered) parts.push('đổi thứ tự')
  if (c.metaChanged) parts.push('đổi thông tin')
  return parts.join(' · ')
}

const formatTime = (iso?: string | null) => (iso ? new Date(iso).toLocaleString('vi-VN') : '')

/**
 * Thay đổi của bản đang soạn so với bản học viên đang học — ADMIN duyệt đúng phần thay đổi,
 * giáo viên kiểm tra trước khi gửi.
 */
export const CourseChangesView: React.FC<{
  courseId: number
  title?: string
  /** Bài có trang soạn để mở (đã có trong danh sách bài); bài mới chờ duyệt (V66) không có → không gắn link. */
  linkable?: (lessonId: number) => boolean
}> = ({ courseId, title = 'Thay đổi chờ duyệt', linkable }) => {
  const { data, isLoading, isError, refetch, isFetching } = useCourseChanges(courseId)
  const changedLessons = (data?.lessons ?? []).filter((l) => l.status !== 'UNCHANGED')
  const changedExams = (data?.exams ?? []).filter((x) => x.status !== 'UNCHANGED')

  return (
    <div className={`${CARD} p-5 space-y-4`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <h4 className="font-bold text-sm text-gray-800 flex items-center gap-1.5">
            <GitCompare className="w-4 h-4 text-indigo-600" /> {title}
          </h4>
          <p className="text-xs text-gray-500 mt-0.5">
            {data?.publishedVersionNo
              ? `So với phiên bản ${data.publishedVersionNo} học viên đang học (duyệt lúc ${formatTime(data.publishedAt)}).`
              : 'Khóa chưa có bản được duyệt — toàn bộ nội dung là mới.'}
          </p>
        </div>
        <AntdButton size="small" icon={<RefreshCw className="w-3.5 h-3.5" />} loading={isFetching} onClick={() => refetch()}>
          Tải lại
        </AntdButton>
      </div>

      {isLoading ? (
        <LoadingState message="Đang so sánh nội dung..." />
      ) : isError ? (
        <EmptyState title="Không tải được danh sách thay đổi" description="Vui lòng thử lại." />
      ) : !data?.hasChanges ? (
        <p className="text-sm text-gray-500">Bản đang soạn giống hệt bản học viên đang học.</p>
      ) : (
        <div className="space-y-3">
          {changedLessons.length > 0 && (
            <ul className="space-y-2">
              {changedLessons.map((l) => (
                <LessonChangeRow
                  key={l.lessonId}
                  courseId={courseId}
                  change={l}
                  linkable={linkable ? linkable(l.lessonId) : true}
                />
              ))}
            </ul>
          )}
          {changedExams.length > 0 && (
            <div className="space-y-1.5">
              <p className="text-xs font-semibold text-gray-600">Đề thi của khóa</p>
              <ul className="space-y-1.5">
                {changedExams.map((x) => (
                  <li key={x.examId} className="flex items-center gap-2 text-sm">
                    <Tag className="rounded-md !m-0" color={STATUS[x.status].color}>{STATUS[x.status].text}</Tag>
                    <Link to={`/admin/exams/${x.examId}/preview`} className="text-gray-800 hover:text-indigo-600 truncate">
                      {x.title}
                    </Link>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

const LessonChangeRow: React.FC<{ courseId: number; change: LessonChange; linkable: boolean }> = ({
  courseId,
  change: l,
  linkable,
}) => {
  const details: string[] = []
  if (l.infoChanged) details.push('đổi tên bài')
  if (l.moved) details.push('đổi vị trí')
  const quiz = describe(l.quiz)
  if (quiz) details.push(`quiz: ${quiz}`)
  if (l.speakingChanged) details.push('luyện nói thay đổi')
  const sections = l.sections.filter((s) => s.status !== 'UNCHANGED')

  const removed = l.status === 'REMOVED' || !linkable
  return (
    // Cả thẻ bấm được (link phủ thẻ qua ::after); bài sẽ gỡ không còn trang để mở
    <li
      className={cn(
        'relative rounded-lg border border-gray-100 p-3 space-y-1.5',
        !removed && 'group transition-colors hover:border-indigo-200 hover:bg-indigo-50/40',
      )}
    >
      <div className="flex items-center gap-2 min-w-0">
        <Tag className="rounded-md !m-0 shrink-0" color={STATUS[l.status].color}>{STATUS[l.status].text}</Tag>
        {removed ? (
          <span className={cn('text-sm font-semibold truncate', l.status === 'REMOVED' ? 'text-gray-500 line-through' : 'text-gray-900')}>
            {l.title}
          </span>
        ) : (
          <Link
            to={`/teacher/courses/${courseId}/lessons/${l.lessonId}`}
            className="text-sm font-semibold text-gray-900 group-hover:text-indigo-600 truncate after:absolute after:inset-0 after:rounded-lg after:content-['']"
          >
            {l.title}
          </Link>
        )}
      </div>
      {details.length > 0 && <p className="text-xs text-gray-500">{details.join(' · ')}</p>}
      {sections.length > 0 && (
        <ul className="text-xs text-gray-600 space-y-0.5 pl-1">
          {sections.map((s) => (
            <li key={s.sectionId}>
              <span className="font-medium">{SECTION_LABEL[s.sectionType]}</span>
              {s.title ? ` — ${s.title}` : ''}: {STATUS[s.status].text.toLowerCase()}
              {describe(s.items) && ` (${describe(s.items)})`}
            </li>
          ))}
        </ul>
      )}
    </li>
  )
}
