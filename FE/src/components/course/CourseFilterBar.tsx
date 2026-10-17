import React from 'react'
import { Filter, X } from 'lucide-react'
import type { JlptLevel } from '@/types'

const JLPT_LEVELS: JlptLevel[] = ['N5', 'N4', 'N3', 'N2', 'N1']

interface CourseFilterBarProps {
  keyword: string
  onKeywordChange: (v: string) => void
  selectedLevel: JlptLevel | undefined
  onLevelChange: (v: JlptLevel | undefined) => void
  /** Khi chỉ hiển thị filter level (không cần search). */
  hideSearch?: boolean
  extra?: React.ReactNode
}

export const CourseFilterBar: React.FC<CourseFilterBarProps> = ({
  keyword,
  onKeywordChange,
  selectedLevel,
  onLevelChange,
  hideSearch = false,
  extra,
}) => {
  const hasFilter = !!keyword || !!selectedLevel

  return (
    <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-white p-3 rounded-md border border-gray-200 shadow-xs">
      {!hideSearch && (
        <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-gray-50 rounded-md border border-gray-200/80 focus-within:border-indigo-400 focus-within:ring-2 focus-within:ring-indigo-100 transition">
          <input
            type="text"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="Tìm kiếm khóa học theo tiêu đề..."
            className="flex-1 bg-transparent text-sm outline-hidden text-gray-900 placeholder:text-gray-400"
          />
          {keyword && (
            <button
              type="button"
              onClick={() => onKeywordChange('')}
              className="p-1 text-gray-400 hover:text-gray-600 rounded transition"
              title="Xoá"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      )}

      <div className="flex items-center gap-1.5 overflow-x-auto">
        <span className="text-xs text-gray-400 font-semibold flex items-center gap-1 pl-1">
          <Filter className="w-3.5 h-3.5" /> JLPT:
        </span>
        <button
          type="button"
          onClick={() => onLevelChange(undefined)}
          className={`px-2.5 py-1.5 rounded text-xs font-bold transition ${
            selectedLevel === undefined
              ? 'bg-indigo-600 text-white'
              : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
          }`}
        >
          Tất cả
        </button>
        {JLPT_LEVELS.map((lvl) => (
          <button
            key={lvl}
            type="button"
            onClick={() =>
              onLevelChange(selectedLevel === lvl ? undefined : lvl)
            }
            className={`px-2.5 py-1.5 rounded text-xs font-bold transition ${
              selectedLevel === lvl
                ? 'bg-indigo-600 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            {lvl}
          </button>
        ))}
        {hasFilter && (
          <button
            type="button"
            onClick={() => {
              onKeywordChange('')
              onLevelChange(undefined)
            }}
            className="px-2.5 py-1.5 rounded text-xs font-semibold text-rose-600 hover:bg-rose-50 transition"
          >
            Xoá lọc
          </button>
        )}
      </div>

      {extra}
    </div>
  )
}
