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
    <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3 bg-surface p-2.5 sm:p-3 rounded-2xl border border-suna/90 shadow-2xs">
      {!hideSearch && (
        <div className="flex-1 flex items-center gap-2 px-3 py-2 bg-kinari/70 rounded-xl border border-suna focus-within:border-ai/50 focus-within:ring-2 focus-within:ring-ai/10 transition">
          <input
            type="text"
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            placeholder="Tìm kiếm khóa học theo tiêu đề..."
            className="flex-1 bg-transparent text-xs sm:text-sm outline-hidden text-sumi placeholder:text-nibi/70"
          />
          {keyword && (
            <button
              type="button"
              onClick={() => onKeywordChange('')}
              className="p-1 text-nibi hover:text-sumi rounded transition cursor-pointer"
              title="Xoá"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      )}

      <div className="flex items-center gap-1.5 overflow-x-auto">
        <span className="text-xs text-nibi font-semibold flex items-center gap-1 pl-1">
          <Filter className="w-3.5 h-3.5 text-ai" /> JLPT:
        </span>
        <button
          type="button"
          onClick={() => onLevelChange(undefined)}
          className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
            selectedLevel === undefined
              ? 'bg-ai text-white shadow-2xs'
              : 'bg-kinari/80 text-sumi hover:bg-kinari border border-suna/60'
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
            className={`px-3 py-1.5 rounded-lg text-xs font-bold transition cursor-pointer ${
              selectedLevel === lvl
                ? 'bg-ai text-white shadow-2xs'
                : 'bg-kinari/80 text-sumi hover:bg-kinari border border-suna/60'
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
            className="px-2.5 py-1.5 rounded-lg text-xs font-semibold text-shu hover:bg-rose-50 transition cursor-pointer"
          >
            Xoá lọc
          </button>
        )}
      </div>

      {extra}
    </div>
  )
}
