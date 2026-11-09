import React, { useEffect, useMemo, useState } from 'react'
import { Button as AntdButton, Input, Pagination } from 'antd'
import { ChevronDown, ChevronUp, Pencil, Plus, Search, Trash2 } from 'lucide-react'
import { cn } from '@/utils/cn'
import { useCourseEditLockHint, useCourseEditLocked } from '@/components/course/edit-lock'
import type { LessonOutline } from '@/types'

const PAGE_SIZE = 10

interface CourseLessonListProps {
  lessons: LessonOutline[]
  onOpen: (lesson: LessonOutline) => void
  onAdd: () => void
  onEdit: (lesson: LessonOutline) => void
  onDelete: (lesson: LessonOutline) => void
  onMove: (lessonId: number, direction: 'up' | 'down') => void
  reorderPending?: boolean
}

/**
 * Danh sách bài học của khóa (trong Tổng quan): tìm theo tên + phân trang phía client
 * (khóa học đã trả đủ outline bài học). Số thứ tự & nút lên/xuống tính theo toàn khóa.
 */
export const CourseLessonList: React.FC<CourseLessonListProps> = ({
  lessons,
  onOpen,
  onAdd,
  onEdit,
  onDelete,
  onMove,
  reorderPending,
}) => {
  // Khóa đang chờ duyệt: tạm khóa thêm / sửa / xóa / sắp xếp
  const locked = useCourseEditLocked()
  const lockHint = useCourseEditLockHint()
  const [keyword, setKeyword] = useState('')
  const [page, setPage] = useState(1)

  const filtered = useMemo(() => {
    const q = keyword.trim().toLowerCase()
    const indexed = lessons.map((lesson, idx) => ({ lesson, idx }))
    return q ? indexed.filter(({ lesson }) => lesson.title.toLowerCase().includes(q)) : indexed
  }, [lessons, keyword])

  // Xóa bài / lọc làm trang hiện tại vượt quá số trang → lùi về trang cuối hợp lệ
  const lastPage = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  useEffect(() => {
    if (page > lastPage) setPage(lastPage)
  }, [page, lastPage])

  const pageItems = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="font-bold text-sm text-sumi">
          Danh sách bài học <span className="font-normal text-slate-500">({lessons.length})</span>
        </h3>
        <div className="flex items-center gap-2">
          {lessons.length > PAGE_SIZE && (
            <Input
              allowClear
              size="middle"
              placeholder="Tìm bài học..."
              prefix={<Search className="w-4 h-4 text-slate-400" />}
              value={keyword}
              onChange={(e) => {
                setKeyword(e.target.value)
                setPage(1)
              }}
              className="!w-56"
            />
          )}
          <AntdButton
            type="primary"
            icon={<Plus className="w-4 h-4" />}
            onClick={onAdd}
            disabled={locked}
            title={lockHint}
            className="!rounded-md"
          >
            Thêm bài học
          </AntdButton>
        </div>
      </div>

      {filtered.length === 0 ? (
        <p className="py-6 text-center text-sm text-slate-400">Không có bài học khớp “{keyword}”.</p>
      ) : (
        <ol className="divide-y divide-suna rounded-lg border border-suna">
          {pageItems.map(({ lesson, idx }) => (
            <li
              key={lesson.id}
              onClick={() => onOpen(lesson)}
              className="group flex items-center gap-3 px-3 py-2.5 cursor-pointer hover:bg-slate-50 transition"
            >
              <span className="shrink-0 w-7 text-xs font-bold text-slate-400 tabular-nums">
                {String(idx + 1).padStart(2, '0')}
              </span>
              <span className="flex-1 min-w-0 truncate text-sm text-sumi" title={lesson.title}>
                {lesson.title}
              </span>
              <div className="flex items-center gap-0.5 shrink-0" onClick={(e) => e.stopPropagation()}>
                <IconAction label="Lên trên" disabled={locked || idx === 0 || reorderPending} onClick={() => onMove(lesson.id, 'up')}>
                  <ChevronUp className="w-4 h-4" />
                </IconAction>
                <IconAction
                  label="Xuống dưới"
                  disabled={locked || idx === lessons.length - 1 || reorderPending}
                  onClick={() => onMove(lesson.id, 'down')}
                >
                  <ChevronDown className="w-4 h-4" />
                </IconAction>
                <IconAction label="Sửa bài học" disabled={locked} onClick={() => onEdit(lesson)}>
                  <Pencil className="w-4 h-4" />
                </IconAction>
                <IconAction label="Xóa bài học" danger disabled={locked} onClick={() => onDelete(lesson)}>
                  <Trash2 className="w-4 h-4" />
                </IconAction>
              </div>
            </li>
          ))}
        </ol>
      )}

      {filtered.length > PAGE_SIZE && (
        <div className="flex justify-end">
          <Pagination
            size="small"
            current={page}
            pageSize={PAGE_SIZE}
            total={filtered.length}
            onChange={setPage}
            showSizeChanger={false}
            showTotal={(total, [from, to]) => `${from}–${to} / ${total} bài`}
          />
        </div>
      )}
    </div>
  )
}

const IconAction: React.FC<{
  label: string
  onClick: () => void
  disabled?: boolean
  danger?: boolean
  children: React.ReactNode
}> = ({ label, onClick, disabled, danger, children }) => (
  <button
    type="button"
    title={label}
    aria-label={label}
    disabled={disabled}
    onClick={onClick}
    className={cn(
      'rounded p-1.5 text-slate-400 transition disabled:opacity-30 disabled:cursor-not-allowed',
      danger ? 'hover:bg-rose-50 hover:text-rose-600' : 'hover:bg-indigo-50 hover:text-indigo-600',
    )}
  >
    {children}
  </button>
)
