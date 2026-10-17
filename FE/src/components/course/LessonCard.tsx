import React from 'react'
import { ChevronUp, ChevronDown, Pencil, Trash2, BookOpen, Sparkles } from 'lucide-react'
import type { LessonSummary, LessonOutline } from '@/types'

interface LessonCardProps {
  lesson: LessonSummary | LessonOutline
  index: number
  total: number
  onEdit?: () => void
  onDelete?: () => void
  onMoveUp?: () => void
  onMoveDown?: () => void
  onOpenSectionEditor?: () => void
}

export const LessonCard: React.FC<LessonCardProps> = ({
  lesson,
  index,
  total,
  onEdit,
  onDelete,
  onMoveUp,
  onMoveDown,
  onOpenSectionEditor,
}) => {
  return (
    <div className="group flex items-center gap-3 p-3.5 bg-white rounded-md border border-gray-200/80 hover:border-indigo-300 hover:shadow-sm transition">
      <div className="shrink-0 w-10 h-10 rounded-md bg-indigo-50 text-indigo-700 font-bold text-sm flex items-center justify-center border border-indigo-200/60">
        {String(index + 1).padStart(2, '0')}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <h4 className="font-semibold text-sm text-gray-900 truncate">
            {lesson.title}
          </h4>
          {lesson.hasQuiz && (
            <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-amber-50 text-amber-700 text-[10px] font-bold border border-amber-200">
              <Sparkles className="w-3 h-3" /> Có quiz
            </span>
          )}
        </div>
        <p className="text-xs text-gray-500 mt-0.5">
          Bài {index + 1}/{total}
        </p>
      </div>

      <div className="flex items-center gap-1">
        {/* ===== TEMP: disabled reorder up/down ===== */}
        {false && onMoveUp && (
          <button
            type="button"
            onClick={onMoveUp}
            disabled={index === 0}
            className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition disabled:opacity-30 disabled:hover:bg-transparent"
            title="Lên"
          >
            <ChevronUp className="w-4 h-4" />
          </button>
        )}
        {false && onMoveDown && (
          <button
            type="button"
            onClick={onMoveDown}
            disabled={index === total - 1}
            className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition disabled:opacity-30 disabled:hover:bg-transparent"
            title="Xuống"
          >
            <ChevronDown className="w-4 h-4" />
          </button>
        )}
        {onOpenSectionEditor && (
          <button
            type="button"
            onClick={onOpenSectionEditor}
            className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition"
            title="Danh sách mục"
          >
            <BookOpen className="w-4 h-4" />
          </button>
        )}
        {onEdit && (
          <button
            type="button"
            onClick={onEdit}
            className="p-1.5 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded transition"
            title="Chỉnh sửa"
          >
            <Pencil className="w-4 h-4" />
          </button>
        )}
        {onDelete && (
          <button
            type="button"
            onClick={onDelete}
            className="p-1.5 text-gray-400 hover:text-rose-600 hover:bg-rose-50 rounded transition"
            title="Xoá"
          >
            <Trash2 className="w-4 h-4" />
          </button>
        )}
      </div>
    </div>
  )
}
