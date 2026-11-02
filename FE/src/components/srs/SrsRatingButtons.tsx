import React from 'react'
import { cn } from '@/utils/cn'
import type { SrsRating } from '@/types'

interface RatingConfig {
  value: SrsRating
  label: string
  hint: string
  className: string
}

const RATINGS: RatingConfig[] = [
  {
    value: 'AGAIN',
    label: 'Lại',
    hint: 'Quên rồi',
    className: 'bg-rose-600 hover:bg-rose-700 active:bg-rose-800',
  },
  {
    value: 'HARD',
    label: 'Khó',
    hint: 'Còn đắn đo',
    className: 'bg-amber-500 hover:bg-amber-600 active:bg-amber-700',
  },
  {
    value: 'GOOD',
    label: 'Tốt',
    hint: 'Đã nhớ',
    className: 'bg-emerald-500 hover:bg-emerald-600 active:bg-emerald-700',
  },
  {
    value: 'EASY',
    label: 'Dễ',
    hint: 'Rất dễ',
    className: 'bg-sky-500 hover:bg-sky-600 active:bg-sky-700',
  },
]

interface SrsRatingButtonsProps {
  disabled?: boolean
  onRate: (rating: SrsRating) => void
}

/**
 * SrsRatingButtons — 4 nút đánh giá SM-2 (Again / Hard / Good / Easy),
 * màu theo chuẩn Anki.
 */
export const SrsRatingButtons: React.FC<SrsRatingButtonsProps> = ({ disabled, onRate }) => {
  return (
    <div className="grid grid-cols-4 gap-2 sm:gap-3 w-full max-w-2xl mx-auto">
      {RATINGS.map((r) => (
        <button
          key={r.value}
          type="button"
          disabled={disabled}
          onClick={() => onRate(r.value)}
          className={cn(
            'flex flex-col items-center justify-center gap-1 py-3 sm:py-4 rounded-2xl text-white font-semibold transition',
            'disabled:opacity-40 disabled:cursor-not-allowed',
            r.className,
          )}
        >
          <span className="text-sm sm:text-base leading-none">{r.label}</span>
          <span className="text-[10px] sm:text-xs font-normal opacity-90">{r.hint}</span>
        </button>
      ))}
    </div>
  )
}
